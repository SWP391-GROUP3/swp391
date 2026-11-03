package com.swp391.kpi.royalty.service;

import com.swp391.kpi.admin.service.SystemParameterService;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.royalty.dto.EventResponse;
import com.swp391.kpi.royalty.entity.Event;
import com.swp391.kpi.royalty.repository.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Kiem tra su kien va ty le bao hoa (BR-IN-03, GBR-09), khong can DB.
 * Su kien mau giong data.sql: "Bao so 3 nam 2026", window_days 7, k_limit 10, exempt_score 90.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final Integer EVENT_ID = 1;

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

    // ------------------------------------------------------------ du lieu mau

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
