package com.swp391.kpi.admin.service;

import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.repository.UserReadRepository;
import com.swp391.kpi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ham doc nguoi dung cho luong khac: kiem tra tai khoan dang hoat dong, lay danh sach nguoi dung (ho
 * ten, vai tro, ban).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   36  field                           repository / service duoc inject
 *   38  requireActive()                 bat buoc active
 *   52  getUsers()                      lay users
 *   57  getUsersForAdmin()              Man "Tai khoan" cua Admin (M-A7): moi tai khoan, sap theo id
 *   65  getUserNames()                  Ho ten theo id nguoi dung, de cac man hinh tien hien ten thay vi so id
 *   74  getUserAccess()                 M-D2: thong tin 1 nguoi dung (vai tro, ban, trang thai); khong co -> 404
 *   84  getActiveUsersByRole()          M-D2: nguoi dung dang ACTIVE co vai tro roleCode (roles.code, vd REPORTER), sap theo id
 *   96  getUserNames()                  M-D2: ho ten cua cac id can dung (thay cho 3 ban loadUserNames() viet rieng)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReadService {
    private final UserReadRepository repository;

    public UserAccessResponse requireActive(Integer userId, String... roles) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập.");
        for (UserAccessResponse user : repository.findAll()) {
            if (!user.id().equals(userId)) continue;
            if (!"ACTIVE".equals(user.status()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không hoạt động.");
            for (String role : roles) {
                if (role.equals(user.role())) return user;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.");
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Người dùng không tồn tại.");
    }

    public List<UserAccessResponse> getUsers() {
        return repository.findAll();
    }

    // Man "Tài khoản" cua Admin (M-A7): moi tai khoan, sap theo id. Khong phai Admin -> 403
    public List<UserAccessResponse> getUsersForAdmin(Integer currentUserId) {
        requireActive(currentUserId, "ADMIN");
        List<UserAccessResponse> users = new ArrayList<>(repository.findAll());
        users.sort(Comparator.comparing(UserAccessResponse::id));
        return users;
    }

    // Ho ten theo id nguoi dung, de cac man hinh tien hien ten thay vi so id
    public Map<Integer, String> getUserNames() {
        Map<Integer, String> names = new HashMap<>();
        for (UserAccessResponse user : repository.findAll()) {
            names.put(user.id(), user.fullName());
        }
        return names;
    }

    // M-D2: thong tin 1 nguoi dung (vai tro, ban, trang thai); khong co -> 404
    public UserAccessResponse getUserAccess(Integer userId) {
        for (UserAccessResponse user : repository.findAll()) {
            if (user.id().equals(userId)) {
                return user;
            }
        }
        throw new NotFoundException("Không tìm thấy người dùng.");
    }

    // M-D2: nguoi dung dang ACTIVE co vai tro roleCode (roles.code, vd REPORTER), sap theo id
    public List<UserAccessResponse> getActiveUsersByRole(String roleCode) {
        List<UserAccessResponse> result = new ArrayList<>();
        for (UserAccessResponse user : repository.findAll()) {
            if (roleCode.equals(user.role()) && "ACTIVE".equals(user.status())) {
                result.add(user);
            }
        }
        result.sort(Comparator.comparing(UserAccessResponse::id));
        return result;
    }

    // M-D2: ho ten cua cac id can dung (thay cho 3 ban loadUserNames() viet rieng)
    public Map<Integer, String> getUserNames(Collection<Integer> userIds) {
        Map<Integer, String> names = new HashMap<>();
        for (UserAccessResponse user : repository.findAll()) {
            if (userIds.contains(user.id())) {
                names.put(user.id(), user.fullName());
            }
        }
        return names;
    }
}
