# Kế hoạch việc của MINH — trước demo thứ 4 (04/11/2026)

> **Đọc cùng:** `docs/KE_HOACH_THU4_QUANG.md` (việc của Quang).
> **Mã việc:** `M-xx` là của Minh, `Q-xx` là của Quang. Hai bạn nhắn nhau theo mã cho nhanh, ví dụ "Q-D1 merge rồi".
> **Đánh dấu tiến độ:** `[ ]` chưa làm, `[~]` đang làm, `[x]` đã merge vào `main`. Sửa file này trong PR của chính việc đó.
> Số dòng ghi trong file là số dòng lúc lập kế hoạch (02/11). Sau khi sửa có thể lệch vài dòng, nên tìm theo tên hàm.

---

## 0. Bối cảnh

- **Thứ 4 (04/11) thầy xem demo cả dự án.** Một bài phải đi được hết chuỗi trên giao diện: nộp bài → chấm → khiếu nại → đăng bài → nhuận bút → KPI. Kịch bản demo ở mục 8.
- **Dương (BF-02), Vũ (BF-03), Khang (BF-05) đã dừng và giao code lại.** Từ giờ chỉ còn Minh và Quang. Mỗi người làm cả BE lẫn FE.
- **Hai vấn đề cần xử lý:**
  1. Các luồng chưa nối với nhau.
  2. Code lộn xộn: lệch AGENTS.md, có code trùng, có dữ liệu giả.
- **Thứ tự làm:**
  - **Nhóm A** là những thứ thầy sẽ thấy khi demo. Phải xong trước 12:00 thứ 3.
  - **Nhóm B** là dọn code, làm sau A, theo đúng thứ tự trong mục 6.
  - B nhiều khả năng **không xong hết** trước thứ 4, như vậy là bình thường. Phần còn lại làm tiếp sau thứ 4.

## 1. Minh giữ gì

| | Của Minh |
|---|---|
| Luồng | **BF-01 `article`** (Article, Duplication), **BF-02 `assessment`** (Assignment, CategoryChange, Criteria, ScoringAttempt, ScoreApproval, Publishing, ScoreCalculator, các class `Scoring*` của BF-02), **`admin`** (Role, Section, User, Genre, MediaFactor, SystemParameter) |
| Frontend | `pages/article/`, `pages/flow2/` (đổi tên thành `pages/assessment/` ở M-B4), `pages/admin/`, `pages/profile/`, `api/articleApi.js` và các file `api/` của BF-02 / admin |
| Trang chủ | `ReporterHome`, `EditorHome`, `HeadHome`, `AdminHome` (+ file `api/*HomeApi.js` tương ứng) |
| File dùng chung Minh giữ | `routes/AppRoutes.jsx`, `components/common/`, `utils/` (format ngày / tiền / số) |

**KHÔNG sửa.** Các file dưới đây Quang giữ, muốn đổi thì nhắn Quang kèm mã việc:

| File | Gửi Quang cái gì |
|---|---|
| `server/.../config/SecurityConfig.java` | Danh sách luật role, ví dụ `POST /api/assignments → HEAD`. Gửi ở M-B1. |
| `server/.../exception/GlobalExceptionHandler.java` | Loại exception cần bắt thêm |
| `database/data.sql`, `database/seeds/` | Dữ liệu cần có để demo phần của Minh, ví dụ "1 bài SUBMITTED ở ban X chưa phân công" |
| `client/src/index.css` | Biến màu `--fm-*` còn thiếu |
| `client/src/api/goiApi.js` | Tuỳ chọn mới cần thêm vào hàm gọi API chung |
| Code BF-03 / BF-04 / BF-05 | Hàm cần thêm hoặc sửa |

Ngoại lệ duy nhất là **M-B6**: PR đổi `Article` sang `@ManyToOne` được sửa luôn chỗ gọi ở phần của Quang, nhưng phải báo trước.

## 2. Lịch

| Lúc | Việc |
|---|---|
| Tối thứ 2 (02/11) | Đọc tài liệu (mục 9). Làm **M-D1**, sau đó M-A1. |
| Sáng thứ 3, trước 12:00 | Xong hết **nhóm A**, merge vào `main` |
| 12:00 thứ 3 | Cùng Quang chạy kịch bản demo (mục 8) trên DB tạo mới. Lỗi nào thì sửa luôn. |
| Trước 13:00 thứ 3 | Giao **M-D2** cho Quang |
| Chiều/tối thứ 3 | **Nhóm B** theo thứ tự M-B1 → M-B6 |
| **20:00 thứ 3: giờ cắt** | Mục B nào **chưa bắt đầu** thì dừng, để sau thứ 4. Nhất là M-B6. |
| **22:00 thứ 3: đóng băng** | Không merge B nữa. Cùng Quang tạo lại DB, chạy hết kịch bản demo. |
| Sáng thứ 4 | Chỉ tập demo và sửa lỗi phát hiện được |

## 3. Việc Minh phải giao cho Quang (Quang đang chờ)

- [ ] **M-D1 — Dọn `routes/AppRoutes.jsx`** (FE). **Làm đầu tiên, tối thứ 2**, để sau đó cả hai chỉ còn thêm route nhỏ, ít conflict.
  - Xoá route debug `/calculator`, `/flow2`, `/flow2-dashboard` (đang khai 3 lần, quanh dòng 226-227). Xoá luôn `/reviewer-deviations` vì trùng `/reviewers` (quanh dòng 318).
  - Trong các mảng `TRANG_..._CHUA_LAM`:
    - xoá dòng lặp: `/my-royalties` 2 lần (63-64), `/my-scorings` 2 lần (69, 72)
    - xoá các dòng đã có trang thật: Editor 68, 70, 71; Head 79-83; Admin 94-95
  - **Xong khi:** `npm run build` qua, console không còn cảnh báo trùng key, mọi menu cũ vẫn mở đúng trang.
- [ ] **M-D2 — Hàm đọc cho luồng khác** (BE). **Giao trước 13:00 thứ 3.**
  - Quang cần các hàm này để bỏ chỗ BF-03/BF-05 gọi thẳng repository hoặc JdbcTemplate của Minh (việc Q-B3, Q-B4).
  - **Trả về đúng các DTO đang có**, để Quang chỉ cần đổi chỗ gọi.
  - Tên hàm có thể đổi, nhưng phải **ghi tên thật vào bảng này** khi merge.

  | Thay cho (đang dùng ở phần Quang) | Hàm Minh thêm (đề xuất) | Trả về |
  |---|---|---|
  | `ArticleReadRepository.isAuthor` | `ArticleService.isAuthor(Integer articleId, Integer userId)` | `boolean` |
  | `ArticleReadRepository.findVersion` | `ArticleService.getVersionForRead(Integer articleId, Integer versionId)` | `ArticleVersionReadResponse` |
  | `ArticleReadRepository.findPublishedByAuthorId` | `ArticleService.getPublishedByAuthor(Integer authorId)` | `List<ArticleReadResponse>` |
  | `ArticleReadRepository.findById` | `ArticleService.getArticleForRead(Integer id)` | `ArticleReadResponse` |
  | `articleRepository.findAll()` (KpiAggregation 93, 276; GenreReport 70) | `ArticlePublishedService.getPublishedArticles()` (đã có; thêm field nếu KPI cần: genre, publishedAt, sectionId) | `List<ArticlePublishedResponse>` |
  | `articleAuthorRepository.findByArticleIdOrderBySharePercentDesc` | `ArticleService.getAuthors(Integer articleId)` | list `{userId, sharePercent}` |
  | `articleAuthorRepository.findByUserIdAndArticleIdIn` | `ArticleService.getAuthorShares(Integer userId, List<Integer> articleIds)` | list `{articleId, sharePercent}` |
  | `articleRepository.existsById` (CsvImport 173) | `ArticleService.existsArticle(Integer id)` | `boolean` |
  | `ScoringReadRepository.findSubmitted` + lọc theo bài | `ScoringAttemptService.getPublishedAttempt(Integer articleId)` (lấy thẳng 1 bài, không tải hết) | `ScoringReadResponse` |
  | `ScoringReadRepository.findCriteria` | `ScoringAttemptService.getAttemptCriteria(Integer attemptId)` | `List<ScoringCriterionResponse>` |
  | `UserReadRepository.findAll`, `userRepository.findAll/findById` (KpiAggregation 81, 157, 261) | `UserService.getUserAccess(Integer id)`, `UserService.getActiveUsersByRole(String roleCode)` | `UserAccessResponse` / list |
  | 3 bản `loadUserNames()` (ArticleService 346, ArticlePublishedService 94, RoyaltyPeriodService 376) | `UserService.getUserNames(Collection<Integer> ids)` | `Map<Integer, String>` |
  | `genreRepository.findByIsActiveTrueOrderByNameAsc` (GenreReport 57) | `GenreService.getActiveGenres()` (**đã có**, chỉ cần báo Quang) | `List<GenreResponse>` |
  | 5 bản hàm đọc tham số hệ thống (`EventService:78`, `RoyaltyCalculator:177`, `RoyaltyGradingService:504`, `ArticleEditorService:460`, `SystemParameterService:115`, hiện chỉ có `readInt` private) | `SystemParameterService.getIntValue(String key)`, `SystemParameterService.getDecimalValue(String key)`: thiếu key thì `BusinessException` tiếng Việt | `int` / `BigDecimal` |

  - **Xong khi:** mỗi hàm có unit test nhỏ hoặc gọi thử được, `mvn test` xanh, đã nhắn Quang "M-D2 merge rồi" kèm tên hàm thật.

## 4. Minh chờ Quang

| Mã | Hàm | Ở đâu | Dùng ở việc | Trạng thái |
|---|---|---|---|---|
| Q-D1 | `BigDecimal getEffectiveScore(Integer articleId)`: điểm sau khiếu nại nếu khiếu nại đã RESOLVED, nếu không thì là điểm S0 đã công bố; chưa có điểm thì `null`. Cần cả lần chấm đang dùng thì gọi `getEffectiveAttempt(articleId)` → `AppealEffectiveAttemptResponse(attemptId, score, fromAppeal)` | `AppealEffectiveScoreService` | M-A2 | [x] đã code + test (02/11), nằm trong PR của Quang |
| Q-D2 | `LocalDateTime getAppealDeadline(LocalDateTime announcedAt)`: `announcedAt + appeal_window_days` của cấu hình đang bật; `announcedAt` null thì trả null | `AppealRuleConfigService` | M-A5 | [x] đã code + test (02/11), nằm trong PR của Quang |

**Chưa có thì làm việc khác trước, không tự viết bản riêng.** Quang hứa xong cả hai trong **tối thứ 2**.

### Ghi chú từ Quang (02/11, 23:00)

- **Quang đã sửa nhỏ trong file của Minh** (để trang của Quang chạy được). Khi làm M-D1 / M-A6, pull về trước để khỏi conflict:
  - `routes/AppRoutes.jsx`: thêm route `/my-royalties` (MyRoyalties) và `/royalty-payments` (RoyaltyPayments); xoá 3 dòng tương ứng trong `TRANG_PV_CHUA_LAM` / `TRANG_KT_CHUA_LAM` (2 mảng giờ rỗng).
  - `api/reporterHomeApi.js`: xoá hàm giả `layViTien`, vì chip ví ở `ReporterLayout` giờ lấy số thật từ `royaltyApi.getUnpaidRoyaltyTotal()`.
- **M-A5:** test `ScoreApprovalServiceTest.getPendingPublishing_Success` **đã qua** trên `main` hiện tại (223/223 test xanh), không cần sửa nữa. Chỉ còn việc thay `plusDays(7)`.
- **M-A2 có bằng chứng thật:** trên DB demo, hàng chờ duyệt đăng trả bài `[BF03-DEMO-03]` với `appealDeadline: null`, `isAppealWindowOpen: false`, vì đang lấy nhầm lần chấm APPEAL làm "lần mới nhất".
- **Thêm vào M-D2:** Quang cần `MediaFactorService` có hàm lấy hệ số theo loại ảnh, ví dụ `BigDecimal getFactor(String mediaKind)`, để `RoyaltyGradingService` bỏ gọi thẳng `MediaFactorRepository`.
- **Dữ liệu demo:** thêm `database/seeds/DEMO_0411_appeal_kpi.sql`. Thứ tự nạp DB mới: `schema.sql` → `data.sql` → `seeds/BF04_sample_royalty.sql` → `seeds/BF04_sample_one_reporter.sql` → `seeds/DEMO_0411_appeal_kpi.sql`.
- **Luật role BF-05 đã có trong `SecurityConfig`:**
  - Chi tiết KPI 1 phóng viên: chỉ Phóng viên (xem của mình) và Trưởng ban.
  - Nếu trang chủ Reporter / Head (M-A6) gọi API KPI khác thì báo Quang.

## 5. Nhóm A — demo thấy được (xong trước 12:00 thứ 3)

- [ ] **M-A1 — Trưởng ban và BTV được xem chi tiết bài** (BE + FE)
  - **File:** `service/ArticleService.java` quanh dòng 137 (đang có `TODO(BF-02)`) và `client/src/pages/flow2/PublishingQueue.jsx` dòng 66-68.
  - **Làm:**
    - Ngoài tác giả, cho xem thêm 2 người: BTV đang được phân công bài đó, và Trưởng ban của ban chứa bài.
    - `PublishingQueue` đang nuốt lỗi khi gọi `getArticleDetail`; phải hiện thông báo lỗi.
  - **Xong khi:** đăng nhập Trưởng ban, mở hàng chờ duyệt đăng là thấy nội dung bài. Người không liên quan vẫn bị chặn với message tiếng Việt.
- [ ] **M-A2 — Lần chấm APPEAL không làm hỏng BF-02** (BE). *Cần Q-D1.*
  - **Vì sao:** BF-03 ghi lần chấm khiếu nại vào `scoring_attempts` với `attemptType = APPEAL` và `attemptNo` = max+1. Mọi chỗ BF-02 dùng `findTopByArticleIdOrderByAttemptNoDesc` đang lấy nhầm lần APPEAL đó.
  - **File:**
    - `repository/ScoringAttemptRepository.java:46`
    - `service/ScoreApprovalService.java`: dòng 68, 96, 148, 207
    - `service/ScoringAttemptService.java`: dòng 67, 142, 198, 242, 245, 310, 389
  - **Làm:**
    - Thêm query method bỏ qua APPEAL, ví dụ `findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc(articleId, AttemptType.APPEAL)`. Thay vào các chỗ trên của BF-02.
    - BTV gốc không được thấy bản nháp APPEAL (`ScoringAttemptService` quanh dòng 75).
    - Hàng chờ duyệt đăng (UC-33) hiện điểm lấy từ `getEffectiveScore(articleId)` (Q-D1).
    - `GenreReportService:175` là phần của Quang, Quang tự sửa ở Q-A3.
  - **Xong khi:** bài có khiếu nại đang chấm lại **vẫn nằm** trong hàng chờ đăng, hiện điểm S0. Khi khiếu nại đã kết luận thì hiện điểm sau khiếu nại. UC-34 vẫn hiện đúng hạn khiếu nại. Có unit test cho query mới.
- [ ] **M-A3 — Bỏ dữ liệu viết cứng, lấy người thao tác từ token** (BE + FE)
  - **Thể loại viết cứng:**
    - Ở `pages/flow2/ScoringSheet.jsx:43-52` (8 thể loại) và `pages/flow2/CriteriaManagement.jsx:177-180` (4 thể loại). Id lệch với `data.sql`: id 1 trong seed là "Tin vắn".
    - **Làm:** gọi `GET /api/genres` (**đã có**, `GenreController` → `GenreService.getActiveGenres()`).
  - **Danh sách BTV viết cứng:**
    - Ở `pages/flow2/AssignmentQueue.jsx:37-41` (có lẫn cả Thư ký id 4).
    - **Làm:** gọi `GET /api/reviewers` (**đã có**, `ReviewerProfileController.getEditors`). Nếu API này không lọc đúng BTV trong ban của Trưởng ban, nhắn Quang (file BF-03).
  - **Người thao tác lấy từ token:**
    - `dto/AssignmentCreateRequest.assignedBy` (dòng 20), `dto/CriteriaSetVersionCreateRequest.createdBy` (dòng 26), `activatedBy` ở `CriteriaSetController.java:69`.
    - **Làm:** bỏ các field này khỏi request. Controller lấy `Integer.valueOf(jwt.getSubject())` qua `@AuthenticationPrincipal Jwt jwt` rồi truyền vào service. Bỏ chỗ ngầm định user **1** ở `CriteriaSetService.java:124,181`. FE bỏ `assignedBy` mặc định 5 (`AssignmentQueue.jsx:124`).
  - **Ban của Trưởng ban:**
    - Đang lấy `sectionId` từ query string (`AssignmentController:40`, `ScoreApprovalController:42,77`).
    - **Làm:** lấy từ user đang đăng nhập (`users.section_id`).
  - **Xong khi:** không còn danh sách cứng. Gửi request thiếu `assignedBy` vẫn chạy và ghi đúng người. Đổi tài khoản Trưởng ban ban khác thì thấy dữ liệu ban đó.
- [ ] **M-A4 — Chặn đổi trạng thái sai, đổi thể loại có log** (BE)
  - **Chặn trạng thái:**
    - `AssignmentService.assignArticle` (quanh dòng 99) đang cho phân công cả bài đã PUBLISHED.
    - **Làm:** chỉ nhận bài đang SUBMITTED (hoặc trạng thái SRS BF-02 cho phép phân công lại). Sai trạng thái thì `throw new BusinessException("...")`.
  - **Đổi thể loại:**
    - `ScoringAttemptService.java:272-273` đang ghi thẳng `articles.category_id`.
    - **Làm:** thêm hàm trong `ArticleService` để đổi thể loại có ghi log, rồi gọi hàm đó. Log ghi vào `category_change_logs` hoặc `article_status_logs`, xem comment trong `schema.sql`.
  - **Xong khi:** gọi phân công cho bài PUBLISHED thì trả 409 kèm message tiếng Việt. Đổi thể loại thì có dòng log.
- [ ] **M-A5 — Hạn khiếu nại theo cấu hình + sửa test fail** (BE). *Cần Q-D2.*
  - **File:** `ScoreApprovalService.java:228`, `ScoringAttemptService.java:162` đang có `plusDays(7)`.
  - **Làm:**
    - Thay `plusDays(7)` bằng `appealRuleConfigService.getAppealDeadline(...)`.
    - Sửa test đang fail `ScoreApprovalServiceTest.getPendingPublishing_Success` (dòng 266, mong 4 nhưng ra 5).
  - **Xong khi:** đổi `appeal_window_days` trong trang admin thì UC-34 và màn khiếu nại BF-03 hiện **cùng một hạn**. `mvn test` xanh.
- [ ] **M-A6 — 4 trang chủ dùng API thật** (FE, có thể thêm BE nhỏ)
  - **Bỏ `giaLapGoiApi`** ở `api/reporterHomeApi.js`, `api/editorHomeApi.js`, `api/headHomeApi.js`, `api/adminHomeApi.js`.
  - **Reporter:** đã có sẵn, chỉ cần gọi:
    - `getMyStatusCounts`, `getMyRevisionRequests` (`articleApi.js:54,59`)
    - `getMyCoAuthorInvites`
    - `kpiApi.layChiTietKpiPhongVien`
    - Chip ví nhuận bút ở layout là phần của Quang (Q-A5).
  - **Editor / Head / Admin:** ghép từ API sẵn có của assessment / appeal / kpi. Ô nào chưa có API thì **bỏ ô đó**, không để số giả.
  - **Sửa link:**
    - `ScoreApprovalQueue` đọc tham số `:id` (HeadHome:41 đang trỏ vào).
    - `CriteriaManagement` đọc `:id`, `?genreId`, `?status` (AdminHome:27, 41, 70).
    - EditorHome:16-19, 154 đang trỏ `/my-scorings` (xem M-A7).
  - **Xong khi:** `grep -rn giaLapGoiApi client/src/api` không còn ở 4 file trên. Số trên trang chủ khớp với dữ liệu thật.
- [ ] **M-A7 — Không còn menu ComingSoon ở phần Minh** (BE + FE)
  - **Editor `/my-scorings`** (`EditorLayout.jsx:25`): danh sách các lần chấm của BTV đang đăng nhập. Có thể dùng `GET /api/reviewers/{editorId}/scoring-history` (đã có) hoặc API của BF-02.
  - **Admin `/system-parameters`:** trang xem/sửa, API **đã có** (`SystemParameterController`).
  - **Admin `/genres`:** trang danh sách (`GET /api/genres` đã có).
  - **Admin `/users`:** trang danh sách user. Cần thêm API admin `GET /api/users`, nhắn Quang thêm luật `hasRole("ADMIN")`.
  - **Không kịp trang nào thì xoá mục đó khỏi menu** (`layouts/*Layout.jsx`). Lúc demo không được mở ra ComingSoon.
  - **Xong khi:** bấm hết menu của Editor và Admin, không trang nào là ComingSoon.

## 6. Nhóm B — dọn code (sau 12:00 thứ 3, làm đúng thứ tự)

- [ ] **M-B1 — BF-02 backend theo AGENTS.md**
  - **Bỏ chỗ gọi repository luồng khác,** đổi sang gọi service của luồng đó (`ArticleService`, `UserService`, `GenreService`). Các chỗ: `AssignmentService.java:44-65`, `ScoreApprovalService.java:49-50`, `ScoringAttemptService.java:45-51`.
  - **`ScoringAttemptService.java:54`** đang dùng `synchronized` cùng `@Transactional`, khoá nhả trước khi commit. **Làm:** bỏ `synchronized`, thêm `@Lock(PESSIMISTIC_WRITE)` trên query method trong repository.
  - **`ScoreCalculator`** đang tạo bằng `new` (`ScoringAttemptService.java:368`). **Làm:** inject.
  - **Gộp `findById().orElseThrow` lặp lại** thành 1 hàm private, ví dụ `findAttemptOrThrow`. Có 6 chỗ ở `ScoringAttemptService` (57, 140, 186, 225, 301, 380) và 3 chỗ ở `ScoreApprovalService` (89, 141, 266).
  - **Bỏ N+1** (lấy tên user từng dòng): `ScoreApprovalService:73, 212-217`, `AssignmentService:60`. **Làm:** dùng `UserService.getUserNames(ids)`.
  - **Exception sai loại:** `ScoringAttemptService:202-218` đang ném `ResponseStatusException`. **Làm:** đổi sang `NotFoundException` / `BusinessException`. Message của `ScoreCalculator:45-82` phải là tiếng Việt (handler của Quang ở Q-B1 sẽ trả 409 cho `IllegalArgumentException`).
  - **Luật role cho Assignment / CriteriaSet / ScoreApproval:** gửi Quang danh sách để Quang thêm vào `SecurityConfig` (Q-B2).
- [ ] **M-B2 — BF-01 backend**
  - `findArticle()` đang có 3 bản (`ArticleService:250`, `ArticleEditorService:444`, `ArticleConfirmationService:211`). **Làm:** giữ 1 bản ở `ArticleService`, 2 chỗ kia gọi lại.
  - Đang map DTO trong service (`ArticleService:292`). **Làm:** chuyển sang `XxxResponse.from(entity)`.
  - Hàm đọc tham số hệ thống viết riêng ở `ArticleEditorService:460`. **Làm:** gọi `SystemParameterService.getIntValue` / `getDecimalValue` (thêm ở M-D2).
- [ ] **M-B3 — Frontend dùng chung**
  - Modal viết 4 lần (AppealRuleConfigPage, PublishingQueue, ScoreApprovalQueue, MonthlyTargetSetting). **Làm:** tạo `components/common/Modal.jsx` (+ css).
  - Phân trang viết 4 lần (MyArticles, AppealManagementPage, ReviewerProfilePage, ReaderDataImport). **Làm:** tạo `components/common/Pagination.jsx`.
  - Hàm định dạng ngày (AssignmentQueue:138, ScoreApprovalQueue:134, ReaderDataImport:130, `appeal/appealFormat.js:10`, `HomeFrame.jsx:4`). **Làm:** gộp vào `utils/formatDate`, có `dinhDangNgay` / `dinhDangNgayGio`.
  - Hàm "hôm nay yyyy-MM-dd" đang có 3 bản. **Làm:** gộp vào `utils`.
  - `toLocaleString` viết tay. **Làm:** đổi sang `formatMoney` / `formatNumber`.
  - Tính tổng điểm có trọng số viết 4 lần (ScoringSheet:109, ScoreCalculatorTester:27, ArticleScoreCard:149, `appeal/blindScorecard.js:18`). **Làm:** gộp thành 1 hàm trong `utils/`.
  - `NATURE_LABELS` có ở cả `utils/articleLabels.js:61` và `utils/royaltyLabels.js:46`. **Làm:** giữ 1 bản.
  - **Thay ở phần của Minh xong thì nhắn Quang "M-B3 merge rồi"** để Quang thay ở appeal / kpi (Q-B6).
- [ ] **M-B4 — Frontend BF-02**
  - Đổi tên thư mục `pages/flow2` → `pages/assessment`, sửa import trong `AppRoutes.jsx` và `article/ArticleDetail.jsx:9`.
  - Xoá `Flow2Dashboard.jsx` và `ScoreCalculatorTester.jsx`.
  - CSS của flow2 đang để trong `src/styles/`. **Làm:** chuyển ra cạnh component.
  - Bỏ mã màu hex, dùng `--fm-*`. Thiếu biến thì nhắn Quang.
  - Bỏ inline style ở `ScoreApprovalQueue` (36 chỗ) và `ScoringSheet` (34 chỗ).
- [ ] **M-B5 — BF-02: tách hàm dài, đặt lại tên**
  - Tách các hàm 65–80 dòng thành hàm nhỏ có tên dễ hiểu: `ScoringAttemptService:55, 223, 299, 378`, `ScoreApprovalService:192`.
  - Đổi tên DTO `*DTO` / `*Dto` → `*Response` / `*Request`, ví dụ `EditorWorkloadDTO`.
  - Endpoint tạo mới trả 201 (`AssignmentController:51`).
  - Sub-path số ít `/article/{id}`, `/editor/{id}`, `/category/{id}`. **Làm:** chỉ đổi nếu kịp sửa cả FE.
- [ ] **M-B6 — `@ManyToOne` cho entity BF-01 / BF-02 / admin. Rủi ro cao nhất, làm cuối.**
  - Chỉ bắt đầu **trước 20:00 thứ 3** và **sau khi Quang đã merge xong** các việc đang làm.
  - Các field `Integer xxxId` đã có entity đích cần đổi:
    - **Article:** `Article:39, 45, 47, 50, 53`. Không đổi `sectionId` vì chưa có entity Section.
    - **Các entity khác của BF-01:** `ArticleVersion:35, 52`, `ArticleStatusLog:35, 38`, `ArticleAttachment:38, 40, 43, 69`, `DuplicationCheck:36, 38`.
    - **BF-02:** `Assignment:30, 34, 38`, `CategoryChangeLog:34, 38, 42`, `CriteriaSetVersion:30, 41, 48`, `ScoringAttempt:31, 35, 43, 47, 62`, `ScoreApproval:42`.
    - **`ArticleAuthor:39`** nằm trong khoá ghép, cần `@MapsId`. Khó thì bỏ qua.
  - **Cách làm:**
    - Đổi **mỗi entity 1 PR**.
    - Sửa hết chỗ gọi `getXxxId()`, kể cả ở phần Quang (báo Quang trước).
    - Server phải khởi động được (`ddl-auto=validate`) và `mvn test` xanh.

## 7. Quy tắc PR / test (áp dụng mọi việc)

- **Nhánh:**
  - Mỗi việc (hoặc vài việc nhỏ cùng file) là 1 nhánh `feature/<luồng>-<việc>` tạo từ `main`, ví dụ `feature/bf02-appeal-attempt`.
  - `git pull origin main` trước khi tạo nhánh.
  - AI không tự tạo nhánh hay commit nếu Minh chưa bảo.
- **Commit:** theo AGENTS.md §7, ví dụ `[BF-02] Bỏ qua lần chấm APPEAL khi lấy lần chấm mới nhất`.
- **Trước khi mở PR:**
  - [ ] `mvn -f server/pom.xml test` xanh
  - [ ] `npm run build` trong `client/` qua
  - [ ] Server khởi động được (không lỗi `validate`)
  - [ ] Bấm thử đúng màn hình đã sửa
- **Duyệt:** PR nhỏ, Quang duyệt trong khoảng 1 giờ. Gấp thì nhắn mã việc.

## 8. Kịch bản demo (cũng là bài test cuối, 12:00 và 22:00 thứ 3)

1. **Phóng viên:** trang chủ có số thật → tạo bài → nộp.
2. **Trưởng ban:** phân công BTV (danh sách thật). *(M-A3)*
3. **BTV:** chấm phiếu. *(thể loại đúng — M-A3)*
4. **Trưởng ban:** duyệt điểm → công bố.
5. **Phóng viên:** xem điểm → khiếu nại. *(hạn đúng — M-A5)*
6. **Chấm lại** (mù / chéo) → kết luận khiếu nại. *(Quang)*
7. **Trưởng ban:** duyệt đăng. Thấy nội dung bài và điểm sau khiếu nại. *(M-A1, M-A2)*
8. **Thư ký:** chấm nhuận bút, dùng điểm sau khiếu nại. *(Quang)*
9. **Kế toán:** duyệt / khoá kỳ → ghi nhận chi trả → kỳ sang PAID. *(Quang)*
10. **Phóng viên:** Nhuận bút của tôi + KPI cá nhân có điểm chất lượng. *(Quang)*
11. **Trưởng ban:** KPI phòng, báo cáo thể loại. *(Quang)*
12. **Admin:** tham số, tiêu chí, người dùng, thể loại. *(M-A7)*

Không menu nào mở ra ComingSoon, không trang nào hiện số giả.

## 9. Gửi AI của Minh (dán vào đầu mỗi phiên)

> Tôi là Minh, phụ trách BF-01 `article`, BF-02 `assessment` và `admin` (đã nhận lại từ Dương). Việc của tôi nằm trong `docs/KE_HOACH_THU4_MINH.md`. Phiên này làm việc **<mã việc, ví dụ M-A2>**.
> Trước khi sửa code:
> - Làm Bước 0 của AGENTS.md: đọc `database/schema.sql` (các bảng của việc này), `docs/SRS_Overview.md`, `docs/CODING_PLAN.md`.
> - Đọc thêm `docs/SRS_BF01_Article.md` hoặc `docs/SRS_BF02_Scoring.md` tuỳ việc. `docs/BF02_SYSTEM_ARCHITECTURE.md` giúp hiểu code BF-02 hiện có.
> - Không sửa file Quang giữ (mục 1 của file kế hoạch). Cần thì dừng lại và nói tôi nhắn Quang.
> - Liệt kê các bước nhỏ, làm từng bước, chạy test/build, rồi giải thích ngắn bằng tiếng Việt.
> - Xong thì đánh dấu `[x]` cho việc đó trong file kế hoạch.
