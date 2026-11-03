package com.swp391.kpi.article.dto;

import com.swp391.kpi.article.entity.DuplicationResult;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ket qua kiem trung noi dung (UC-06): ban xem truoc khi nop (chua luu, saved = false) hoac
 * ban da luu luc nop (bang duplication_checks + duplication_matches).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class DuplicationCheckResponse {

    private boolean saved;                 // false = ket qua tinh thu, chua luu (B5)
    private BigDecimal maxOverlapPercent;  // % trung cao nhat (B2)
    private DuplicationResult result;      // NORMAL / NEED_EXPLAIN / BLOCKED
    private String explanation;            // giai trinh cua PV (UC-08), chi co khi NEED_EXPLAIN
    private LocalDateTime checkedAt;
    private List<DuplicationMatchResponse> matches;   // trung nhieu xep truoc
}
