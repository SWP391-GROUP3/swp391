package com.swp391.kpi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.kpi.dto.*;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.*;
import com.swp391.kpi.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class CrossReviewService {
    private final CrossReviewBatchRepository batches;
    private final CrossReviewTaskRepository tasks;
    private final ScoringReadService scores;
    private final ScoringAppealService scoring;
    private final ArticleReadService articles;
    private final UserReadService users;
    private final AuditService audit;
    private final NotificationService notifications;
    private final EntityManager em;
    private final ObjectMapper json;
    private final ReviewerDeviationRefreshService deviations;

    public List<CrossReviewBatchResponse> getBatches(Integer userId) {
        var head = users.requireActive(userId, "HEAD_OF_DEPARTMENT");
        List<CrossReviewBatchResponse> result = new ArrayList<>();
        for (var batch : batches.findAllByOrderByCreatedAtDescIdDesc())
            if (head.sectionId() != null && head.sectionId().equals(section(batch))) result.add(response(batch));
        return result;
    }

    public CrossReviewBatchResponse getBatch(Integer id, Integer userId) {
        return response(scopedBatch(id, userId, false));
    }

    public List<CrossReviewCandidateResponse> getCandidates(Integer userId, LocalDate from, LocalDate to) {
        var head = users.requireActive(userId, "HEAD_OF_DEPARTMENT");
        validateDates(from, to);
        List<CrossReviewCandidateResponse> result = new ArrayList<>();
        for (var original : scores.getSubmittedAttempts()) {
            if ("APPEAL".equals(original.attemptType()) || original.submittedAt() == null) continue;
            LocalDate day = original.submittedAt().toLocalDate();
            if (day.isBefore(from) || day.isAfter(to)) continue;
            var article = articles.getArticle(original.articleId());
            if (head.sectionId() == null || !head.sectionId().equals(article.sectionId())) continue;
            var card = scoring.getComparisonCard(original.id());
            result.add(new CrossReviewCandidateResponse(original.id(), article.id(), card.title(), original.submittedAt(),
                    eligible(original.articleId(), article.sectionId())));
        }
        return result;
    }

    @Transactional
    public CrossReviewBatchResponse create(CrossReviewBatchCreateRequest request, Integer userId) {
        var head = users.requireActive(userId, "HEAD_OF_DEPARTMENT");
        deviations.lockWorkflow();
        validateDates(request.getPeriodStart(), request.getPeriodEnd());
        String name = request.getBatchName() == null ? "" : request.getBatchName().trim();
        if (name.isEmpty() || name.length() > 100 || request.getTasks() == null || request.getTasks().isEmpty())
            throw new BusinessException("Tên đợt hoặc danh sách bài không hợp lệ.");
        if (!batches.findNameLocked(name).isEmpty()) throw new BusinessException("Tên đợt chấm chéo đã tồn tại.");
        var selected = new HashSet<Integer>();
        var batch = new CrossReviewBatch(); batch.setBatchName(name); batch.setPeriodStart(request.getPeriodStart());
        batch.setPeriodEnd(request.getPeriodEnd()); batch.setCreatedBy(userId); batch.setCreatedAt(LocalDateTime.now());
        batch.setStatus(CrossReviewBatchStatus.OPEN); batches.save(batch);
        for (var selection : request.getTasks()) {
            var original = scores.getAttempt(selection.getOriginalScoringId());
            var article = articles.getArticle(original.articleId());
            if (head.sectionId() == null || !head.sectionId().equals(article.sectionId())) throw forbidden();
            LocalDate day = original.submittedAt().toLocalDate();
            if ("APPEAL".equals(original.attemptType()) || day.isBefore(batch.getPeriodStart()) || day.isAfter(batch.getPeriodEnd())
                    || !selected.add(article.id())) throw new BusinessException("Phiếu gốc không hợp lệ hoặc một bài được chọn nhiều lần.");
            AppealReviewerCandidateResponse candidate = null;
            for (var reviewer : eligible(article.id(), article.sectionId()))
                if (reviewer.reviewerId().equals(selection.getReviewerId())) candidate = reviewer;
            if (candidate == null) throw new BusinessException("Biên tập viên không đủ điều kiện chấm chéo.");
            if (candidate.crossDepartment() && (selection.getReason() == null || selection.getReason().isBlank()))
                throw new BusinessException("Phân công khác ban phải có lý do.");
            var task = new CrossReviewTask(); task.setBatchId(batch.getId()); task.setArticleId(article.id());
            task.setReviewerId(candidate.reviewerId()); task.setOriginalScoringId(original.id()); task.setStatus(CrossReviewTaskStatus.PENDING);
            tasks.save(task);
            Map<String, Object> assignment = new LinkedHashMap<>();
            assignment.put("articleId", article.id()); assignment.put("reviewerId", candidate.reviewerId());
            assignment.put("originalScoringId", original.id()); assignment.put("reason", selection.getReason());
            audit.log(userId, "ASSIGN", "cross_review_tasks", task.getId(), null, encode(assignment));
            notifications.notifyUser(candidate.reviewerId(), article.id(), "Bạn được giao bài chấm chéo #" + task.getId() + ".");
        }
        var result = response(batch);
        audit.log(userId, "CREATE", "cross_review_batches", batch.getId(), null,
                encode(Map.of("sectionId", head.sectionId(), "batch", result)));
        return result;
    }

    public List<AppealReviewTaskResponse> getMyTasks(Integer userId) {
        users.requireActive(userId, "EDITOR");
        List<AppealReviewTaskResponse> result = new ArrayList<>();
        for (var task : tasks.findByReviewerIdOrderByIdDesc(userId)) {
            var card = scoring.getComparisonCard(task.getOriginalScoringId());
            result.add(new AppealReviewTaskResponse(task.getId(), card.title(), task.getSubmittedAt()));
        }
        return result;
    }

    public AppealBlindScorecardResponse getTask(Integer id, Integer userId) {
        users.requireActive(userId, "EDITOR");
        var task = tasks.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy bài chấm chéo."));
        if (!userId.equals(task.getReviewerId())) throw forbidden();
        return blind(task);
    }

    @Transactional
    public AppealBlindScorecardResponse submit(Integer id, Integer userId, AppealScorecardRequest request) {
        users.requireActive(userId, "EDITOR");
        deviations.lockWorkflow();
        var ref = tasks.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy bài chấm chéo."));
        var batch = lockedBatch(ref.getBatchId());
        var task = tasks.findLockedById(id).orElseThrow(() -> new NotFoundException("Bài chấm chéo đã bị gỡ."));
        em.refresh(task, LockModeType.PESSIMISTIC_WRITE);
        if (!userId.equals(task.getReviewerId())) throw forbidden();
        if (batch.getStatus() != CrossReviewBatchStatus.OPEN || task.getStatus() != CrossReviewTaskStatus.PENDING)
            throw new BusinessException("Đợt đã đóng hoặc bài chấm chéo đã nộp.");
        var original = scoring.getComparisonCard(task.getOriginalScoringId());
        Map<Integer, AppealScorecardRequest.Item> incoming = new HashMap<>();
        if (request == null || request.getCriteria() == null) throw new BusinessException("Thiếu phiếu chấm.");
        for (var item : request.getCriteria())
            if (item == null || item.getCriterionId() == null || incoming.put(item.getCriterionId(), item) != null)
                throw new BusinessException("Tiêu chí không hợp lệ hoặc bị trùng.");
        List<AppealScoreCalculator.Item> calculation = new ArrayList<>();
        for (var criterion : original.criteria()) {
            var item = incoming.remove(criterion.criterionId());
            calculation.add(new AppealScoreCalculator.Item(criterion.criterionId(), criterion.weight(),
                    item == null ? null : item.getScore(), item == null ? null : item.getComment()));
        }
        if (!incoming.isEmpty()) throw new BusinessException("Tiêu chí không thuộc phiếu gốc.");
        task.setScore(new AppealScoreCalculator().weightedTotal(calculation, true));
        task.setDeviation(task.getScore().subtract(original.totalScore())); task.setStatus(CrossReviewTaskStatus.DONE);
        task.setSubmittedAt(LocalDateTime.now());
        audit.log(userId, "SUBMIT_SCORE", "cross_review_tasks", id, null, encode(request));
        var article = articles.getArticle(task.getArticleId());
        notifications.notifyHeads(article.id(), article.sectionId(), "Đã có kết quả bài chấm chéo #" + id + ".");
        tasks.flush();
        deviations.refreshSection(task.getSubmittedAt().toLocalDate().withDayOfMonth(1), article.sectionId());
        return blind(task);
    }

    @Transactional
    public CrossReviewBatchResponse close(Integer id, Integer userId) {
        var batch = scopedBatch(id, userId, true);
        requireOpen(batch);
        for (var task : tasks.findByBatchIdOrderById(id)) {
            em.refresh(task, LockModeType.PESSIMISTIC_WRITE);
            if (task.getStatus() != CrossReviewTaskStatus.DONE)
                throw new BusinessException("Không thể đóng đợt khi vẫn còn bài chưa chấm.");
        }
        batch.setStatus(CrossReviewBatchStatus.CLOSED);
        audit.log(userId, "CLOSE", "cross_review_batches", id, "{\"status\":\"OPEN\"}",
                encode(Map.of("status", "CLOSED", "sectionId", section(batch))));
        return response(batch);
    }

    @Transactional
    public CrossReviewBatchResponse removeTask(Integer id, Integer taskId, Integer userId) {
        var batch = scopedBatch(id, userId, true); requireOpen(batch);
        var task = tasks.findLockedById(taskId).orElseThrow(() -> new NotFoundException("Không tìm thấy bài chấm chéo."));
        em.refresh(task, LockModeType.PESSIMISTIC_WRITE);
        if (!id.equals(task.getBatchId()) || task.getStatus() != CrossReviewTaskStatus.PENDING)
            throw new BusinessException("Chỉ gỡ bài chưa nộp thuộc đúng đợt chấm chéo.");
        var before = new CrossReviewBatchResponse.Task(taskId, task.getArticleId(), "", task.getReviewerId(),
                task.getOriginalScoringId(), null, null, task.getStatus().name(), null);
        audit.log(userId, "REMOVE_TASK", "cross_review_batches", id, encode(before), encode(Map.of("sectionId", section(batch))));
        notifications.notifyUser(task.getReviewerId(), task.getArticleId(), "Bài chấm chéo #" + taskId + " đã được gỡ khỏi đợt.");
        tasks.delete(task); tasks.flush();
        return response(batch);
    }

    private List<AppealReviewerCandidateResponse> eligible(Integer articleId, Integer sectionId) {
        var excluded = new HashSet<Integer>();
        for (var attempt : scoring.getArticleAttempts(articleId)) excluded.add(attempt.reviewerId());
        List<AppealReviewerCandidateResponse> home = new ArrayList<>(), other = new ArrayList<>();
        for (var user : users.getUsers()) {
            if (!"ACTIVE".equals(user.status()) || !"EDITOR".equals(user.role()) || user.sectionId() == null || excluded.contains(user.id())) continue;
            boolean cross = !sectionId.equals(user.sectionId());
            (cross ? other : home).add(new AppealReviewerCandidateResponse(user.id(), user.fullName(), user.sectionId(), user.sectionName(), cross));
        }
        return home.isEmpty() ? other : home;
    }

    private CrossReviewBatchResponse response(CrossReviewBatch batch) {
        List<CrossReviewBatchResponse.Task> rows = new ArrayList<>();
        for (var task : tasks.findByBatchIdOrderById(batch.getId())) {
            var card = scoring.getComparisonCard(task.getOriginalScoringId());
            rows.add(new CrossReviewBatchResponse.Task(task.getId(), task.getArticleId(), card.title(), task.getReviewerId(),
                    task.getOriginalScoringId(), task.getScore(), task.getDeviation(), task.getStatus().name(), task.getSubmittedAt()));
        }
        return new CrossReviewBatchResponse(batch.getId(), batch.getBatchName(), batch.getPeriodStart(), batch.getPeriodEnd(),
                batch.getStatus().name(), batch.getCreatedBy(), batch.getCreatedAt(), rows);
    }

    private AppealBlindScorecardResponse blind(CrossReviewTask task) {
        var original = scoring.getComparisonCard(task.getOriginalScoringId());
        List<AppealBlindScorecardResponse.Criterion> rubric = new ArrayList<>();
        for (var item : original.criteria()) rubric.add(new AppealBlindScorecardResponse.Criterion(item.criterionId(), item.name(), item.weight(), null, null));
        return new AppealBlindScorecardResponse(task.getId(), original.articleId(), original.versionId(), original.categoryId(),
                original.criteriaSetVersionId(), original.title(), original.content(), task.getScore(), task.getSubmittedAt(), rubric);
    }

    private CrossReviewBatch scopedBatch(Integer id, Integer userId, boolean lock) {
        var head = users.requireActive(userId, "HEAD_OF_DEPARTMENT");
        if (lock) deviations.lockWorkflow();
        var batch = lock ? lockedBatch(id) : batches.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy đợt chấm chéo."));
        if (head.sectionId() == null || !head.sectionId().equals(section(batch))) throw forbidden();
        return batch;
    }

    private Integer section(CrossReviewBatch batch) {
        var rows = tasks.findByBatchIdOrderById(batch.getId());
        if (!rows.isEmpty()) return articles.getArticle(rows.get(0).getArticleId()).sectionId();
        for (var user : users.getUsers()) if (user.id().equals(batch.getCreatedBy())) return user.sectionId();
        return null;
    }
    private CrossReviewBatch lockedBatch(Integer id) {
        var batch = batches.findLockedById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy đợt chấm chéo."));
        em.refresh(batch, LockModeType.PESSIMISTIC_WRITE); return batch;
    }
    private void requireOpen(CrossReviewBatch batch) {
        if (batch.getStatus() != CrossReviewBatchStatus.OPEN) throw new BusinessException("Đợt chấm chéo đã đóng.");
    }
    private void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) throw new BusinessException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
    }
    private ResponseStatusException forbidden() { return new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem hoặc xử lý dữ liệu chấm chéo này."); }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Không thể ghi nhật ký chấm chéo.", ex); }
    }
}
