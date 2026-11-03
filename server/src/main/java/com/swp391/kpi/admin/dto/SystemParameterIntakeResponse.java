package com.swp391.kpi.admin.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 6 tham so nhap bai cua BF-01 tra ve cho man Configure Intake Parameters (UC-13, Admin).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class SystemParameterIntakeResponse {

    private Integer maxAuthorsPerArticle;  // MAX_AUTHORS_PER_ARTICLE: so tac gia toi da 1 bai (BR-IN-02)
    private Integer coAuthorConfirmDays;   // CO_AUTHOR_CONFIRM_DAYS: so ngay dong tac gia xac nhan (BR-IN-06)
    private Integer dupWarnPercent;        // DUP_WARN_PERCENT: tu % nay phai giai trinh (BR-IN-04)
    private Integer dupBlockPercent;       // DUP_BLOCK_PERCENT: tren % nay, bai goc bi chan (BR-IN-04)
    private Integer dhashMaxBits;          // DHASH_MAX_BITS: lech <= so bit nay la anh trung (BR-IN-05)
    private Integer shingleSize;           // SHINGLE_SIZE: so am tiet 1 manh khi kiem trung noi dung (BR-IN-04)
}
