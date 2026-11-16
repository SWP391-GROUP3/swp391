package com.swp391.kpi.royalty.service;

import com.swp391.kpi.admin.service.SystemParameterService;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.royalty.dto.EventResponse;
import com.swp391.kpi.royalty.dto.EventSaveRequest;
import com.swp391.kpi.royalty.entity.Event;
import com.swp391.kpi.royalty.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiem tra su kien va ty le bao hoa (BR-IN-03, GBR-09), khong can DB.
 * Su kien mau giong data.sql: "Bao so 3 nam 2026", window_days 7, k_limit 10, exempt_score 90.
 * Nua sau file: man quan ly su kien cua Thu ky (xem tat ca, tao, sua, bat / tat).
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Integer EVENT_ID = 1;
    private static final Integer SECRETARY_ID = 4;   // Thu ky toa soan trong data.sql

    @Mock private EventRepository eventRepository;
    @Mock private SystemParameterService systemParameterService;

    @InjectMocks
    private EventService eventService;

    @Test
    void getOpenEventsReturnsEventsOpenToday() {
        when(eventRepository.findOpenOn(LocalDate.now())).thenReturn(List.of(stormEvent()));

        List<EventResponse> result = eventService.getOpenEvents();

        assertEquals(1, result.size());
        EventResponse event = result.get(0);
        assertEquals(EVENT_ID, event.getId());
        assertEquals("Bão số 3 năm 2026", event.getName());
        assertEquals(Integer.valueOf(7), event.getWindowDays());
        assertEquals(Integer.valueOf(10), event.getArticlesPerLevel());
    }

    @Test
    void getSaturationRateUsesKLimitOfEventAndRatesFromParameters() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(stormEvent()));
        when(systemParameterService.getValuesByFlow("BF-04")).thenReturn(saturationParams());

        assertEquals(0, new BigDecimal("100").compareTo(eventService.getSaturationRate(EVENT_ID, 10)));
        assertEquals(0, new BigDecimal("80").compareTo(eventService.getSaturationRate(EVENT_ID, 11)));
        assertEquals(0, new BigDecimal("60").compareTo(eventService.getSaturationRate(EVENT_ID, 21)));
    }

    @Test
    void getSaturationRateFailsWhenEventNotFound() {
        when(eventRepository.findById(99)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> eventService.getSaturationRate(99, 1));

        assertEquals("Không tìm thấy sự kiện có ID: 99", ex.getMessage());
    }

    @Test
    void getSaturationRateFailsWhenParameterIsMissing() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(stormEvent()));
        when(systemParameterService.getValuesByFlow("BF-04"))
                .thenReturn(Map.of("SATURATION_RATE_LEVEL_2", "80"));   // DB chua chay migration du

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> eventService.getSaturationRate(EVENT_ID, 15));

        assertEquals("Thiếu tham số hệ thống: SATURATION_RATE_LEVEL_3", ex.getMessage());
    }

    // ------------------------------------------------------------ man quan ly: xem tat ca

    @Test
    void getAllEventsReturnsEveryEventWithActiveFlag() {
        Event turnedOff = stormEvent();
        turnedOff.setIsActive(false);
        when(eventRepository.findAllByOrderByStartDateDesc()).thenReturn(List.of(turnedOff));

        List<EventResponse> result = eventService.getAllEvents();

        assertEquals(1, result.size());
        assertEquals(Boolean.FALSE, result.get(0).getActive());
    }

    // ------------------------------------------------------------ man quan ly: tao

    @Test
    void createEventSavesTrimmedNameTurnsEventOnAndRecordsCreator() {
        EventSaveRequest request = validRequest();
        request.setName("  Bầu cử 2027  ");
        when(eventRepository.existsByName("Bầu cử 2027")).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse result = eventService.createEvent(request, SECRETARY_ID);

        assertEquals("Bầu cử 2027", result.getName());
        assertEquals(Boolean.TRUE, result.getActive());
        assertEquals(Integer.valueOf(5), result.getArticlesPerLevel());
        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertEquals(SECRETARY_ID, saved.getValue().getCreatedBy());
    }

    @Test
    void createEventFailsWhenStartDateIsAfterEndDate() {
        EventSaveRequest request = validRequest();
        request.setStartDate(LocalDate.now().plusDays(10));
        request.setEndDate(LocalDate.now().plusDays(5));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.createEvent(request, SECRETARY_ID));

        assertEquals("Ngày bắt đầu không được sau ngày kết thúc.", ex.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void createEventFailsWhenEndDateIsInThePast() {
        EventSaveRequest request = validRequest();
        request.setStartDate(LocalDate.now().minusDays(10));
        request.setEndDate(LocalDate.now().minusDays(1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.createEvent(request, SECRETARY_ID));

        assertEquals("Ngày kết thúc không được ở quá khứ.", ex.getMessage());
    }

    @Test
    void createEventAllowsEndDateToday() {
        EventSaveRequest request = validRequest();
        request.setStartDate(LocalDate.now());
        request.setEndDate(LocalDate.now());
        when(eventRepository.existsByName("Bầu cử 2027")).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse result = eventService.createEvent(request, SECRETARY_ID);

        assertEquals(LocalDate.now(), result.getEndDate());
    }

    @Test
    void createEventFailsWhenNameAlreadyExists() {
        EventSaveRequest request = validRequest();
        when(eventRepository.existsByName("Bầu cử 2027")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.createEvent(request, SECRETARY_ID));

        assertEquals("Đã có sự kiện tên \"Bầu cử 2027\". Vui lòng đặt tên khác.", ex.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    // ------------------------------------------------------------ man quan ly: sua

    @Test
    void updateEventChangesEveryFieldWhenEventHasNotStarted() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(upcomingEvent()));
        when(eventRepository.existsByNameAndIdNot("Bầu cử 2027", EVENT_ID)).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EventSaveRequest request = validRequest();   // khac su kien cu o ca 6 o

        EventResponse result = eventService.updateEvent(EVENT_ID, request);

        assertEquals("Bầu cử 2027", result.getName());
        assertEquals(request.getStartDate(), result.getStartDate());
        assertEquals(request.getEndDate(), result.getEndDate());
        assertEquals(Integer.valueOf(14), result.getWindowDays());
        assertEquals(Integer.valueOf(5), result.getArticlesPerLevel());
        assertEquals(0, new BigDecimal("85").compareTo(result.getExemptScore()));
    }

    @Test
    void updateEventFailsWhenEventNotFound() {
        when(eventRepository.findById(99)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> eventService.updateEvent(99, validRequest()));

        assertEquals("Không tìm thấy sự kiện có ID: 99", ex.getMessage());
    }

    @Test
    void updateEventFailsWhenAnotherEventHasTheSameName() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(upcomingEvent()));
        when(eventRepository.existsByNameAndIdNot("Bầu cử 2027", EVENT_ID)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.updateEvent(EVENT_ID, validRequest()));

        assertEquals("Đã có sự kiện khác tên \"Bầu cử 2027\". Vui lòng đặt tên khác.", ex.getMessage());
    }

    @Test
    void updateEventFailsWhenStartedEventChangesArticlesPerLevel() {
        Event started = stormEvent();   // bat dau 05/09/2026, da qua
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(started));
        when(eventRepository.existsByNameAndIdNot(started.getName(), EVENT_ID)).thenReturn(false);
        EventSaveRequest request = requestFrom(started);
        request.setArticlesPerLevel(2);   // doi so bai moi bac: 10 -> 2

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.updateEvent(EVENT_ID, request));

        assertEquals("Sự kiện đã bắt đầu nên chỉ được sửa tên và ngày kết thúc. "
                + "Muốn đổi cách tính bão hoà, hãy tắt sự kiện này và tạo sự kiện mới.", ex.getMessage());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEventAllowsNewNameAndEndDateWhenEventHasStarted() {
        Event started = stormEvent();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(started));
        when(eventRepository.existsByNameAndIdNot("Bão số 3 (kéo dài)", EVENT_ID)).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EventSaveRequest request = requestFrom(started);
        request.setName("Bão số 3 (kéo dài)");
        request.setEndDate(LocalDate.of(2026, 9, 30));
        request.setExemptScore(new BigDecimal("90.00"));   // 90 va 90.00 la cung 1 gia tri, khong tinh la doi

        EventResponse result = eventService.updateEvent(EVENT_ID, request);

        assertEquals("Bão số 3 (kéo dài)", result.getName());
        assertEquals(LocalDate.of(2026, 9, 30), result.getEndDate());
    }

    // ------------------------------------------------------------ man quan ly: bat / tat

    @Test
    void setEventActiveTurnsEventOff() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(stormEvent()));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse result = eventService.setEventActive(EVENT_ID, false);

        assertEquals(Boolean.FALSE, result.getActive());
    }

    @Test
    void setEventActiveFailsWhenEventIsAlreadyInThatState() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(stormEvent()));   // dang bat

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.setEventActive(EVENT_ID, true));

        assertEquals("Sự kiện đang bật sẵn.", ex.getMessage());
    }

    // ------------------------------------------------------------ man quan ly: xoa

    @Test
    void deleteEventRemovesEventThatNoArticleUses() {
        Event event = stormEvent();
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        eventService.deleteEvent(EVENT_ID);

        verify(eventRepository).delete(event);
        verify(eventRepository).flush();
    }

    @Test
    void deleteEventFailsWhenEventNotFound() {
        when(eventRepository.findById(99)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> eventService.deleteEvent(99));

        assertEquals("Không tìm thấy sự kiện có ID: 99", ex.getMessage());
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void deleteEventFailsWhenArticlesStillUseTheEvent() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(stormEvent()));
        // DB tu choi vi khoa ngoai articles.event_id -> events
        doThrow(new DataIntegrityViolationException("fk_articles_event")).when(eventRepository).flush();

        BusinessException ex = assertThrows(BusinessException.class, () -> eventService.deleteEvent(EVENT_ID));

        assertEquals("Sự kiện \"Bão số 3 năm 2026\" đã có bài viết gắn vào nên không xoá được. "
                + "Hãy tắt sự kiện thay vì xoá.", ex.getMessage());
    }

    // ------------------------------------------------------------ du lieu mau

    // Su kien chua bat dau (bat dau ngay mai): con sua duoc ca 6 o
    private static Event upcomingEvent() {
        Event event = stormEvent();
        event.setStartDate(LocalDate.now().plusDays(1));
        event.setEndDate(LocalDate.now().plusDays(20));
        return event;
    }

    // Form hop le, khac stormEvent() o ca 6 o
    private static EventSaveRequest validRequest() {
        EventSaveRequest request = new EventSaveRequest();
        request.setName("Bầu cử 2027");
        request.setStartDate(LocalDate.now().plusDays(3));
        request.setEndDate(LocalDate.now().plusDays(30));
        request.setWindowDays(14);
        request.setArticlesPerLevel(5);
        request.setExemptScore(new BigDecimal("85"));
        return request;
    }

    // Form giu nguyen moi gia tri cua 1 su kien (giong luc Thu ky bam "Sua" roi chua doi gi)
    private static EventSaveRequest requestFrom(Event event) {
        EventSaveRequest request = new EventSaveRequest();
        request.setName(event.getName());
        request.setStartDate(event.getStartDate());
        request.setEndDate(event.getEndDate());
        request.setWindowDays(event.getWindowDays());
        request.setArticlesPerLevel(event.getKLimit());
        request.setExemptScore(event.getExemptScore());
        return request;
    }

    private static Event stormEvent() {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setName("Bão số 3 năm 2026");
        event.setStartDate(LocalDate.of(2026, 9, 5));
        event.setEndDate(LocalDate.of(2026, 9, 20));
        event.setWindowDays(7);
        event.setKLimit(10);
        event.setExemptScore(new BigDecimal("90"));
        event.setIsActive(true);
        return event;
    }

    private static Map<String, String> saturationParams() {
        return Map.of("SATURATION_RATE_LEVEL_2", "80", "SATURATION_RATE_LEVEL_3", "60");
    }
}
