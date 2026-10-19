package com.swp391.kpi.dto;

import com.swp391.kpi.entity.MediaFactor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 1 loai anh/video va he so cua no, tra ve cho man Media Factor (UC-56).
 *
 * MUC LUC (so dong)
 *   20  field                        - cac truong tra ve cho giao dien
 *   25  from()                       - Map tay entity -> DTO (AGENTS.md muc 3.4)
 */
@Getter
@Setter
public class MediaFactorResponse {

    private Integer id;
    private String mediaKind;
    private BigDecimal factor;

    // Map tay entity -> DTO (AGENTS.md muc 3.4)
    public static MediaFactorResponse from(MediaFactor mediaFactor) {
        MediaFactorResponse response = new MediaFactorResponse();
        response.setId(mediaFactor.getId());
        response.setMediaKind(mediaFactor.getMediaKind());
        response.setFactor(mediaFactor.getFactor());
        return response;
    }
}
