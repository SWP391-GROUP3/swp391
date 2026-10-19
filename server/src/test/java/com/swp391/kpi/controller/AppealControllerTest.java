package com.swp391.kpi.controller;

import com.swp391.kpi.dto.AppealEligibilityResponse;
import com.swp391.kpi.dto.AppealResponse;
import com.swp391.kpi.exception.GlobalExceptionHandler;
import com.swp391.kpi.service.AppealService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
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
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256").subject("1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        mockMvc = MockMvcBuilders.standaloneSetup(new AppealController(appealService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitAppealReturnsCreatedResponse() throws Exception {
        AppealResponse response = new AppealResponse();
        response.setId(12);
        response.setStatus("SUBMITTED");
        when(appealService.submitAppeal(any(), eq(1))).thenReturn(response);

        mockMvc.perform(post("/api/appeals")
                        .header("X-User-Id", 8)
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
                        .header("X-User-Id", 8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleId").value(1));
    }

    @Test
    void getMyAppealsReturnsCurrentReporterAppeals() throws Exception {
        when(appealService.getMyAppeals(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/appeals/mine")
                        .header("X-User-Id", 8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
