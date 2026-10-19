package com.swp391.kpi.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MUC LUC (so dong)
 *   14  field                        - repository / service duoc inject
 */
@Getter
@AllArgsConstructor
public class ReviewerDirectoryEntry {

    private Integer editorId;
    private String editorName;
    private Integer sectionId;
    private String sectionName;
}
