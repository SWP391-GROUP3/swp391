package com.swp391.kpi.service;

import com.swp391.kpi.dto.ScoreApprovalDecisionRequest;
import com.swp391.kpi.dto.ScoreApprovalItemResponse;
import com.swp391.kpi.dto.ScoreApprovalResponse;
import com.swp391.kpi.entity.ApprovalDecision;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.ScoreApproval;
import com.swp391.kpi.entity.ScoringAttempt;
import com.swp391.kpi.entity.User;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ArticleRepository;
import com.swp391.kpi.repository.ScoreApprovalRepository;
import com.swp391.kpi.repository.ScoringAttemptRepository;
import com.swp391.kpi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xu ly phe duyet diem danh gia cua Truong ban bien tap (UC-29, UC-31, UC-32, BR-SC-10).
 */
@Service
@RequiredArgsConstructor
public class ScoreApprovalService {

    private final ScoreApprovalRepository scoreApprovalRepository;
    private final ScoringAttemptRepository scoringAttemptRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final ArticleService articleService;
    private final ScoringAttemptService scoringAttemptService;

    /**
     * Lay danh sach cac bai viet cho Truong ban duyet diem (UC-29).
     * Chi lay cac bai co status = PENDING_APPROVAL.
     */
    @Transactional(readOnly = true)
    public List<ScoreApprovalItemResponse> getPendingApprovals(Integer sectionId) {
        List<Article> articles = (sectionId != null)
                ? articleRepository.findByStatusAndSectionIdOrderByUpdatedAtDesc(ArticleStatus.PENDING_APPROVAL, sectionId)
                : articleRepository.findByStatusOrderByUpdatedAtDesc(ArticleStatus.PENDING_APPROVAL);

        List<ScoreApprovalItemResponse> result = new ArrayList<>();
        for (Article article : articles) {
            ScoringAttempt attempt = scoringAttemptRepository
                    .findTopByArticleIdOrderByAttemptNoDesc(article.getId())
                    .orElse(null);

            if (attempt != null && attempt.getSubmittedAt() != null) {
                String reviewerName = userRepository.findById(attempt.getReviewerId())
                        .map(User::getFullName)
                        .orElse("Biên tập viên #" + attempt.getReviewerId());

                result.add(ScoreApprovalItemResponse.from(attempt, article.getTitle(), reviewerName));
            }
        }
        return result;
    }

    /**
     * Truong ban phe duyet diem danh gia (UC-32, BR-SC-10).
     * Chuyen trang thai bai viet sang SCORE_PUBLISHED va ghi nhan thoi diem cong bo announced_at.
     */
    @Transactional
    public ScoreApprovalResponse approveScore(Integer articleId, Integer headId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        if (article.getStatus() != ArticleStatus.PENDING_APPROVAL) {
            throw new BusinessException("Chỉ có thể duyệt điểm các bài viết đang ở trạng thái Chờ duyệt điểm (PENDING_APPROVAL)");
        }

        ScoringAttempt attempt = scoringAttemptRepository
                .findTopByArticleIdOrderByAttemptNoDesc(articleId)
                .orElseThrow(() -> new BusinessException("Bài viết chưa có lần chấm điểm nào"));

        if (attempt.getSubmittedAt() == null) {
            throw new BusinessException("Biên tập viên chưa hoàn tất nộp phiếu chấm điểm cho bài viết này");
        }

        if (scoreApprovalRepository.findByScoringAttemptId(attempt.getId()).isPresent()) {
            throw new BusinessException("Lần chấm điểm này đã có quyết định phê duyệt trước đó");
        }

        LocalDateTime now = LocalDateTime.now();
        ScoreApproval approval = new ScoreApproval();
        approval.setScoringAttempt(attempt);
        approval.setDecision(ApprovalDecision.APPROVED);
        approval.setReason(null);
        approval.setDecidedBy(headId);
        approval.setDecidedAt(now);
        approval.setAnnouncedAt(now);

        ScoreApproval saved = scoreApprovalRepository.save(approval);

        // Cap nhat trang thai bai viet sang SCORE_PUBLISHED va ghi log lich su
        articleService.updateArticleStatus(
                articleId,
                ArticleStatus.SCORE_PUBLISHED,
                headId,
                "APPROVE_SCORE",
                "Trưởng ban duyệt điểm đánh giá bài viết (Tổng điểm: " + attempt.getTotalScore() + ")"
        );

        return ScoreApprovalResponse.from(saved);
    }

    /**
     * Truong ban yeu cau cham lai (UC-31, BR-SC-10).
     * Bat buoc co ly do. Chuyen trang thai bai viet ve ASSIGNED va mo lan cham moi (RESCORE) cho dung BTV do.
     */
    @Transactional
    public ScoreApprovalResponse returnScore(Integer articleId, ScoreApprovalDecisionRequest request, Integer headId) {
        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập lý do yêu cầu chấm lại");
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        if (article.getStatus() != ArticleStatus.PENDING_APPROVAL) {
            throw new BusinessException("Chỉ có thể yêu cầu chấm lại các bài viết đang ở trạng thái Chờ duyệt điểm (PENDING_APPROVAL)");
        }

        ScoringAttempt attempt = scoringAttemptRepository
                .findTopByArticleIdOrderByAttemptNoDesc(articleId)
                .orElseThrow(() -> new BusinessException("Bài viết chưa có lần chấm điểm nào"));

        if (attempt.getSubmittedAt() == null) {
            throw new BusinessException("Biên tập viên chưa hoàn tất nộp phiếu chấm điểm cho bài viết này");
        }

        if (scoreApprovalRepository.findByScoringAttemptId(attempt.getId()).isPresent()) {
            throw new BusinessException("Lần chấm điểm này đã có quyết định phê duyệt trước đó");
        }

        LocalDateTime now = LocalDateTime.now();
        String trimmedReason = request.getReason().trim();

        ScoreApproval approval = new ScoreApproval();
        approval.setScoringAttempt(attempt);
        approval.setDecision(ApprovalDecision.RETURNED);
        approval.setReason(trimmedReason);
        approval.setDecidedBy(headId);
        approval.setDecidedAt(now);
        approval.setAnnouncedAt(null);

        ScoreApproval saved = scoreApprovalRepository.save(approval);

        // Cap nhat trang thai bai viet ve ASSIGNED va ghi log lich su
        articleService.updateArticleStatus(
                articleId,
                ArticleStatus.ASSIGNED,
                headId,
                "RETURN_SCORE",
                trimmedReason
        );

        // Tu dong khoi tao lan cham tiep theo (attempt_type = RESCORE) cho dung BTV do
        scoringAttemptService.openScoringAttempt(articleId, attempt.getReviewerId());

        return ScoreApprovalResponse.from(saved);
    }
}
