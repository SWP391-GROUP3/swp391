package com.swp391.kpi.service;

import com.swp391.kpi.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tao JWT cho user vua dang nhap dung.
 * Noi dung token (payload):
 *   sub  = id user (controller lay ra de biet ai dang thao tac)
 *   role = ma vai tro, vd REPORTER (SecurityConfig doi thanh quyen ROLE_REPORTER)
 *   iat / exp = luc tao / luc het han
 *
 * MUC LUC (so dong - ten - tac dung)
 *   32  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   35  field                           repository / service duoc inject
 *   40  createToken()                   tao token
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    public static final String ROLE_CLAIM = "role";
    private static final String ISSUER = "swp391-kpi";

    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.expiration-minutes}")
    private long expirationMinutes;

    public String createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .subject(String.valueOf(user.getId()))
                .claim(ROLE_CLAIM, user.getRole().getCode().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
