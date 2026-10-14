package com.swp391.kpi.service;

import com.swp391.kpi.dto.AppealRuleConfigCreateRequest;
import com.swp391.kpi.dto.AppealRuleConfigResponse;
import com.swp391.kpi.entity.AppealRuleConfig;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.AppealRuleConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppealRuleConfigServiceTest {

    @Mock
    private AppealRuleConfigRepository appealRuleConfigRepository;

    @Mock
    private AuditService auditService;

    @Mock private UserReadService userReadService;

    private AppealRuleConfigService appealRuleConfigService;

    @BeforeEach
    void setUp() {
        appealRuleConfigService = new AppealRuleConfigService(appealRuleConfigRepository, auditService, userReadService);
    }

    @Test
    void createConfigCreatesNewVersionWithoutChangingOldVersion() {
        AppealRuleConfigCreateRequest request = validRequest();
        when(appealRuleConfigRepository.existsByEffectiveFrom(request.getEffectiveFrom())).thenReturn(false);
        when(appealRuleConfigRepository.save(any(AppealRuleConfig.class))).thenAnswer(invocation -> {
            AppealRuleConfig saved = invocation.getArgument(0);
            saved.setId(2);
            return saved;
        });

        AppealRuleConfigResponse response = appealRuleConfigService.createConfig(request, 8);

        assertEquals(2, response.getId());
        assertEquals(7, response.getAppealWindowDays());
        assertEquals(new BigDecimal("5.00"), response.getVarianceThreshold());
        verify(appealRuleConfigRepository).save(any(AppealRuleConfig.class));
        verify(auditService).log(
                8,
                "CREATE",
                "appeal_rule_configs",
                2,
                null,
                "{\"appealWindowDays\":7,\"maxAppealsPerArticle\":1,\"varianceThreshold\":5.00,\"effectiveFrom\":\"2026-11-01\"}"
        );
    }

    @Test
    void createConfigRejectsDuplicateEffectiveDate() {
        AppealRuleConfigCreateRequest request = validRequest();
        when(appealRuleConfigRepository.existsByEffectiveFrom(request.getEffectiveFrom())).thenReturn(true);

        BusinessException error = assertThrows(
                BusinessException.class,
                () -> appealRuleConfigService.createConfig(request, 8)
        );

        assertEquals("Đã có cấu hình áp dụng từ ngày này.", error.getMessage());
        verify(appealRuleConfigRepository, never()).save(any());
    }

    private AppealRuleConfigCreateRequest validRequest() {
        AppealRuleConfigCreateRequest request = new AppealRuleConfigCreateRequest();
        request.setAppealWindowDays(7);
        request.setMaxAppealsPerArticle(1);
        request.setVarianceThreshold(new BigDecimal("5.00"));
        request.setEffectiveFrom(LocalDate.of(2026, 11, 1));
        return request;
    }
}
