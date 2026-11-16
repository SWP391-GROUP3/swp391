package com.swp391.kpi.royalty.service;

import com.swp391.kpi.admin.service.SystemParameterService;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.royalty.dto.EventResponse;
import com.swp391.kpi.royalty.dto.EventSaveRequest;
import com.swp391.kpi.royalty.entity.Event;
import com.swp391.kpi.royalty.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Su kien va ty le bao hoa (BF-04). Co 2 nhom ham:
 *  1. Cho BF-01 goi luc nop bai (BR-IN-03, GBR-09): BF-01 tu dem hang cua bai trong su kien
 *     (vi do la bang articles cua BF-01), service nay chi doi hang -> ty le.
 *  2. Cho Thu ky toa soan quan ly su kien: xem tat ca, tao, sua, bat / tat, xoa.
 * Luong khac goi service nay, khong goi EventRepository.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   51  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   53  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   58  getOpenEvents()                 Su kien dang mo hom nay, cho o chon su kien cua BF-01 (BR-IN-03)
 *   68  getEvent()                      1 su kien theo id (BF-01 can windowDays, kLimit de dem hang cua bai)
 *   76  getSaturationRate()             Ty le bao hoa (%) cua bai xep hang rank trong su kien eventId (GBR-09)
 *   86  getAllEvents()                  Tat ca su kien cho man quan ly cua Thu ky, moi nhat len dau
 *   97  createEvent()                   Thu ky tao su kien moi: kiem thu tu ngay, ngay ket thuc, trung ten roi luu
 *  123  updateEvent()                   Thu ky sua su kien: su kien da bat dau thi chi sua duoc ten va ngay ket thuc
 *  144  setEventActive()                Thu ky bat / tat su kien (tat thi PV khong chon duoc nua)
 *  157  deleteEvent()                   Thu ky xoa su kien; su kien da co bai gan vao thi bao loi, khong xoa
 *  ---- Ham phu (private, chi dung trong file) ----
 *  169  findEvent()                     tim su kien theo id, khong co thi NotFoundException
 *  175  checkDateOrder()                Ngay bat dau khong duoc sau ngay ket thuc (CHECK ck_events_dates)
 *  183  checkStartedEventUnchanged()    Su kien da bat dau: khong cho doi ngay bat dau, cua so, so bai moi bac, diem mien giam
 *  195  copyFields()                    Chep 6 o nhap tu request vao entity
 *  206  readRate()                      Lay 1 tham so ty le (%)
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

    // Tat ca su kien (ca da tat, da ket thuc) cho man quan ly cua Thu ky, moi nhat len dau
    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents() {
        List<EventResponse> result = new ArrayList<>();
        for (Event event : eventRepository.findAllByOrderByStartDateDesc()) {
            result.add(EventResponse.from(event));
        }
        return result;
    }

    // Thu ky tao su kien moi. Dinh dang tung o da kiem bang @Valid (EventSaveRequest);
    // o day kiem 3 luat can so sanh nhieu o hoac hoi DB. Su kien moi tao thi bat san.
    @Transactional
    public EventResponse createEvent(EventSaveRequest request, Integer currentUserId) {
        String name = request.getName().trim();
        checkDateOrder(request.getStartDate(), request.getEndDate());

        // Su kien da qua thi PV khong chon duoc (getOpenEvents chi lay su kien con trong khoang ngay)
        if (request.getEndDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Ngày kết thúc không được ở quá khứ.");
        }
        // Bang events khong co UNIQUE tren cot name nen phai tu kiem
        if (eventRepository.existsByName(name)) {
            throw new BusinessException("Đã có sự kiện tên \"" + name + "\". Vui lòng đặt tên khác.");
        }

        Event event = new Event();
        copyFields(event, request, name);
        event.setIsActive(true);
        event.setCreatedBy(currentUserId);
        return EventResponse.from(eventRepository.save(event));
    }

    // Thu ky sua su kien. Su kien CHUA bat dau: sua duoc ca 6 o.
    // Su kien DA bat dau: co the da co bai nop va khoa ty le bao hoa theo cua so / so bai moi bac
    // cua su kien (BR-IN-03), nen chi cho sua ten va ngay ket thuc.
    // [TAM] Dung "da bat dau" thay cho "da co bai khoa ty le" vi luong article chua co ham service
    // dem bai theo su kien; co ham do thi doi dieu kien o dong if ben duoi.
    @Transactional
    public EventResponse updateEvent(Integer eventId, EventSaveRequest request) {
        Event event = findEvent(eventId);
        String name = request.getName().trim();
        checkDateOrder(request.getStartDate(), request.getEndDate());

        // IdNot: bo qua chinh su kien dang sua, chi xet su kien khac
        if (eventRepository.existsByNameAndIdNot(name, eventId)) {
            throw new BusinessException("Đã có sự kiện khác tên \"" + name + "\". Vui lòng đặt tên khác.");
        }
        boolean hasStarted = !event.getStartDate().isAfter(LocalDate.now());
        if (hasStarted) {
            checkStartedEventUnchanged(event, request);
        }

        copyFields(event, request, name);
        return EventResponse.from(eventRepository.save(event));
    }

    // Thu ky bat / tat su kien. Tat thi su kien khong con trong o chon cua PV (findOpenOn loc is_active = 1);
    // bai da nop truoc do van giu ty le bao hoa da khoa.
    @Transactional
    public EventResponse setEventActive(Integer eventId, boolean active) {
        Event event = findEvent(eventId);
        if (event.getIsActive() == active) {
            throw new BusinessException(active ? "Sự kiện đang bật sẵn." : "Sự kiện đang tắt sẵn.");
        }
        event.setIsActive(active);
        return EventResponse.from(eventRepository.save(event));
    }

    // Thu ky xoa su kien tao nham. Bang articles co khoa ngoai event_id -> events: su kien da co bai
    // gan vao thi DB tu choi xoa. flush() gui lenh DELETE xuong DB ngay trong ham nay de bat duoc
    // loi do va doi thanh cau tieng Viet (khong flush thi loi chi no ra luc ket thuc giao dich).
    @Transactional
    public void deleteEvent(Integer eventId) {
        Event event = findEvent(eventId);
        try {
            eventRepository.delete(event);
            eventRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("Sự kiện \"" + event.getName() + "\" đã có bài viết gắn vào nên không xoá được. "
                    + "Hãy tắt sự kiện thay vì xoá.");
        }
    }

    // tim su kien theo id, khong co thi NotFoundException
    private Event findEvent(Integer eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện có ID: " + eventId));
    }

    // Ngay bat dau khong duoc sau ngay ket thuc (DB cung co CHECK ck_events_dates, nhung loi tu DB kho hieu)
    private static void checkDateOrder(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
    }

    // Su kien da bat dau: 4 o duoi day phai giu nguyen gia tri cu.
    // exemptScore la BigDecimal nen so bang compareTo (90 va 90.00 la bang nhau, equals thi khong).
    private static void checkStartedEventUnchanged(Event event, EventSaveRequest request) {
        boolean sameStartDate = event.getStartDate().equals(request.getStartDate());
        boolean sameWindowDays = event.getWindowDays().equals(request.getWindowDays());
        boolean sameArticlesPerLevel = event.getKLimit().equals(request.getArticlesPerLevel());
        boolean sameExemptScore = event.getExemptScore().compareTo(request.getExemptScore()) == 0;
        if (!sameStartDate || !sameWindowDays || !sameArticlesPerLevel || !sameExemptScore) {
            throw new BusinessException("Sự kiện đã bắt đầu nên chỉ được sửa tên và ngày kết thúc. "
                    + "Muốn đổi cách tính bão hoà, hãy tắt sự kiện này và tạo sự kiện mới.");
        }
    }

    // Chep 6 o nhap tu request vao entity (dung chung cho tao va sua). name da duoc cat khoang trang 2 dau.
    private static void copyFields(Event event, EventSaveRequest request, String name) {
        event.setName(name);
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setWindowDays(request.getWindowDays());
        event.setKLimit(request.getArticlesPerLevel());
        event.setExemptScore(request.getExemptScore());
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
