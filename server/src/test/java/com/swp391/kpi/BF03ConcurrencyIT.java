package com.swp391.kpi;

import com.swp391.kpi.appeal.dto.AppealBlindScorecardResponse;
import com.swp391.kpi.appeal.dto.AppealReassignRequest;
import com.swp391.kpi.appeal.dto.AppealScorecardRequest;
import com.swp391.kpi.appeal.service.AppealManagementService;
import com.swp391.kpi.appeal.service.AppealReviewService;
import com.swp391.kpi.appeal.service.CrossReviewService;
import com.swp391.kpi.appeal.service.ReviewerDeviationRefreshService;
import com.swp391.kpi.appeal.service.ScoringAppealService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties="app.jwt.secret=bf03-integration-test-only-secret-32-bytes")
class BF03ConcurrencyIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired AppealReviewService reviews;
    @Autowired AppealManagementService management;
    @Autowired CrossReviewService crossReviews;
    @MockitoSpyBean ScoringAppealService scoring;
    @MockitoSpyBean ReviewerDeviationRefreshService deviations;

    @Test void ownedReadAndReassignmentAreAtomicAcrossConnections() throws Exception {
        var f=seed(true);
        var reading=new CountDownLatch(1);var release=new CountDownLatch(1);
        var readerThread=new AtomicReference<Thread>();
        doAnswer(call->{
            if(Thread.currentThread()==readerThread.get()) {reading.countDown();assertTrue(release.await(8,TimeUnit.SECONDS));}
            return call.callRealMethod();
        }).when(scoring).getComparisonCard(f.draft);
        var pool=Executors.newFixedThreadPool(2);
        Future<AppealBlindScorecardResponse> reader=null;Future<?> writer=null;
        try {
            reader=pool.submit(()->{readerThread.set(Thread.currentThread());return reviews.getCard(f.draft,f.first);});
            assertTrue(reading.await(5,TimeUnit.SECONDS));
            writer=pool.submit(()->{
                var request=new AppealReassignRequest();request.setAttemptId(f.draft);request.setExpectedReviewerId(f.first);
                request.setReviewerId(f.second);request.setReason("Race regression");
                management.reassign(f.appeal,request,5);
                reviews.save(f.draft,f.second,body(f,77),false);
            });
            final Future<?> writing=writer;
            assertThrows(TimeoutException.class,()->writing.get(500,TimeUnit.MILLISECONDS));
            release.countDown();
            var card=reader.get(8,TimeUnit.SECONDS);
            assertNull(card.totalScore());assertTrue(card.criteria().stream().allMatch(c->c.score()==null));
            writer.get(8,TimeUnit.SECONDS);
        } finally {
            release.countDown();settle(reader);settle(writer);pool.shutdownNow();pool.awaitTermination(8,TimeUnit.SECONDS);cleanup(f);
        }
    }

    @Test void twoCrossSubmissionsSerializeBeforeWorkflowLocksAndKeepCompleteAggregate() throws Exception {
        var f=seed(false);
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var secondEntered=new CountDownLatch(1);
        var calls=new AtomicInteger();
        doAnswer(call->{
            if(calls.incrementAndGet()==1) {entered.countDown();assertTrue(release.await(8,TimeUnit.SECONDS));}
            else secondEntered.countDown();
            return call.callRealMethod();
        }).when(deviations).refreshSection(any(LocalDate.class),eq(1));
        var pool=Executors.newFixedThreadPool(2);Future<?> first=null,second=null;
        try {
            first=pool.submit(()->crossReviews.submit(f.tasks.get(0),f.first,body(f,80)));
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            second=pool.submit(()->crossReviews.submit(f.tasks.get(1),f.second,body(f,90)));
            assertFalse(secondEntered.await(500,TimeUnit.MILLISECONDS),"Second refresh entered while first workflow still owns its task write lock");
            release.countDown();first.get(8,TimeUnit.SECONDS);second.get(8,TimeUnit.SECONDS);
            assertEquals(new BigDecimal("15.00"),jdbc.queryForObject("SELECT avg_deviation FROM reviewer_deviation_indices WHERE editor_id=? AND period=?",BigDecimal.class,f.originalReviewer,LocalDate.now().withDayOfMonth(1)));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM cross_review_tasks WHERE original_scoring_id=? AND status='DONE'",Integer.class,f.original));
            assertEquals(70,jdbc.queryForObject("SELECT total_score FROM scoring_attempts WHERE id=?",BigDecimal.class,f.original).intValue());
        } finally {
            release.countDown();settle(first);settle(second);pool.shutdownNow();pool.awaitTermination(8,TimeUnit.SECONDS);cleanup(f);
        }
    }

    private void settle(Future<?> future) {if(future!=null)try{future.get(10,TimeUnit.SECONDS);}catch(Exception ignored){}}

    private Fixture seed(boolean draftNeeded) {
        return new TransactionTemplate(transactions).execute(status->{
            int first=editor(),second=editor(),originalReviewer=editor();
            int article=jdbc.queryForObject("INSERT INTO articles(author_id,section_id,declared_category_id,category_id,title,status) OUTPUT INSERTED.id VALUES (1,1,3,3,N'BF03 isolated concurrency fixture','SCORE_PUBLISHED')",Integer.class);
            int version=jdbc.queryForObject("INSERT INTO article_versions(article_id,version_no,kind,title,content,created_by) OUTPUT INSERTED.id VALUES (?,1,'SUBMISSION',N'Race',N'Frozen',1)",Integer.class,article);
            int assignment=jdbc.queryForObject("INSERT INTO assignments(article_id,editor_id,assigned_by) OUTPUT INSERTED.id VALUES (?,?,5)",Integer.class,article,originalReviewer);
            int set=jdbc.queryForObject("SELECT criteria_set_version_id FROM scoring_attempts WHERE id=1",Integer.class);
            int original=jdbc.queryForObject("INSERT INTO scoring_attempts(article_id,version_id,assignment_id,reviewer_id,attempt_no,criteria_set_version_id,category_id,total_score,decision,submitted_at) OUTPUT INSERTED.id VALUES (?,?,?,?,1,?,3,70,'ACCEPT',SYSDATETIME())",Integer.class,article,version,assignment,originalReviewer,set);
            jdbc.update("INSERT INTO scoring_attempt_details(scoring_attempt_id,criterion_id,score) SELECT ?,id,70 FROM criteria WHERE criteria_set_version_id=?",original,set);
            jdbc.update("INSERT INTO score_approvals(scoring_attempt_id,decision,decided_by,announced_at) VALUES (?,'APPROVED',5,SYSDATETIME())",original);
            int config=jdbc.queryForObject("SELECT TOP 1 id FROM appeal_rule_configs ORDER BY id",Integer.class);
            int appeal=jdbc.queryForObject("INSERT INTO appeals(article_id,scoring_attempt_id,appellant_id,rule_config_id,status) OUTPUT INSERTED.id VALUES (?,?,1,?,'IN_REVIEW')",Integer.class,article,original,config);
            int draft=0;
            if(draftNeeded)draft=jdbc.queryForObject("INSERT INTO scoring_attempts(article_id,version_id,assignment_id,reviewer_id,attempt_no,attempt_type,appeal_id,criteria_set_version_id,category_id) OUTPUT INSERTED.id VALUES (?,?,?,?,2,'APPEAL',?,?,3)",Integer.class,article,version,assignment,first,appeal,set);
            List<Integer> batches=new ArrayList<>(),tasks=new ArrayList<>();
            if(!draftNeeded)for(int reviewer:List.of(first,second)){
                int batch=jdbc.queryForObject("INSERT INTO cross_review_batches(batch_name,period_start,period_end,created_by) OUTPUT INSERTED.id VALUES (?,?,?,5)",Integer.class,"race_"+UUID.randomUUID(),LocalDate.now(),LocalDate.now());batches.add(batch);
                tasks.add(jdbc.queryForObject("INSERT INTO cross_review_tasks(batch_id,article_id,reviewer_id,original_scoring_id) OUTPUT INSERTED.id VALUES (?,?,?,?)",Integer.class,batch,article,reviewer,original));
            }
            return new Fixture(article,version,assignment,original,appeal,draft,set,first,second,originalReviewer,batches,tasks);
        });
    }
    private int editor(){String name="race_"+UUID.randomUUID().toString().substring(0,12);return jdbc.queryForObject("INSERT INTO users(username,email,password_hash,full_name,role_id,section_id) OUTPUT INSERTED.id SELECT ?,?,'test-only',N'Race isolated editor',id,1 FROM roles WHERE code='EDITOR'",Integer.class,name,name+"@test.invalid");}
    private AppealScorecardRequest body(Fixture f,int score){
        var request=new AppealScorecardRequest();var rows=new ArrayList<AppealScorecardRequest.Item>();
        for(int id:jdbc.queryForList("SELECT id FROM criteria WHERE criteria_set_version_id=?",Integer.class,f.set)){
            var item=new AppealScorecardRequest.Item();item.setCriterionId(id);item.setScore(BigDecimal.valueOf(score));rows.add(item);
        }request.setCriteria(rows);return request;
    }
    private void cleanup(Fixture f){
        new TransactionTemplate(transactions).executeWithoutResult(status->{
            for(int task:f.tasks)jdbc.update("DELETE FROM audit_logs WHERE entity_type='cross_review_tasks' AND entity_id=?",task);
            jdbc.update("DELETE FROM cross_review_tasks WHERE article_id=?",f.article);
            for(int batch:f.batches)jdbc.update("DELETE FROM cross_review_batches WHERE id=?",batch);
            jdbc.update("DELETE FROM reviewer_deviation_indices WHERE editor_id IN (?,?,?)",f.first,f.second,f.originalReviewer);
            jdbc.update("DELETE FROM notifications WHERE article_id=?",f.article);
            jdbc.update("DELETE FROM audit_logs WHERE entity_type='appeals' AND entity_id=?",f.appeal);
            jdbc.update("DELETE FROM scoring_attempt_details WHERE scoring_attempt_id IN (SELECT id FROM scoring_attempts WHERE article_id=?)",f.article);
            jdbc.update("DELETE FROM scoring_attempts WHERE article_id=? AND attempt_type='APPEAL'",f.article);
            jdbc.update("DELETE FROM appeals WHERE id=?",f.appeal);
            jdbc.update("DELETE FROM score_approvals WHERE scoring_attempt_id=?",f.original);
            jdbc.update("DELETE FROM scoring_attempts WHERE id=?",f.original);
            jdbc.update("DELETE FROM assignments WHERE id=?",f.assignment);
            jdbc.update("DELETE FROM article_versions WHERE id=?",f.version);
            jdbc.update("DELETE FROM articles WHERE id=?",f.article);
            jdbc.update("DELETE FROM users WHERE id IN (?,?,?)",f.first,f.second,f.originalReviewer);
        });
    }
    private record Fixture(int article,int version,int assignment,int original,int appeal,int draft,int set,int first,int second,int originalReviewer,List<Integer>batches,List<Integer>tasks){}
}
