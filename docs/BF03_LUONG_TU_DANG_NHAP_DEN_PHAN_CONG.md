# BF-03: Từ đăng nhập đến Trưởng ban phân công biên tập viên

Tài liệu này mô tả luồng đi từ lúc đăng nhập đến lúc Trưởng ban (TB) giao phiếu chấm mù cho Biên tập viên (BTV). Mỗi bước có file, số dòng, đoạn code và giải thích.

Gồm ba phần:
1. Đăng nhập.
2. Phóng viên (PV) gửi khiếu nại (tóm tắt, để biết TB nhận được gì).
3. Trưởng ban xem hàng chờ và phân công BTV.

Quá trình BTV chấm mù nằm ngoài phạm vi tài liệu này.

Đọc theo thứ tự: **giao diện (React) → hàm gọi API → Controller → Service → Repository/DB**.

---

## 1. Đăng nhập

### 1.1 Front-end

**Bước 1: Form đăng nhập gửi thông tin** — [Login.jsx:52-66](../client/src/pages/Login.jsx)

```jsx
async function guiForm(e) {
  e.preventDefault()   // chan trinh duyet tai lai trang theo kieu form HTML co
  setLoi('')
  setDangGui(true)

  try {
    const phien = await dangNhap(tenDangNhap.trim(), matKhau)
    onDangNhap(phien)
  } catch (err) {
    setLoi(err.message)
    setMatKhau('')       // xoa mat khau, giu lai ten dang nhap da go
  } finally {
    setDangGui(false)
  }
}
```

*Làm gì:* lấy tên đăng nhập và mật khẩu, gọi `dangNhap()`. Nếu thành công thì báo màn hình cha, nếu lỗi thì hiện thông báo.
*Vì sao:* `preventDefault()` chặn trình duyệt tải lại trang. Xóa mật khẩu khi lỗi để không giữ lại trên màn hình.

**Bước 2: Gọi API và đổi dữ liệu server** — [authApi.js:14-31](../client/src/api/authApi.js)

```js
export async function dangNhap(login, matKhau) {
  const data = await goiApi('/api/auth/login', {
    method: 'POST',
    body: { login, password: matKhau },
  })
  return {
    token: data.token,
    nguoiDung: {
      id: data.id,
      username: data.username,
      email: data.email,
      hoTen: data.fullName,
      vaiTro: [data.roleCode],      // mang: RequireRole, AppRoutes dang dung .includes()
      tenVaiTro: data.roleName,     // "Phóng viên", "Kế toán"... de hien len man hinh
      sectionId: data.sectionId,
    },
  }
}
```

*Làm gì:* gửi `POST /api/auth/login`, rồi đổi tên trường từ tiếng Anh của server sang dạng app đang dùng.
*Vì sao:* `vaiTro` là mảng để các màn hình dùng `.includes()` thống nhất.

**Bước 3: Hàm gọi API chung** — [goiApi.js:20-54](../client/src/api/goiApi.js)

```js
export async function goiApi(duongDan, tuyChon = {}) {
  const headers = {}
  const isFormData = typeof FormData !== 'undefined' && tuyChon.body instanceof FormData
  if (tuyChon.body !== undefined && !isFormData) {
    headers['Content-Type'] = 'application/json'
  }
  const token = layToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(duongDan, {
    method: tuyChon.method || 'GET',
    headers,
    body: tuyChon.body !== undefined ? (isFormData ? tuyChon.body : JSON.stringify(tuyChon.body)) : undefined,
  })

  // Dang co token ma van 401 = token het han hoac bi sua -> bat dang nhap lai.
  // (Chua co token ma 401 thi la dang nhap sai, de Login tu hien loi.)
  if (res.status === 401 && token) {
    xoaPhien()
    window.location.href = '/login'
    const loi = new Error('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.')
    loi.status = res.status
    throw loi
  }

  const json = await docJson(res)
  if (!res.ok || json.success === false) {
    const loi = new Error(chuLoi(json))
    loi.status = res.status
    throw loi
  }
  return json.data
}
```

*Làm gì:* gắn token vào mọi request, đổi lỗi server thành `Error` tiếng Việt, trả về phần `data`.
*Vì sao:* mọi API đi qua một hàm nên chỉ viết logic gắn token và xử lý lỗi một lần. Lần đăng nhập này chưa có token nên không có header `Authorization`.

**Bước 4: Lưu phiên và cập nhật state** — [App.jsx:18-24](../client/src/App.jsx) và [phienDangNhap.js:42-44](../client/src/auth/phienDangNhap.js)

```jsx
const [nguoiDung, setNguoiDung] = useState(() => docPhien()?.nguoiDung ?? null)

// phien = { token, nguoiDung } tu authApi.dangNhap()
function dangNhap(phien) {
  luuPhien(phien)
  setNguoiDung(phien.nguoiDung)
}
```

```js
export function luuPhien(phien) {
  sessionStorage.setItem(KHOA_PHIEN, JSON.stringify(phien))
}
```

*Làm gì:* lưu `{ token, nguoiDung }` vào `sessionStorage`, rồi cập nhật state để React vẽ lại.
*Vì sao:* state được khởi tạo bằng `docPhien()` nên tải lại trang vẫn còn đăng nhập.

**Bước 5: Chuyển về trang chính** — [AppRoutes.jsx:201-209](../client/src/routes/AppRoutes.jsx)

```jsx
<Route
  path="/login"
  element={
    // Da dang nhap roi thi khong cho xem lai form nua
    nguoiDung
      ? <Navigate to="/main" replace />
      : <Login onDangNhap={onDangNhap} />
  }
/>
```

*Làm gì:* đã có `nguoiDung` thì chuyển về `/main`.
*Vì sao:* người đã đăng nhập không cần thấy form lần nữa.

**Bước 6: Gắn token cho các lần gọi sau** — [goiApi.js:26-29](../client/src/api/goiApi.js) và [phienDangNhap.js:52-55](../client/src/auth/phienDangNhap.js)

```js
const token = layToken()
if (token) {
  headers['Authorization'] = `Bearer ${token}`
}
```

```js
export function layToken() {
  const phien = docPhien()
  return phien ? phien.token : null
}
```

*Làm gì:* mọi API sau đăng nhập đều có header `Authorization: Bearer ...`.

### 1.2 Back-end

**Bước 7: Controller nhận request** — [AuthController.java:29-34](../server/src/main/java/com/swp391/kpi/common/controller/AuthController.java)

```java
// POST /api/auth/login  body: { "login": "an.nv@toasoan.vn", "password": "123456" }
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse data = authService.login(request);
    return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
}
```

*Làm gì:* kiểm `@Valid`, chuyển cho service, trả `ApiResponse`.
*Vì sao:* controller không có logic nghiệp vụ.

**Bước 8: Service kiểm tài khoản và mật khẩu** — [AuthService.java:39-63](../server/src/main/java/com/swp391/kpi/common/service/AuthService.java)

```java
@Transactional(readOnly = true)
public LoginResponse login(LoginRequest request) {
    String login = request.getLogin().trim();

    // 1. Tim theo username hoac email
    Optional<User> found = userRepository.findByUsernameOrEmail(login, login);
    if (found.isEmpty()) {
        throw new UnauthorizedException(WRONG_LOGIN_MESSAGE);
    }
    User user = found.get();

    // 2. So mat khau go vao voi hash BCrypt trong DB (BCrypt tu lay "muoi" trong hash ra de so)
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        throw new UnauthorizedException(WRONG_LOGIN_MESSAGE);
    }

    // 3. Chi tai khoan ACTIVE moi duoc vao (UC-69). Kiem SAU mat khau:
    //    ai khong biet mat khau thi khong biet duoc tai khoan co bi khoa hay khong.
    if (user.getStatus() != UserStatus.ACTIVE) {
        throw new UnauthorizedException(LOCKED_MESSAGE);
    }

    // 4. Tao token, tra kem thong tin user
    String token = tokenService.createToken(user);
    return LoginResponse.from(user, token);
}
```

*Làm gì:* tìm user, so mật khẩu BCrypt, kiểm trạng thái `ACTIVE`, rồi tạo token.
*Vì sao:* kiểm mật khẩu trước, rồi mới kiểm trạng thái, để người không biết mật khẩu không biết tài khoản có bị khóa không. Thông báo lỗi giống nhau cho sai tên và sai mật khẩu.

**Bước 9: Tạo JWT** — [TokenService.java:40-51](../server/src/main/java/com/swp391/kpi/common/service/TokenService.java)

```java
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
```

*Làm gì:* tạo JWT có `subject` là id user và claim `role`, rồi ký bằng HS256.
*Vì sao:* các API sau lấy `userId` từ `subject` và kiểm vai trò từ claim, không tin dữ liệu client gửi lên.

**Bước 10: Chặn request không có token** — [SecurityConfig.java:61-62](../server/src/main/java/com/swp391/kpi/config/SecurityConfig.java), [84](../server/src/main/java/com/swp391/kpi/config/SecurityConfig.java), [86-87](../server/src/main/java/com/swp391/kpi/config/SecurityConfig.java)

```java
.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
        ...
        .anyRequest().authenticated())
// Bat che do kiem JWT: doc header Authorization, kiem chu ky + han dung bang JwtDecoder
.oauth2ResourceServer(oauth2 -> oauth2
        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
```

*Làm gì:* chỉ `POST /api/auth/login` được gọi không cần token. Mọi đường dẫn khác phải có JWT hợp lệ.
*Vì sao:* `.anyRequest().authenticated()` là lưới an toàn. Các API của luồng BF-03 không có luật riêng ở đây, nên quyền được kiểm trong service.

---

## 2. Phóng viên gửi khiếu nại (tóm tắt)

Phần này để hiểu dữ liệu Trưởng ban nhận được.

**Bước 11: Route chỉ dành cho phóng viên** — [AppRoutes.jsx:231-234](../client/src/routes/AppRoutes.jsx)

```jsx
{vaiTro === 'REPORTER' && (
  <>
    <Route path="/my-appeals" element={<MyAppealsPage />} />
    <Route path="/my-appeals/new" element={<AppealCreatePage />} />
```

**Bước 12: Lấy bài đủ điều kiện** — [AppealCreatePage.jsx:42-48](../client/src/pages/appeal/AppealCreatePage.jsx) → [appealApi.js:38-40](../client/src/api/appealApi.js) → [AppealController.java:41-47](../server/src/main/java/com/swp391/kpi/appeal/controller/AppealController.java)

```jsx
useEffect(() => {
  let conHienThi = true
  layBaiCoTheKhieuNai()
    .then((data) => { if (conHienThi) setBaiCoTheKhieuNai(data) })
    .catch((error) => { if (conHienThi) setLoi(error.message) })
  return () => { conHienThi = false }
}, [])
```

```js
export async function layBaiCoTheKhieuNai() {
  return goiApi('/api/appeals/eligible-articles')
}
```

```java
@GetMapping("/appeals/eligible-articles")
public ResponseEntity<ApiResponse<List<AppealEligibilityResponse>>> getEligibleArticles(
        @AuthenticationPrincipal Jwt jwt) {
    Integer currentUserId = Integer.valueOf(jwt.getSubject());
    return ResponseEntity.ok(ApiResponse.success("Lấy bài có thể khiếu nại thành công",
            appealService.getEligibleArticles(currentUserId)));
}
```

*Làm gì:* lấy `userId` từ token, gọi service. Service gọi `getEligibility()` cho từng bài ([AppealService.java:174-179](../server/src/main/java/com/swp391/kpi/appeal/service/AppealService.java)).

**Bước 13: Xem điều kiện của một bài** — [AppealCreatePage.jsx:50-69](../client/src/pages/appeal/AppealCreatePage.jsx) → [appealApi.js:26-28](../client/src/api/appealApi.js) → [AppealController.java:67-72](../server/src/main/java/com/swp391/kpi/appeal/controller/AppealController.java)

Khi người dùng chọn bài, `articleId` trên URL đổi và effect chạy lại:

```jsx
useEffect(() => {
  if (!articleIdHopLe) return undefined

  let conHienThi = true
  layDieuKienKhieuNai(articleId)
    .then((data) => {
      if (conHienThi) {
        setDuLieu(data)
        setLyDoTheoTieuChi({})
        setLoi('')
      }
    })
    .catch((error) => {
      if (conHienThi) {
        setDuLieu(null)
        setLoi(error.message)
      }
    })
  return () => { conHienThi = false }
}, [articleId, articleIdHopLe])
```

```js
export async function layDieuKienKhieuNai(articleId) {
  return goiApi(`/api/articles/${articleId}/appeal-eligibility`)
}
```

```java
@GetMapping("/articles/{articleId}/appeal-eligibility")
public ResponseEntity<ApiResponse<AppealEligibilityResponse>> getEligibility(
        @PathVariable Integer articleId,
        @AuthenticationPrincipal Jwt jwt) {
    Integer currentUserId = Integer.valueOf(jwt.getSubject());
    AppealEligibilityResponse data = appealService.getEligibility(articleId, currentUserId);
```

*Làm gì:* hiện điểm, hạn khiếu nại và các tiêu chí. Đây là API chỉ đọc.

**Bước 14: Gửi khiếu nại** — [AppealCreatePage.jsx:87-109](../client/src/pages/appeal/AppealCreatePage.jsx) → [appealApi.js:30-32](../client/src/api/appealApi.js)

```jsx
async function guiForm(event) {
  event.preventDefault()
  setLoi('')
  const criteria = Object.entries(lyDoTheoTieuChi).map(([criterionId, reason]) => ({
    criterionId: Number(criterionId),
    reason: reason.trim(),
  }))

  if (criteria.length === 0 || criteria.some((item) => !item.reason)) {
    setLoi('Phải chọn ít nhất một tiêu chí và nhập lý do cho từng tiêu chí.')
    return
  }

  setDangGui(true)
  try {
    await guiKhieuNai(articleId, criteria)
    navigate('/my-appeals', { state: { thongBao: 'Gửi khiếu nại thành công.' } })
  } catch (error) {
    setLoi(error.message)
  } finally {
    setDangGui(false)
  }
}
```

```js
export async function guiKhieuNai(articleId, criteria) {
  return goiApi('/api/appeals', { method: 'POST', body: { articleId, criteria } })
}
```

*Làm gì:* kiểm ở client rằng có tiêu chí và có lý do, rồi gửi `POST /api/appeals`.

**Bước 15: Controller nhận khiếu nại** — [AppealController.java:49-57](../server/src/main/java/com/swp391/kpi/appeal/controller/AppealController.java)

```java
@PostMapping("/appeals")
public ResponseEntity<ApiResponse<AppealResponse>> submitAppeal(
        @Valid @RequestBody AppealCreateRequest request,
        @AuthenticationPrincipal Jwt jwt) {
    Integer currentUserId = Integer.valueOf(jwt.getSubject());
    AppealResponse data = appealService.submitAppeal(request, currentUserId);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Gửi khiếu nại thành công", data));
}
```

**Bước 16: Service lưu khiếu nại** — [AppealService.java:105-139](../server/src/main/java/com/swp391/kpi/appeal/service/AppealService.java)

```java
@Transactional
public AppealResponse submitAppeal(AppealCreateRequest request, Integer currentUserId) {
    userReadService.requireActive(currentUserId, "REPORTER");
    AppealSubmissionContext context = appealSubmissionDataProvider
            .getSubmissionContext(request.getArticleId());
    AppealRuleConfig ruleConfig = appealRuleConfigService.getActiveConfigEntity();
    validateSubmission(request, currentUserId, context, ruleConfig);

    Appeal appeal = new Appeal();
    appeal.setArticleId(context.getArticleId());
    appeal.setScoringAttemptId(context.getScoringAttemptId());
    appeal.setAppellantId(currentUserId);
    appeal.setRuleConfigId(ruleConfig.getId());
    appeal.setStatus(AppealStatus.SUBMITTED);
    appeal.setSubmittedAt(LocalDateTime.now(clock));
    Appeal savedAppeal;
    try {
        savedAppeal = appealRepository.save(appeal);
    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
        // Hai request cung luc: UNIQUE(appeals.article_id) chan cai den sau, tra dung cau cua UC-35 E2.
        throw new BusinessException("Mỗi bài viết chỉ được khiếu nại một lần.");
    }

    List<AppealCriterion> savedCriteria = saveCriteria(savedAppeal, request.getCriteria());
    notificationService.notifyHeads(savedAppeal.getArticleId(),
            articleReadService.getArticle(savedAppeal.getArticleId()).sectionId(),
            "Có khiếu nại mới #" + savedAppeal.getId() + " về điểm bài viết #" + savedAppeal.getArticleId() + ".");
    auditService.log(
            currentUserId,
            "CREATE",
            "appeals",
            savedAppeal.getId(),
            null,
            "{\"articleId\":" + savedAppeal.getArticleId() + ",\"status\":\"SUBMITTED\"}"
    );
```

*Làm gì:* kiểm role, kiểm dữ liệu, lưu `appeals` và `appeal_criteria`, báo Trưởng ban, ghi nhật ký.
*Vì sao:* UNIQUE trong DB là lưới an toàn khi hai request gửi cùng lúc. `catch` đổi lỗi đó thành thông báo tiếng Việt.

**Bước 17: Kiểm dữ liệu khiếu nại** — [AppealService.java:202-235](../server/src/main/java/com/swp391/kpi/appeal/service/AppealService.java)

```java
private void validateSubmission(
        AppealCreateRequest request,
        Integer currentUserId,
        AppealSubmissionContext context,
        AppealRuleConfig ruleConfig) {
    validateArticleAndWindow(currentUserId, context, ruleConfig);
    if (appealRepository.existsByArticleId(context.getArticleId())) {
        throw new BusinessException("Mỗi bài viết chỉ được khiếu nại một lần.");
    }
    validateCriteria(request.getCriteria(), context);
}

private void validateArticleAndWindow(
        Integer currentUserId,
        AppealSubmissionContext context,
        AppealRuleConfig ruleConfig) {
    if (!context.getMainAuthorId().equals(currentUserId)) {
        throw new BusinessException("Chỉ tác giả chính được gửi khiếu nại.");
    }
    if (!SCORE_PUBLISHED.equals(context.getArticleStatus())
            && !PUBLISHED.equals(context.getArticleStatus())) {
        throw new BusinessException("Bài viết chưa đủ điều kiện để khiếu nại.");
    }
    if (context.getAnnouncedAt() == null) {
        throw new BusinessException("Kết quả chấm chưa được công bố.");
    }
    if (context.getAnnouncedAt().isAfter(LocalDateTime.now(clock))) {
        throw new BusinessException("Kết quả chấm chưa được công bố.");
    }
    LocalDateTime deadline = context.getAnnouncedAt().plusDays(ruleConfig.getAppealWindowDays());
    if (LocalDateTime.now(clock).isAfter(deadline)) {
        throw new BusinessException("Thời hạn khiếu nại đã kết thúc.");
    }
}
```

*Làm gì:* kiểm người gửi là tác giả chính, bài đã được công bố điểm, còn trong hạn, và mỗi bài chỉ có một khiếu nại.
*Vì sao:* kiểm trước để trả thông báo tiếng Việt rõ ràng. UNIQUE trong DB chỉ là lưới an toàn.

**Lưu ý:** `validateSubmission()` không kiểm `max_appeals_per_article`. Cấu hình này chưa có tác dụng.

---

## 3. Trưởng ban xem hàng chờ và phân công BTV

### 3.1 Vào trang khiếu nại

**Bước 18: Menu và route chỉ dành cho Trưởng ban** — [HeadLayout.jsx:39](../client/src/layouts/HeadLayout.jsx), [AppRoutes.jsx:258](../client/src/routes/AppRoutes.jsx) và [267-268](../client/src/routes/AppRoutes.jsx)

```jsx
{ path: '/appeals', nhan: 'Khiếu nại',                             // UC-38, 39, 37 (xac nhan S2), 43 (giao lai)
```

```jsx
{vaiTro === 'HEAD_OF_DEPARTMENT' && (
  ...
  <Route path="/appeals" element={<AppealManagementPage />} />
  <Route path="/appeals/:id" element={<AppealDetailPage />} />
```

Trang chủ Trưởng ban cũng có liên kết: [HeadHome.jsx:70](../client/src/pages/home/HeadHome.jsx) `{ nhan: 'Khiếu nại đang mở', ..., duongDan: '/appeals' }`.

**Bước 19: Tải hàng chờ** — [AppealManagementPage.jsx:40-44](../client/src/pages/appeal/AppealManagementPage.jsx) → [appealApi.js:42-50](../client/src/api/appealApi.js)

```jsx
useEffect(() => {
  let conHienThi = true
  layHangChoKhieuNai(Object.fromEntries(new URLSearchParams(khoa)))
    .then((data) => { if (conHienThi) setKetQua({ khoa, duLieu: data, loi: '' }) })
    .catch((error) => { if (conHienThi) setKetQua({ khoa, duLieu: null, loi: error.message }) })
  return () => { conHienThi = false }
}, [khoa])
```

```js
export function layHangChoKhieuNai(boLoc = {}) {
  const thamSo = new URLSearchParams()
  for (const ten of ['status', 'articleId', 'reviewerId', 'fromDate', 'toDate']) {
    if (boLoc[ten]) thamSo.set(ten, boLoc[ten])
  }
  thamSo.set('page', boLoc.page ?? 0)
  thamSo.set('size', 10)
  return goiApi(`/api/appeals?${thamSo}`)
}
```

*Làm gì:* gửi `GET /api/appeals` với bộ lọc trên URL. Khi bộ lọc đổi thì `khoa` đổi và tải lại.

**Bước 20: Controller hàng chờ** — [AppealManagementController.java:34-42](../server/src/main/java/com/swp391/kpi/appeal/controller/AppealManagementController.java)

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<AppealManagementResponse>>> getQueue(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required = false) String status, @RequestParam(required = false) Integer articleId,
        @RequestParam(required = false) Integer reviewerId, @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khiếu nại thành công", service.getQueue(
            Integer.valueOf(jwt.getSubject()), status, articleId, reviewerId, fromDate, toDate, page, size)));
}
```

**Bước 21: Service lọc hàng chờ** — [AppealManagementService.java:61-85](../server/src/main/java/com/swp391/kpi/appeal/service/AppealManagementService.java)

```java
public Page<AppealManagementResponse> getQueue(Integer userId, String status, Integer articleId,
        Integer reviewerId, LocalDate fromDate, LocalDate toDate, int page, int size) {
    var head = users.requireActive(userId, "HEAD_OF_DEPARTMENT");
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE
            || (fromDate != null && toDate != null && fromDate.isAfter(toDate)))
        throw badRequest("Trang, kích thước hoặc khoảng ngày không hợp lệ.");
    AppealStatus filter = null;
    if (status != null && !status.isBlank()) {
        try { filter = AppealStatus.valueOf(status); }
        catch (IllegalArgumentException ex) { throw badRequest("Trạng thái khiếu nại không hợp lệ."); }
    }
    ...
    for (var appeal : appeals.findAll()) {
        var article = articles.getArticle(appeal.getArticleId());
        if (head.sectionId() == null || !head.sectionId().equals(article.sectionId())) continue;
        if (filter != null && filter != appeal.getStatus()) continue;
        if (articleId != null && !articleId.equals(article.id())) continue;
        LocalDate day = appeal.getSubmittedAt().toLocalDate();
        if ((fromDate != null && day.isBefore(fromDate)) || (toDate != null && day.isAfter(toDate))) continue;
        var current = currentAttempt(appeal);
        if (reviewerId != null && (current == null || !reviewerId.equals(current.reviewerId()))) continue;
        rows.add(response(appeal, article, current, false, names));
    }
```

*Làm gì:* kiểm role Trưởng ban, kiểm tham số, rồi lọc từng khiếu nại theo ban, trạng thái, bài, ngày và BTV.
*Vì sao:* Trưởng ban chỉ thấy khiếu nại của ban mình (dòng `head.sectionId()`).
*Lưu ý:* lọc bằng Java sau `findAll()`. Chạy được với số lượng hiện tại, nhưng chậm khi có nhiều khiếu nại.

**Bước 22: Mở chi tiết và lấy danh sách BTV** — [AppealDetailPage.jsx:40-48](../client/src/pages/appeal/AppealDetailPage.jsx) → [AppealManagementController.java:44-56](../server/src/main/java/com/swp391/kpi/appeal/controller/AppealManagementController.java)

```jsx
const coTheGiao = khieuNai && (khieuNai.status === 'SUBMITTED' || (khieuNai.status === 'IN_REVIEW' && phieu && !phieu.submittedAt))

useEffect(() => {
  let conHienThi = true
  Promise.all([layChiTietKhieuNai(id), layNguoiChamHopLe(id)])
    .then(([khieuNai, nguoiCham]) => { if (conHienThi) setKetQua({ khoa, duLieu: { khieuNai, nguoiCham }, loi: '' }) })
    .catch((error) => { if (conHienThi) setKetQua({ khoa, duLieu: null, loi: error.message }) })
  return () => { conHienThi = false }
}, [id, khoa])
```

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<AppealManagementResponse>> getDetail(@PathVariable Integer id,
        @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết khiếu nại thành công",
            service.getDetail(id, Integer.valueOf(jwt.getSubject()))));
}

@GetMapping("/{id}/reviewer-candidates")
public ResponseEntity<ApiResponse<List<AppealReviewerCandidateResponse>>> getCandidates(@PathVariable Integer id,
        @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(ApiResponse.success("Lấy người chấm hợp lệ thành công",
            service.getCandidates(id, Integer.valueOf(jwt.getSubject()))));
}
```

*Làm gì:* tải chi tiết khiếu nại và danh sách BTV có thể giao. `coTheGiao` quyết định có hiện form phân công hay không.

**Danh sách BTV có thể giao** — [AppealManagementService.java:189-214](../server/src/main/java/com/swp391/kpi/appeal/service/AppealManagementService.java)

```java
private List<AppealReviewerCandidateResponse> candidates(Appeal appeal) {
    var current = currentAttempt(appeal);
    if (appeal.getStatus() == AppealStatus.RESOLVED
            || (current != null && current.submittedAt() != null && !requiresSecond(appeal)))
        return List.of();
    Set<Integer> excluded = new HashSet<>();
    for (var attempt : scoring.getArticleAttempts(appeal.getArticleId())) excluded.add(attempt.reviewerId());
    for (var log : audit.getLogs(null, null, "appeals", appeal.getId(), null, null)) {
        if (!"ASSIGN".equals(log.getActionType()) && !"REASSIGN".equals(log.getActionType())) continue;
        addHistoricalReviewer(excluded, log.getOldValue()); addHistoricalReviewer(excluded, log.getNewValue());
    }
    Integer section = articles.getArticle(appeal.getArticleId()).sectionId();
    List<AppealReviewerCandidateResponse> same = new ArrayList<>();
    List<AppealReviewerCandidateResponse> other = new ArrayList<>();
    for (var editor : users.getUsers()) {
        if (!"ACTIVE".equals(editor.status()) || !"EDITOR".equals(editor.role())
                || editor.sectionId() == null || excluded.contains(editor.id())) continue;
        boolean cross = !section.equals(editor.sectionId());
        var candidate = new AppealReviewerCandidateResponse(editor.id(), editor.fullName(),
                editor.sectionId(), editor.sectionName(), cross);
        (cross ? other : same).add(candidate);
    }
    var result = same.isEmpty() ? other : same;
    result.sort(Comparator.comparing(AppealReviewerCandidateResponse::reviewerId));
    return result;
}
```

*Làm gì:* loại BTV đã chấm bài này, BTV đã từng được giao khiếu nại này, BTV không hoạt động. Ưu tiên BTV cùng ban; chỉ khi không có ai mới lấy BTV khác ban.
*Vì sao:* chấm mù nghĩa là người chấm lại không được là người đã chấm bài đó.

### 3.2 Trưởng ban phân công

**Bước 23: Kiểm dữ liệu phía client** — [AppealDetailPage.jsx:50-62](../client/src/pages/appeal/AppealDetailPage.jsx)

```jsx
async function luuPhanCong(e) {
  e.preventDefault()
  const form = e.currentTarget
  const nhap = new FormData(form)
  const reviewerId = Number(nhap.get('reviewerId'))
  const reason = String(nhap.get('reason') || '').trim()
  const ungVien = nguoiCham.find((item) => item.reviewerId === reviewerId)
  const giaoLai = khieuNai.status === 'IN_REVIEW'
  if (!ungVien || ((giaoLai || ungVien.crossDepartment) && !reason)) {
    setThongBao({ id, loi: 'Chọn người hợp lệ và nhập lý do khi giao lại hoặc khác ban.', thanhCong: '' })
    return
  }
  if (!window.confirm(giaoLai ? 'Xác nhận giao lại? Nháp cũ được lưu trong nhật ký và xóa khỏi phiếu.' : 'Xác nhận phân công chấm mù?')) return
```

*Làm gì:* chỉ cho gửi nếu BTV nằm trong danh sách và đã có lý do khi cần. Hỏi xác nhận trước khi gọi server.

**Bước 24: Gọi API phân công** — [AppealDetailPage.jsx:63-70](../client/src/pages/appeal/AppealDetailPage.jsx) → [appealApi.js:60-62](../client/src/api/appealApi.js)

```jsx
setDangLuu(true)
setThongBao({ id, loi: '', thanhCong: '' })
try {
  if (giaoLai) await giaoLaiKhieuNai(id, { attemptId: phieu.id, expectedReviewerId: phieu.reviewerId, reviewerId, reason })
  else await phanCongKhieuNai(id, reviewerId, reason)
  setThongBao({ id, loi: '', thanhCong: giaoLai ? 'Giao lại thành công.' : 'Phân công thành công.' })
```

```js
export function phanCongKhieuNai(id, reviewerId, reason) {
  return goiApi(`/api/appeals/${id}/assign`, { method: 'POST', body: { reviewerId, reason } })
}
```

**Bước 25: Controller phân công** — [AppealManagementController.java:58-63](../server/src/main/java/com/swp391/kpi/appeal/controller/AppealManagementController.java)

```java
@PostMapping("/{id}/assign")
public ResponseEntity<ApiResponse<AppealManagementResponse>> assign(@PathVariable Integer id,
        @Valid @RequestBody AppealAssignRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(ApiResponse.success("Phân công chấm mù thành công",
            service.assign(id, request, Integer.valueOf(jwt.getSubject()))));
}
```

**Bước 26: Service phân công** — [AppealManagementService.java:104-117](../server/src/main/java/com/swp391/kpi/appeal/service/AppealManagementService.java)

```java
@Transactional
public AppealManagementResponse assign(Integer id, AppealAssignRequest request, Integer userId) {
    var appeal = scopedAppeal(id, userId, true);
    if (appeal.getStatus() != AppealStatus.SUBMITTED)
        throw new BusinessException("Khiếu nại không còn chờ phân công.");
    var candidate = requireCandidate(appeal, request.getReviewerId(), request.getReason(), false);
    var attempt = scoring.createDraft(id, appeal.getArticleId(), appeal.getScoringAttemptId(),
            candidate.reviewerId(), candidate.crossDepartment());
    appeal.setStatus(AppealStatus.IN_REVIEW);
    audit.log(userId, "ASSIGN", "appeals", id, "{\"status\":\"SUBMITTED\"}",
            assignmentJson(attempt, request.getReason()));
    notifications.notifyUser(candidate.reviewerId(), appeal.getArticleId(),
            "Bạn được giao phiếu chấm mù #" + attempt.id() + ".");
    return response(appeal, articles.getArticle(appeal.getArticleId()), attempt, true, userNames());
}
```

*Làm gì:* khóa, kiểm trạng thái, kiểm BTV, tạo phiếu chấm, đổi trạng thái khiếu nại, ghi nhật ký, báo BTV.
*Vì sao:* thứ tự này để nếu bước nào lỗi thì cả giao dịch được hoàn tác (`@Transactional`).

**Bước 27: Kiểm quyền và khóa khi phân công** — [AppealManagementService.java:166-178](../server/src/main/java/com/swp391/kpi/appeal/service/AppealManagementService.java)

```java
private Appeal scopedAppeal(Integer id, Integer userId, boolean lock) {
    var head = users.requireActive(userId, "HEAD_OF_DEPARTMENT");
    if (lock) deviations.lockWorkflow();
    var appeal = (lock ? appeals.findLockedById(id) : appeals.findById(id))
            .orElseThrow(() -> new NotFoundException("Không tìm thấy khiếu nại."));
    if (lock) {
        em.refresh(appeal, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        scoring.lockReviewSnapshots(id, appeal.getArticleId());
    }
    var article = articles.getArticle(appeal.getArticleId());
    if (head.sectionId() == null || !head.sectionId().equals(article.sectionId()))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý khiếu nại này.");
    return appeal;
}
```

*Làm gì:* kiểm role, khóa toàn cục, khóa dòng khiếu nại, khóa các phiếu gốc, rồi kiểm đúng ban.
*Vì sao:* hai Trưởng ban không được phân công cùng lúc cho một khiếu nại.

**Bước 28: Kiểm BTV được chọn** — [AppealManagementService.java:216-224](../server/src/main/java/com/swp391/kpi/appeal/service/AppealManagementService.java)

```java
private AppealReviewerCandidateResponse requireCandidate(Appeal appeal, Integer id, String reason, boolean reassign) {
    for (var candidate : candidates(appeal)) {
        if (!candidate.reviewerId().equals(id)) continue;
        if ((candidate.crossDepartment() || reassign) && (reason == null || reason.isBlank()))
            throw new BusinessException("Phải nhập lý do giao lại hoặc phân công khác ban.");
        return candidate;
    }
    throw new BusinessException("Biên tập viên không đủ điều kiện hoặc đã tham gia chấm bài này.");
}
```

*Làm gì:* kiểm BTV có trong danh sách hợp lệ không. Nếu khác ban thì phải có lý do.

**Bước 29: Khóa toàn cục** — [ReviewerDeviationRefreshService.java:47-56](../server/src/main/java/com/swp391/kpi/appeal/service/ReviewerDeviationRefreshService.java)

```java
public void lockWorkflow() {
    Integer result = jdbc.queryForObject("""
            DECLARE @result INT;
            EXEC @result = sys.sp_getapplock @Resource=N'BF03_SCORE_WRITES',
                @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=15000;
            SELECT @result;
            """, Integer.class);
    if (result == null || result < 0)
        throw new com.swp391.kpi.exception.BusinessException("Đang có thao tác chất lượng chấm khác. Vui lòng thử lại.");
}
```

*Làm gì:* lấy khóa ứng dụng tên `BF03_SCORE_WRITES`, chờ tối đa 15 giây. Khóa tự nhả khi giao dịch kết thúc.
*Vì sao:* mọi thao tác ghi BF-03 xếp hàng, không chạy song song.

**Bước 30: Tạo phiếu chấm mù** — [ScoringAppealService.java:182-203](../server/src/main/java/com/swp391/kpi/appeal/service/ScoringAppealService.java)

```java
@Transactional
public ScoringAppealAttemptResponse createDraft(Integer appealId, Integer articleId,
        Integer originalId, Integer reviewerId, boolean crossDepartment) {
    var original = attempts.findLockedById(originalId)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chấm gốc."));
    if (!articleId.equals(original.getArticleId()) || original.getSubmittedAt() == null
            || original.getAttemptType() == AttemptType.APPEAL) {
        throw new BusinessException("Phiếu chấm gốc không hợp lệ.");
    }
    int next = 1;
    for (var attempt : attempts.findByArticleIdOrderByAttemptNoAsc(articleId)) {
        if (appealId.equals(attempt.getAppealId()))
            throw new BusinessException("Khiếu nại đã được phân công trước đó.");
        next = Math.max(next, attempt.getAttemptNo() + 1);
    }
    var draft = new ScoringAttempt();
    draft.setArticleId(articleId); draft.setVersionId(original.getVersionId());
    draft.setAssignment(original.getAssignment()); draft.setCriteriaSetVersion(original.getCriteriaSetVersion());
    draft.setCategoryId(original.getCategoryId()); draft.setAppealId(appealId);
    draft.setReviewerId(reviewerId); draft.setAttemptNo(next); draft.setAttemptType(AttemptType.APPEAL);
    draft.setIsCrossDepartment(crossDepartment);
    return ScoringAppealAttemptResponse.from(attempts.save(draft));
}
```

*Làm gì:* kiểm phiếu gốc hợp lệ, kiểm khiếu nại chưa được phân công, rồi tạo phiếu `APPEAL` mới với `submitted_at` để trống.
*Vì sao:* phiếu mới dùng cùng phiên bản bài và bộ tiêu chí với phiếu gốc, để điểm có thể so sánh được.

**Bước 31: Phản hồi và cập nhật giao diện** — [AppealDetailPage.jsx:68-70](../client/src/pages/appeal/AppealDetailPage.jsx)

```jsx
setThongBao({ id, loi: '', thanhCong: giaoLai ? 'Giao lại thành công.' : 'Phân công thành công.' })
form.reset()
setLanTai((cu) => cu + 1)
```

*Làm gì:* hiện thông báo, xóa form, tăng `lanTai` để tải lại dữ liệu.

---

## 4. Điểm cần biết

1. **Trang hàng chờ lọc bằng Java.** `getQueue()` lấy toàn bộ khiếu nại rồi lọc trong bộ nhớ.
2. **Khóa toàn cục làm chậm các thao tác khác.** Mọi thao tác ghi BF-03 xếp hàng, tối đa 15 giây.
3. **`max_appeals_per_article` chưa được kiểm.** Xem bước 17.
4. **Link trang chủ BTV có thể sai.** [EditorHome.jsx:68](../client/src/pages/home/EditorHome.jsx) dùng `appealId` cho `/re-reviews/:id`, nhưng trang chi tiết nhận `attemptId`. Chưa kiểm dữ liệu thực tế.
5. **Chưa đọc:** `lockReviewSnapshots()`, `getSubmissionContext()`, phần trả về DTO của `submitAppeal()`, và `validateCriteria()`.

---

## 5. Các file chính

| Lớp | File |
|---|---|
| Đăng nhập (FE) | `client/src/pages/Login.jsx`, `client/src/api/authApi.js`, `client/src/api/goiApi.js`, `client/src/auth/phienDangNhap.js`, `client/src/App.jsx`, `client/src/routes/AppRoutes.jsx` |
| Khiếu nại, PV (FE) | `client/src/pages/appeal/AppealCreatePage.jsx`, `client/src/api/appealApi.js` |
| Khiếu nại, TB (FE) | `client/src/pages/appeal/AppealManagementPage.jsx`, `client/src/pages/appeal/AppealDetailPage.jsx`, `client/src/layouts/HeadLayout.jsx`, `client/src/pages/home/HeadHome.jsx` |
| Đăng nhập (BE) | `AuthController.java`, `AuthService.java`, `TokenService.java`, `SecurityConfig.java` |
| Khiếu nại, PV (BE) | `AppealController.java`, `AppealService.java` |
| Khiếu nại, TB (BE) | `AppealManagementController.java`, `AppealManagementService.java`, `ScoringAppealService.java` |
| Khóa (BE) | `ReviewerDeviationRefreshService.java` |
