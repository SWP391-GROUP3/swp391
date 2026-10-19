package com.swp391.kpi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
 * - BF-04: them don gia = Ke toan, sua he so anh/video = Admin, xem ky nhuan but = Thu ky + Ke toan
 * - moi API con lai: phai gui header "Authorization: Bearer <token>" hop le
 * Chan theo vai tro (vd chi Ke toan them don gia) -> them .requestMatchers(...).hasRole(...)
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String NOT_LOGGED_IN_MESSAGE = "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn.";
    private static final String FORBIDDEN_MESSAGE = "Bạn không có quyền thực hiện chức năng này.";

    private final ObjectMapper objectMapper;   // Spring Boot tao san, dung de doi ApiResponse -> JSON

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
                        // BF-04: ky nhuan but co tong tien, chi Thu ky va Ke toan xem (UC-49)
                        .requestMatchers("/api/royalty-periods/**").hasAnyRole("EDITORIAL_SECRETARY", "ACCOUNTANT")
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
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roleConverter = new JwtGrantedAuthoritiesConverter();
        roleConverter.setAuthoritiesClaimName(TokenService.ROLE_CLAIM);
        roleConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roleConverter);
        return converter;
    }

    // Ghi loi ra response dang { success: false, message: "..." } giong GlobalExceptionHandler
    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message, null));
    }
}
