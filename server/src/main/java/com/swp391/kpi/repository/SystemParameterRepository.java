package com.swp391.kpi.repository;

import com.swp391.kpi.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Doc bang system_parameters.
 *
 * MUC LUC (so dong)
 *   17  findByFlow()                 - Moi tham so cua 1 luong, vd findByFlow("BF-04")
 */
public interface SystemParameterRepository extends JpaRepository<SystemParameter, Integer> {

    // Moi tham so cua 1 luong, vd findByFlow("BF-04")
    List<SystemParameter> findByFlow(String flow);
}
