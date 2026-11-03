package com.swp391.kpi.service;

import com.swp391.kpi.dto.GenreResponse;
import com.swp391.kpi.dto.PublishingItemResponse;
import com.swp391.kpi.dto.PublishingResponse;
import com.swp391.kpi.dto.ScoreApprovalDecisionRequest;
import com.swp391.kpi.dto.ScoreApprovalItemResponse;
import com.swp391.kpi.dto.ScoreApprovalResponse;
import com.swp391.kpi.dto.UserAccessResponse;
import com.swp391.kpi.entity.ApprovalDecision;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.AttemptType;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service xu ly phe duyet diem danh gia va duyet xuat ban cua Truong ban bien tap (UC-29, UC-31, UC-32, UC-33, BR-SC-10, BR-SC-11).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   54  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   71  getPendingApprovals()           Lay danh sach cac bai viet cho Truong ban duyet diem (UC-29)
 *   98  approveScore()                  Truong ban phe duyet diem danh gia (UC-31, BR-SC-10)
 *  146  returnScore()                   Truong ban yeu cau cham lai (UC-32, BR-SC-10)
 *  203  getPendingPublishing()          Lay danh sach cac bai viet cho Truong ban duyet xuat ban (UC-33)
 *  277  approvePublishing()             Truong ban phe duyet xuat ban bai viet (UC-33, BR-SC-11)
 *  ---- Ham phu (private, chi dung trong file) ----
 *  299  findLatestAttempt()             M-A2: lan cham moi nhat cua BF-02 (INITIAL / RESCORE), bo qua lan cham APPEAL do BF-03 ghi
 *  304  checkHeadOfSection()            Truong ban chi duyet bai cua ban minh (users.section_id = articles.section_id)
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
    private final GenreService genreService;
    // Du lieu BF-03 -> goi service cua Quang (Q-D1, Q-D2)
    private final AppealRuleConfigService appealRuleConfigService;
    private final AppealEffectiveScoreService appealEffectiveScoreService;
    private final UserReadService userReadService;   // service luong admin: lay ban cua Truong ban

    /**
     * Lay danh sach cac bai viet cho Truong ban duyet diem (UC-29).
     * Chi lay cac bai co status = PENDING_APPROVAL.
     */
    @Transactional(readOnly = true)
    public List<ScoreApprovalItemResponse> getPendingApprovals(Integer currentUserId) {
        // M-A3: chi bai cua ban Truong ban dang dang nhap
        Integer sectionId = userReadService.requireActive(currentUserId, "HEAD_OF_DEPARTMENT").sectionId();
        List<Article> articles = articleRepository.findByStatusAndSectionIdOrderByUpdatedAtDesc(
                ArticleStatus.PENDING_APPROVAL, sectionId);

        List<ScoreApprovalItemResponse> result = new ArrayList<>();
        for (Article article : articles) {
            ScoringAttempt attempt = findLatestAttempt(article.getId())
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
     * Truong ban phe duyet diem danh gia (UC-31, BR-SC-10).
     * Chuyen trang thai bai viet sang SCORE_PUBLISHED va ghi nhan thoi diem cong bo announced_at.
     */
    @Transactional
    public ScoreApprovalResponse approveScore(Integer articleId, Integer headId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));
        checkHeadOfSection(headId, article);

        if (article.getStatus() != ArticleStatus.PENDING_APPROVAL) {
            throw new BusinessException("Chỉ có thể duyệt điểm các bài viết đang ở trạng thái Chờ duyệt điểm (PENDING_APPROVAL)");
        }

        ScoringAttempt attempt = findLatestAttempt(articleId)
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
     * Truong ban yeu cau cham lai (UC-32, BR-SC-10).
     * Bat buoc co ly do. Chuyen trang thai bai viet ve ASSIGNED va mo lan cham moi (RESCORE) cho dung BTV do.
     */
    @Transactional
    public ScoreApprovalResponse returnScore(Integer articleId, ScoreApprovalDecisionRequest request, Integer headId) {
        if (request == null || request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập lý do yêu cầu chấm lại");
        }

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));
        checkHeadOfSection(headId, article);

        if (article.getStatus() != ArticleStatus.PENDING_APPROVAL) {
            throw new BusinessException("Chỉ có thể yêu cầu chấm lại các bài viết đang ở trạng thái Chờ duyệt điểm (PENDING_APPROVAL)");
        }

        ScoringAttempt attempt = findLatestAttempt(articleId)
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

    /**
     * Lay danh sach cac bai viet cho Truong ban duyet xuat ban (UC-33).
     * Chi lay cac bai co status = SCORE_PUBLISHED.
     */
    @Transactional(readOnly = true)
    public List<PublishingItemResponse> getPendingPublishing(Integer currentUserId) {
        // M-A3: chi bai cua ban Truong ban dang dang nhap
        Integer sectionId = userReadService.requireActive(currentUserId, "HEAD_OF_DEPARTMENT").sectionId();
        List<Article> articles = articleRepository.findByStatusAndSectionIdOrderByUpdatedAtDesc(
                ArticleStatus.SCORE_PUBLISHED, sectionId);

        Map<Integer, String> genreMap = new HashMap<>();
        for (GenreResponse g : genreService.getActiveGenres()) {
            genreMap.put(g.getId(), g.getName());
        }

        List<PublishingItemResponse> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Article article : articles) {
            ScoringAttempt attempt = findLatestAttempt(article.getId())
                    .orElse(null);

            if (attempt != null && attempt.getSubmittedAt() != null) {
                String reviewerName = userRepository.findById(attempt.getReviewerId())
                        .map(User::getFullName)
                        .orElse("Biên tập viên #" + attempt.getReviewerId());

                String authorName = userRepository.findById(article.getAuthorId())
                        .map(User::getFullName)
                        .orElse("Tác giả #" + article.getAuthorId());

                String categoryName = genreMap.getOrDefault(article.getCategoryId(), "Chưa phân loại");

                ScoreApproval approval = scoreApprovalRepository.findByScoringAttemptId(attempt.getId())
                        .orElse(null);

                LocalDateTime scoreApprovedAt = (approval != null && approval.getAnnouncedAt() != null)
                        ? approval.getAnnouncedAt()
                        : (approval != null ? approval.getDecidedAt() : null);

                // M-A5: han khieu nai theo cau hinh dang bat (giong man khieu nai BF-03)
                LocalDateTime appealDeadline = appealRuleConfigService.getAppealDeadline(scoreApprovedAt);
                boolean appealOpen = appealDeadline != null && now.isBefore(appealDeadline);
                long daysRemaining = 0;
                if (appealOpen && scoreApprovedAt != null) {
                    daysRemaining = ChronoUnit.DAYS.between(now, appealDeadline);
                    if (daysRemaining < 0) {
                        daysRemaining = 0;
                    }
                }

                PublishingItemResponse item = new PublishingItemResponse(
                        article.getId(),
                        article.getTitle(),
                        authorName,
                        article.getCategoryId(),
                        categoryName,
                        // M-A2: diem hieu luc = diem sau khieu nai neu khieu nai da ket luan, nguoc lai diem S0
                        appealEffectiveScoreService.getEffectiveScore(article.getId(), attempt.getTotalScore()),
                        reviewerName,
                        scoreApprovedAt,
                        appealDeadline,
                        appealOpen,
                        (int) daysRemaining,
                        article.getUpdatedAt()
                );
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Truong ban phe duyet xuat ban bai viet (UC-33, BR-SC-11).
     * Chuyen bai viet sang PUBLISHED, dong dau thoi gian published_at va nguoi duyet approved_by.
     * Ban giao sang luong BF-04 de tinh nhuan but.
     */
    @Transactional
    public PublishingResponse approvePublishing(Integer articleId, Integer headId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));
        checkHeadOfSection(headId, article);

        if (article.getStatus() != ArticleStatus.SCORE_PUBLISHED) {
            throw new BusinessException("Chỉ có thể duyệt xuất bản các bài viết đã công bố điểm (SCORE_PUBLISHED)");
        }

        // Goi articleService.publishArticle de cap nhat trang thai va ghi article_status_logs (AGENTS.md muc 2)
        articleService.publishArticle(articleId, headId);

        return new PublishingResponse(
                articleId,
                ArticleStatus.PUBLISHED.name(),
                LocalDateTime.now(),
                headId,
                "Duyệt xuất bản bài viết thành công"
        );
    }

    // M-A2: lan cham moi nhat cua BF-02 (INITIAL / RESCORE), bo qua lan cham APPEAL do BF-03 ghi
    private Optional<ScoringAttempt> findLatestAttempt(Integer articleId) {
        return scoringAttemptRepository.findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc(articleId, AttemptType.APPEAL);
    }

    // Truong ban chi duyet bai cua ban minh (users.section_id = articles.section_id)
    private void checkHeadOfSection(Integer headId, Article article) {
        UserAccessResponse head = userReadService.requireActive(headId, "HEAD_OF_DEPARTMENT");
        if (!article.getSectionId().equals(head.sectionId())) {
            throw new BusinessException("Bài viết không thuộc ban của bạn.");
        }
    }
}
