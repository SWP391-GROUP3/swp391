package com.swp391.kpi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.kpi.dto.ScoringAppealAttemptResponse;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ScoringAttemptRepository;
import com.swp391.kpi.repository.ScoringAttemptDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoringAppealService {
    private final ScoringAttemptRepository attempts;
    private final ScoringAttemptDetailRepository details;
    private final AuditService audit;
    private final UserReadService users;
    private final ObjectMapper json;

    public List<ScoringAppealAttemptResponse> getArticleAttempts(Integer articleId) {
        return attempts.findByArticleIdOrderByAttemptNoAsc(articleId).stream()
                .map(ScoringAppealAttemptResponse::from).toList();
    }

    public long getPendingCount(Integer reviewerId) {
        return attempts.countByReviewerIdAndAttemptTypeAndSubmittedAtIsNull(reviewerId, AttemptType.APPEAL);
    }

    @Transactional
    public ScoringAppealAttemptResponse createDraft(Integer appealId, Integer articleId,
            Integer originalId, Integer reviewerId, boolean crossDepartment) {
        var original = attempts.findLockedById(originalId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm gốc."));
        if (!articleId.equals(original.getArticleId()) || original.getSubmittedAt() == null
                || original.getAttemptType() == AttemptType.APPEAL) {
            throw new BusinessException("Phiếu chấm gốc không hợp lệ.");
        }
        int next = 1;
        for (var attempt : attempts.findByArticleIdOrderByAttemptNoAsc(articleId)) {
            if (appealId.equals(attempt.getAppealId()))
                throw new BusinessException("Khiếu nại đã được phân công trước đó.");
            next = Math.max(next, attempt.getAttemptNo() + 1);
        }
        var draft = new ScoringAttempt();
        draft.setArticleId(articleId); draft.setVersionId(original.getVersionId());
        draft.setAssignment(original.getAssignment()); draft.setCriteriaSetVersion(original.getCriteriaSetVersion());
        draft.setCategoryId(original.getCategoryId()); draft.setAppealId(appealId);
        draft.setReviewerId(reviewerId); draft.setAttemptNo(next); draft.setAttemptType(AttemptType.APPEAL);
        draft.setIsCrossDepartment(crossDepartment);
        return ScoringAppealAttemptResponse.from(attempts.save(draft));
    }

    @Transactional
    public ScoringAppealAttemptResponse reassignDraft(Integer appealId, Integer articleId, Integer attemptId,
            Integer expectedReviewerId, Integer reviewerId, boolean crossDepartment, String reason, Integer actorId) {
        var draft = lockedDraft(attemptId);
        if (!appealId.equals(draft.getAppealId()) || !articleId.equals(draft.getArticleId())
                || !expectedReviewerId.equals(draft.getReviewerId()))
            throw new BusinessException("Phân công đã thay đổi. Vui lòng tải lại khiếu nại.");
        if (reason == null || reason.isBlank() || reason.length() > 1000 || reviewerId.equals(expectedReviewerId))
            throw new BusinessException("Cần chọn người chấm khác và nhập lý do giao lại hợp lệ.");
        var oldDetails = details.findByScoringAttemptId(attemptId);
        String oldValue = snapshot(draft, oldDetails, null);
        // Giu ban nhap cu trong audit, khong chuyen diem/nhan xet rieng sang BTV moi.
        draft.setReviewerId(reviewerId); draft.setIsCrossDepartment(crossDepartment);
        draft.setTotalScore(null); draft.setDecision(null); draft.setDecisionNote(null);
        audit.log(actorId, "REASSIGN", "appeals", appealId, oldValue, snapshot(draft, List.of(), reason.trim()));
        details.deleteAll(oldDetails);
        return ScoringAppealAttemptResponse.from(draft);
    }

    @Transactional
    public ScoringAppealAttemptResponse requireOwnedDraft(Integer attemptId, Integer reviewerId) {
        users.requireActive(reviewerId, "EDITOR");
        var draft = lockedDraft(attemptId);
        if (!reviewerId.equals(draft.getReviewerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không còn được giao phiếu chấm này.");
        return ScoringAppealAttemptResponse.from(draft);
    }

    private ScoringAttempt lockedDraft(Integer id) {
        var draft = attempts.findLockedById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm."));
        if (draft.getAttemptType() != AttemptType.APPEAL || draft.getAppealId() == null)
            throw new BusinessException("Đây không phải phiếu chấm khiếu nại.");
        if (draft.getSubmittedAt() != null)
            throw new BusinessException("Không thể đổi người chấm cho phiếu đã nộp.");
        return draft;
    }

    private String snapshot(ScoringAttempt draft, List<ScoringAttemptDetail> scores, String reason) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("attemptId", draft.getId()); value.put("reviewerId", draft.getReviewerId());
        value.put("attemptNo", draft.getAttemptNo()); value.put("crossDepartment", draft.getIsCrossDepartment());
        value.put("totalScore", draft.getTotalScore()); value.put("decision", draft.getDecision());
        value.put("decisionNote", draft.getDecisionNote()); value.put("reason", reason);
        List<Map<String, Object>> items = new ArrayList<>();
        for (var score : scores) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("criterionId", score.getCriterion().getId()); item.put("score", score.getScore());
            item.put("comment", score.getComment()); items.add(item);
        }
        value.put("details", items);
        try { return json.writeValueAsString(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Không thể lưu nhật ký giao lại.", ex);
        }
    }
}
