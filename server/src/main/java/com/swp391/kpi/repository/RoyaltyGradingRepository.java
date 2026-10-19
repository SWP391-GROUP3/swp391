package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyGrading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Doc/ghi bang royalty_gradings (phieu cham nhuan but, UC-46, UC-47).
 *
 * MUC LUC (so dong)
 *   22  findGradedArticleIds()       - Id cac bai DA co phieu cham (UC-46 loai cac bai nay khoi danh sach cho …
 *   25  existsByArticleId()          - Bai da co phieu cham chua (UC-47: Iter 2 moi bai chi cham 1 lan, cham l…
 *   29  findByPeriodIdOrderByGradedAtAsc() - Cac phieu cham thuoc 1 ky, cham truoc len dau (bang ke ky, UC-50).
 */
public interface RoyaltyGradingRepository extends JpaRepository<RoyaltyGrading, Integer> {

    // Id cac bai DA co phieu cham (UC-46 loai cac bai nay khoi danh sach cho cham).
    // DISTINCT vi 1 bai cham lai co nhieu phien ban, chi can lay id 1 lan.
    @Query("SELECT DISTINCT g.articleId FROM RoyaltyGrading g")
    List<Integer> findGradedArticleIds();

    // Bai da co phieu cham chua (UC-47: Iter 2 moi bai chi cham 1 lan, cham lai la Iter 3)
    boolean existsByArticleId(Integer articleId);

    // Cac phieu cham thuoc 1 ky, cham truoc len dau (bang ke ky, UC-50).
    // "PeriodId" = truong period -> id: Spring tu di qua quan he @ManyToOne.
    List<RoyaltyGrading> findByPeriodIdOrderByGradedAtAsc(Integer periodId);
}
