package com.swp391.kpi.repository;

import com.swp391.kpi.entity.CriteriaSetStatus;
import com.swp391.kpi.entity.CriteriaSetVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MUC LUC (so dong)
 *   21  findByCategoryIdAndStatus()  - Lấy phiên bản đang ACTIVE của một thể loại (BR-QA-03)
 *   24  findTopByCategoryIdOrderByVersionNoDesc() - Lấy phiên bản có số version_no lớn nhất của một thể loại
 *   27  findByCategoryIdOrderByVersionNoDesc() - Lấy tất cả các phiên bản của một thể loại sắp xếp theo version_no giảm …
 */
@Repository
public interface CriteriaSetVersionRepository extends JpaRepository<CriteriaSetVersion, Integer> {

    // Lấy phiên bản đang ACTIVE của một thể loại (BR-QA-03)
    Optional<CriteriaSetVersion> findByCategoryIdAndStatus(Integer categoryId, CriteriaSetStatus status);

    // Lấy phiên bản có số version_no lớn nhất của một thể loại
    Optional<CriteriaSetVersion> findTopByCategoryIdOrderByVersionNoDesc(Integer categoryId);

    // Lấy tất cả các phiên bản của một thể loại sắp xếp theo version_no giảm dần (UC-14)
    List<CriteriaSetVersion> findByCategoryIdOrderByVersionNoDesc(Integer categoryId);
}
