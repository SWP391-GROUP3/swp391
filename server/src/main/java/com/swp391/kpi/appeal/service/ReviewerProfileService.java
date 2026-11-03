package com.swp391.kpi.appeal.service;

import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.appeal.dto.ReviewerCrossReviewResponse;
import com.swp391.kpi.appeal.dto.ReviewerDeviationResponse;
import com.swp391.kpi.appeal.dto.ReviewerDirectoryResponse;
import com.swp391.kpi.appeal.dto.ReviewerProfileResponse;
import com.swp391.kpi.appeal.dto.ReviewerScoringHistoryResponse;
import com.swp391.kpi.appeal.entity.CrossReviewTask;
import com.swp391.kpi.appeal.entity.CrossReviewTaskStatus;
import com.swp391.kpi.appeal.repository.CrossReviewTaskRepository;
import com.swp391.kpi.article.service.ArticleReadService;
import com.swp391.kpi.assessment.dto.ScoringReadResponse;
import com.swp391.kpi.assessment.service.AssignmentService;
import com.swp391.kpi.assessment.service.ScoringReadService;
import com.swp391.kpi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Nghiep vu ho so BTV (UC-42): danh sach BTV, chi so chat luong (dung lai phep tinh UC-41, chi doc),
 * lich su cham.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   56  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   57  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   66  getEditors()                    lay editors
 *   80  getProfile()                    lay profile
 *  107  getHistory()                    lay history
 *  ---- Ham phu (private, chi dung trong file) ----
 *  132  requireEditor()                 bat buoc editor
 *  145  normalizePeriod()               normalize period
 *  153  inPeriod()                      in period
 *  157  calculateDeviation()            tinh deviation
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
    private final CrossReviewTaskRepository crossReviewTaskRepository;
    private final ScoringAppealService scoringAppealService;
    private final ReviewerDeviationRefreshService deviationService;

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
                calculateDeviation(editor, month), history);
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

    private ReviewerDeviationResponse calculateDeviation(UserAccessResponse editor, LocalDate period) {
        // UC42 chi doc: dung chung phep tinh UC41, khong ghi bang chi so khi mo ho so.
        var index = deviationService.computeIndexes(period, "ADMIN", null).get(editor.id());
        if (index == null) return null;
        return ReviewerDeviationResponse.from(index, editor.fullName(), editor.sectionId(), editor.sectionName());
    }
}
