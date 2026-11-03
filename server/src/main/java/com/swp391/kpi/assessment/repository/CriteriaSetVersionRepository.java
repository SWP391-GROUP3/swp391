package com.swp391.kpi.assessment.repository;

import com.swp391.kpi.assessment.entity.CriteriaSetStatus;
import com.swp391.kpi.assessment.entity.CriteriaSetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy van bang cua entity CriteriaSetVersion (Spring Data JPA). Chi service cung luong
 * goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  findByCategoryIdAndStatus()     Lay phien ban dang ACTIVE cua mot the loai (BR-SC-03)
 *   27  findTopByCategoryIdOrderByVersionNoDesc()  Lay phien ban co so version_no lon nhat cua mot the loai
 *   30  findByCategoryIdOrderByVersionNoDesc()  Lay tat ca cac phien ban cua mot the loai sap xep theo version_no giam dan (UC-14)
 */
@Repository
public interface CriteriaSetVersionRepository extends JpaRepository<CriteriaSetVersion, Integer> {

    // Lấy phiên bản đang ACTIVE của một thể loại (BR-SC-03)
    Optional<CriteriaSetVersion> findByCategoryIdAndStatus(Integer categoryId, CriteriaSetStatus status);

    // Lấy phiên bản có số version_no lớn nhất của một thể loại
    Optional<CriteriaSetVersion> findTopByCategoryIdOrderByVersionNoDesc(Integer categoryId);

    // Lấy tất cả các phiên bản của một thể loại sắp xếp theo version_no giảm dần (UC-14)
    List<CriteriaSetVersion> findByCategoryIdOrderByVersionNoDesc(Integer categoryId);
}
