
package com.swp391.kpi.assessment.service;

import com.swp391.kpi.assessment.dto.CriteriaItemDTO;
import com.swp391.kpi.assessment.dto.CriteriaSetVersionCreateRequest;
import com.swp391.kpi.assessment.dto.CriteriaSetVersionResponse;
import com.swp391.kpi.assessment.entity.Criteria;
import com.swp391.kpi.assessment.entity.CriteriaSetStatus;
import com.swp391.kpi.assessment.entity.CriteriaSetVersion;
import com.swp391.kpi.assessment.repository.CriteriaRepository;
import com.swp391.kpi.assessment.repository.CriteriaSetVersionRepository;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ Quản lý bộ tiêu chí chấm (Luồng BF-02 / Admin).
 * Thực thi các Use Cases: UC-14, UC-15, UC-16, UC-17 và Business Rules:
 * BR-SC-01, 02, 03.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   39  field                           repository / service duoc inject
 *   42  CriteriaSetService()            ham khoi tao
 *   53  getVersionsByCategory()         Lay toan bo danh sach phien ban bo tieu chi cua mot the loai (UC-14)
 *   71  getActiveVersionByCategory()    Lay phien ban bo tieu chi dang ACTIVE cua mot the loai (BR-SC-03)
 *   88  createDraftVersion()            Tao phien ban bo tieu chi moi o trang thai DRAFT (UC-15, UC-16)
 *  151  activateVersion()               Kich hoat mot phien ban DRAFT thanh ACTIVE (UC-17, BR-SC-02)
 */
@Service
public class CriteriaSetService {

    private final CriteriaSetVersionRepository criteriaSetVersionRepository;
    private final CriteriaRepository criteriaRepository;

    public CriteriaSetService(CriteriaSetVersionRepository criteriaSetVersionRepository,
            CriteriaRepository criteriaRepository) {
        this.criteriaSetVersionRepository = criteriaSetVersionRepository;
        this.criteriaRepository = criteriaRepository;
    }

    /**
     * Lấy toàn bộ danh sách phiên bản bộ tiêu chí của một thể loại (UC-14).
     * Sắp xếp theo version_no giảm dần (phiên bản mới nhất lên đầu).
     */
    @Transactional(readOnly = true)
    public List<CriteriaSetVersionResponse> getVersionsByCategory(Integer categoryId) {
        List<CriteriaSetVersion> versions = criteriaSetVersionRepository
                .findByCategoryIdOrderByVersionNoDesc(categoryId);
        List<CriteriaSetVersionResponse> responses = new ArrayList<>();

        for (CriteriaSetVersion v : versions) {
            List<Criteria> criteriaList = criteriaRepository.findByCriteriaSetVersionIdOrderBySortOrderAsc(v.getId());
            responses.add(CriteriaSetVersionResponse.from(v, criteriaList));
        }

        return responses;
    }

    /**
     * Lấy phiên bản bộ tiêu chí đang ACTIVE của một thể loại (BR-SC-03).
     * Sử dụng khi BTV mở bài để nạp tiêu chí chấm (UC-22, BR-SC-03).
     */
    @Transactional(readOnly = true)
    public CriteriaSetVersionResponse getActiveVersionByCategory(Integer categoryId) {
        CriteriaSetVersion activeVersion = criteriaSetVersionRepository
                .findByCategoryIdAndStatus(categoryId, CriteriaSetStatus.ACTIVE)
                .orElseThrow(
                        () -> new NotFoundException("Thể loại này hiện chưa có bộ tiêu chí nào ở trạng thái ACTIVE"));

        List<Criteria> criteriaList = criteriaRepository
                .findByCriteriaSetVersionIdOrderBySortOrderAsc(activeVersion.getId());
        return CriteriaSetVersionResponse.from(activeVersion, criteriaList);
    }

    /**
     * Tạo phiên bản bộ tiêu chí mới ở trạng thái DRAFT (UC-15, UC-16).
     * Kiểm tra tổng trọng số phải đúng 100.00% (BR-SC-01).
     * Version mới luôn có version_no = max(version_no) + 1 (BR-SC-02).
     */
    @Transactional
    public CriteriaSetVersionResponse createDraftVersion(CriteriaSetVersionCreateRequest request, Integer currentUserId) {
        List<CriteriaItemDTO> items = request.getCriteria();

        // Kiểm tra tối thiểu 2 tiêu chí
        if (items == null || items.size() < 2) {
            throw new BusinessException("Bộ tiêu chí phải có ít nhất 2 tiêu chí thành phần");
        }

        // BR-SC-01: Kiểm tra tổng trọng số phải chính xác 100.00%
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (CriteriaItemDTO item : items) {
            if (item.getWeight() == null || item.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Mỗi tiêu chí phải có trọng số lớn hơn 0%");
            }
            totalWeight = totalWeight.add(item.getWeight());
        }

        if (totalWeight.compareTo(new BigDecimal("100.00")) != 0) {
            throw new BusinessException("Tổng trọng số của tất cả các tiêu chí phải chính xác bằng 100.00%. Hiện tại: "
                    + totalWeight + "%");
        }

        // BR-SC-02: Xác định version_no kế tiếp
        Optional<CriteriaSetVersion> maxVersion = criteriaSetVersionRepository
                .findTopByCategoryIdOrderByVersionNoDesc(request.getCategoryId());
        int nextVersionNo = maxVersion.map(v -> v.getVersionNo() + 1).orElse(1);

        // Lưu phiên bản DRAFT
        CriteriaSetVersion newVersion = new CriteriaSetVersion();
        newVersion.setCategoryId(request.getCategoryId());
        newVersion.setVersionNo(nextVersionNo);
        newVersion.setStatus(CriteriaSetStatus.DRAFT);
        // M-A3: nguoi tao la Admin dang dang nhap (lay tu token)
        newVersion.setCreatedBy(currentUserId);
        newVersion.setCreatedAt(LocalDateTime.now());

        CriteriaSetVersion savedVersion = criteriaSetVersionRepository.save(newVersion);

        // Lưu danh sách tiêu chí thành phần
        List<Criteria> savedCriteriaList = new ArrayList<>();
        int defaultSort = 1;
        for (CriteriaItemDTO item : items) {
            Criteria c = new Criteria();
            c.setCriteriaSetVersion(savedVersion);
            c.setName(item.getName().trim());
            c.setWeight(item.getWeight());
            c.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : defaultSort++);
            savedCriteriaList.add(criteriaRepository.save(c));
        }

        // Nếu có chọn autoActivate thì kích hoạt luôn
        if (Boolean.TRUE.equals(request.getAutoActivate())) {
            return activateVersion(savedVersion.getId(), savedVersion.getCreatedBy());
        }

        return CriteriaSetVersionResponse.from(savedVersion, savedCriteriaList);
    }

    /**
     * Kích hoạt một phiên bản DRAFT thành ACTIVE (UC-17, BR-SC-02).
     * Phiên bản ACTIVE cũ của cùng thể loại sẽ tự động chuyển thành RETIRED.
     */
    @Transactional
    public CriteriaSetVersionResponse activateVersion(Integer versionId, Integer currentUserId) {
        CriteriaSetVersion versionToActivate = criteriaSetVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản bộ tiêu chí với ID: " + versionId));

        if (versionToActivate.getStatus() == CriteriaSetStatus.ACTIVE) {
            throw new BusinessException("Phiên bản này hiện đã ở trạng thái ACTIVE");
        }

        if (versionToActivate.getStatus() == CriteriaSetStatus.RETIRED) {
            throw new BusinessException("Không thể kích hoạt lại phiên bản đã bị ngưng áp dụng (RETIRED)");
        }

        // Chuyển phiên bản ACTIVE cũ thành RETIRED (nếu có)
        // Bắt buộc dùng saveAndFlush để ghi ngay trạng thái RETIRED xuống Database,
        // tránh vi phạm Unique Filtered Index 'uq_criteria_set_versions_active' khi
        // kích hoạt bản mới
        Optional<CriteriaSetVersion> currentActive = criteriaSetVersionRepository
                .findByCategoryIdAndStatus(versionToActivate.getCategoryId(), CriteriaSetStatus.ACTIVE);

        if (currentActive.isPresent()) {
            CriteriaSetVersion oldActive = currentActive.get();
            oldActive.setStatus(CriteriaSetStatus.RETIRED);
            criteriaSetVersionRepository.saveAndFlush(oldActive);
        }

        // Cập nhật phiên bản mới thành ACTIVE
        versionToActivate.setStatus(CriteriaSetStatus.ACTIVE);
        versionToActivate.setActivatedBy(currentUserId);
        versionToActivate.setActivatedAt(LocalDateTime.now());

        CriteriaSetVersion activated = criteriaSetVersionRepository.saveAndFlush(versionToActivate);
        List<Criteria> criteriaList = criteriaRepository
                .findByCriteriaSetVersionIdOrderBySortOrderAsc(activated.getId());

        return CriteriaSetVersionResponse.from(activated, criteriaList);
    }
}
