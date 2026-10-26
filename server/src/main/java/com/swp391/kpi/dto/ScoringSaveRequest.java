package com.swp391.kpi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoringSaveRequest {

    @NotEmpty(message = "Danh sách điểm tiêu chí không được để trống")
    @Valid
    private List<CriteriaScoreItemRequest> scores;
}
