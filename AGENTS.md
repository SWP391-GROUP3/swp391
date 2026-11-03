# AGENTS.md — Quy tắc code chung cho SWP391

File này là **nguồn quy tắc duy nhất** cho mọi AI (Codex, Antigravity, Claude Code) và cho cả 5 thành viên.
Claude Code đọc qua `CLAUDE.md` (file đó chỉ import file này). Muốn đổi quy tắc → sửa file này qua PR để cả nhóm thấy.
**Hai file `AGENTS.md` và `CLAUDE.md` phải nằm ở thư mục gốc repo** — để chỗ khác (vd `docs/`) thì AI không tự đọc được.

Mục có nhãn **[ĐỀ XUẤT]** là chưa chốt: AI làm theo đề xuất, nhưng nhóm có thể đổi.

---

## Bước 0 — BẮT BUỘC: đọc tài liệu trước khi code

Áp dụng cho **mọi AI và mọi thành viên**. Mỗi phiên làm việc (mỗi lần mở AI mới / mỗi task mới), **trước khi tạo hay sửa bất kỳ file nào** trong `server/`, `client/`, `database/` thì phải đọc xong các tài liệu dưới đây. Đọc bằng tool thật (mở file), không đoán nội dung từ tên file hay từ trí nhớ phiên trước.

**Tài liệu chung — luôn đọc:**

| File | Đọc để biết |
|---|---|
| `database/schema.sql` | Bảng, cột, CHECK/UNIQUE, và **comment phía trên mỗi bảng** (luật nghiệp vụ). Ít nhất đọc hết các bảng mà task đụng tới + bảng có FK tới chúng |
| `docs/SRS_Overview.md` | Mục **Vấn đề đã biết** (chỗ SRS lệch schema), 5 luồng nghiệp vụ, entity, actor, luật chung GBR-xx, ràng buộc kỹ thuật (stack, tiền, làm tròn, đăng nhập) |
| `docs/CODING_PLAN.md` | Quy ước code chi tiết (bản nháp cũ — chỗ nào khác file này thì theo file này) |

**Tài liệu riêng của luồng — đọc thêm khi làm luồng đó:**

| Luồng | File |
|---|---|
| `article` (BF-01) | `docs/SRS_BF01_Article.md` (UC-01…13, BR-IN-xx) |
| `assessment` (BF-02, gồm duyệt đăng UC-33) | `docs/SRS_BF02_Scoring.md` (UC-14…34, BR-SC-xx), `docs/SRS_BF03_Appeal.md` (UC-35…45, BR-QC-xx) |
| `appeal` (BF-03) | `docs/SRS_BF03_Appeal.md` (UC-35…45, BR-QC-xx), `docs/SRS_BF02_Scoring.md` (lần chấm lại do khiếu nại ghi vào `scoring_attempts`) |
| `royalty` (BF-04) | `docs/SRS_BF04_Royalty.md` (UC-46…57, BR-RY-xx) |
| `kpi` (BF-05) | `docs/SRS_BF05_KPI.md` (UC-58…68, BR-KPI-xx) |
| `admin` | Chỉ cần tài liệu chung |
| Mọi việc **frontend** (`client/`) | Ảnh thiết kế trong `docs/ui-images/` (export từ file `.fig`, xem mục 4) |

Thêm/đổi file tài liệu của luồng → sửa bảng này **và** `FLOW_DOCS` trong `.claude/hooks/require-docs-read.js`.

**Chỉ đọc file `.md`.** Bộ SRS đã được chuyển từ `SRS.pdf` sang Markdown. Không đọc file `.docx` / `.pdf`. Sơ đồ (ERD, swimlane, use case) và mockup nằm trong `docs/srs-images/`, mở bằng Read khi cần xem hình. Tài liệu gốc thay đổi → cập nhật lại file `.md` tương ứng qua PR.

**Khi tài liệu mâu thuẫn nhau**, ưu tiên theo thứ tự:
1. `database/schema.sql` — cấu trúc bảng, kiểu, tên cột, giá trị enum
2. `AGENTS.md` (file này) — quy ước code
3. `docs/SRS_*.docx`, `docs/SDS_*.docx` — nghiệp vụ, luồng, luật BR-xx
4. `docs/CODING_PLAN.md`

Mâu thuẫn về **nghiệp vụ** (vd SRS nói một kiểu, schema làm một kiểu) → **không tự chọn**, dừng lại báo người dùng.

**Chứng minh đã đọc:** trước khi sửa file code đầu tiên, AI ghi ngắn trong câu trả lời:
- `Đã đọc:` danh sách file ở trên
- Các bảng, luật nghiệp vụ (BR-xx), UC liên quan tới task sẽ áp dụng

**Cưỡng chế:** với Claude Code, hook `.claude/hooks/require-docs-read.js` (khai báo trong `.claude/settings.json`) **chặn** Edit/Write vào `server/src/`, `client/src/` và `database/` nếu trong phiên chưa **mở thành công bằng tool Read** đủ các file trên (chỉ nhắc tên file trong Grep/Bash không được tính). Codex / Antigravity không có hook → bắt buộc tự tuân thủ mục này.

---

## 0. Dự án

Hệ thống đánh giá chất lượng bài báo & quản lý nhuận bút (môn SWP391).

| Phần | Công nghệ | Cổng |
|---|---|---|
| `server/` | Spring Boot 3.5, Java 17, Spring Data JPA, SQL Server, Lombok, Maven | 8080 |
| `client/` | React 19 + Vite, **JavaScript** (không TypeScript), react-router-dom | 5173 |
| `database/` | `schema.sql` (nguồn DUY NHẤT định nghĩa bảng), `data.sql` (dữ liệu mẫu) | — |
| `docs/` | Tài liệu SRS/SDS, coding plan, sơ đồ — xem **Bước 0** | — |

Client chỉ gọi REST API, không kết nối DB. `client/` không dùng JSP/Servlet nữa: không thêm code vào `client/src/main/java` hay `webapp`.

## 1. Nguyên tắc số 1: code ở mức sinh viên

Người nộp code phải **tự giải thích được từng dòng** khi thầy hỏi. Vì vậy:

**Được dùng:** Controller → Service → Repository; query method của Spring Data (`findByStatus`); `@Query` JPQL ngắn; `for` + `if/else` rõ ràng; stream 1–2 bước (`.filter().toList()`); hàm nhỏ có tên dễ hiểu.

**KHÔNG dùng** (trừ khi nhóm đồng ý và ghi vào file này):
- Design pattern tự dựng (Strategy, Factory, Builder tự viết...), base class generic (`BaseService<T, ID>`)
- MapStruct, ModelMapper → map tay (mục 3.4)
- WebFlux, `CompletableFuture`, event bus, cache, custom annotation, AOP tự viết
- Stream lồng nhau, `Optional` nối chuỗi dài, lambda nhiều dòng → thay bằng `for` + `if`
- Thư viện chưa có trong `pom.xml` / `package.json` → phải hỏi nhóm trước

## 2. Ai sở hữu gì

Backend **chia theo luồng** (mục 3.1, từ 03/11/2026): mỗi luồng 1 thư mục `com/swp391/kpi/<luồng>/`, bên trong mới chia tầng `controller/`, `service/`, `repository/`, `dto/`, `entity/`. Người phụ trách luồng nào chủ yếu sửa file trong thư mục luồng đó. Tên class vẫn giữ **tiền tố** của luồng (bảng dưới) để tìm nhanh bằng tên.

Package gốc: `com.swp391.kpi`

| Luồng (= thư mục) | Người phụ trách (từ 02/11/2026) | Tiền tố tên class | Bảng sở hữu |
|---|---|---|---|
| Dùng chung — **sửa phải báo cả nhóm** | Quang: `common/`, `exception/`, `config/`, `data.sql`, `index.css`, `goiApi.js`. Minh: `AppRoutes.jsx`, `components/common/`, `utils/` | không có tiền tố: `ApiResponse`, đăng nhập (`Auth`, `Token`, `Login`), `Audit`, `Notification` → `common/`; file trong `exception/`, `config/` | `notifications`, `audit_logs` |
| `admin` — Nền tảng / Admin | Minh | `Role`, `Section`, `User`, `Genre`, `MediaFactor`, `SystemParameter` | `roles`, `sections`, `users`, `genres`, `media_factors`, `system_parameters` |
| `article` — BF-01 Viết & nộp bài | Minh | `Article`, `Duplication` | `articles`, `article_authors`, `article_versions`, `article_attachments`, `article_status_logs`, `duplication_checks`, `duplication_matches` |
| `assessment` — BF-02 Phân công, đánh giá chất lượng & duyệt đăng (UC-33) | Minh (nhận lại từ Dương) | `Assignment`, `CategoryChange`, `Criteria`, `ScoringAttempt`, `ScoreApproval`, `Publishing` | `assignments`, `category_change_logs`, `criteria_set_versions`, `criteria`, `scoring_attempts`, `scoring_attempt_details`, `score_approvals` |
| `appeal` — BF-03 Khiếu nại & kiểm soát chất lượng chấm | Quang (nhận lại từ Vũ) | `Appeal`, `CrossReview`, `ReviewerDeviation` | `appeals`, `appeal_criteria`, `appeal_resolutions`, `appeal_rule_configs`, `cross_review_batches`, `cross_review_tasks`, `reviewer_deviation_indices` |
| `royalty` — BF-04 Chấm & chi nhuận bút | Quang | `Royalty`, `Event` | `royalty_rates`, `events`, `royalty_periods`, `royalty_gradings`, `royalty_media_items`, `royalty_payments`, `royalty_lines` |
| `kpi` — BF-05 KPI & báo cáo | Quang (nhận lại từ Khang) | `Kpi`, `CsvImport`, `ReaderInteraction` | `kpi_targets`, `kpi_snapshots`, `csv_import_logs`, `reader_interactions` |

Cột **Tiền tố** và **Bảng sở hữu** lấy theo danh sách bảng ở đầu `schema.sql`. Thêm bảng hoặc tiền tố mới → sửa bảng này **và** `FLOW_DOCS` trong `.claude/hooks/require-docs-read.js`.

Vài class cũ không theo tiền tố nhưng vẫn nằm đúng thư mục luồng: `Reviewer*`, `ScoringAppeal*` → `appeal/`; `ScoringRead*`, `ScoreCalculator`, `AttemptType`, `ApprovalDecision` → `assessment/`; `ReporterKpi*`, `DepartmentKpi*`, `GenreReport*`, `GenrePopularity*`, `TrendingEvent*` → `kpi/`. Class mới thì đặt tên theo tiền tố của luồng.

**Từ 02/11/2026 nhóm chỉ còn Quang và Minh** (Dương, Vũ, Khang đã giao lại code). Việc cụ thể của từng người trước demo 04/11 nằm ở `docs/KE_HOACH_THU4_QUANG.md` và `docs/KE_HOACH_THU4_MINH.md`. Mở phiên AI mới thì đọc file của người đang làm.

Class mới luôn đặt ở `com.swp391.kpi.<luồng>.<tầng>` (vd `com.swp391.kpi.royalty.service.RoyaltyPeriodService`). **Không** tạo thư mục tầng ở ngoài luồng (`com.swp391.kpi.service`...), **không** tạo thêm cấp con khác bên trong tầng, và **không** đặt class của luồng mình vào thư mục luồng khác.

**Dùng dữ liệu của luồng khác:**
- Cần dữ liệu luồng khác → gọi **Service** của luồng đó, không gọi Repository của họ, không tự viết query vào bảng của họ.
- Entity thuộc luồng nào thì chỉ người phụ trách luồng đó sửa. Cần thêm cột/hàm → báo người đó.
- Đổi trạng thái bài viết (`articles.status`) → luôn qua hàm của `ArticleService` để cùng lúc ghi `article_status_logs`.

## 3. Backend

### 3.1 Cấu trúc thư mục (chia theo luồng, trong luồng chia theo tầng)

```
server/src/main/java/com/swp391/kpi/
├── KpiApplication.java
├── config/       Cấu hình (Security, Jwt)                                 — dùng chung
├── exception/    GlobalExceptionHandler, NotFoundException, BusinessException — dùng chung
├── common/       ApiResponse, đăng nhập (Auth, Token), AuditLog, Notification — dùng chung
├── admin/        Role, Section, User, Genre, MediaFactor, SystemParameter
├── article/      BF-01
├── assessment/   BF-02
├── appeal/       BF-03
├── royalty/      BF-04
└── kpi/          BF-05

Mỗi thư mục luồng (vd royalty/) có đủ 5 tầng:
royalty/
├── controller/   RoyaltyPeriodController, RoyaltyPaymentController...
├── dto/          RoyaltyPeriodResponse, RoyaltyPeriodCreateRequest...
├── entity/       RoyaltyPeriod, RoyaltyPeriodStatus (enum)...
├── repository/   RoyaltyPeriodRepository...
└── service/      RoyaltyPeriodService, RoyaltyCalculator...
```

- Test để cùng package với class được test: `server/src/test/java/com/swp391/kpi/royalty/service/RoyaltyPeriodServiceTest.java`.
- Dùng class của luồng khác thì `import` package của luồng đó, vd `import com.swp391.kpi.article.service.ArticlePublishedService;`. Thấy `import com.swp391.kpi.<luồng khác>.repository...` trong code của mình là đang vi phạm mục 2 (phải gọi Service).
- Enum để chung thư mục `entity/` với entity dùng nó.
- **Không có thư mục `mapper/`**: đổi entity → DTO bằng hàm `from()` trong DTO (mục 3.4).
- Logic tính toán thuần (chia tiền, tính điểm...) để trong `service/`, là class thường không có `@Service` (mục 3.9).

### 3.2 Việc của từng tầng

| Tầng | Được làm | KHÔNG được làm |
|---|---|---|
| Controller | Nhận request, `@Valid` DTO, gọi 1 hàm service, trả `ResponseEntity<ApiResponse<...>>` | `if` nghiệp vụ, gọi repository, `try/catch` để trả lỗi |
| Service | Toàn bộ logic nghiệp vụ, kiểm tra trạng thái, `@Transactional` | Trả entity ra controller |
| Repository | `extends JpaRepository<Entity, Integer>`, query method, `@Query` | Chứa logic |
| Entity | Ánh xạ bảng | Trả thẳng ra API |
| DTO | Dữ liệu vào/ra API | — |

- Service là class thường có `@Service`, **không tách interface + Impl** **[ĐỀ XUẤT]**.
- Inject bằng `private final` + `@RequiredArgsConstructor`. Không dùng `@Autowired` trên field.
- Hàm chỉ đọc: `@Transactional(readOnly = true)`. Hàm ghi DB: `@Transactional`.

### 3.3 Entity & JPA

- `spring.jpa.hibernate.ddl-auto=validate` — **không bao giờ đổi**. Entity lệch DB thì app báo lỗi ngay lúc khởi động; khi đó sửa entity, không sửa DB.
- Cột `snake_case` ↔ field `camelCase` tự map, không cần `@Column(name=...)`.
- Khoá chính: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;`
- Lombok trên entity: `@Getter @Setter @NoArgsConstructor`. **Không `@Data`** (toString/hashCode duyệt quan hệ → lặp vô hạn, query thừa).
- Enum: luôn `@Enumerated(EnumType.STRING)`. Tên hằng enum trùng đúng giá trị trong CHECK của `schema.sql`.
- Quan hệ: chỉ dùng `@ManyToOne(fetch = FetchType.LAZY)`. `@OneToMany` chỉ khi thật sự cần.
- FK tới entity **chưa có** (của luồng khác chưa làm) → tạm để `private Integer xxxId;` kèm comment, đổi sang `@ManyToOne` khi entity kia có.
- `spring.jpa.open-in-view=false` → mọi dữ liệu lazy phải load xong trong service rồi mới đổi sang DTO.

**Kiểu dữ liệu — phải khớp đúng, sai là `validate` báo lỗi:**

| SQL Server | Java |
|---|---|
| `INT` | `Integer` (**không dùng `Long`**) |
| `SMALLINT` | `Short` |
| `TINYINT` | `Byte` |
| `BIT` | `Boolean` |
| `DECIMAL(18,0)` (tiền VNĐ) | `BigDecimal` — **cấm `double`/`float`** |
| `DECIMAL(5,2)`, `DECIMAL(4,2)` (hệ số, điểm) | `BigDecimal` |
| `DATE` | `LocalDate` |
| `DATETIME2` | `LocalDateTime` |
| `VARCHAR` trạng thái/mã | `enum` + `@Enumerated(EnumType.STRING)` |
| `NVARCHAR` | `String` |

DTO có thể dùng `Integer` thay `Short`/`Byte` cho dễ dùng, đổi kiểu trong hàm map.

### 3.4 DTO & mapping

- Tên: `XxxCreateRequest`, `XxxUpdateRequest`, `XxxResponse`.
- Map tay bằng static method trong DTO: `public static XxxResponse from(Xxx entity)`.
- Request DTO dùng `@Getter @Setter` + annotation validate (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`) với `message` tiếng Việt.

### 3.5 Kiểm tra dữ liệu

DB **có** CHECK/UNIQUE constraint (xem `schema.sql`), nhưng chúng chỉ là lưới an toàn cuối: lỗi từ DB ra là lỗi SQL khó hiểu. Vì vậy backend vẫn phải kiểm tra trước để trả message tiếng Việt:

- Lỗi định dạng đầu vào (rỗng, độ dài, khoảng giá trị) → annotation trên Request DTO.
- Quy tắc nghiệp vụ (trạng thái, quyền, trùng dữ liệu, ràng buộc giữa nhiều bảng mà `schema.sql` ghi "kiểm ở tầng service") → trong service, vi phạm thì `throw new BusinessException("...")`.
- Không tìm thấy → `throw new NotFoundException("...")`.

### 3.6 Response & lỗi

Mọi API trả `com.swp391.kpi.common.dto.ApiResponse`:

```json
{ "success": true,  "message": "Tạo kỳ nhuận bút thành công", "data": { } }
{ "success": false, "message": "Dữ liệu không hợp lệ", "errors": { "periodMonth": "Tháng phải từ 1 đến 12" } }
```

| Status | Khi nào |
|---|---|
| 200 | Đọc, sửa thành công |
| 201 | Tạo mới thành công |
| 400 | `@Valid` fail |
| 401 / 403 | Chưa đăng nhập / sai role |
| 404 | `NotFoundException` |
| 409 | `BusinessException` (sai trạng thái nghiệp vụ, trùng dữ liệu) |

- Chỉ có **một** `GlobalExceptionHandler` (`@RestControllerAdvice`) trong thư mục `exception/`, cùng chỗ với `NotFoundException`, `BusinessException`. Nếu chưa có thì báo người dùng tạo trong `exception/` (file dùng chung) — **không tự tạo bản riêng cho luồng của mình**.
- Message trả cho người dùng: **tiếng Việt có dấu** (frontend hiển thị thẳng).

### 3.7 URL

- Tiền tố `/api`, danh từ số nhiều, `kebab-case`: `/api/royalty-periods`.
- CRUD: `GET` / `POST` / `PUT` / `DELETE`. Hành động nghiệp vụ: `POST /api/royalty-periods/{id}/lock`.
- Phân trang: `?page=0&size=10` bằng `Pageable`, trang đầu là 0.
- **Không dùng `@CrossOrigin`**: client gọi qua Vite proxy nên không có CORS.

### 3.8 Đăng nhập & người thao tác

Đã có: Spring Security + JWT, mật khẩu BCrypt (`users.password_hash`). File dùng chung: `config/SecurityConfig.java`, `config/JwtConfig.java`, `common/service/AuthService.java`, `common/service/TokenService.java`.

- Đăng nhập: `POST /api/auth/login` body `{ "login": "<email hoặc username>", "password": "..." }` → trả `token` + thông tin user. Tài khoản mẫu trong `data.sql`, mật khẩu chung `123456`.
- **Mọi API khác bắt buộc gửi** header `Authorization: Bearer <token>`. Thiếu / sai / hết hạn → 401.
- **Biết ai đang thao tác:** thêm tham số `@AuthenticationPrincipal Jwt jwt` vào hàm controller, rồi `Integer currentUserId = Integer.valueOf(jwt.getSubject());` và truyền vào service. **Không dùng header `X-User-Id` nữa.**
- **Chặn theo vai trò** (403): thêm luật trong `SecurityConfig`, vd `.requestMatchers(HttpMethod.POST, "/api/royalty-rates").hasRole("ACCOUNTANT")` (tên vai trò = `roles.code`). Sửa `SecurityConfig` phải báo nhóm.
- Mỗi máy phải có `app.jwt.secret` (≥ 32 ký tự) trong `application-local.properties`, thiếu thì server không khởi động (xem `application-local.properties.example`).
- Không tự viết cơ chế đăng nhập riêng trong luồng.

### 3.9 Test

- Logic tính toán thuần (tiền, phân bổ, điểm — vd hàm chia tiền nhuận bút cho đồng tác giả) **bắt buộc** có unit test JUnit 5 trong `server/src/test`, không cần Spring/DB.
- API: chạy thật và gọi thử bằng Postman/trình duyệt trước khi tạo PR.

## 4. Frontend

```
client/src/
├── main.jsx             khởi động React — không sửa
├── App.jsx              giữ trạng thái đăng nhập, gọi <AppRoutes />                     — dùng chung
├── index.css            CHỈ biến màu Figma (--fm-*), reset, class dùng chung (.an-di...) — dùng chung
├── api/                 goiApi.js (hàm gọi API chung, tự gắn token) + mỗi luồng 1 file: articleApi.js, royaltyApi.js... — CHỈ chỗ này gọi server
├── auth/                phienDangNhap.js: lưu / đọc / xoá { token, nguoiDung } trong sessionStorage
├── routes/              AppRoutes.jsx (khai báo route), RequireRole.jsx (chặn route theo role) — dùng chung
├── layouts/             AppLayout.jsx (khung chung: sidebar + thanh trên) + 1 file / role chỉ khai báo menu: ReporterLayout, EditorLayout, SecretaryLayout, HeadLayout — dùng chung
├── components/common/   thứ dùng ở ≥ 2 màn: Icon, HomeFrame/PendingList/StatusCounter/SummaryCard (khối trang chủ)... — dùng chung
├── pages/
│   ├── Login.jsx, ComingSoon.jsx   màn chung
│   ├── home/            trang chủ theo role: ReporterHome, HeadHome...
│   └── article/ assessment/ appeal/ royalty/ kpi/ admin/   — 1 thư mục / luồng
└── utils/               formatMoney, formatNumber, formatDate                           — dùng chung
```

**Đặt file ở đâu:**
1. **CSS để cạnh component:** `Xxx.jsx` đi kèm `Xxx.css`, `import './Xxx.css'` ở đầu file jsx. Không thêm style riêng của màn vào `index.css`.
   - File CSS thường áp dụng cho **cả trang**, không chỉ riêng component import nó → tên class phải có **tiền tố riêng của màn** (vd `.trang-chu-pv__...`, `.ds-bai__...`) để không đè class của màn khác.
   - Màu, font luôn dùng biến `--fm-*` trong `index.css`, không tự viết mã màu mới. Thiếu màu → thêm biến vào `index.css` (báo nhóm).
2. **Component chỉ 1 màn dùng** → để cùng thư mục với màn đó (vd `pages/home/TodoCard.jsx`). Màn thứ 2 cần dùng → chuyển lên `components/common/`.
3. **Route mới** khai báo trong `routes/AppRoutes.jsx`: trang chưa làm nằm trong mảng `TRANG_..._CHUA_LAM` của vai trò (hiện ComingSoon), làm xong thì xoá dòng đó và thêm `<Route>` trỏ tới trang thật. Menu của vai trò nằm trong `layouts/<VaiTro>Layout.jsx`.
4. **Ai sửa gì:** `pages/<luồng>/` và `api/<luồng>Api.js` thuộc người phụ trách luồng. File ghi "dùng chung" ở cây trên: sửa phải báo nhóm (giống mục 2).

**Giao diện phải làm theo thiết kế Figma có sẵn:** `docs/Web Dashboard UI - Task & Project Management (Community).fig`
- Bố cục, màu, font, khoảng cách, kiểu bảng/form/nút/menu lấy theo file này. Không tự nghĩ ra phong cách giao diện khác.
- File `.fig` là định dạng nhị phân của Figma, **AI không đọc được**. Muốn AI làm theo: mở file trong Figma, export màn hình cần làm ra PNG vào `docs/ui-images/` (tên theo màn hình, vd `dashboard.png`), AI mở ảnh bằng Read trước khi code.
- Chưa có ảnh của màn hình đang làm → AI **dừng lại hỏi**, không tự đoán giao diện.
- Màn hình thiết kế không có (vd form nhập nhuận bút) → ghép từ component, màu, kiểu có sẵn trong thiết kế.

- Function component + hooks. Không Redux; thông tin đăng nhập để trong một Context.
- **Chỉ gọi API qua file trong `src/api/`**, URL bắt đầu bằng `/api/...`. Không bao giờ viết `http://localhost:8080` trong code React.
- **Trong file `api/` luôn gọi `goiApi(...)`, không gọi `fetch` trực tiếp**: `goiApi` tự gắn token, tự đổi lỗi server thành `Error` tiếng Việt, token hết hạn thì tự về trang đăng nhập. Vd `goiApi('/api/royalty-rates', { method: 'POST', body: duLieu })` → trả phần `data`.
- Người đang đăng nhập: prop `nguoiDung` = `{ id, username, email, hoTen, vaiTro: ['REPORTER'], tenVaiTro, sectionId }`.
- Tiền: dùng chung `formatMoney()` (`Intl.NumberFormat('vi-VN')`).
- Tên file: component `PascalCase.jsx`, file khác `camelCase.js`.
- Thư viện **[ĐỀ XUẤT, chưa cài]**: `axios` để gọi API, Ant Design (`antd`) cho bảng/form. Chưa được nhóm duyệt thì dùng `fetch` và HTML/CSS thường.

## 5. Đặt tên & comment

| Loại | Quy ước | Ví dụ |
|---|---|---|
| Class | PascalCase + hậu tố tầng | `RoyaltyPeriodService` |
| Method, biến | camelCase, method là động từ | `lockPeriod()` |
| Hằng số | UPPER_SNAKE | `MAX_AUTHORS = 5` |
| Bảng, cột | snake_case (đã có trong schema) | `royalty_periods.period_month` |
| URL | kebab-case, số nhiều | `/api/royalty-periods` |

- **Backend (Java):** tên trong code bằng **tiếng Anh**.
- **Frontend (React):** tên component và tên file component bằng **tiếng Anh** PascalCase (`ReporterHome.jsx`); tên hàm, biến, class CSS bằng **tiếng Việt không dấu** (`layDinhMucChuan()`, `tenVaiTro`, `.trang-chu-pv`), giống code hiện có. Field nhận từ API giữ **đúng tên tiếng Anh của DTO** (`balance`, `confirmDeadline`), không đổi sang tiếng Việt.
- Comment bằng **tiếng Việt không dấu** trong file Java và JS/JSX (giống code hiện có), chỉ comment ở chỗ có logic nghiệp vụ.
- Không có "số ma": ngưỡng, tỷ lệ, số ngày... đặt thành hằng số có tên hoặc lấy từ bảng cấu hình.
- File lưu UTF-8.

## 6. Database

- `database/schema.sql` là nguồn duy nhất. Không sửa bảng trong SSMS rồi quên cập nhật file.
- Muốn đổi schema: sửa `schema.sql` (+ `data.sql` nếu cần), PR ghi **"CÓ ĐỔI DB"** ở đầu mô tả, báo cả nhóm chạy lại. Chủ DB duyệt **[ĐỀ XUẤT: chọn 1 người]**.
- Chuỗi tiếng Việt trong `data.sql` phải có tiền tố `N'...'`.

## 7. Git

- Nhánh: `main` (nhánh chung, luôn chạy được, dùng để demo) ← `feature/bf04-ten-viec`. **Không dùng nhánh `develop`.**
- Mỗi việc tách một nhánh `feature/<luồng>-<việc>` từ `main`, xong mở Pull Request vào `main`, một người khác duyệt rồi mới merge.
- Không push thẳng lên `main`, mọi thay đổi qua Pull Request.
- Trước khi tạo PR: `git pull origin main`, sửa conflict, chạy lại app.
- Commit: `[BF-04] Thêm API tạo kỳ nhuận bút`. Việc chung: `[COMMON]`, sửa DB: `[DB]`.
- **Không bao giờ commit** `application-local.properties`, mật khẩu, `target/`, `node_modules/`.

## 8. Quy tắc riêng cho AI

### 8.1 Hỏi rõ trước khi code (mỗi phiên code)

Đầu **mỗi phiên code** (mỗi lần mở AI mới / mỗi task mới), trước khi tạo hay sửa file code, AI phải hỏi và nhận câu trả lời rõ ràng từ người dùng về:
- Người dùng phụ trách **luồng** nào (xem thư mục luồng ở mục 2).
- Cần làm **chính xác việc gì**: chức năng nào, UC nào, API/màn hình nào.
- **Phạm vi**: chỉ backend, chỉ frontend hay cả hai; những gì **không** làm trong phiên này.
- Kết quả mong muốn: làm xong thế nào thì coi là đạt.

Yêu cầu còn mơ hồ hoặc có nhiều cách hiểu → hỏi tiếp, **không tự đoán rồi code**. Hỏi xong thì nhắc lại ngắn gọn yêu cầu đã hiểu để người dùng xác nhận.

### 8.2 Code từng bước nhỏ, không làm một mạch

- **Không** code một mạch cả chức năng từ đầu đến cuối (entity + repository + service + controller + DTO + frontend trong một lượt).
- Trước khi code, liệt kê **kế hoạch các bước nhỏ** (mỗi bước khoảng 1 hàm hoặc 1 file nhỏ) để người dùng xem.
- Mỗi lượt chỉ làm **1 bước**: viết 1 hàm (hoặc 1 class nhỏ), compile/chạy thử, giải thích ngắn hàm đó làm gì, rồi **dừng lại chờ người dùng đồng ý** mới sang bước tiếp.
- Đi theo thứ tự tầng: entity → repository → service (từng hàm) → controller → frontend. Không nhảy sang tầng sau khi tầng trước chưa chạy được.
- Không tự làm thêm việc ngoài bước đang làm (không "tiện tay" sửa chỗ khác, không viết trước hàm cho bước sau).

### 8.3 Các quy tắc khác

1. Chưa rõ người dùng phụ trách luồng nào → hỏi trước khi viết code (xem 8.1).
2. Làm đủ **Bước 0** (đọc tài liệu) trước khi code. Viết entity/query → đọc lại đúng bảng đó trong `database/schema.sql` (cả comment phía trên bảng: nhiều luật nghiệp vụ nằm ở đó).
3. Chỉ sửa file trong thư mục luồng của người dùng (mục 2). Cần sửa file dùng chung (`common/`, `exception/`, `config/`), file của luồng khác, `schema.sql`, `pom.xml`, `package.json` → **dừng lại**, nói rõ cần sửa gì để người dùng báo nhóm.
4. Không đổi `ddl-auto`, không thêm thư viện, không tạo pattern/abstraction ngoài mục 1.
5. Viết xong backend → chạy `mvn -f server/pom.xml compile` (có DB thì chạy `spring-boot:run` và gọi thử API). Frontend → `npm run build` trong `client/`.
6. Không commit/push khi người dùng chưa yêu cầu.
7. Cuối câu trả lời: giải thích ngắn bằng tiếng Việt đã làm gì và vì sao, để người dùng tự trình bày lại được.

## 9. Việc nhóm cần chốt

| # | Câu hỏi | Đề xuất hiện tại |
|---|---|---|
| 1 | Tách Service thành interface + Impl? | Không (trừ khi thầy yêu cầu) |
| 2 | Tên luồng BF-03 | **Đã chốt:** `appeal` (khiếu nại, chấm mù theo SRS). Duyệt đăng UC-33 thuộc `assessment` |
| 3 | Đăng nhập | **Đã làm:** Spring Security + JWT (mục 3.8). Bỏ header `X-User-Id` |
| 4 | Thư viện frontend | `axios` + Ant Design |
| 5 | Ai làm luồng `admin` + đăng nhập? | **Đã chốt (02/11):** `admin` → Minh; đăng nhập đã làm xong, file dùng chung trong `config/` do Quang giữ |
| 6 | Ai là chủ DB? | Chưa có |
