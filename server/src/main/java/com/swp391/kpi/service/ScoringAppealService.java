package com.swp391.kpi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.kpi.dto.ScoringAppealAttemptResponse;
import com.swp391.kpi.dto.AppealBlindScorecardResponse;
import com.swp391.kpi.dto.AppealScorecardRequest;
import com.swp391.kpi.dto.AppealReviewTaskResponse;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ScoringAttemptRepository;
import com.swp391.kpi.repository.ScoringAttemptDetailRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Phieu cham phuc vu khieu nai (BF-03 dung du lieu BF-02): tao / giao lai ban nhap cham mu, luu phieu,
 * lay phieu so sanh, dem phieu cho.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   52  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   62  lockReviewSnapshots()           khoa review snapshots
 *   67  getAttemptReference()           lay attempt reference
 *   74  getAssignedTasks()              lay assigned tasks
 *   85  getOwnedScorecard()             lay owned scorecard
 *   96  getComparisonCard()             Chi goi tu service da kiem tra quyen Head hoac chu phieu; khong mo endpoint chung
 *  113  saveScorecard()                 luu scorecard
 *  150  createSecondDraft()             tao second draft
 *  172  getArticleAttempts()            lay article attempts
 *  177  getPendingCount()               lay pending count
 *  182  createDraft()                   tao draft
 *  206  reassignDraft()                 phan cong lai draft
 *  225  requireOwnedDraft()             bat buoc owned draft
 *  ---- Ham phu (private, chi dung trong file) ----
 *  233  lockedDraft()                   locked draft
 *  245  snapshot()                      snapshot
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoringAppealService {
    private final ScoringAttemptRepository attempts;
    private final ScoringAttemptDetailRepository details;
    private final AuditService audit;
    private final UserReadService users;
    private final ObjectMapper json;
    private final EntityManager entityManager;
    private final com.swp391.kpi.repository.CriteriaRepository criteria;
    private final ArticleReadService articles;

    @Transactional
    public void lockReviewSnapshots(Integer appealId, Integer articleId) {
        for (var attempt : attempts.findByArticleIdOrderByAttemptNoAsc(articleId))
            if (appealId.equals(attempt.getAppealId())) entityManager.refresh(attempt, LockModeType.PESSIMISTIC_WRITE);
    }

    public ScoringAppealAttemptResponse getAttemptReference(Integer id) {
        var attempt = attempts.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm."));
        if (attempt.getAttemptType() != AttemptType.APPEAL)
            throw new BusinessException("Đây không phải phiếu chấm khiếu nại.");
        return ScoringAppealAttemptResponse.from(attempt);
    }

    public List<AppealReviewTaskResponse> getAssignedTasks(Integer reviewerId) {
        users.requireActive(reviewerId, "EDITOR");
        List<AppealReviewTaskResponse> result = new ArrayList<>();
        for (var attempt : attempts.findByReviewerIdAndAttemptTypeOrderByOpenedAtDesc(reviewerId, AttemptType.APPEAL)) {
            var version = articles.getVersion(attempt.getArticleId(), attempt.getVersionId());
            result.add(new AppealReviewTaskResponse(attempt.getId(), version.title(), attempt.getSubmittedAt()));
        }
        return result;
    }

    @Transactional
    public AppealBlindScorecardResponse getOwnedScorecard(Integer id, Integer reviewerId) {
        users.requireActive(reviewerId, "EDITOR");
        var locked = attempts.findLockedById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm."));
        entityManager.refresh(locked, LockModeType.PESSIMISTIC_WRITE);
        var attempt = getAttemptReference(id);
        if (!reviewerId.equals(attempt.reviewerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không được giao phiếu chấm này.");
        return getComparisonCard(id);
    }

    // Chi goi tu service da kiem tra quyen Head hoac chu phieu; khong mo endpoint chung.
    public AppealBlindScorecardResponse getComparisonCard(Integer id) {
        var attempt = attempts.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm."));
        var version = articles.getVersion(attempt.getArticleId(), attempt.getVersionId());
        var scores = details.findByScoringAttemptId(id);
        List<AppealBlindScorecardResponse.Criterion> rows = new ArrayList<>();
        for (var criterion : criteria.findByCriteriaSetVersionIdOrderBySortOrderAsc(attempt.getCriteriaSetVersion().getId())) {
            ScoringAttemptDetail own = null;
            for (var score : scores) if (criterion.getId().equals(score.getCriterion().getId())) own = score;
            rows.add(new AppealBlindScorecardResponse.Criterion(criterion.getId(), criterion.getName(), criterion.getWeight(),
                    own == null ? null : own.getScore(), own == null ? null : own.getComment()));
        }
        return new AppealBlindScorecardResponse(id, attempt.getArticleId(), attempt.getVersionId(), attempt.getCategoryId(),
                attempt.getCriteriaSetVersion().getId(), version.title(), version.content(), attempt.getTotalScore(),
                attempt.getSubmittedAt(), rows);
    }

    @Transactional
    public AppealBlindScorecardResponse saveScorecard(Integer id, Integer reviewerId, AppealScorecardRequest request, boolean submit) {
        users.requireActive(reviewerId, "EDITOR");
        var attempt = lockedDraft(id);
        if (!reviewerId.equals(attempt.getReviewerId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không còn được giao phiếu chấm này.");
        var rubric = criteria.findByCriteriaSetVersionIdOrderBySortOrderAsc(attempt.getCriteriaSetVersion().getId());
        Map<Integer, AppealScorecardRequest.Item> incoming = new LinkedHashMap<>();
        if (request == null || request.getCriteria() == null) throw new BusinessException("Thiếu dữ liệu phiếu chấm.");
        for (var item : request.getCriteria()) {
            if (item == null || item.getCriterionId() == null || incoming.put(item.getCriterionId(), item) != null)
                throw new BusinessException("Tiêu chí không hợp lệ hoặc bị trùng.");
        }
        List<AppealScoreCalculator.Item> calculation = new ArrayList<>();
        List<ScoringAttemptDetail> replacement = new ArrayList<>();
        for (var criterion : rubric) {
            var item = incoming.remove(criterion.getId());
            calculation.add(new AppealScoreCalculator.Item(criterion.getId(), criterion.getWeight(),
                    item == null ? null : item.getScore(), item == null ? null : item.getComment()));
            if (item == null || item.getScore() == null) continue;
            var detail = new ScoringAttemptDetail(); detail.setScoringAttempt(attempt); detail.setCriterion(criterion);
            detail.setScore(item.getScore()); detail.setComment(item.getComment() == null ? null : item.getComment().trim());
            replacement.add(detail);
        }
        if (!incoming.isEmpty()) throw new BusinessException("Tiêu chí không thuộc bộ tiêu chí của phiếu chấm.");
        var total = new AppealScoreCalculator().weightedTotal(calculation, submit);
        var oldDetails = details.findByScoringAttemptId(id);
        String before = snapshot(attempt, oldDetails, null);
        details.deleteAll(oldDetails); details.flush(); details.saveAll(replacement);
        attempt.setTotalScore(total); attempt.setDecision(null); attempt.setDecisionNote(null);
        if (submit) attempt.setSubmittedAt(java.time.LocalDateTime.now());
        audit.log(reviewerId, submit ? "SUBMIT_SCORE" : "SAVE_DRAFT", "appeals", attempt.getAppealId(),
                before, snapshot(attempt, replacement, null));
        attempts.flush();
        return getComparisonCard(id);
    }

    @Transactional
    public ScoringAppealAttemptResponse createSecondDraft(Integer appealId, Integer articleId,
            Integer originalId, Integer reviewerId, boolean crossDepartment) {
        var originals = attempts.findByArticleIdOrderByAttemptNoAsc(articleId);
        ScoringAttempt first = null;
        int count = 0;
        for (var attempt : originals) if (appealId.equals(attempt.getAppealId())) { count++; first = attempt; }
        if (count != 1 || first == null || first.getSubmittedAt() == null)
            throw new BusinessException("Chỉ tạo S2 sau khi S1 đã nộp và chưa có S2.");
        var original = attempts.findLockedById(originalId).orElseThrow(() -> new NotFoundException("Không có phiếu gốc."));
        if (!articleId.equals(original.getArticleId()) || original.getSubmittedAt() == null || original.getAttemptType() == AttemptType.APPEAL
                || reviewerId.equals(original.getReviewerId()) || reviewerId.equals(first.getReviewerId()))
            throw new BusinessException("Phiếu hoặc người chấm S2 không hợp lệ.");
        var draft = new ScoringAttempt();
        int next = 1;
        for (var attempt : originals) next = Math.max(next, attempt.getAttemptNo() + 1);
        draft.setArticleId(articleId); draft.setVersionId(original.getVersionId()); draft.setAssignment(original.getAssignment());
        draft.setCriteriaSetVersion(original.getCriteriaSetVersion()); draft.setCategoryId(original.getCategoryId());
        draft.setAppealId(appealId); draft.setReviewerId(reviewerId); draft.setAttemptNo(next);
        draft.setAttemptType(AttemptType.APPEAL); draft.setIsCrossDepartment(crossDepartment);
        return ScoringAppealAttemptResponse.from(attempts.save(draft));
    }

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
        // Query co khoa khong tu nap lai entity da cache trong cung transaction.
        entityManager.refresh(draft, LockModeType.PESSIMISTIC_WRITE);
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
        value.put("submittedAt", draft.getSubmittedAt());
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
