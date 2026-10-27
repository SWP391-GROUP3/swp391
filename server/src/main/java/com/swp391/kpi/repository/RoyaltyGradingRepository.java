package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyGrading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Doc/ghi bang royalty_gradings (phieu cham nhuan but, UC-46, UC-47).
 *
 * MUC LUC (so dong)
 *   23  findGradedArticleIds()       - Id cac bai DA co phieu cham (UC-46 loai cac bai nay khoi danh sach cho …
 *   26  findFirstByArticleIdOrderByVersionNoDesc() - Phien ban cham moi nhat cua 1 bai; rong = chua cham (cham lai Iter 3)
 *   30  findByPeriodIdOrderByGradedAtAsc() - Cac phieu cham thuoc 1 ky, cham truoc len dau (bang ke ky, UC-50).
 */
public interface RoyaltyGradingRepository extends JpaRepository<RoyaltyGrading, Integer> {

    // Id cac bai DA co phieu cham (UC-46 loai cac bai nay khoi danh sach cho cham).
    // DISTINCT vi 1 bai cham lai co nhieu phien ban, chi can lay id 1 lan.
    @Query("SELECT DISTINCT g.articleId FROM RoyaltyGrading g")
    List<Integer> findGradedArticleIds();

    // Phien ban cham moi nhat cua 1 bai; rong = bai chua cham lan nao (UC-47, cham lai Iter 3)
    Optional<RoyaltyGrading> findFirstByArticleIdOrderByVersionNoDesc(Integer articleId);

    // Cac phieu cham thuoc 1 ky, cham truoc len dau (bang ke ky, UC-50).
    // "PeriodId" = truong period -> id: Spring tu di qua quan he @ManyToOne.
    List<RoyaltyGrading> findByPeriodIdOrderByGradedAtAsc(Integer periodId);
}
