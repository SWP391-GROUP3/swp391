package com.swp391.kpi.repository;

import com.swp391.kpi.entity.CsvImportLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository thao tac voi bang csv_import_logs (BF-05).
 *
 * MUC LUC (so dong)
 *   16  findAllByOrderByImportedAtDesc() - danh sach nhat ky import sap xep theo thoi gian moi nhat
 */
public interface CsvImportLogRepository extends JpaRepository<CsvImportLog, Integer> {

    Page<CsvImportLog> findAllByOrderByImportedAtDesc(Pageable pageable);
}
