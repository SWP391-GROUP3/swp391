package com.swp391.kpi.service;

import com.swp391.kpi.dto.ScoringReadResponse;
import com.swp391.kpi.dto.ScoringCriterionResponse;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ScoringReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Ham doc phieu cham cho luong khac (BF-03, BF-05): cac lan cham da nop, phieu da cong bo, tieu chi
 * cua phieu.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   27  field                           repository / service duoc inject
 *   29  getSubmittedAttempts()          lay submitted attempts
 *   31  getAttempt()                    lay attempt
 *   38  getPublishedAttempt()           lay published attempt
 *   48  getCriteria()                   lay criteria
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScoringReadService {
    private final ScoringReadRepository repository;

    public List<ScoringReadResponse> getSubmittedAttempts() { return repository.findSubmitted(); }

    public ScoringReadResponse getAttempt(Integer id) {
        for (var attempt : repository.findSubmitted()) {
            if (id.equals(attempt.id())) return attempt;
        }
        throw new NotFoundException("Không tìm thấy phiếu chấm đã nộp.");
    }

    public ScoringReadResponse getPublishedAttempt(Integer articleId) {
        ScoringReadResponse latest = null;
        for (var attempt : repository.findSubmitted()) {
            if (!articleId.equals(attempt.articleId()) || attempt.announcedAt() == null) continue;
            if (latest == null || attempt.announcedAt().isAfter(latest.announcedAt())) latest = attempt;
        }
        if (latest == null) throw new NotFoundException("Không tìm thấy kết quả chấm đã công bố.");
        return latest;
    }

    public List<ScoringCriterionResponse> getCriteria(Integer attemptId) { return repository.findCriteria(attemptId); }
}
