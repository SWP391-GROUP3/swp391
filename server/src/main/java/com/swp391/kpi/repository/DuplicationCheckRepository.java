package com.swp391.kpi.repository;

import com.swp391.kpi.entity.DuplicationCheck;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Bang duplication_checks (BF-01). Chua co ham rieng: chi dung ham co san cua JpaRepository
 * (save() luc nop bai - ArticleEditorService.doSubmit()).
 *
 * MUC LUC (so dong - ten - tac dung)
 *  (khong co ham - chi khai bao du lieu)
 */
public interface DuplicationCheckRepository extends JpaRepository<DuplicationCheck, Integer> {
}
