package com.swp391.kpi.repository;

import com.swp391.kpi.entity.KpiSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tac du lieu voi bang kpi_snapshots.
 * Phuc vu Job tong hop KPI tu dong va Dashboard ban bien tap (BF-05).
 */
public interface KpiSnapshotRepository extends JpaRepository<KpiSnapshot, Integer> {

    // Lay snapshot cua 1 phong vien trong 1 ky (nam, thang)
    Optional<KpiSnapshot> findByReporterIdAndPeriodYearAndPeriodMonth(Integer reporterId, Short periodYear, Byte periodMonth);

    // Kiem tra da co ban ghi snapshot trong ky chua
    boolean existsByReporterIdAndPeriodYearAndPeriodMonth(Integer reporterId, Short periodYear, Byte periodMonth);

    // Lay danh sach snapshot cua ca ban trong 1 ky (nam, thang) cho Dashboard ban bien tap (UC-61)
    List<KpiSnapshot> findByPeriodYearAndPeriodMonth(Short periodYear, Byte periodMonth);

    // Lay lich su snapshot cua 1 phong vien sap xep moi nhat truoc
    List<KpiSnapshot> findByReporterIdOrderByPeriodYearDescPeriodMonthDesc(Integer reporterId);
}
