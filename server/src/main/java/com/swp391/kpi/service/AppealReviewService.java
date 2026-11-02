package com.swp391.kpi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.kpi.dto.*;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.*;
import com.swp391.kpi.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Nghiep vu cham lai mu va chot khieu nai (UC-37...39): hang cho va phieu cua BTV, luu / nop phieu, so
 * sanh diem va chot ket qua theo luat chenh lech.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   40  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   54  getEffectiveScore()             lay effective score
 *   67  getQueue()                      lay queue
 *   72  getCard()                       lay card
 *   80  save()                          luu
 *   98  getComparison()                 lay comparison
 *  103  finalizeAppeal()                chot appeal
 *  ---- Ham phu (private, chi dung trong file) ----
 *  132  comparison()                    comparison
 *  152  scopedAppeal()                  scoped appeal
 *  162  lockedAppeal()                  locked appeal
 *  169  encode()                        encode
 */
@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppealReviewService {
    private final AppealRepository appeals;
    private final AppealResolutionRepository resolutions;
    private final AppealRuleConfigRepository rules;
    private final ScoringAppealService scoring;
    private final UserReadService users;
    private final ArticleReadService articles;
    private final AuditService audit;
    private final NotificationService notifications;
    private final EntityManager em;
    private final ObjectMapper json;
    private final ReviewerDeviationRefreshService deviations;
    private final AppealEffectiveScoreService effectiveScores;
    private final ScoringReadService publishedScores;

    public AppealEffectiveScoreResponse getEffectiveScore(Integer articleId, Integer userId) {
        var user = users.requireActive(userId, "REPORTER", "HEAD_OF_DEPARTMENT", "ADMIN");
        var article = articles.getArticle(articleId);
        if (("REPORTER".equals(user.role()) && !articles.isAuthor(articleId, userId))
                || ("HEAD_OF_DEPARTMENT".equals(user.role())
                    && (user.sectionId() == null || !user.sectionId().equals(article.sectionId()))))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem điểm bài này.");
        var published = publishedScores.getPublishedAttempt(articleId);
        var appeal = appeals.findByArticleId(articleId).orElse(null);
        return new AppealEffectiveScoreResponse(articleId, appeal == null ? null : appeal.getStatus().name(),
                effectiveScores.getEffectiveScore(articleId, published.totalScore()));
    }

    public List<AppealReviewTaskResponse> getQueue(Integer userId) {
        return scoring.getAssignedTasks(userId);
    }

    @Transactional
    public AppealBlindScorecardResponse getCard(Integer attemptId, Integer userId) {
        users.requireActive(userId, "EDITOR");
        var ref = scoring.getAttemptReference(attemptId);
        lockedAppeal(ref.appealId());
        return scoring.getOwnedScorecard(attemptId, userId);
    }

    @Transactional
    public AppealBlindScorecardResponse save(Integer attemptId, Integer userId, AppealScorecardRequest request, boolean submit) { // [LUONG 3 - CHAM MU] buoc 5/7 service: khoa khieu nai TRUOC khoa phieu, phai dang IN_REVIEW, goi saveScorecard, nop xong bao Truong ban
        users.requireActive(userId, "EDITOR");
        deviations.lockWorkflow();
        var ref = scoring.getAttemptReference(attemptId);
        // Moi luong ghi deu khoa appeal truoc phieu, tranh deadlock voi giao lai/chot ket qua.
        var appeal = lockedAppeal(ref.appealId());
        if (appeal.getStatus() != AppealStatus.IN_REVIEW)
            throw new BusinessException("Khiếu nại không còn ở trạng thái chấm lại.");
        var card = scoring.saveScorecard(attemptId, userId, request, submit);
        if (submit) {
            var article = articles.getArticle(appeal.getArticleId());
            notifications.notifyHeads(article.id(), article.sectionId(), "Đã có kết quả chấm mù cho khiếu nại #" + appeal.getId() + ".");
            var comparison = comparison(appeal);
            audit.log(userId, "COMPARE", "appeals", appeal.getId(), null, encode(comparison.proposal()));
        }
        return card;
    }

    public AppealComparisonResponse getComparison(Integer id, Integer userId) {
        return comparison(scopedAppeal(id, userId, false));
    }

    @Transactional
    public AppealComparisonResponse finalizeAppeal(Integer id, Integer userId, AppealFinalizeRequest request) { // [LUONG 4 - SO SANH & CHOT] buoc 6/7 service chot: tu tinh lai bang compare() -> ghi appeal_resolutions -> RESOLVED -> audit FINALIZE -> cap nhat do lech
        var appeal = scopedAppeal(id, userId, true);
        if (appeal.getStatus() != AppealStatus.IN_REVIEW || resolutions.findByAppealId(id).isPresent())
            throw new BusinessException("Khiếu nại đã được xử lý trước đó hoặc chưa được phân công.");
        var compared = comparison(appeal);
        var current = compared.second() == null ? compared.first() : compared.second();
        if (current == null || current.submittedAt() == null || !current.id().equals(request.getExpectedAttemptId()))
            throw new BusinessException("Phiếu đã thay đổi hoặc chưa nộp. Vui lòng tải lại kết quả.");
        var proposed = compared.proposal();
        if (proposed == null || proposed.requiresSecondReview())
            throw new BusinessException("Cần hoàn tất S2 trước khi xác nhận điểm hiệu lực.");
        var resolution = new AppealResolution();
        resolution.setAppealId(id); resolution.setEffectiveAttemptId(current.id());
        resolution.setEffectiveScore(proposed.effectiveScore()); resolution.setRuleApplied(proposed.rule());
        resolution.setFinalizedBy(userId); resolution.setFinalizedAt(LocalDateTime.now());
        resolutions.save(resolution);
        appeal.setStatus(AppealStatus.RESOLVED); appeal.setResolvedAt(resolution.getFinalizedAt());
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("status", "RESOLVED"); event.put("effectiveScore", proposed.effectiveScore()); event.put("rule", proposed.rule());
        event.put("effectiveAttemptId", current.id()); event.put("downstreamStatus", "EFFECTIVE_SCORE_READY"); // BF-04/BF-05 doc diem nay qua AppealEffectiveScoreService
        audit.log(userId, "FINALIZE", "appeals", id, "{\"status\":\"IN_REVIEW\"}", encode(event));
        var article = articles.getArticle(appeal.getArticleId());
        notifications.notifyUser(appeal.getAppellantId(), article.id(), "Khiếu nại #" + id + " đã được xác nhận. Điểm hiệu lực: " + proposed.effectiveScore() + ".");
        notifications.notifyHeads(article.id(), article.sectionId(), "Khiếu nại #" + id + " đã được giải quyết.");
        appeals.flush();
        deviations.refreshSection(resolution.getFinalizedAt().toLocalDate().withDayOfMonth(1), article.sectionId());
        return comparison(appeal);
    }

    private AppealComparisonResponse comparison(Appeal appeal) { // [LUONG 4 - SO SANH & CHOT] 6a: gom S0, S1, S2 + lay nguong T cua LUAT LUC GUI khieu nai (BR-QC-12), goi compare()
        var original = scoring.getComparisonCard(appeal.getScoringAttemptId());
        List<AppealBlindScorecardResponse> reviews = new ArrayList<>();
        for (var attempt : scoring.getArticleAttempts(appeal.getArticleId()))
            if (appeal.getId().equals(attempt.appealId())) reviews.add(scoring.getComparisonCard(attempt.id()));
        if (reviews.size() > 2) throw new BusinessException("Dữ liệu có quá hai đợt chấm lại, cần kiểm tra nhật ký.");
        var first = reviews.isEmpty() ? null : reviews.get(0);
        var second = reviews.size() < 2 ? null : reviews.get(1);
        var rule = rules.findById(appeal.getRuleConfigId()).orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình gắn với khiếu nại."));
        AppealScoreCalculator.Result proposed = null;
        if (first != null && first.submittedAt() != null && (second == null || second.submittedAt() != null))
            proposed = new AppealScoreCalculator().compare(original.totalScore(), first.totalScore(),
                    second == null ? null : second.totalScore(), rule.getVarianceThreshold());
        var confirmed = resolutions.findByAppealId(appeal.getId()).orElse(null);
        return new AppealComparisonResponse(appeal.getId(), appeal.getStatus().name(), rule.getVarianceThreshold(),
                original, first, second, proposed, confirmed == null ? null
                        : effectiveScores.getEffectiveScore(appeal.getArticleId(), original.totalScore()),
                confirmed == null || confirmed.getRuleApplied() == null ? null : confirmed.getRuleApplied().name());
    }

    private Appeal scopedAppeal(Integer id, Integer userId, boolean lock) {
        var head = users.requireActive(userId, "HEAD_OF_DEPARTMENT");
        if (lock) deviations.lockWorkflow();
        var appeal = lock ? lockedAppeal(id) : appeals.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy khiếu nại."));
        var article = articles.getArticle(appeal.getArticleId());
        if (head.sectionId() == null || !head.sectionId().equals(article.sectionId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xử lý khiếu nại này.");
        return appeal;
    }

    private Appeal lockedAppeal(Integer id) {
        var appeal = appeals.findLockedById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy khiếu nại."));
        em.refresh(appeal, LockModeType.PESSIMISTIC_WRITE);
        scoring.lockReviewSnapshots(appeal.getId(), appeal.getArticleId());
        return appeal;
    }

    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Không thể ghi nhật ký khiếu nại.", ex); }
    }
}
