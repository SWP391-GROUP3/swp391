package com.swp391.kpi.service;

import com.swp391.kpi.dto.AppealCreateRequest;
import com.swp391.kpi.dto.AppealCriterionRequest;
import com.swp391.kpi.dto.AppealResponse;
import com.swp391.kpi.entity.Appeal;
import com.swp391.kpi.entity.AppealRuleConfig;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.AppealCriterionRepository;
import com.swp391.kpi.repository.AppealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppealServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-10-14T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock
    private AppealRepository appealRepository;

    @Mock
    private AppealCriterionRepository appealCriterionRepository;

    @Mock
    private AppealRuleConfigService appealRuleConfigService;

    @Mock
    private AppealSubmissionDataProvider appealSubmissionDataProvider;

    @Mock
    private AuditService auditService;

    @Mock private UserReadService userReadService;
    @Mock private NotificationService notificationService;
    @Mock private ArticleReadService articleReadService;

    private AppealService appealService;

    @BeforeEach
    void setUp() {
        appealService = new AppealService(
                appealRepository,
                appealCriterionRepository,
                appealRuleConfigService,
                appealSubmissionDataProvider,
                auditService,
                userReadService,
                notificationService,
                articleReadService,
                FIXED_CLOCK);
    }

    @Test
    void submitAppealSavesSelectedCriteriaForMainAuthor() {
        when(articleReadService.getArticle(9)).thenReturn(
                new com.swp391.kpi.dto.ArticleReadResponse(9, 1, 1, "Bai test", "SCORE_PUBLISHED"));
        when(appealSubmissionDataProvider.getSubmissionContext(9)).thenReturn(validContext());
        when(appealRuleConfigService.getActiveConfigEntity()).thenReturn(activeRule());
        when(appealRepository.existsByArticleId(9)).thenReturn(false);
        when(appealRepository.save(any(Appeal.class))).thenAnswer(invocation -> {
            Appeal appeal = invocation.getArgument(0);
            appeal.setId(11);
            return appeal;
        });
        when(appealCriterionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppealResponse response = appealService.submitAppeal(validRequest(), 1);

        assertEquals(Integer.valueOf(11), response.getId());
        assertEquals("SUBMITTED", response.getStatus());
        assertEquals(2, response.getCriteria().size());
        verify(appealCriterionRepository).saveAll(any());
        verify(auditService).log(
                1,
                "CREATE",
                "appeals",
                11,
                null,
                "{\"articleId\":9,\"status\":\"SUBMITTED\"}"
        );
    }

    @Test
    void submitAppealRejectsUserWhoIsNotMainAuthor() {
        when(appealSubmissionDataProvider.getSubmissionContext(9)).thenReturn(validContext());
        when(appealRuleConfigService.getActiveConfigEntity()).thenReturn(activeRule());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.submitAppeal(validRequest(), 2));

        assertEquals("Chỉ tác giả chính được gửi khiếu nại.", exception.getMessage());
        verify(appealRepository, never()).save(any());
    }

    @Test
    void submitAppealAcceptsAtExactDeadline() {
        when(articleReadService.getArticle(9)).thenReturn(
                new com.swp391.kpi.dto.ArticleReadResponse(9, 1, 1, "Bai test", "SCORE_PUBLISHED"));
        AppealSubmissionContext context = validContext();
        context.setAnnouncedAt(LocalDateTime.of(2026, 10, 7, 10, 0));
        when(appealSubmissionDataProvider.getSubmissionContext(9)).thenReturn(context);
        when(appealRuleConfigService.getActiveConfigEntity()).thenReturn(activeRule());
        when(appealRepository.existsByArticleId(9)).thenReturn(false);
        when(appealRepository.save(any(Appeal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appealService.submitAppeal(validRequest(), 1);

        verify(appealRepository).save(any(Appeal.class));
    }

    @Test
    void submitAppealRejectsAfterDeadline() {
        AppealSubmissionContext context = validContext();
        context.setAnnouncedAt(LocalDateTime.of(2026, 10, 7, 9, 59));
        when(appealSubmissionDataProvider.getSubmissionContext(9)).thenReturn(context);
        when(appealRuleConfigService.getActiveConfigEntity()).thenReturn(activeRule());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.submitAppeal(validRequest(), 1));

        assertEquals("Thời hạn khiếu nại đã kết thúc.", exception.getMessage());
    }

    @Test
    void submitAppealRejectsSecondAppealForArticle() {
        when(appealSubmissionDataProvider.getSubmissionContext(9)).thenReturn(validContext());
        when(appealRuleConfigService.getActiveConfigEntity()).thenReturn(activeRule());
        when(appealRepository.existsByArticleId(9)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.submitAppeal(validRequest(), 1));

        assertEquals("Mỗi bài viết chỉ được khiếu nại một lần.", exception.getMessage());
    }

    @Test
    void submitAppealRejectsCriterionOutsidePublishedScorecard() {
        when(appealSubmissionDataProvider.getSubmissionContext(9)).thenReturn(validContext());
        when(appealRuleConfigService.getActiveConfigEntity()).thenReturn(activeRule());
        when(appealRepository.existsByArticleId(9)).thenReturn(false);
        AppealCreateRequest request = validRequest();
        request.setCriteria(List.of(criterion(99, "Không đồng ý")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> appealService.submitAppeal(request, 1));

        assertEquals("Tiêu chí khiếu nại không thuộc phiếu chấm đã công bố.", exception.getMessage());
        verify(appealRepository, never()).save(any());
    }

    private AppealCreateRequest validRequest() {
        AppealCreateRequest request = new AppealCreateRequest();
        request.setArticleId(9);
        request.setCriteria(List.of(
                criterion(1, "Điểm chưa phản ánh đúng tài liệu đã cung cấp."),
                criterion(2, "Bài được thực hiện trong điều kiện tác nghiệp khó.")));
        return request;
    }

    private AppealCriterionRequest criterion(Integer criterionId, String reason) {
        AppealCriterionRequest request = new AppealCriterionRequest();
        request.setCriterionId(criterionId);
        request.setReason(reason);
        return request;
    }

    private AppealSubmissionContext validContext() {
        AppealSubmissionContext context = new AppealSubmissionContext();
        context.setArticleId(9);
        context.setArticleTitle("Phỏng vấn chuyên gia về chuyển đổi số y tế");
        context.setArticleStatus("SCORE_PUBLISHED");
        context.setMainAuthorId(1);
        context.setScoringAttemptId(21);
        context.setAnnouncedAt(LocalDateTime.of(2026, 10, 10, 8, 0));
        context.setTotalScore(new BigDecimal("72.50"));
        context.setCriteria(List.of(
                new AppealSubmissionCriterion(1, "Tính phát hiện", new BigDecimal("70.00"), ""),
                new AppealSubmissionCriterion(2, "Độ khó tác nghiệp", new BigDecimal("75.00"), "")));
        return context;
    }

    private AppealRuleConfig activeRule() {
        AppealRuleConfig config = new AppealRuleConfig();
        config.setId(3);
        config.setAppealWindowDays(7);
        config.setMaxAppealsPerArticle(1);
        config.setVarianceThreshold(new BigDecimal("5.00"));
        config.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        return config;
    }
}
