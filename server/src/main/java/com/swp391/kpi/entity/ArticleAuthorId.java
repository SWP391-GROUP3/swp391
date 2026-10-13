package com.swp391.kpi.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Khoa chinh cua bang article_authors gom 2 cot (article_id, user_id).
 * JPA yeu cau class khoa nhieu cot phai Serializable va co equals/hashCode
 * de so sanh 2 khoa voi nhau.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ArticleAuthorId implements Serializable {

    private Integer articleId;

    private Integer userId;
}
