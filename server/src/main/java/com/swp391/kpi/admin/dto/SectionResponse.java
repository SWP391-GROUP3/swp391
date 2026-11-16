package com.swp391.kpi.admin.dto;

import com.swp391.kpi.admin.entity.Section;
import lombok.Getter;
import lombok.Setter;

/**
 * 1 ban trong danh sach chon (form tao tai khoan).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  field                           cac truong tra ve cho giao dien
 *   24  from()                          doi entity Section -> DTO
 */
@Getter
@Setter
public class SectionResponse {

    private Integer id;
    private String code;
    private String name;

    // doi entity Section -> DTO
    public static SectionResponse from(Section section) {
        SectionResponse dto = new SectionResponse();
        dto.setId(section.getId());
        dto.setCode(section.getCode());
        dto.setName(section.getName());
        return dto;
    }
}
