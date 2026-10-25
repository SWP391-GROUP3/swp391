package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 1 phong vien co the chon lam dong tac gia o man soan bai (UC-04).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  field                           cac truong tra ve cho giao dien
 *   21  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ArticleCoAuthorCandidateResponse {

    private Integer id;
    private String fullName;
    private String sectionName;

    public static ArticleCoAuthorCandidateResponse from(UserAccessResponse user) {
        ArticleCoAuthorCandidateResponse dto = new ArticleCoAuthorCandidateResponse();
        dto.setId(user.id());
        dto.setFullName(user.fullName());
        dto.setSectionName(user.sectionName());
        return dto;
    }
}
