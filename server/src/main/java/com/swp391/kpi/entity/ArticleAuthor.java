package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tac gia cua bai (bang article_authors), gom ca tac gia chinh
 * (dong cua tac gia chinh ACCEPTED ngay). Toi da 5 tac gia, tong
 * share_percent = 100 (BR-IN-02). Dong tac gia phai xac nhan truoc
 * confirm_deadline, qua han thi EXPIRED (BR-IN-06).
 */
@Entity
@Table(name = "article_authors")
@IdClass(ArticleAuthorId.class)   // khoa chinh gom 2 cot ben duoi
@Getter
@Setter
@NoArgsConstructor
public class ArticleAuthor {

    @Id
    private Integer articleId;

    // TODO(admin): doi sang @ManyToOne User khi luong admin co entity User
    @Id
    private Integer userId;

    private BigDecimal sharePercent;   // ty le nhuan but, luu dang phan tram (60.00 = 60%)

    @Enumerated(EnumType.STRING)
    private ArticleAuthorConfirmStatus confirmStatus;

    private LocalDateTime confirmDeadline;   // han xac nhan, null voi tac gia chinh

    private LocalDateTime respondedAt;

    @Nationalized   // cot NVARCHAR, bat buoc khi REJECTED
    private String rejectReason;
}
