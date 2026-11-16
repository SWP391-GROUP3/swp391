# Kế hoạch việc của QUANG (nhóm trưởng) — trước demo thứ 4 (04/11/2026)

> **Đọc cùng:** `docs/KE_HOACH_THU4_MINH.md` (việc của Minh).
> **Mã việc:** `Q-xx` là của Quang, `M-xx` là của Minh. Hai bạn nhắn nhau theo mã cho nhanh.
> **Đánh dấu:** `[ ]` chưa làm, `[~]` đang làm, `[x]` đã merge vào `main`. Sửa file này trong PR của chính việc đó.
> Số dòng là số lúc lập kế hoạch (02/11), có thể lệch vài dòng. Nên tìm theo tên hàm.

---

## 0. Bối cảnh

- **Thứ 4 (04/11) thầy xem demo cả dự án.** Một bài phải đi được hết chuỗi trên giao diện (kịch bản ở mục 9).
- **Dương, Vũ, Khang đã dừng và giao code lại.** Từ giờ chia đôi:
  - **Minh** giữ thượng nguồn: BF-01, BF-02, admin.
  - **Quang** giữ hạ nguồn: BF-03, BF-04, BF-05. Ba luồng này đều dùng điểm đã chấm.
- **Chỗ đứt chính nằm ở phần của Quang:**
  - Kết quả khiếu nại chưa đi tiếp sang nhuận bút và KPI (`AppealReviewService.java:122` ghi `PENDING_INTEGRATION`).
  - KPI có `effectiveScore = null` (`KpiAggregationService.java:112`), nên ai cũng NOT_PASS.
  - Chưa có ghi nhận chi trả (UC-54), nên kỳ không bao giờ sang PAID.
  - `data.sql` chưa có bài nào đi trọn chuỗi.
- **Thứ tự làm:**
  - Nhóm **A** (demo thấy được) xong trước 12:00 thứ 3.
  - Nhóm **B** (dọn code) làm sau, theo thứ tự trong mục 6.
  - B không xong hết trước thứ 4 là bình thường.

## Tình trạng (02/11/2026, 23:00): đã code + test, CHƯA commit / PR

| Mã | Tình trạng | Ghi chú |
|---|---|---|
| Q-D1, Q-D2 | xong | `getEffectiveScore` / `getEffectiveAttempt`, `getAppealDeadline` + unit test |
| Q-A1 | xong | `seeds/DEMO_0411_appeal_kpi.sql`. Đã nạp thử cả chuỗi 5 file vào DB riêng `swp391_kpi_demo`, không lỗi |
| Q-A2 | xong | Kiểm trên DB demo: bài DEMO-03 chấm nhuận bút bằng 85.50 (điểm sau khiếu nại), không phải 82 |
| Q-A3 | xong | KPI An 10/2026: điểm từng bài 82.5 / 85.5 / 92.5, TB 86.83, PASS |
| Q-A4 | xong | API + trang `/royalty-payments`. Chi hết phiếu thì kỳ 8 sang PAID; PV gọi bị 403 |
| Q-A5 | xong | Trang `/my-royalties` + chip ví (= tổng nhuận bút chưa nhận) |
| Q-A6 | xong | SecretaryHome hết crash, AccountantHome số thật, RoyaltyPeriods đọc `?status` |
| Q-A7 | xong một phần | Trang KPI mặc định tháng hiện tại. **Chưa** bấm thử trọn vòng khiếu nại mới trên giao diện |
| Q-B1 | xong (đổi so với kế hoạch) | Bắt `IllegalArgument` → 400, `IllegalState` → 500 kèm câu tiếng Việt. **Không** đổi `ResponseStatusException`: 22 chỗ đều là 403/400 đúng nghĩa, đổi sang `BusinessException` sẽ thành 409 sai |
| Q-B2 | xong phần BF-05 | Luật role KPI; PV chỉ xem KPI của mình. Phần BF-02 chờ Minh gửi danh sách |
| Q-B4 | một phần | Service tổng hợp trả DTO, gộp kiểm role lặp, bỏ `catch (Exception)`. `@Valid` cho list: bỏ qua vì DTO không có ràng buộc. **Chưa** tách hàm dài, chưa gộp URL |
| Q-B5 | một phần | `loadUserNames` dùng chung `UserReadService.getUserNames()`, sửa comment cũ. Phần tham số hệ thống / `MediaFactorService` chờ M-D2 |
| Q-B6 | một phần | Thêm biến `--fm-primary`, `--fm-border`... vào `index.css`; tải CSV mẫu qua `goiApi({ taiFile: true })`. Modal / Pagination chờ M-B3 |
| Q-B3, Q-B7 | chưa làm | B3 phần JdbcTemplate chờ M-D2; B7 `@ManyToOne` để sau thứ 4 |

## 1. Quang giữ gì

| | Của Quang |
|---|---|
| Luồng | **BF-03 `appeal`** (Appeal, CrossReview, ReviewerDeviation, ReviewerProfile, ReviewerDirectory, các class `Scoring*` / `*Read*` do BF-03 viết: `ScoringAppealService`, `ScoringReadService`, `ArticleReadService`, `UserReadService` và repository JdbcTemplate đi kèm), **BF-04 `royalty`** (Royalty, Event), **BF-05 `kpi`** (Kpi, CsvImport, ReaderInteraction, DepartmentKpi, ReporterKpi, GenreReport) |
| Frontend | `pages/appeal/`, `pages/royalty/`, `pages/kpi/`, các file `api/` tương ứng |
| Trang chủ | `SecretaryHome`, `AccountantHome` (+ `api/accountantHomeApi.js`), chip ví nhuận bút ở layout Reporter |
| File dùng chung Quang giữ | `database/data.sql` + `database/seeds/`, `config/SecurityConfig.java`, `exception/GlobalExceptionHandler.java`, `client/src/index.css` (biến `--fm-*`), `client/src/api/goiApi.js`, `AGENTS.md` |

**KHÔNG sửa.** Các file dưới đây Minh giữ, cần đổi thì nhắn Minh kèm mã việc:
- `routes/AppRoutes.jsx`, `components/common/`, `utils/`
- code BF-01 / BF-02 / admin: `ArticleService`, `ScoringAttemptService`, `UserService`, `SystemParameterService`...

Khi cần thêm route cho trang của Quang (`/my-royalties`, `/royalty-payments`):
- chờ **M-D1** merge xong rồi mới thêm, mỗi lần 1–2 dòng;
- hoặc nhắn Minh thêm hộ.

## 2. Lịch

| Lúc | Việc |
|---|---|
| Tối thứ 2 (02/11) | Merge PR kế hoạch (**Q-L1**). Làm **Q-D1, Q-D2** (Minh đang chờ). Bắt đầu Q-A1. |
| Sáng thứ 3, trước 12:00 | Xong hết **nhóm A** |
| 12:00 thứ 3 | **Q-L4:** tạo lại DB, cùng Minh chạy kịch bản demo |
| Chiều/tối thứ 3 | **Nhóm B** theo thứ tự Q-B1 → Q-B7. Q-B3 và Q-B4 cần M-D2 (Minh hẹn trước 13:00). |
| **20:00 thứ 3: giờ cắt** | Mục B nào chưa bắt đầu thì dừng, để sau thứ 4. Nhất là Q-B7. |
| **22:00 thứ 3: đóng băng** | Không merge B nữa. **Q-L4** lần 2: chạy hết kịch bản với mọi role. |
| Sáng thứ 4 | Chỉ tập demo và sửa lỗi phát hiện được |

## 3. Việc Quang phải giao cho Minh (Minh đang chờ, làm tối thứ 2)

- [ ] **Q-D1 — `BigDecimal getEffectiveScore(Integer articleId)`** trong `service/AppealEffectiveScoreService.java` (BE)
  - **Đã có sẵn** bản 2 tham số `getEffectiveScore(Integer articleId, BigDecimal publishedScore)` (dòng 26).
  - **Làm:** thêm bản 1 tham số.
    - Tự lấy điểm S0 qua `ScoringReadService.getPublishedAttempt(articleId)`; sau Q-B3 thì lấy qua hàm của Minh ở M-D2.
    - Rồi gọi lại bản 2 tham số.
    - Chưa có điểm công bố thì trả `null`.
  - **Dùng ở:** M-A2 (hàng chờ đăng bài), Q-A2 (nhuận bút), Q-A3 (KPI). Đây là **nguồn điểm duy nhất** sau khi chấm.
  - **Xong khi:** có unit test cho 3 trường hợp, gồm chưa có khiếu nại, khiếu nại chưa xong, khiếu nại RESOLVED. Đã nhắn Minh "Q-D1 merge rồi".
- [ ] **Q-D2 — `LocalDateTime getAppealDeadline(LocalDateTime announcedAt)`** trong `service/AppealRuleConfigService.java` (BE)
  - **Làm:**
    - Thêm hàm `announcedAt.plusDays(getActiveConfigEntity().getAppealWindowDays())`.
    - Thay 2 chỗ tự cộng trong `AppealService.java:158, 231` bằng hàm này.
  - **Dùng ở:** M-A5, thay `plusDays(7)` ở BF-02.
  - **Xong khi:** có unit test, `mvn test` xanh, đã nhắn Minh.

## 4. Quang chờ Minh

| Mã | Cái gì | Dùng ở việc | Trạng thái |
|---|---|---|---|
| M-D1 | Dọn `AppRoutes.jsx` | Thêm route `/my-royalties`, `/royalty-payments` (Q-A4, Q-A5) | [ ] |
| M-D2 | Các hàm đọc trong `ArticleService`, `ArticlePublishedService`, `ScoringAttemptService`, `UserService` (`getUserNames`, `getUserAccess`, `getActiveUsersByRole`), `SystemParameterService` (`getIntValue`, `getDecimalValue`). Bảng chi tiết ở mục 3 file của Minh. | Q-B3, Q-B4, Q-B5 | [ ] |
| M-B3 | `components/common/Modal`, `Pagination`, `utils/formatDate` | Q-B6 | [ ] |
| (gửi) | Danh sách luật role cho BF-02 | Q-B2 | [ ] |

Minh chưa giao thì làm việc khác trước, **không tự viết bản riêng** vào file của Minh.

## 5. Nhóm A — demo thấy được (xong trước 12:00 thứ 3)

- [ ] **Q-A1 — Dữ liệu demo** (DB). **Làm sớm nhất, Minh cũng cần để test.**
  - **File:** `database/data.sql`. Gộp luôn `database/seeds/BF04_sample_one_reporter.sql` (đang chưa commit).
  - **Cần có:**
    - **1 bài "vàng"** để chạy trực tiếp: phóng viên có sẵn bài DRAFT, hoặc tạo mới lúc demo.
    - **Ít nhất 1 bài đứng chờ ở mỗi bước:**
      - SUBMITTED chưa phân công
      - ASSIGNED đang chấm
      - đã chấm, chờ duyệt điểm
      - SCORE_PUBLISHED, **hạn khiếu nại còn mở tới sau 04/11** (`announced_at` gần đây)
      - đang khiếu nại, có task chấm lại mở
      - khiếu nại RESOLVED và **điểm đổi** so với S0, chờ duyệt đăng
      - PUBLISHED chưa chấm nhuận bút
      - đã chấm nhuận bút trong kỳ OPEN
    - **Kỳ nhuận bút:** hiện chỉ có `(2026, 9, 'OPEN')` ở dòng 98-99. Thêm 1 kỳ **LOCKED** có phiếu UNPAID để demo UC-54, và 1 kỳ OPEN của tháng 10/11 cho bài mới.
    - **KPI:** chỉ tiêu và snapshot cho tháng có dữ liệu, để trang KPI không trống.
    - **Danh sách tài khoản theo role** (mật khẩu `123456`), ghi vào mục 9 của file này để demo khỏi nhớ.
  - **Hỏi Minh** dữ liệu Minh cần cho BF-01 / BF-02 / admin.
  - **Xong khi:** xoá DB, chạy `schema.sql` + `data.sql` không lỗi, server khởi động, mỗi bước ở mục 9 có sẵn bài.
- [ ] **Q-A2 — Nhuận bút dùng điểm sau khiếu nại** (BE). *Sau Q-D1.*
  - **File:** `service/RoyaltyGradingService.java`, hàm `calculate`, quanh dòng 261. Hiện đang lấy S0 qua `getPublishedAttempt`.
  - **Làm:**
    - Đổi sang `getEffectiveScore(articleId)`.
    - Sửa `AppealReviewService.java:122` bỏ `PENDING_INTEGRATION`, ghi đúng trạng thái.
    - Phần ADJUSTMENT khi kỳ đã LOCKED: **để sau thứ 4**, như đã chốt ngày 27/10.
  - **Xong khi:** bài có khiếu nại RESOLVED được chấm nhuận bút theo điểm mới. Có unit test.
- [ ] **Q-A3 — KPI dùng điểm thật** (BE). *Sau Q-D1.*
  - **File:**
    - `service/KpiAggregationService.java:111-112`, đang để `effectiveScore = null`.
    - `KpiAggregationService.java:322-323`, màn chi tiết đang hiện điểm TB của snapshot cho từng bài.
    - `service/GenreReportService.java:175`, đang lấy lần chấm mới nhất, kể cả APPEAL.
  - **Làm:** cả 3 chỗ dùng `getEffectiveScore(articleId)`.
  - **Xong khi:**
    - Có `actual_avg_score`, `KpiCalculator.evaluatePassStatus` (dòng 109) ra PASS/NOT_PASS đúng theo dữ liệu.
    - Màn chi tiết hiện điểm từng bài.
    - Có unit test cho phần tính.
- [ ] **Q-A4 — UC-54 Ghi nhận chi trả** (BE + FE)
  - **BE:**
    - Hàm + endpoint ghi nhận đã chi cho phiếu `royalty_payments` (UNPAID → PAID), ví dụ `POST /api/royalty-payments/{id}/pay`.
    - Khi mọi phiếu của kỳ đã chi xong thì kỳ chuyển sang PAID (theo `SRS_BF04_Royalty.md`).
  - **FE:**
    - Trang `/royalty-payments` của Kế toán (`AccountantLayout.jsx:24`, hiện là ComingSoon).
    - Thêm route sau khi M-D1 đã merge.
  - **Luật role:** `hasRole("ACCOUNTANT")` trong `SecurityConfig`.
  - **Xong khi:** Kế toán bấm chi trả được, kỳ LOCKED chuyển sang PAID trên giao diện.
- [ ] **Q-A5 — UC-57 Nhuận bút của tôi** (BE + FE)
  - **BE:** API danh sách phiếu / dòng nhuận bút của phóng viên đang đăng nhập (lấy id từ `jwt`).
  - **FE:**
    - Trang `/my-royalties` (`ReporterLayout.jsx:40`, hiện là ComingSoon).
    - Chip ví nhuận bút ở layout Reporter lấy số thật, thay TODO ở `api/reporterHomeApi.js:23`.
    - Phần còn lại của trang chủ Reporter là của Minh (M-A6). Báo Minh khi API có.
  - **Xong khi:** phóng viên thấy nhuận bút của mình theo kỳ, chip ví ra số thật.
- [ ] **Q-A6 — Trang chủ Thư ký / Kế toán** (FE)
  - **`SecretaryHome.jsx:65`** đang crash khi `openPeriod` là null. **Làm:** kiểm null, hiện "Chưa có kỳ mở".
  - **`api/accountantHomeApi.js`:** bỏ `giaLapGoiApi`, dùng API royalty thật. Ô nào chưa có API thì bỏ.
  - **`RoyaltyPeriods`:** đọc `?status` (AccountantHome:29 đang gửi).
  - **Xong khi:** hai trang chủ không crash, không có số giả.
- [ ] **Q-A7 — Chạy thử BF-03 và BF-05 trên dữ liệu mới** (BE + FE)
  - BF-03 do Vũ viết, BF-05 do Khang viết, Quang chưa test bao giờ.
  - **BF-03:** đi hết một vòng khiếu nại (UC-35…45): nộp → chấm mù / chấm chéo → kết luận.
  - **BF-05:** các trang KPI đang mặc định năm 2026, tháng 9:
    - `DepartmentKpiDashboard:23-24`, `GenrePopularityReport:19-20`, `PersonalKpiProgress:20-21`, `MonthlyTargetSetting:37-38`, `ReporterKpiDetails:31-32`.
    - **Làm:** đổi mặc định sang tháng có dữ liệu demo hoặc tháng hiện tại. Dropdown năm không được cứng 2025-2026.
  - **Xong khi:** bước 5, 6, 10, 11 của kịch bản demo chạy được, không lỗi console.

## 6. Nhóm B — dọn code (sau 12:00 thứ 3, làm đúng thứ tự)

- [ ] **Q-B1 — Lỗi trả về đúng chuẩn** (BE, file dùng chung)
  - **`GlobalExceptionHandler`:** bắt thêm `IllegalArgumentException` / `IllegalStateException`, trả 409 kèm message. Hiện các lỗi này ra 500 trần (`RoyaltyCalculator:94, 170-185`, `EventService:81-86`, `SystemParameterService:120`, `ScoreCalculator:45-82`).
  - **`ResponseStatusException` ở BF-03:** chuyển sang `NotFoundException` / `BusinessException`. Các file: `AppealAuditQuery`, `AppealManagement`, `AppealReview`, `CrossReview`, `ReviewerDeviation`, `ReviewerDeviationRefresh`, `ReviewerProfile`, `UserRead`, `ScoringAppeal`. Bản ở `ScoringAttemptService` là việc của Minh (M-B1).
  - **Message tiếng Anh:** `ReviewerDeviationRefreshService:68` ("Missing department") → tiếng Việt.
  - **Xong khi:** có test trong `GlobalExceptionHandlerTest`. Lỗi nghiệp vụ trả 409 kèm câu tiếng Việt.
- [ ] **Q-B2 — Luật role trong `SecurityConfig`** (BE, file dùng chung)
  - BF-02 theo danh sách Minh gửi: Assignment, CriteriaSet, ScoreApproval.
  - BF-05: `KpiTarget`, `KpiSnapshot` (hiện không chặn role).
  - `GET /api/users` cho ADMIN (M-A7).
  - **Xong khi:** gọi sai role thì 403. Đăng nhập đúng role vẫn chạy hết kịch bản demo.
- [ ] **Q-B3 — BF-03 theo AGENTS.md** (BE). *Cần M-D2.*
  - **Bỏ tầng JdbcTemplate,** đổi sang gọi hàm đọc của Minh. Các file: `repository/ArticleReadRepository` (SQL thẳng vào bảng BF-01), `repository/ScoringReadRepository` (bảng BF-02, mỗi lần gọi tải hết các lần chấm), `repository/UserReadRepository`, và JdbcTemplate ở `ReviewerDeviationRefreshService:43`.
  - **`UserReadService.requireActive:29`** đang tải hết user để tìm 1 người. **Làm:** dùng `UserService.getUserAccess(id)`.
  - **Bỏ hai cặp interface + Impl,** gộp mỗi cặp thành 1 service thường: `AppealSubmissionDataProvider` / `AppealDatabaseSubmissionDataProvider` và `ReviewerDirectoryProvider` / `ReviewerDatabaseDirectoryProvider`.
  - **`AppealService.java:66`:** bỏ constructor viết tay có `@Autowired`, dùng `@RequiredArgsConstructor`. **`AppealService:52-53`:** so sánh trạng thái bằng enum `ArticleStatus`, không so chuỗi.
  - **Class dữ liệu đặt sai chỗ:** `AppealSubmissionContext`, `AppealSubmissionCriterion`, `ReviewerDirectoryEntry` đang ở `service/`. **Làm:** chuyển sang `dto/`.
  - **Nếu kịp:**
    - `AppealManagementService:88` phân trang trong bộ nhớ → dùng `Pageable`.
    - Viết lại các dòng gộp nhiều câu lệnh (`CrossReviewService:101-103, 117`), mỗi dòng 1 câu.
  - **Để sau thứ 4:** `ScoringAppealService` đang ghi thẳng vào bảng `scoring_attempts` của BF-02 (dòng 140, 169, 202, 220). Muốn đi qua `ScoringAttemptService` thì phải làm cùng Minh.
- [ ] **Q-B4 — BF-05 theo AGENTS.md** (BE). *Cần M-D2.*
  - **Bỏ chỗ gọi repository luồng khác,** đổi sang hàm M-D2 và `RoyaltyGradingService` (của Quang):
    - `KpiAggregationService:56-59` (dùng ở 81, 93, 106, 157, 261, 276, 291)
    - `GenreReportService:42-46` (dùng ở 57, 70, 110, 175)
    - `CsvImportService:52` (dùng ở 173)
  - **Tách hàm dài:** `GenreReportService:55` (180 dòng), `CsvImportService:61` (172), `KpiAggregationService:258` (133), `:153` (98), `:70` (78).
  - **Service trả entity ra controller:** `KpiAggregationService:70` trả entity, `KpiSnapshotController:7, 73-77` tự map. **Làm:** service trả DTO.
  - **Validate:** `KpiTargetController:63` nhận body `List` nhưng thiếu `@Valid`. **Làm:** thêm `@Valid`.
  - **Code lặp và bắt lỗi quá rộng:**
    - Kiểm role viết 2 lần ở `KpiAggregationService:84, 160` → gộp 1 hàm.
    - `catch (Exception)` ở `CsvImportService:80, 245` → bắt đúng loại lỗi.
  - **Số ma:** `GenreReportCalculator:25-26, 84-100` có giá trị mặc định viết cứng → đặt thành hằng số có tên.
  - **Gộp URL:** `/api/reports` và `/api/kpi-reports` thành 1 tiền tố. Sửa luôn FE.
- [ ] **Q-B5 — BF-04 gọn lại** (BE). *Cần M-D2.*
  - **Hàm đọc tham số hệ thống viết riêng:** `EventService:78`, `RoyaltyCalculator:177`, `RoyaltyGradingService:504`. **Làm:** đổi sang `SystemParameterService.getIntValue` / `getDecimalValue`.
  - **`RoyaltyPeriodService:376 loadUserNames()`:** đổi sang `UserService.getUserNames`.
  - **`RoyaltyGradingService:104, 363`** dùng `MediaFactorRepository`. **Làm:** đổi sang `MediaFactorService`.
  - **Comment cũ:** ở `RoyaltyPeriodRepository:40` ghi RoyaltyLine chưa có. **Làm:** xoá.
- [ ] **Q-B6 — Frontend BF-03 / BF-05** (FE + file dùng chung)
  - **`index.css`:**
    - Thêm các biến đang thiếu `--fm-primary`, `--fm-border`, `--fm-text-primary`, `--fm-text-secondary`. CSS KPI đang dùng 4 biến này nên luôn rơi về mã hex dự phòng.
    - Xoá block LOGIN (trùng `styles/Login.css`) và block MAIN (không dùng).
    - Chuyển block KPI sang file CSS riêng của `KpiBenchmarksConfig` / `MonthlyTargetSetting`.
  - **Bỏ mã màu hex** ở CSS: `ReaderDataImport.css` (75), `ReporterKpiDetails.css` (44), `DepartmentKpiDashboard.css` (43), `PersonalKpiProgress.css` (38), `GenrePopularityReport.css` (6). Và trong JSX của `ReaderDataImport`, `GenrePopularityReport`.
  - **Dùng component chung** (*sau M-B3*): `Modal` ở `AppealRuleConfigPage`, `MonthlyTargetSetting`; `Pagination` ở `AppealManagementPage`, `ReviewerProfilePage`, `ReaderDataImport`; ngày tháng dùng `utils/formatDate` (bỏ `appeal/appealFormat.js:10`, `ReaderDataImport:130`).
  - **`api/readerImportApi.js:42`** đang `fetch` thẳng để tải mẫu CSV. **Làm:** gọi qua `goiApi`. Nếu `goiApi.js` chưa hỗ trợ tải file thì thêm 1 tuỳ chọn nhỏ.
  - **CSS đặt sai chỗ:** file CSS của appeal / royalty / kpi trong `src/styles/`. **Làm:** chuyển ra cạnh component.
- [ ] **Q-B7 — `@ManyToOne` cho entity BF-03 / BF-04 / BF-05. Rủi ro cao nhất, làm cuối.**
  - Chỉ bắt đầu trước 20:00 thứ 3. Mỗi entity 1 PR. Server phải khởi động được (`validate`) và `mvn test` xanh.
  - **Danh sách field:**
    - **BF-03:** `Appeal:33-36`, `AppealCriterion:38`, `AppealResolution:21-25`, `AppealRuleConfig:41`, `CrossReviewTask:21-24`, `CrossReviewBatch:25`, `ReviewerDeviationIndex:35`.
    - **BF-04:** `RoyaltyPeriod:44`, `RoyaltyRate:41-42`, `RoyaltyGrading:40-45`, `RoyaltyMediaItem:43`, `RoyaltyPayment:47, 57`, `RoyaltyLine:65`, `Event:50`.
    - **BF-05:** `KpiTarget:32-33, 46-47`, `KpiSnapshot:38, 57`, `CsvImportLog:40`, `ReaderInteraction:33-34, 44-45` (TODO ở dòng 44 đã cũ).
  - **Quyết định của nhóm trưởng:** `RoyaltyGrading:40-41` và `AuditLog:34` cố ý để `Integer`, vì luật cũ "không đụng luồng khác". Giờ chỉ còn 2 người, nên đổi luôn là hợp lý. Ghi quyết định vào AGENTS.md.

## 7. Việc của nhóm trưởng

- [ ] **Q-L1 — Merge PR kế hoạch tối thứ 2.** PR gồm 2 file kế hoạch + bảng §2 mới trong `AGENTS.md`. Nhắn Minh `git pull origin main` rồi đọc `docs/KE_HOACH_THU4_MINH.md`.
- [ ] **Q-L2 — Duyệt PR của Minh trong khoảng 1 giờ.** Kiểm theo checklist mục 8. Gấp thì Minh nhắn mã việc.
- [ ] **Q-L3 — Nhận yêu cầu sửa file dùng chung:** luật role, dữ liệu, biến màu, `goiApi`. Mỗi lần làm 1 PR nhỏ, hoặc gộp vào PR đang mở.
- [ ] **Q-L4 — Tổng duyệt lúc 12:00 và 22:00 thứ 3.**
  - Xoá DB, chạy `schema.sql` + `data.sql`.
  - `mvn -f server/pom.xml spring-boot:run` và `npm run dev`.
  - Cùng Minh đi hết mục 9 với từng tài khoản. Lỗi nào thì ghi mã việc mới (Q-Fx / M-Fx) vào file của người sửa.
- [ ] **Q-L5 — Quyết định có nới AGENTS.md §8.2 hay không.** Hiện là "AI làm 1 hàm/lượt rồi chờ"; đề xuất đổi thành "1 mục trong file kế hoạch/lượt" để kịp tiến độ. Quyết xong ghi vào AGENTS.md.
- [ ] **Q-L6 — Sau thứ 4:** gom các mục B chưa xong của cả hai thành kế hoạch tiếp theo.

## 8. Quy tắc PR / test (áp dụng mọi việc)

- **Nhánh:**
  - Mỗi việc là 1 nhánh `feature/<luồng>-<việc>` từ `main`, ví dụ `feature/bf04-uc54-payment`.
  - `git pull origin main` trước khi tạo nhánh.
  - AI không tự tạo nhánh hay commit khi chưa được bảo.
- **Commit:** `[BF-03] ...`, `[BF-04] ...`, `[BF-05] ...`, việc chung ghi `[COMMON]`, sửa DB ghi `[DB]`. PR có đổi `schema.sql` thì đầu mô tả ghi **"CÓ ĐỔI DB"**.
- **Trước khi mở PR:**
  - [ ] `mvn -f server/pom.xml test` xanh
  - [ ] `npm run build` trong `client/` qua
  - [ ] Server khởi động được (không lỗi `validate`)
  - [ ] Bấm thử đúng màn hình đã sửa
- **Duyệt:** Minh duyệt PR của Quang, Quang duyệt PR của Minh. PR nhỏ, merge sớm để ít conflict.

## 9. Kịch bản demo (bài test cuối)

Tài khoản demo (điền ở Q-A1, mật khẩu `123456`):

| Role | Tài khoản |
|---|---|
| Phóng viên | … |
| Biên tập viên | … |
| Trưởng ban | … |
| Thư ký | … |
| Kế toán | … |
| Admin | … |

1. **Phóng viên:** trang chủ có số thật → tạo bài → nộp. *(Minh)*
2. **Trưởng ban:** phân công BTV (danh sách thật). *(Minh)*
3. **BTV:** chấm phiếu. *(Minh)*
4. **Trưởng ban:** duyệt điểm → công bố. *(Minh)*
5. **Phóng viên:** xem điểm → khiếu nại trong hạn. *(Quang Q-A7, Minh M-A5)*
6. **Chấm lại** (mù / chéo) → kết luận khiếu nại. *(Quang Q-A7)*
7. **Trưởng ban:** duyệt đăng. Thấy nội dung bài và điểm sau khiếu nại. *(Minh M-A1, M-A2)*
8. **Thư ký:** chấm nhuận bút, dùng điểm sau khiếu nại. *(Quang Q-A2)*
9. **Kế toán:** duyệt / khoá kỳ → ghi nhận chi trả → kỳ sang PAID. *(Quang Q-A4)*
10. **Phóng viên:** Nhuận bút của tôi + chip ví + KPI cá nhân có điểm chất lượng. *(Quang Q-A3, Q-A5)*
11. **Trưởng ban:** KPI phòng, báo cáo thể loại. *(Quang Q-A3, Q-A7)*
12. **Admin:** tham số, tiêu chí, người dùng, thể loại. *(Minh M-A7)*

Không menu nào mở ra ComingSoon. `grep -rn giaLapGoiApi client/src/api` không còn ở các file trang chủ.

## 10. Gửi AI của Quang (dán vào đầu mỗi phiên)

> Tôi là Quang, nhóm trưởng, phụ trách BF-03 `appeal`, BF-04 `royalty`, BF-05 `kpi` (đã nhận lại từ Vũ và Khang). Việc của tôi nằm trong `docs/KE_HOACH_THU4_QUANG.md`. Phiên này làm việc **<mã việc, ví dụ Q-D1>**.
> Trước khi sửa code:
> - Làm Bước 0 của AGENTS.md.
> - Đọc thêm SRS của luồng: `docs/SRS_BF03_Appeal.md`, `docs/SRS_BF04_Royalty.md` hoặc `docs/SRS_BF05_KPI.md`.
> - Muốn hiểu code BF-03 có sẵn thì xem `docs/BF03_FLOW_AND_METHOD_GUIDE.md`; BF-05 thì xem `docs/BF05-Kich-ban-review.md`.
> - Không sửa file Minh giữ (mục 1). Cần thì dừng lại và nói tôi nhắn Minh.
> - Liệt kê các bước nhỏ, làm từng bước, chạy test/build, rồi giải thích ngắn bằng tiếng Việt.
> - Xong thì đánh dấu `[x]` trong file kế hoạch.
