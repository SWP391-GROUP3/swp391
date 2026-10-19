package com.swp391.kpi.service;

import com.swp391.kpi.dto.ScoringAttemptDetailResponse;
import com.swp391.kpi.dto.ScoringAttemptResponse;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Service xu ly lan cham diem cua BTV (UC-22, BR-SC-03, BR-SC-06)
@Service
@RequiredArgsConstructor
public class ScoringAttemptService {

    private final ScoringAttemptRepository scoringAttemptRepository;
    private final ScoringAttemptDetailRepository scoringAttemptDetailRepository;
    private final CriteriaSetVersionRepository criteriaSetVersionRepository;
    private final CriteriaRepository criteriaRepository;
    private final AssignmentRepository assignmentRepository;
    private final ArticleRepository articleRepository;
    private final ArticleVersionRepository articleVersionRepository;
    private final CategoryChangeLogRepository categoryChangeLogRepository;
    private final GenreRepository genreRepository;

    // Mo phieu cham cho bai viet va snapshot bo tieu chi ACTIVE
    @Transactional
    public ScoringAttemptResponse openScoringAttempt(Integer articleId, Integer reviewerId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        Assignment assignment = assignmentRepository.findByArticleId(articleId)
                .orElseThrow(() -> new BusinessException("Bài viết chưa được phân công cho Biên tập viên"));

        if (!assignment.getEditorId().equals(reviewerId)) {
            throw new BusinessException("Bạn không được phân công chấm bài viết này!");
        }

        Optional<ScoringAttempt> openAttemptOpt = scoringAttemptRepository
                .findTopByArticleIdOrderByAttemptNoDesc(articleId);

        // Neu da co attempt chua nop (submitted_at == null), tra ve de tiep tuc cham
        if (openAttemptOpt.isPresent() && openAttemptOpt.get().getSubmittedAt() == null) {
            ScoringAttempt openAttempt = openAttemptOpt.get();
            List<ScoringAttemptDetail> details = scoringAttemptDetailRepository.findByScoringAttemptId(openAttempt.getId());
            List<ScoringAttemptDetailResponse> detailResponses = details.stream()
                    .map(ScoringAttemptDetailResponse::from)
                    .toList();
            return ScoringAttemptResponse.from(openAttempt, detailResponses, article.getTitle(), article.getContent());
        }

        // Lay the loai de tim bo tieu chi ACTIVE
        Integer categoryId = article.getCategoryId() != null ? article.getCategoryId() : article.getDeclaredCategoryId();
        CriteriaSetVersion activeCriteriaSet = criteriaSetVersionRepository
                .findByCategoryIdAndStatus(categoryId, CriteriaSetStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("Thể loại của bài viết chưa có bộ tiêu chí nào ở trạng thái ACTIVE"));

        List<Criteria> criteriaList = criteriaRepository
                .findByCriteriaSetVersionIdOrderBySortOrderAsc(activeCriteriaSet.getId());
        if (criteriaList.isEmpty()) {
            throw new BusinessException("Bộ tiêu chí ACTIVE chưa có tiêu chí thành phần nào");
        }

        // Lay ban nop moi nhat cua bai viet
        ArticleVersion articleVersion = articleVersionRepository
                .findFirstByArticleIdAndKindOrderByVersionNoDesc(articleId, ArticleVersionKind.SUBMISSION)
                .orElseGet(() -> {
                    ArticleVersion fallback = new ArticleVersion();
                    fallback.setArticleId(articleId);
                    fallback.setVersionNo(1);
                    fallback.setKind(ArticleVersionKind.SUBMISSION);
                    fallback.setTitle(article.getTitle());
                    fallback.setContent(article.getContent());
                    fallback.setCreatedBy(article.getAuthorId());
                    fallback.setCreatedAt(LocalDateTime.now());
                    return articleVersionRepository.save(fallback);
                });

        int nextAttemptNo = openAttemptOpt.map(a -> a.getAttemptNo() + 1).orElse(1);
        AttemptType attemptType = (nextAttemptNo == 1) ? AttemptType.INITIAL : AttemptType.RESCORE;

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setArticleId(articleId);
        attempt.setVersionId(articleVersion.getId());
        attempt.setAssignment(assignment);
        attempt.setReviewerId(reviewerId);
        attempt.setAttemptNo(nextAttemptNo);
        attempt.setAttemptType(attemptType);
        attempt.setCriteriaSetVersion(activeCriteriaSet);
        attempt.setCategoryId(categoryId);
        attempt.setOpenedAt(LocalDateTime.now());
        attempt.setIsCrossDepartment(false);
        if (openAttemptOpt.isPresent()) {
            attempt.setReplacesAttempt(openAttemptOpt.get());
        }

        ScoringAttempt savedAttempt = scoringAttemptRepository.save(attempt);

        // Snapshot danh sach tieu chi vao scoring_attempt_details
        List<ScoringAttemptDetailResponse> detailResponses = new ArrayList<>();
        for (Criteria c : criteriaList) {
            ScoringAttemptDetail detail = new ScoringAttemptDetail();
            detail.setScoringAttempt(savedAttempt);
            detail.setCriterion(c);
            detail.setScore(BigDecimal.ZERO);
            detail.setComment(null);
            ScoringAttemptDetail savedDetail = scoringAttemptDetailRepository.save(detail);
            detailResponses.add(ScoringAttemptDetailResponse.from(savedDetail));
        }

        return ScoringAttemptResponse.from(savedAttempt, detailResponses, article.getTitle(), article.getContent());
    }

    // Lay lan cham gan nhat cua mot bai viet (UC-30, UC-34)
    @Transactional(readOnly = true)
    public ScoringAttemptResponse getLatestAttemptByArticle(Integer articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        ScoringAttempt attempt = scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(articleId)
                .orElseThrow(() -> new NotFoundException("Bài viết này chưa có lần chấm điểm nào"));

        List<ScoringAttemptDetail> details = scoringAttemptDetailRepository.findByScoringAttemptId(attempt.getId());
        List<ScoringAttemptDetailResponse> detailResponses = details.stream()
                .map(ScoringAttemptDetailResponse::from)
                .toList();

        return ScoringAttemptResponse.from(attempt, detailResponses, article.getTitle(), article.getContent());
    }

    // BTV doi the loai bai viet truoc khi cham (UC-23, BR-SC-05)
    @Transactional
    public ScoringAttemptResponse changeCategory(Integer articleId, Integer newCategoryId, Integer editorId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        Assignment assignment = assignmentRepository.findByArticleId(articleId)
                .orElseThrow(() -> new BusinessException("Bài viết chưa được phân công"));

        if (!assignment.getEditorId().equals(editorId)) {
            throw new BusinessException("Bạn không được phân công chấm bài viết này!");
        }

        genreRepository.findById(newCategoryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại mới với ID: " + newCategoryId));

        Integer currentCategoryId = article.getCategoryId() != null ? article.getCategoryId() : article.getDeclaredCategoryId();
        if (currentCategoryId.equals(newCategoryId)) {
            throw new BusinessException("Thể loại mới phải khác với thể loại hiện tại của bài viết!");
        }

        ScoringAttempt attempt = scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(articleId)
                .orElseGet(() -> {
                    openScoringAttempt(articleId, editorId);
                    return scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(articleId).orElseThrow();
                });

        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException("Không thể đổi thể loại cho phiếu chấm đã nộp!");
        }

        CriteriaSetVersion newActiveCriteriaSet = criteriaSetVersionRepository
                .findByCategoryIdAndStatus(newCategoryId, CriteriaSetStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("Thể loại mới chưa có bộ tiêu chí nào ở trạng thái ACTIVE"));

        List<Criteria> newCriteriaList = criteriaRepository
                .findByCriteriaSetVersionIdOrderBySortOrderAsc(newActiveCriteriaSet.getId());
        if (newCriteriaList.isEmpty()) {
            throw new BusinessException("Bộ tiêu chí ACTIVE của thể loại mới chưa có tiêu chí thành phần nào");
        }

        // Luu log vao category_change_logs
        CategoryChangeLog log = new CategoryChangeLog();
        log.setAssignment(assignment);
        log.setFromCategoryId(currentCategoryId);
        log.setToCategoryId(newCategoryId);
        log.setChangedBy(editorId);
        log.setChangedAt(LocalDateTime.now());
        categoryChangeLogRepository.save(log);

        // Cap nhat the loai bai viet
        article.setCategoryId(newCategoryId);
        articleRepository.save(article);

        // Cap nhat the loai va snapshot bo tieu chi moi cho attempt
        attempt.setCategoryId(newCategoryId);
        attempt.setCriteriaSetVersion(newActiveCriteriaSet);
        scoringAttemptRepository.save(attempt);

        List<ScoringAttemptDetail> oldDetails = scoringAttemptDetailRepository.findByScoringAttemptId(attempt.getId());
        scoringAttemptDetailRepository.deleteAll(oldDetails);

        List<ScoringAttemptDetailResponse> detailResponses = new ArrayList<>();
        for (Criteria c : newCriteriaList) {
            ScoringAttemptDetail detail = new ScoringAttemptDetail();
            detail.setScoringAttempt(attempt);
            detail.setCriterion(c);
            detail.setScore(BigDecimal.ZERO);
            detail.setComment(null);
            ScoringAttemptDetail savedDetail = scoringAttemptDetailRepository.save(detail);
            detailResponses.add(ScoringAttemptDetailResponse.from(savedDetail));
        }

        return ScoringAttemptResponse.from(attempt, detailResponses, article.getTitle(), article.getContent());
    }
}
