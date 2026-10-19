package com.swp391.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MUC LUC (so dong)
 *   18  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditorWorkloadDTO {

    private Integer editorId;
    private long activeArticleCount;
}
