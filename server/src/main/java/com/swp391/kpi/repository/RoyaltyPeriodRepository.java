package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.entity.RoyaltyPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Doc bang royalty_periods (ky nhuan but, UC-49).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   28  findAllByOrderByPeriodYearDescPeriodMonthDesc()  Cac ky, thang moi nhat len dau
 *   32  findFirstByStatusOrderByPeriodYearAscPeriodMonthAsc()  Ky dang o 1 trang thai, ky cu nhat len dau
 *   35  existsByPeriodYearAndPeriodMonth()  Thang nay da co ky chua (UNIQUE period_year + period_month): dung khi tu tao ky thang sau
 *   38  findByPeriodYearAndPeriodMonth()  Ky cua 1 thang: khoa ky can tim ky thang truoc de lay no chuyen tiep (UC-53 A2)
 *   44  sumLineAmountByPeriodId()      Tong tien 1 ky = tong moi dong tien (bai, anh, dieu chinh) cua ky trong royalty_lines
 *   49  countGradedArticlesByPeriodId()  So bai da cham trong ky (1 bai cham lai nhieu phien ban van tinh 1 bai)
 *   59  findPeriodSummaries()           UC-49: tong tien + so bai da cham cua moi ky trong 1 cau SQL
 */
public interface RoyaltyPeriodRepository extends JpaRepository<RoyaltyPeriod, Integer> {

    // Cac ky, thang moi nhat len dau
    List<RoyaltyPeriod> findAllByOrderByPeriodYearDescPeriodMonthDesc();

    // Ky dang o 1 trang thai, ky cu nhat len dau. UC-47 dung voi OPEN: bai cham xong
    // vao ky dang mo (BR-RY-05). Binh thuong chi co 1 ky OPEN.
    Optional<RoyaltyPeriod> findFirstByStatusOrderByPeriodYearAscPeriodMonthAsc(RoyaltyPeriodStatus status);

    // Thang nay da co ky chua (UNIQUE period_year + period_month): dung khi tu tao ky thang sau
    boolean existsByPeriodYearAndPeriodMonth(Short periodYear, Byte periodMonth);

    // Ky cua 1 thang: khoa ky can tim ky thang truoc de lay no chuyen tiep (UC-53 A2)
    Optional<RoyaltyPeriod> findByPeriodYearAndPeriodMonth(Short periodYear, Byte periodMonth);

    // Tong tien 1 ky = tong moi dong tien (bai, anh, dieu chinh) cua ky trong royalty_lines.
    // Dung SQL thuong (nativeQuery) cho gon: chi can 1 con so tong, khong can nap entity.
    @Query(value = "SELECT COALESCE(SUM(amount), 0) FROM royalty_lines WHERE period_id = :periodId",
            nativeQuery = true)
    BigDecimal sumLineAmountByPeriodId(@Param("periodId") Integer periodId);

    // So bai da cham trong ky (1 bai cham lai nhieu phien ban van tinh 1 bai)
    @Query(value = "SELECT COUNT(DISTINCT article_id) FROM royalty_gradings WHERE period_id = :periodId",
            nativeQuery = true)
    Integer countGradedArticlesByPeriodId(@Param("periodId") Integer periodId);

    // UC-49: tong tien + so bai da cham cua MOI ky trong 1 cau SQL, thay vi moi ky goi
    // 2 ham o tren (N ky -> 2N cau). Ten bi danh (AS ...) trung ten getter cua RoyaltyPeriodSummaryRow.
    @Query(value = """
            SELECT p.id AS periodId,
                   COALESCE((SELECT SUM(l.amount) FROM royalty_lines l WHERE l.period_id = p.id), 0) AS totalAmount,
                   (SELECT COUNT(DISTINCT g.article_id) FROM royalty_gradings g WHERE g.period_id = p.id) AS gradedArticleCount
            FROM royalty_periods p
            """, nativeQuery = true)
    List<RoyaltyPeriodSummaryRow> findPeriodSummaries();
}
