package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ReviewerDeviationIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewerDeviationRepository extends JpaRepository<ReviewerDeviationIndex, Integer> {

    List<ReviewerDeviationIndex> findAllByOrderByPeriodDescEditorIdAsc();
}
