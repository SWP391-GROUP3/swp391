package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticlePublishedResponse;
import com.swp391.kpi.dto.ArticleRoyaltyAttachmentResponse;
import com.swp391.kpi.dto.ArticleRoyaltyAuthorResponse;
import com.swp391.kpi.dto.ArticleRoyaltyInfoResponse;
import com.swp391.kpi.dto.RoyaltyEstimateResponse;
import com.swp391.kpi.dto.RoyaltyGradingCreateRequest;
import com.swp391.kpi.dto.RoyaltyGradingMediaResponse;
import com.swp391.kpi.dto.RoyaltyGradingResponse;
import com.swp391.kpi.dto.RoyaltyPendingArticleResponse;
import com.swp391.kpi.dto.AppealEffectiveAttemptResponse;
import com.swp391.kpi.entity.Event;
import com.swp391.kpi.entity.MediaFactor;
import com.swp391.kpi.entity.RoyaltyGrading;
import com.swp391.kpi.entity.RoyaltyLine;
import com.swp391.kpi.entity.RoyaltyLineType;
import com.swp391.kpi.entity.RoyaltyMediaItem;
import com.swp391.kpi.entity.RoyaltyNature;
import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.entity.RoyaltyPeriodStatus;
import com.swp391.kpi.entity.RoyaltyRate;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.EventRepository;
import com.swp391.kpi.repository.MediaFactorRepository;
import com.swp391.kpi.repository.RoyaltyGradingRepository;
import com.swp391.kpi.repository.RoyaltyLineRepository;
import com.swp391.kpi.repository.RoyaltyMediaItemRepository;
import com.swp391.kpi.repository.RoyaltyPeriodRepository;
import com.swp391.kpi.repository.RoyaltyRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiem tra cham nhuan but (UC-46, 47, 48) va cham lai trong ky OPEN, khong can DB.
 * So lieu mau giong bai 5 trong data.sql: diem 82.50, Bai phan anh (he so chuan 5.00),
 * 2 tac gia 60/40, 3 anh (1 anh moi, 1 anh nguon ngoai, 1 anh trung anh da tra tien).
 */
@ExtendWith(MockitoExtension.class)
class RoyaltyGradingServiceTest {

    private static final Integer ARTICLE_ID = 5;
    private static final Integer SECRETARY_ID = 4;
    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 10, 12, 9, 0);

    @Mock private RoyaltyGradingRepository royaltyGradingRepository;
    @Mock private RoyaltyMediaItemRepository royaltyMediaItemRepository;
    @Mock private RoyaltyLineRepository royaltyLineRepository;
    @Mock private RoyaltyPeriodRepository royaltyPeriodRepository;
    @Mock private RoyaltyRateRepository royaltyRateRepository;
    @Mock private MediaFactorRepository mediaFactorRepository;
    @Mock private EventRepository eventRepository;
    @Mock private ArticlePublishedService articlePublishedService;
    @Mock private AppealEffectiveScoreService appealEffectiveScoreService;
    @Mock private GenreService genreService;
    @Mock private SystemParameterService systemParameterService;
    @Mock private AuditService auditService;

    @InjectMocks
    private RoyaltyGradingService royaltyGradingService;

    // ------------------------------------------------------------------ UC-46

    @Test
    void getPendingArticlesSkipsGradedArticlesAndWarnsNearDeadline() {
        LocalDate today = LocalDate.now();
        when(systemParameterService.getValuesByFlow("BF-04"))
                .thenReturn(Map.of("PAYMENT_DEADLINE_DAYS", "30", "PAYMENT_WARN_DAYS", "7"));
        when(articlePublishedService.getPublishedArticles()).thenReturn(List.of(
                publishedArticle(1, today.minusDays(40)),   // da cham -> bi loai
                publishedArticle(2, today.minusDays(25)),   // con 5 ngay -> canh bao
                publishedArticle(3, today.minusDays(2))));  // con 28 ngay -> chua canh bao
        when(royaltyGradingRepository.findGradedArticleIds()).thenReturn(List.of(1));

        List<RoyaltyPendingArticleResponse> result = royaltyGradingService.getPendingArticles();

        assertEquals(2, result.size());
        RoyaltyPendingArticleResponse nearDue = result.get(0);
        assertEquals(Integer.valueOf(2), nearDue.getArticleId());
        assertEquals(today.plusDays(5), nearDue.getPaymentDeadline());
        assertEquals(Long.valueOf(5), nearDue.getDaysLeft());
        assertTrue(nearDue.getNearDeadline());

        RoyaltyPendingArticleResponse fresh = result.get(1);
        assertEquals(Integer.valueOf(3), fresh.getArticleId());
        assertEquals(Long.valueOf(28), fresh.getDaysLeft());
        assertFalse(fresh.getNearDeadline());
    }

    @Test
    void getPendingArticlesMarksOverdueArticleWithNegativeDaysLeft() {
        LocalDate today = LocalDate.now();
        when(systemParameterService.getValuesByFlow("BF-04"))
                .thenReturn(Map.of("PAYMENT_DEADLINE_DAYS", "30", "PAYMENT_WARN_DAYS", "7"));
        when(articlePublishedService.getPublishedArticles()).thenReturn(List.of(publishedArticle(4, today.minusDays(33))));
        when(royaltyGradingRepository.findGradedArticleIds()).thenReturn(List.of());

        RoyaltyPendingArticleResponse overdue = royaltyGradingService.getPendingArticles().get(0);

        assertEquals(Long.valueOf(-3), overdue.getDaysLeft());   // qua han 3 ngay
        assertTrue(overdue.getNearDeadline());
    }

    @Test
    void getPendingArticlesFailsWhenDeadlineParameterIsMissing() {
        when(systemParameterService.getValuesByFlow("BF-04")).thenReturn(Map.of("PAYMENT_WARN_DAYS", "7"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyGradingService.getPendingArticles());

        assertTrue(error.getMessage().contains("PAYMENT_DEADLINE_DAYS"));
    }

    // ------------------------------------------------------------- UC-47, 48

    @Test
    void previewGradingCalculatesArticleAndMediaAmounts() {
        stubArticle(royaltyArticle(null, null));

        RoyaltyGradingResponse preview = royaltyGradingService.previewGrading(ARTICLE_ID);

        // tinh chat lay theo PV khai (ORIGINAL), Thu ky khong chon lai
        assertEquals(RoyaltyNature.ORIGINAL, preview.getNature());
        // 5.00 x 82.50 / 100 = 4.125 -> 4.13; x 100% x 100% = 4.13
        assertMoney("4.13", preview.getSuggestedFactor());
        assertMoney("4.13", preview.getFinalFactor());
        // 4.13 x 234.000 = 966.420 -> lam tron xuong 966.000
        assertMoney("966000", preview.getArticleAmount());
        // dong tac gia 40% = 386.400 -> 386.000; tac gia chinh nhan phan con lai 580.000
        assertMoney("580000", preview.getAuthors().get(0).getAmount());
        assertMoney("386000", preview.getAuthors().get(1).getAmount());
        // chi anh 2 duoc tinh: 1.00 x 234.000
        List<RoyaltyGradingMediaResponse> media = preview.getMediaItems();
        assertTrue(media.get(0).getPayable());
        assertMoney("234000", media.get(0).getAmount());
        assertFalse(media.get(1).getPayable());       // nguon ngoai
        assertTrue(media.get(1).getNoRoyaltyReason().contains("nguồn ngoài"));
        assertFalse(media.get(2).getPayable());       // trung anh da tra nhuan but
        assertMoney("234000", preview.getMediaAmount());
        assertMoney("1200000", preview.getTotalAmount());
        assertNull(preview.getGradingId());            // xem truoc: chua luu
        assertEquals(Integer.valueOf(1), preview.getPeriodId());
    }

    @Test
    void previewGradingUsesNatureDeclaredByReporter() {
        ArticleRoyaltyInfoResponse translated = royaltyArticle(null, null);
        translated.setDeclaredNature("TRANSLATION");   // PV khai bai dich, khoa luc nop (BR-IN-03)
        stubArticle(translated);

        RoyaltyGradingResponse preview = royaltyGradingService.previewGrading(ARTICLE_ID);

        // 4.13 x 70% = 2.891 -> 2.89; 2.89 x 234.000 = 676.260 -> 676.000
        assertEquals(RoyaltyNature.TRANSLATION, preview.getNature());
        assertMoney("70", preview.getNatureRate());
        assertMoney("2.89", preview.getFinalFactor());
        assertMoney("676000", preview.getArticleAmount());
    }

    @Test
    void previewGradingUsesLockedSaturationRateWhenScoreBelowExemptScore() {
        Event storm = new Event();
        storm.setId(1);
        storm.setExemptScore(new BigDecimal("90"));
        stubArticle(royaltyArticle(1, new BigDecimal("80.00")));
        when(eventRepository.findById(1)).thenReturn(Optional.of(storm));

        RoyaltyGradingResponse preview = royaltyGradingService.previewGrading(ARTICLE_ID);

        // diem 82.50 < 90 nen bi giam con 80%: 4.13 x 80% = 3.304 -> 3.30; x 234.000 = 772.200 -> 772.000
        assertMoney("80.00", preview.getSaturationRate());
        assertMoney("3.30", preview.getFinalFactor());
        assertMoney("772000", preview.getArticleAmount());
    }

    @Test
    void previewGradingFailsWhenNoUnitPriceOnPublishDate() {
        stubArticle(royaltyArticle(null, null));
        when(royaltyRateRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(PUBLISHED_AT.toLocalDate()))
                .thenReturn(Optional.empty());

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyGradingService.previewGrading(ARTICLE_ID));

        assertTrue(error.getMessage().contains("12/10/2026"));
    }

    // Q-A2: khieu nai da RESOLVED -> phieu nhuan but dung diem Truong ban chot va lan cham S1/S2 (BR-QC-10)
    @Test
    void previewGradingUsesScoreConfirmedAfterAppeal() {
        stubArticle(royaltyArticle(null, null));
        when(appealEffectiveScoreService.getEffectiveAttempt(ARTICLE_ID))
                .thenReturn(new AppealEffectiveAttemptResponse(9, new BigDecimal("90.00"), true));

        RoyaltyGradingResponse preview = royaltyGradingService.previewGrading(ARTICLE_ID);

        assertEquals(Integer.valueOf(9), preview.getScoringAttemptId());
        assertMoney("90", preview.getScoreUsed());
        // 5.00 x 90 / 100 = 4.50
        assertMoney("4.50", preview.getSuggestedFactor());
    }

    @Test
    void previewGradingFailsWhenScoreNotPublished() {
        stubArticle(royaltyArticle(null, null));
        when(appealEffectiveScoreService.getEffectiveAttempt(ARTICLE_ID)).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyGradingService.previewGrading(ARTICLE_ID));

        assertTrue(error.getMessage().contains("chưa có điểm"));
    }

    @Test
    void completeGradingSavesGradingMediaItemAndLinesOfEachPerson() {
        stubArticle(royaltyArticle(null, null));
        when(royaltyGradingRepository.save(any(RoyaltyGrading.class))).thenAnswer(call -> {
            RoyaltyGrading grading = call.getArgument(0);
            grading.setId(10);
            return grading;
        });
        when(royaltyMediaItemRepository.save(any(RoyaltyMediaItem.class))).thenAnswer(call -> {
            RoyaltyMediaItem item = call.getArgument(0);
            item.setId(20);
            return item;
        });

        RoyaltyGradingResponse result = royaltyGradingService.completeGrading(createRequest(), SECRETARY_ID);

        // Phieu cham: phien ban 1, tier_rate = diem, applied = suggested (BR-RY-01 moi)
        ArgumentCaptor<RoyaltyGrading> gradingCaptor = ArgumentCaptor.forClass(RoyaltyGrading.class);
        verify(royaltyGradingRepository).save(gradingCaptor.capture());
        RoyaltyGrading grading = gradingCaptor.getValue();
        assertEquals(Integer.valueOf(1), grading.getVersionNo());
        assertEquals(SECRETARY_ID, grading.getGradedBy());
        assertMoney("82.50", grading.getTierRate());
        assertEquals(grading.getSuggestedFactor(), grading.getAppliedFactor());
        assertMoney("966000", grading.getAmount());
        assertEquals(Integer.valueOf(1), grading.getPeriod().getId());

        // 3 dong tien: 2 dong ARTICLE (60/40) + 1 dong MEDIA cho tac gia anh
        ArgumentCaptor<RoyaltyLine> lineCaptor = ArgumentCaptor.forClass(RoyaltyLine.class);
        verify(royaltyLineRepository, times(3)).save(lineCaptor.capture());
        List<RoyaltyLine> lines = lineCaptor.getAllValues();
        assertEquals(RoyaltyLineType.ARTICLE, lines.get(0).getLineType());
        assertEquals(Integer.valueOf(1), lines.get(0).getUserId());
        assertMoney("580000", lines.get(0).getAmount());
        assertEquals(RoyaltyLineType.ARTICLE, lines.get(1).getLineType());
        assertMoney("386000", lines.get(1).getAmount());
        assertEquals(RoyaltyLineType.MEDIA, lines.get(2).getLineType());
        assertEquals(Integer.valueOf(2), lines.get(2).getUserId());
        assertEquals(Integer.valueOf(20), lines.get(2).getMediaItem().getId());

        verify(royaltyMediaItemRepository, times(1)).save(any(RoyaltyMediaItem.class));
        verify(auditService).log(eq(SECRETARY_ID), eq("CREATE"), eq("royalty_gradings"), eq(10), isNull(), anyString());
        assertEquals(Integer.valueOf(10), result.getGradingId());
    }

    // ------------------------------------------- cham lai (nhom chot 27/10)

    @Test
    void completeGradingRegradesInSameOpenPeriodWithNewVersionAndDeletesOldLines() {
        stubArticle(royaltyArticle(null, null));
        // Phien ban 1 thuoc ky 9 dang OPEN (Ke toan vua tra ve), khac ky OPEN cu nhat ma stub tra (id 1)
        RoyaltyGrading version1 = previousGrading(7, period(3, 9, RoyaltyPeriodStatus.OPEN));
        when(royaltyGradingRepository.findFirstByArticleIdOrderByVersionNoDesc(ARTICLE_ID)).thenReturn(Optional.of(version1));
        when(royaltyGradingRepository.save(any(RoyaltyGrading.class))).thenAnswer(call -> {
            RoyaltyGrading grading = call.getArgument(0);
            grading.setId(11);
            return grading;
        });
        when(royaltyMediaItemRepository.save(any(RoyaltyMediaItem.class))).thenAnswer(call -> call.getArgument(0));
        RoyaltyGradingCreateRequest request = createRequest();
        request.setRecalcReason("  Sửa tỷ lệ đồng tác giả  ");

        royaltyGradingService.completeGrading(request, SECRETARY_ID);

        ArgumentCaptor<RoyaltyGrading> captor = ArgumentCaptor.forClass(RoyaltyGrading.class);
        verify(royaltyGradingRepository).save(captor.capture());
        RoyaltyGrading version2 = captor.getValue();
        assertEquals(Integer.valueOf(2), version2.getVersionNo());
        assertEquals("Sửa tỷ lệ đồng tác giả", version2.getRecalcReason());
        assertEquals(Integer.valueOf(3), version2.getPeriod().getId());   // cung ky voi phien ban cu
        verify(royaltyLineRepository).deleteByGradingId(7);
        verify(royaltyLineRepository, times(3)).save(any(RoyaltyLine.class));
        verify(auditService).log(eq(SECRETARY_ID), eq("RECALCULATE"), eq("royalty_gradings"), eq(11), isNull(), anyString());
    }

    @Test
    void completeGradingFailsWhenRegradeHasNoReason() {
        stubArticle(royaltyArticle(null, null));
        RoyaltyGrading version1 = previousGrading(7, period(1, 10, RoyaltyPeriodStatus.OPEN));
        when(royaltyGradingRepository.findFirstByArticleIdOrderByVersionNoDesc(ARTICLE_ID)).thenReturn(Optional.of(version1));

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyGradingService.completeGrading(createRequest(), SECRETARY_ID));

        assertTrue(error.getMessage().contains("phải nhập lý do"));
        verify(royaltyLineRepository, never()).deleteByGradingId(any());
        verify(royaltyGradingRepository, never()).save(any(RoyaltyGrading.class));
    }

    @Test
    void completeGradingFailsWhenPreviousVersionPeriodIsNotOpen() {
        stubArticle(royaltyArticle(null, null));
        // Ky 9 da khoa: khong sua duoc (BR-RY-06), chenh lech se la dong dieu chinh khi lam BF-03
        RoyaltyGrading version1 = previousGrading(7, period(3, 9, RoyaltyPeriodStatus.LOCKED));
        when(royaltyGradingRepository.findFirstByArticleIdOrderByVersionNoDesc(ARTICLE_ID)).thenReturn(Optional.of(version1));
        RoyaltyGradingCreateRequest request = createRequest();
        request.setRecalcReason("Sửa tỷ lệ");

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyGradingService.completeGrading(request, SECRETARY_ID));

        assertTrue(error.getMessage().contains("không chấm lại được"));
        verify(royaltyLineRepository, never()).deleteByGradingId(any());
        verify(royaltyGradingRepository, never()).save(any(RoyaltyGrading.class));
    }

    @Test
    void completeGradingFailsWhenNoOpenPeriod() {
        stubArticle(royaltyArticle(null, null));
        when(royaltyPeriodRepository.findFirstByStatusOrderByPeriodYearAscPeriodMonthAsc(RoyaltyPeriodStatus.OPEN))
                .thenReturn(Optional.empty());

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyGradingService.completeGrading(createRequest(), SECRETARY_ID));

        assertTrue(error.getMessage().contains("kỳ nhuận bút đang mở"));
        verify(royaltyGradingRepository, never()).save(any(RoyaltyGrading.class));
    }

    // ----------------------------------------------------------- du lieu gia

    // Bai da dang gia cho UC-46, chi can id va ngay dang (dang luc 9 gio sang)
    // ------------------------------------------------------------------ UC-09

    @Test
    void estimateRoyaltyCalculatesArticleAmountAtThreeScoreLevels() {
        stubEstimate();

        RoyaltyEstimateResponse result = royaltyGradingService.estimateRoyalty(3, "ORIGINAL", null, null);

        assertMoney("234000", result.getUnitPrice());
        assertEquals(3, result.getItems().size());
        // 5.00 x 100 / 100 = 5.00 -> 5.00 x 234.000 = 1.170.000
        assertMoney("100", result.getItems().get(0).getScore());
        assertMoney("5.00", result.getItems().get(0).getFinalFactor());
        assertMoney("1170000", result.getItems().get(0).getAmount());
        // 5.00 x 80 / 100 = 4.00 -> 936.000
        assertMoney("936000", result.getItems().get(1).getAmount());
        // 5.00 x 50 / 100 = 2.50 -> 585.000
        assertMoney("585000", result.getItems().get(2).getAmount());
    }

    @Test
    void estimateRoyaltyAppliesSaturationOnlyBelowExemptScore() {
        stubEstimate();
        Event storm = new Event();
        storm.setId(1);
        storm.setExemptScore(new BigDecimal("90"));
        when(eventRepository.findById(1)).thenReturn(Optional.of(storm));

        RoyaltyEstimateResponse result = royaltyGradingService.estimateRoyalty(3, "ORIGINAL", 1, new BigDecimal("80"));

        // diem 100 >= 90 -> mien giam: 5.00 -> 1.170.000
        assertMoney("100", result.getItems().get(0).getSaturationRate());
        assertMoney("1170000", result.getItems().get(0).getAmount());
        // diem 80 < 90 -> 4.00 x 80% = 3.20 -> 748.800 -> lam tron xuong 748.000
        assertMoney("80", result.getItems().get(1).getSaturationRate());
        assertMoney("748000", result.getItems().get(1).getAmount());
    }

    @Test
    void estimateRoyaltyFailsWhenNatureIsInvalid() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> royaltyGradingService.estimateRoyalty(3, "PHONG_SU", null, null));

        assertEquals("Tính chất bài không hợp lệ: PHONG_SU", ex.getMessage());
    }

    @Test
    void estimateRoyaltyFailsWhenNoUnitPriceToday() {
        when(genreService.getBaseFactor(3)).thenReturn(new BigDecimal("5.00"));
        when(royaltyRateRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate.now()))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> royaltyGradingService.estimateRoyalty(3, "ORIGINAL", null, null));

        assertTrue(ex.getMessage().startsWith("Chưa có đơn giá nhuận bút hiệu lực tại ngày"));
    }

    // Du lieu chuan cho uoc tinh: Bai phan anh (he so chuan 5.00), don gia 234.000 hieu luc hom nay
    private void stubEstimate() {
        RoyaltyRate rate = new RoyaltyRate();
        rate.setId(1);
        rate.setAmount(new BigDecimal("234000"));
        rate.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        when(genreService.getBaseFactor(3)).thenReturn(new BigDecimal("5.00"));
        when(royaltyRateRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate.now()))
                .thenReturn(Optional.of(rate));
        when(systemParameterService.getValuesByFlow("BF-04")).thenReturn(royaltyParams());
    }

    private static ArticlePublishedResponse publishedArticle(Integer id, LocalDate publishedDate) {
        ArticlePublishedResponse article = new ArticlePublishedResponse();
        article.setId(id);
        article.setTitle("Bai " + id);
        article.setPublishedAt(publishedDate.atTime(9, 0));
        return article;
    }

    // Bai 5 de cham nhuan but; eventId/saturationRate = null nghia la khong thuoc su kien
    private static ArticleRoyaltyInfoResponse royaltyArticle(Integer eventId, BigDecimal saturationRate) {
        ArticleRoyaltyInfoResponse article = new ArticleRoyaltyInfoResponse();
        article.setId(ARTICLE_ID);
        article.setTitle("Duong dan sinh xuong cap");
        article.setCategoryId(3);
        article.setDeclaredNature("ORIGINAL");
        article.setEventId(eventId);
        article.setSaturationRate(saturationRate);
        article.setPublishedAt(PUBLISHED_AT);
        article.setAuthors(List.of(author(1, "60.00", true), author(2, "40.00", false)));
        article.setAttachments(List.of(
                attachment(2, 2, "PHOTO", null, "NEW", null),                      // duoc tinh tien
                attachment(3, 1, "GRAPHIC", "So Giao thong van tai", "NEW", null), // nguon ngoai
                attachment(4, 1, "PHOTO", null, "DUPLICATE", 1)));                 // trung anh 1 da tra tien
        return article;
    }

    // Cac service/repository khac tra du lieu chuan. Dung lenient vi co test dung som (bao loi)
    // nen khong goi het, Mockito che do strict se bao "stub thua".
    private void stubArticle(ArticleRoyaltyInfoResponse article) {
        RoyaltyRate rate = new RoyaltyRate();
        rate.setId(1);
        rate.setAmount(new BigDecimal("234000"));
        rate.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        RoyaltyPeriod october = new RoyaltyPeriod();
        october.setId(1);
        october.setPeriodYear((short) 2026);
        october.setPeriodMonth((byte) 10);
        october.setStatus(RoyaltyPeriodStatus.OPEN);
        MediaFactor photo = new MediaFactor();
        photo.setMediaKind("PHOTO");
        photo.setFactor(new BigDecimal("1.00"));

        lenient().when(articlePublishedService.getArticleForRoyalty(ARTICLE_ID)).thenReturn(article);
        lenient().when(appealEffectiveScoreService.getEffectiveAttempt(ARTICLE_ID)).thenReturn(
                new AppealEffectiveAttemptResponse(3, new BigDecimal("82.50"), false));
        lenient().when(genreService.getBaseFactor(3)).thenReturn(new BigDecimal("5.00"));
        lenient().when(systemParameterService.getValuesByFlow("BF-04")).thenReturn(royaltyParams());
        lenient().when(royaltyRateRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                PUBLISHED_AT.toLocalDate())).thenReturn(Optional.of(rate));
        lenient().when(royaltyPeriodRepository.findFirstByStatusOrderByPeriodYearAscPeriodMonthAsc(
                RoyaltyPeriodStatus.OPEN)).thenReturn(Optional.of(october));
        lenient().when(mediaFactorRepository.findByMediaKind("PHOTO")).thenReturn(Optional.of(photo));
        lenient().when(royaltyMediaItemRepository.existsByAttachmentId(1)).thenReturn(true);  // anh 1 da tra tien
    }

    // Tham so BF-04 giong data.sql
    private static Map<String, String> royaltyParams() {
        return Map.of(
                "NATURE_RATE_ORIGINAL", "100",
                "NATURE_RATE_TRANSLATION", "70",
                "NATURE_RATE_COMPILATION", "50",
                "NATURE_RATE_REPOST", "30",
                "MIN_FINAL_FACTOR", "1",
                "ROUNDING_UNIT", "1000",
                "UNIT_PRICE_PERCENT", "10");
    }

    private static ArticleRoyaltyAuthorResponse author(Integer userId, String share, boolean mainAuthor) {
        ArticleRoyaltyAuthorResponse author = new ArticleRoyaltyAuthorResponse();
        author.setUserId(userId);
        author.setFullName("Tac gia " + userId);
        author.setSharePercent(new BigDecimal(share));
        author.setMainAuthor(mainAuthor);
        return author;
    }

    private static ArticleRoyaltyAttachmentResponse attachment(Integer id, Integer authorId, String mediaKind,
                                                               String externalSource, String hashResult,
                                                               Integer matchedAttachmentId) {
        ArticleRoyaltyAttachmentResponse attachment = new ArticleRoyaltyAttachmentResponse();
        attachment.setId(id);
        attachment.setAuthorId(authorId);
        attachment.setMediaType("IMAGE");
        attachment.setMediaKind(mediaKind);
        attachment.setExternalSource(externalSource);
        attachment.setHashResult(hashResult);
        attachment.setMatchedAttachmentId(matchedAttachmentId);
        return attachment;
    }

    private static RoyaltyPeriod period(Integer id, int month, RoyaltyPeriodStatus status) {
        RoyaltyPeriod period = new RoyaltyPeriod();
        period.setId(id);
        period.setPeriodYear((short) 2026);
        period.setPeriodMonth((byte) month);
        period.setStatus(status);
        return period;
    }

    // Phien ban 1 da luu cua bai 5, thuoc ky truyen vao
    private static RoyaltyGrading previousGrading(Integer id, RoyaltyPeriod period) {
        RoyaltyGrading grading = new RoyaltyGrading();
        grading.setId(id);
        grading.setArticleId(ARTICLE_ID);
        grading.setVersionNo(1);
        grading.setPeriod(period);
        return grading;
    }

    private static RoyaltyGradingCreateRequest createRequest() {
        RoyaltyGradingCreateRequest request = new RoyaltyGradingCreateRequest();
        request.setArticleId(ARTICLE_ID);
        return request;
    }

    // So sanh BigDecimal theo gia tri (4.13 == 4.130), khong theo so chu so thap phan
    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "mong doi " + expected + " nhung la " + actual);
    }
}
