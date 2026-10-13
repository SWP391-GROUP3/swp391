package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Doc bang royalty_periods (ky nhuan but, UC-49).
 */
public interface RoyaltyPeriodRepository extends JpaRepository<RoyaltyPeriod, Integer> {

    // Cac ky, thang moi nhat len dau
    List<RoyaltyPeriod> findAllByOrderByPeriodYearDescPeriodMonthDesc();

    // Tong tien 1 ky = tong moi dong tien (bai, anh, dieu chinh) cua ky trong royalty_lines.
    // Dung SQL thuong (nativeQuery) vi entity RoyaltyLine chua co, Iter2 moi lam.
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM royalty_lines WHERE period_id = :periodId",
            nativeQuery = true)
    BigDecimal sumLineAmountByPeriodId(@Param("periodId") Integer periodId);

    // So bai da cham trong ky (1 bai cham lai nhieu phien ban van tinh 1 bai)
    @Query(value = "SELECT COUNT(DISTINCT article_id) FROM royalty_gradings WHERE period_id = :periodId",
            nativeQuery = true)
    Integer countGradedArticlesByPeriodId(@Param("periodId") Integer periodId);
}
