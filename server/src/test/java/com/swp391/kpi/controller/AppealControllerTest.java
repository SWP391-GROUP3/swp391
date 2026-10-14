package com.swp391.kpi.controller;

import com.swp391.kpi.dto.AppealEligibilityResponse;
import com.swp391.kpi.dto.AppealResponse;
import com.swp391.kpi.exception.GlobalExceptionHandler;
import com.swp391.kpi.service.AppealService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppealControllerTest {

    @Mock
    private AppealService appealService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AppealController(appealService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void submitAppealReturnsCreatedResponse() throws Exception {
        AppealResponse response = new AppealResponse();
        response.setId(12);
        response.setStatus("SUBMITTED");
        when(appealService.submitAppeal(any(), eq(1))).thenReturn(response);

        mockMvc.perform(post("/api/appeals")
                        .header("X-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "articleId": 1,
                                  "criteria": [
                                    {"criterionId": 4, "reason": "Điểm thời sự chưa phù hợp."}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(12));
    }

    @Test
    void getEligibilityReturnsPublishedScorecard() throws Exception {
        AppealEligibilityResponse response = new AppealEligibilityResponse();
        response.setArticleId(1);
        when(appealService.getEligibility(1, 1)).thenReturn(response);

        mockMvc.perform(get("/api/articles/1/appeal-eligibility")
                        .header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleId").value(1));
    }

    @Test
    void getMyAppealsReturnsCurrentReporterAppeals() throws Exception {
        when(appealService.getMyAppeals(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/appeals/mine")
                        .header("X-User-Id", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
