package com.swp391.kpi.dto;

public record AppealReviewerCandidateResponse(Integer reviewerId, String reviewerName,
        Integer sectionId, String sectionName, boolean crossDepartment) {}
