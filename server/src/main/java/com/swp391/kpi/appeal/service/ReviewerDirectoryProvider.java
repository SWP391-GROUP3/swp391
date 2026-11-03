package com.swp391.kpi.appeal.service;

import java.util.Optional;

/**
 * Interface tim thong tin BTV, de service BF-03 khong goi thang repository cua luong khac.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   13  findEditor()                    tim editor
 */
public interface ReviewerDirectoryProvider {

    Optional<ReviewerDirectoryEntry> findEditor(Integer editorId);
}
