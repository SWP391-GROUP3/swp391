package com.swp391.kpi.service;

import com.swp391.kpi.dto.EventResponse;
import com.swp391.kpi.entity.Event;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Su kien va ty le bao hoa (BF-04) cho BF-01 goi luc nop bai (BR-IN-03, GBR-09).
 * BF-01 tu dem hang cua bai trong su kien (vi do la bang articles cua BF-01),
 * service nay chi doi hang -> ty le. Luong khac goi service nay, khong goi EventRepository.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   37  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   39  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   44  getOpenEvents()                 Su kien dang mo hom nay, cho o chon su kien cua BF-01
 *   54  getEvent()                      1 su kien theo id (BF-01 can windowDays, kLimit de dem hang cua bai)
 *   62  getSaturationRate()             Ty le bao hoa (%) cua bai xep hang rank trong su kien eventId (GBR-09)
 *  ---- Ham phu (private, chi dung trong file) ----
 *   71  findEvent()                     tim su kien theo id, khong co thi NotFoundException
 *   78  readRate()                      Lay 1 tham so ty le (%)
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private static final String ROYALTY_FLOW = "BF-04";  // gia tri cot system_parameters.flow cua luong nhuan but

    private final EventRepository eventRepository;
    private final SystemParameterService systemParameterService;

    // Su kien dang mo hom nay, cho o chon su kien cua BF-01 (BR-IN-03)
    @Transactional(readOnly = true)
    public List<EventResponse> getOpenEvents() {
        List<EventResponse> result = new ArrayList<>();
        for (Event event : eventRepository.findOpenOn(LocalDate.now())) {
            result.add(EventResponse.from(event));
        }
        return result;
    }

    // 1 su kien theo id (BF-01 can windowDays, kLimit de dem hang cua bai)
    @Transactional(readOnly = true)
    public EventResponse getEvent(Integer eventId) {
        return EventResponse.from(findEvent(eventId));
    }

    // Ty le bao hoa (%) cua bai xep hang rank trong su kien eventId (GBR-09).
    // BF-01 goi luc nop bai lan dau roi luu vao articles.saturation_rank / saturation_rate (BR-IN-03);
    // nop lai thi giu ty le cu (BR-IN-07). Cach doi hang -> ty le nam trong RoyaltyCalculator.
    @Transactional(readOnly = true)
    public BigDecimal getSaturationRate(Integer eventId, int rank) {
        Event event = findEvent(eventId);
        Map<String, String> params = systemParameterService.getValuesByFlow(ROYALTY_FLOW);
        BigDecimal level2Rate = readRate(params, "SATURATION_RATE_LEVEL_2");  // bac 2, mau 80%
        BigDecimal level3Rate = readRate(params, "SATURATION_RATE_LEVEL_3");  // bac 3, mau 60%
        return RoyaltyCalculator.saturationRateByRank(rank, event.getKLimit(), level2Rate, level3Rate);
    }

    // tim su kien theo id, khong co thi NotFoundException
    private Event findEvent(Integer eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện có ID: " + eventId));
    }

    // Lay 1 tham so ty le (%). Thieu hoac sai dinh dang thi bao loi ro rang,
    // tranh am tham khoa sai ty le cho bai (vd DB chua chay migration BF04_iter3_saturation_params.sql).
    private static BigDecimal readRate(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null) {
            throw new IllegalStateException("Thiếu tham số hệ thống: " + key);
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Tham số hệ thống " + key + " không phải là số: " + value);
        }
    }
}
