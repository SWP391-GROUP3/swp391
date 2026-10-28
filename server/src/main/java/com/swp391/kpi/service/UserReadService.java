package com.swp391.kpi.service;

import com.swp391.kpi.dto.UserAccessResponse;
import com.swp391.kpi.repository.UserReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

/**
 * Ham doc nguoi dung cho luong khac: kiem tra tai khoan dang hoat dong, lay danh sach nguoi dung (ho
 * ten, vai tro, ban).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   25  field                           repository / service duoc inject
 *   27  requireActive()                 bat buoc active
 *   41  getUsers()                      lay users
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
}
