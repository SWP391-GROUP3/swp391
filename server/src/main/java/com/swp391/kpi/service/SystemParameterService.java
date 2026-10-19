package com.swp391.kpi.service;

import com.swp391.kpi.entity.SystemParameter;
import com.swp391.kpi.repository.SystemParameterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Cung cap tham so he thong cho cac luong. Luong khac goi service nay,
 * khong goi thang SystemParameterRepository (AGENTS.md muc 2).
 *
 * MUC LUC (so dong)
 *   24  field                        - repository / service duoc inject
 *   28  getValuesByFlow()            - Moi tham so cua 1 luong (cot flow, vd "BF-04") thanh Map param_key -> p…
 */
@Service
@RequiredArgsConstructor
public class SystemParameterService {

    private final SystemParameterRepository systemParameterRepository;

    // Moi tham so cua 1 luong (cot flow, vd "BF-04") thanh Map param_key -> param_value
    @Transactional(readOnly = true)
    public Map<String, String> getValuesByFlow(String flow) {
        Map<String, String> values = new HashMap<>();
        for (SystemParameter parameter : systemParameterRepository.findByFlow(flow)) {
            values.put(parameter.getParamKey(), parameter.getParamValue());
        }
        return values;
    }
}
