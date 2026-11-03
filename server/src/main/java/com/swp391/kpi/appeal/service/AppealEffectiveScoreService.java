package com.swp391.kpi.appeal.service;

import com.swp391.kpi.appeal.dto.AppealEffectiveAttemptResponse;
import com.swp391.kpi.appeal.entity.Appeal;
import com.swp391.kpi.appeal.entity.AppealResolution;
import com.swp391.kpi.appeal.entity.AppealStatus;
import com.swp391.kpi.appeal.repository.AppealRepository;
import com.swp391.kpi.appeal.repository.AppealResolutionRepository;
import com.swp391.kpi.assessment.dto.ScoringReadResponse;
import com.swp391.kpi.assessment.service.ScoringReadService;
import com.swp391.kpi.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

/**
 * Tinh diem hieu luc cua bai: diem phieu goc da cong bo, hoac diem da chot sau khieu nai.
 * Day la NGUON DIEM DUY NHAT sau khi cham (BR-QC-10): hang cho duyet dang (BF-02), cham nhuan but
 * (BF-04) va KPI (BF-05) deu goi class nay, khong tu doc bang scoring_attempts.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   35  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   40  getEffectiveScore()             Ben goi cung cap diem cua phieu goc da cong bo; appeal dang xu ly khong doi diem nay
 *   47  getEffectiveScore()             Diem hieu luc cua bai, tu lay diem S0 da duyet
 *   55  getEffectiveAttempt()           Diem hieu luc kem lan cham dang dung: chua co khieu nai RESOLVED -> lan cham S0 da duyet, co -> lan cham S1/S…
 *  ---- Ham phu (private, chi dung trong file) ----
 *   67  findResolution()                Khieu nai SUBMITTED / IN_REVIEW chua doi diem (SRS BF-03: diem cu tam thoi van co hieu luc)
 *   75  resolvedScore()                 Du lieu cu (truoc Iter3) khong ghi effective_score, khi do lay tong diem cua lan cham duoc chon
 */
@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppealEffectiveScoreService {
    private final AppealRepository appeals;
    private final AppealResolutionRepository resolutions;
    private final ScoringReadService scoring;

    // Ben goi cung cap diem cua phieu goc da cong bo; appeal dang xu ly khong doi diem nay.
    public BigDecimal getEffectiveScore(Integer articleId, BigDecimal publishedScore) {
        AppealResolution resolution = findResolution(articleId);
        if (resolution == null) return publishedScore;
        return resolvedScore(resolution);
    }

    // Diem hieu luc cua bai, tu lay diem S0 da duyet. Bai chua duoc cong bo diem thi tra null.
    public BigDecimal getEffectiveScore(Integer articleId) {
        AppealEffectiveAttemptResponse effective = getEffectiveAttempt(articleId);
        if (effective == null) return null;
        return effective.score();
    }

    // Diem hieu luc kem lan cham dang dung: chua co khieu nai RESOLVED -> lan cham S0 da duyet,
    // co -> lan cham S1/S2 ma Truong ban chot. Bai chua duoc cong bo diem thi tra null.
    public AppealEffectiveAttemptResponse getEffectiveAttempt(Integer articleId) {
        ScoringReadResponse published = scoring.findPublishedAttempt(articleId);
        if (published == null) return null;

        AppealResolution resolution = findResolution(articleId);
        if (resolution == null) {
            return new AppealEffectiveAttemptResponse(published.id(), published.totalScore(), false);
        }
        return new AppealEffectiveAttemptResponse(resolution.getEffectiveAttemptId(), resolvedScore(resolution), true);
    }

    // Khieu nai SUBMITTED / IN_REVIEW chua doi diem (SRS BF-03: diem cu tam thoi van co hieu luc).
    private AppealResolution findResolution(Integer articleId) {
        Appeal appeal = appeals.findByArticleId(articleId).orElse(null);
        if (appeal == null || appeal.getStatus() != AppealStatus.RESOLVED) return null;
        return resolutions.findByAppealId(appeal.getId()).orElseThrow(() ->
                new BusinessException("Khiếu nại đã giải quyết nhưng thiếu kết quả xác nhận."));
    }

    // Du lieu cu (truoc Iter3) khong ghi effective_score, khi do lay tong diem cua lan cham duoc chon.
    private BigDecimal resolvedScore(AppealResolution resolution) {
        if (resolution.getEffectiveScore() != null) return resolution.getEffectiveScore();
        return scoring.getAttempt(resolution.getEffectiveAttemptId()).totalScore();
    }
}
