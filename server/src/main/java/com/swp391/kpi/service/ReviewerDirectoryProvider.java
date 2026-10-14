package com.swp391.kpi.service;

import java.util.Optional;

public interface ReviewerDirectoryProvider {

    Optional<ReviewerDirectoryEntry> findEditor(Integer editorId);
}
