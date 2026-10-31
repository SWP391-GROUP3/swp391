package com.swp391.kpi.service;

import com.swp391.kpi.dto.ScoringReadResponse;
import com.swp391.kpi.entity.AppealStatus;
import com.swp391.kpi.entity.CrossReviewTaskStatus;
import com.swp391.kpi.entity.ReviewerDeviationIndex;
import com.swp391.kpi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Tinh lai va luu bang reviewer_deviation_indices theo ky / theo ban (UC-41), co khoa de khong chay
 * trung.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   37  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   47  lockWorkflow()                  ponytail: khoa ghi BF03 toan DB; tach tong hop rieng khi so luong ghi tang cao
 *   59  refresh()                       lam moi
 *   67  refreshSection()                Chi workflow BF03 da kiem tra quyen duoc goi; khong co endpoint nhan sectionId tu client
 *  141  getPeriods()                    lay periods
 *  ---- Ham phu (private, chi dung trong file) ----
 *   73  calculatePeriod()               tinh period
 *  154  inScope()                       in scope
 *  159  inPeriod()                      in period
 *  163  addDifference()                 them difference
 */
@Service @RequiredArgsConstructor
public class ReviewerDeviationRefreshService {
    private final UserReadService userReadService;
    private final ScoringReadService scoringReadService;
    private final AppealRepository appealRepository;
    private final AppealResolutionRepository resolutionRepository;
    private final CrossReviewTaskRepository crossReviewTaskRepository;
    private final ReviewerDeviationRepository indexRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    // ponytail: khoa ghi BF03 toan DB; tach tong hop rieng khi so luong ghi tang cao.
    @Transactional
    public void lockWorkflow() {
        Integer result = jdbc.queryForObject("""
                DECLARE @result INT;
                EXEC @result = sys.sp_getapplock @Resource=N'BF03_SCORE_WRITES',
                    @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=15000;
                SELECT @result;
                """, Integer.class);
        if (result == null || result < 0)
            throw new com.swp391.kpi.exception.BusinessException("Đang có thao tác chất lượng chấm khác. Vui lòng thử lại.");
    }

    @Transactional
    public Set<Integer> refresh(LocalDate period, Integer currentUserId) {
        var user = userReadService.requireActive(currentUserId, "ADMIN", "HEAD_OF_DEPARTMENT");
        lockWorkflow();
        return calculatePeriod(period, user.role(), user.sectionId());
    }

    // Chi workflow BF03 da kiem tra quyen duoc goi; khong co endpoint nhan sectionId tu client.
    @Transactional
    public Set<Integer> refreshSection(LocalDate period, Integer sectionId) {
        if (sectionId == null) throw new IllegalArgumentException("Missing department");
        lockWorkflow();
        return calculatePeriod(period, "HEAD_OF_DEPARTMENT", sectionId);
    }

    // Tinh va luu chi so cua ky; tra ve cac BTV co du lieu so sanh.
    private Set<Integer> calculatePeriod(LocalDate period, String role, Integer sectionId) {
        var calculated = computeIndexes(period, role, sectionId);
        var existing = indexRepository.findAllByOrderByPeriodDescEditorIdAsc();
        for (var entry : calculated.entrySet()) {
            ReviewerDeviationIndex target = null;
            for (var index : existing) {
                if (period.equals(index.getPeriod()) && entry.getKey().equals(index.getEditorId())) target = index;
            }
            if (target == null) target = new ReviewerDeviationIndex();
            var value = entry.getValue();
            target.setEditorId(value.getEditorId());
            target.setPeriod(period);
            target.setAvgDeviation(value.getAvgDeviation());
            target.setAppealCount(value.getAppealCount());
            target.setOverturnRate(value.getOverturnRate());
            target.setWorkloadCount(value.getWorkloadCount());
            target.setLastUpdated(value.getLastUpdated());
            indexRepository.save(target);
        }
        return calculated.keySet();
    }

    // Cach tinh UC-41 duy nhat: chi doc, khong ghi DB. Bao cao va ho so BTV (UC-42) cung dung ham nay.
    public Map<Integer, ReviewerDeviationIndex> computeIndexes(LocalDate period, String role, Integer sectionId) {
        if (period == null || period.getDayOfMonth() != 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kỳ báo cáo phải là ngày đầu tháng.");
        var attempts = scoringReadService.getSubmittedAttempts();
        Map<Integer, Integer> reviewerSections = new HashMap<>();
        for (var reviewer : userReadService.getUsers()) {
            if ("EDITOR".equals(reviewer.role())) reviewerSections.put(reviewer.id(), reviewer.sectionId());
        }
        Map<Integer, ScoringReadResponse> byId = new HashMap<>();
        for (var attempt : attempts) byId.put(attempt.id(), attempt);
        Map<Integer, List<BigDecimal>> differences = new HashMap<>();
        Map<Integer, Integer> appealCounts = new HashMap<>();
        Map<Integer, Integer> overturned = new HashMap<>();
        Map<Integer, Integer> workload = new HashMap<>();

        for (var task : crossReviewTaskRepository.findAll()) {
            if (task.getStatus() != CrossReviewTaskStatus.DONE || !inPeriod(task.getSubmittedAt(), period)) continue;
            var original = byId.get(task.getOriginalScoringId());
            if (original == null || !inScope(role, sectionId, original.reviewerId(), reviewerSections)) continue;
            addDifference(differences, original.reviewerId(), task.getScore().subtract(original.totalScore()));
        }
        for (var resolution : resolutionRepository.findAll()) {
            if (!inPeriod(resolution.getFinalizedAt(), period)) continue;
            var appeal = appealRepository.findById(resolution.getAppealId()).orElse(null);
            if (appeal == null || appeal.getStatus() != AppealStatus.RESOLVED) continue;
            var original = byId.get(appeal.getScoringAttemptId());
            if (original == null || !inScope(role, sectionId, original.reviewerId(), reviewerSections)) continue;
            BigDecimal effective = resolution.getEffectiveScore();
            if (effective == null) {
                var effectiveAttempt = byId.get(resolution.getEffectiveAttemptId());
                if (effectiveAttempt == null) continue;
                effective = effectiveAttempt.totalScore();
            }
            var difference = effective.subtract(original.totalScore());
            addDifference(differences, original.reviewerId(), difference);
            appealCounts.merge(original.reviewerId(), 1, Integer::sum);
            if (difference.signum() != 0) overturned.merge(original.reviewerId(), 1, Integer::sum);
        }
        for (var attempt : attempts) {
            if (!"APPEAL".equals(attempt.attemptType()) && inPeriod(attempt.submittedAt(), period)
                    && inScope(role, sectionId, attempt.reviewerId(), reviewerSections)) {
                workload.merge(attempt.reviewerId(), 1, Integer::sum);
            }
        }
        Map<Integer, ReviewerDeviationIndex> result = new HashMap<>();
        for (var entry : differences.entrySet()) {
            Integer editorId = entry.getKey();
            var calculated = new ReviewerDeviationCalculator().calculate(entry.getValue(),
                    appealCounts.getOrDefault(editorId, 0), overturned.getOrDefault(editorId, 0),
                    workload.getOrDefault(editorId, 0));
            calculated.setEditorId(editorId);
            calculated.setPeriod(period);
            calculated.setLastUpdated(LocalDateTime.now());
            result.put(editorId, calculated);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Set<LocalDate> getPeriods() {
        Set<LocalDate> periods = new TreeSet<>();
        for (var task : crossReviewTaskRepository.findAll()) {
            if (task.getStatus() == CrossReviewTaskStatus.DONE && task.getSubmittedAt() != null)
                periods.add(task.getSubmittedAt().toLocalDate().withDayOfMonth(1));
        }
        for (var resolution : resolutionRepository.findAll()) {
            if (resolution.getFinalizedAt() != null)
                periods.add(resolution.getFinalizedAt().toLocalDate().withDayOfMonth(1));
        }
        return periods;
    }

    private boolean inScope(String role, Integer sectionId, Integer reviewerId, Map<Integer, Integer> reviewerSections) {
        return "ADMIN".equals(role) || (sectionId != null
                && sectionId.equals(reviewerSections.get(reviewerId)));
    }

    private boolean inPeriod(LocalDateTime timestamp, LocalDate period) {
        return timestamp != null && timestamp.toLocalDate().withDayOfMonth(1).equals(period);
    }

    private void addDifference(Map<Integer, List<BigDecimal>> differences, Integer editorId, BigDecimal value) {
        differences.computeIfAbsent(editorId, ignored -> new ArrayList<>()).add(value);
    }
}
