package com.swp391.kpi.service;

import java.util.Optional;

/**
 * MUC LUC (so dong)
 *   11  findEditor()
 */
public interface ReviewerDirectoryProvider {

    Optional<ReviewerDirectoryEntry> findEditor(Integer editorId);
}
