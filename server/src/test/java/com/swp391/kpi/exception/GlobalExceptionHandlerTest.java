package com.swp391.kpi.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiem tra GlobalExceptionHandler doi loi thanh dung status + ApiResponse.
 * Dung MockMvc voi controller gia nen khong can bat server, khong can DB.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FakeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returns404WhenNotFoundExceptionIsThrown() throws Exception {
        mockMvc.perform(get("/fake/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Không tìm thấy bài viết"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void returns409WhenBusinessExceptionIsThrown() throws Exception {
        mockMvc.perform(get("/fake/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Kỳ nhuận bút đã khoá, không thể sửa"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void returns401WhenUnauthorizedExceptionIsThrown() throws Exception {
        mockMvc.perform(get("/fake/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email/tên đăng nhập hoặc mật khẩu không đúng."));
    }

    @Test
    void returns400WithFieldErrorsWhenValidationFails() throws Exception {
        mockMvc.perform(post("/fake/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Dữ liệu không hợp lệ"))
                .andExpect(jsonPath("$.errors.title").value("Tiêu đề không được để trống"));
    }

    @Test
    void emptyFieldBreakingTwoRulesAlwaysReportsRequiredMessage() throws Exception {
        // username rong vi pham ca @NotBlank lan @Pattern -> luon lay cau "khong duoc de trong"
        mockMvc.perform(post("/fake/validate-username")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").value("Tên đăng nhập không được để trống"));
    }

    @Test
    void returns400WhenPathValueHasWrongType() throws Exception {
        mockMvc.perform(get("/fake/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tham số trên đường dẫn không hợp lệ."));
    }

    @Test
    void returns400WhenBodyIsNotReadableJson() throws Exception {
        mockMvc.perform(post("/fake/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Dữ liệu gửi lên không đọc được (sai định dạng JSON hoặc sai kiểu dữ liệu)."));
    }

    @Test
    void returns400WhenCalculatorRejectsInput() throws Exception {
        mockMvc.perform(get("/fake/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tiêu chí đạt dưới 50 điểm bắt buộc phải nhập nhận xét lý do!"));
    }

    @Test
    void returns500WithVietnameseMessageWhenSystemParameterMissing() throws Exception {
        mockMvc.perform(get("/fake/illegal-state"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Thiếu tham số hệ thống: ROUNDING_UNIT"));
    }

    // Controller gia chi dung trong test: moi API co tinh nem ra mot loai loi
    @RestController
    @RequestMapping("/fake")
    public static class FakeController {

        @GetMapping("/not-found")
        public String notFound() {
            throw new NotFoundException("Không tìm thấy bài viết");
        }

        @GetMapping("/business")
        public String business() {
            throw new BusinessException("Kỳ nhuận bút đã khoá, không thể sửa");
        }

        @GetMapping("/unauthorized")
        public String unauthorized() {
            throw new UnauthorizedException("Email/tên đăng nhập hoặc mật khẩu không đúng.");
        }

        @GetMapping("/illegal-argument")
        public String illegalArgument() {
            throw new IllegalArgumentException("Tiêu chí đạt dưới 50 điểm bắt buộc phải nhập nhận xét lý do!");
        }

        @GetMapping("/illegal-state")
        public String illegalState() {
            throw new IllegalStateException("Thiếu tham số hệ thống: ROUNDING_UNIT");
        }

        @PostMapping("/validate")
        public String validate(@Valid @RequestBody FakeRequest request) {
            return "ok";
        }

        @PostMapping("/validate-username")
        public String validateUsername(@Valid @RequestBody FakeUsernameRequest request) {
            return "ok";
        }

        @GetMapping("/items/{id}")
        public String item(@PathVariable Integer id) {
            return "ok";
        }
    }

    // Request DTO gia: 1 o co 2 luat, de rong thi vi pham ca 2
    @Getter
    @Setter
    public static class FakeUsernameRequest {
        @NotBlank(message = "Tên đăng nhập không được để trống")
        @Pattern(regexp = "^[a-z]+$", message = "Tên đăng nhập chỉ gồm chữ thường")
        private String username;
    }

    // Request DTO gia de thu @Valid
    @Getter
    @Setter
    public static class FakeRequest {
        @NotBlank(message = "Tiêu đề không được để trống")
        private String title;
    }
}
