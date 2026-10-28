package com.swp391.kpi.service;

import com.swp391.kpi.dto.UserPasswordChangeRequest;
import com.swp391.kpi.dto.UserProfileResponse;
import com.swp391.kpi.dto.UserProfileUpdateRequest;
import com.swp391.kpi.entity.User;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ho so ca nhan: moi vai tro tu xem / sua ho so cua CHINH MINH.
 * SRS khong co UC rieng cho chuc nang nay (xem docs/ProjectTracking.md).
 *
 * Sai mat khau hien tai -> BusinessException (409), KHONG dung UnauthorizedException (401):
 * frontend (goiApi) gap 401 la tu dang xuat, nguoi dung go nham 1 lan se bi da ra ngoai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   36  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   42  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   46  getMyProfile()                  lay my profile
 *   52  updateMyProfile()               cap nhat my profile
 *   69  changeMyPassword()              doi my password
 *  ---- Ham phu (private, chi dung trong file) ----
 *   90  findUser()                      tim user
 */
@Service
@RequiredArgsConstructor
public class UserService {

    static final String USER_NOT_FOUND_MESSAGE = "Không tìm thấy tài khoản.";
    static final String EMAIL_TAKEN_MESSAGE = "Email này đã được tài khoản khác sử dụng.";
    static final String WRONG_CURRENT_PASSWORD_MESSAGE = "Mật khẩu hiện tại không đúng.";
    static final String CONFIRM_MISMATCH_MESSAGE = "Mật khẩu nhập lại không khớp với mật khẩu mới.";
    static final String SAME_PASSWORD_MESSAGE = "Mật khẩu mới phải khác mật khẩu hiện tại.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Integer currentUserId) {
        User user = findUser(currentUserId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Integer currentUserId, UserProfileUpdateRequest request) {
        User user = findUser(currentUserId);
        String email = request.getEmail().trim();

        // Email la duy nhat toan he thong (SRS 3.3 "Unique email"). Kiem truoc de bao loi
        // tieng Viet, khong de DB nem loi UNIQUE kho hieu.
        if (userRepository.existsByEmailAndIdNot(email, currentUserId)) {
            throw new BusinessException(EMAIL_TAKEN_MESSAGE);
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void changeMyPassword(Integer currentUserId, UserPasswordChangeRequest request) {
        User user = findUser(currentUserId);

        // 1. Phai biet mat khau cu moi duoc doi (lo de may khong khoa thi nguoi khac khong doi duoc)
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(WRONG_CURRENT_PASSWORD_MESSAGE);
        }
        // 2. Go lai mat khau moi 2 lan phai giong nhau
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(CONFIRM_MISMATCH_MESSAGE);
        }
        // 3. Doi sang chinh mat khau cu thi vo nghia
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(SAME_PASSWORD_MESSAGE);
        }

        // Chi luu hash BCrypt, khong bao gio luu mat khau tho
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE));
    }
}
