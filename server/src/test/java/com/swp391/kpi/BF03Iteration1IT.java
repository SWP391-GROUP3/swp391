package com.swp391.kpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.jwt.secret=bf03-integration-test-only-secret-32-bytes")
@AutoConfigureMockMvc
@Transactional
class BF03Iteration1IT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager entityManager;
    @Autowired JwtEncoder jwtEncoder;

    private String bearer(Integer userId) {
        String role = jdbc.queryForObject(
                "SELECT r.code FROM users u JOIN roles r ON r.id=u.role_id WHERE u.id=?",
                String.class, userId);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(userId.toString())
                .claim("role", role).issuedAt(now).expiresAt(now.plusSeconds(300)).build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test void realDatabaseEnforcesIdentityRoleAndLockedAccount() throws Exception {
        for (String path : List.of("/api/audit-logs", "/api/reports/reviewer-deviations",
                "/api/appeal-rule-configs", "/api/appeal-rule-configs/active")) {
            mvc.perform(get(path).header("X-User-Id", 8)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false));
            mvc.perform(get(path).header("Authorization", bearer(1))).andExpect(status().isForbidden());
            mvc.perform(get(path).header("Authorization", bearer(8))).andExpect(status().isOk());
        }
        Integer locked = jdbc.queryForObject("""
                INSERT INTO users(username, email, password_hash, full_name, role_id, status)
                OUTPUT INSERTED.id VALUES (?, ?, 'test-only', N'Locked test admin', 6, 'LOCKED')
                """, Integer.class, "bf3_" + UUID.randomUUID(), UUID.randomUUID() + "@test.local");
        mvc.perform(get("/api/appeal-rule-configs").header("Authorization", bearer(locked)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/appeals/mine").header("Authorization", bearer(8)).header("X-User-Id", 1)).andExpect(status().isForbidden());
        mvc.perform(get("/api/appeals/mine")).andExpect(status().isUnauthorized());
    }

    @Test void submitStoresCriteriaBoundConfigAuditAndNotificationInOneTransaction() throws Exception {
        Fixture fixture = fixture(1, 1);
        mvc.perform(get("/api/appeals/eligible-articles").header("Authorization", bearer(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].articleId")
                        .value(org.hamcrest.Matchers.hasItem(fixture.articleId)));
        mvc.perform(get("/api/articles/" + fixture.articleId + "/appeal-eligibility").header("Authorization", bearer(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalScore").value(70));
        mvc.perform(get("/api/articles/" + fixture.articleId + "/appeal-eligibility").header("Authorization", bearer(2)))
                .andExpect(status().isConflict());
        String payload = payload(fixture.articleId, fixture.criterionId);
        mvc.perform(post("/api/appeals").header("Authorization", bearer(1))
                .contentType("application/json").content(payload))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        Integer appealId = jdbc.queryForObject("SELECT id FROM appeals WHERE article_id = ?", Integer.class, fixture.articleId);
        assertNotNull(jdbc.queryForObject("SELECT rule_config_id FROM appeals WHERE id = ?", Integer.class, appealId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM appeal_criteria WHERE appeal_id = ?", Integer.class, appealId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE entity_type='appeals' AND entity_id=?",
                Integer.class, appealId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE article_id=? AND user_id=5",
                Integer.class, fixture.articleId));
        mvc.perform(get("/api/appeals/eligible-articles").header("Authorization", bearer(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].articleId")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(fixture.articleId))));
        mvc.perform(post("/api/appeals").header("Authorization", bearer(1)).contentType("application/json").content(payload))
                .andExpect(status().isConflict());
    }

    @Test void headCannotReadOtherDepartmentAuditEvenWithDirectFilters() throws Exception {
        Fixture fixture = fixture(7, 2);
        mvc.perform(post("/api/appeals").header("Authorization", bearer(7)).contentType("application/json")
                .content(payload(fixture.articleId, fixture.criterionId))).andExpect(status().isCreated());
        Integer appealId = jdbc.queryForObject("SELECT id FROM appeals WHERE article_id = ?", Integer.class, fixture.articleId);
        assertNotNull(appealId, "Expected the newly created appeal ID.");
        mvc.perform(get("/api/audit-logs").param("articleId", fixture.articleId.toString()).header("Authorization", bearer(5)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/audit-logs").param("entityType", "appeals")
                .param("entityId", appealId.toString()).header("Authorization", bearer(5))).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit-logs").param("appealId", appealId.toString()).header("Authorization", bearer(8)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(get("/api/reports/reviewer-deviations").param("sectionId", "2").header("Authorization", bearer(5)))
                .andExpect(status().isForbidden());
    }

    @Test void reportRecalculatesActualResolvedAppealAndCrossReview() throws Exception {
        String name = "bf03_report_" + UUID.randomUUID().toString().substring(0, 10);
        Integer reviewer = jdbc.queryForObject("""
                INSERT INTO users(username,email,password_hash,full_name,role_id,section_id)
                OUTPUT INSERTED.id SELECT ?,?,'test-only',N'BTV báo cáo kiểm thử',id,1
                FROM roles WHERE code='EDITOR'
                """, Integer.class, name, name + "@test.invalid");
        Fixture fixture = fixture(1, 1, reviewer);
        mvc.perform(post("/api/appeals").header("Authorization", bearer(1)).contentType("application/json")
                .content(payload(fixture.articleId, fixture.criterionId))).andExpect(status().isCreated());
        Integer appealId = jdbc.queryForObject("SELECT id FROM appeals WHERE article_id = ?", Integer.class, fixture.articleId);
        var now = LocalDateTime.now();
        jdbc.update("UPDATE appeals SET status='RESOLVED', resolved_at=? WHERE id=?", now, appealId);
        jdbc.update("""
                INSERT INTO appeal_resolutions(appeal_id, rule_applied, effective_attempt_id,
                        effective_score, finalized_by, finalized_at)
                VALUES (?, 'USE_RESCORE', ?, 77.50, 5, ?)
                """, appealId, fixture.attemptId, now);
        Integer batchId = jdbc.queryForObject("""
                INSERT INTO cross_review_batches(batch_name, period_start, period_end, created_by)
                OUTPUT INSERTED.id VALUES (?, ?, ?, 5)
                """, Integer.class, "BF3 test " + UUID.randomUUID(), now.toLocalDate(), now.toLocalDate());
        jdbc.update("""
                INSERT INTO cross_review_tasks(batch_id, article_id, reviewer_id, original_scoring_id,
                    score, deviation, status, submitted_at) VALUES (?, ?, 9, ?, 65, -5, 'DONE', ?)
                """, batchId, fixture.articleId, fixture.attemptId, now);
        entityManager.clear();
        String response = mvc.perform(get("/api/reports/reviewer-deviations")
                .header("Authorization", bearer(5)).param("period", now.toLocalDate().withDayOfMonth(1).toString())
                .param("editorId", reviewer.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var data = json.readTree(response).path("data");
        assertEquals(1, data.size());
        assertEquals(reviewer.intValue(), data.get(0).path("editorId").asInt());
        assertEquals(6.25, data.get(0).path("avgDeviation").asDouble());
        assertEquals(100.0, data.get(0).path("overturnRate").asDouble());
        assertEquals(1, data.get(0).path("appealCount").asInt());
        // Chay lai de chung minh update cung chi so, khong tao trung.
        mvc.perform(get("/api/reports/reviewer-deviations").header("Authorization", bearer(5))
                .param("period", now.toLocalDate().withDayOfMonth(1).toString())).andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM reviewer_deviation_indices WHERE editor_id=? AND period=?",
                Integer.class, reviewer, now.toLocalDate().withDayOfMonth(1)));
    }

    @Test void configurationCreationIsVersionedAuditedAndValidatesInput() throws Exception {
        String config = """
                {"appealWindowDays":7,"maxAppealsPerArticle":1,"varianceThreshold":5,"effectiveFrom":"2099-12-31"}
                """;
        mvc.perform(post("/api/appeal-rule-configs").header("Authorization", bearer(5)).contentType("application/json").content(config))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/appeal-rule-configs").header("Authorization", bearer(8)).contentType("application/json").content(config))
                .andExpect(status().isCreated());
        Integer configId = jdbc.queryForObject("SELECT id FROM appeal_rule_configs WHERE effective_from='2099-12-31'", Integer.class);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE entity_type='appeal_rule_configs' AND entity_id=?",
                Integer.class, configId));
        mvc.perform(post("/api/appeal-rule-configs").header("Authorization", bearer(8)).contentType("application/json").content(config))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/appeal-rule-configs").header("Authorization", bearer(8)).contentType("application/json")
                .content(config.replace("\"varianceThreshold\":5", "\"varianceThreshold\":101")))
                .andExpect(status().isBadRequest());
        assertEquals(1, jdbc.queryForObject("SELECT rule_config_id FROM appeals WHERE id=1", Integer.class));
    }

    private String payload(Integer articleId, Integer criterionId) throws Exception {
        return json.writeValueAsString(Map.of("articleId", articleId, "criteria",
                List.of(Map.of("criterionId", criterionId, "reason", "Lý do thử nghiệm có dấu."))));
    }

    @Test void futureConfigurationDoesNotReplaceActiveRulesOrRebindExistingAppeals() throws Exception {
        Fixture fixture = fixture(1, 1);
        mvc.perform(post("/api/appeals").header("Authorization", bearer(1))
                .contentType("application/json").content(payload(fixture.articleId, fixture.criterionId)))
                .andExpect(status().isCreated());
        Integer boundConfig = jdbc.queryForObject("SELECT rule_config_id FROM appeals WHERE article_id=?",
                Integer.class, fixture.articleId);
        String activeBefore = mvc.perform(get("/api/appeal-rule-configs/active")
                .header("Authorization", bearer(8))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int activeId = json.readTree(activeBefore).path("data").path("id").asInt();
        String future = "{\"appealWindowDays\":3,\"maxAppealsPerArticle\":1,\"varianceThreshold\":9.25,\"effectiveFrom\":\"2099-11-29\"}";
        mvc.perform(post("/api/appeal-rule-configs").header("Authorization", bearer(8))
                .contentType("application/json").content(future)).andExpect(status().isCreated());
        mvc.perform(get("/api/appeal-rule-configs/active").header("Authorization", bearer(8)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(activeId));
        assertEquals(boundConfig, jdbc.queryForObject("SELECT rule_config_id FROM appeals WHERE article_id=?",
                Integer.class, fixture.articleId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE entity_type='appeal_rule_configs'"
                + " AND entity_id=(SELECT id FROM appeal_rule_configs WHERE effective_from='2099-11-29')"
                + " AND old_value IS NOT NULL AND new_value IS NOT NULL", Integer.class));
    }

    @Test void invalidConfigurationValuesAndAuditDateRangeAreRejected() throws Exception {
        String valid = "{\"appealWindowDays\":7,\"maxAppealsPerArticle\":1,\"varianceThreshold\":5,\"effectiveFrom\":\"2099-11-28\"}";
        for (String invalid : List.of(
                valid.replace("\"appealWindowDays\":7", "\"appealWindowDays\":0"),
                valid.replace("\"maxAppealsPerArticle\":1", "\"maxAppealsPerArticle\":0"),
                valid.replace("\"varianceThreshold\":5", "\"varianceThreshold\":-1"),
                valid.replace("\"varianceThreshold\":5", "\"varianceThreshold\":1.234"))) {
            mvc.perform(post("/api/appeal-rule-configs").header("Authorization", bearer(8))
                    .contentType("application/json").content(invalid)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/audit-logs").header("Authorization", bearer(8))
                .param("fromTime", "2026-10-20T00:00:00").param("toTime", "2026-10-19T00:00:00"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/reports/reviewer-deviations").header("Authorization", bearer(8))
                .param("period", "2026-10-02")).andExpect(status().isBadRequest());
    }

    private Fixture fixture(int authorId, int sectionId) {
        return fixture(authorId, sectionId, 3);
    }

    private Fixture fixture(int authorId, int sectionId, int reviewerId) {
        Integer articleId = jdbc.queryForObject("""
                INSERT INTO articles(author_id, section_id, declared_category_id, category_id, title, status)
                OUTPUT INSERTED.id VALUES (?, ?, 3, 3, N'BF3 transaction test', 'SCORE_PUBLISHED')
                """, Integer.class, authorId, sectionId);
        Integer versionId = jdbc.queryForObject("""
                INSERT INTO article_versions(article_id, version_no, kind, title, content, created_by)
                OUTPUT INSERTED.id VALUES (?, 1, 'SUBMISSION', N'BF3 test', N'Noi dung test', ?)
                """, Integer.class, articleId, authorId);
        Integer assignmentId = jdbc.queryForObject("""
                INSERT INTO assignments(article_id, editor_id, assigned_by)
                OUTPUT INSERTED.id VALUES (?, ?, 5)
                """, Integer.class, articleId, reviewerId);
        Integer setId = jdbc.queryForObject("SELECT criteria_set_version_id FROM scoring_attempts WHERE id=1", Integer.class);
        Integer attemptId = jdbc.queryForObject("""
                INSERT INTO scoring_attempts(article_id, version_id, assignment_id, reviewer_id, attempt_no,
                    attempt_type, criteria_set_version_id, category_id, total_score, decision, submitted_at)
                OUTPUT INSERTED.id VALUES (?, ?, ?, ?, 1, 'INITIAL', ?, 3, 70, 'ACCEPT', ?)
                """, Integer.class, articleId, versionId, assignmentId, reviewerId, setId, LocalDateTime.now());
        jdbc.update("""
                INSERT INTO scoring_attempt_details(scoring_attempt_id, criterion_id, score, comment)
                SELECT ?, id, 70, N'Nhan xet test' FROM criteria WHERE criteria_set_version_id=?
                """, attemptId, setId);
        jdbc.update("""
                INSERT INTO score_approvals(scoring_attempt_id, decision, decided_by, announced_at)
                VALUES (?, 'APPROVED', 5, ?)
                """, attemptId, LocalDateTime.now().minusDays(2));
        Integer criterionId = jdbc.queryForObject("SELECT MIN(id) FROM criteria WHERE criteria_set_version_id=?", Integer.class, setId);
        return new Fixture(articleId, attemptId, criterionId);
    }

    private record Fixture(Integer articleId, Integer attemptId, Integer criterionId) {}
}
