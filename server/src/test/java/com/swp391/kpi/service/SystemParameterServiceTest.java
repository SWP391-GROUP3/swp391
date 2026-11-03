package com.swp391.kpi.service;

import com.swp391.kpi.dto.SystemParameterIntakeResponse;
import com.swp391.kpi.dto.SystemParameterIntakeUpdateRequest;
import com.swp391.kpi.entity.SystemParameter;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.SystemParameterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiem tra Admin xem/sua 5 tham so nhap bai (UC-13), khong can DB.
 * Gia tri mau giong data.sql: 5 tac gia, 3 ngay, 20%, 50%, 5 bit.
 */
@ExtendWith(MockitoExtension.class)
class SystemParameterServiceTest {

    private static final Integer ADMIN_ID = 8;

    @Mock private SystemParameterRepository systemParameterRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private SystemParameterService systemParameterService;

    @Test
    void getIntakeParametersReadsFiveValues() {
        stubIntakeParameters();

        SystemParameterIntakeResponse result = systemParameterService.getIntakeParameters();

        assertEquals(Integer.valueOf(5), result.getMaxAuthorsPerArticle());
        assertEquals(Integer.valueOf(3), result.getCoAuthorConfirmDays());
        assertEquals(Integer.valueOf(20), result.getDupWarnPercent());
        assertEquals(Integer.valueOf(50), result.getDupBlockPercent());
        assertEquals(Integer.valueOf(5), result.getDhashMaxBits());
        assertEquals(Integer.valueOf(5), result.getShingleSize());
    }

    @Test
    void updateSavesOnlyChangedParametersAndWritesOneAuditRowEach() {
        Map<String, SystemParameter> stored = stubIntakeParameters();

        // doi so tac gia 5 -> 4 va nguong canh bao 20 -> 25, 3 tham so con lai giu nguyen
        SystemParameterIntakeResponse result =
                systemParameterService.updateIntakeParameters(request(4, 3, 25, 50, 5), ADMIN_ID);

        assertEquals(Integer.valueOf(4), result.getMaxAuthorsPerArticle());
        assertEquals(Integer.valueOf(25), result.getDupWarnPercent());
        verify(systemParameterRepository, times(2)).save(any(SystemParameter.class));
        verify(auditService).log(ADMIN_ID, "UPDATE", "system_parameters", 1,
                "{\"param_key\":\"MAX_AUTHORS_PER_ARTICLE\",\"param_value\":\"5\"}",
                "{\"param_key\":\"MAX_AUTHORS_PER_ARTICLE\",\"param_value\":\"4\"}");
        verify(auditService).log(ADMIN_ID, "UPDATE", "system_parameters", 3,
                "{\"param_key\":\"DUP_WARN_PERCENT\",\"param_value\":\"20\"}",
                "{\"param_key\":\"DUP_WARN_PERCENT\",\"param_value\":\"25\"}");

        SystemParameter maxAuthors = stored.get("MAX_AUTHORS_PER_ARTICLE");
        assertEquals(ADMIN_ID, maxAuthors.getUpdatedBy());
        assertNotNull(maxAuthors.getUpdatedAt());
    }

    @Test
    void updateWritesNothingWhenNoValueChanged() {
        stubIntakeParameters();

        systemParameterService.updateIntakeParameters(request(5, 3, 20, 50, 5), ADMIN_ID);

        verify(systemParameterRepository, never()).save(any(SystemParameter.class));
        verify(auditService, never()).log(anyInt(), anyString(), anyString(), anyInt(), anyString(), anyString());
    }

    @Test
    void updateFailsWhenWarnThresholdIsNotBelowBlockThreshold() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> systemParameterService.updateIntakeParameters(request(5, 3, 50, 50, 5), ADMIN_ID));

        assertEquals("Ngưỡng cảnh báo trùng lặp phải nhỏ hơn ngưỡng chặn.", ex.getMessage());
        verify(systemParameterRepository, never()).save(any(SystemParameter.class));
    }

    // ------------------------------------------------------------ du lieu mau

    // 6 tham so giong data.sql, id 1..6 theo thu tu; tra ve de test kiem tra gia tri sau khi sua
    private Map<String, SystemParameter> stubIntakeParameters() {
        Map<String, SystemParameter> stored = new HashMap<>();
        stored.put("MAX_AUTHORS_PER_ARTICLE", parameter(1, "MAX_AUTHORS_PER_ARTICLE", "5"));
        stored.put("CO_AUTHOR_CONFIRM_DAYS", parameter(2, "CO_AUTHOR_CONFIRM_DAYS", "3"));
        stored.put("DUP_WARN_PERCENT", parameter(3, "DUP_WARN_PERCENT", "20"));
        stored.put("DUP_BLOCK_PERCENT", parameter(4, "DUP_BLOCK_PERCENT", "50"));
        stored.put("DHASH_MAX_BITS", parameter(5, "DHASH_MAX_BITS", "5"));
        stored.put("SHINGLE_SIZE", parameter(6, "SHINGLE_SIZE", "5"));
        for (SystemParameter parameter : stored.values()) {
            when(systemParameterRepository.findByParamKey(parameter.getParamKey()))
                    .thenReturn(Optional.of(parameter));
        }
        return stored;
    }

    private static SystemParameter parameter(Integer id, String key, String value) {
        SystemParameter parameter = new SystemParameter();
        parameter.setId(id);
        parameter.setParamKey(key);
        parameter.setParamValue(value);
        parameter.setFlow("BF-01");
        return parameter;
    }

    private static SystemParameterIntakeUpdateRequest request(int maxAuthors, int confirmDays, int warn,
                                                              int block, int dhash) {
        SystemParameterIntakeUpdateRequest request = new SystemParameterIntakeUpdateRequest();
        request.setMaxAuthorsPerArticle(maxAuthors);
        request.setCoAuthorConfirmDays(confirmDays);
        request.setDupWarnPercent(warn);
        request.setDupBlockPercent(block);
        request.setDhashMaxBits(dhash);
        request.setShingleSize(5);   // giu nguyen gia tri mac dinh, test chi doi 5 o kia
        return request;
    }

    @Test
    void getIntValueAndDecimalValue_readParameterByKey() {
        when(systemParameterRepository.findByParamKey("DUP_WARN_PERCENT"))
                .thenReturn(Optional.of(parameter(3, "DUP_WARN_PERCENT", " 20 ")));
        when(systemParameterRepository.findByParamKey("SOME_RATE"))
                .thenReturn(Optional.of(parameter(9, "SOME_RATE", "1.25")));

        assertEquals(20, systemParameterService.getIntValue("DUP_WARN_PERCENT"));
        assertEquals(new BigDecimal("1.25"), systemParameterService.getDecimalValue("SOME_RATE"));
    }

    @Test
    void getIntValue_missingKey_throwsBusinessException() {
        when(systemParameterRepository.findByParamKey("KHONG_CO")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> systemParameterService.getIntValue("KHONG_CO"));
    }
}
