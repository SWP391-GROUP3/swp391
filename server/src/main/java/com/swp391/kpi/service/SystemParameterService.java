package com.swp391.kpi.service;

import com.swp391.kpi.dto.SystemParameterIntakeResponse;
import com.swp391.kpi.dto.SystemParameterIntakeUpdateRequest;
import com.swp391.kpi.entity.SystemParameter;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.SystemParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Cung cap tham so he thong cho cac luong. Luong khac goi service nay,
 * khong goi thang SystemParameterRepository (AGENTS.md muc 2).
 * Admin xem/sua 6 tham so nhap bai cua BF-01 (UC-13); moi tham so doi gia tri ghi 1 dong audit_logs.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   44  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   53  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   58  getValuesByFlow()               Moi tham so cua 1 luong (cot flow, vd "BF-04") thanh Map param_key -> param_value
 *   68  getIntakeParameters()           UC-13: 6 tham so nhap bai hien tai cho man Admin (Q1 02/11: gui/nhan ca 6 trong 1 object)
 *   83  updateIntakeParameters()        UC-13: Admin sua 6 tham so nhap bai
 *  139  getIntValue()                   M-D2: doc 1 tham so he thong dang so nguyen theo ten (thay cho cac ban readInt viet rieng o luong khac)
 *  145  getDecimalValue()               M-D2: doc 1 tham so he thong dang so thap phan theo ten
 *  ---- Ham phu (private, chi dung trong file) ----
 *  101  updateIfChanged()               Chi ghi tham so thuc su doi gia tri: luu nguoi sua, luc sua va 1 dong audit_logs (gia tri cu -> moi)
 *  116  findParameter()                 tim tham so theo ten, khong co thi NotFoundException
 *  122  readInt()                       Lay 1 tham so dang so nguyen
 *  133  toAuditJson()                   Gia tri cu/moi ghi vao audit_logs dang JSON (schema: old_value/new_value luu dang JSON)
 *  155  findParameterOrBusiness()       Luong khac doc tham so: thieu tham so la loi cau hinh nghiep vu -> BusinessException tieng Viet (plan M-D2)
 */
@Service
@RequiredArgsConstructor
public class SystemParameterService {

    // Ten param_key cua 6 tham so UC-13 (xem database/data.sql)
    private static final String PARAM_MAX_AUTHORS = "MAX_AUTHORS_PER_ARTICLE";
    private static final String PARAM_CONFIRM_DAYS = "CO_AUTHOR_CONFIRM_DAYS";
    private static final String PARAM_DUP_WARN = "DUP_WARN_PERCENT";
    private static final String PARAM_DUP_BLOCK = "DUP_BLOCK_PERCENT";
    private static final String PARAM_DHASH_MAX_BITS = "DHASH_MAX_BITS";
    private static final String PARAM_SHINGLE_SIZE = "SHINGLE_SIZE";
    private static final String AUDIT_ACTION_UPDATE = "UPDATE";           // audit_logs.action_type
    private static final String AUDIT_ENTITY = "system_parameters";       // audit_logs.entity_type = ten bang

    private final SystemParameterRepository systemParameterRepository;
    private final AuditService auditService;

    // Moi tham so cua 1 luong (cot flow, vd "BF-04") thanh Map param_key -> param_value
    @Transactional(readOnly = true)
    public Map<String, String> getValuesByFlow(String flow) {
        Map<String, String> values = new HashMap<>();
        for (SystemParameter parameter : systemParameterRepository.findByFlow(flow)) {
            values.put(parameter.getParamKey(), parameter.getParamValue());
        }
        return values;
    }

    // UC-13: 6 tham so nhap bai hien tai cho man Admin (Q1 02/11: gui/nhan ca 6 trong 1 object)
    @Transactional(readOnly = true)
    public SystemParameterIntakeResponse getIntakeParameters() {
        SystemParameterIntakeResponse response = new SystemParameterIntakeResponse();
        response.setMaxAuthorsPerArticle(readInt(findParameter(PARAM_MAX_AUTHORS)));
        response.setCoAuthorConfirmDays(readInt(findParameter(PARAM_CONFIRM_DAYS)));
        response.setDupWarnPercent(readInt(findParameter(PARAM_DUP_WARN)));
        response.setDupBlockPercent(readInt(findParameter(PARAM_DUP_BLOCK)));
        response.setDhashMaxBits(readInt(findParameter(PARAM_DHASH_MAX_BITS)));
        response.setShingleSize(readInt(findParameter(PARAM_SHINGLE_SIZE)));
        return response;
    }

    // UC-13: Admin sua 6 tham so nhap bai. Gioi han tung o da kiem bang @Valid o request;
    // o day kiem luat can nhieu o va ghi lai. Chi anh huong bai nop SAU khi sua
    // (ket qua kiem trung, han xac nhan cua bai cu da luu, khong tinh lai).
    @Transactional
    public SystemParameterIntakeResponse updateIntakeParameters(SystemParameterIntakeUpdateRequest request,
                                                                Integer adminId) {
        // BR-IN-04: tu nguong canh bao toi nguong chan moi phai giai trinh, nen canh bao phai nho hon chan
        if (request.getDupWarnPercent() >= request.getDupBlockPercent()) {
            throw new BusinessException("Ngưỡng cảnh báo trùng lặp phải nhỏ hơn ngưỡng chặn.");
        }
        LocalDateTime now = LocalDateTime.now();
        updateIfChanged(PARAM_MAX_AUTHORS, request.getMaxAuthorsPerArticle(), adminId, now);
        updateIfChanged(PARAM_CONFIRM_DAYS, request.getCoAuthorConfirmDays(), adminId, now);
        updateIfChanged(PARAM_DUP_WARN, request.getDupWarnPercent(), adminId, now);
        updateIfChanged(PARAM_DUP_BLOCK, request.getDupBlockPercent(), adminId, now);
        updateIfChanged(PARAM_DHASH_MAX_BITS, request.getDhashMaxBits(), adminId, now);
        updateIfChanged(PARAM_SHINGLE_SIZE, request.getShingleSize(), adminId, now);
        return getIntakeParameters();
    }

    // Chi ghi tham so thuc su doi gia tri: luu nguoi sua, luc sua va 1 dong audit_logs (gia tri cu -> moi).
    // Gia tri giu nguyen thi bo qua, khong ghi nhat ky thua.
    private void updateIfChanged(String key, Integer newValue, Integer adminId, LocalDateTime now) {
        SystemParameter parameter = findParameter(key);
        String oldValue = parameter.getParamValue();
        if (readInt(parameter) == newValue) {
            return;
        }
        parameter.setParamValue(String.valueOf(newValue));
        parameter.setUpdatedBy(adminId);
        parameter.setUpdatedAt(now);
        systemParameterRepository.save(parameter);
        auditService.log(adminId, AUDIT_ACTION_UPDATE, AUDIT_ENTITY, parameter.getId(),
                toAuditJson(key, oldValue), toAuditJson(key, parameter.getParamValue()));
    }

    // tim tham so theo ten, khong co thi NotFoundException
    private SystemParameter findParameter(String key) {
        return systemParameterRepository.findByParamKey(key)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tham số hệ thống: " + key));
    }

    // Lay 1 tham so dang so nguyen. Sai dinh dang thi bao loi ro rang (du lieu DB bi sua tay sai).
    private static int readInt(SystemParameter parameter) {
        String value = parameter.getParamValue();
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Tham số hệ thống " + parameter.getParamKey()
                    + " không phải là số nguyên: " + value);
        }
    }

    // Gia tri cu/moi ghi vao audit_logs dang JSON (schema: old_value/new_value luu dang JSON)
    private static String toAuditJson(String key, String value) {
        return "{\"param_key\":\"" + key + "\",\"param_value\":\"" + value + "\"}";
    }

    // M-D2: doc 1 tham so he thong dang so nguyen theo ten (thay cho cac ban readInt viet rieng o luong khac)
    @Transactional(readOnly = true)
    public int getIntValue(String key) {
        return readInt(findParameterOrBusiness(key));
    }

    // M-D2: doc 1 tham so he thong dang so thap phan theo ten
    @Transactional(readOnly = true)
    public BigDecimal getDecimalValue(String key) {
        SystemParameter parameter = findParameterOrBusiness(key);
        try {
            return new BigDecimal(parameter.getParamValue().trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Tham số hệ thống " + key + " không phải là số: " + parameter.getParamValue());
        }
    }

    // Luong khac doc tham so: thieu tham so la loi cau hinh nghiep vu -> BusinessException tieng Viet (plan M-D2)
    private SystemParameter findParameterOrBusiness(String key) {
        return systemParameterRepository.findByParamKey(key)
                .orElseThrow(() -> new BusinessException("Chưa cấu hình tham số hệ thống: " + key));
    }
}
