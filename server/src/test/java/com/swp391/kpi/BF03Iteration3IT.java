package com.swp391.kpi;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.jwt.secret=bf03-integration-test-only-secret-32-bytes")
@AutoConfigureMockMvc
@Transactional
class BF03Iteration3IT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired com.swp391.kpi.appeal.service.AppealEffectiveScoreService effectiveScores;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired com.swp391.kpi.appeal.service.AppealReviewService reviewService;
    @Autowired org.springframework.transaction.PlatformTransactionManager txManager;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.swp391.kpi.common.service.NotificationService notifications;

    @Test void blindCardUsesOriginalVersionAndNeverReturnsOldScoringOrAppealInformation() throws Exception {
        var f=fixture(); int reviewer=editor(); int draft=assign(f,reviewer);
        jdbc.update("INSERT INTO article_versions(article_id,version_no,kind,title,content,created_by) VALUES (?,2,'EDITED',N'Later title',N'Later content',1)",f.article);
        var r=mvc.perform(get("/api/appeal-reviews/"+draft).header("Authorization",bearer(reviewer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.versionId").value(f.version))
                .andExpect(jsonPath("$.data.content").value("Frozen content"))
                .andExpect(jsonPath("$.data.criteria[0].score").isEmpty())
                .andExpect(jsonPath("$.data.totalScore").isEmpty()).andReturn();
        String body=r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        for(String hidden:List.of("Lý do bí mật","Original secret","reviewerId","appellantId","appealId","originalScore","comparison"))
            assertFalse(body.contains(hidden),hidden);
        mvc.perform(get("/api/appeal-reviews/"+draft).header("Authorization",bearer(3))).andExpect(status().isForbidden());
        mvc.perform(get("/api/appeal-reviews/"+draft).header("Authorization",bearer(1))).andExpect(status().isForbidden());
        mvc.perform(get("/api/appeal-reviews/"+draft)).andExpect(status().isUnauthorized());
    }

    @Test void draftIsPartialSubmitValidatesAndLocksWithoutPublicationDecision() throws Exception {
        var f=fixture();int reviewer=editor();int draft=assign(f,reviewer);
        var ids=criterionIds(f);
        String partial=json.writeValueAsString(Map.of("criteria",List.of(Map.of("criterionId",ids.get(0),"score",40))));
        mvc.perform(put("/api/appeal-reviews/"+draft).header("Authorization",bearer(reviewer)).contentType("application/json").content(partial))
                .andExpect(status().isOk());
        mvc.perform(post("/api/appeal-reviews/"+draft+"/submit").header("Authorization",bearer(reviewer)).contentType("application/json").content(partial))
                .andExpect(status().isConflict());
        submit(draft,reviewer,body(f,74));
        assertNull(jdbc.queryForObject("SELECT decision FROM scoring_attempts WHERE id=?",String.class,draft));
        assertEquals("74.00",jdbc.queryForObject("SELECT total_score FROM scoring_attempts WHERE id=?",java.math.BigDecimal.class,draft).toPlainString());
        mvc.perform(put("/api/appeal-reviews/"+draft).header("Authorization",bearer(reviewer)).contentType("application/json").content(body(f,90)))
                .andExpect(status().isConflict());
        assertEquals("SCORE_PUBLISHED",jdbc.queryForObject("SELECT status FROM articles WHERE id=?",String.class,f.article));
        assertEquals(70,jdbc.queryForObject("SELECT total_score FROM scoring_attempts WHERE id=?",java.math.BigDecimal.class,f.original).intValue());
    }

    @Test void s1WithinThresholdCanOnlyBeConfirmedOnceByOwnHead() throws Exception {
        var f=fixture();int reviewer=editor();int draft=assign(f,reviewer);submit(draft,reviewer,body(f,75));
        mvc.perform(get("/api/appeals/"+f.appeal+"/comparison").header("Authorization",bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.proposal.effectiveScore").value(75))
                .andExpect(jsonPath("$.data.proposal.rule").value("USE_S1"));
        mvc.perform(get("/api/appeals/"+f.appeal+"/comparison").header("Authorization",bearer(reviewer))).andExpect(status().isForbidden());
        finalizeAppeal(f,draft).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("RESOLVED"));
        finalizeAppeal(f,draft).andExpect(status().isConflict());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM appeal_resolutions WHERE appeal_id=?",Integer.class,f.appeal));
        assertEquals("USE_S1",jdbc.queryForObject("SELECT rule_applied FROM appeal_resolutions WHERE appeal_id=?",String.class,f.appeal));
        assertEquals(5,jdbc.queryForObject("SELECT finalized_by FROM appeal_resolutions WHERE appeal_id=?",Integer.class,f.appeal));
        assertEquals(75,effectiveScores.getEffectiveScore(f.article,new java.math.BigDecimal("70")).intValue());
        mvc.perform(get("/api/appeals/mine").header("Authorization",bearer(1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[?(@.id == "+f.appeal+")].effectiveScore").value(org.hamcrest.Matchers.hasItem(75.0)));
        assertTrue(jdbc.queryForObject("SELECT appeal_count FROM reviewer_deviation_indices WHERE editor_id=3 AND period=?",Integer.class,java.time.LocalDate.now().withDayOfMonth(1))>=1);
        mvc.perform(post("/api/appeals/"+f.appeal+"/assign-second").header("Authorization",bearer(5)).contentType("application/json").content("{\"reviewerId\":"+editor()+"}"))
                .andExpect(status().isConflict());
    }

    @Test void largeVarianceRequiresS2ArbitrationAndNoThirdRound() throws Exception {
        var f=fixture(); int first=editor(); int s1=assign(f,first);submit(s1,first,body(f,90));
        mvc.perform(get("/api/appeals/"+f.appeal+"/comparison").header("Authorization",bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.proposal.requiresSecondReview").value(true));
        finalizeAppeal(f,s1).andExpect(status().isConflict());
        mvc.perform(post("/api/appeals/"+f.appeal+"/assign-second").header("Authorization",bearer(5)).contentType("application/json").content("{\"reviewerId\":"+first+"}"))
                .andExpect(status().isConflict());
        int second=editor(); var assigned=mvc.perform(post("/api/appeals/"+f.appeal+"/assign-second").header("Authorization",bearer(5))
                .contentType("application/json").content("{\"reviewerId\":"+second+"}"))
                .andExpect(status().isOk()).andReturn();
        int s2=json.readTree(assigned.getResponse().getContentAsString()).at("/data/currentAttempt/id").asInt();
        submit(s2,second,body(f,40));
        finalizeAppeal(f,s1).andExpect(status().isConflict());
        finalizeAppeal(f,s2).andExpect(status().isOk());
        assertEquals("70.00",jdbc.queryForObject("SELECT effective_score FROM appeal_resolutions WHERE appeal_id=?",java.math.BigDecimal.class,f.appeal).toPlainString());
        assertEquals("MEDIAN_S0_S1_S2",jdbc.queryForObject("SELECT rule_applied FROM appeal_resolutions WHERE appeal_id=?",String.class,f.appeal));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM scoring_attempts WHERE appeal_id=?",Integer.class,f.appeal));
    }

    @Test void badCriteriaDuplicateAndLowScoreWithoutCommentCannotBeSubmitted() throws Exception {
        var f=fixture();int reviewer=editor();int draft=assign(f,reviewer);
        String item="{\"criterionId\":"+criterionIds(f).get(0)+",\"score\":40}";
        for(String payload:List.of("{\"criteria\":["+item+","+item+"]}","{\"criteria\":[{\"criterionId\":2147483647,\"score\":70}]}",body(f,40).replace("Giải thích", " ")))
            mvc.perform(post("/api/appeal-reviews/"+draft+"/submit").header("Authorization",bearer(reviewer))
                    .contentType("application/json").content(payload)).andExpect(status().isConflict());
        assertNull(jdbc.queryForObject("SELECT submitted_at FROM scoring_attempts WHERE id=?",java.sql.Timestamp.class,draft));
    }

    private org.springframework.test.web.servlet.ResultActions finalizeAppeal(Fixture f,int attempt) throws Exception {
        return mvc.perform(post("/api/appeals/"+f.appeal+"/finalize").header("Authorization",bearer(5))
                .contentType("application/json").content("{\"expectedAttemptId\":"+attempt+"}"));
    }

    @Test void crossReviewIsBlindAnalyticalAndBatchCannotCloseUntilDone() throws Exception {
        var f=fixture();int reviewer=editor();int batch=createBatch(f,reviewer);
        var detail=mvc.perform(get("/api/cross-review-batches/"+batch).header("Authorization",bearer(5)))
                .andExpect(status().isOk()).andReturn();
        int task=json.readTree(detail.getResponse().getContentAsString()).at("/data/tasks/0/id").asInt();
        mvc.perform(post("/api/cross-review-batches/"+batch+"/close").header("Authorization",bearer(5))).andExpect(status().isConflict());
        var card=mvc.perform(get("/api/cross-review-tasks/"+task).header("Authorization",bearer(reviewer)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalScore").isEmpty())
                .andExpect(jsonPath("$.data.criteria[0].score").isEmpty()).andReturn();
        assertFalse(card.getResponse().getContentAsString().contains("Original secret"));
        assertFalse(card.getResponse().getContentAsString().contains("originalScoringId"));
        mvc.perform(post("/api/cross-review-tasks/"+task+"/submit").header("Authorization",bearer(reviewer))
                .contentType("application/json").content(body(f,80)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalScore").value(80));
        assertEquals(10,jdbc.queryForObject("SELECT deviation FROM cross_review_tasks WHERE id=?",java.math.BigDecimal.class,task).intValue());
        assertEquals(70,jdbc.queryForObject("SELECT total_score FROM scoring_attempts WHERE id=?",java.math.BigDecimal.class,f.original).intValue());
        assertEquals("SUBMITTED",jdbc.queryForObject("SELECT status FROM appeals WHERE id=?",String.class,f.appeal));
        mvc.perform(post("/api/cross-review-tasks/"+task+"/submit").header("Authorization",bearer(reviewer))
                .contentType("application/json").content(body(f,90))).andExpect(status().isConflict());
        mvc.perform(post("/api/cross-review-batches/"+batch+"/close").header("Authorization",bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CLOSED"));
        mvc.perform(get("/api/audit-logs").header("Authorization",bearer(5)).param("entityType","cross_review_batches").param("entityId",""+batch))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].actionType",org.hamcrest.Matchers.hasItem("CLOSE")));
        mvc.perform(delete("/api/cross-review-batches/"+batch+"/tasks/"+task).header("Authorization",bearer(5))).andExpect(status().isConflict());
    }

    @Test void invalidCrossReviewAndDuplicateBatchAreRejectedAndRemovalIsAudited() throws Exception {
        var f=fixture();int reviewer=editor();String name="Cross "+UUID.randomUUID();
        String payload=crossBody(f,reviewer,name);
        var created=mvc.perform(post("/api/cross-review-batches").header("Authorization",bearer(5)).contentType("application/json").content(payload))
                .andExpect(status().isCreated()).andReturn();
        int batch=json.readTree(created.getResponse().getContentAsString()).at("/data/id").asInt();
        int task=json.readTree(created.getResponse().getContentAsString()).at("/data/tasks/0/id").asInt();
        mvc.perform(post("/api/cross-review-batches").header("Authorization",bearer(5)).contentType("application/json").content(payload)).andExpect(status().isConflict());
        mvc.perform(post("/api/cross-review-batches").header("Authorization",bearer(5)).contentType("application/json").content(crossBody(f,3,"Invalid "+name)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/cross-review-tasks/"+task).header("Authorization",bearer(3))).andExpect(status().isForbidden());
        mvc.perform(get("/api/cross-review-batches/"+batch).header("Authorization",bearer(reviewer))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/cross-review-batches/"+batch+"/tasks/"+task).header("Authorization",bearer(5)))
                .andExpect(status().isOk());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM cross_review_tasks WHERE id=?",Integer.class,task));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE entity_type='cross_review_batches' AND entity_id=? AND action_type='REMOVE_TASK'",Integer.class,batch));
        mvc.perform(get("/api/audit-logs").header("Authorization",bearer(5)).param("entityType","cross_review_tasks").param("entityId",""+task))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[*].actionType",org.hamcrest.Matchers.hasItem("ASSIGN")));
    }

    @Test void crossReviewDatesDuplicateArticlesAndOtherDepartmentAreRejected() throws Exception {
        var f=fixture();int reviewer=editor();int batch=createBatch(f,reviewer);
        String name="invalid_"+UUID.randomUUID();
        var reversed=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(crossBody(f,reviewer,name));
        reversed.put("periodStart",java.time.LocalDate.now().plusDays(1).toString());
        mvc.perform(post("/api/cross-review-batches").header("Authorization",bearer(5)).contentType("application/json").content(json.writeValueAsString(reversed)))
                .andExpect(status().isConflict());
        var duplicate=(com.fasterxml.jackson.databind.node.ObjectNode)json.readTree(crossBody(f,reviewer,name));
        ((com.fasterxml.jackson.databind.node.ArrayNode)duplicate.get("tasks")).add(duplicate.get("tasks").get(0).deepCopy());
        mvc.perform(post("/api/cross-review-batches").header("Authorization",bearer(5)).contentType("application/json").content(json.writeValueAsString(duplicate)))
                .andExpect(status().isConflict());
        int other=jdbc.queryForObject("INSERT INTO users(username,email,password_hash,full_name,role_id,section_id) OUTPUT INSERTED.id SELECT ?,?,'test-only',N'Head chấm chéo khác ban',id,2 FROM roles WHERE code='HEAD_OF_DEPARTMENT'",Integer.class,name,name+"@test.invalid");
        mvc.perform(get("/api/cross-review-batches/"+batch).header("Authorization",bearer(other))).andExpect(status().isForbidden());
        mvc.perform(post("/api/cross-review-batches/"+batch+"/close").header("Authorization",bearer(other))).andExpect(status().isForbidden());
    }

    private int createBatch(Fixture f,int reviewer) throws Exception {
        var response=mvc.perform(post("/api/cross-review-batches").header("Authorization",bearer(5))
                .contentType("application/json").content(crossBody(f,reviewer,"Cross "+UUID.randomUUID())))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(response.getResponse().getContentAsString()).at("/data/id").asInt();
    }

    @Test void legacyResolutionFallsBackToEffectiveAttemptScore() throws Exception {
        var f=fixture();
        jdbc.update("UPDATE appeals SET status='RESOLVED',resolved_at=SYSDATETIME() WHERE id=?",f.appeal);
        jdbc.update("INSERT INTO appeal_resolutions(appeal_id,rule_applied,effective_attempt_id,finalized_by) VALUES (?,'KEEP_ORIGINAL',?,5)",f.appeal,f.original);
        assertEquals(70,effectiveScores.getEffectiveScore(f.article,new java.math.BigDecimal("80")).intValue());
        mvc.perform(get("/api/appeals/"+f.appeal+"/comparison").header("Authorization",bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.confirmedScore").value(70));
    }

    @Test void reassignedOrLockedEditorCannotReadOrWriteDraft() throws Exception {
        var f=fixture();int first=editor(),second=editor();int draft=assign(f,first);
        mvc.perform(post("/api/appeals/"+f.appeal+"/reassign").header("Authorization",bearer(5)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("attemptId",draft,"expectedReviewerId",first,"reviewerId",second,"reason","Chuyển công việc"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/appeal-reviews/"+draft).header("Authorization",bearer(first))).andExpect(status().isForbidden());
        mvc.perform(post("/api/appeal-reviews/"+draft+"/submit").header("Authorization",bearer(first)).contentType("application/json").content(body(f,75)))
                .andExpect(status().isForbidden());
        jdbc.update("UPDATE users SET status='LOCKED' WHERE id=?",second);
        mvc.perform(get("/api/appeal-reviews/"+draft).header("Authorization",bearer(second))).andExpect(status().isForbidden());
    }

    @Test void boundRuleIsUsedAndInactiveDifferentDepartmentCannotFinalize() throws Exception {
        var f=fixture();int reviewer=editor();int draft=assign(f,reviewer);submit(draft,reviewer,body(f,76));
        jdbc.update("INSERT INTO appeal_rule_configs(appeal_window_days,max_appeals_per_article,variance_threshold,effective_from,updated_by) VALUES (7,1,100,'1799-01-01',8)");
        mvc.perform(get("/api/appeals/"+f.appeal+"/comparison").header("Authorization",bearer(5)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.threshold").value(5))
                .andExpect(jsonPath("$.data.proposal.requiresSecondReview").value(true));
        String name="other_"+UUID.randomUUID().toString().substring(0,10);
        int other=jdbc.queryForObject("INSERT INTO users(username,email,password_hash,full_name,role_id,section_id) OUTPUT INSERTED.id SELECT ?,?,'test-only',N'Head khác ban',id,2 FROM roles WHERE code='HEAD_OF_DEPARTMENT'",Integer.class,name,name+"@test.invalid");
        mvc.perform(get("/api/appeals/"+f.appeal+"/comparison").header("Authorization",bearer(other))).andExpect(status().isForbidden());
    }

    @Test void initialScoringStillRequiresPublicationDecisionAtDatabaseLevel() {
        var f=fixture();
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,()->jdbc.update("UPDATE scoring_attempts SET decision=NULL WHERE id=?",f.original));
    }

    @Test void coauthorCanReadEffectiveScoreButBlindEditorCannot() throws Exception {
        var f=fixture();
        jdbc.update("INSERT INTO article_authors(article_id,user_id,share_percent,confirm_status) VALUES (?,1,80,'ACCEPTED'),(?,2,20,'ACCEPTED')",f.article,f.article);
        mvc.perform(get("/api/articles/"+f.article+"/effective-score").header("Authorization",bearer(2)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.effectiveScore").value(70));
        mvc.perform(get("/api/articles/"+f.article+"/effective-score").header("Authorization",bearer(3))).andExpect(status().isForbidden());
    }

    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void finalizationNotificationFailureRollsBackResolutionStatusAndAudit() {
        int before=jdbc.queryForObject("SELECT COUNT(*) FROM appeal_resolutions",Integer.class);
        int logs=jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs",Integer.class);
        var tx=new org.springframework.transaction.support.TransactionTemplate(txManager);
        var failure=assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(status->{
            var f=fixture();int reviewer=editor();
            try {
                int draft=assign(f,reviewer);submit(draft,reviewer,body(f,75));
                org.mockito.Mockito.doThrow(new IllegalStateException("test notification failure"))
                        .when(notifications).notifyUser(org.mockito.ArgumentMatchers.eq(1),org.mockito.ArgumentMatchers.eq(f.article),org.mockito.ArgumentMatchers.anyString());
                var request=new com.swp391.kpi.appeal.dto.AppealFinalizeRequest();request.setExpectedAttemptId(draft);
                reviewService.finalizeAppeal(f.appeal,5,request);
            } catch(RuntimeException ex) {throw ex;} catch(Exception ex) {throw new IllegalStateException(ex);}
        }));
        assertEquals("test notification failure",failure.getMessage());
        assertEquals(before,jdbc.queryForObject("SELECT COUNT(*) FROM appeal_resolutions",Integer.class));
        assertEquals(logs,jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs",Integer.class));
    }
    private String crossBody(Fixture f,int reviewer,String name) throws Exception {
        return json.writeValueAsString(Map.of("batchName",name,"periodStart",java.time.LocalDate.now().minusDays(1).toString(),
                "periodEnd",java.time.LocalDate.now().toString(),"tasks",List.of(Map.of("originalScoringId",f.original,"reviewerId",reviewer))));
    }
    private void submit(int attempt,int reviewer,String body) throws Exception {
        mvc.perform(post("/api/appeal-reviews/"+attempt+"/submit").header("Authorization",bearer(reviewer)).contentType("application/json").content(body))
                .andExpect(status().isOk());
    }
    private int assign(Fixture f,int reviewer) throws Exception {
        var result=mvc.perform(post("/api/appeals/"+f.appeal+"/assign").header("Authorization",bearer(5))
                .contentType("application/json").content("{\"reviewerId\":"+reviewer+"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).at("/data/currentAttempt/id").asInt();
    }
    private List<Integer> criterionIds(Fixture f) {return jdbc.queryForList("SELECT id FROM criteria WHERE criteria_set_version_id=? ORDER BY sort_order",Integer.class,f.criteriaSet);}
    private String body(Fixture f,int score) throws Exception {
        var criteria=new ArrayList<Map<String,Object>>();
        for(int id:criterionIds(f))criteria.add(Map.of("criterionId",id,"score",score,"comment",score<50?"Giải thích":""));
        return json.writeValueAsString(Map.of("criteria",criteria));
    }
    private Fixture fixture() {
        int article=jdbc.queryForObject("INSERT INTO articles(author_id,section_id,declared_category_id,category_id,title,status) OUTPUT INSERTED.id VALUES (1,1,3,3,N'Iter3 rollback','SCORE_PUBLISHED')",Integer.class);
        int version=jdbc.queryForObject("INSERT INTO article_versions(article_id,version_no,kind,title,content,created_by) OUTPUT INSERTED.id VALUES (?,1,'SUBMISSION',N'Frozen title',N'Frozen content',1)",Integer.class,article);
        int assignment=jdbc.queryForObject("INSERT INTO assignments(article_id,editor_id,assigned_by) OUTPUT INSERTED.id VALUES (?,3,5)",Integer.class,article);
        int set=jdbc.queryForObject("SELECT criteria_set_version_id FROM scoring_attempts WHERE id=1",Integer.class);
        int original=jdbc.queryForObject("INSERT INTO scoring_attempts(article_id,version_id,assignment_id,reviewer_id,attempt_no,criteria_set_version_id,category_id,total_score,decision,submitted_at) OUTPUT INSERTED.id VALUES (?,?,?,3,1,?,3,70,'ACCEPT',SYSDATETIME())",Integer.class,article,version,assignment,set);
        jdbc.update("INSERT INTO score_approvals(scoring_attempt_id,decision,decided_by,announced_at) VALUES (?,'APPROVED',5,SYSDATETIME())",original);
        jdbc.update("INSERT INTO scoring_attempt_details(scoring_attempt_id,criterion_id,score,comment) SELECT ?,id,70,N'Original secret' FROM criteria WHERE criteria_set_version_id=?",original,set);
        int config=jdbc.queryForObject("INSERT INTO appeal_rule_configs(appeal_window_days,max_appeals_per_article,variance_threshold,effective_from,updated_by) OUTPUT INSERTED.id VALUES (7,1,5,?,8)",Integer.class,java.time.LocalDate.of(1800,1,1).plusDays(new Random().nextInt(50000)));
        int appeal=jdbc.queryForObject("INSERT INTO appeals(article_id,scoring_attempt_id,appellant_id,rule_config_id) OUTPUT INSERTED.id VALUES (?,?,1,?)",Integer.class,article,original,config);
        jdbc.update("INSERT INTO appeal_criteria(appeal_id,criterion_id,reason) SELECT ?,MIN(id),N'Lý do bí mật' FROM criteria WHERE criteria_set_version_id=?",appeal,set);
        return new Fixture(article,version,original,set,appeal);
    }
    private int editor() {
        String name="iter3_"+UUID.randomUUID().toString().substring(0,12);
        return jdbc.queryForObject("INSERT INTO users(username,email,password_hash,full_name,role_id,section_id) OUTPUT INSERTED.id SELECT ?,?,'test-only',N'Iter3 BTV',id,1 FROM roles WHERE code='EDITOR'",Integer.class,name,name+"@test.invalid");
    }
    private String bearer(int id) {
        String role=jdbc.queryForObject("SELECT r.code FROM users u JOIN roles r ON r.id=u.role_id WHERE u.id=?",String.class,id);
        var claims=JwtClaimsSet.builder().subject(""+id).claim("role",role).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        return "Bearer "+encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
    private record Fixture(int article,int version,int original,int criteriaSet,int appeal) {}
}
