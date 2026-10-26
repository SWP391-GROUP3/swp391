package com.swp391.kpi.dto;

import java.time.LocalDateTime;

public record AppealReviewTaskResponse(Integer id, String title, LocalDateTime submittedAt) {}
