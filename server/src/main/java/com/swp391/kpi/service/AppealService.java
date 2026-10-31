package com.swp391.kpi.service;

import com.swp391.kpi.dto.AppealCreateRequest;
import com.swp391.kpi.dto.AppealCriterionRequest;
import com.swp391.kpi.dto.AppealCriterionResponse;
import com.swp391.kpi.dto.AppealEligibilityResponse;
import com.swp391.kpi.dto.AppealResponse;
import com.swp391.kpi.dto.AppealScoreCriterionResponse;
import com.swp391.kpi.entity.Appeal;
import com.swp391.kpi.entity.AppealCriterion;
import com.swp391.kpi.entity.AppealRuleConfig;
import com.swp391.kpi.entity.AppealStatus;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.AppealCriterionRepository;
import com.swp391.kpi.repository.AppealRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Nghiep vu Phong vien gui khieu nai (UC-35): kiem tra bai va han khieu nai, tieu chi hop le, luu
 * khieu nai; danh sach bai co the khieu nai va khieu nai cua toi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   52  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   55  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   67  AppealService()                 ham khoi tao
 *  106  submitAppeal()                  nop appeal
 *  138  getEligibility()                lay eligibility
 *  168  getEligibleArticles()           lay eligible articles
 *  182  getMyAppeals()                  lay my appeals
 *  ---- Ham phu (private, chi dung trong file) ----
 *  196  validateSubmission()            kiem tra submission
 *  208  validateArticleAndWindow()      kiem tra article va window
 *  231  validateCriteria()              kiem tra criteria
 *  259  saveCriteria()                  luu criteria
 *  273  toResponse()                    doi sang response
 */
@Service
public class AppealService {

    private static final String SCORE_PUBLISHED = "SCORE_PUBLISHED";
    private static final String PUBLISHED = "PUBLISHED";

    private final AppealRepository appealRepository;
    private final AppealCriterionRepository appealCriterionRepository;
    private final AppealRuleConfigService appealRuleConfigService;
    private final AppealSubmissionDataProvider appealSubmissionDataProvider;
    private final AuditService auditService;
    private final UserReadService userReadService;
    private final NotificationService notificationService;
    private final ArticleReadService articleReadService;
    private final Clock clock;
    private final AppealEffectiveScoreService effectiveScores;

    @Autowired
    public AppealService(
            AppealRepository appealRepository,
            AppealCriterionRepository appealCriterionRepository,
            AppealRuleConfigService appealRuleConfigService,
            AppealSubmissionDataProvider appealSubmissionDataProvider,
            AuditService auditService,
            UserReadService userReadService,
            NotificationService notificationService,
            ArticleReadService articleReadService,
            AppealEffectiveScoreService effectiveScores) {
        this(appealRepository, appealCriterionRepository, appealRuleConfigService,
                appealSubmissionDataProvider, auditService, userReadService, notificationService,
                articleReadService, effectiveScores, Clock.systemDefaultZone());
    }

    AppealService(
            AppealRepository appealRepository,
            AppealCriterionRepository appealCriterionRepository,
            AppealRuleConfigService appealRuleConfigService,
            AppealSubmissionDataProvider appealSubmissionDataProvider,
            AuditService auditService,
            UserReadService userReadService,
            NotificationService notificationService,
            ArticleReadService articleReadService,
            AppealEffectiveScoreService effectiveScores,
            Clock clock) {
        this.appealRepository = appealRepository;
        this.appealCriterionRepository = appealCriterionRepository;
        this.appealRuleConfigService = appealRuleConfigService;
        this.appealSubmissionDataProvider = appealSubmissionDataProvider;
        this.auditService = auditService;
        this.userReadService = userReadService;
        this.notificationService = notificationService;
        this.articleReadService = articleReadService;
        this.clock = clock;
        this.effectiveScores = effectiveScores;
    }

    @Transactional
    public AppealResponse submitAppeal(AppealCreateRequest request, Integer currentUserId) {
        userReadService.requireActive(currentUserId, "REPORTER");
        AppealSubmissionContext context = appealSubmissionDataProvider
                .getSubmissionContext(request.getArticleId());
        AppealRuleConfig ruleConfig = appealRuleConfigService.getActiveConfigEntity();
        validateSubmission(request, currentUserId, context, ruleConfig);

        Appeal appeal = new Appeal();
        appeal.setArticleId(context.getArticleId());
        appeal.setScoringAttemptId(context.getScoringAttemptId());
        appeal.setAppellantId(currentUserId);
        appeal.setRuleConfigId(ruleConfig.getId());
        appeal.setStatus(AppealStatus.SUBMITTED);
        appeal.setSubmittedAt(LocalDateTime.now(clock));
        Appeal savedAppeal;
        try {
            savedAppeal = appealRepository.save(appeal);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Hai request cung luc: UNIQUE(appeals.article_id) chan cai den sau, tra dung cau cua UC-35 E2.
            throw new BusinessException("Mỗi bài viết chỉ được khiếu nại một lần.");
        }

        List<AppealCriterion> savedCriteria = saveCriteria(savedAppeal, request.getCriteria());
        notificationService.notifyHeads(savedAppeal.getArticleId(),
                articleReadService.getArticle(savedAppeal.getArticleId()).sectionId(),
                "Có khiếu nại mới #" + savedAppeal.getId() + " về điểm bài viết #" + savedAppeal.getArticleId() + ".");
        auditService.log(
                currentUserId,
                "CREATE",
                "appeals",
                savedAppeal.getId(),
                null,
                "{\"articleId\":" + savedAppeal.getArticleId() + ",\"status\":\"SUBMITTED\"}"
        );
        return toResponse(savedAppeal, savedCriteria, context);
    }

    @Transactional(readOnly = true)
    public AppealEligibilityResponse getEligibility(Integer articleId, Integer currentUserId) {
        userReadService.requireActive(currentUserId, "REPORTER");
        AppealSubmissionContext context = appealSubmissionDataProvider.getSubmissionContext(articleId);
        AppealRuleConfig ruleConfig = appealRuleConfigService.getActiveConfigEntity();
        validateArticleAndWindow(currentUserId, context, ruleConfig);
        if (appealRepository.existsByArticleId(articleId)) {
            throw new BusinessException("Mỗi bài viết chỉ được khiếu nại một lần.");
        }

        AppealEligibilityResponse response = new AppealEligibilityResponse();
        response.setArticleId(context.getArticleId());
        response.setArticleTitle(context.getArticleTitle());
        response.setTotalScore(context.getTotalScore());
        response.setAnnouncedAt(context.getAnnouncedAt());
        response.setAppealDeadline(context.getAnnouncedAt().plusDays(ruleConfig.getAppealWindowDays()));

        List<AppealScoreCriterionResponse> criteria = new ArrayList<>();
        for (AppealSubmissionCriterion item : context.getCriteria()) {
            AppealScoreCriterionResponse criterion = new AppealScoreCriterionResponse();
            criterion.setCriterionId(item.getCriterionId());
            criterion.setCriterionName(item.getCriterionName());
            criterion.setScore(item.getScore());
            criterion.setComment(item.getComment());
            criteria.add(criterion);
        }
        response.setCriteria(criteria);
        return response;
    }

    @Transactional(readOnly = true)
    public List<AppealEligibilityResponse> getEligibleArticles(Integer currentUserId) {
        userReadService.requireActive(currentUserId, "REPORTER");
        List<AppealEligibilityResponse> result = new ArrayList<>();
        for (var article : articleReadService.getPublishedByAuthor(currentUserId)) {
            try {
                result.add(getEligibility(article.id(), currentUserId));
            } catch (BusinessException | com.swp391.kpi.exception.NotFoundException ex) {
                // Bai het han, da khieu nai hoac chua cong bo phieu cham thi khong hien trong danh sach.
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<AppealResponse> getMyAppeals(Integer currentUserId) {
        userReadService.requireActive(currentUserId, "REPORTER");
        List<AppealResponse> result = new ArrayList<>();
        List<Appeal> appeals = appealRepository.findAllByAppellantIdOrderBySubmittedAtDesc(currentUserId);
        for (Appeal appeal : appeals) {
            AppealSubmissionContext context = appealSubmissionDataProvider
                    .getSubmissionContext(appeal.getArticleId(), appeal.getScoringAttemptId());
            List<AppealCriterion> criteria = appealCriterionRepository
                    .findAllByAppealIdOrderById(appeal.getId());
            result.add(toResponse(appeal, criteria, context));
        }
        return result;
    }

    private void validateSubmission(
            AppealCreateRequest request,
            Integer currentUserId,
            AppealSubmissionContext context,
            AppealRuleConfig ruleConfig) {
        validateArticleAndWindow(currentUserId, context, ruleConfig);
        if (appealRepository.existsByArticleId(context.getArticleId())) {
            throw new BusinessException("Mỗi bài viết chỉ được khiếu nại một lần.");
        }
        validateCriteria(request.getCriteria(), context);
    }

    private void validateArticleAndWindow(
            Integer currentUserId,
            AppealSubmissionContext context,
            AppealRuleConfig ruleConfig) {
        if (!context.getMainAuthorId().equals(currentUserId)) {
            throw new BusinessException("Chỉ tác giả chính được gửi khiếu nại.");
        }
        if (!SCORE_PUBLISHED.equals(context.getArticleStatus())
                && !PUBLISHED.equals(context.getArticleStatus())) {
            throw new BusinessException("Bài viết chưa đủ điều kiện để khiếu nại.");
        }
        if (context.getAnnouncedAt() == null) {
            throw new BusinessException("Kết quả chấm chưa được công bố.");
        }
        if (context.getAnnouncedAt().isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException("Kết quả chấm chưa được công bố.");
        }
        LocalDateTime deadline = context.getAnnouncedAt().plusDays(ruleConfig.getAppealWindowDays());
        if (LocalDateTime.now(clock).isAfter(deadline)) {
            throw new BusinessException("Thời hạn khiếu nại đã kết thúc.");
        }
    }

    private void validateCriteria(
            List<AppealCriterionRequest> requestedCriteria,
            AppealSubmissionContext context) {
        if (requestedCriteria == null || requestedCriteria.isEmpty()) {
            throw new BusinessException("Phải chọn ít nhất một tiêu chí và nhập lý do cho từng tiêu chí.");
        }

        Set<Integer> availableIds = new HashSet<>();
        for (AppealSubmissionCriterion criterion : context.getCriteria()) {
            availableIds.add(criterion.getCriterionId());
        }

        Set<Integer> selectedIds = new HashSet<>();
        for (AppealCriterionRequest criterion : requestedCriteria) {
            if (criterion == null)
                throw new BusinessException("Tiêu chí khiếu nại không được để trống.");
            if (criterion.getCriterionId() == null || !availableIds.contains(criterion.getCriterionId())) {
                throw new BusinessException("Tiêu chí khiếu nại không thuộc phiếu chấm đã công bố.");
            }
            if (!selectedIds.add(criterion.getCriterionId())) {
                throw new BusinessException("Không được chọn trùng tiêu chí khiếu nại.");
            }
            if (criterion.getReason() == null || criterion.getReason().trim().isEmpty()) {
                throw new BusinessException("Phải chọn ít nhất một tiêu chí và nhập lý do cho từng tiêu chí.");
            }
        }
    }

    private List<AppealCriterion> saveCriteria(
            Appeal appeal,
            List<AppealCriterionRequest> requests) {
        List<AppealCriterion> criteria = new ArrayList<>();
        for (AppealCriterionRequest request : requests) {
            AppealCriterion criterion = new AppealCriterion();
            criterion.setAppeal(appeal);
            criterion.setCriterionId(request.getCriterionId());
            criterion.setReason(request.getReason().trim());
            criteria.add(criterion);
        }
        return appealCriterionRepository.saveAll(criteria);
    }

    private AppealResponse toResponse(
            Appeal appeal,
            List<AppealCriterion> criteria,
            AppealSubmissionContext context) {
        Map<Integer, String> criterionNames = new HashMap<>();
        for (AppealSubmissionCriterion criterion : context.getCriteria()) {
            criterionNames.put(criterion.getCriterionId(), criterion.getCriterionName());
        }

        List<AppealCriterionResponse> criterionResponses = new ArrayList<>();
        for (AppealCriterion criterion : criteria) {
            AppealCriterionResponse item = new AppealCriterionResponse();
            item.setCriterionId(criterion.getCriterionId());
            item.setCriterionName(criterionNames.get(criterion.getCriterionId()));
            item.setReason(criterion.getReason());
            criterionResponses.add(item);
        }

        AppealResponse response = new AppealResponse();
        response.setId(appeal.getId());
        response.setArticleId(appeal.getArticleId());
        response.setArticleTitle(context.getArticleTitle());
        response.setScoringAttemptId(appeal.getScoringAttemptId());
        response.setStatus(appeal.getStatus().name());
        response.setSubmittedAt(appeal.getSubmittedAt());
        response.setCriteria(criterionResponses);
        response.setResolvedAt(appeal.getResolvedAt());
        if (appeal.getStatus() == AppealStatus.RESOLVED)
            response.setEffectiveScore(effectiveScores.getEffectiveScore(appeal.getArticleId(), context.getTotalScore()));
        return response;
    }
}
