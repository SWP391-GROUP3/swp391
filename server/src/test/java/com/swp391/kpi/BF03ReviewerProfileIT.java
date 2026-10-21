package com.swp391.kpi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.jwt.secret=bf03-integration-test-only-secret-32-bytes")
@AutoConfigureMockMvc
@Transactional
class BF03ReviewerProfileIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtEncoder encoder;

    @Test void profileAndHistoryRespectJwtAndDepartmentAndDoNotWriteData() throws Exception {
        Integer logs = jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class);
        Integer indices = jdbc.queryForObject("SELECT COUNT(*) FROM reviewer_deviation_indices", Integer.class);
        mvc.perform(get("/api/reviewers/3")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reviewers/3").header("Authorization", bearer(1)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reviewers").header("Authorization", bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].sectionId")
                        .value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(1))));
        mvc.perform(get("/api/reviewers/3").header("Authorization", bearer(5)).param("period", "2026-09-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.editorId").value(3))
                .andExpect(jsonPath("$.data.sectionId").value(1))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        mvc.perform(get("/api/reviewers/9").header("Authorization", bearer(5)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reviewers/9").header("Authorization", bearer(8)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.editorId").value(9));
        mvc.perform(get("/api/reviewers/3/scoring-history").header("Authorization", bearer(5))
                .param("period", "2026-09-01").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.size").value(1));
        mvc.perform(get("/api/reviewers/9/scoring-history").header("Authorization", bearer(5)))
                .andExpect(status().isForbidden());
        assertEquals(logs, jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class));
        assertEquals(indices, jdbc.queryForObject("SELECT COUNT(*) FROM reviewer_deviation_indices", Integer.class));
    }

    @Test void invalidMonthPageAndMissingEditorReturnExpectedErrors() throws Exception {
        mvc.perform(get("/api/reviewers/999999").header("Authorization", bearer(8)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/reviewers/3").header("Authorization", bearer(8)).param("period", "2026-10-02"))
                .andExpect(status().isBadRequest());
        for (String value : new String[]{"0", "101"}) {
            mvc.perform(get("/api/reviewers/3/scoring-history").header("Authorization", bearer(8))
                    .param("size", value)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/reviewers/3/scoring-history").header("Authorization", bearer(8))
                .param("page", "-1")).andExpect(status().isBadRequest());
    }

    private String bearer(int id) {
        String role = jdbc.queryForObject("SELECT r.code FROM users u JOIN roles r ON r.id=u.role_id WHERE u.id=?",
                String.class, id);
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(String.valueOf(id)).claim("role", role)
                .issuedAt(now).expiresAt(now.plusSeconds(300)).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
