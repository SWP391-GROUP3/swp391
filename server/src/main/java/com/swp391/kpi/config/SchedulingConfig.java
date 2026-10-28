package com.swp391.kpi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bat chay job dinh ky (file dung chung - sua phai bao nhom).
 *
 * Co @EnableScheduling thi Spring moi chay cac ham gan @Scheduled. Viet job theo mau:
 * dat @Scheduled(cron = "0 0 * * * *") tren ham trong service (cron 6 o: giay phut gio ngay thang thu,
 * mau nay = dau moi gio). Dung cron, KHONG dung fixedRate: fixedRate chay ngay luc app khoi dong,
 * ke ca khi chay test, de lam doi du lieu.
 * Se dung cho: job het han xac nhan dong tac gia 3 ngay (BF-01, UC-12 A2), job tong hop KPI (BF-05).
 *
 * MUC LUC (so dong - ten - tac dung)
 *  (khong co ham - chi khai bao du lieu)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
