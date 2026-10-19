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
 * MUC LUC (so dong)
 *   29  field                        - repository / service duoc inject
 *   37  refresh()
 *  106  getPeriods()
 *  119  inScope()
 *  124  inPeriod()
 *  128  addDifference()
 */
@Service @RequiredArgsConstructor
public class ReviewerDeviationRefreshService {
    private final UserReadService userReadService;
    private final ScoringReadService scoringReadService;
    private final AppealRepository appealRepository;
    private final AppealResolutionRepository resolutionRepository;
    private final CrossReviewTaskRepository crossReviewTaskRepository;
    private final ReviewerDeviationRepository indexRepository;

    @Transactional
    public Set<Integer> refresh(LocalDate period, Integer currentUserId) {
        var user = userReadService.requireActive(currentUserId, "ADMIN", "HEAD_OF_DEPARTMENT");
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
            if (original == null || !inScope(user.role(), user.sectionId(), original.reviewerId(), reviewerSections)) continue;
            addDifference(differences, original.reviewerId(), task.getScore().subtract(original.totalScore()));
        }
        for (var resolution : resolutionRepository.findAll()) {
            if (!inPeriod(resolution.getFinalizedAt(), period)) continue;
            var appeal = appealRepository.findById(resolution.getAppealId()).orElse(null);
            if (appeal == null || appeal.getStatus() != AppealStatus.RESOLVED) continue;
            var original = byId.get(appeal.getScoringAttemptId());
            if (original == null || !inScope(user.role(), user.sectionId(), original.reviewerId(), reviewerSections)) continue;
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
                    && inScope(user.role(), user.sectionId(), attempt.reviewerId(), reviewerSections)) {
                workload.merge(attempt.reviewerId(), 1, Integer::sum);
            }
        }
        var existing = indexRepository.findAllByOrderByPeriodDescEditorIdAsc();
        for (var entry : differences.entrySet()) {
            Integer editorId = entry.getKey();
            var calculated = new ReviewerDeviationCalculator().calculate(entry.getValue(),
                    appealCounts.getOrDefault(editorId, 0), overturned.getOrDefault(editorId, 0),
                    workload.getOrDefault(editorId, 0));
            ReviewerDeviationIndex target = null;
            for (var index : existing) {
                if (period.equals(index.getPeriod()) && editorId.equals(index.getEditorId())) target = index;
            }
            if (target == null) target = new ReviewerDeviationIndex();
            target.setEditorId(editorId);
            target.setPeriod(period);
            target.setAvgDeviation(calculated.getAvgDeviation());
            target.setAppealCount(calculated.getAppealCount());
            target.setOverturnRate(calculated.getOverturnRate());
            target.setWorkloadCount(calculated.getWorkloadCount());
            target.setLastUpdated(LocalDateTime.now());
            indexRepository.save(target);
        }
        return differences.keySet();
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
