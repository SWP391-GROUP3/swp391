package com.swp391.kpi.royalty.repository;

import com.swp391.kpi.royalty.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Doc bang events (su kien, BF-04). findById() co san dung de lay exemptScore
 * khi cham nhuan but (GBR-09) va k_limit khi doi hang ra ty le bao hoa.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  findOpenOn()                    Su kien dang mo tai 1 ngay: is_active = 1 va ngay nam trong [start_date, end_date]
 *   29  findAllByOrderByStartDateDesc() Tat ca su kien cho man quan ly, moi nhat len dau
 *   32  existsByName()                  Da co su kien mang ten nay chua (dung khi tao)
 *   35  existsByNameAndIdNot()          Co su kien khac mang ten nay khong (dung khi sua)
 */
public interface EventRepository extends JpaRepository<Event, Integer> {

    // Su kien dang mo tai 1 ngay: is_active = 1 va ngay nam trong [start_date, end_date].
    // BF-01 dung cho o chon su kien luc soan/nop bai (BR-IN-03). Su kien bat dau som len dau.
    @Query("SELECT e FROM Event e WHERE e.isActive = true AND e.startDate <= :date AND e.endDate >= :date ORDER BY e.startDate")
    List<Event> findOpenOn(@Param("date") LocalDate date);

    // Tat ca su kien (ca da tat, da het han) cho man quan ly cua Thu ky, su kien moi nhat len dau.
    List<Event> findAllByOrderByStartDateDesc();

    // Da co su kien nao mang ten nay chua: dung khi TAO moi de chan trung ten.
    boolean existsByName(String name);

    // Co su kien KHAC (id khac) mang ten nay khong: dung khi SUA, de su kien khong bi tinh la trung voi chinh no.
    boolean existsByNameAndIdNot(String name, Integer id);
}
