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
| `docs/SRS_Constraints.docx` | Ràng buộc kỹ thuật: stack, tiền, làm tròn, đăng nhập, phạm vi |
| `docs/SRS_3.2_Entity_Descriptions.docx` | Ý nghĩa từng entity/bảng |
| `docs/SDS_Architecture_DB_SourceCode.docx` | Kiến trúc, thiết kế DB, cấu trúc source code |
| `docs/CODING_PLAN.md` | Quy ước code chi tiết (bản nháp cũ — chỗ nào khác file này thì theo file này) |

**Tài liệu riêng của luồng — đọc thêm khi làm luồng đó:**

| Luồng | File |
|---|---|
| `royalty` (BF-04) | `docs/SRS_UC_FullDressed_BF04.docx` (UC, Normal/Alternative flow, BR-RY-xx) |
| `article`, `assessment`, `publishing`, `kpi`, `admin` | Chưa có. Khi nhóm thêm file UC/SRS của luồng vào `docs/` → thêm 1 dòng vào bảng này **và** vào `FLOW_DOCS` trong `.claude/hooks/require-docs-read.js` |

**Nên xem (không bắt buộc):** `docs/SWP391_PackageDiagram.png`, `docs/SWP391_SoftwareArchitecture.png`.

**Cách đọc file `.docx`** (máy nhóm không có Python/pandoc; Claude Code không mở được `.docx` bằng Read):

```bash
# Git Bash
unzip -p docs/SRS_Constraints.docx word/document.xml | sed -e 's/<\/w:tc>/ | /g; s/<\/w:p>/\n/g; s/<[^>]*>//g' | grep -v '^\s*$'
```

```powershell
# PowerShell
Add-Type -AssemblyName System.IO.Compression.FileSystem; $z = [IO.Compression.ZipFile]::OpenRead((Resolve-Path 'docs\SRS_Constraints.docx')); $r = New-Object IO.StreamReader($z.GetEntry('word/document.xml').Open()); $x = $r.ReadToEnd(); $r.Close(); $z.Dispose(); ($x -replace '</w:tc>', ' | ' -replace '</w:p>', "`n" -replace '<[^>]+>', '') -split "`n" | Where-Object { $_.Trim() }
```

**Khi tài liệu mâu thuẫn nhau**, ưu tiên theo thứ tự:
1. `database/schema.sql` — cấu trúc bảng, kiểu, tên cột, giá trị enum
2. `AGENTS.md` (file này) — quy ước code
3. `docs/SRS_*.docx`, `docs/SDS_*.docx` — nghiệp vụ, luồng, luật BR-xx
4. `docs/CODING_PLAN.md`

Mâu thuẫn về **nghiệp vụ** (vd SRS nói một kiểu, schema làm một kiểu) → **không tự chọn**, dừng lại báo người dùng.

**Chứng minh đã đọc:** trước khi sửa file code đầu tiên, AI ghi ngắn trong câu trả lời:
- `Đã đọc:` danh sách file ở trên
- Các bảng, luật nghiệp vụ (BR-xx), UC liên quan tới task sẽ áp dụng

**Cưỡng chế:** với Claude Code, hook `.claude/hooks/require-docs-read.js` (khai báo trong `.claude/settings.json`) **chặn** Edit/Write vào `server/src/` và `client/src/` nếu trong phiên chưa đọc đủ các file trên. Codex / Antigravity không có hook → bắt buộc tự tuân thủ mục này.

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

Mỗi luồng là một package, người phụ trách luồng nào chủ yếu sửa trong package đó.

Package gốc: `com.swp391.kpi`

| Package | Luồng | Bảng sở hữu |
|---|---|---|
| `common` | Dùng chung — **sửa phải báo cả nhóm** | `notifications` |
| `admin` | Nền tảng / Admin **[ĐỀ XUẤT: chia người phụ trách]** | `roles`, `sections`, `users`, `genres`, `media_factors` |
| `article` | BF-01 Viết & nộp bài | `articles`, `article_authors`, `article_versions`, `article_attachments`, `article_status_logs` |
| `assessment` | BF-02 Phân công & đánh giá chất lượng | `quality_criteria`, `quality_reviews`, `quality_review_items` |
| `publishing` | BF-03 Duyệt đăng **[ĐỀ XUẤT tên]** | không có bảng riêng, dùng bảng của `article` qua `ArticleService` |
| `royalty` | BF-04 Chấm & chi nhuận bút | `unit_prices`, `royalty_periods`, `royalty_gradings`, `royalty_media_items`, `royalty_lines`, `royalty_payments` |
| `kpi` | BF-05 KPI & báo cáo | `kpi_targets` |

Các thư mục tầng ở gốc (`com.swp391.kpi.controller`, `.entity`, `.service`...) là khung cũ: **không thêm code mới vào đó**.

**Dùng dữ liệu của luồng khác:**
- Cần dữ liệu luồng khác → gọi **Service** của luồng đó, không gọi Repository của họ, không tự viết query vào bảng của họ.
- Entity thuộc luồng nào thì chỉ người phụ trách luồng đó sửa. Cần thêm cột/hàm → báo người đó.
- Đổi trạng thái bài viết (`articles.status`) → luôn qua hàm của `ArticleService` để cùng lúc ghi `article_status_logs`.

## 3. Backend

### 3.1 Cấu trúc trong mỗi luồng

```
royalty/
├── controller/   RoyaltyPeriodController.java
├── service/      RoyaltyPeriodService.java
├── repository/   RoyaltyPeriodRepository.java
├── entity/       RoyaltyPeriod.java, RoyaltyPeriodStatus.java
└── dto/          RoyaltyPeriodResponse.java, RoyaltyPeriodCreateRequest.java
```

Code mẫu chuẩn để bắt chước: package `com.swp391.kpi.royalty` (kỳ nhuận bút).

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

Mọi API trả `com.swp391.kpi.common.response.ApiResponse`:

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

- Chỉ có **một** `GlobalExceptionHandler` (`@RestControllerAdvice`) trong `common/exception`, cùng chỗ với `NotFoundException`, `BusinessException`. Nếu chưa có thì báo người dùng tạo trong `common` — **không tự tạo bản riêng trong package luồng**.
- Message trả cho người dùng: **tiếng Việt có dấu** (frontend hiển thị thẳng).

### 3.7 URL

- Tiền tố `/api`, danh từ số nhiều, `kebab-case`: `/api/royalty-periods`.
- CRUD: `GET` / `POST` / `PUT` / `DELETE`. Hành động nghiệp vụ: `POST /api/royalty-periods/{id}/lock`.
- Phân trang: `?page=0&size=10` bằng `Pageable`, trang đầu là 0.
- **Không dùng `@CrossOrigin`**: client gọi qua Vite proxy nên không có CORS.

### 3.8 Đăng nhập & người thao tác [ĐỀ XUẤT]

- Đích: Spring Security + JWT, mật khẩu BCrypt (`users.password_hash`), chặn role ở tầng controller/security.
- **Trước khi có đăng nhập:** API cần biết ai thao tác thì tạm nhận `@RequestHeader("X-User-Id") Integer currentUserId` ở controller rồi truyền vào service, ghi `// TODO(auth): lấy từ token`. Không tự viết cơ chế đăng nhập riêng trong luồng.

### 3.9 Test

- Logic tính toán thuần (tiền, phân bổ, điểm — vd `royalty/PhanBoTien`) **bắt buộc** có unit test JUnit 5 trong `server/src/test`, không cần Spring/DB.
- API: chạy thật và gọi thử bằng Postman/trình duyệt trước khi tạo PR.

## 4. Frontend

```
client/src/
├── api/            axiosClient.js (baseURL '/api', xử lý lỗi chung), mỗi luồng 1 file: royaltyApi.js...
├── pages/          chia theo luồng: article/, assessment/, publishing/, royalty/, kpi/, admin/
├── components/common/   Button, Table, Modal... dùng chung
├── layouts/        layout theo role
├── routes/         khai báo route + chặn route theo role
└── utils/          formatMoney, formatDate
```

- Function component + hooks. Không Redux; thông tin đăng nhập để trong một Context.
- **Chỉ gọi API qua file trong `src/api/`**, URL bắt đầu bằng `/api/...`. Không bao giờ viết `http://localhost:8080` trong code React.
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

- Tên trong code mới bằng **tiếng Anh**. Comment bằng **tiếng Việt không dấu** trong file Java (giống code hiện có), chỉ comment ở chỗ có logic nghiệp vụ.
- Không có "số ma": ngưỡng, tỷ lệ, số ngày... đặt thành hằng số có tên hoặc lấy từ bảng cấu hình.
- File lưu UTF-8.

## 6. Database

- `database/schema.sql` là nguồn duy nhất. Không sửa bảng trong SSMS rồi quên cập nhật file.
- Muốn đổi schema: sửa `schema.sql` (+ `data.sql` nếu cần), PR ghi **"CÓ ĐỔI DB"** ở đầu mô tả, báo cả nhóm chạy lại. Chủ DB duyệt **[ĐỀ XUẤT: chọn 1 người]**.
- Chuỗi tiếng Việt trong `data.sql` phải có tiền tố `N'...'`.

## 7. Git

- Nhánh: `main` (ổn định, demo) ← `develop` (tích hợp, luôn chạy được) ← `feature/bf04-ten-viec`.
- Không push thẳng lên `main`/`develop`, mọi thay đổi qua Pull Request.
- Trước khi tạo PR: `git pull origin develop`, sửa conflict, chạy lại app.
- Commit: `[BF-04] Thêm API tạo kỳ nhuận bút`. Việc chung: `[COMMON]`, sửa DB: `[DB]`.
- **Không bao giờ commit** `application-local.properties`, mật khẩu, `target/`, `node_modules/`.

## 8. Quy tắc riêng cho AI

1. Chưa rõ người dùng phụ trách luồng nào → hỏi trước khi viết code.
2. Làm đủ **Bước 0** (đọc tài liệu) trước khi code. Viết entity/query → đọc lại đúng bảng đó trong `database/schema.sql` (cả comment phía trên bảng: nhiều luật nghiệp vụ nằm ở đó).
3. Chỉ sửa trong package luồng của người dùng. Cần sửa `common/`, luồng khác, `schema.sql`, `pom.xml`, `package.json` → **dừng lại**, nói rõ cần sửa gì để người dùng báo nhóm.
4. Không đổi `ddl-auto`, không thêm thư viện, không tạo pattern/abstraction ngoài mục 1.
5. Viết xong backend → chạy `mvn -f server/pom.xml compile` (có DB thì chạy `spring-boot:run` và gọi thử API). Frontend → `npm run build` trong `client/`.
6. Không commit/push khi người dùng chưa yêu cầu.
7. Cuối câu trả lời: giải thích ngắn bằng tiếng Việt đã làm gì và vì sao, để người dùng tự trình bày lại được.

## 9. Việc nhóm cần chốt

| # | Câu hỏi | Đề xuất hiện tại |
|---|---|---|
| 1 | Tách Service thành interface + Impl? | Không (trừ khi thầy yêu cầu) |
| 2 | Tên package BF-03 | `publishing` |
| 3 | Đăng nhập | Spring Security + JWT; tạm thời header `X-User-Id` |
| 4 | Thư viện frontend | `axios` + Ant Design |
| 5 | Ai làm package `admin` + đăng nhập? | Chưa có |
| 6 | Ai là chủ DB? | Chưa có |
