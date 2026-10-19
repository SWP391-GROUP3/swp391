package com.swp391.kpi.service;

import com.swp391.kpi.dto.UserPasswordChangeRequest;
import com.swp391.kpi.dto.UserProfileResponse;
import com.swp391.kpi.dto.UserProfileUpdateRequest;
import com.swp391.kpi.entity.Role;
import com.swp391.kpi.entity.RoleCode;
import com.swp391.kpi.entity.User;
import com.swp391.kpi.entity.UserStatus;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiem tra ho so ca nhan: xem, sua ho ten/email, doi mat khau.
 * Repository la mock, BCrypt dung that (hash trong data.sql = "123456").
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String HASH_OF_123456 = "$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    private static User reporter() {
        Role role = new Role();
        role.setId(1);
        role.setCode(RoleCode.REPORTER);
        role.setName("Phóng viên");

        User user = new User();
        user.setId(1);
        user.setUsername("an.nv");
        user.setEmail("an.nv@toasoan.vn");
        user.setPasswordHash(HASH_OF_123456);
        user.setFullName("Nguyễn Văn An");
        user.setRole(role);
        user.setSectionId(1);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private static UserProfileUpdateRequest updateRequest(String fullName, String email) {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        return request;
    }

    private static UserPasswordChangeRequest passwordRequest(String current, String newPassword, String confirm) {
        UserPasswordChangeRequest request = new UserPasswordChangeRequest();
        request.setCurrentPassword(current);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirm);
        return request;
    }

    @Test
    void getMyProfileReturnsProfileWithoutPassword() {
        when(userRepository.findById(1)).thenReturn(Optional.of(reporter()));

        UserProfileResponse profile = userService.getMyProfile(1);

        assertEquals("an.nv", profile.getUsername());
        assertEquals("Phóng viên", profile.getRoleName());
        assertEquals("ACTIVE", profile.getStatus());
    }

    @Test
    void getMyProfileThrowsNotFoundWhenUserMissing() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getMyProfile(99));
    }

    @Test
    void updateMyProfileTrimsAndSavesNameAndEmail() {
        User user = reporter();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIdNot("an.moi@toasoan.vn", 1)).thenReturn(false);

        UserProfileResponse profile = userService.updateMyProfile(1,
                updateRequest("  Nguyễn Văn An Mới ", " an.moi@toasoan.vn "));

        assertEquals("Nguyễn Văn An Mới", profile.getFullName());
        assertEquals("an.moi@toasoan.vn", user.getEmail());
        verify(userRepository).save(user);
    }

    @Test
    void updateMyProfileRejectsEmailUsedByAnotherUser() {
        when(userRepository.findById(1)).thenReturn(Optional.of(reporter()));
        when(userRepository.existsByEmailAndIdNot("binh.tt@toasoan.vn", 1)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateMyProfile(1, updateRequest("An", "binh.tt@toasoan.vn")));

        assertEquals(UserService.EMAIL_TAKEN_MESSAGE, ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeMyPasswordStoresNewBcryptHash() {
        User user = reporter();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        userService.changeMyPassword(1, passwordRequest("123456", "matkhau-moi", "matkhau-moi"));

        // Luu hash, khong luu mat khau tho; hash moi khop mat khau moi
        assertTrue(passwordEncoder.matches("matkhau-moi", user.getPasswordHash()));
        verify(userRepository).save(user);
    }

    @Test
    void changeMyPasswordRejectsWrongCurrentPassword() {
        when(userRepository.findById(1)).thenReturn(Optional.of(reporter()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeMyPassword(1, passwordRequest("sai", "matkhau-moi", "matkhau-moi")));

        assertEquals(UserService.WRONG_CURRENT_PASSWORD_MESSAGE, ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changeMyPasswordRejectsMismatchedConfirmation() {
        when(userRepository.findById(1)).thenReturn(Optional.of(reporter()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeMyPassword(1, passwordRequest("123456", "matkhau-moi", "matkhau-khac")));

        assertEquals(UserService.CONFIRM_MISMATCH_MESSAGE, ex.getMessage());
    }

    @Test
    void changeMyPasswordRejectsSamePasswordAsCurrent() {
        when(userRepository.findById(1)).thenReturn(Optional.of(reporter()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changeMyPassword(1, passwordRequest("123456", "123456", "123456")));

        assertEquals(UserService.SAME_PASSWORD_MESSAGE, ex.getMessage());
    }
}
