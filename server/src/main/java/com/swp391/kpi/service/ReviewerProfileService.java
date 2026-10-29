package com.swp391.kpi.service;

import com.swp391.kpi.dto.*;
import com.swp391.kpi.entity.AppealStatus;
import com.swp391.kpi.entity.CrossReviewTask;
import com.swp391.kpi.entity.CrossReviewTaskStatus;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.AppealRepository;
import com.swp391.kpi.repository.AppealResolutionRepository;
import com.swp391.kpi.repository.CrossReviewTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Nghiep vu ho so BTV (UC-42): danh sach BTV, chi so chat luong (dung lai phep tinh UC-41, chi doc),
 * lich su cham.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   46  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   47  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   56  getEditors()                    lay editors
 *   70  getProfile()                    lay profile
 *   97  getHistory()                    lay history
 *  ---- Ham phu (private, chi dung trong file) ----
 *  122  requireEditor()                 bat buoc editor
 *  135  normalizePeriod()               normalize period
 *  143  inPeriod()                      in period
 *  147  calculateDeviation()            tinh deviation
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewerProfileService {
    private static final int MAX_PAGE_SIZE = 100;
    private final UserReadService userReadService;
    private final ScoringReadService scoringReadService;
    private final AssignmentService assignmentService;
    private final ArticleReadService articleReadService;
    private final AppealRepository appealRepository;
    private final AppealResolutionRepository resolutionRepository;
    private final CrossReviewTaskRepository crossReviewTaskRepository;
    private final ScoringAppealService scoringAppealService;

    public List<ReviewerDirectoryResponse> getEditors(Integer currentUserId) {
        var user = userReadService.requireActive(currentUserId, "ADMIN", "HEAD_OF_DEPARTMENT");
        List<ReviewerDirectoryResponse> result = new ArrayList<>();
        for (var editor : userReadService.getUsers()) {
            if (!"EDITOR".equals(editor.role())) continue;
            if (!"ADMIN".equals(user.role()) && (user.sectionId() == null
                    || !user.sectionId().equals(editor.sectionId()))) continue;
            result.add(new ReviewerDirectoryResponse(editor.id(), editor.fullName(), editor.sectionId(),
                    editor.sectionName(), editor.status()));
        }
        result.sort(Comparator.comparing(ReviewerDirectoryResponse::editorId));
        return result;
    }

    public ReviewerProfileResponse getProfile(Integer editorId, LocalDate period, Integer currentUserId) {
        var editor = requireEditor(editorId, currentUserId);
        LocalDate month = normalizePeriod(period);
        long activeArticles = 0;
        for (var assignment : assignmentService.getAssignmentsByEditorId(editorId)) {
            if ("ASSIGNED".equals(articleReadService.getArticle(assignment.getArticleId()).status())) {
                activeArticles++;
            }
        }
        var tasks = crossReviewTaskRepository.findAll();
        int pendingCrossReviews = 0;
        List<ReviewerCrossReviewResponse> history = new ArrayList<>();
        for (var task : tasks) {
            if (!editorId.equals(task.getReviewerId())) continue;
            if (task.getStatus() == CrossReviewTaskStatus.PENDING) pendingCrossReviews++;
            if (task.getStatus() == CrossReviewTaskStatus.DONE && inPeriod(task.getSubmittedAt(), month)) {
                history.add(new ReviewerCrossReviewResponse(task.getId(), task.getArticleId(),
                        task.getScore(), task.getDeviation(), task.getStatus().name(), task.getSubmittedAt()));
            }
        }
        history.sort(Comparator.comparing(ReviewerCrossReviewResponse::submittedAt).reversed());
        return new ReviewerProfileResponse(editor.id(), editor.fullName(), editor.sectionId(),
                editor.sectionName(), editor.status(), month, activeArticles, pendingCrossReviews,
                scoringAppealService.getPendingCount(editorId),
                calculateDeviation(editor, month, tasks), history);
    }

    public Page<ReviewerScoringHistoryResponse> getHistory(Integer editorId, LocalDate period,
            int page, int size, Integer currentUserId) {
        requireEditor(editorId, currentUserId);
        LocalDate month = normalizePeriod(period);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang không hợp lệ hoặc kích thước trang phải từ 1 đến 100.");
        }
        List<ScoringReadResponse> attempts = new ArrayList<>();
        for (var attempt : scoringReadService.getSubmittedAttempts()) {
            if (editorId.equals(attempt.reviewerId()) && inPeriod(attempt.submittedAt(), month)) attempts.add(attempt);
        }
        attempts.sort(Comparator.comparing(ScoringReadResponse::submittedAt).reversed()
                .thenComparing(ScoringReadResponse::id, Comparator.reverseOrder()));
        var pageable = PageRequest.of(page, size);
        int start = (int) Math.min(pageable.getOffset(), attempts.size());
        int end = Math.min(start + size, attempts.size());
        List<ReviewerScoringHistoryResponse> result = new ArrayList<>();
        for (var attempt : attempts.subList(start, end)) {
            result.add(new ReviewerScoringHistoryResponse(attempt.id(), attempt.articleId(),
                    articleReadService.getArticle(attempt.articleId()).title(), attempt.attemptType(),
                    attempt.totalScore(), attempt.submittedAt()));
        }
        return new PageImpl<>(result, pageable, attempts.size());
    }

    private UserAccessResponse requireEditor(Integer editorId, Integer currentUserId) {
        var user = userReadService.requireActive(currentUserId, "ADMIN", "HEAD_OF_DEPARTMENT");
        for (var editor : userReadService.getUsers()) {
            if (!editor.id().equals(editorId) || !"EDITOR".equals(editor.role())) continue;
            if (!"ADMIN".equals(user.role()) && (user.sectionId() == null
                    || !user.sectionId().equals(editor.sectionId()))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hồ sơ biên tập viên này.");
            }
            return editor;
        }
        throw new NotFoundException("Không tìm thấy biên tập viên.");
    }

    private LocalDate normalizePeriod(LocalDate period) {
        if (period == null) return LocalDate.now().withDayOfMonth(1);
        if (period.getDayOfMonth() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kỳ báo cáo phải là ngày đầu tháng.");
        }
        return period;
    }

    private boolean inPeriod(LocalDateTime time, LocalDate period) {
        return time != null && time.toLocalDate().withDayOfMonth(1).equals(period);
    }

    private ReviewerDeviationResponse calculateDeviation(UserAccessResponse editor, LocalDate period,
            List<CrossReviewTask> tasks) {
        Map<Integer, ScoringReadResponse> byId = new HashMap<>();
        int workload = 0;
        for (var attempt : scoringReadService.getSubmittedAttempts()) {
            byId.put(attempt.id(), attempt);
            if (editor.id().equals(attempt.reviewerId()) && !"APPEAL".equals(attempt.attemptType())
                    && inPeriod(attempt.submittedAt(), period)) workload++;
        }
        List<BigDecimal> differences = new ArrayList<>();
        for (var task : tasks) {
            var original = byId.get(task.getOriginalScoringId());
            if (task.getStatus() == CrossReviewTaskStatus.DONE && inPeriod(task.getSubmittedAt(), period)
                    && original != null && editor.id().equals(original.reviewerId())) {
                differences.add(task.getScore().subtract(original.totalScore()));
            }
        }
        int appealCount = 0;
        int overturned = 0;
        for (var resolution : resolutionRepository.findAll()) {
            if (!inPeriod(resolution.getFinalizedAt(), period)) continue;
            var appeal = appealRepository.findById(resolution.getAppealId()).orElse(null);
            if (appeal == null || appeal.getStatus() != AppealStatus.RESOLVED) continue;
            var original = byId.get(appeal.getScoringAttemptId());
            if (original == null || !editor.id().equals(original.reviewerId())) continue;
            BigDecimal effective = resolution.getEffectiveScore();
            if (effective == null) {
                var effectiveAttempt = byId.get(resolution.getEffectiveAttemptId());
                if (effectiveAttempt == null) continue;
                effective = effectiveAttempt.totalScore();
            }
            BigDecimal difference = effective.subtract(original.totalScore());
            differences.add(difference);
            appealCount++;
            if (difference.signum() != 0) overturned++;
        }
        if (differences.isEmpty()) return null;
        // UC42 chi doc: tai dung phep tinh UC41, khong ghi bang chi so khi mo ho so.
        var index = new ReviewerDeviationCalculator().calculate(differences, appealCount, overturned, workload);
        index.setEditorId(editor.id());
        index.setPeriod(period);
        index.setLastUpdated(LocalDateTime.now());
        return ReviewerDeviationResponse.from(index, editor.fullName(), editor.sectionId(), editor.sectionName());
    }
}
