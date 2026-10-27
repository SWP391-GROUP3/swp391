package com.swp391.kpi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job het han xac nhan dong tac gia (UC-12 A2, BR-IN-06), ten theo SDS: CoAuthorExpiryJob.
 * Chay dau moi gio (cron, khong dung fixedRate - nhom truong 24/10, A1; xem config/SchedulingConfig).
 * Tach class rieng de loi goi di qua proxy cua Spring -> @Transactional cua service co tac dung.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  hang so                         ten tham so, ten hanh dong, gioi han... (khong viet so ma trong code)
 *   26  field                           repository / service duoc inject
 *   30  expireOverdueConfirmations()    Dau moi gio: goi service dua bai qua han xac nhan ve Nhap, ghi log so bai da xu ly
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleCoAuthorExpiryJob {

    // cron 6 o: giay phut gio ngay thang thu -> giay 0, phut 0 cua moi gio
    private static final String EVERY_HOUR = "0 0 * * * *";

    private final ArticleConfirmationService articleConfirmationService;

    // Dau moi gio: goi service dua bai qua han xac nhan ve Nhap, ghi log so bai da xu ly
    @Scheduled(cron = EVERY_HOUR)
    public void expireOverdueConfirmations() {
        int count = articleConfirmationService.expireOverdueConfirmations();
        if (count > 0) {
            log.info("Job het han xac nhan dong tac gia: da dua {} bai ve Nhap", count);
        }
    }
}
