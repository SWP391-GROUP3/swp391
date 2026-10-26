package com.swp391.kpi.repository;

import com.swp391.kpi.entity.DuplicationCheck;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Bang duplication_checks (BF-01). Chua co ham rieng: chi dung ham co san cua JpaRepository
 * (save() luc nop bai - ArticleEditorService.doSubmit()).
 */
public interface DuplicationCheckRepository extends JpaRepository<DuplicationCheck, Integer> {
}
