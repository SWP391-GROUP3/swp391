package com.swp391.kpi.service;

import com.swp391.kpi.entity.AppealStatus;
import com.swp391.kpi.repository.AppealRepository;
import com.swp391.kpi.repository.AppealResolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/**
 * Tinh diem hieu luc cua bai: diem phieu goc da cong bo, hoac diem da chot sau khieu nai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           repository / service duoc inject
 *   26  getEffectiveScore()             Ben goi cung cap diem cua phieu goc da cong bo; appeal dang xu ly khong doi diem nay
 */
@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppealEffectiveScoreService {
    private final AppealRepository appeals;
    private final AppealResolutionRepository resolutions;
    private final ScoringReadService scoring;

    // Ben goi cung cap diem cua phieu goc da cong bo; appeal dang xu ly khong doi diem nay.
    public BigDecimal getEffectiveScore(Integer articleId, BigDecimal publishedScore) {
        var appeal = appeals.findByArticleId(articleId).orElse(null);
        if (appeal == null || appeal.getStatus() != AppealStatus.RESOLVED) return publishedScore;
        var resolution = resolutions.findByAppealId(appeal.getId()).orElseThrow(() ->
                new com.swp391.kpi.exception.BusinessException("Khiếu nại đã giải quyết nhưng thiếu kết quả xác nhận."));
        if (resolution.getEffectiveScore() == null) return scoring.getAttempt(resolution.getEffectiveAttemptId()).totalScore();
        return resolution.getEffectiveScore();
    }
}
