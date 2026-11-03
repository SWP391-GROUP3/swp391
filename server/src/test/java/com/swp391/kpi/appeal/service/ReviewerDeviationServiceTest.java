package com.swp391.kpi.appeal.service;

import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.appeal.dto.ReviewerDeviationResponse;
import com.swp391.kpi.appeal.entity.ReviewerDeviationIndex;
import com.swp391.kpi.appeal.repository.ReviewerDeviationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewerDeviationServiceTest {

    @Mock
    private ReviewerDeviationRepository reviewerDeviationRepository;

    @Mock
    private ReviewerDirectoryProvider reviewerDirectoryProvider;

    @Mock private UserReadService userReadService;
    @Mock private ReviewerDeviationRefreshService refreshService;

    private ReviewerDeviationService reviewerDeviationService;

    @BeforeEach
    void setUp() {
        reviewerDeviationService = new ReviewerDeviationService(
                reviewerDeviationRepository,
                reviewerDirectoryProvider, userReadService, refreshService
        );
        org.mockito.Mockito.lenient().when(userReadService.requireActive(8, "ADMIN", "HEAD_OF_DEPARTMENT")).thenReturn(
                new com.swp391.kpi.admin.dto.UserAccessResponse(8, "Admin", "ADMIN", null, null, "ACTIVE"));
    }

    @Test
    void getReportAddsReviewerInformationAndFiltersBySection() {
        ReviewerDeviationIndex first = index(1, 3, new BigDecimal("4.25"));
        ReviewerDeviationIndex second = index(2, 9, new BigDecimal("7.50"));
        when(reviewerDeviationRepository.findAllByOrderByPeriodDescEditorIdAsc())
                .thenReturn(List.of(first, second));
        when(reviewerDirectoryProvider.findEditor(3))
                .thenReturn(Optional.of(new ReviewerDirectoryEntry(3, "Lê Hoàng Cường", 1, "Ban Thời sự")));
        when(reviewerDirectoryProvider.findEditor(9))
                .thenReturn(Optional.of(new ReviewerDirectoryEntry(9, "Ngô Thu Giang", 2, "Ban Kinh tế")));

        List<ReviewerDeviationResponse> result = reviewerDeviationService.getReport(null, null, 1, 8);

        assertEquals(1, result.size());
        assertEquals("Lê Hoàng Cường", result.get(0).getEditorName());
        assertEquals("Ban Thời sự", result.get(0).getSectionName());
        assertEquals(new BigDecimal("4.25"), result.get(0).getAvgDeviation());
        // Mo bao cao chi doc: khong tinh lai, khong khoa he thong.
        org.mockito.Mockito.verifyNoInteractions(refreshService);
    }

    @Test
    void recalculateRefreshesOnlyTheRequestedMonth() {
        LocalDate month = LocalDate.of(2026, 9, 1);
        reviewerDeviationService.recalculate(month, 8);
        org.mockito.Mockito.verify(refreshService).refresh(month, 8);
        org.mockito.Mockito.verify(refreshService, org.mockito.Mockito.never()).getPeriods();
    }

    @Test
    void recalculateWithoutMonthRefreshesEveryKnownMonth() {
        when(refreshService.getPeriods()).thenReturn(java.util.Set.of(LocalDate.of(2026, 9, 1)));
        reviewerDeviationService.recalculate(null, 8);
        org.mockito.Mockito.verify(refreshService).refresh(LocalDate.of(2026, 9, 1), 8);
    }

    @Test
    void getReportReturnsEmptyListWhenPeriodHasNoData() {
        ReviewerDeviationIndex index = index(1, 3, new BigDecimal("4.25"));
        when(reviewerDeviationRepository.findAllByOrderByPeriodDescEditorIdAsc())
                .thenReturn(List.of(index));
        List<ReviewerDeviationResponse> result = reviewerDeviationService.getReport(
                LocalDate.of(2026, 10, 1),
                null,
                null, 8
        );

        assertEquals(0, result.size());
    }

    private ReviewerDeviationIndex index(Integer id, Integer editorId, BigDecimal avgDeviation) {
        ReviewerDeviationIndex index = new ReviewerDeviationIndex();
        index.setId(id);
        index.setEditorId(editorId);
        index.setPeriod(LocalDate.of(2026, 9, 1));
        index.setAvgDeviation(avgDeviation);
        index.setAppealCount(1);
        index.setOverturnRate(new BigDecimal("50.00"));
        index.setWorkloadCount(4);
        return index;
    }
}
