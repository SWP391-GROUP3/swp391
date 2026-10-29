package com.swp391.kpi.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Thong tin gon cua 1 BTV (id, ho ten, ban) dung trong BF-03.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  field                           repository / service duoc inject
 */
@Getter
@AllArgsConstructor
public class ReviewerDirectoryEntry {

    private Integer editorId;
    private String editorName;
    private Integer sectionId;
    private String sectionName;
}
