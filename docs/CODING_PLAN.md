# CODING PLAN — Hệ thống Đánh giá Chất lượng & Quản lý Nhuận bút (SWP391)

> Đặt file này tại `swp391/docs/CODING_PLAN.md`. Mọi thay đổi quy ước phải sửa qua PR để cả nhóm cùng thấy.
>
> Trạng thái: **BẢN NHÁP — đang chốt Phần 1**

## Mục lục

- [Phần 0 — Nguyên tắc khi dùng AI sinh code](#phần-0--nguyên-tắc-khi-dùng-ai-sinh-code)
- [Phần 1 — Quy ước code chung](#phần-1--quy-ước-code-chung)
- Phần 2 — Thứ tự & phụ thuộc giữa 5 luồng *(chưa viết)*
- Phần 3 — Definition of Done & quy trình review *(chưa viết)*
- [Câu hỏi mở cần nhóm chốt](#câu-hỏi-mở-cần-nhóm-chốt)

---

## Phần 0 — Nguyên tắc khi dùng AI sinh code

**Mục tiêu:** code ở mức môn học, **dễ đọc hơn là tối ưu**. Ai nộp code thì phải tự giải thích được từng dòng, vì khi review iteration thầy có thể hỏi bất kỳ ai.

### 0.1 Được dùng thoải mái

- Controller → Service → Repository, mỗi tầng một việc (xem 1.3)
- Method của Spring Data JPA theo tên hàm (`findByStatus`, `findByAuthorId`...), `@Query` JPQL ngắn
- Vòng `for` thông thường, `if/else` rõ ràng, tách hàm nhỏ có tên dễ hiểu
- Stream đơn giản 1–2 bước (`.filter().toList()`)

### 0.2 Không dùng (trừ khi cả nhóm đồng ý và ghi vào file này)

| Không dùng | Lý do |
|---|---|
| Design pattern tự dựng (Strategy, Factory, Builder tự viết, Template Method...) | Tăng số file/tầng mà với bài toán này không cần |
| Base class generic nhiều tầng (`BaseService<T, ID>`, `BaseController<...>`) | Người khác đọc không hiểu code chạy ở đâu |
| MapStruct, ModelMapper | Mapping thủ công dễ đọc và dễ debug hơn (xem 1.4) |
| WebFlux / reactive, CompletableFuture, event bus, cache (Redis...) | Quá mức cần thiết cho môn học |
| Custom annotation, AOP tự viết | Khó giải thích, khó debug |
| Stream lồng nhau, `Optional` nối chuỗi dài, lambda nhiều dòng | Thay bằng vòng `for` + `if` |
| Thư viện mới chưa có trong `pom.xml` / `package.json` | Phải hỏi nhóm trước khi thêm |

### 0.3 Đoạn mở đầu dán kèm mỗi lần nhờ AI viết code

```
Tôi đang làm project môn học SWP391: Spring Boot 3.5 + JPA + SQL Server (backend),
React 19 + Vite (frontend). Yêu cầu:
- Code ở mức sinh viên, dễ đọc, KHÔNG cần tối ưu, KHÔNG dùng design pattern phức tạp,
  KHÔNG dùng MapStruct/ModelMapper, KHÔNG dùng generic base class.
- Theo cấu trúc chia theo tầng: controller / service / repository / entity / dto dùng chung, tên class có tiền tố của luồng (vd RoyaltyPeriodService).
- Controller chỉ gọi service, trả về ApiResponse. Logic nghiệp vụ nằm trong service.
- Tiền dùng BigDecimal. Enum lưu dạng STRING.
- Comment ngắn bằng tiếng Việt ở những chỗ có logic nghiệp vụ.
Đây là code hiện có liên quan: <dán entity / service liên quan>
Việc cần làm: <mô tả>
```

### 0.4 Tự kiểm tra trước khi commit code do AI sinh

- [ ] Đọc hiểu và giải thích được từng dòng; dòng nào không hiểu thì hỏi lại hoặc viết lại cho đơn giản
- [ ] Không có file, class, thư viện "lạ" ngoài những gì đã quy định
- [ ] Chạy thật và gọi thử API bằng Postman, không chỉ dừng ở compile được
- [ ] Không sửa file của luồng khác nếu chưa báo người phụ trách

---

## Phần 1 — Quy ước code chung

### 1.1 Cấu trúc repo

```
swp391/
├── database/
│   ├── schema.sql        # Nguồn DUY NHẤT định nghĩa bảng
│   └── data.sql          # Dữ liệu mẫu
├── server/               # Spring Boot, cổng 8080
├── client/               # React + Vite, cổng 5173
└── docs/
    └── CODING_PLAN.md
```

### 1.2 Backend — cấu trúc package

> **ĐÃ ĐỔI:** nhóm chuyển sang **chia theo tầng** (`controller/`, `service/`, `entity/`... dùng chung, phân biệt luồng bằng tiền tố tên class). Mục này và ví dụ `royalty/...` bên dưới **không còn áp dụng**, theo `AGENTS.md` mục 2 và 3.1.

Chia **theo luồng** (mỗi người sở hữu một package), bên trong mỗi luồng mới chia tầng. Làm vậy để mỗi người chủ yếu sửa trong thư mục của mình, ít bị conflict khi merge.

```
com.<tennhom>.swp391
├── common/                 # Dùng chung — sửa phải báo cả nhóm
│   ├── response/           # ApiResponse
│   ├── exception/          # NotFoundException, BusinessException, GlobalExceptionHandler
│   └── config/             # CORS, Security...
├── admin/                  # Cấu hình: user, role, thể loại, tiêu chí, đơn giá
├── article/                # BF-01 Viết & nộp bài
├── assessment/             # BF-02 Phân công & đánh giá chất lượng
├── complaint/              # BF-03 (tên package chốt theo tên luồng — xem câu hỏi mở)
├── royalty/                # BF-04 Chấm & chi nhuận bút
└── kpi/                    # BF-05 KPI & Báo cáo
```

Bên trong mỗi luồng:

```
royalty/
├── controller/   RoyaltyController.java
├── service/      RoyaltyService.java
├── repository/   ArticleRoyaltyRepository.java
├── entity/       ArticleRoyalty.java, RoyaltyPeriod.java
└── dto/          RoyaltySuggestResponse.java, RoyaltyConfirmRequest.java
```

**Quy tắc dùng chung dữ liệu giữa các luồng:**

- Entity thuộc luồng nào thì chỉ người phụ trách luồng đó được sửa. Ví dụ `Article` thuộc BF-01; BF-04 cần thêm cột thì báo người làm BF-01.
- Cần dữ liệu của luồng khác → **gọi service của luồng đó**, không tự viết query vào bảng của người khác.

### 1.3 Backend — việc của từng tầng

| Tầng | Được làm | Không được làm |
|---|---|---|
| Controller | Nhận request, `@Valid` DTO, gọi service, trả `ApiResponse` | Chứa `if` nghiệp vụ, gọi repository |
| Service | Toàn bộ logic nghiệp vụ, kiểm tra trạng thái, `@Transactional` cho hàm ghi DB | Trả entity ra ngoài controller |
| Repository | `extends JpaRepository`, method theo tên, `@Query` | Chứa logic |
| Entity | Ánh xạ bảng | Trả thẳng ra API |
| DTO | Dữ liệu vào/ra API | — |

**Ví dụ mẫu — mức độ mong muốn:**

```java
// dto/RoyaltyConfirmRequest.java
@Getter @Setter
public class RoyaltyConfirmRequest {
    @NotNull(message = "Hệ số áp dụng không được để trống")
    private BigDecimal appliedFactor;
    private String reason;
}

// controller/RoyaltyController.java
@RestController
@RequestMapping("/api/articles/{articleId}/royalty")
@RequiredArgsConstructor
public class RoyaltyController {
    private final RoyaltyService royaltyService;

    @PostMapping
    public ResponseEntity<ApiResponse<RoyaltyResponse>> confirm(
            @PathVariable Long articleId,
            @Valid @RequestBody RoyaltyConfirmRequest request) {
        RoyaltyResponse result = royaltyService.confirm(articleId, request);
        return ResponseEntity.status(201).body(ApiResponse.success("Chấm nhuận bút thành công", result));
    }
}

// service/RoyaltyService.java
@Service
@RequiredArgsConstructor
public class RoyaltyService {
    private final ArticleRoyaltyRepository royaltyRepository;
    private final ArticleService articleService;   // gọi service của BF-01, không gọi repository của họ

    @Transactional
    public RoyaltyResponse confirm(Long articleId, RoyaltyConfirmRequest request) {
        Article article = articleService.getById(articleId);   // không có → NotFoundException

        // Chỉ bài đã được Trưởng ban duyệt điểm mới được chấm nhuận bút
        if (!"SCORE_APPROVED".equals(article.getStatus())) {
            throw new BusinessException("Bài chưa được duyệt điểm, chưa thể chấm nhuận bút");
        }
        // ... tính toán, lưu snapshot
        return RoyaltyResponse.from(saved);
    }
}
```

> Tên trạng thái `SCORE_APPROVED` chỉ là ví dụ minh họa — dùng đúng tên trong `schema.sql`.

### 1.4 Backend — quy ước kỹ thuật

**Entity & JPA**

- `spring.jpa.hibernate.ddl-auto=validate`: bảng do `schema.sql` tạo, **không để Hibernate tự tạo/sửa bảng**. Với `validate`, entity lệch với DB thì app báo lỗi ngay lúc khởi động → phát hiện sớm.
- Tên cột trong DB là `snake_case`, tên field Java là `camelCase`. Spring Boot tự chuyển đổi (`authorId` ↔ `author_id`) nên không cần `@Column(name=...)` nếu đặt tên đúng.
- Enum: luôn dùng `@Enumerated(EnumType.STRING)`. Mặc định là `ORDINAL` (lưu số 0, 1, 2...) → ai chèn thêm một giá trị vào giữa enum là dữ liệu cũ sai hết.
- Quan hệ: ưu tiên chỉ dùng `@ManyToOne(fetch = FetchType.LAZY)`. Chỉ thêm `@OneToMany` khi thật sự cần.
- Lombok trên entity: dùng `@Getter @Setter @NoArgsConstructor`, **không dùng `@Data`**. `@Data` sinh `toString/hashCode` duyệt cả quan hệ → dễ gây lặp vô hạn hoặc query thừa.

**DTO & mapping**

- Mapping thủ công bằng static method trong DTO:

```java
public static RoyaltyResponse from(ArticleRoyalty r) {
    RoyaltyResponse dto = new RoyaltyResponse();
    dto.setArticleId(r.getArticle().getId());
    dto.setAppliedFactor(r.getAppliedFactor());
    dto.setAmount(r.getAmount());
    return dto;
}
```

- Đặt tên: `XxxCreateRequest`, `XxxUpdateRequest`, `XxxResponse`.

**Kiểu dữ liệu**

| Dữ liệu | Java | SQL Server |
|---|---|---|
| Tiền (VNĐ) | `BigDecimal` — **cấm `double`/`float`** | `DECIMAL(18,0)` |
| Hệ số, điểm | `BigDecimal` | `DECIMAL(5,2)` |
| Ngày | `LocalDate` | `DATE` |
| Ngày giờ | `LocalDateTime` | `DATETIME2` |
| Chuỗi tiếng Việt | `String` | `NVARCHAR` — insert trong `data.sql` phải có tiền tố `N'...'` |

> Kiểm tra lại `schema.sql` hiện tại có khớp bảng này không. Chỗ nào đang dùng `FLOAT` cho tiền hoặc `VARCHAR` cho tiếng Việt thì sửa ngay từ đầu.

**Validate:** DB không có CHECK constraint nên **toàn bộ kiểm tra nằm ở backend**:

- Dữ liệu đầu vào (rỗng, độ dài, khoảng giá trị) → annotation trên DTO (`@NotBlank`, `@Size`, `@Min`...)
- Quy tắc nghiệp vụ (trạng thái, quyền, giới hạn 5 tác giả...) → trong service, vi phạm thì `throw new BusinessException("...")`

**Cấu hình & mật khẩu**

- `application.properties` commit lên repo, mật khẩu DB đọc từ biến môi trường:
  `spring.datasource.password=${DB_PASSWORD}`
- Hoặc mỗi người tạo `application-local.properties` (đã thêm vào `.gitignore`).
- **Không commit mật khẩu SSMS của bất kỳ ai.**

### 1.5 API — quy ước chung

**URL**

- Tiền tố `/api`, danh từ số nhiều, `kebab-case`: `/api/articles`, `/api/royalty-periods`
- CRUD: `GET` / `POST` / `PUT` / `DELETE`
- Hành động nghiệp vụ: `POST /api/articles/{id}/submit`, `POST /api/royalty-periods/{id}/lock`

**Định dạng response (mọi API đều theo mẫu này)**

```json
// Thành công
{ "success": true, "message": "Nộp bài thành công", "data": { ... } }

// Lỗi
{ "success": false, "message": "Dữ liệu không hợp lệ",
  "errors": { "title": "Tiêu đề không được để trống" } }
```

**HTTP status**

| Status | Khi nào |
|---|---|
| 200 | Thành công (đọc, sửa) |
| 201 | Tạo mới thành công |
| 400 | Dữ liệu đầu vào sai (`@Valid` fail) |
| 401 | Chưa đăng nhập |
| 403 | Sai role (vd Phóng viên gọi API của Kế toán) |
| 404 | Không tìm thấy (`NotFoundException`) |
| 409 | Sai trạng thái nghiệp vụ (`BusinessException`) — vd nộp lại bài đã nộp, chi trả kỳ chưa khóa |

- Chỉ có **một** `GlobalExceptionHandler` (`@RestControllerAdvice`) trong `common/exception`, không controller nào tự `try/catch` để trả lỗi.
- Message lỗi viết bằng **tiếng Việt**, vì frontend hiển thị thẳng cho người dùng.
- Phân trang: `?page=0&size=10` (Spring `Pageable`, trang đầu tiên là **0**).

### 1.6 Frontend — cấu trúc & quy ước

```
client/src/
├── api/
│   ├── axiosClient.js       # baseURL, gắn token, xử lý lỗi chung
│   ├── articleApi.js        # mỗi luồng một file
│   └── royaltyApi.js
├── pages/                   # chia theo luồng, giống backend
│   ├── article/
│   ├── assessment/
│   ├── royalty/
│   └── kpi/
├── components/common/       # Button, Table, Modal... dùng chung
├── layouts/                 # Layout theo role (sidebar khác nhau)
├── routes/                  # Khai báo route + chặn route theo role
└── utils/                   # formatMoney, formatDate
```

- Component viết dạng function + hooks (`useState`, `useEffect`). Không dùng Redux; thông tin đăng nhập để trong một Context.
- **Chỉ gọi API qua các file trong `src/api/`**, không gọi `axios` trực tiếp trong component.
- Dùng Vite proxy chuyển `/api` sang `http://localhost:8080` → frontend gọi `/api/...`, không phải cấu hình CORS lúc dev.
- Hiển thị tiền: dùng chung `formatMoney()` trong `utils/` (`Intl.NumberFormat('vi-VN')`), không mỗi màn tự format một kiểu.
- Tên file: component `PascalCase.jsx` (`RoyaltyConfirmPage.jsx`), file khác `camelCase.js`.

### 1.7 Đặt tên chung

| Loại | Quy ước | Ví dụ |
|---|---|---|
| Class Java | PascalCase + hậu tố theo tầng | `RoyaltyService`, `ArticleRoyaltyRepository` |
| Method, biến | camelCase, là động từ | `confirmRoyalty()`, `lockPeriod()` |
| Hằng số | UPPER_SNAKE | `MAX_AUTHORS = 5` |
| Bảng, cột DB | snake_case (đã có trong schema) | `article_royalties.applied_factor` |
| URL | kebab-case, số nhiều | `/api/royalty-periods` |
| Ngôn ngữ | Tên trong code bằng tiếng Anh, comment được viết tiếng Việt | |

**Không để "số ma" rải trong code:** các con số nghiệp vụ (ngưỡng 90/70, tỷ lệ 70/50/30%, 5 tác giả, 3 ngày hết hạn, 7 ngày khiếu nại...) đặt thành hằng số có tên, hoặc lấy từ bảng cấu hình của Admin.

### 1.8 Git

**Nhánh**

> **ĐÃ ĐỔI:** nhóm bỏ nhánh `develop`, chỉ dùng `main` + nhánh feature (xem `AGENTS.md` mục 7).

```
main                          ← nhánh chung, luôn chạy được, dùng để demo review iteration
feature/bf04-cham-nhuan-but   ← mỗi việc một nhánh, tách từ main, PR vào main
```

- Không ai push thẳng lên `main`, mọi thay đổi đi qua Pull Request.
- Trước khi tạo PR: `git pull origin main` về nhánh mình, sửa conflict, chạy lại app.

**Commit message:** `[BF-04] Thêm API gợi ý hệ số nhuận bút`
(Việc dùng chung thì ghi `[COMMON]`, sửa DB thì ghi `[DB]`.)

**Sửa database**

- `database/schema.sql` là nguồn duy nhất. Không ai tự sửa bảng trong SSMS rồi quên cập nhật file.
- PR có sửa `schema.sql` thì ghi rõ **"CÓ ĐỔI DB"** ở đầu mô tả. Sau khi merge, cả nhóm chạy lại `schema.sql` + `data.sql`.
- Đề xuất: 1 người giữ vai trò "chủ DB", duyệt mọi PR có sửa `schema.sql`.

**`.gitignore` tối thiểu:** `server/target/`, `client/node_modules/`, `client/dist/`, `application-local.properties`, `.env.local`, `.idea/`, `.vscode/`

---

## Phần 2 — Thứ tự & phụ thuộc giữa 5 luồng

*(Chưa viết — làm sau khi Phần 1 được chốt)*

## Phần 3 — Definition of Done & quy trình review

*(Chưa viết — làm sau khi Phần 2 được chốt)*

---

## Câu hỏi mở cần nhóm chốt

| # | Câu hỏi | Ảnh hưởng tới |
|---|---|---|
| 1 | Tên package gốc: `com.<tennhom>.swp391` là gì? | 1.2 |
| 2 | Tên chính thức của BF-03 → đặt tên package (`complaint`?) | 1.2 |
| 3 | Service có bắt buộc tách `interface` + `Impl` không? (Một số thầy yêu cầu, nếu không thì bỏ cho gọn) | 1.3 |
| 4 | Có dùng Lombok không? | 1.3, 1.4 |
| 5 | Đăng nhập bằng JWT hay session? Phân quyền theo role làm ở đâu? | 1.5, 1.6 |
| 6 | Frontend dùng JavaScript hay TypeScript? Có dùng thư viện UI (Ant Design, MUI...) không? | 1.6 |
| 7 | Ai là "chủ DB"? | 1.8 |
