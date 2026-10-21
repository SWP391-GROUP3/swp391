package com.swp391.kpi.dto;

public record ReviewerDirectoryResponse(Integer editorId, String editorName, Integer sectionId,
        String sectionName, String status) {}
