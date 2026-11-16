package com.swp391.kpi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.common.service.AuthService;
import com.swp391.kpi.common.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Cau hinh Spring Security (file dung chung - sua phai bao nhom).
 *
 * Spring Security la chuoi bo loc dung TRUOC moi controller: request khong hop le
 * bi tra 401/403 ngay, khong vao toi controller.
 *
 * Luat hien tai:
 * - POST /api/auth/login: ai cung goi duoc (chua co token thi moi can dang nhap)
 * - BF-04: them don gia = Ke toan, sua he so anh/video = Admin, xem ky nhuan but = Thu ky + Ke toan,
 *   cham nhuan but = Thu ky, quan ly su kien bao hoa = Thu ky (rieng GET /api/events/open: ai dang nhap cung goi duoc)
 * - BF-01: tao / sua / nop bai (POST, PUT /api/articles/**) = Phong vien
 * - Admin: xem / tao / khoa / mo khoa tai khoan (/api/users, /api/users/{id}/lock|unlock)
 * - moi API con lai: phai gui header "Authorization: Bearer <token>" hop le
 * - token con han nhung tai khoan da bi khoa -> 401 ngay o request ke tiep (convertActiveUser)
 * Chan theo vai tro (vd chi Ke toan them don gia) -> them .requestMatchers(...).hasRole(...)
 *
 * MUC LUC (so dong - ten - tac dung)
 *   57  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   61  field                           cac truong du lieu
 *  ---- Ham public (controller / luong khac goi) ----
 *   65  securityFilterChain()           security filter chain
 *  ---- Ham phu (private, chi dung trong file) ----
 *  135  jwtAuthenticationConverter()   Claim "role" trong token (vd "REPORTER") -> quyen "ROLE_REPORTER", nho vay viet duoc .hasRole("REPORTER") khi…
 *  148  convertActiveUser()           Tai khoan da bi khoa thi tu choi token (401), con hoat dong thi doi token thanh nguoi dang dang nhap
 *  157  writeError()                Ghi loi ra response dang { success: false, message: "..." } giong GlobalExceptionHandler
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String NOT_LOGGED_IN_MESSAGE = "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn.";
    private static final String FORBIDDEN_MESSAGE = "Bạn không có quyền thực hiện chức năng này.";
    private static final String LOCKED_ACCOUNT_MESSAGE = "Tài khoản đã bị khoá.";

    private final ObjectMapper objectMapper;   // Spring Boot tao san, dung de doi ApiResponse -> JSON
    private final AuthService authService;     // hoi tai khoan con hoat dong khong o moi request co token

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF chi can khi dung cookie phien; API dung token gui trong header thi tat
                .csrf(csrf -> csrf.disable())
                // STATELESS: server khong luu phien dang nhap, moi request tu mang token theo
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // /error: Spring chuyen loi ve day; khoa lai thi moi loi deu bien thanh 401
                        .requestMatchers("/error").permitAll()
                        // BF-04: chi Ke toan them don gia (UC-55), chi Admin sua he so anh/video (UC-56)
                        .requestMatchers(HttpMethod.POST, "/api/royalty-rates").hasRole("ACCOUNTANT")
                        .requestMatchers(HttpMethod.PUT, "/api/media-factors/**").hasRole("ADMIN")
                        // BF-04: chi Thu ky chot ky (UC-50). Phai dat TRUOC luat /api/royalty-periods/**
                        // vi Spring xet tu tren xuong, gap luat khop dau tien la dung.
                        .requestMatchers(HttpMethod.POST, "/api/royalty-periods/*/close").hasRole("EDITORIAL_SECRETARY")
                        // BF-04: chi Ke toan tra ky (UC-52) va khoa ky (UC-53). Cung phai dat TRUOC /api/royalty-periods/**
                        .requestMatchers(HttpMethod.POST, "/api/royalty-periods/*/return").hasRole("ACCOUNTANT")
                        .requestMatchers(HttpMethod.POST, "/api/royalty-periods/*/lock").hasRole("ACCOUNTANT")
                        // BF-04: ky nhuan but co tong tien, chi Thu ky va Ke toan xem (UC-49, UC-50)
                        .requestMatchers("/api/royalty-periods/**").hasAnyRole("EDITORIAL_SECRETARY", "ACCOUNTANT")
                        // BF-04: nhuan but cua toi (UC-57): ai da dang nhap cung xem duoc phan tien cua chinh minh.
                        // Phai dat TRUOC luat /api/royalty-payments/** ben duoi.
                        .requestMatchers(HttpMethod.GET, "/api/royalty-payments/me").authenticated()
                        // BF-04: chi Ke toan xem phieu chi theo ky va ghi nhan da chi (UC-54)
                        .requestMatchers("/api/royalty-payments/**").hasRole("ACCOUNTANT")
                        // BF-04: cham nhuan but chi Thu ky toa soan (UC-46, 47, 48)
                        .requestMatchers("/api/royalty-gradings/**").hasRole("EDITORIAL_SECRETARY")
                        // BF-04: quan ly su kien bao hoa (xem tat ca, tao, sua, bat/tat) chi Thu ky toa soan.
                        // /api/events/open (o chon su kien luc PV nop bai) phai dat TRUOC de moi vai tro van goi duoc.
                        .requestMatchers(HttpMethod.GET, "/api/events/open").authenticated()
                        .requestMatchers("/api/events/**").hasRole("EDITORIAL_SECRETARY")
                        // BF-05: dinh muc chuan KPI (UC-58): Truong ban va Admin. Phai dat TRUOC /api/kpi-targets/**
                        .requestMatchers("/api/kpi-targets/benchmarks/**").hasAnyRole("HEAD_OF_DEPARTMENT", "ADMIN")
                        // BF-05: dat chi tieu thang (UC-59), bang KPI ban + tong hop (UC-61), bao cao the loai (UC-64): Truong ban
                        .requestMatchers("/api/kpi-targets/**").hasRole("HEAD_OF_DEPARTMENT")
                        .requestMatchers("/api/kpi-snapshots/dashboard", "/api/kpi-snapshots/aggregate").hasRole("HEAD_OF_DEPARTMENT")
                        .requestMatchers("/api/kpi-reports/**").hasRole("HEAD_OF_DEPARTMENT")
                        // BF-05: chi tiet KPI 1 phong vien (UC-60, 62): PV xem cua minh (service kiem), Truong ban xem moi PV
                        .requestMatchers("/api/kpi-snapshots/reporters/**").hasAnyRole("REPORTER", "HEAD_OF_DEPARTMENT")
                        // BF-05: chi Admin nhap so lieu doc gia tu CSV va xem nhat ky (UC-63)
                        .requestMatchers("/api/reader-imports/**").hasRole("ADMIN")
                        // Admin: xem / sua tham so nhap bai cua BF-01 (UC-13)
                        .requestMatchers("/api/system-parameters/**").hasRole("ADMIN")
                        // Admin: xem / tao / khoa / mo khoa tai khoan. Chi khop dung "/api/users" va
                        // "/api/users/{id}/lock|unlock" -> /api/users/me (ho so ca nhan) van cho moi vai tro.
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/lock", "/api/users/*/unlock").hasRole("ADMIN")
                        // BF-01: chi Phong vien tao / sua / nop / rut bai, xac nhan dong tac gia (UC-03, 05, 10, 11, 12).
                        // GET de nguyen vi luong khac cung doc qua /api/articles (vd BF-03 appeal-eligibility).
                        .requestMatchers(HttpMethod.POST, "/api/articles/**").hasRole("REPORTER")
                        .requestMatchers(HttpMethod.PUT, "/api/articles/**").hasRole("REPORTER")
                        .anyRequest().authenticated())
                // Bat che do kiem JWT: doc header Authorization, kiem chu ky + han dung bang JwtDecoder
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, NOT_LOGGED_IN_MESSAGE)))
                // Tra loi 401/403 dang ApiResponse tieng Viet (mac dinh Spring tra trang rong)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, ex) ->
                                writeError(response, HttpStatus.UNAUTHORIZED, NOT_LOGGED_IN_MESSAGE))
                        .accessDeniedHandler((request, response, ex) ->
                                writeError(response, HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE)));
        return http.build();
    }

    // Claim "role" trong token (vd "REPORTER") -> quyen "ROLE_REPORTER",
    // nho vay viet duoc .hasRole("REPORTER") khi can chan theo vai tro.
    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roleConverter = new JwtGrantedAuthoritiesConverter();
        roleConverter.setAuthoritiesClaimName(TokenService.ROLE_CLAIM);
        roleConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roleConverter);
        // Truoc khi doi token thanh "nguoi dang dang nhap" thi kiem tai khoan con hoat dong khong
        return jwt -> convertActiveUser(converter, jwt);
    }

    // Token con han nhung tai khoan da bi Admin khoa (hoac khong con) -> tu choi nhu chua dang nhap (401).
    // LockedException la loi dang nhap cua Spring Security, tu dong di vao authenticationEntryPoint ben tren.
    private AbstractAuthenticationToken convertActiveUser(JwtAuthenticationConverter converter, Jwt jwt) {
        Integer userId = Integer.valueOf(jwt.getSubject());
        if (!authService.isActiveUser(userId)) {
            throw new LockedException(LOCKED_ACCOUNT_MESSAGE);
        }
        return converter.convert(jwt);
    }

    // Ghi loi ra response dang { success: false, message: "..." } giong GlobalExceptionHandler
    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message, null));
    }
}
