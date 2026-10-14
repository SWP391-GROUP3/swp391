package com.swp391.kpi.dto;

public record UserAccessResponse(Integer id, String fullName, String role, Integer sectionId,
                                 String sectionName, String status) {}
