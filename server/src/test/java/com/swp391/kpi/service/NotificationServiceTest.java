package com.swp391.kpi.service;
import com.swp391.kpi.dto.UserAccessResponse;
import com.swp391.kpi.entity.Notification;
import com.swp391.kpi.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {
    @Test void notifiesOnlyActiveHeadsOfArticleSection() {
        var repository = mock(NotificationRepository.class);
        var users = mock(UserReadService.class);
        when(users.getUsers()).thenReturn(List.of(
                new UserAccessResponse(5, "Head", "HEAD_OF_DEPARTMENT", 1, "Ban 1", "ACTIVE"),
                new UserAccessResponse(6, "Other", "HEAD_OF_DEPARTMENT", 2, "Ban 2", "ACTIVE"),
                new UserAccessResponse(7, "Locked", "HEAD_OF_DEPARTMENT", 1, "Ban 1", "LOCKED"),
                new UserAccessResponse(1, "Reporter", "REPORTER", 1, "Ban 1", "ACTIVE")));
        new NotificationService(repository, users).notifyHeads(1, 1, "Có khiếu nại mới.");
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertEquals(5, captor.getValue().getUserId());
        assertFalse(captor.getValue().getIsRead());
    }

    @Test void notifyRoleSendsToActiveUsersOfThatRoleOnly() {
        var repository = mock(NotificationRepository.class);
        var users = mock(UserReadService.class);
        when(users.getUsers()).thenReturn(List.of(
                new UserAccessResponse(6, "Ke toan", "ACCOUNTANT", null, null, "ACTIVE"),
                new UserAccessResponse(10, "Ke toan nghi", "ACCOUNTANT", null, null, "LOCKED"),
                new UserAccessResponse(4, "Thu ky", "EDITORIAL_SECRETARY", null, null, "ACTIVE")));
        new NotificationService(repository, users).notifyRole("ACCOUNTANT", "Kỳ 9/2026 đã chốt.");
        var captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertEquals(6, captor.getValue().getUserId());
        assertNull(captor.getValue().getArticleId());
    }
}
