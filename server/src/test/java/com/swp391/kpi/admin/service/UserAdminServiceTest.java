package com.swp391.kpi.admin.service;

import com.swp391.kpi.admin.dto.UserCreateRequest;
import com.swp391.kpi.admin.dto.UserProfileResponse;
import com.swp391.kpi.admin.entity.Role;
import com.swp391.kpi.admin.entity.RoleCode;
import com.swp391.kpi.admin.entity.Section;
import com.swp391.kpi.admin.entity.User;
import com.swp391.kpi.admin.entity.UserStatus;
import com.swp391.kpi.admin.repository.RoleRepository;
import com.swp391.kpi.admin.repository.SectionRepository;
import com.swp391.kpi.admin.repository.UserRepository;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiem tra Admin tao / khoa / mo khoa tai khoan.
 * Repository va UserReadService la mock, BCrypt dung that.
 */
@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    private static final Integer ADMIN_ID = 8;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private SectionRepository sectionRepository;
    @Mock
    private UserReadService userReadService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserAdminService service;

    @BeforeEach
    void setUp() {
        service = new UserAdminService(userRepository, roleRepository, sectionRepository,
                userReadService, passwordEncoder);
    }

    private static Role role(RoleCode code, String name) {
        Role role = new Role();
        role.setId(code.ordinal() + 1);
        role.setCode(code);
        role.setName(name);
        return role;
    }

    private static Section section(Integer id, boolean active) {
        Section section = new Section();
        section.setId(id);
        section.setCode("THOISU");
        section.setName("Ban Thời sự");
        section.setIsActive(active);
        return section;
    }

    private static UserCreateRequest createRequest(String roleCode, Integer sectionId) {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("lan.pt");
        request.setEmail(" lan.pt@toasoan.vn ");
        request.setPassword("matkhau1");
        request.setFullName(" Phạm Thị Lan ");
        request.setRoleCode(roleCode);
        request.setSectionId(sectionId);
        return request;
    }

    private static User userWithStatus(Integer id, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    // ---------- createUser ----------

    @Test
    void createUserSavesReporterWithHashedPasswordAndActiveStatus() {
        when(userRepository.existsByUsername("lan.pt")).thenReturn(false);
        when(userRepository.existsByEmail("lan.pt@toasoan.vn")).thenReturn(false);
        when(roleRepository.findByCode(RoleCode.REPORTER)).thenReturn(Optional.of(role(RoleCode.REPORTER, "Phóng viên")));
        when(sectionRepository.findById(1)).thenReturn(Optional.of(section(1, true)));

        UserProfileResponse result = service.createUser(ADMIN_ID, createRequest("REPORTER", 1));

        assertEquals("lan.pt@toasoan.vn", result.getEmail());
        assertEquals("Phạm Thị Lan", result.getFullName());
        assertEquals("REPORTER", result.getRoleCode());
        assertEquals(1, result.getSectionId());
        assertEquals("ACTIVE", result.getStatus());
        assertNotNull(result.getCreatedAt());
        verify(userReadService).requireActive(ADMIN_ID, "ADMIN");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserStoresBcryptHashNotPlainPassword() {
        when(roleRepository.findByCode(RoleCode.ACCOUNTANT)).thenReturn(Optional.of(role(RoleCode.ACCOUNTANT, "Kế toán")));
        User[] saved = new User[1];
        when(userRepository.save(any(User.class))).thenAnswer(call -> {
            saved[0] = call.getArgument(0);
            return saved[0];
        });

        service.createUser(ADMIN_ID, createRequest("ACCOUNTANT", null));

        assertTrue(passwordEncoder.matches("matkhau1", saved[0].getPasswordHash()));
        assertNull(saved[0].getSectionId());
    }

    @Test
    void createUserRejectsTakenUsername() {
        when(userRepository.existsByUsername("lan.pt")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createUser(ADMIN_ID, createRequest("REPORTER", 1)));

        assertEquals(UserAdminService.USERNAME_TAKEN_MESSAGE, ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserRejectsTakenEmail() {
        when(userRepository.existsByEmail("lan.pt@toasoan.vn")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createUser(ADMIN_ID, createRequest("REPORTER", 1)));

        assertEquals(UserAdminService.EMAIL_TAKEN_MESSAGE, ex.getMessage());
    }

    @Test
    void createUserRejectsUnknownRoleCode() {
        // ma vai tro la du lieu gui len sai -> IllegalArgumentException (GlobalExceptionHandler doi thanh 400)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createUser(ADMIN_ID, createRequest("GIAM_DOC", null)));

        assertEquals(UserAdminService.INVALID_ROLE_MESSAGE, ex.getMessage());
    }

    @Test
    void createUserRejectsPasswordOver72Bytes() {
        // 30 ky tu co dau = 90 byte UTF-8: qua @Size(max = 72 ky tu) nhung vuot gioi han 72 byte cua BCrypt
        UserCreateRequest request = createRequest("REPORTER", 1);
        request.setPassword("ễ".repeat(30));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.createUser(ADMIN_ID, request));

        assertEquals(UserAdminService.PASSWORD_TOO_LONG_MESSAGE, ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUserThrowsNotFoundWhenRoleMissingInDatabase() {
        when(roleRepository.findByCode(RoleCode.REPORTER)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.createUser(ADMIN_ID, createRequest("REPORTER", 1)));

        assertEquals(UserAdminService.ROLE_NOT_FOUND_MESSAGE, ex.getMessage());
    }

    @Test
    void createUserThrowsNotFoundWhenSectionMissing() {
        when(roleRepository.findByCode(RoleCode.REPORTER)).thenReturn(Optional.of(role(RoleCode.REPORTER, "Phóng viên")));
        when(sectionRepository.findById(99)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.createUser(ADMIN_ID, createRequest("REPORTER", 99)));

        assertEquals(UserAdminService.SECTION_NOT_FOUND_MESSAGE, ex.getMessage());
    }

    @Test
    void createUserRequiresSectionForReporter() {
        when(roleRepository.findByCode(RoleCode.REPORTER)).thenReturn(Optional.of(role(RoleCode.REPORTER, "Phóng viên")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createUser(ADMIN_ID, createRequest("REPORTER", null)));

        assertEquals(UserAdminService.SECTION_REQUIRED_MESSAGE, ex.getMessage());
    }

    @Test
    void createUserRejectsSectionForAccountant() {
        when(roleRepository.findByCode(RoleCode.ACCOUNTANT)).thenReturn(Optional.of(role(RoleCode.ACCOUNTANT, "Kế toán")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.createUser(ADMIN_ID, createRequest("ACCOUNTANT", 1)));

        assertEquals(UserAdminService.SECTION_NOT_ALLOWED_MESSAGE, ex.getMessage());
    }

    @Test
    void createUserRejectsInactiveSection() {
        when(roleRepository.findByCode(RoleCode.EDITOR)).thenReturn(Optional.of(role(RoleCode.EDITOR, "Biên tập viên")));
        when(sectionRepository.findById(2)).thenReturn(Optional.of(section(2, false)));

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> service.createUser(ADMIN_ID, createRequest("EDITOR", 2)));

        assertEquals(UserAdminService.SECTION_NOT_FOUND_MESSAGE, ex.getMessage());
    }

    @Test
    void createUserStopsWhenCallerIsNotAdmin() {
        when(userReadService.requireActive(1, "ADMIN"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."));

        assertThrows(ResponseStatusException.class,
                () -> service.createUser(1, createRequest("REPORTER", 1)));
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- lockUser / unlockUser ----------

    @Test
    void lockUserSetsStatusLocked() {
        User user = userWithStatus(1, UserStatus.ACTIVE);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        service.lockUser(ADMIN_ID, 1);

        assertEquals(UserStatus.LOCKED, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void lockUserRejectsLockingSelf() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.lockUser(ADMIN_ID, ADMIN_ID));

        assertEquals(UserAdminService.CANNOT_LOCK_SELF_MESSAGE, ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void lockUserRejectsAlreadyLockedUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(userWithStatus(1, UserStatus.LOCKED)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.lockUser(ADMIN_ID, 1));

        assertEquals(UserAdminService.ALREADY_LOCKED_MESSAGE, ex.getMessage());
    }

    @Test
    void lockUserThrowsNotFoundWhenUserMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.lockUser(ADMIN_ID, 99));
    }

    @Test
    void unlockUserSetsStatusActive() {
        User user = userWithStatus(1, UserStatus.LOCKED);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        service.unlockUser(ADMIN_ID, 1);

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void unlockUserRejectsActiveUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(userWithStatus(1, UserStatus.ACTIVE)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.unlockUser(ADMIN_ID, 1));

        assertEquals(UserAdminService.ALREADY_ACTIVE_MESSAGE, ex.getMessage());
    }

    @Test
    void unlockUserThrowsNotFoundWhenUserMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.unlockUser(ADMIN_ID, 99));
    }
}
