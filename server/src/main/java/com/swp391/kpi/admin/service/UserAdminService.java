package com.swp391.kpi.admin.service;

import com.swp391.kpi.admin.dto.SectionResponse;
import com.swp391.kpi.admin.dto.UserCreateRequest;
import com.swp391.kpi.admin.dto.UserPasswordChangeRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin quan ly tai khoan: tao moi, khoa, mo khoa (man "Tài khoản", M-A7).
 * Khong xoa user, chi khoa bang status = LOCKED (comment dau schema.sql).
 * Moi ham deu kiem nguoi goi la Admin dang hoat dong (UserReadService.requireActive).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   50  hang so                         message, vai tro bat buoc co ban
 *   68  field                           repository / service duoc inject
 *  ---- Ham public (controller goi) ----
 *   75  createUser()                    Admin tao tai khoan moi [POST /api/users]
 *  108  lockUser()                      Khoa tai khoan, khong cho tu khoa minh [PUT /api/users/{id}/lock]
 *  123  unlockUser()                    Mo khoa tai khoan [PUT /api/users/{id}/unlock]
 *  134  getActiveSections()             Cac ban dang hoat dong cho form tao tai khoan [GET /api/sections]
 *  ---- Ham phu (private, chi dung trong file) ----
 *  144  parseRoleCode()                 doi chuoi "REPORTER" -> RoleCode, sai ma -> 400
 *  155  checkPasswordBytes()            mat khau qua 72 byte (gioi han BCrypt) -> 400
 *  163  checkSection()                  luat ban theo vai tro, tra sectionId hop le
 *  182  findUser()                      tim user, khong co -> 404
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    static final String ADMIN_ROLE = "ADMIN";
    static final String USERNAME_TAKEN_MESSAGE = "Tên đăng nhập này đã có người sử dụng.";
    static final String EMAIL_TAKEN_MESSAGE = "Email này đã được tài khoản khác sử dụng.";
    static final String INVALID_ROLE_MESSAGE = "Vai trò không hợp lệ.";
    static final String PASSWORD_TOO_LONG_MESSAGE = "Mật khẩu quá dài, vui lòng rút ngắn hoặc bỏ bớt ký tự có dấu.";
    static final String ROLE_NOT_FOUND_MESSAGE = "Không tìm thấy vai trò.";
    static final String SECTION_REQUIRED_MESSAGE = "Phóng viên, Biên tập viên và Trưởng ban phải thuộc một ban.";
    static final String SECTION_NOT_ALLOWED_MESSAGE = "Vai trò này không thuộc ban nào, vui lòng bỏ chọn ban.";
    static final String SECTION_NOT_FOUND_MESSAGE = "Không tìm thấy ban hoặc ban đã ngừng hoạt động.";
    static final String USER_NOT_FOUND_MESSAGE = "Không tìm thấy tài khoản.";
    static final String CANNOT_LOCK_SELF_MESSAGE = "Bạn không thể tự khoá tài khoản của chính mình.";
    static final String ALREADY_LOCKED_MESSAGE = "Tài khoản này đã bị khoá.";
    static final String ALREADY_ACTIVE_MESSAGE = "Tài khoản này đang hoạt động.";

    // PV / BTV / Truong ban bat buoc co ban, vai tro khac de NULL (comment bang users trong schema.sql)
    static final List<RoleCode> ROLES_WITH_SECTION =
            List.of(RoleCode.REPORTER, RoleCode.EDITOR, RoleCode.HEAD_OF_DEPARTMENT);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SectionRepository sectionRepository;
    private final UserReadService userReadService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserProfileResponse createUser(Integer currentUserId, UserCreateRequest request) {
        userReadService.requireActive(currentUserId, ADMIN_ROLE);
        checkPasswordBytes(request.getPassword());
        String email = request.getEmail().trim();

        // username, email la UNIQUE: kiem truoc de bao loi tieng Viet thay vi loi SQL
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(USERNAME_TAKEN_MESSAGE);
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(EMAIL_TAKEN_MESSAGE);
        }

        RoleCode roleCode = parseRoleCode(request.getRoleCode());
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new NotFoundException(ROLE_NOT_FOUND_MESSAGE));
        Integer sectionId = checkSection(roleCode, request.getSectionId());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));   // chi luu BCrypt hash
        user.setFullName(request.getFullName().trim());
        user.setRole(role);
        user.setSectionId(sectionId);
        // JPA ghi ca cot null -> DEFAULT cua DB khong chay, phai tu gan status va created_at
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void lockUser(Integer currentUserId, Integer userId) {
        userReadService.requireActive(currentUserId, ADMIN_ROLE);
        // Admin tu khoa minh se mat quyen quan tri ngay lap tuc
        if (userId.equals(currentUserId)) {
            throw new BusinessException(CANNOT_LOCK_SELF_MESSAGE);
        }
        User user = findUser(userId);
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException(ALREADY_LOCKED_MESSAGE);
        }
        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);
    }

    @Transactional
    public void unlockUser(Integer currentUserId, Integer userId) {
        userReadService.requireActive(currentUserId, ADMIN_ROLE);
        User user = findUser(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(ALREADY_ACTIVE_MESSAGE);
        }
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> getActiveSections() {
        List<SectionResponse> result = new ArrayList<>();
        for (Section section : sectionRepository.findByIsActiveTrueOrderByNameAsc()) {
            result.add(SectionResponse.from(section));
        }
        return result;
    }

    // doi chuoi "REPORTER" -> RoleCode; ma khong co trong enum la du lieu gui len sai -> 400
    // (IllegalArgumentException duoc GlobalExceptionHandler doi thanh 400, BusinessException la 409)
    private RoleCode parseRoleCode(String code) {
        for (RoleCode roleCode : RoleCode.values()) {
            if (roleCode.name().equals(code)) {
                return roleCode;
            }
        }
        throw new IllegalArgumentException(INVALID_ROLE_MESSAGE);
    }

    // @Size tren DTO dem KY TU, con BCrypt chi nhan toi da 72 BYTE. Chu co dau chiem 2-3 byte nen
    // mat khau duoi 72 ky tu van co the vuot -> kiem truoc de bao tieng Viet (400) thay vi loi tieng Anh cua BCrypt
    private void checkPasswordBytes(String password) {
        int byteCount = password.getBytes(StandardCharsets.UTF_8).length;
        if (byteCount > UserPasswordChangeRequest.MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(PASSWORD_TOO_LONG_MESSAGE);
        }
    }

    // Luat ban theo vai tro: PV / BTV / Truong ban phai chon ban dang hoat dong, vai tro khac khong co ban
    private Integer checkSection(RoleCode roleCode, Integer sectionId) {
        if (!ROLES_WITH_SECTION.contains(roleCode)) {
            if (sectionId != null) {
                throw new BusinessException(SECTION_NOT_ALLOWED_MESSAGE);
            }
            return null;
        }
        if (sectionId == null) {
            throw new BusinessException(SECTION_REQUIRED_MESSAGE);
        }
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new NotFoundException(SECTION_NOT_FOUND_MESSAGE));
        if (!Boolean.TRUE.equals(section.getIsActive())) {
            throw new NotFoundException(SECTION_NOT_FOUND_MESSAGE);
        }
        return sectionId;
    }

    // tim user, khong co -> 404
    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(USER_NOT_FOUND_MESSAGE));
    }
}
