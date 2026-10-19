package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;

/**
 * MUC LUC (so dong)
 *   19  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer userId;
    private Integer articleId;
    @Nationalized @Column(length = 500)
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
