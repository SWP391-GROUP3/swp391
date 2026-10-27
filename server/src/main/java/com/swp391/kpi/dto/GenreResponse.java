package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Genre;
import lombok.Getter;
import lombok.Setter;

/**
 * 1 the loai dang dung, tra ve cho o chon the loai (vd man soan bai UC-03 cua BF-01).
 * Khong tra base_factor: he so nhuan but la viec cua Thu ky / Ke toan, phong vien khong can.
 *
 * MUC LUC (so dong)
 *   19  field                        - cac truong tra ve cho giao dien
 *   24  from()                       - Map tay entity -> DTO (AGENTS.md muc 3.4)
 */
@Getter
@Setter
public class GenreResponse {

    private Integer id;
    private String code;    // vd PHAN_ANH
    private String name;    // vd "Bai phan anh"

    // Map tay entity -> DTO (AGENTS.md muc 3.4)
    public static GenreResponse from(Genre genre) {
        GenreResponse response = new GenreResponse();
        response.setId(genre.getId());
        response.setCode(genre.getCode());
        response.setName(genre.getName());
        return response;
    }
}
