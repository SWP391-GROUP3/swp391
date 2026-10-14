package com.swp391.kpi.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Tao cac "bean" dung cho dang nhap (file dung chung):
 * - JwtEncoder: ky (tao) token khi dang nhap thanh cong
 * - JwtDecoder: kiem chu ky + han dung cua token o moi request
 * - PasswordEncoder: ma hoa / so mat khau BCrypt
 *
 * Token ky bang HS256: 1 khoa bi mat dung chung cho ca ky va kiem.
 * Khoa nam trong application-local.properties (app.jwt.secret), KHONG commit.
 */
@Configuration
public class JwtConfig {

    // HS256 can khoa toi thieu 256 bit = 32 byte
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${app.jwt.secret:}")    // ":" = mac dinh rong neu chua khai bao
    private String secret;

    // Doi chuoi bi mat thanh khoa HMAC. Thieu / qua ngan -> dung app ngay luc khoi dong
    // de nguoi chay biet phai them app.jwt.secret, thay vi loi kho hieu luc dang nhap.
    private SecretKey secretKey() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "Thieu app.jwt.secret (toi thieu 32 ky tu) trong application-local.properties. "
                    + "Xem application-local.properties.example.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
