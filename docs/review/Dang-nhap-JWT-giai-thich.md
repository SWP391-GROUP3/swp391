# Đăng nhập thật bằng Spring Security + JWT — giải thích chi tiết

> Nhánh `feature/admin-auth-login` · 13/10/2026 · Thay cho tài khoản giả `taiKhoanTam.js` và header `X-User-Id`.
> Đọc theo thứ tự: **Phần 1 lý thuyết → Phần 2 luồng chạy → Phần 3 từng file → Phần 4 kiểm thử → Phần 5 câu hỏi khi review.**

---

## Mục lục

1. [Lý thuyết nền](#phần-1--lý-thuyết-nền)
2. [Luồng chạy từ đầu đến cuối](#phần-2--luồng-chạy-từ-đầu-đến-cuối)
3. [Giải thích từng file](#phần-3--giải-thích-từng-file)
4. [Kiểm thử đã chạy](#phần-4--kiểm-thử-đã-chạy)
5. [Câu hỏi thầy có thể hỏi](#phần-5--câu-hỏi-thầy-có-thể-hỏi)
6. [Việc cả nhóm phải làm sau khi pull](#phần-6--việc-cả-nhóm-phải-làm-sau-khi-pull)
7. [Hạn chế còn lại](#phần-7--hạn-chế-còn-lại--việc-tiếp-theo)

---

## Phần 1 — Lý thuyết nền

### 1.1 Vì sao phải bỏ `X-User-Id`

Trước đây frontend tự gửi `X-User-Id: 8` và server **tin luôn** con số đó. Ai mở Postman gửi `X-User-Id: 8` là thành Admin, không cần mật khẩu. Ngoài ra code nhóm còn ghi cứng (`kpiApi.js` luôn gửi 5, `appealRuleConfigApi.js` luôn gửi 8), nên DB ghi sai người thao tác.

Giờ: muốn có danh tính phải **đăng nhập đúng mật khẩu**, server cấp **token có chữ ký**, và server tự đọc id từ token — frontend không tự khai được nữa.

### 1.2 Mật khẩu BCrypt

| Khái niệm | Giải thích |
|---|---|
| **Hash** | Hàm biến `"123456"` thành chuỗi dài, **một chiều**: từ hash không tính ngược ra mật khẩu. DB bị lộ thì kẻ gian cũng không đọc được mật khẩu. |
| **Vì sao không lưu mật khẩu thô** | Schema ghi rõ `password_hash -- BCrypt hash, KHÔNG lưu plain text`. Người dùng hay dùng 1 mật khẩu cho nhiều nơi, lộ DB là lộ hết. |
| **Muối (salt)** | BCrypt trộn thêm 1 chuỗi ngẫu nhiên trước khi băm, nên **cùng "123456" mỗi lần encode ra hash khác nhau**. Muối được cất ngay trong hash. |
| **So mật khẩu** | Không "giải mã" được, nên `matches("123456", hash)` lấy muối trong hash, băm lại `"123456"`, rồi so 2 kết quả. |
| **Đọc hash** `$2a$10$zT84p...` | `$2a$` = phiên bản BCrypt · `10` = độ khó (2¹⁰ vòng lặp — cố tình chậm để đoán mò rất tốn thời gian) · 22 ký tự tiếp = muối · phần còn lại = kết quả băm. |

### 1.3 JWT (JSON Web Token)

Token là 1 chuỗi 3 phần nối bằng dấu chấm: `header.payload.chữ-ký`

```
eyJhbGciOiJIUzI1NiJ9 . eyJpc3MiOiJzd3AzOTEta3BpIiwic3ViIjoiNiIsInJvbGUiOiJBQ0NPVU5UQU5UIi... . CdNG49RyEIGIgp0L...
      header                               payload                                          chữ ký
```

Giải mã base64 phần giữa của token thật lúc test (đăng nhập `chi.hl`):

```json
{ "iss": "swp391-kpi", "sub": "6", "role": "ACCOUNTANT", "iat": 1791908160, "exp": 1791936960 }
```

| Trường | Nghĩa |
|---|---|
| `iss` (issuer) | Ai phát token — hệ thống của mình |
| `sub` (subject) | **Id của user** (chi.hl có id 6) — controller dùng cái này thay `X-User-Id` |
| `role` | Mã vai trò, để chặn quyền theo vai trò |
| `iat` / `exp` | Lúc tạo / lúc hết hạn (giây). `exp - iat = 28800` giây = **8 tiếng** |

**Chữ ký** = HMAC-SHA256(header + payload, **khoá bí mật**). Chỉ server biết khoá, nên:
- Sửa payload (vd đổi `sub` thành 8) → chữ ký không khớp → server từ chối (đã thử thật, xem Phần 4).
- ⚠️ JWT **được ký chứ không được mã hoá**: ai cầm token cũng **đọc** được payload (base64 không phải mã hoá). Vì vậy **không bao giờ để mật khẩu hay dữ liệu bí mật trong payload**.

**HS256** = ký bằng 1 khoá bí mật dùng chung cho cả ký và kiểm. Hợp với dự án chỉ có 1 server.

### 1.4 Stateless (không lưu phiên)

| Cách cũ: Session | Cách mới: JWT |
|---|---|
| Server nhớ "phiên số X là của an.nv" trong bộ nhớ, trình duyệt giữ cookie phiên | Server **không nhớ gì**. Mọi thông tin cần thiết nằm trong token, mỗi request tự mang theo |
| Đăng xuất = server xoá phiên | Đăng xuất = **trình duyệt xoá token** (UC-70) |
| Cần chống CSRF vì cookie tự gửi kèm | Token gửi tay trong header `Authorization` → **không cần CSRF** |

### 1.5 Spring Security là gì

Một **chuỗi bộ lọc (filter chain)** đứng **trước** mọi controller:

```
Request ──► [Filter đọc token] ──► [Filter kiểm quyền] ──► Controller
                  │ sai/thiếu token          │ sai vai trò
                  ▼                          ▼
                 401                        403
```

Request không hợp lệ bị trả lời ngay, **không bao giờ vào tới controller**. Vì vậy controller không phải tự kiểm tra đăng nhập.

| Mã | Khi nào |
|---|---|
| **401 Unauthorized** | Chưa đăng nhập, token sai, token hết hạn, đăng nhập sai mật khẩu ("bạn là ai?") |
| **403 Forbidden** | Đã đăng nhập nhưng vai trò không được phép ("biết bạn là ai, nhưng không cho") |

### 1.6 `Authorization: Bearer <token>`

Header chuẩn của HTTP để gửi token. "Bearer" = "người cầm" — ai cầm token thì được đối xử như chủ token. Vì vậy **token phải giữ kín như mật khẩu**.

---

## Phần 2 — Luồng chạy từ đầu đến cuối

### 2.1 Đăng nhập

```
Trình duyệt (Login.jsx)            Server                                   DB
       │ POST /api/auth/login               │                                 │
       │ { login: "chi.hl", password }      │                                 │
       ├───────────────────────────────────►│ SecurityConfig: URL này permitAll
       │                                    │ AuthController.login()          │
       │                                    │  └► AuthService.login()         │
       │                                    │      1. findByUsernameOrEmail ──►│ SELECT ... WHERE username=? OR email=?
       │                                    │      2. BCrypt.matches(pass, hash)
       │                                    │      3. status == ACTIVE ?       │
       │                                    │      4. TokenService.createToken()
       │ 200 { token, id, fullName, role }  │                                 │
       │◄───────────────────────────────────┤                                 │
       │ luuPhien() → sessionStorage        │                                 │
       │ chuyển sang /main theo vai trò     │                                 │
```

### 2.2 Gọi 1 API bất kỳ sau khi đăng nhập

```
kpiApi.taoChiTieu(duLieu)
  └► goiApi('/api/kpi-targets', { method:'POST', body })
       gắn header  Authorization: Bearer eyJ...
            │
            ▼
Server: filter của oauth2ResourceServer
   1. Lấy token sau chữ "Bearer "
   2. JwtDecoder kiểm chữ ký bằng khoá bí mật + kiểm exp
   3. Sai → 401 (JSON tiếng Việt)      Đúng → tạo đối tượng Jwt, cho đi tiếp
            │
            ▼
KpiTargetController.createTarget(request, @AuthenticationPrincipal Jwt jwt)
   currentUserId = Integer.valueOf(jwt.getSubject())   // "5" → 5
   kpiTargetService.createTarget(request, currentUserId)
```

### 2.3 Token hết hạn / bị sửa

`goiApi` thấy **401 trong lúc đang có token** → xoá phiên → chuyển về `/login` → người dùng đăng nhập lại.

---

## Phần 3 — Giải thích từng file

### 3.0 Danh sách thay đổi

| Phía | File | Mới / Sửa | Việc |
|---|---|---|---|
| Server | `pom.xml` | Sửa | Thêm `spring-boot-starter-oauth2-resource-server` |
| Server | `config/JwtConfig.java` | Mới | Tạo bean JwtEncoder, JwtDecoder, PasswordEncoder |
| Server | `config/SecurityConfig.java` | Mới | Luật chặn: login mở, còn lại cần token; trả 401/403 JSON |
| Server | `entity/Role.java`, `RoleCode.java`, `User.java`, `UserStatus.java` | Mới | Ánh xạ bảng `roles`, `users` |
| Server | `repository/UserRepository.java` | Mới | Tìm user theo username hoặc email |
| Server | `dto/LoginRequest.java`, `LoginResponse.java` | Mới | Dữ liệu vào/ra API đăng nhập |
| Server | `service/AuthService.java` | Mới | Logic đăng nhập UC-69 |
| Server | `service/TokenService.java` | Mới | Tạo JWT |
| Server | `controller/AuthController.java` | Mới | `POST /api/auth/login` |
| Server | `exception/UnauthorizedException.java` + `GlobalExceptionHandler` | Mới + Sửa | Lỗi đăng nhập → 401 |
| Server | `RoyaltyRateController`, `KpiTargetController` (Khang), `AppealRuleConfigController` (Vũ) | Sửa | Bỏ `X-User-Id`, lấy id từ token |
| Server | `application.properties`, `.example` | Sửa | Thời hạn token, hướng dẫn khoá bí mật |
| Server | `AuthServiceTest.java`, `GlobalExceptionHandlerTest.java` | Mới + Sửa | 5 test đăng nhập + 1 test 401 |
| DB | `database/data.sql` | Sửa | Hash BCrypt thật cho 9 tài khoản mẫu → **PR "CÓ ĐỔI DB"** |
| Client | `auth/phienDangNhap.js` | Mới | Lưu/đọc/xoá `{token, nguoiDung}` |
| Client | `api/goiApi.js` | Mới | Hàm gọi API chung, tự gắn token |
| Client | `api/authApi.js` | Mới | Gọi API đăng nhập |
| Client | `App.jsx`, `Login.jsx` | Sửa | Đăng nhập thật |
| Client | `kpiApi.js` (Khang), `appealRuleConfigApi.js` (Vũ) | Sửa | Chuyển sang `goiApi` |
| Client | `ReporterLayout.jsx`, `Main.jsx`, `MonthlyTargetSetting.jsx`, `index.css` | Sửa | Bỏ phụ thuộc vào file tạm |
| Client | `auth/taiKhoanTam.js` | **Xoá** | Hết tài khoản giả + mật khẩu ghi trong code |

---

### 3.1 `pom.xml` — thêm 1 thư viện

```xml
<artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
```

Một gói **chính chủ của Spring**, mang theo 3 thứ: Spring Security (chuỗi bộ lọc), công cụ tạo/kiểm JWT (thư viện Nimbus bên trong), và `BCryptPasswordEncoder`.

**Vì sao không dùng `jjwt` như nhiều tutorial?** Với `jjwt` phải **tự viết 1 filter** đọc header, tách token, kiểm chữ ký, đặt user vào context — đây là phần dễ sai nhất về bảo mật và khó giải thích nhất. Gói resource-server làm sẵn phần đó: chỉ cần khai báo "dùng JWT" trong `SecurityConfig`.

---

### 3.2 `config/JwtConfig.java` — tạo các "bean"

**Bean** = đối tượng Spring tự tạo 1 lần và đưa (inject) vào chỗ nào cần. Hàm có `@Bean` trong class `@Configuration` = "Spring ơi, hãy tạo đối tượng này".

| Code | Giải thích |
|---|---|
| `@Value("${app.jwt.secret:}") private String secret;` | Đọc khoá từ properties. `:` = nếu chưa khai báo thì để chuỗi rỗng (để tự báo lỗi dễ hiểu ở dưới, thay vì lỗi khó hiểu của Spring) |
| `MIN_SECRET_BYTES = 32` | HS256 cần khoá ≥ 256 bit = 32 byte. Hằng số có tên, không "số ma" |
| `secretKey()` | Đổi chuỗi → `SecretKeySpec(..., "HmacSHA256")`. **Thiếu hoặc ngắn → `throw IllegalStateException` ngay lúc khởi động** kèm hướng dẫn (fail fast: lỗi lộ sớm, rõ ràng) |
| `jwtEncoder()` | `NimbusJwtEncoder` dùng khoá để **ký** token (TokenService dùng) |
| `jwtDecoder()` | `NimbusJwtDecoder.withSecretKey(...).macAlgorithm(HS256)` để **kiểm** chữ ký + hạn dùng ở mỗi request (Spring Security tự dùng) |
| `passwordEncoder()` | `new BCryptPasswordEncoder()` — AuthService dùng để so mật khẩu |

---

### 3.3 `config/SecurityConfig.java` — luật chặn

```java
.csrf(csrf -> csrf.disable())
```
CSRF là kiểu tấn công lợi dụng **cookie** đăng nhập có sẵn trong trình duyệt. Mình không dùng cookie mà gửi token trong header → tắt.

```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```
Server không tạo `HttpSession`, không nhớ ai đăng nhập (mục 1.4).

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
        .requestMatchers("/error").permitAll()
        .anyRequest().authenticated())
```
Đọc **từ trên xuống, luật đầu tiên khớp thì dùng**:
- Đăng nhập: ai cũng gọi được (chưa có token thì mới cần đăng nhập).
- `/error`: khi có lỗi Spring chuyển nội bộ về đây; khoá lại thì **mọi lỗi 400/404/500 đều biến thành 401**, rất khó debug.
- Còn lại: phải đã xác thực (có token hợp lệ).

```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())) ...)
```
Bật bộ lọc JWT: đọc header `Authorization: Bearer ...`, kiểm bằng `JwtDecoder` ở JwtConfig.

```java
.authenticationEntryPoint((request, response, ex) -> writeError(response, HttpStatus.UNAUTHORIZED, NOT_LOGGED_IN_MESSAGE))
.accessDeniedHandler((request, response, ex) -> writeError(response, HttpStatus.FORBIDDEN, FORBIDDEN_MESSAGE))
```
Mặc định Spring trả 401/403 với **body rỗng**. Hai dòng này cho trả `{ "success": false, "message": "Bạn chưa đăng nhập..." }` giống `GlobalExceptionHandler`, frontend hiển thị thẳng được. (Khai báo ở cả `oauth2ResourceServer` lẫn `exceptionHandling` để mọi đường 401 đều ra cùng định dạng.)

`jwtAuthenticationConverter()`: lấy claim `role` ("ACCOUNTANT") → quyền `ROLE_ACCOUNTANT`. Nhờ đó sau này viết được `.hasRole("ACCOUNTANT")` để trả 403 cho vai trò sai.

`writeError(...)`: đặt status, kiểu `application/json`, UTF-8 (để tiếng Việt không lỗi font), rồi dùng `ObjectMapper` (Spring Boot có sẵn) đổi `ApiResponse` thành JSON.

---

### 3.4 Entity: `Role`, `RoleCode`, `User`, `UserStatus`

Theo đúng AGENTS 3.3:

| Điểm | Lý do |
|---|---|
| `@Getter @Setter @NoArgsConstructor`, không `@Data` | `@Data` sinh `toString/hashCode` đi theo quan hệ → lặp vô hạn |
| `RoleCode`, `UserStatus` là **enum** + `@Enumerated(EnumType.STRING)` | Cột VARCHAR trạng thái/mã → enum. `STRING` lưu chữ "ACTIVE" chứ không lưu số 0/1 (chèn thêm giá trị vào giữa enum không làm sai dữ liệu cũ) |
| `@Nationalized` trên `name`, `fullName` | Cột `NVARCHAR` (tiếng Việt). Thiếu thì `ddl-auto=validate` báo lệch kiểu |
| `@ManyToOne(fetch = LAZY) @JoinColumn(name = "role_id") Role role` | Mỗi user 1 vai trò. LAZY = chỉ query bảng roles khi thật sự gọi `getRole()` |
| `Integer sectionId` | Bảng `sections` chưa có entity → tạm để số (AGENTS 3.3) |
| `passwordHash` | Chỉ lưu hash. **Không bao giờ trả ra API** (LoginResponse không có field này) |

Không có `@Column(name=...)`: Spring tự đổi `passwordHash` ↔ `password_hash`.

---

### 3.5 `repository/UserRepository.java`

```java
Optional<User> findByUsernameOrEmail(String username, String email);
```
**Derived query**: Spring đọc tên hàm và tự sinh `SELECT ... WHERE username = ? OR email = ?`. Gọi với cùng 1 giá trị cho cả 2 tham số → người dùng gõ username hay email đều được.

Vì sao nhận cả 2: **UC-69 ghi "username"**, còn màn Login của nhóm dùng **email**. Cả 2 cột đều `UNIQUE` nên nhận cả hai là thoả cả SRS lẫn giao diện.

`Optional` = "có thể không có". Không tìm thấy thì trả `Optional.empty()` chứ không trả `null`.

---

### 3.6 `dto/LoginRequest.java` và `LoginResponse.java`

`LoginRequest`: `login`, `password`, mỗi field có `@NotBlank(message = "...")` tiếng Việt → để trống thì `@Valid` ở controller chặn, trả 400 kèm lỗi từng ô (đã test).

`LoginResponse.from(user, token)`: map tay (AGENTS 3.4, không MapStruct). Trả `token`, `id`, `username`, `email`, `fullName`, `roleCode`, `roleName`, `sectionId`. **Không có `passwordHash`.**

Hàm `from()` gọi `user.getRole()` — role là LAZY nên phải gọi **bên trong transaction** của service (vì `open-in-view=false`, ra khỏi service là hết kết nối DB).

---

### 3.7 `service/AuthService.java` — trái tim của đăng nhập (UC-69)

```java
@Transactional(readOnly = true)
public LoginResponse login(LoginRequest request) {
    String login = request.getLogin().trim();                        // bỏ khoảng trắng thừa 2 đầu
    Optional<User> found = userRepository.findByUsernameOrEmail(login, login);
    if (found.isEmpty()) throw new UnauthorizedException(WRONG_LOGIN_MESSAGE);      // (1)
    User user = found.get();
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
        throw new UnauthorizedException(WRONG_LOGIN_MESSAGE);                          // (2)
    if (user.getStatus() != UserStatus.ACTIVE)
        throw new UnauthorizedException(LOCKED_MESSAGE);                                // (3)
    String token = tokenService.createToken(user);                                     // (4)
    return LoginResponse.from(user, token);
}
```

**Hai quyết định bảo mật cần nói được khi review:**

1. **(1) và (2) báo CÙNG một câu** "Email/tên đăng nhập hoặc mật khẩu không đúng." Nếu báo riêng "Email không tồn tại" thì kẻ gian dò được email nào có trong hệ thống.
2. **Kiểm `ACTIVE` (3) SAU mật khẩu (2).** Nếu kiểm trước, người không biết mật khẩu vẫn biết được "tài khoản này đang bị khoá". Chỉ ai nhập đúng mật khẩu mới thấy câu "Tài khoản đã bị khoá" (UC-69: chỉ ACTIVE mới được vào).

`@Transactional(readOnly = true)`: hàm chỉ đọc (AGENTS 3.2), và giữ kết nối mở để `getRole()` (LAZY) chạy được.

Đăng xuất (UC-70) **không cần API**: server không lưu phiên, frontend xoá token là xong.

---

### 3.8 `service/TokenService.java`

```java
JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(ISSUER)                                              // "swp391-kpi"
        .issuedAt(now)
        .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES)) // 480 phút
        .subject(String.valueOf(user.getId()))                       // sub = id user
        .claim(ROLE_CLAIM, user.getRole().getCode().name())          // role = "REPORTER"
        .build();
JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
```
`builder()` ở đây là API **có sẵn của Spring**, không phải pattern tự dựng (AGENTS chỉ cấm tự viết Builder). `expirationMinutes` đọc từ `application.properties` (không viết cứng số).

---

### 3.9 `controller/AuthController.java`

Đúng mẫu AGENTS 3.2: nhận request, `@Valid`, gọi **1** hàm service, trả `ResponseEntity<ApiResponse<...>>`. Không `if`, không `try/catch` — lỗi bay lên `GlobalExceptionHandler`.

---

### 3.10 `UnauthorizedException` + `GlobalExceptionHandler`

Cùng kiểu với `BusinessException` (409), `NotFoundException` (404) có sẵn: thêm 1 lớp lỗi riêng và 1 hàm `@ExceptionHandler` trả **401**.

---

### 3.11 Ba controller cũ — bỏ `X-User-Id`

Trước:
```java
@RequestHeader(value = "X-User-Id", defaultValue = "5") Integer currentUserId   // TODO(auth)
```
Sau:
```java
@AuthenticationPrincipal Jwt jwt
...
Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id nguoi dang nhap, lay tu token
```
`@AuthenticationPrincipal` = "đưa cho tôi **người đang đăng nhập**" — với JWT đó là đối tượng `Jwt` mà bộ lọc đã kiểm xong. `getSubject()` trả `"5"` → đổi sang `Integer`. **Service không phải sửa gì**, vẫn nhận `currentUserId` như cũ.

Lưu ý: `defaultValue = "5"` / `"8"` cũ rất nguy hiểm — quên gửi header vẫn chạy, ghi sai người mà không ai biết. Giờ không có token thì bị chặn 401 từ cửa.

---

### 3.12 Cấu hình & khoá bí mật

| File | Nội dung | Commit? |
|---|---|---|
| `application.properties` | `app.jwt.expiration-minutes=480` | Có (không bí mật) |
| `application-local.properties` | `app.jwt.secret=<chuỗi ngẫu nhiên 64 ký tự>` | **Không** (đã nằm trong `.gitignore`) |
| `application-local.properties.example` | Hướng dẫn + chuỗi mẫu `DOI_CHUOI_NAY` | Có |

Chuỗi mẫu **cố tình ngắn hơn 32 ký tự**: ai chép file `.example` mà quên đổi thì server báo lỗi ngay, không lặng lẽ chạy bằng một khoá cả nhóm đều biết. **Ai biết khoá là tự tạo được token giả** (vd token Admin) → không commit, không gửi lên nhóm chat.

---

### 3.13 `database/data.sql`

`'REPLACE_WITH_BCRYPT_HASH'` → `'$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm'` cho cả 9 user (hash của `123456`, tạo bằng chính `BCryptPasswordEncoder` của dự án và đã kiểm `matches("123456")=true`, `matches("1234567")=false`). Dùng chung 1 hash cho gọn — chỉ là dữ liệu mẫu.

---

### 3.14 Client: `auth/phienDangNhap.js`

4 hàm nhỏ quanh `sessionStorage` khoá `phienDangNhap`:

| Hàm | Việc |
|---|---|
| `docPhien()` | Đọc `{ token, nguoiDung }`. JSON hỏng hoặc **thiếu token** → coi như chưa đăng nhập. Xoá luôn khoá cũ `nguoiDung` của thời tài khoản giả |
| `luuPhien(phien)` | Lưu sau khi đăng nhập |
| `xoaPhien()` | Đăng xuất (UC-70) |
| `layToken()` | Cho `goiApi` gắn vào header |

**sessionStorage** (tự xoá khi đóng tab) thay vì localStorage (nằm lại mãi) — ở phòng máy trường, người sau không mở ra được tài khoản người trước.

### 3.15 Client: `api/goiApi.js` — hàm gọi API chung

Trước đây mỗi file `api/` tự viết `fetch` + `res.json()` + `if (!res.ok) throw` (lặp lại 9 lần). Giờ gom vào 1 chỗ:

```js
const token = layToken()
if (token) headers['Authorization'] = `Bearer ${token}`        // 1. gắn token
...
if (res.status === 401 && token) {                              // 4. token hết hạn / bị sửa
  xoaPhien(); window.location.href = '/login'; throw ...
}
if (!res.ok || json.success === false) throw new Error(chuLoi(json))   // 3. lỗi tiếng Việt
return json.data
```
Chú ý điều kiện `&& token`: **chưa có token mà bị 401** là đăng nhập sai mật khẩu → không chuyển trang, để màn Login tự hiện lỗi. `chuLoi()` ưu tiên lỗi từng ô (`errors` của `@Valid`), rồi đến `message`.

### 3.16 Client: `api/authApi.js` + `Login.jsx` + `App.jsx`

`dangNhap(login, matKhau)` gọi API rồi **đổi tên field** từ DTO tiếng Anh sang dạng các màn đang dùng: `fullName → hoTen`, `roleCode → vaiTro: ['REPORTER']` (mảng, vì `RequireRole`/`AppRoutes` dùng `.includes()`), `roleName → tenVaiTro`. Nhờ vậy **các màn khác không phải sửa**.

`Login.jsx`: ô nhập đổi thành "Email hoặc tên đăng nhập" (`type="text"`). Sai mật khẩu → hiện lỗi + xoá ô mật khẩu.

Dưới form có **danh sách 9 tài khoản mẫu** (lấy từ `data.sql`, mật khẩu chung `123456`); bấm 1 dòng là điền sẵn cả 2 ô. Danh sách bọc trong `{import.meta.env.DEV && (...)}`: biến này của Vite bằng `true` khi chạy `npm run dev` và `false` khi `npm run build`, nên **bản build để nộp/deploy tự loại bỏ cả khối** (đã kiểm: file build không chứa danh sách). Style để riêng trong `pages/Login.css` theo quy ước CSS cạnh component.

`App.jsx`: `useState(() => docPhien()?.nguoiDung ?? null)` — hàm trong `useState` chạy **1 lần** lúc mở app để F5 vẫn còn đăng nhập.

### 3.17 Client: file của Khang và Vũ

`kpiApi.js`, `appealRuleConfigApi.js`: giữ nguyên tên hàm, ruột đổi sang `goiApi(...)`. `taoChiTieu(duLieu)` bỏ tham số `userId` (server tự biết người giao từ token); `MonthlyTargetSetting.jsx` bỏ đối số `nguoiDung?.id || 5` tương ứng.

---

## Phần 4 — Kiểm thử đã chạy

### 4.1 Unit test (`mvn test`): **34/34 qua**

| Test mới | Kiểm gì |
|---|---|
| `loginReturnsTokenAndUserInfoWhenPasswordIsCorrect` | Đúng mật khẩu → có token, đúng id/vai trò; khoảng trắng thừa vẫn vào được |
| `loginRejectsWrongPassword` | Sai mật khẩu → 401, **không tạo token** |
| `loginRejectsUnknownAccountWithSameMessageAsWrongPassword` | Tài khoản không tồn tại → cùng câu với sai mật khẩu |
| `loginRejectsLockedAccount` | LOCKED + đúng mật khẩu → "Tài khoản đã bị khoá" |
| `lockedAccountWithWrongPasswordDoesNotRevealLockedStatus` | LOCKED + sai mật khẩu → chỉ câu chung, **không lộ** đang bị khoá |
| `returns401WhenUnauthorizedExceptionIsThrown` | `GlobalExceptionHandler` đổi lỗi thành 401 + JSON |

Test dùng **BCrypt thật** với hash trong `data.sql` → chứng minh hash mẫu khớp `123456`.

### 4.2 Gọi API thật (server chạy với DB thật)

| # | Thử | Kết quả |
|---|---|---|
| 1 | Gọi API không token | 401 "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn." |
| 2 | Token giả `abc.def.ghi` | 401 |
| 3 | Sai mật khẩu | 401 "Email/tên đăng nhập hoặc mật khẩu không đúng." |
| 4 | Để trống ô | 400 + lỗi từng ô |
| 5 | Đăng nhập bằng username `chi.hl` | 200, token + thông tin, không có hash |
| 6 | Gọi API kèm token thật | 200 |
| 7 | **Sửa payload thành `sub: 8, role: ADMIN`** (giả làm Admin) | **401 — chữ ký không khớp** |
| 8 | POST có token, body sai | Qua cửa Security, vào controller, chặn ở `@Valid` (400) |

### 4.3 Trình duyệt

| Thử | Kết quả |
|---|---|
| Sai mật khẩu | Hiện lỗi đỏ, ô mật khẩu tự xoá, không lưu phiên |
| Phóng viên `an.nv@toasoan.vn` | Vào khung Phóng viên, tên + "Phóng viên" lấy từ DB |
| Admin bằng username `admin` | 2 tab Khang + Vũ, mọi API trả 200 (tức là đã tự gửi token) |
| F5 | Vẫn đăng nhập |
| Sửa token trong sessionStorage rồi gọi API | Tự xoá phiên, về `/login` |
| Đăng xuất | Xoá phiên, về `/login` |

---

## Phần 5 — Câu hỏi thầy có thể hỏi

**H: Vì sao không lưu mật khẩu mà lưu hash? Đăng nhập so sánh kiểu gì?**
Đ: Hash một chiều, lộ DB không ra mật khẩu. Đăng nhập dùng `passwordEncoder.matches(matKhauGoVao, hash)`: lấy muối trong hash, băm lại mật khẩu gõ vào, so kết quả (mục 1.2).

**H: Cùng mật khẩu 123456 sao hash mỗi lần một khác?**
Đ: Vì muối ngẫu nhiên. Trong `data.sql` 9 user cùng 1 hash vì mình encode 1 lần rồi dùng chung cho dữ liệu mẫu.

**H: JWT có bảo mật không, ai đọc được payload không?**
Đ: **Đọc được** (base64), nhưng **không sửa được** vì có chữ ký HMAC bằng khoá chỉ server biết — đã thử sửa `sub` thành 8 → 401. Nên không để dữ liệu bí mật trong payload.

**H: Server lấy id người đang thao tác ở đâu?**
Đ: `@AuthenticationPrincipal Jwt jwt` → `jwt.getSubject()`. Bộ lọc Spring Security đã kiểm chữ ký trước khi vào controller.

**H: Đăng xuất làm gì ở server?**
Đ: Không gì cả, vì stateless: xoá token ở trình duyệt. Hạn chế: token đã lộ vẫn dùng được tới khi hết 8 tiếng (mục 7).

**H: 401 khác 403 thế nào?**
Đ: 401 = chưa xác định được bạn là ai (chưa đăng nhập / token sai / sai mật khẩu). 403 = biết bạn là ai nhưng vai trò không được phép.

**H: Vì sao tắt CSRF?**
Đ: CSRF lợi dụng cookie tự gửi kèm. Mình gửi token tay trong header `Authorization`, trang web lạ không tự gửi được header này.

**H: Vì sao sai email và sai mật khẩu báo cùng một câu? Vì sao kiểm khoá sau mật khẩu?**
Đ: Để không ai dò được email nào tồn tại hoặc tài khoản nào bị khoá (mục 3.7).

**H: Khoá bí mật để ở đâu, lộ thì sao?**
Đ: `application-local.properties`, đã `.gitignore`. Lộ khoá = ai cũng tạo được token giả (kể cả Admin) → phải đổi khoá; mọi token cũ tự mất hiệu lực.

**H: Request đi qua những gì trước khi vào controller?**
Đ: Chuỗi bộ lọc Spring Security: bộ lọc đọc Bearer token → `JwtDecoder` kiểm chữ ký + hạn → kiểm luật `authorizeHttpRequests` → controller (mục 1.5, 2.2).

**H: `@Bean`, `@Configuration` là gì?**
Đ: `@Configuration` đánh dấu class cấu hình, Spring tự quét. Hàm `@Bean` trả về đối tượng mà Spring tạo 1 lần và inject vào nơi cần (vd `PasswordEncoder` inject vào `AuthService` qua constructor do `@RequiredArgsConstructor` sinh).

---

## Phần 6 — Việc cả nhóm phải làm sau khi pull

1. **Thêm khoá bí mật** vào `server/src/main/resources/application-local.properties` (mỗi máy tự tạo, không gửi nhau):
   ```bash
   openssl rand -base64 48
   ```
   rồi thêm dòng `app.jwt.secret=<kết quả>`. Thiếu dòng này server **không khởi động** và báo rõ lý do.
2. **Cập nhật mật khẩu mẫu trong DB** — chọn 1 trong 2:
   - Chạy lại `schema.sql` rồi `data.sql` (**mất dữ liệu đang có**), hoặc
   - Giữ dữ liệu, chỉ chạy câu này trong SSMS:
     ```sql
     UPDATE users SET password_hash = '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm'
     WHERE password_hash = 'REPLACE_WITH_BCRYPT_HASH';
     ```
3. **Khởi động lại server** (Maven tải thêm thư viện lần đầu).
4. Đăng nhập: email **hoặc** username trong `data.sql`, mật khẩu `123456`.
5. **Code mới:** backend lấy người thao tác bằng `@AuthenticationPrincipal Jwt jwt`; frontend gọi API bằng `goiApi(...)` — không dùng `fetch` trực tiếp, không gửi `X-User-Id` (AGENTS 3.8, mục 4).

PR này phải ghi **"CÓ ĐỔI DB"** ở đầu mô tả (đổi `data.sql`) và nêu: thêm thư viện vào `pom.xml`, sửa file dùng chung (`config/`, `exception/`, `App.jsx`, `index.css`), sửa file của Khang (`KpiTargetController`, `kpiApi.js`, `MonthlyTargetSetting.jsx`) và Vũ (`AppealRuleConfigController`, `appealRuleConfigApi.js`).

---

## Phần 7 — Hạn chế còn lại / việc tiếp theo

| Hạn chế | Ảnh hưởng | Hướng làm sau |
|---|---|---|
| **Chưa chặn theo vai trò** | Đã đăng nhập là gọi được mọi API (vd Phóng viên gọi được API thêm đơn giá của Kế toán) | Thêm `.requestMatchers(...).hasRole(...)` trong `SecurityConfig` theo Actor của từng UC — mỗi luồng tự đề xuất cho API của mình |
| Token không thu hồi được | Lộ token thì dùng được tới khi hết 8 tiếng | Rút ngắn thời hạn, hoặc thêm refresh token / danh sách đen (ngoài phạm vi môn) |
| Token nằm trong sessionStorage | Nếu trang dính lỗi XSS thì script lạ đọc được token | React đã tự escape dữ liệu khi hiển thị; tránh `dangerouslySetInnerHTML` |
| Chưa giới hạn số lần đăng nhập sai | Có thể đoán mò mật khẩu | BCrypt chậm đã giảm tốc độ đoán; nếu cần thì khoá tạm sau N lần sai |
| `MonthlyTargetSetting` còn prop `nguoiDung` không dùng | oxlint cảnh báo, không lỗi | Khang tự quyết bỏ hay giữ |
| AGENTS nói "thông tin đăng nhập để trong Context" | Hiện vẫn truyền qua props như code cũ | Chuyển sang React Context khi số màn tăng |
