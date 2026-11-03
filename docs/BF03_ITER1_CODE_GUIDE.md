# BF-03 — Giải thích UC và source code Iter1

Ngày đối chiếu source: 14/10/2026.

Tài liệu này giải thích **code đang có trong workspace**, phục vụ đọc code, demo và bảo vệ bài. Đây là tài liệu hướng dẫn bổ sung; yêu cầu nghiệp vụ tham chiếu `docs/SRS_BF03_Appeal.md`, cấu trúc dữ liệu tham chiếu `database/schema.sql`.

## 1. Iter1 gồm những gì?

| UC | Tên chức năng | Người sử dụng | Kết quả |
|---|---|---|---|
| UC-35 | Submit Quality Appeal — Gửi và theo dõi khiếu nại điểm | Phóng viên là tác giả chính | Lưu khiếu nại, tiêu chí và lý do; thông báo Trưởng ban; ghi audit |
| UC-41 | Analyze Reviewer Score Deviations — Phân tích độ lệch điểm | Trưởng ban; bản triển khai hiện tại cũng cho Admin xem | Tính và xem chỉ số chấm điểm theo BTV và tháng |
| UC-44 | Configure Appeal Rules & Scoring Parameters — Cấu hình luật khiếu nại | Admin | Tạo phiên bản cấu hình có ngày hiệu lực, giữ lịch sử |
| UC-45 | Audit System & Grading Logs — Xem nhật ký hệ thống và chấm điểm | Trưởng ban, Admin | Tra cứu các sự kiện được phép xem, không có API sửa/xóa nhật ký |

**Phân biệt Iter1 với toàn bộ BF-03:** Iter1 chưa triển khai màn phân công lại UC-43, hồ sơ BTV UC-42, quy trình chấm mù UC-36/37, quản lý khiếu nại UC-38, so sánh/chốt điểm UC-39 hay tạo đợt chấm chéo UC-40. UC-41 đã có thể đọc kết quả chấm chéo/khiếu nại đã giải quyết khi các nguồn đó tồn tại.

Luật chính áp dụng: BR-QC-01/02 về điều kiện khiếu nại; BR-QC-08 về chỉ số phân tích; BR-QC-09 về audit; BR-QC-12 về cấu hình theo phiên bản. Việc cấm Editor xem audit hỗ trợ bảo vệ dữ liệu chấm mù của BR-QC-03.

## 2. Cách đọc cấu trúc chương trình

Backend đặt trong `server/src/main/java/com/swp391/kpi/`. Frontend đặt trong `client/src/`.

```text
Trang React
    → file gọi API trong client/src/api/
    → Controller nhận HTTP request
    → Service kiểm tra quyền và xử lý nghiệp vụ
    → Repository đọc/ghi database
    → DTO đóng gói kết quả
    → ApiResponse trả về React
```

### 2.1. Vai trò từng tầng

| Thành phần | Nhiệm vụ trong Iter1 |
|---|---|
| Controller | Nhận path, query, header và request body; dùng `@Valid`; gọi service; trả mã HTTP |
| Service | Kiểm tra người dùng, điều kiện nghiệp vụ, phạm vi ban; tính toán và quản lý transaction |
| Repository JPA | Truy vấn/lưu các entity khiếu nại, cấu hình, chỉ số, audit và thông báo |
| Repository đọc bằng JDBC | Đọc dữ liệu người dùng, bài viết, phiếu chấm qua SQL có tham số |
| Entity | Ánh xạ các bảng SQL Server sang đối tượng Java |
| DTO | Chỉ chứa dữ liệu cần nhận/trả; không trả thẳng entity JPA cho trình duyệt |

Các `UserReadRepository`, `ArticleReadRepository`, `ScoringReadRepository` hiện dùng `JdbcTemplate`, khác với repository JPA của các bảng BF-03. Chúng là các phần đọc tối thiểu được bổ sung để tích hợp dữ liệu thật trên nhánh hiện tại. Code BF-03 gọi các **read service** tương ứng, không truy cập trực tiếp các read repository.

### 2.2. Các annotation cần giải thích được

- `@RestController`: class nhận và trả dữ liệu HTTP.
- `@Service`: khai báo thành phần xử lý nghiệp vụ do Spring quản lý.
- `@RequiredArgsConstructor`: Lombok sinh constructor cho các field `final`, dùng để truyền dependency.
- `@Transactional`: gộp các thao tác database thành một transaction. Lỗi runtime làm transaction rollback.
- `@Transactional(readOnly = true)`: dành cho luồng chỉ đọc, không có nghĩa là tự kiểm tra quyền.
- `@Valid`: chạy validation trên DTO trước khi gọi service.
- `@Enumerated(EnumType.STRING)`: lưu tên enum như `SUBMITTED`, không lưu số thứ tự enum.
- `@Nationalized`: ánh xạ chuỗi Unicode cho cột `NVARCHAR`.
- `@Immutable` trên `AuditLog`: Hibernate không cập nhật entity audit đã tồn tại.

## 3. Phân quyền và nhận diện người thao tác

### 3.1. Backend: UserReadService

[UserReadService.java](../server/src/main/java/com/swp391/kpi/admin/service/UserReadService.java)

Controller nhận `X-User-Id`, rồi service gọi:

```text
userReadService.requireActive(currentUserId, "ADMIN");
```

Hàm `requireActive()` kiểm tra lần lượt:

1. Không có ID → HTTP 401.
2. ID không tồn tại trong database → HTTP 401.
3. Tài khoản không ở trạng thái `ACTIVE` → HTTP 403.
4. Vai trò không thuộc các vai trò cho phép → HTTP 403.
5. Hợp lệ → trả `UserAccessResponse`, có ID, vai trò và ban để kiểm tra tiếp.

Ví dụ audit chấp nhận `ADMIN` hoặc `HEAD_OF_DEPARTMENT`; API gửi khiếu nại chỉ chấp nhận `REPORTER`.

Không được chỉ dựa vào việc frontend ẩn tab. Người dùng vẫn có thể gọi API trực tiếp, vì vậy backend phải tự kiểm tra.

### 3.2. Frontend: appealSessionHeaders.js

`layHeaderNguoiDung()` đọc `nguoiDung` từ `sessionStorage`.

- Ưu tiên `nguoiDung.id`.
- Nếu chưa có ID, ánh xạ username của tài khoản đăng nhập tạm sang ID dữ liệu mẫu.
- Trả header `X-User-Id` cho API.
- Thiếu phiên hoặc phiên lỗi thì báo người dùng đăng nhập lại.

**Giới hạn:** đây là cơ chế tạm theo quy ước dự án, chưa phải xác thực JWT. Header do client gửi có thể bị giả mạo; kiểm tra role/ACTIVE không chứng minh người gửi thực sự sở hữu tài khoản. Khi tích hợp đăng nhập thật, cần lấy danh tính từ token đã xác minh thay cho header và bỏ bảng ánh xạ tài khoản mẫu.

## 4. UC-35 — Gửi và theo dõi khiếu nại điểm

### 4.1. Nghiệp vụ

Tác giả chính không đồng ý với điểm đã công bố có thể chọn tiêu chí và nhập lý do để gửi khiếu nại.

Điều kiện bắt buộc:

- Người gửi là `articles.author_id`, không chỉ là đồng tác giả.
- Bài ở trạng thái `SCORE_PUBLISHED` hoặc `PUBLISHED`.
- Phiếu chấm đã nộp, được duyệt `APPROVED` và có `announced_at`.
- Thời điểm công bố không nằm trong tương lai.
- Chưa quá hạn được cấu hình.
- Bài chưa có khiếu nại.
- Có ít nhất một tiêu chí; tiêu chí thuộc phiếu chấm đã công bố; không chọn trùng; mỗi tiêu chí có lý do.

```text
deadline = announced_at + appeal_window_days
Cho phép gửi khi currentTime <= deadline
```

Đúng tại deadline vẫn được gửi. Vượt deadline mới bị từ chối.

### 4.2. Các file chính

| File | Vai trò |
|---|---|
| `AppealController.java` | API gửi, xem khiếu nại và lấy bài đủ điều kiện |
| `AppealService.java` | Toàn bộ kiểm tra và lưu khiếu nại |
| `AppealDatabaseSubmissionDataProvider.java` | Ghép bài viết, phiếu chấm và tiêu chí thành dữ liệu phục vụ UC-35 |
| `AppealSubmissionContext.java` | Dữ liệu nội bộ: tác giả chính, trạng thái bài, điểm, thời gian công bố, tiêu chí |
| `Appeal.java`, `AppealCriterion.java` | Entity ánh xạ `appeals`, `appeal_criteria` |
| `AppealRepository.java`, `AppealCriterionRepository.java` | Lưu và truy vấn khiếu nại, các tiêu chí |
| `AppealCreateRequest.java`, `AppealCriterionRequest.java` | Dữ liệu đầu vào và validation |
| `AppealEligibilityResponse.java` | Bài, điểm, tiêu chí, thời gian công bố và hạn khiếu nại |
| `AppealResponse.java` | Khiếu nại, trạng thái và các tiêu chí/lý do đã gửi |
| `NotificationService.java` | Ghi thông báo cho Trưởng ban phù hợp |

[AppealService.java](../server/src/main/java/com/swp391/kpi/appeal/service/AppealService.java)

### 4.3. Luồng submitAppeal()

1. Kiểm tra người gửi là Reporter ACTIVE.
2. Lấy dữ liệu bài và phiếu chấm thật qua provider.
3. Lấy cấu hình đang có hiệu lực.
4. Gọi `validateSubmission()`.
5. Tạo `Appeal`: lưu bài, phiếu chấm bị khiếu nại, người gửi, cấu hình, trạng thái `SUBMITTED`, thời gian gửi.
6. `saveCriteria()` lưu từng tiêu chí và lý do đã `trim()`.
7. `notifyHeads()` tạo thông báo cho các Trưởng ban ACTIVE cùng ban với bài.
8. `AuditService.log()` ghi sự kiện `CREATE` cho `appeals`.
9. `toResponse()` đổi dữ liệu thành DTO trả cho frontend.

Các bước ghi nằm trong transaction. Nếu ghi tiêu chí, thông báo hoặc audit thất bại bằng lỗi runtime, các bản ghi trong lần gửi đó rollback cùng nhau.

`notifyHeads()` ghi vào bảng `notifications`, không gửi email/push. Nếu không có Trưởng ban ACTIVE phù hợp, vòng lặp không tạo thông báo.

### 4.4. Các hàm phụ cần nắm

| Hàm | Giải thích |
|---|---|
| `validateArticleAndWindow()` | Kiểm tra tác giả chính, trạng thái bài, thời điểm công bố và deadline |
| `validateCriteria()` | Dùng `Set` kiểm tra tiêu chí thuộc phiếu chấm và không bị trùng; kiểm tra lý do |
| `validateSubmission()` | Gộp kiểm tra bài/hạn, kiểm tra bài đã khiếu nại và kiểm tra tiêu chí |
| `getEligibility()` | Kiểm tra đủ điều kiện rồi trả bảng điểm để điền form |
| `getEligibleArticles()` | Duyệt các bài đã công bố của tác giả chính, bỏ các bài hết hạn/đã khiếu nại/chưa có phiếu chấm công bố |
| `getMyAppeals()` | Chỉ truy vấn khiếu nại có `appellant_id` bằng người đang thao tác |
| `toResponse()` | Ghép tên tiêu chí với lý do và trả DTO, không để frontend phụ thuộc entity JPA |

`getMyAppeals()` đọc theo `scoring_attempt_id` đã lưu trên khiếu nại, không tự chuyển sang phiếu chấm mới nhất của bài.

### 4.5. Vì sao AppealService có hai constructor và Clock?

Constructor có `@Autowired` được Spring sử dụng, truyền đồng hồ hệ thống. Constructor còn lại cho test truyền `Clock.fixed(...)`.

Nhờ vậy test có thể đặt thời gian đúng tại deadline hoặc sau deadline mà không phải chờ nhiều ngày hay sửa giờ máy.

### 4.6. Vì sao vừa kiểm tra trùng trong service vừa có UNIQUE?

Service kiểm tra trước bằng `existsByArticleId()` để trả lỗi dễ hiểu. Tuy nhiên hai request đồng thời có thể cùng thấy “chưa có khiếu nại”.

Ràng buộc `UNIQUE(article_id)` ở database là lớp bảo vệ cuối, ngăn lưu hai khiếu nại cho cùng một bài. Xung đột ràng buộc được xử lý thành HTTP 409.

### 4.7. Frontend UC-35

- `AppealCreatePage.jsx`: chọn bài đủ điều kiện, tải điểm/nhận xét, tick tiêu chí, nhập lý do, gọi API gửi.
- `MyAppealsPage.jsx`: xem trạng thái và mở phần tiêu chí/lý do đã gửi.
- `AppealPage.css`: CSS riêng với tiền tố `.khieu-nai-pv`.
- `appealFormat.js`: định dạng ngày giờ và tính phần thời gian còn lại. Thời gian còn lại được tính khi render, chưa có bộ đếm cập nhật liên tục.
- `appealApi.js`: nơi gọi API của UC-35.
- `AppRoutes.jsx`: khai báo `/my-appeals`, `/my-appeals/new` trong nhóm Reporter.
- `reporterHomeApi.js`: phần `appealableScores` lấy từ API thật. Các dữ liệu mẫu khác của trang chủ không thuộc UC-35 vẫn được giữ.

Frontend kiểm tra lý do để người dùng sửa nhanh; backend vẫn kiểm tra độc lập.

## 5. UC-44 — Cấu hình luật khiếu nại

### 5.1. Nghiệp vụ

Admin ACTIVE tạo phiên bản cấu hình gồm thời hạn khiếu nại, số lần tối đa, ngưỡng chênh lệch và ngày hiệu lực.

Mặc định nghiệp vụ: 7 ngày, 1 lần/bài, ngưỡng 5 điểm. Có thể tạo cấu hình có ngày hiệu lực trong tương lai.

[AppealRuleConfigService.java](../server/src/main/java/com/swp391/kpi/appeal/service/AppealRuleConfigService.java)

| Hàm | Nhiệm vụ |
|---|---|
| `getActiveConfig(currentUserId)` | Kiểm tra quyền Admin và trả cấu hình hiện tại cho màn hình |
| `getActiveConfigEntity()` | Hàm nội bộ lấy entity cấu hình cho service BF-03 |
| `getAllConfigs(currentUserId)` | Kiểm tra Admin rồi trả lịch sử theo ngày hiệu lực giảm dần |
| `createConfig()` | Kiểm tra Admin, ngày trùng, tạo bản ghi mới và ghi audit |
| `toAuditValue()` | Chuyển các giá trị cấu hình thành chuỗi JSON lưu trong audit |

Cấu hình hiện tại được chọn bằng điều kiện:

```text
is_active = true
effective_from <= ngày hôm nay
Lấy dòng có effective_from lớn nhất
```

Không cần đặt tất cả phiên bản cũ thành inactive mỗi khi thêm phiên bản mới.

### 5.2. Validation

- Thời hạn và số lần tối đa phải lớn hơn 0.
- Ngưỡng nằm trong 0–100, tối đa hai chữ số thập phân.
- Ngày hiệu lực bắt buộc và không trùng phiên bản đã có.
- DTO kiểm tra định dạng/khoảng giá trị; service kiểm tra trùng ngày; database có UNIQUE để chặn race condition.

**Điểm cần hiểu đúng:** code hiện cho nhập trường `maxAppealsPerArticle` là số dương, nhưng UC-35 và UNIQUE vẫn cố định mỗi bài một khiếu nại theo BR-QC-01. Nhập giá trị lớn hơn 1 chưa làm UC-35 cho gửi nhiều lần. Đây là điểm cần chốt/đồng bộ nếu nghiệp vụ tương lai muốn thay đổi giới hạn.

### 5.3. Đóng băng cấu hình là gì?

Ví dụ khiếu nại A gửi khi cấu hình 1 có hiệu lực. A lưu `rule_config_id = 1`.

Admin tạo cấu hình 2 thì A vẫn giữ ID 1. Không sửa hàng cấu hình 1 để tránh làm thay đổi luật áp dụng cho A.

Iter1 thực hiện việc **lưu liên kết cấu hình và tạo phiên bản mới**. Khi triển khai xử lý/chốt điểm ở iteration sau, phải đọc cấu hình gắn trên appeal thay vì cấu hình hiện tại.

Frontend nằm ở `pages/admin/AppealRuleConfigPage.jsx`; gọi `appealRuleConfigApi.js` và hiển thị trong tab Admin tại `Main.jsx`.

## 6. UC-41 — Phân tích độ lệch điểm BTV

### 6.1. Báo cáo dùng để làm gì?

Báo cáo giúp theo dõi mức chênh lệch của kết quả chấm, số khiếu nại đã giải quyết, tỷ lệ đổi điểm và khối lượng chấm.

Đây là dữ liệu phân tích. Việc xem/tính báo cáo không đổi điểm chính thức, không khóa tài khoản và không tự phân công lại.

### 6.2. Ba class xử lý chính

| Class | Nhiệm vụ |
|---|---|
| `ReviewerDeviationService` | Kiểm tra quyền, giới hạn ban, gọi tính lại, lọc kết quả và map DTO |
| `ReviewerDeviationRefreshService` | Gom dữ liệu nguồn, tính theo reviewer/tháng và lưu chỉ số |
| `ReviewerDeviationCalculator` | Tính toán thuần bằng BigDecimal, không cần Spring/DB |

[ReviewerDeviationRefreshService.java](../server/src/main/java/com/swp391/kpi/appeal/service/ReviewerDeviationRefreshService.java)

### 6.3. Nguồn so sánh hợp lệ

1. **Chấm chéo:** task ở trạng thái `DONE`, đã nộp trong tháng. Độ lệch = điểm chấm chéo − điểm gốc.
2. **Khiếu nại:** appeal đã `RESOLVED`, resolution có `finalized_at` trong tháng. Độ lệch = điểm hiệu lực − điểm gốc.

Các độ lệch được tính cho **BTV chấm gốc**, không gán sang người chấm chéo/chấm lại.

Với resolution, ưu tiên `effective_score`. Nếu dữ liệu cũ chưa có giá trị này, đọc điểm từ `effective_attempt_id`. Không được tự coi một điểm trung bình là một scoring attempt có sẵn.

### 6.4. Công thức

```text
avg_deviation = tổng trị tuyệt đối của các độ lệch / số phép so sánh

appeal_count = số khiếu nại đã giải quyết của BTV trong kỳ

overturn_rate = số khiếu nại có điểm hiệu lực khác điểm gốc
                / appeal_count × 100

workload_count = số scoring attempt đã nộp trong kỳ
                 có attempt_type khác APPEAL
```

Nếu không có appeal đã giải quyết, `overturn_rate = 0`.

Ví dụ điểm gốc là 70:

| Nguồn | Điểm so sánh | Độ lệch có dấu | Độ lệch tuyệt đối |
|---|---:|---:|---:|
| Chấm chéo | 65 | −5 | 5 |
| Khiếu nại đã chốt | 77,50 | +7,50 | 7,50 |

Kết quả:

- `avg_deviation = (5 + 7,50) / 2 = 6,25`.
- Có 1 khiếu nại đã giải quyết và điểm đổi → `appeal_count = 1`, `overturn_rate = 100%`.
- Khối lượng được đếm riêng từ các lần chấm non-APPEAL, không phải số phép so sánh.

`BigDecimal` tránh sai số kiểu `double`; phép chia làm tròn hai chữ số bằng `HALF_UP`.

### 6.5. Cách refresh() gom dữ liệu

1. Kiểm tra Admin/Trưởng ban ACTIVE và kỳ là ngày đầu tháng.
2. Đọc các phiếu chấm đã nộp; lập map theo attempt ID.
3. Lập map BTV → ban.
4. Duyệt task chấm chéo hợp lệ, thêm độ lệch vào danh sách của BTV gốc.
5. Duyệt resolution hợp lệ, thêm độ lệch và đếm appeal/overturned.
6. Duyệt scoring attempt để đếm workload.
7. Gọi calculator cho từng BTV có phép so sánh.
8. Tìm chỉ số có cùng BTV/tháng: đã có thì cập nhật; chưa có thì tạo mới.

Các map giúp lưu dữ liệu theo BTV mà không phải lặp tìm lại ở mọi phép tính.

### 6.6. Phân quyền và không tạo chỉ số giả

- Admin có thể xem các ban.
- Trưởng ban chỉ xem BTV thuộc ban mình; cố yêu cầu một `sectionId` khác thì trả 403.
- Phạm vi báo cáo theo ban của **BTV**, còn phạm vi audit theo ban của **bài viết**. Đây là hai cách kiểm tra khác nhau.
- Không có phép so sánh hợp lệ thì không tạo một chỉ số 0 giả.
- `getReport()` chỉ trả các chỉ số được xác nhận có nguồn so sánh hợp lệ; hàng mẫu cũ trong DB không tự động trở thành kết quả đáng tin.

**Cơ chế hiện tại:** mở/lọc báo cáo sẽ tính lại các kỳ cần xem. Vì vậy `getReport()` dùng `@Transactional`, không dùng `readOnly=true`, dù endpoint là GET. Chưa có scheduler hoặc listener tự refresh ngay khi một flow khác ghi kết quả.

Frontend: `ReviewerDeviationPage.jsx`, `QualityReportPage.css`, `reviewerDeviationApi.js`. Tháng trên form như `2026-10` được chuyển thành `period=2026-10-01` trước khi gọi server.

## 7. UC-45 — Nhật ký hệ thống và chấm điểm

### 7.1. Tách ghi và truy vấn có quyền

| Thành phần | Trách nhiệm |
|---|---|
| `AuditService.log()` | Tạo entity audit mới và lưu sự kiện |
| `AuditLogRepository` | Lưu và tìm nhật ký; không khai báo hàm delete |
| `AppealAuditQueryService.getLogs()` | Kiểm tra người xem, phạm vi dữ liệu và bộ lọc |
| `AuditLogController` | API gọi service truy vấn có quyền |
| `AuditLogResponse` | DTO chứa actor, hành động, entity, thời gian, old/new value |

[AppealAuditQueryService.java](../server/src/main/java/com/swp391/kpi/appeal/service/AppealAuditQueryService.java)

`AuditService` còn có hàm đọc tổng quát `getLogs()`, nhưng controller hiện dùng **AppealAuditQueryService** để áp dụng quyền; không gọi trực tiếp hàm đọc tổng quát.

### 7.2. Các trường của một sự kiện

- `actor_id`: người thực hiện; NULL hiển thị “Hệ thống”.
- `action_type`: ví dụ `CREATE`.
- `entity_type`, `entity_id`: bảng và bản ghi chịu tác động.
- `old_value`, `new_value`: giá trị trước/sau dưới dạng chuỗi JSON.
- `created_at`: thời điểm ghi sự kiện.

Khi tạo khiếu nại, audit mới lưu article ID và trạng thái SUBMITTED. Nó không chứa toàn bộ lý do/điểm. Khi tạo cấu hình, audit lưu cấu hình đang áp dụng trước thao tác và các giá trị của phiên bản mới.

### 7.3. Kiểm tra phạm vi theo ban

`findArticle()` xác định bài viết gắn với sự kiện:

| Loại entity | Cách tìm bài |
|---|---|
| `articles` | Dùng trực tiếp entity ID |
| `appeals` | Đọc article ID từ khiếu nại |
| `appeal_criteria` | Đi qua appeal của tiêu chí |
| `appeal_resolutions` | Đi qua appeal của resolution |
| `scoring_attempts` | Đọc article ID qua ScoringReadService |
| `cross_review_tasks` | Đọc article ID của task |

Admin được xem toàn bộ. Trưởng ban chỉ được xem sự kiện xác định được bài thuộc ban mình. Sự kiện cấu hình toàn hệ thống hoặc entity chưa có cách ánh xạ bài không được đưa vào danh sách của Trưởng ban.

Yêu cầu trực tiếp bài ngoài ban bị trả 403. Khi lọc cụ thể entity có bản ghi audit ngoài phạm vi, API cũng trả 403; xem danh sách chung thì bỏ các sự kiện ngoài phạm vi.

### 7.4. Các bộ lọc

- Người thao tác: `actorId`.
- Hành động: `actionType`.
- Entity: `entityType`, `entityId`.
- Bài viết/khiếu nại/BTV: `articleId`, `appealId`, `editorId`.
- Khoảng thời gian: `fromTime`, `toTime`.

`matchesAppeal()` hỗ trợ sự kiện appeal, tiêu chí, resolution và scoring attempt có appeal ID tương ứng.

`matchesEditor()` nhận sự kiện do BTV đó thao tác hoặc sự kiện trên bài có lần chấm đã nộp của BTV đó. Không đồng nghĩa mọi sự kiện tìm được đều do BTV đó tạo; muốn lọc chính xác người tạo thì dùng `actorId`.

Kết quả sắp xếp mới nhất trước, rồi theo ID giảm dần.

### 7.5. Bảo vệ nhật ký và giao diện

Ứng dụng không có API sửa/xóa nhật ký; `log()` luôn tạo entity mới; entity có `@Immutable`. Các biện pháp này bảo vệ ở tầng ứng dụng/ORM, không ngăn một người có quyền SQL trực tiếp tự sửa database.

Frontend `AuditLogPage.jsx` có bộ lọc và phần mở rộng để xem đầy đủ old/new value. Editor không có quyền gọi API audit, kể cả khi tự nhập URL.

Iter1 hiện tự ghi sự kiện từ gửi khiếu nại và tạo cấu hình. Các sự kiện phân công/chấm lại/chốt điểm của iteration sau chỉ xuất hiện khi các flow đó gọi service ghi audit; viewer không tự tạo các sự kiện chưa xảy ra.

## 8. Danh sách API

Các endpoint dưới đây nhận `X-User-Id`. ID dùng để minh họa theo dữ liệu mẫu: Reporter 1, Trưởng ban 5, Admin 8.

| Method | URL | Quyền | Chức năng |
|---|---|---|---|
| GET | `/api/appeals/eligible-articles` | Reporter | Bài của tác giả chính còn đủ điều kiện gửi |
| GET | `/api/articles/{articleId}/appeal-eligibility` | Reporter là tác giả chính | Điều kiện và bảng điểm của một bài |
| POST | `/api/appeals` | Reporter là tác giả chính | Gửi khiếu nại |
| GET | `/api/appeals/mine` | Reporter | Khiếu nại của người đang thao tác |
| GET | `/api/appeal-rule-configs` | Admin | Lịch sử cấu hình |
| GET | `/api/appeal-rule-configs/active` | Admin | Cấu hình đang có hiệu lực |
| POST | `/api/appeal-rule-configs` | Admin | Tạo phiên bản cấu hình |
| GET | `/api/reports/reviewer-deviations` | Trưởng ban/Admin | Tính lại và lấy báo cáo |
| GET | `/api/audit-logs` | Trưởng ban/Admin | Nhật ký có kiểm tra phạm vi |

Ví dụ body gửi khiếu nại:

```json
{
  "articleId": 123,
  "criteria": [
    {
      "criterionId": 4,
      "reason": "Bài được thực hiện và nộp ngay trong ngày xảy ra sự kiện."
    }
  ]
}
```

ID 123 chỉ minh họa; phải thay bằng bài thật đủ điều kiện.

Ví dụ body cấu hình:

```json
{
  "appealWindowDays": 7,
  "maxAppealsPerArticle": 1,
  "varianceThreshold": 5.00,
  "effectiveFrom": "2026-11-01"
}
```

Response thành công theo `ApiResponse`:

```json
{
  "success": true,
  "message": "Gửi khiếu nại thành công",
  "data": {
    "id": 456,
    "articleId": 123,
    "status": "SUBMITTED"
  }
}
```

Response trên đã rút gọn trường để minh họa.

| HTTP status | Ý nghĩa |
|---|---|
| 200 | Đọc dữ liệu thành công |
| 201 | Tạo khiếu nại/cấu hình thành công |
| 400 | Input hoặc khoảng thời gian không hợp lệ |
| 401 | Thiếu danh tính hoặc ID người dùng không tồn tại |
| 403 | Sai vai trò, tài khoản khóa, ngoài phạm vi ban |
| 404 | Không tìm thấy dữ liệu cần dùng |
| 409 | Sai điều kiện nghiệp vụ hoặc xung đột ràng buộc dữ liệu |

`GlobalExceptionHandler` chuyển exception thành response thống nhất. `BusinessException` trả 409; `NotFoundException` trả 404; `ResponseStatusException` giữ mã lỗi được service chọn; lỗi validation trả 400.

## 9. Database và migration an toàn

| Bảng | Vai trò trong Iter1 |
|---|---|
| `appeals` | Khiếu nại; lưu article/scoring/appellant/rule config ID, trạng thái và thời gian |
| `appeal_criteria` | Các tiêu chí và lý do của từng khiếu nại |
| `appeal_rule_configs` | Các phiên bản cấu hình |
| `reviewer_deviation_indices` | Chỉ số BTV theo tháng |
| `cross_review_tasks` | Nguồn kết quả chấm chéo đã hoàn thành |
| `appeal_resolutions` | Nguồn điểm hiệu lực của khiếu nại đã giải quyết |
| `notifications` | Thông báo gửi Trưởng ban |
| `audit_logs` | Sự kiện thay đổi dữ liệu |

Các bảng `users`, `roles`, `sections`, `articles`, `scoring_attempts`, `score_approvals`, `scoring_attempt_details`, `criteria` được đọc để tích hợp.

### 9.1. Migration đầu tiên

[BF03_iter1_safe.sql](../database/migrations/BF03_iter1_safe.sql)

- Thêm `appeals.rule_config_id`.
- Gắn khiếu nại lịch sử với cấu hình active có ngày hiệu lực phù hợp thời điểm gửi.
- Chuyển cột thành NOT NULL và thêm khóa ngoại.
- Thêm UNIQUE cho article ID của appeal và ngày hiệu lực của cấu hình.
- Dừng/rollback nếu có dữ liệu trùng hoặc không tìm được cấu hình lịch sử phù hợp.

### 9.2. Migration bổ sung

[BF03_iter1_completion.sql](../database/migrations/BF03_iter1_completion.sql)

- Thêm `appeal_resolutions.effective_score DECIMAL(5,2) NULL`.
- Thêm CHECK giới hạn 0–100.
- Giữ NULL cho bản ghi cũ; báo cáo có fallback qua effective attempt.
- Không xóa hoặc cập nhật điểm lịch sử.

Cả hai script kiểm tra tồn tại trước khi thêm thành phần và dùng transaction, nên chạy lại được.

**Không chạy schema.sql để nâng cấp database đang có dữ liệu:** phần đầu script này xóa/tạo lại toàn bộ bảng. `data.sql` là seed, không phải migration. Khi chỉ nâng cấp Iter1, dùng migration riêng.

## 10. Kiểm thử và cách chạy

### 10.1. Những test cần đọc

| File test | Nội dung đáng chú ý |
|---|---|
| `AppealServiceTest` | Người gửi, deadline chính xác/hết hạn, trùng bài, tiêu chí không thuộc phiếu chấm |
| `AppealControllerTest` | HTTP và DTO của các API UC-35 |
| `AppealRuleConfigServiceTest` | Tạo phiên bản mới, chặn trùng ngày và ghi audit |
| `UserReadServiceTest` | Thiếu/không tồn tại danh tính, sai role, khóa tài khoản |
| `NotificationServiceTest` | Chỉ thông báo Trưởng ban ACTIVE đúng ban |
| `ReviewerDeviationCalculatorTest` | Trị tuyệt đối, làm tròn, tỷ lệ bằng 0, không có so sánh |
| `ReviewerDeviationRefreshServiceTest` | Ghép chấm chéo với điểm hiệu lực, không tạo chỉ số giả |
| `ReviewerDeviationServiceTest` | Map/lọc kết quả báo cáo |
| `AppealAuditQueryServiceTest` | Giới hạn ban, chặn truy cập trực tiếp ngoài phạm vi, Admin đọc sự kiện hệ thống |
| `AuditServiceTest` | Ghi sự kiện và hiển thị actor hệ thống |
| `BF03Iteration1IT` | Tích hợp Spring, JPA, JDBC và SQL Server thật |

[BF03Iteration1IT.java](../server/src/test/java/com/swp391/kpi/BF03Iteration1IT.java)

Năm test tích hợp kiểm tra:

1. Thiếu danh tính, sai role và tài khoản khóa.
2. Gửi khiếu nại lưu đủ tiêu chí/cấu hình/audit/thông báo, loại bài đã gửi khỏi danh sách đủ điều kiện.
3. Trưởng ban không đọc audit ngoài ban.
4. Báo cáo tính ra 6,25 từ hai phép so sánh và cập nhật cùng chỉ số khi chạy lại.
5. Tạo cấu hình mới, ghi audit, chặn trùng/giá trị sai, giữ liên kết cấu hình của appeal cũ.

Test tích hợp dùng `@Transactional`: dữ liệu fixture rollback sau test, không giữ các bài/người dùng/thông báo thử nghiệm. SQL Server vẫn có thể tăng bộ đếm IDENTITY dù transaction rollback; khoảng trống ID là bình thường.

### 10.2. Lệnh kiểm tra

Chạy tại thư mục gốc repo, nếu Maven đã nằm trong PATH:

```powershell
mvn -f server/pom.xml test
mvn -f server/pom.xml "-Dtest=*Test,BF03Iteration1IT" test
```

Lệnh thứ nhất chạy các test thông thường. Class kết thúc bằng `IT` không được bộ lọc mặc định của Surefire chọn, vì vậy phải chỉ định như lệnh thứ hai để chạy cả tích hợp.

Test tích hợp cần cấu hình local, SQL Server đang hoạt động, schema đã migration và dữ liệu nền theo `data.sql`. Fixture hiện dùng các ID nền của seed, nên không phải bộ test có thể chạy trên một database bất kỳ chưa có dữ liệu nền.

Frontend, chạy trong `client/`:

```powershell
npm run build
npm run lint
```

Kết quả xác minh gần nhất: **63 test toàn dự án đạt**, gồm 5 test tích hợp; frontend build/lint thành công. Con số 63 bao gồm cả test các luồng khác, không phải 63 test riêng BF-03.

Không có khẳng định đã kiểm thử giao diện bằng thao tác trình duyệt trong kết quả này; phần frontend đã được build và lint.

## 11. Gợi ý trình tự demo

1. Đăng nhập Reporter: mở “Khiếu nại điểm của tôi”.
2. Chọn “Gửi khiếu nại”: dropdown chỉ hiện bài thật còn đủ điều kiện.
3. Chọn bài, xem điểm từng tiêu chí, chọn tiêu chí, nhập lý do rồi gửi.
4. Mở danh sách để xem trạng thái SUBMITTED và lý do đã lưu.
5. Đăng nhập Admin: mở “Cấu hình khiếu nại”, tạo phiên bản mới có ngày tương lai.
6. Mở “Nhật ký chấm điểm”: lọc theo entity/appeal/bài và mở old/new value.
7. Đăng nhập Trưởng ban: mở “Độ lệch điểm”, chọn tháng có kết quả so sánh đã hoàn thành.
8. Kiểm tra yêu cầu ngoài ban bị từ chối.

Nếu không có bài đủ điều kiện, dropdown rỗng là đúng. Nếu không có phép so sánh hợp lệ, báo cáo hiển thị “Chưa có dữ liệu độ lệch trong kỳ đã chọn.”, không tự bịa chỉ số.

Với dữ liệu hiện có lúc kiểm tra, bài 1 đã có một khiếu nại cũ và thời gian công bố đã cũ. Không thể dùng lại bài này để demo tạo khiếu nại mới; không xóa khiếu nại cũ để né luật một lần/bài. Dùng bài hợp lệ từ BF-01/BF-02 hoặc chạy test tích hợp với fixture rollback.

## 12. Các câu hỏi thường gặp khi bảo vệ

**Tại sao không trả entity thẳng ra frontend?**  
DTO giới hạn trường được lộ ra, tránh vấn đề lazy loading và giảm phụ thuộc giữa giao diện với database.

**Tại sao kiểm tra ở backend dù frontend đã có validation?**  
Frontend có thể bị bỏ qua bằng cách gọi API trực tiếp. Backend và constraint database mới là nơi bảo vệ dữ liệu.

**Tại sao phải có rule_config_id?**  
Để lưu phiên bản luật áp dụng tại lúc gửi; cấu hình mới không sửa luật của khiếu nại cũ.

**Tại sao lưu cả scoring_attempt_id?**  
Để xác định đúng phiếu chấm bị khiếu nại, không nhầm với lần chấm mới của cùng bài.

**Độ lệch âm có làm giảm avg_deviation không?**  
Không. Calculator lấy trị tuyệt đối trước khi cộng; điểm −5 và +7,5 tạo trung bình 6,25.

**overturn_rate bằng 0 có nghĩa là BTV luôn đúng không?**  
Không. Có thể đơn giản là chưa có appeal đã giải quyết trong kỳ. Báo cáo không kết luận vi phạm hay tự xử phạt.

**UC-41 đã tự chạy nền chưa?**  
Chưa. Bản hiện tại tính lại khi mở/lọc báo cáo; chưa có job tự cập nhật ngay khi flow khác ghi kết quả.

**Audit có bất biến tuyệt đối trong SQL Server không?**  
Ứng dụng không cung cấp sửa/xóa và ORM đánh dấu Immutable. Điều đó không thay thế việc quản lý quyền người truy cập database trực tiếp.

**Header X-User-Id có thay JWT được không?**  
Không. Nó chỉ là cách nhận người thao tác tạm trong giai đoạn tích hợp; JWT phải xác minh danh tính bằng token.

**Iter1 có thực hiện chấm lại và chốt điểm không?**  
Không. Iter1 gửi khiếu nại, cấu hình, đọc/tính chỉ số và xem audit. Các UC chấm lại/chốt điểm thuộc iteration tiếp theo.
