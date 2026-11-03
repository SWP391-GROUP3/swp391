package com.swp391.kpi.kpi.service;

import com.swp391.kpi.admin.entity.Role;
import com.swp391.kpi.admin.entity.RoleCode;
import com.swp391.kpi.admin.entity.User;
import com.swp391.kpi.admin.repository.UserRepository;
import com.swp391.kpi.article.repository.ArticleAuthorRepository;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.common.service.AuditService;
import com.swp391.kpi.common.service.NotificationService;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.kpi.dto.KpiFinalizeResponse;
import com.swp391.kpi.kpi.entity.KpiSnapshot;
import com.swp391.kpi.kpi.entity.KpiSnapshotStatus;
import com.swp391.kpi.kpi.entity.KpiTarget;
import com.swp391.kpi.kpi.repository.KpiSnapshotRepository;
import com.swp391.kpi.kpi.repository.KpiTargetRepository;
import com.swp391.kpi.kpi.repository.ReaderInteractionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KpiAggregationService - Kiểm thử chốt kỳ KPI tháng (UC-66, BR-KPI-05, BR-KPI-06)")
class KpiFinalizationServiceTest {

    @Mock
    private KpiSnapshotRepository kpiSnapshotRepository;

    @Mock
    private KpiTargetRepository kpiTargetRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleAuthorRepository articleAuthorRepository;

    @Mock
    private ReaderInteractionRepository readerInteractionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private KpiAggregationService kpiAggregationService;

    private final Short year = 2026;
    private final Byte month = 9;
    private final Integer headUserId = 4;

    @Test
    @DisplayName("Chặn chốt kỳ khi kỳ KPI đã ở trạng thái FINALIZED bất biến (E1, BR-KPI-06)")
    void testFinalizeWhenAlreadyFinalizedThrowsBusinessException() {
        KpiSnapshot finalizedSnapshot = new KpiSnapshot();
        finalizedSnapshot.setStatus(KpiSnapshotStatus.FINALIZED);

        when(kpiSnapshotRepository.findByPeriodYearAndPeriodMonth(year, month))
                .thenReturn(Collections.singletonList(finalizedSnapshot));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kpiAggregationService.finalizeMonthlyKpi(year, month, headUserId, Collections.emptyMap())
        );

        assertTrue(ex.getMessage().contains("đã được khóa và chốt kết quả bất biến"));
        verify(kpiSnapshotRepository, never()).save(any());
        verify(notificationService, never()).notifyUser(any(), any(), any());
    }

    @Test
    @DisplayName("Chốt kỳ thành công: chuyển sang FINALIZED, gán ghi chú, gửi thông báo và ghi audit")
    void testFinalizeMonthlyKpiSuccess() {
        // Chưa có bản ghi FINALIZED
        when(kpiSnapshotRepository.findByPeriodYearAndPeriodMonth(year, month))
                .thenReturn(Collections.emptyList());

        User reporter = new User();
        reporter.setId(10);
        Role reporterRole = new Role();
        reporterRole.setCode(RoleCode.REPORTER);
        reporter.setRole(reporterRole);
        when(userRepository.findAll()).thenReturn(Collections.singletonList(reporter));

        KpiTarget target = new KpiTarget();
        target.setReporterId(10);
        target.setMinPublished(5);
        target.setMinAvgScore(new BigDecimal("70.00"));
        when(kpiTargetRepository.findByTargetYearAndTargetMonth(year, month))
                .thenReturn(Collections.singletonList(target));

        when(articleRepository.findAll()).thenReturn(Collections.emptyList());

        when(kpiSnapshotRepository.findByReporterIdAndPeriodYearAndPeriodMonth(10, year, month))
                .thenReturn(Optional.empty());

        when(kpiSnapshotRepository.save(any(KpiSnapshot.class))).thenAnswer(invocation -> {
            KpiSnapshot snapshot = invocation.getArgument(0);
            snapshot.setId(100);
            return snapshot;
        });

        Map<Integer, String> notes = new HashMap<>();
        notes.put(10, "Cần cải thiện năng suất bài viết");

        KpiFinalizeResponse res = kpiAggregationService.finalizeMonthlyKpi(year, month, headUserId, notes);

        assertNotNull(res);
        assertEquals(year, res.getYear());
        assertEquals(month, res.getMonth());
        assertEquals(headUserId, res.getFinalizedBy());
        assertEquals(1, res.getTotalReporters());

        verify(notificationService).notifyUser(eq(10), isNull(), anyString());
        verify(auditService).log(eq(headUserId), eq("FINALIZE_KPI"), eq("kpi_snapshots"), eq(100), anyString(), anyString());
    }

    @Test
    @DisplayName("Không ghi audit khi snapshot được lưu nhưng không có ID")
    void testFinalizeRejectsSnapshotWithoutId() {
        when(kpiSnapshotRepository.findByPeriodYearAndPeriodMonth(year, month))
                .thenReturn(Collections.emptyList());

        User reporter = new User();
        reporter.setId(10);
        Role reporterRole = new Role();
        reporterRole.setCode(RoleCode.REPORTER);
        reporter.setRole(reporterRole);
        when(userRepository.findAll()).thenReturn(Collections.singletonList(reporter));
        when(kpiTargetRepository.findByTargetYearAndTargetMonth(year, month))
                .thenReturn(Collections.emptyList());
        when(articleRepository.findAll()).thenReturn(Collections.emptyList());
        when(kpiSnapshotRepository.findByReporterIdAndPeriodYearAndPeriodMonth(10, year, month))
                .thenReturn(Optional.empty());
        when(kpiSnapshotRepository.save(any(KpiSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                kpiAggregationService.finalizeMonthlyKpi(year, month, headUserId, Collections.emptyMap())
        );

        assertTrue(ex.getMessage().contains("snapshot KPI"));
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

}
