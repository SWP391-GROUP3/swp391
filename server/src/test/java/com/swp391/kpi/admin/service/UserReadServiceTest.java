package com.swp391.kpi.admin.service;
import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.repository.UserReadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserReadServiceTest {
    private final UserReadRepository repository = mock(UserReadRepository.class);
    private final UserReadService service = new UserReadService(repository);

    @Test void missingIdentityReturns401() {
        var error = assertThrows(ResponseStatusException.class, () -> service.requireActive(null, "ADMIN"));
        assertEquals(401, error.getStatusCode().value());
        verifyNoInteractions(repository);
    }
    @Test void unknownIdentityReturns401() {
        when(repository.findAll()).thenReturn(List.of());
        var error = assertThrows(ResponseStatusException.class, () -> service.requireActive(999, "ADMIN"));
        assertEquals(401, error.getStatusCode().value());
    }
    @Test void wrongRoleReturns403() {
        when(repository.findAll()).thenReturn(List.of(user("ACTIVE", "EDITOR")));
        var error = assertThrows(ResponseStatusException.class, () -> service.requireActive(3, "ADMIN"));
        assertEquals(403, error.getStatusCode().value());
    }
    @Test void lockedAdminReturns403() {
        when(repository.findAll()).thenReturn(List.of(user("LOCKED", "ADMIN")));
        var error = assertThrows(ResponseStatusException.class, () -> service.requireActive(3, "ADMIN"));
        assertEquals(403, error.getStatusCode().value());
    }
    @Test void activeAllowedRoleCanProceed() {
        when(repository.findAll()).thenReturn(List.of(user("ACTIVE", "HEAD_OF_DEPARTMENT")));
        assertEquals(1, service.requireActive(3, "ADMIN", "HEAD_OF_DEPARTMENT").sectionId());
    }
    private UserAccessResponse user(String status, String role) {
        return new UserAccessResponse(3, "Test", role, 1, "Ban 1", status);
    }

    @Test void activeUsersByRoleAndNamesByIds() {
        UserAccessResponse reporter = new UserAccessResponse(1, "An", "REPORTER", 1, "Ban 1", "ACTIVE");
        UserAccessResponse locked = new UserAccessResponse(2, "Bình", "REPORTER", 1, "Ban 1", "LOCKED");
        UserAccessResponse editor = new UserAccessResponse(3, "Cường", "EDITOR", 1, "Ban 1", "ACTIVE");
        when(repository.findAll()).thenReturn(List.of(editor, locked, reporter));

        assertEquals(List.of(reporter), service.getActiveUsersByRole("REPORTER"));
        assertEquals(Map.of(1, "An", 3, "Cường"), service.getUserNames(Set.of(1, 3)));
        assertEquals(editor, service.getUserAccess(3));
    }
}
