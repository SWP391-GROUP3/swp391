package com.swp391.kpi.repository;

import com.swp391.kpi.entity.SystemParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Doc/ghi bang system_parameters.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  findByFlow()                    Moi tham so cua 1 luong, vd findByFlow("BF-04")
 *   22  findByParamKey()                1 tham so theo ten (param_key la UNIQUE), dung khi Admin sua (UC-13)
 */
public interface SystemParameterRepository extends JpaRepository<SystemParameter, Integer> {

    // Moi tham so cua 1 luong, vd findByFlow("BF-04")
    List<SystemParameter> findByFlow(String flow);

    // 1 tham so theo ten (param_key la UNIQUE), dung khi Admin sua (UC-13)
    Optional<SystemParameter> findByParamKey(String paramKey);
}
