package com.swp391.kpi.service;

import com.swp391.kpi.dto.AppealRuleConfigCreateRequest;
import com.swp391.kpi.dto.AppealRuleConfigResponse;
import com.swp391.kpi.entity.AppealRuleConfig;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.AppealRuleConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Nghiep vu cau hinh luat khieu nai (UC-44): lay cau hinh dang hieu luc, lich su, them cau hinh moi va
 * ghi nhat ky.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   35  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   43  getActiveConfig()               Lay cau hinh khiieu nai dang co hieu luc tinh den ngay hom nay
 *   53  getActiveConfigEntity()         Ham noi bo de cac service khac trong luong appeal lay entity cau hinh dang ap dung
 *   63  getAllConfigs()                 Lay danh sach tat ca cac phien ban cau hinh de Admin theo doi lich su (UC-44 Normal Flow 1)
 *   78  createConfig()                  Tao phien ban cau hinh moi theo ngay hieu luc (UC-44)
 *  ---- Ham phu (private, chi dung trong file) ----
 *  110  toAuditValue()                  doi sang audit value
 */
@Service
@RequiredArgsConstructor
public class AppealRuleConfigService {

    private final AppealRuleConfigRepository appealRuleConfigRepository;
    private final AuditService auditService;
    private final UserReadService userReadService;

    /**
     * Lay cau hinh khiieu nai dang co hieu luc tinh den ngay hom nay.
     */
    @Transactional(readOnly = true)
    public AppealRuleConfigResponse getActiveConfig(Integer currentUserId) {
        userReadService.requireActive(currentUserId, "ADMIN");
        AppealRuleConfig config = getActiveConfigEntity();
        return AppealRuleConfigResponse.from(config);
    }

    /**
     * Ham noi bo de cac service khac trong luong appeal lay entity cau hinh dang ap dung.
     */
    @Transactional(readOnly = true)
    public AppealRuleConfig getActiveConfigEntity() {
        return appealRuleConfigRepository
                .findFirstByIsActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate.now())
                .orElseThrow(() -> new NotFoundException("Chưa có cấu hình khiếu nại nào đang có hiệu lực"));
    }

    /**
     * Lay danh sach tat ca cac phien ban cau hinh de Admin theo doi lich su (UC-44 Normal Flow 1).
     */
    @Transactional(readOnly = true)
    public List<AppealRuleConfigResponse> getAllConfigs(Integer currentUserId) {
        userReadService.requireActive(currentUserId, "ADMIN");
        List<AppealRuleConfig> list = appealRuleConfigRepository.findAllByOrderByEffectiveFromDesc();
        List<AppealRuleConfigResponse> result = new ArrayList<>();
        for (AppealRuleConfig config : list) {
            result.add(AppealRuleConfigResponse.from(config));
        }
        return result;
    }

    /**
     * Tao phien ban cau hinh moi theo ngay hieu luc (UC-44).
     * BR-QC-12: Khong sua de len ban cu da ap dung cho khieu nai ma tao ban ghi moi.
     */
    @Transactional
    public AppealRuleConfigResponse createConfig(AppealRuleConfigCreateRequest request, Integer currentUserId) {
        userReadService.requireActive(currentUserId, "ADMIN");
        // UC-44 E2: Kiem tra trung ngay hieu luc
        if (appealRuleConfigRepository.existsByEffectiveFrom(request.getEffectiveFrom())) {
            throw new BusinessException("Đã có cấu hình áp dụng từ ngày này.");
        }

        String oldValue = appealRuleConfigRepository
                .findFirstByIsActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate.now())
                .map(this::toAuditValue)
                .orElse(null);

        AppealRuleConfig entity = new AppealRuleConfig();
        entity.setAppealWindowDays(request.getAppealWindowDays());
        entity.setMaxAppealsPerArticle(request.getMaxAppealsPerArticle());
        entity.setVarianceThreshold(request.getVarianceThreshold());
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setIsActive(true);
        entity.setUpdatedBy(currentUserId);

        AppealRuleConfig saved = appealRuleConfigRepository.save(entity);
        auditService.log(
                currentUserId,
                "CREATE",
                "appeal_rule_configs",
                saved.getId(),
                oldValue,
                toAuditValue(saved)
        );
        return AppealRuleConfigResponse.from(saved);
    }

    private String toAuditValue(AppealRuleConfig config) {
        return "{\"appealWindowDays\":" + config.getAppealWindowDays()
                + ",\"maxAppealsPerArticle\":" + config.getMaxAppealsPerArticle()
                + ",\"varianceThreshold\":" + config.getVarianceThreshold()
                + ",\"effectiveFrom\":\"" + config.getEffectiveFrom() + "\"}";
    }
}
