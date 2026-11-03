package com.swp391.kpi.kpi.repository;

import com.swp391.kpi.kpi.entity.CsvImportLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository thao tac voi bang csv_import_logs (BF-05).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  findAllByOrderByImportedAtDesc()  tim tat ca theo order theo imported at desc
 */
public interface CsvImportLogRepository extends JpaRepository<CsvImportLog, Integer> {

    Page<CsvImportLog> findAllByOrderByImportedAtDesc(Pageable pageable);
}
