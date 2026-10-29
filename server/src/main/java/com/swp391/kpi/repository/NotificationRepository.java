package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository truy van bang cua entity Notification (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  (khong co ham - chi khai bao du lieu)
 */
public interface NotificationRepository extends JpaRepository<Notification, Integer> {}
