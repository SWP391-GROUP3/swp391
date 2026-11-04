# BF-03 — Bài học: từ màn hình đến code, theo Iter

> Tài liệu dạng bài giảng để bạn học lại luồng mình làm và trả lời giáo viên. Tên file, hàm và route đều lấy từ source trên nhánh `VuNQ`.
> Đọc kèm: [BF03_REVIEW_AND_FLOW.md](BF03_REVIEW_AND_FLOW.md) (bảng nút → bảng dữ liệu, mục 7), [BF03_ITER1_CODE_GUIDE.md](BF03_ITER1_CODE_GUIDE.md), [BF03_ITER3_CODE_GUIDE.md](BF03_ITER3_CODE_GUIDE.md).

---

## Bài 0 — Bức tranh chung

**Thầy hỏi:** Một lần bấm nút đi qua những tầng nào?

**Em trả lời:** Từ trên xuống dưới:

```text
Màn React (pages/appeal/*.jsx)
  → hàm trong src/api/*.js (appealApi.js, crossReviewApi.js, ...)
  → goiApi.js (gắn Bearer token, đổi lỗi sang tiếng Việt)
  → Controller (nhận request, lấy userId từ JWT, KHÔNG có logic)
  → Service (kiểm tra quyền, trạng thái, tính điểm, ghi audit)
  → Repository (JPA: entity; JdbcTemplate: đọc dữ liệu luồng khác)
  → SQL Server (database swp391_kpi)
  → DTO trong ApiResponse → React cập nhật màn
```

**Thầy hỏi:** Vì sao Controller không được có `if`?

**Em trả lời:** Để quy tắc nghiệp vụ nằm ở một chỗ duy nhất là Service. Nếu Controller có `if`, mỗi API sẽ kiểm tra khác nhau và khó test. Controller chỉ làm ba việc: nhận dữ liệu, gọi một hàm Service, trả `ApiResponse`.

**Thầy hỏi:** Người thao tác được xác định thế nào?

**Em trả lời:** Từ token JWT. Controller lấy `jwt.getSubject()` rồi truyền xuống Service. Client không gửi `userId` để tự xưng danh. Service gọi `UserReadService.requireActive(userId, "ROLE")` để kiểm tra tài khoản đang ACTIVE và đúng vai trò.

**Thầy hỏi:** Vì sao phải có Iter?

**Em trả lời:** Mỗi Iter là một lát cắt chức năng để nhóm nghiệm thu từng phần. Iter1 làm các UC độc lập. Iter2 làm phân công và hồ sơ BTV, nền cho Iter3. Iter3 làm quy trình chấm mù, so sánh, chốt điểm và chấm chéo.

---

## Bài 1 — Iter1: gửi khiếu nại, cấu hình luật, độ lệch, nhật ký

Iter1 gồm **UC-35, UC-41, UC-44, UC-45**.

### 1.1 UC-35: Phóng viên gửi khiếu nại

| Màn (route) | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| Menu *Khiếu nại điểm* → `/my-appeals` (`MyAppealsPage.jsx`) | Mở trang | `layKhieuNaiCuaToi()` | `GET /api/appeals/mine` | `AppealController.getMyAppeals` → `AppealService.getMyAppeals` |
| *Gửi khiếu nại* → `/my-appeals/new` (`AppealCreatePage.jsx`) | Mở trang | `layBaiCoTheKhieuNai()` | `GET /api/appeals/eligible-articles` | `getEligibleArticles` → `getEligibility` cho từng bài |
| Chọn bài, *Xem kết quả chấm* | Đổi `?articleId=` | `layDieuKienKhieuNai()` | `GET /api/articles/{id}/appeal-eligibility` | `getEligibility` |
| *Gửi khiếu nại* | Nộp form | `guiKhieuNai()` | `POST /api/appeals` | `submitAppeal` |

**Backend của UC-35 (đọc theo thứ tự):**
1. `AppealService.submitAppeal()` gọi `UserReadService.requireActive(id, "REPORTER")`.
2. Lấy ngữ cảnh bài qua `AppealSubmissionDataProvider` → `AppealDatabaseSubmissionDataProvider`, đọc `articles`, `scoring_attempts` + `score_approvals`, `scoring_attempt_details`.
3. Lấy luật đang hiệu lực: `AppealRuleConfigService.getActiveConfigEntity()`.
4. `validateSubmission()` → `validateArticleAndWindow()` (đúng tác giả chính, trạng thái, `now ≤ announced_at + appeal_window_days`) → `validateCriteria()` (tiêu chí thuộc phiếu gốc, có lý do).
5. Lưu `appeals` (trạng thái SUBMITTED, gắn `rule_config_id`), lưu `appeal_criteria` qua `saveCriteria()`, ghi `audit_logs` qua `AuditService.log()`, báo Trưởng ban qua `NotificationService.notifyHeads()`.

Tất cả nằm trong một `@Transactional`: lỗi ở bước nào cũng rollback.

**Thầy hỏi:** Vì sao vừa kiểm tra trùng trong Service vừa có `UNIQUE(appeals.article_id)`?

**Em trả lời:** Service kiểm tra để trả câu thông báo tiếng Việt đúng. `UNIQUE` là lưới an toàn khi hai request chạy cùng lúc. Vì vậy `submitAppeal()` còn bắt `DataIntegrityViolationException` và trả đúng câu UC-35 E2.

**Thầy hỏi:** Vì sao `AppealService` có hai constructor?

**Em trả lời:** Constructor có tham số `Clock` dùng cho test để cố định thời gian. Hạn khiếu nại phụ thuộc thời gian nên test phải kiểm soát được. Constructor thường dùng `Clock.systemDefaultZone()`.

### 1.2 UC-44: Admin cấu hình luật

| Màn | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| *Cấu hình khiếu nại* → `/appeal-rules` (`admin/AppealRuleConfigPage.jsx`) | Mở trang | `getActiveConfig()`, `getAllConfigs()` | `GET /api/appeal-rule-configs/active` và `GET /api/appeal-rule-configs` | `AppealRuleConfigController` → `AppealRuleConfigService` |
| *+ Tạo phiên bản mới* → *Lưu* | Gửi form | `createConfig()` | `POST /api/appeal-rule-configs` | `createConfig` |

**Ý chính:** `createConfig()` luôn **thêm dòng mới**, không sửa dòng cũ, và từ chối trùng `effective_from`. Khiếu nại đã gửi giữ `rule_config_id` cũ nên không bị ảnh hưởng (BR-QC-12).

**Thầy hỏi:** Vì sao không UPDATE luật cho gọn?

**Em trả lời:** Vì khiếu nại đang xử lý phải dùng đúng luật lúc gửi. Nếu sửa dòng cũ, kết quả của những khiếu nại trước sẽ bị đổi ngầm. Lịch sử cấu hình cũng mất.

### 1.3 UC-41: Báo cáo độ lệch BTV

| Màn | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| *Độ lệch & hồ sơ BTV* → `/reviewers` (`ReviewerDeviationPage.jsx`) | Mở trang / *Lọc báo cáo* | `layBaoCaoDoLech()` | `GET /api/reports/reviewer-deviations` | `ReviewerDeviationController` → `ReviewerDeviationService.getReport` |

**Ghi chú về Iter1 và Iter3:** ở Iter1, GET báo cáo tự tính lại và khoá toàn hệ thống. Sau này (#3 trong tài liệu review) nhóm đổi thành **chỉ đọc**; tính lại có endpoint riêng `POST /api/reports/reviewer-deviations/refresh` (nút *Tính lại chỉ số*).

**Công thức (`ReviewerDeviationCalculator.calculate`):**
- `avg_deviation` = trung bình |độ lệch| (trị tuyệt đối).
- `overturn_rate` = số khiếu nại có điểm hiệu lực ≠ S0 / số khiếu nại đã giải quyết × 100; mẫu số 0 thì bằng 0.
- `workload_count` = số phiếu không phải APPEAL đã nộp trong tháng.

**Cách tính dùng chung:** `ReviewerDeviationRefreshService.computeIndexes()`. Cả báo cáo lẫn hồ sơ BTV (`ReviewerProfileService`) đều gọi hàm này, nên hai nơi luôn cho cùng một con số.

**Thầy hỏi:** Vì sao báo cáo được tính từ `cross_review_tasks` và `appeal_resolutions` chứ không từ một bảng riêng?

**Em trả lời:** Đây là dữ liệu nguồn. Độ lệch không được lưu rồi sửa tay, mà tính từ dữ liệu gốc mỗi lần làm mới. Bảng `reviewer_deviation_indices` chỉ là bản lưu của kết quả đó.

### 1.4 UC-45: Nhật ký

| Màn | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| *Nhật ký chấm* / *Nhật ký hệ thống* → `/audit-logs` (`AuditLogPage.jsx`) | *Lọc nhật ký* | `layNhatKy()` | `GET /api/audit-logs` | `AuditLogController` → `AppealAuditQueryService.getLogs` |

**Hai đường khác nhau:**
- **Ghi:** `AuditService.log()`, gọi từ mọi nghiệp vụ. Chỉ `INSERT`.
- **Đọc:** `AppealAuditQueryService.getLogs()`, kiểm tra quyền rồi mới trả về.

**Thầy hỏi:** Vì sao Trưởng ban không được lọc nhật ký ban khác?

**Em trả lời:** `getLogs` kiểm tra `cannotView(role, sectionId, articleId)`. Trưởng ban chỉ xem log của bài trong ban mình. Admin xem tất cả. Không có API nào sửa hoặc xoá audit.

---

## Bài 2 — Iter2: phân công tối thiểu, giao lại, hồ sơ BTV

Iter2 gồm **UC-42, UC-43** và phần phân công tối thiểu của **UC-38** làm nền cho Iter3.

### 2.1 UC-38 (phần phân công): Trưởng ban nhận và phân công

| Màn | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| *Khiếu nại* → `/appeals` (`AppealManagementPage.jsx`) | Mở, lọc, đổi trang | `layHangChoKhieuNai()` | `GET /api/appeals?…` | `AppealManagementController.getQueue` → `AppealManagementService.getQueue` |
| Bấm một khiếu nại → `/appeals/:id` (`AppealDetailPage.jsx`) | Mở trang | `layChiTietKhieuNai()`, `layNguoiChamHopLe()` | `GET /api/appeals/{id}`, `GET …/reviewer-candidates` | `getDetail`, `getCandidates` |
| *Phân công* | Chọn BTV, bấm nút | `phanCongKhieuNai()` | `POST /api/appeals/{id}/assign` | `assign` → `ScoringAppealService.createDraft()` |

**Ý chính của `assign()`:**
1. `scopedAppeal(id, userId, true)`: chỉ Trưởng ban đúng ban của bài. Dùng khoá để không ai chen vào giữa.
2. Chỉ khi khiếu nại đang SUBMITTED.
3. `requireCandidate()`: người được chọn phải nằm trong `candidates()`.
4. `createDraft()` tạo một dòng `scoring_attempts` loại **APPEAL**, sao chép phiên bản bài, thể loại, bộ tiêu chí và phân công từ S0. **Phiếu mới chưa có điểm.**
5. Đổi `appeals` sang IN_REVIEW, ghi `audit_logs` (ASSIGN), thông báo BTV.

**Thầy hỏi:** Vì sao phân công không tính điểm?

**Em trả lời:** Phân công là giao việc, không phải chấm. Điểm chỉ được ghi khi BTV nộp phiếu ở Iter3. Vì vậy phiếu mới có `total_score = NULL` và `submitted_at = NULL`.

**Thầy hỏi:** `candidates()` loại những ai?

**Em trả lời:** Loại người không ACTIVE, không phải EDITOR, người đã chấm S0, người đã chấm bất kỳ lần nào của khiếu nại này, và người đã từng được giao rồi bị giao lại (đọc từ audit). Còn BTV cùng ban được ưu tiên; chỉ khi hết mới liệt kê khác ban, và khi đó bắt buộc có lý do.

### 2.2 UC-43: Giao lại phiếu chưa nộp

| Màn | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| `/appeals/:id` | *Giao lại* | `giaoLaiKhieuNai()` | `POST /api/appeals/{id}/reassign` | `reassign` → `ScoringAppealService.reassignDraft()` |

**Ý chính:**
- Request gửi `attemptId` và `expectedReviewerId`. Nếu phân công đã đổi trước đó, server báo lỗi để client tải lại. Đây là kiểm tra **phiên bản dữ liệu**.
- `reassignDraft()` lưu nháp cũ vào audit (`old_value`), xoá điểm nháp, đổi người. **Số vòng S1/S2 không tăng.**
- Phiếu đã nộp không được giao lại (409).

### 2.3 UC-42: Hồ sơ BTV

| Màn | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| Bấm tên BTV → `/reviewers/:id` (`ReviewerProfilePage.jsx`) | Mở, đổi tháng, đổi trang | `layHoSoBienTapVien()`, `layLichSuCham()` | `GET /api/reviewers/{id}`, `GET …/scoring-history` | `ReviewerProfileController` → `ReviewerProfileService` |

- `requireEditor()` kiểm tra Trưởng ban có được xem BTV này không.
- Hồ sơ **không ghi** bảng chỉ số. Độ lệch lấy từ `computeIndexes()` (Bài 1.3).

**Thầy hỏi:** Hồ sơ BTV có làm mất bộ lọc khi quay lại báo cáo không?

**Em trả lời:** Có xử lý. Trang truyền `location.state.boLoc` khi chuyển trang. Iter2 đã sửa lỗi trước đó làm mất state khi đổi tháng hoặc trang.

---

## Bài 3 — Iter3: chấm mù, so sánh, chốt điểm, chấm chéo

Iter3 gồm **UC-36, UC-37, UC-39, UC-40** và hoàn thiện **UC-38**. Có thay đổi DB: chạy `database/migrations/BF03_iter3_safe.sql`, không chạy lại `schema.sql`.

### 3.1 UC-36: BTV chấm mù S1

| Màn | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| *Chấm lại & chấm chéo* → `/re-reviews` (`BlindReviewQueuePage.jsx`) | Mở trang | `layPhieuChamMu()`, `layPhieuChamCheo()` | `GET /api/appeal-reviews`, `GET /api/cross-review-tasks` | `AppealReviewController.queue` → `getQueue` |
| Mở phiếu → `/re-reviews/:id` (`BlindReviewPage.jsx`) | Mở trang | `layChiTietChamMu()` | `GET /api/appeal-reviews/{id}` | `card` → `AppealReviewService.getCard` → `ScoringAppealService.getOwnedScorecard` |
| *Lưu nháp* | Bấm nút | `luuNhapChamMu()` | `PUT /api/appeal-reviews/{id}` | `draft` → `save(..., false)` |
| *Nộp phiếu chấm* | Bấm nút | `nopPhieuChamMu()` | `POST /api/appeal-reviews/{id}/submit` | `submit` → `save(..., true)` |

**Tại sao gọi là chấm mù?** `ScoringAppealService.getComparisonCard()` chỉ trả điểm và nhận xét của **chính phiếu đó**. Không có điểm S0, không có tên người chấm cũ, không có tiêu chí bị khiếu nại. Việc này làm ở **response backend**, không phải ẩn bằng CSS.

**Ý chính của `save()`:**
1. `deviations.lockWorkflow()`: khoá ứng dụng để các thao tác ghi không chạy song song.
2. `lockedAppeal()` trước rồi mới khoá phiếu: thứ tự khoá cố định để tránh deadlock.
3. Khiếu nại phải đang IN_REVIEW.
4. `saveScorecard()`:
   - kiểm tra đúng chủ phiếu và phiếu chưa nộp;
   - ghép tiêu chí của request với bộ tiêu chí đã gắn, từ chối tiêu chí lạ hoặc trùng;
   - `AppealScoreCalculator.weightedTotal(items, submit)` tính tổng;
   - thay toàn bộ `scoring_attempt_details`;
   - nếu nộp thì đặt `submitted_at`, ghi audit SUBMIT_SCORE.
5. Nếu nộp: thông báo Trưởng ban và ghi audit COMPARE kèm đề xuất.

**Thầy hỏi:** Vì sao server phải tính lại tổng, khi frontend đã tính `tinhTongTam()`?

**Em trả lời:** Vì client có thể bị sửa. Frontend chỉ hiện tổng để người chấm xem trước. Tổng được lưu là tổng server tính.

**Thầy hỏi:** Vì sao cần `PESSIMISTIC_WRITE`?

**Em trả lời:** Nếu Trưởng ban giao lại trong lúc BTV đang nộp, hai request có thể cùng sửa một phiếu. Khoá dòng `UPDLOCK` bắt request sau chờ. Iter3 có ca kiểm thử để chứng minh: entity đã được cache trước khi khoá thì phải `refresh` lại dưới khoá (`lockedDraft()`).

### 3.2 UC-37, UC-39: so sánh và chốt điểm

| Màn | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| So sánh & chốt → `/appeals/:id/comparison` (`AppealComparisonPage.jsx`) | Mở trang | `laySoSanhKhieuNai()` | `GET /api/appeals/{id}/comparison` | `AppealReviewController.comparison` → `AppealReviewService.getComparison` → `comparison()` |
| *Phân công S2* | Chọn BTV | `phanCongLanHai()` | `POST /api/appeals/{id}/assign-second` | `AppealManagementService.assignSecond` → `createSecondDraft()` |
| *Xác nhận điểm hiệu lực* | Bấm nút | `chotDiemKhieuNai()` | `POST /api/appeals/{id}/finalize` | `finalizeAppeal` |

**Công thức nằm ở `AppealScoreCalculator.compare()`:**

| Điều kiện | Kết quả | Mã luật |
|---|---|---|
| \|S1 − S0\| ≤ T | S1 | `USE_S1` |
| vượt T, chưa có S2 | chờ S2, chưa chốt | — |
| có S2 | trung vị của S0, S1, S2 = S2 kẹp vào `min(S0,S1)`..`max(S0,S1)` | `MEDIAN_S0_S1_S2` |

**Ví dụ kiểm chứng:** S0 = 70, S1 = 90, S2 = 40, T = 5 → \|90 − 70\| = 20 > 5 nên cần S2; sắp xếp 40, 70, 90 → trung vị **70**. Dữ liệu mẫu trong DB (`database/seeds/BF03_sample_appeals.sql`): khiếu nại bài [BF03-DEMO-03] có S0 = 82, S1 = 98, S2 = 90 → trung vị 90; bài [BF03-DEMO-02] có S0 = 77, S1 = 95 đang chờ S2, màn so sánh hiện khoảng 77–95.

**Thầy hỏi:** `T` lấy ở đâu?

**Em trả lời:** Từ `appeal_rule_configs` qua `appeals.rule_config_id`, là luật lúc gửi khiếu nại. Không lấy cấu hình mới nhất. Đây là BR-QC-12.

**Thầy hỏi:** Vì sao client không gửi điểm cuối?

**Em trả lời:** `finalizeAppeal()` chỉ nhận `expectedAttemptId`. Server tự tính lại và kiểm tra phiếu cuối đã nộp. Nếu client gửi điểm, Trưởng ban có thể chọn điểm tuỳ ý.

`finalizeAppeal()` ghi `appeal_resolutions`, đổi khiếu nại sang RESOLVED, ghi audit FINALIZE với `downstreamStatus=PENDING_INTEGRATION`, rồi cập nhật chỉ số độ lệch.

**Thầy hỏi:** Vì sao audit ghi `PENDING_INTEGRATION`?

**Em trả lời:** Vì BF-04 và BF-05 chưa tự tính lại tiền và KPI. Em không được nói là đã đồng bộ tự động. Đây là giới hạn cần nói đúng khi trình bày.

### 3.3 UC-40: chấm chéo phân tích

| Màn | Bấm gì | Hàm React | API | Controller → Service |
|---|---|---|---|---|
| *Đợt chấm chéo* → `/cross-reviews` (`CrossReviewBatchPage.jsx`) | Tìm bài, tạo đợt | `layBaiChamCheoHopLe()`, `taoDotChamCheo()` | `GET /api/cross-review-batches/candidates`, `POST /api/cross-review-batches` | `CrossReviewController` → `CrossReviewService.getCandidates` / `create` |
| Chi tiết đợt → `/cross-reviews/:id` (`CrossReviewBatchDetailPage.jsx`) | Gỡ bài, đóng đợt | `goBaiChamCheo()`, `dongDotChamCheo()` | `DELETE …/tasks/{taskId}`, `POST …/close` | `removeTask`, `close` |
| BTV mở task → `/cross-review-tasks/:id` (`BlindReviewPage` với `cheo`) | Nộp | `nopPhieuChamCheo()` | `POST /api/cross-review-tasks/{id}/submit` | `submit` |

**Ý chính:**
- `create()` khoá tên đợt bằng `WITH (UPDLOCK, HOLDLOCK)` để không tạo trùng tên.
- `submit()` lưu `score` và `deviation = score − điểm gốc` vào `cross_review_tasks`. **Không ghi** `scoring_attempts` hay `appeal_resolutions`. Kết quả chấm chéo chỉ phục vụ phân tích.
- `close()` chỉ đóng khi không còn task PENDING.

### 3.4 Điểm hiệu lực cho người khác dùng

`AppealEffectiveScoreService.getEffectiveScore()`:
- chưa RESOLVED: dùng điểm đã công bố;
- RESOLVED: dùng `appeal_resolutions.effective_score`; dữ liệu cũ không có cột này thì lấy tổng của `effective_attempt_id`.

BF-04 và BF-05 chỉ gọi hàm này. Chưa có hợp đồng tự đồng bộ tiền và KPI.

---

## Bài 4 — Câu hỏi thầy hay hỏi (đáp án mẫu)

| Thầy hỏi | Em trả lời ngắn |
|---|---|
| Vì sao không cho Phóng viên gửi khiếu nại cho đồng tác giả? | BR-QC-01: chỉ tác giả chính `articles.author_id`, vì một người đứng tên khiếu nại thì không được chia lượt. |
| Vì sao điểm mới có thể tăng hoặc giảm? | Quy tắc S2 không có sàn S0 (BR-QC-07). Điểm không được trung vị chọn chỉ là "lệch so với nhóm", không tự kết luận vi phạm. |
| Vì sao có `rule_config_id` trong `appeals`? | BR-QC-12: khiếu nại giữ luật lúc gửi, cấu hình mới không làm đổi kết quả đang xử lý. |
| Vì sao audit không có API sửa/xoá? | BR-QC-09: nhật ký bất biến. Đây là ràng buộc nghiệp vụ, không chỉ là chọn không làm. |
| Vì sao cần `lockWorkflow()`? | Các thao tác ghi BF-03 không được chạy song song. Khoá ứng dụng `BF03_SCORE_WRITES` có thời hạn 15 giây. |
| Editor chấm mù có thấy điểm cũ không? | Không. Response không có điểm S0, S1 của người khác, và `BlindReviewPage` không nhận các trường đó. |
| Chấm chéo có đổi điểm chính thức không? | Không. Chỉ lưu độ lệch để phân tích (BR-QC-08). |
| Giới hạn đã biết? | `max_appeals_per_article` trong cấu hình luôn bị bỏ qua vì `UNIQUE(appeals.article_id)` chỉ cho một lần. Cần nhóm quyết. |

---

## Bài 5 — Tự kiểm tra

Đọc theo đúng thứ tự, mỗi file tự trả lời được một câu:

1. `routes/AppRoutes.jsx`: `/my-appeals`, `/appeals/:id/comparison`, `/re-reviews/:id`, `/cross-reviews` gắn với component nào?
2. `layouts/ReporterLayout.jsx`, `HeadLayout.jsx`, `EditorLayout.jsx`: menu nào dẫn tới route nào?
3. `api/goiApi.js`: gắn token ở đâu, đổi lỗi thế nào?
4. `controller/AppealController.java`: vì sao không có `if` nghiệp vụ?
5. `service/AppealService.java` → `validateArticleAndWindow()`: đúng những điều kiện nào?
6. `service/ScoringAppealService.java` → `saveScorecard()`: vì sao tổng do server tính?
7. `service/AppealScoreCalculator.java` → `compare()`: vẽ bảng quyết định từ trí nhớ.
8. `database/schema.sql`: `ck_scoring_attempts_appeal` nói gì về `appeal_id`?

Nếu trả lời được các câu trên, em đã nắm luồng BF-03.

---

**Ghi chú trung thực:** tài liệu này lấy từ source và các file hướng dẫn có sẵn trên `VuNQ`. Tôi chưa chạy lại từng màn trong trình duyệt khi viết bài này. Các đường dẫn và tên hàm đã được kiểm tra có tồn tại trong source.
