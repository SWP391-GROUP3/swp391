package com.swp391.kpi.kpi.service;

import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.kpi.dto.KpiBenchmarkConfigDto;
import com.swp391.kpi.kpi.dto.KpiTargetCreateRequest;
import com.swp391.kpi.kpi.dto.KpiTargetResponse;
import com.swp391.kpi.kpi.dto.KpiTargetUpdateRequest;
import com.swp391.kpi.kpi.entity.KpiTarget;
import com.swp391.kpi.kpi.repository.KpiTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý toàn bộ logic nghiệp vụ liên quan đến chỉ tiêu KPI tháng (UC-58, UC-59).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   38  field                           repository / service duoc inject
 *   64  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   44  getTargetsByPeriod()            Lay danh sach chi tieu theo ky (nam va thang) cho man hinh UC-59
 *   57  getTargetById()                 Lay chi tiet chi tieu theo ID
 *   74  getDefaultBenchmarks()          Lay danh sach dinh muc chuan theo ngach phong vien (UC-58)
 *   81  updateBenchmarks()              Cap nhat danh sach dinh muc chuan theo ngach phong vien (UC-58)
 *   92  createTarget()                  Giao chi tieu moi cho phong vien trong thang (UC-59)
 *  122  updateTarget()                  Dieu chinh chi tieu da giao (UC-59 Nhanh ngoai le A2)
 *  137  deleteTarget()                  Xoa chi tieu KPI theo ID
 */
@Service
@RequiredArgsConstructor
public class KpiTargetService {

    private final KpiTargetRepository kpiTargetRepository;
    private final UserReadService userReadService;

    /**
     * Lấy danh sách chỉ tiêu theo kỳ (năm và tháng) cho màn hình UC-59.
     */
    @Transactional(readOnly = true)
    public List<KpiTargetResponse> getTargetsByPeriod(Short targetYear, Byte targetMonth) {
        List<KpiTarget> targets = kpiTargetRepository.findByTargetYearAndTargetMonth(targetYear, targetMonth);
        List<KpiTargetResponse> result = new ArrayList<>();
        for (KpiTarget target : targets) {
            result.add(KpiTargetResponse.from(target));
        }
        return result;
    }

    /**
     * Lấy chi tiết chỉ tiêu theo ID.
     */
    @Transactional(readOnly = true)
    public KpiTargetResponse getTargetById(Integer id) {
        KpiTarget target = kpiTargetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chỉ tiêu KPI có ID: " + id));
        return KpiTargetResponse.from(target);
    }

    // Danh sách định mức chuẩn mẫu theo ngạch phóng viên (UC-58)
    private static final List<KpiBenchmarkConfigDto> BENCHMARK_LIST = new java.util.concurrent.CopyOnWriteArrayList<>(List.of(
            new KpiBenchmarkConfigDto("PROBATIONARY", "Phóng viên tập sự", 4, new BigDecimal("60.00")),
            new KpiBenchmarkConfigDto("JUNIOR", "Phóng viên sơ cấp", 6, new BigDecimal("65.00")),
            new KpiBenchmarkConfigDto("SENIOR", "Phóng viên kỳ cựu", 8, new BigDecimal("70.00"))
    ));

    /**
     * Lấy danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
     */
    @Transactional(readOnly = true)
    public List<KpiBenchmarkConfigDto> getDefaultBenchmarks() {
        return new ArrayList<>(BENCHMARK_LIST);
    }

    /**
     * Cập nhật danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
     */
    public List<KpiBenchmarkConfigDto> updateBenchmarks(List<KpiBenchmarkConfigDto> updatedList) {
        BENCHMARK_LIST.clear();
        BENCHMARK_LIST.addAll(updatedList);
        return new ArrayList<>(BENCHMARK_LIST);
    }

    /**
     * Giao chỉ tiêu mới cho phóng viên trong tháng (UC-59).
     * Kiểm tra ràng buộc BR-KPI-01: duy nhất (reporter_id, target_year, target_month).
     */
    @Transactional
    public KpiTargetResponse createTarget(KpiTargetCreateRequest request, Integer setByUserId) {
        UserAccessResponse reporter = null;
        List<UserAccessResponse> users = userReadService.getUsers();
        for (UserAccessResponse user : users) {
            if (user.id().equals(request.getReporterId())) {
                reporter = user;
                break;
            }
        }

        if (reporter == null) {
            throw new NotFoundException(
                    "Không tìm thấy người dùng có ID phóng viên: " + request.getReporterId()
            );
        }
        if (!"REPORTER".equals(reporter.role())) {
            throw new BusinessException(
                    "Người dùng ID " + request.getReporterId() + " không phải phóng viên"
            );
        }
        if (!"ACTIVE".equals(reporter.status())) {
            throw new BusinessException(
                    "Phóng viên ID " + request.getReporterId() + " không hoạt động"
            );
        }

        // Kiểm tra xem đã có chỉ tiêu trong kỳ này chưa (BR-KPI-01)
        boolean exists = kpiTargetRepository.existsByReporterIdAndTargetYearAndTargetMonth(
                request.getReporterId(),
                request.getTargetYear(),
                request.getTargetMonth()
        );
        if (exists) {
            throw new BusinessException(
                    "Phóng viên (ID: " + request.getReporterId() + ") đã được thiết lập chỉ tiêu cho tháng "
                            + request.getTargetMonth() + "/" + request.getTargetYear() + ". Vui lòng chỉnh sửa thay vì tạo mới."
            );
        }

        KpiTarget target = new KpiTarget();
        target.setReporterId(request.getReporterId());
        target.setTargetYear(request.getTargetYear());
        target.setTargetMonth(request.getTargetMonth());
        target.setMinPublished(request.getMinPublished());
        target.setMinAvgScore(request.getMinAvgScore());
        target.setSetBy(setByUserId);

        KpiTarget saved = kpiTargetRepository.save(target);
        return KpiTargetResponse.from(saved);
    }

    /**
     * Điều chỉnh chỉ tiêu đã giao (UC-59 Nhánh ngoại lệ A2).
     */
    @Transactional
    public KpiTargetResponse updateTarget(Integer id, KpiTargetUpdateRequest request) {
        KpiTarget target = kpiTargetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chỉ tiêu KPI có ID: " + id));

        target.setMinPublished(request.getMinPublished());
        target.setMinAvgScore(request.getMinAvgScore());

        KpiTarget updated = kpiTargetRepository.save(target);
        return KpiTargetResponse.from(updated);
    }

    /**
     * Xoá chỉ tiêu KPI theo ID.
     */
    @Transactional
    public void deleteTarget(Integer id) {
        KpiTarget target = kpiTargetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chỉ tiêu KPI có ID: " + id));
        kpiTargetRepository.delete(target);
    }
}
