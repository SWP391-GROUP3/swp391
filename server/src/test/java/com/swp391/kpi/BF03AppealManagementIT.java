package com.swp391.kpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.kpi.service.ScoringAppealService;
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
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.jwt.secret=bf03-integration-test-only-secret-32-bytes")
@AutoConfigureMockMvc
@Transactional
class BF03AppealManagementIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired ScoringAppealService scoring;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired org.springframework.transaction.PlatformTransactionManager txManager;
    @Autowired com.swp391.kpi.service.AppealManagementService management;
    @Autowired com.swp391.kpi.repository.AppealRepository appealRepository;
    @Autowired com.swp391.kpi.repository.ScoringAttemptRepository attemptRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.swp391.kpi.service.NotificationService notifications;

    @Test void assignmentFreezesSnapshotKeepsOriginalAndNotifiesOnlyAssignedEditor() throws Exception {
        var f = fixture(1); int editor = editor(1);
        var result = mvc.perform(post("/api/appeals/" + f.appeal + "/assign").header("Authorization", bearer(5))
                .contentType("application/json").content(json.writeValueAsString(Map.of("reviewerId", editor))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.data.currentAttempt.reviewerId").value(editor)).andReturn();
        int attempt = json.readTree(result.getResponse().getContentAsString()).at("/data/currentAttempt/id").asInt();
        var row = jdbc.queryForMap("SELECT * FROM scoring_attempts WHERE id=?", attempt);
        assertEquals(f.version, row.get("version_id")); assertEquals(f.assignment, row.get("assignment_id"));
        assertEquals(f.criteriaSet, row.get("criteria_set_version_id")); assertEquals(3, row.get("category_id"));
        assertNull(row.get("submitted_at")); assertNull(row.get("total_score")); assertNull(row.get("decision"));
        assertEquals(3, jdbc.queryForObject("SELECT editor_id FROM assignments WHERE id=?", Integer.class, f.assignment));
        assertEquals("SCORE_PUBLISHED", jdbc.queryForObject("SELECT status FROM articles WHERE id=?", String.class, f.article));
        assertEquals(70, jdbc.queryForObject("SELECT total_score FROM scoring_attempts WHERE id=?", java.math.BigDecimal.class, f.original).intValue());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE article_id=? AND user_id=?", Integer.class, f.article, editor));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE article_id=?", Integer.class, f.article));
        mvc.perform(get("/api/audit-logs").header("Authorization", bearer(5)).param("appealId", ""+f.appeal))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].actionType").value("ASSIGN"));
        mvc.perform(get("/api/audit-logs").header("Authorization", bearer(5)).param("appealId", ""+f.appeal)
                .param("editorId", ""+editor)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(post("/api/appeals/" + f.appeal + "/assign").header("Authorization", bearer(5))
                .contentType("application/json").content("{\"reviewerId\":" + editor + "}"))
                .andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM scoring_attempts WHERE appeal_id=?", Integer.class, f.appeal));
    }

    @Test void headScopeFiltersAndValidationAreEnforcedBeforeReadOrWrite() throws Exception {
        var own = fixture(1); var other = fixture(2);
        mvc.perform(get("/api/appeals")).andExpect(status().isUnauthorized());
        for (int id : new int[]{1, 3, 8}) {
            mvc.perform(get("/api/appeals").header("Authorization", bearer(id))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/appeals/"+other.appeal).header("Authorization", bearer(5))).andExpect(status().isForbidden());
        mvc.perform(get("/api/appeals/"+other.appeal+"/reviewer-candidates").header("Authorization", bearer(5)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/appeals").header("Authorization", bearer(5)).param("articleId", ""+own.article))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mvc.perform(get("/api/appeals").header("Authorization", bearer(5)).param("articleId", ""+other.article))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mvc.perform(get("/api/appeals").header("Authorization", bearer(5)).param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/appeals").header("Authorization", bearer(5)).param("status", "INVALID"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/appeals").header("Authorization", bearer(5)).param("fromDate", "2026-10-20")
                .param("toDate", "2026-10-19")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/appeals/"+own.appeal+"/assign").header("Authorization", bearer(5))
                .contentType("application/json").content("{\"reviewerId\":3}"))
                .andExpect(status().isConflict());
    }

    @Test void reassignmentKeepsRoundAuditsPrivateDraftAndRevokesOldOwner() throws Exception {
        var f = fixture(1); int first = editor(1); int second = editor(1);
        int attempt = assign(f, first);
        jdbc.update("UPDATE scoring_attempts SET total_score=42, decision_note=N'Private old draft' WHERE id=?", attempt);
        jdbc.update("INSERT INTO scoring_attempt_details(scoring_attempt_id,criterion_id,score,comment) SELECT ?,MIN(id),42,N'Private old comment' FROM criteria WHERE criteria_set_version_id=?", attempt, f.criteriaSet);
        em.clear();
        String payload = reassignBody(attempt, first, second, "Cần bàn giao công việc");
        mvc.perform(post("/api/appeals/"+f.appeal+"/reassign").header("Authorization", bearer(5))
                .contentType("application/json").content(payload)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAttempt.id").value(attempt))
                .andExpect(jsonPath("$.data.currentAttempt.attemptNo").value(2))
                .andExpect(jsonPath("$.data.currentAttempt.reviewerId").value(second));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM scoring_attempts WHERE appeal_id=?", Integer.class, f.appeal));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM scoring_attempt_details WHERE scoring_attempt_id=?", Integer.class, attempt));
        assertNull(jdbc.queryForObject("SELECT total_score FROM scoring_attempts WHERE id=?", java.math.BigDecimal.class, attempt));
        String old = jdbc.queryForObject("SELECT old_value FROM audit_logs WHERE entity_type='appeals' AND entity_id=? AND action_type='REASSIGN'", String.class, f.appeal);
        assertTrue(old.contains("Private old draft")); assertTrue(old.contains("Private old comment"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> scoring.requireOwnedDraft(attempt, first));
        assertEquals(second, scoring.requireOwnedDraft(attempt, second).reviewerId());
        mvc.perform(get("/api/reviewers/"+second).header("Authorization", bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.pendingAppealReviewCount").value(1));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE article_id=?", Integer.class, f.article));
        mvc.perform(get("/api/appeals/"+f.appeal+"/reviewer-candidates").header("Authorization", bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].reviewerId",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItems(first, second, 3))));
        mvc.perform(post("/api/appeals/"+f.appeal+"/reassign").header("Authorization", bearer(5))
                .contentType("application/json").content(payload)).andExpect(status().isConflict());
        assertEquals(second, jdbc.queryForObject("SELECT reviewer_id FROM scoring_attempts WHERE id=?", Integer.class, attempt));
    }

    @Test void crossDepartmentRequiresReasonAndNoEligibleHomeEditorAndSubmittedIsImmutable() throws Exception {
        var f = fixture(1); int home = editor(1); int outside = editor(2);
        mvc.perform(post("/api/appeals/"+f.appeal+"/assign").header("Authorization", bearer(5))
                .contentType("application/json").content("{\"reviewerId\":"+outside+",\"reason\":\"ngoại lệ\"}"))
                .andExpect(status().isConflict());
        int attempt = assign(f, home);
        mvc.perform(post("/api/appeals/"+f.appeal+"/reassign").header("Authorization", bearer(5))
                .contentType("application/json").content(reassignBody(attempt, home, outside, " ")))
                .andExpect(status().isBadRequest());
        // Chi fixture rollback: danh dau cac BTV cung ban da tham gia, khong sua tai khoan dung chung.
        jdbc.update("INSERT INTO audit_logs(actor_id,action_type,entity_type,entity_id,new_value) SELECT 5,'ASSIGN','appeals',?,CONCAT('{\"reviewerId\":',id,'}') FROM users WHERE section_id=1 AND id NOT IN (3,?)", f.appeal, home);
        mvc.perform(post("/api/appeals/"+f.appeal+"/reassign").header("Authorization", bearer(5))
                .contentType("application/json").content(reassignBody(attempt, home, outside, "Không còn BTV cùng ban")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.currentAttempt.crossDepartment").value(true));
        jdbc.update("UPDATE scoring_attempts SET total_score=75,decision='ACCEPT',submitted_at=SYSDATETIME() WHERE id=?", attempt);
        em.clear();
        mvc.perform(post("/api/appeals/"+f.appeal+"/reassign").header("Authorization", bearer(5))
                .contentType("application/json").content(reassignBody(attempt, outside, home, "Không hợp lệ")))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void notificationFailureRollsBackDraftStatusAuditAndNotification() {
        int count = jdbc.queryForObject("SELECT COUNT(*) FROM scoring_attempts", Integer.class);
        int logs = jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class);
        int notices = jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Integer.class);
        int[] article = {0};
        org.mockito.Mockito.doThrow(new IllegalStateException("test notification failure"))
                .when(notifications).notifyUser(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.anyString());
        var tx = new org.springframework.transaction.support.TransactionTemplate(txManager);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            var f = fixture(1); article[0] = f.article; int reviewer = editor(1);
            var request = new com.swp391.kpi.dto.AppealAssignRequest(); request.setReviewerId(reviewer);
            management.assign(f.appeal, request, 5);
        }));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM articles WHERE id=?", Integer.class, article[0]));
        assertEquals(count, jdbc.queryForObject("SELECT COUNT(*) FROM scoring_attempts", Integer.class));
        assertEquals(logs, jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class));
        assertEquals(notices, jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Integer.class));
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void failureNotifyingNewReviewerRollsBackReassignmentAndOldReviewerNotice() {
        int logs = jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class);
        int notices = jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Integer.class);
        int attempts = jdbc.queryForObject("SELECT COUNT(*) FROM scoring_attempts", Integer.class);
        var tx = new org.springframework.transaction.support.TransactionTemplate(txManager);
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            var f = fixture(1); int first = editor(1); int second = editor(1);
            var assign = new com.swp391.kpi.dto.AppealAssignRequest(); assign.setReviewerId(first);
            int draft = management.assign(f.appeal, assign, 5).currentAttempt().id();
            org.mockito.Mockito.doThrow(new IllegalStateException("test new reviewer notification failure"))
                    .when(notifications).notifyUser(org.mockito.ArgumentMatchers.eq(second),
                            org.mockito.ArgumentMatchers.eq(f.article), org.mockito.ArgumentMatchers.anyString());
            var request = new com.swp391.kpi.dto.AppealReassignRequest(); request.setAttemptId(draft);
            request.setExpectedReviewerId(first); request.setReviewerId(second); request.setReason("test rollback");
            management.reassign(f.appeal, request, 5);
        }));
        assertEquals(logs, jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs", Integer.class));
        assertEquals(notices, jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Integer.class));
        assertEquals(attempts, jdbc.queryForObject("SELECT COUNT(*) FROM scoring_attempts", Integer.class));
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void databaseLocksSerializeAppealAndAttemptAccessAcrossTransactions() throws Exception {
        assertSerializes(() -> appealRepository.findLockedById(1).orElseThrow());
        assertSerializes(() -> attemptRepository.findLockedById(1).orElseThrow());
    }

    private void assertSerializes(java.util.function.Supplier<?> lock) throws Exception {
        var workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        var acquired = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var started = new java.util.concurrent.CountDownLatch(1);
        var tx = new org.springframework.transaction.support.TransactionTemplate(txManager);
        try {
            var first = workers.submit(() -> tx.executeWithoutResult(status -> {
                status.setRollbackOnly(); lock.get(); acquired.countDown();
                try { if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("lock test timed out"); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
            }));
            assertTrue(acquired.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var second = workers.submit(() -> tx.executeWithoutResult(status -> {
                status.setRollbackOnly(); started.countDown(); lock.get();
            }));
            assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS));
            assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> second.get(200, java.util.concurrent.TimeUnit.MILLISECONDS));
            release.countDown(); first.get(5, java.util.concurrent.TimeUnit.SECONDS);
            second.get(5, java.util.concurrent.TimeUnit.SECONDS);
        } finally { release.countDown(); workers.shutdownNow(); }
    }

    @Test void lockedWrongRoleResolvedAndMalformedRequestsCannotBeAssigned() throws Exception {
        var f = fixture(1); int locked = editor(1);
        jdbc.update("UPDATE users SET status='LOCKED' WHERE id=?", locked);
        for (int candidate : new int[]{locked, 8}) {
            mvc.perform(post("/api/appeals/"+f.appeal+"/assign").header("Authorization", bearer(5))
                    .contentType("application/json").content("{\"reviewerId\":"+candidate+"}"))
                    .andExpect(status().isConflict());
        }
        mvc.perform(post("/api/appeals/"+f.appeal+"/assign").header("Authorization", bearer(5))
                .contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
        jdbc.update("UPDATE appeals SET status='RESOLVED',resolved_at=SYSDATETIME() WHERE id=?", f.appeal);
        em.clear();
        mvc.perform(get("/api/appeals/"+f.appeal).header("Authorization", bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("RESOLVED"));
        mvc.perform(get("/api/appeals/"+f.appeal+"/reviewer-candidates").header("Authorization", bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(post("/api/appeals/"+f.appeal+"/assign").header("Authorization", bearer(5))
                .contentType("application/json").content("{\"reviewerId\":9,\"reason\":\"invalid\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/appeals/999999999").header("Authorization", bearer(5)))
                .andExpect(status().isNotFound());
    }

    @Test void lockingReloadsDraftAlreadyCachedBeforeAnotherWriterSubmittedIt() throws Exception {
        var f = fixture(1); int first = editor(1); int next = editor(1);
        int draft = assign(f, first);
        scoring.getArticleAttempts(f.article);
        jdbc.update("UPDATE scoring_attempts SET submitted_at=SYSDATETIME(),total_score=76,decision='ACCEPT',decision_note=N'Latest committed note' WHERE id=?", draft);
        assertThrows(com.swp391.kpi.exception.BusinessException.class,
                () -> scoring.reassignDraft(f.appeal, f.article, draft, first, next, false, "stale draft test", 5));
        assertEquals(first, jdbc.queryForObject("SELECT reviewer_id FROM scoring_attempts WHERE id=?", Integer.class, draft));
        assertEquals(76, jdbc.queryForObject("SELECT total_score FROM scoring_attempts WHERE id=?", java.math.BigDecimal.class, draft).intValue());
        assertEquals("Latest committed note", jdbc.queryForObject("SELECT decision_note FROM scoring_attempts WHERE id=?", String.class, draft));
    }

    private int assign(Fixture f, int editor) throws Exception {
        var result = mvc.perform(post("/api/appeals/"+f.appeal+"/assign").header("Authorization", bearer(5))
                .contentType("application/json").content("{\"reviewerId\":"+editor+"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).at("/data/currentAttempt/id").asInt();
    }

    private String reassignBody(int attempt, int old, int next, String reason) throws Exception {
        return json.writeValueAsString(Map.of("attemptId",attempt,"expectedReviewerId",old,"reviewerId",next,"reason",reason));
    }

    private Fixture fixture(int section) {
        int article = jdbc.queryForObject("INSERT INTO articles(author_id,section_id,declared_category_id,category_id,title,status) OUTPUT INSERTED.id VALUES (1,?,3,3,N'BF03 assignment rollback test','SCORE_PUBLISHED')", Integer.class, section);
        int version = jdbc.queryForObject("INSERT INTO article_versions(article_id,version_no,kind,title,content,created_by) OUTPUT INSERTED.id VALUES (?,1,'SUBMISSION',N'Test',N'Test content',1)", Integer.class, article);
        int assignment = jdbc.queryForObject("INSERT INTO assignments(article_id,editor_id,assigned_by) OUTPUT INSERTED.id VALUES (?,3,5)", Integer.class, article);
        int set = jdbc.queryForObject("SELECT criteria_set_version_id FROM scoring_attempts WHERE id=1", Integer.class);
        int original = jdbc.queryForObject("INSERT INTO scoring_attempts(article_id,version_id,assignment_id,reviewer_id,attempt_no,criteria_set_version_id,category_id,total_score,decision,submitted_at) OUTPUT INSERTED.id VALUES (?,?,?,3,1,?,3,70,'ACCEPT',SYSDATETIME())", Integer.class, article, version, assignment, set);
        jdbc.update("INSERT INTO score_approvals(scoring_attempt_id,decision,decided_by,announced_at) VALUES (?,'APPROVED',5,SYSDATETIME())", original);
        jdbc.update("INSERT INTO scoring_attempt_details(scoring_attempt_id,criterion_id,score) SELECT ?,id,70 FROM criteria WHERE criteria_set_version_id=?", original, set);
        int config = jdbc.queryForObject("SELECT TOP 1 id FROM appeal_rule_configs ORDER BY effective_from", Integer.class);
        int appeal = jdbc.queryForObject("INSERT INTO appeals(article_id,scoring_attempt_id,appellant_id,rule_config_id) OUTPUT INSERTED.id VALUES (?,?,1,?)", Integer.class, article, original, config);
        jdbc.update("INSERT INTO appeal_criteria(appeal_id,criterion_id,reason) SELECT ?, MIN(id),N'Lý do khiếu nại thử nghiệm' FROM criteria WHERE criteria_set_version_id=?", appeal, set);
        return new Fixture(article, version, assignment, original, set, appeal);
    }

    private int editor(int section) {
        String name = "bf03_"+UUID.randomUUID().toString().substring(0,12);
        return jdbc.queryForObject("INSERT INTO users(username,email,password_hash,full_name,role_id,section_id) OUTPUT INSERTED.id SELECT ?,?,'test-only',N'BTV kiểm thử',id,? FROM roles WHERE code='EDITOR'", Integer.class, name, name+"@test.invalid", section);
    }

    private String bearer(int id) {
        String role = jdbc.queryForObject("SELECT r.code FROM users u JOIN roles r ON r.id=u.role_id WHERE u.id=?", String.class, id);
        var claims = JwtClaimsSet.builder().subject(""+id).claim("role", role).issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300)).build();
        return "Bearer "+encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private record Fixture(int article, int version, int assignment, int original, int criteriaSet, int appeal) {}
}
