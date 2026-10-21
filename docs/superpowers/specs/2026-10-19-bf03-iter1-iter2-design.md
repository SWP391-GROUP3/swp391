# BF03 Iter1–Iter2: Thiết kế, luồng màn hình và bản đồ source code

Ngày: 19/10/2026. Nhánh thực hiện: `VuNQ`.

**Trạng thái 21/10/2026:** hoàn tất phạm vi Iter1 (UC35/41/44/45), Iter2 (UC42/43) và phần phân công tối thiểu UC38 đã thống nhất. Đã xử lý ba finding của lượt review 19/10: refresh phiếu dưới khóa, giữ bộ lọc báo cáo khi đổi tháng/trang, đồng bộ form audit khi đổi URL. Backend 133 test, frontend 13 test và 3 ca browser bộ lọc đạt. Không bao gồm màn chấm mù/chốt điểm/chiến dịch chấm chéo của Iter3. Xem bằng chứng và giới hạn kiểm chứng ở mục 11 và [checkpoint](../checkpoints/2026-10-19-bf03-iter1-iter2-checkpoint.md).

## 1. Mục tiêu và phạm vi đã thống nhất

- Người phụ trách: Vũ, luồng BF03 `appeal`; làm cả backend và frontend.
- Hoàn tất kiểm chứng Iter1: UC35, UC44, UC45, UC41. Tận dụng code đã có, không viết lại cả chức năng.
- Iter2: UC42, UC43; đưa phần tối thiểu của UC38 lên trước UC43 vì giao lại phải có phân công ban đầu.
- UC43 do Trưởng ban quản lý ban của bài thực hiện, theo SRS; Admin không tự có quyền giao lại.
- Giao diện đơn giản, tái sử dụng cấu trúc đang có; không thay thiết kế chung của ứng dụng.
- Không sửa `database/schema.sql`, không đổi `ddl-auto=validate`, không thêm thư viện, không tự chạy lại `data.sql` trên DB đang dùng.
- Không sửa entity/repository/service của BF02, cấu hình bảo mật hay service dùng chung nếu chưa được chấp thuận riêng cho đúng điểm tích hợp.
- Không tự commit/push. Người dùng đã yêu cầu tiếp tục liên tục, không dừng sau từng bước; vẫn phải xin quyền riêng khi vượt ranh giới ownership. Khi một cửa sổ usage còn tối đa 10%, lưu checkpoint và dừng an toàn.

Tài liệu bổ sung này không thay thế SRS và không bổ sung một nguồn quy tắc nghiệp vụ mới. Nguồn tham chiếu: [SRS BF03](../../SRS_BF03_Appeal.md), [SRS BF02](../../SRS_BF02_Scoring.md), [tổng quan](../../SRS_Overview.md), [coding plan](../../CODING_PLAN.md), [schema chỉ đọc](../../../database/schema.sql), [AGENTS](../../../AGENTS.md).

## 2. Hiện trạng đã kiểm tra

| Phần | Hiện trạng source | Việc còn phải xác nhận |
|---|---|---|
| UC35 | Form, danh sách Reporter, API, thời hạn/tiêu chí/tác giả, binding luật, audit | Đạt hồi quy; ca ghi kiểm tra bằng transaction rollback |
| UC44 | Màn Admin, phiên bản luật theo hiệu lực, 404 trạng thái trống, audit | Đạt kiểm chứng quyền, validation, tương lai, binding cũ |
| UC45 | Nhật ký chỉ đọc, giới hạn ban, lọc người cũ/mới trong phân công | Đạt; link appeal và form đồng bộ với URL |
| UC41 | Báo cáo theo kỳ từ so sánh hợp lệ, không thay điểm chính thức | Đạt hồi quy công thức/quyền/dữ liệu rỗng |
| UC42 | Directory, hồ sơ, lịch sử theo tháng/phân trang, ba loại workload | Đạt; giữ bộ lọc báo cáo sau đổi tháng/trang |
| UC38 tối thiểu | Hàng chờ/chi tiết, ứng viên hợp lệ, phân công lần đầu | Đạt phần phụ thuộc Iter2; quản lý toàn bộ kết quả vẫn thuộc Iter3 |
| UC43 | Giao lại phiếu chưa nộp, cùng/khác ban, reason, audit/thông báo | Đạt; khóa và refresh trước recheck, test rollback và stale owner |

Khảo sát trước khi viết tài liệu: `mvn -f server/pom.xml test` đạt 96 test, `npm run build` thành công. Đây là baseline unit test/build; không phải xác nhận Iter2 chạy được hoặc toàn bộ luồng đã test bằng trình duyệt. `BF03Iteration1IT` không được Maven chạy mặc định theo tên file; cần gọi riêng khi có DB phù hợp.

## 3. Thứ tự thực hiện

| Bước | UC | Kết quả cần bàn giao | Vì sao làm ở vị trí này |
|---|---|---|---|
| 1 | UC44 | Cấu hình và đóng băng luật được kiểm chứng | Là đầu vào của gửi/xử lý khiếu nại |
| 2 | UC45 | Nhật ký và phạm vi truy cập được kiểm chứng | Dùng để đối chiếu các thao tác tiếp theo |
| 3 | UC41 | Báo cáo đúng công thức, không tạo số liệu giả khi chưa có so sánh | Là điểm mở hồ sơ BTV trong UC42 |
| 4 | UC35 | Hồi quy gửi và theo dõi khiếu nại | Khóa chất lượng Iter1 trước khi mở rộng workflow |
| 5 | UC42 | Hồ sơ BTV và lịch sử chấm chỉ đọc | Ít rủi ro ghi dữ liệu; tái dùng chỉ số của UC41 |
| 6 | UC38 tối thiểu | Hàng chờ, chi tiết, chọn BTV đủ điều kiện và phân công lần đầu | Tạo trạng thái thật để UC43 có thể giao lại |
| 7 | UC43 | Giao lại an toàn, đúng quyền, có lý do/audit/thông báo | Phụ thuộc bước 6 và tích hợp BF02 |
| 8 | Toàn bộ | Kiểm thử liên màn, cập nhật hướng dẫn và bảng tình trạng | Chỉ đánh dấu hoàn tất sau khi có bằng chứng |

UC36/37 (nhập và nộp điểm chấm mù), UC39 (so sánh/chốt kết quả), UC40 (chiến dịch chấm chéo) vẫn ở Iter3. UC38 trong đợt này chỉ hỗ trợ phân công/giao lại, không mở rộng thành toàn bộ Iter3.

## 4. Luồng đi của các màn

### 4.1 Reporter — gửi và theo dõi khiếu nại (đã có)

```text
Đăng nhập Reporter
  → /main
  → chọn bài còn hạn / vào “Khiếu nại của tôi”
  → /my-appeals/new?articleId=<id>
  → xem phiếu điểm → chọn tiêu chí → nhập lý do → Gửi
  → /my-appeals
  → xem trạng thái và lý do đã gửi
```

- Mở `/my-appeals/new` không có `articleId`: chọn bài từ danh sách đủ điều kiện.
- Thành công: lưu appeal `SUBMITTED`, các tiêu chí, cấu hình áp dụng, audit và thông báo cho Trưởng ban trong một transaction.
- Hết hạn, không phải tác giả chính, đã khiếu nại hoặc tiêu chí sai: backend từ chối; frontend hiển thị lỗi tiếng Việt. Không chỉ dựa vào nút bị ẩn.
- Reporter không được đi sang màn phân công hoặc xem hồ sơ BTV.

### 4.2 Admin — cấu hình và nhật ký (đã có)

```text
Đăng nhập Admin
  → menu Cấu hình khiếu nại → /appeal-rules
  → xem bản đang hiệu lực/lịch sử → tạo phiên bản mới
  → menu Nhật ký → /audit-logs
  → lọc entity appeal_rule_configs để đối chiếu thay đổi
```

UC44 tạo bản mới, không sửa bản cấu hình đã gắn với appeal. UC45 chỉ đọc nhật ký, không có nút sửa/xóa.

### 4.3 Trưởng ban/Admin — báo cáo sang hồ sơ BTV (đã triển khai UC42)

```text
Đăng nhập đúng vai trò
  → /reviewers (báo cáo hiện có; alias /reviewer-deviations)
  → lọc tháng/BTV/ban → nhấn tên BTV trong báo cáo hoặc danh sách BTV
  → /reviewers/:id?period=YYYY-MM&page=0
  → thông tin + khối lượng + lịch sử chấm + chỉ số theo kỳ
  → Quay lại báo cáo, giữ bộ lọc
```

Hồ sơ gồm tên, trạng thái tài khoản, ban, công việc đang xử lý, các lần chấm đã nộp, chỉ số khiếu nại và lịch sử chấm chéo. Dữ liệu dùng để tham khảo phân công, không tự xếp hạng/phạt BTV.

- Trưởng ban chỉ xem hồ sơ BTV trong ban mình; Admin được xem các ban.
- Hồ sơ ngoài phạm vi trả 403, BTV không tồn tại trả 404.
- Không có lần chấm/so sánh thì hiển thị rỗng rõ ràng, không gán “độ lệch 0” để suy ra người chấm tốt.
- Danh sách lịch sử cần phân trang và lọc kỳ. Mặc định kỳ là tháng hiện tại, nhận tháng theo dạng ngày đầu tháng ở API.

### 4.4 Trưởng ban — phân công tối thiểu và giao lại (đã triển khai)

```text
Đăng nhập Trưởng ban
  → /appeals → chọn khiếu nại thuộc ban
  → /appeals/:id
      ├─ SUBMITTED → chọn BTV đủ điều kiện → Phân công
      │    → tạo phiếu APPEAL chưa nộp → IN_REVIEW
      └─ IN_REVIEW + phiếu chưa nộp → Giao lại
           → xem danh sách hợp lệ → chọn BTV mới → nhập lý do → Xác nhận
           → cập nhật chi tiết → mở nhật ký để đối chiếu
```

Form giao lại đặt trong màn chi tiết, không tạo thêm một trang độc lập chỉ để chọn người.

Phạm vi UC38 tối thiểu:

1. Danh sách theo trạng thái và chi tiết appeal trong ban, gồm tiêu chí/lý do cho Trưởng ban.
2. Danh sách BTV ACTIVE không chấm gốc hoặc tham gia lần đánh giá trước của appeal; ưu tiên cùng ban.
3. Phân công ban đầu cùng ban. Nếu không có người hợp lệ cùng ban, đi theo nhánh ngoại lệ UC43 với lý do bắt buộc; không bỏ appeal ở trạng thái IN_REVIEW khi chưa có phiếu được giao thành công.
4. Tạo phiếu chưa nộp trên đúng article version, category, criteria version của lần gốc; giữ điểm cũ và trạng thái xuất bản của bài.

Ngoại lệ khác ban dùng cùng quy tắc hợp lệ và audit của UC43. Khác ban chỉ được chọn khi không có BTV hợp lệ cùng ban; việc giao lại thông thường dù cùng ban vẫn bắt buộc lý do.

### 4.5 Editor — ranh giới với Iter3

Màn chấm mù `/re-reviews/:id` chưa nằm trong đợt triển khai này. Backend tương lai phải kiểm tra người được giao hiện tại ở **mỗi lần đọc/ghi**, không tin danh sách task đã tải trên trình duyệt. Sau giao lại, Editor cũ mất quyền với phiếu chưa nộp.

Ở Iter2, kiểm chứng chủ sở hữu phiếu và hợp đồng kiểm tra quyền bằng test ở service tích hợp. Không tuyên bố đã kiểm thử giao diện chấm mù khi UC36 chưa triển khai. Iter3 bắt buộc dùng lại hợp đồng đó, không mở endpoint trả thẳng entity có điểm cũ.

## 5. Một thao tác trên màn hình đi qua source như thế nào?

```text
Page.jsx → api/<luồng>Api.js → goiApi.js (gắn JWT)
  → Controller (lấy userId từ JWT, @Valid)
  → Service (quyền + luật nghiệp vụ + transaction)
  → Repository → SQL Server
  → DTO → ApiResponse → Page cập nhật nội dung
```

Ý nghĩa các tầng:

- **Page:** nhận thao tác, loading/lỗi/thành công; không tự quyết quyền hoặc tính điểm chính thức.
- **API client:** định nghĩa URL và payload; gọi `goiApi`, không tự tạo cơ chế đăng nhập.
- **Controller:** nhận request và gọi service; không chứa thuật toán phân công.
- **Service:** nơi trả lời “ai được làm, trạng thái có đúng không, lưu gì cùng nhau”.
- **Repository:** đọc/ghi bảng, không quyết định ai được giao lại.
- **DTO:** chỉ chứa dữ liệu màn đó cần; đặc biệt không tái dùng DTO có điểm cũ cho màn chấm mù.

## 6. Bản đồ source hiện có

Các link dưới đây trỏ tới file đã tồn tại tại thời điểm thiết kế. “Rà soát” không có nghĩa phải sửa tất cả các file.

### 6.1 Frontend

| File | Trách nhiệm và tác động |
|---|---|
| [AppRoutes.jsx](../../../client/src/routes/AppRoutes.jsx) | Khai báo route theo role. Khi triển khai mới, thay đúng placeholder bằng trang thật, tránh hai route trùng `/reviewers` của Admin; không thay route BF01/02/04/05 |
| [ReporterLayout.jsx](../../../client/src/layouts/ReporterLayout.jsx), [HeadLayout.jsx](../../../client/src/layouts/HeadLayout.jsx), [AdminLayout.jsx](../../../client/src/layouts/AdminLayout.jsx) | Menu điều hướng. Tái sử dụng mục đang có; đây là file dùng chung, cần báo trước nếu thực sự phải sửa |
| [AppealCreatePage.jsx](../../../client/src/pages/appeal/AppealCreatePage.jsx) | Chọn bài, tải phiếu điểm, chọn tiêu chí, nhập lý do và gửi UC35 |
| [MyAppealsPage.jsx](../../../client/src/pages/appeal/MyAppealsPage.jsx) | Hiển thị appeal của Reporter và trạng thái; không phân công |
| [AppealRuleConfigPage.jsx](../../../client/src/pages/admin/AppealRuleConfigPage.jsx) | Form/lịch sử UC44; chỉ Admin dùng |
| [AuditLogPage.jsx](../../../client/src/pages/appeal/AuditLogPage.jsx) | Bộ lọc và chi tiết old/new UC45 |
| [ReviewerDeviationPage.jsx](../../../client/src/pages/appeal/ReviewerDeviationPage.jsx) | UC41. Bổ sung liên kết mở UC42 và giữ điều kiện lọc khi quay lại |
| [appealApi.js](../../../client/src/api/appealApi.js) | API UC35 và API hàng chờ/chi tiết/ứng viên/phân công/giao lại |
| [appealRuleConfigApi.js](../../../client/src/api/appealRuleConfigApi.js), [auditLogApi.js](../../../client/src/api/auditLogApi.js), [reviewerDeviationApi.js](../../../client/src/api/reviewerDeviationApi.js) | API cho từng màn Iter1; giữ các hàm đang được trang gọi |
| [goiApi.js](../../../client/src/api/goiApi.js), [phienDangNhap.js](../../../client/src/auth/phienDangNhap.js), [RequireRole.jsx](../../../client/src/routes/RequireRole.jsx) | Gửi token, quản lý phiên và chặn route. Chỉ tái sử dụng, không viết đăng nhập BF03 riêng |

Frontend UC42 đã tạo:

- [ReviewerProfilePage.jsx](../../../client/src/pages/appeal/ReviewerProfilePage.jsx) và CSS cạnh trang: hồ sơ UC42, tháng và trang nằm trên URL; quay lại giữ bộ lọc báo cáo.
- [reviewerProfileApi.js](../../../client/src/api/reviewerProfileApi.js): gọi directory/hồ sơ/lịch sử qua `goiApi`, không đọc DB trực tiếp.

Frontend UC38/43 đã tạo:

- [AppealManagementPage.jsx](../../../client/src/pages/appeal/AppealManagementPage.jsx) và CSS: hàng chờ tối thiểu UC38, lọc trạng thái/bài/người đang chấm/ngày; tham số trên URL, giữ khi quay lại từ chi tiết.
- [AppealDetailPage.jsx](../../../client/src/pages/appeal/AppealDetailPage.jsx) và CSS: tiêu chí/lý do cho Head, ứng viên, xác nhận phân công/giao lại; gửi `attemptId` và `expectedReviewerId` để phát hiện thao tác cũ. Khi 409, báo lỗi và tải lại, không tự thử ghi lần nữa.
- `appealApi.js`: `layHangChoKhieuNai`, `layChiTietKhieuNai`, `layNguoiChamHopLe`, `phanCongKhieuNai`, `giaoLaiKhieuNai`; giữ nguyên API Reporter.

Chưa có `docs/ui-images/` trong repo được khảo sát. UC42 dùng các trang BF03 hiện có làm mẫu theo phạm vi giao diện đơn giản đã được người dùng đồng ý; tái dùng `QualityReportPage.css`, CSS mới có tiền tố riêng. Không thay phong cách toàn ứng dụng.

### 6.2 Backend BF03

| File | Trách nhiệm và tác động |
|---|---|
| [AppealController.java](../../../server/src/main/java/com/swp391/kpi/controller/AppealController.java) | Các endpoint Reporter hiện có; giữ tương thích |
| [AppealService.java](../../../server/src/main/java/com/swp391/kpi/service/AppealService.java) | Gửi/xem appeal, thời hạn, tác giả, tiêu chí, lưu cấu hình áp dụng; không nhét toàn bộ Iter2 vào đây |
| [AppealRuleConfigService.java](../../../server/src/main/java/com/swp391/kpi/service/AppealRuleConfigService.java) | Chọn bản luật đang hiệu lực và tạo phiên bản mới, dùng cho UC44/35 |
| [AppealAuditQueryService.java](../../../server/src/main/java/com/swp391/kpi/service/AppealAuditQueryService.java) | Chỉ đọc audit trong phạm vi được phép; kiểm tra thêm log phân công/giao lại sau khi có dữ liệu mới |
| [ReviewerDeviationService.java](../../../server/src/main/java/com/swp391/kpi/service/ReviewerDeviationService.java) | Lấy báo cáo theo kỳ/BTV/ban, kiểm tra phạm vi |
| [ReviewerDeviationRefreshService.java](../../../server/src/main/java/com/swp391/kpi/service/ReviewerDeviationRefreshService.java) | Tổng hợp dữ liệu nguồn và cập nhật chỉ số theo tháng |
| [ReviewerDeviationCalculator.java](../../../server/src/main/java/com/swp391/kpi/service/ReviewerDeviationCalculator.java) | Phép tính thuần: độ lệch tuyệt đối, tỷ lệ đổi điểm, làm tròn; dùng test không cần DB |
| [AppealRepository.java](../../../server/src/main/java/com/swp391/kpi/repository/AppealRepository.java) | Bổ sung query/khóa appeal cho hàng chờ và chuyển trạng thái khi đã đến bước backend |
| [Appeal.java](../../../server/src/main/java/com/swp391/kpi/entity/Appeal.java), [AppealStatus.java](../../../server/src/main/java/com/swp391/kpi/entity/AppealStatus.java) | Giữ ba trạng thái persisted; không thêm trạng thái DB để biểu diễn màn hình |

Backend UC42 đã tạo:

- [ReviewerProfileController.java](../../../server/src/main/java/com/swp391/kpi/controller/ReviewerProfileController.java) → [ReviewerProfileService.java](../../../server/src/main/java/com/swp391/kpi/service/ReviewerProfileService.java): directory, hồ sơ và lịch sử; lấy actor từ JWT, giới hạn Head theo ban, Admin xem các ban.
- DTO `ReviewerDirectoryResponse`, `ReviewerProfileResponse`, `ReviewerScoringHistoryResponse`, `ReviewerCrossReviewResponse`: chỉ trả trường cần hiển thị, không trả password/token hay entity trực tiếp.
- Workload hiện tại: số phân công gốc có bài `ASSIGNED`, số chấm chéo `PENDING`, số phiếu APPEAL chưa nộp (`pendingAppealReviewCount`). Không dùng tổng mọi phân công hoặc tổng phiếu đã nộp làm số việc đang chờ.
- Chỉ số hồ sơ tính từ dữ liệu nguồn qua phép tính thuần UC41, không ghi `reviewer_deviation_indices` khi mở hồ sơ. Không có so sánh thì trả `null`, không tạo độ lệch 0 giả.

Backend UC38/43 đã tạo:

- [AppealManagementController.java](../../../server/src/main/java/com/swp391/kpi/controller/AppealManagementController.java) → [AppealManagementService.java](../../../server/src/main/java/com/swp391/kpi/service/AppealManagementService.java): danh sách/chi tiết/phân công/giao lại, tách khỏi service gửi appeal của Reporter. Controller nhận JWT/DTO, service kiểm tra Head ACTIVE cùng ban và transaction.
- [ScoringAppealService.java](../../../server/src/main/java/com/swp391/kpi/service/ScoringAppealService.java): điểm tích hợp BF02 đã được cho phép. Tạo phiếu nháp giữ version/assignment/category/criteria-set gốc; giao lại cùng phiếu, audit nháp cũ rồi xóa điểm/nhận xét nháp khỏi phiếu mới giao. `lockedDraft` gọi `EntityManager.refresh(..., PESSIMISTIC_WRITE)` vì query có khóa không tự làm mới entity đã cache; sau đó mới kiểm tra submittedAt/người được giao và chụp audit. `requireOwnedDraft` dùng cùng cơ chế để từ chối BTV cũ.
- DTO `AppealManagementResponse`, `AppealReviewerCandidateResponse`, `AppealAssignRequest`, `AppealReassignRequest`: dữ liệu màn Trưởng ban. Request giao lại có người mới, lý do và mã phiếu đang giao để phát hiện thao tác cũ.
- Các class nằm trong thư mục tầng hiện có (`controller`, `service`, `dto`), không tạo package con theo luồng, không tự dựng interface + Impl cho mỗi service.

### 6.3 Điểm tích hợp có sẵn — chỉ gọi qua service

| Source | Đã cung cấp | Thiếu cho Iter2 / cách xử lý |
|---|---|---|
| [UserReadService.java](../../../server/src/main/java/com/swp391/kpi/service/UserReadService.java) | User, role, status, section; `requireActive` | Tái dùng để kiểm tra quyền và lọc BTV, không gọi repository user trực tiếp |
| [ArticleReadService.java](../../../server/src/main/java/com/swp391/kpi/service/ArticleReadService.java) | Thông tin bài, tác giả, ban | Tái dùng kiểm tra ban; không đổi trạng thái bài khi giao người chấm appeal |
| [ScoringReadService.java](../../../server/src/main/java/com/swp391/kpi/service/ScoringReadService.java) | Lần chấm đã nộp/công bố và tiêu chí | Dùng cho dữ liệu đã nộp; nháp đi qua `ScoringAppealService`, không query BF02 trực tiếp từ BF03 |
| [ScoringAppealService.java](../../../server/src/main/java/com/swp391/kpi/service/ScoringAppealService.java) | Snapshot, tạo/giao lại nháp, kiểm tra chủ phiếu, workload APPEAL | Hợp đồng Iter2 đã triển khai; Iter3 phải tái dùng kiểm tra chủ phiếu trong transaction và bổ sung kiểm tra appeal status |
| [AssignmentService.java](../../../server/src/main/java/com/swp391/kpi/service/AssignmentService.java) | Khối lượng công việc/phân công BF02 | Có thể gọi hàm đọc khối lượng UC42; không gọi `assignArticle` để giao lại appeal vì sẽ sửa phân công gốc |
| [AuditService.java](../../../server/src/main/java/com/swp391/kpi/service/AuditService.java) | `log(actorId, actionType, entityType, entityId, oldValue, newValue)` | Tái dùng để ghi log; UI đọc qua query service đã kiểm tra phạm vi, không mở đường vòng audit không giới hạn |
| [NotificationService.java](../../../server/src/main/java/com/swp391/kpi/service/NotificationService.java) | Gửi Trưởng ban/theo role/riêng người bằng `notifyUser` | Bổ sung riêng người đã được chấp thuận; cùng transaction, thông báo không chứa điểm/lý do khiếu nại |

## 7. Hợp đồng API

### 7.1 API đã có — giữ nguyên

| API | Màn sử dụng |
|---|---|
| `GET /api/appeals/eligible-articles` | Bài Reporter có thể khiếu nại |
| `GET /api/articles/{articleId}/appeal-eligibility` | Phiếu điểm và thời hạn UC35 |
| `POST /api/appeals` | Gửi khiếu nại |
| `GET /api/appeals/mine` | Theo dõi của Reporter |
| `GET /api/appeal-rule-configs/active`, `GET /api/appeal-rule-configs`, `POST /api/appeal-rule-configs` | UC44 |
| `GET /api/audit-logs` | UC45 |
| `GET /api/reports/reviewer-deviations` | UC41 |

### 7.2 API UC42 đã triển khai

| API | Mục đích và quyền |
|---|---|
| `GET /api/reviewers` | Danh sách BTV độc lập với việc đã có chỉ số hay chưa; Head cùng ban/Admin |
| `GET /api/reviewers/{editorId}` | Hồ sơ và chỉ số theo `period`; Head cùng ban/Admin |
| `GET /api/reviewers/{editorId}/scoring-history` | Lịch sử phân trang `page=0&size=10`, lọc kỳ; cùng quyền hồ sơ |

API nhận `period=YYYY-MM-01`; frontend chuyển từ tháng `YYYY-MM`. Lịch sử chỉ lấy phiếu đã nộp, mới nhất trước.

### 7.3 API UC38/43 đã triển khai

| API | Mục đích và quyền |
|---|---|
| `GET /api/appeals` | Hàng chờ thuộc ban của Head, phân trang/lọc trạng thái |
| `GET /api/appeals/{appealId}` | Chi tiết để Head quản lý; không tái dùng cho Editor |
| `GET /api/appeals/{appealId}/reviewer-candidates` | BTV hợp lệ, trạng thái cùng/khác ban; cùng quyền chi tiết |
| `POST /api/appeals/{appealId}/assign` | Phân công ban đầu từ SUBMITTED; Head quản lý bài |
| `POST /api/appeals/{appealId}/reassign` | Giao lại phiếu chưa nộp trong IN_REVIEW; Head quản lý bài |

Mọi API mới dùng `ApiResponse`, lấy userId từ JWT, không nhận actorId do client tự khai. Phân trang dùng giới hạn kích thước tối đa 100; page âm/size ngoài 1–100 bị từ chối 400. ID không hợp lệ bị từ chối; không tự đổi thành ID mặc định.

## 8. Quy tắc dữ liệu và an toàn UC43

### 8.1 Trạng thái

```text
SUBMITTED --phân công đầu thành công--> IN_REVIEW --Iter3 chốt kết quả--> RESOLVED
                                          |
                                          +--giao lại phiếu chưa nộp--> IN_REVIEW
```

- Giao lại không tạo thêm một lượt chấm lại; không tăng số vòng S1/S2.
- Không sửa điểm/nhận xét lần chấm gốc hoặc các phiếu đã nộp.
- Không đổi `articles.status` hoặc sửa `assignments.editor_id` để biểu diễn người chấm appeal.
- BTV mới ACTIVE/EDITOR, không là người chấm gốc, không tham gia một đánh giá trước đó của cùng appeal và khác người đang được giao.
- Lịch sử được giao trước đó phải lấy từ phiếu và audit để không chọn lại người không đủ điều kiện khi một phiếu nháp đã đổi chủ.

### 8.2 Transaction và thao tác đồng thời

Service giao lại khóa dòng appeal và phiếu liên quan trong transaction, rồi kiểm tra lại trạng thái, quyền, người đang được giao và `submitted_at` sau khi có khóa. Hai request dùng cùng trạng thái cũ: chỉ request đầu được ghi, request còn lại trả 409, không ghi đè người vừa giao.

Việc kiểm tra “chưa nộp” và cập nhật người chấm phải là thao tác an toàn tại service BF02 sở hữu phiếu; không tách một lần đọc và một lần ghi độc lập để phiếu có thể được nộp xen giữa.

Cập nhật người được giao, dấu khác ban, audit và thông báo cùng transaction. Nếu bất kỳ bước lưu nào lỗi thì rollback toàn bộ. Thông báo không chứa điểm, nhận xét cũ, tiêu chí/lý do khiếu nại hoặc tên người chấm gốc.

Audit lưu appealId, attemptId, người cũ/mới, lý do, dấu khác ban, actor và thời gian. Giữ dữ liệu cũ trong audit, không xóa log. Dùng action `ASSIGN`/`REASSIGN`, entity `appeals` để query phạm vi được xác định từ bài của appeal.

### 8.3 Hợp đồng tích hợp BF02 đã được cho phép

`scoring_attempts` thuộc BF02 và đã có entity `ScoringAttempt`. Không tạo entity BF03 thứ hai ánh xạ cùng bảng hoặc repository BF03 để né ownership.

Người dùng đã cho phép bổ sung `ScoringAppealService` và các phương thức repository cần thiết cho Iter2. Hợp đồng sử dụng:

1. Đọc snapshot lần gốc: articleId, versionId, assignmentId, categoryId, criteriaSetVersionId, attemptNo, reviewerId.
2. Đọc danh sách phiếu APPEAL và người đang được giao, gồm phiếu chưa nộp; lịch sử đã tham gia.
3. Tạo phiếu APPEAL chưa nộp gắn `appeal_id`, giữ đúng snapshot, không thay phân công gốc.
4. Đổi người chấm có kiểm tra lại phiếu chưa nộp/người hiện tại dưới khóa; đánh dấu khác ban và xử lý dữ liệu nháp không để Editor mới thấy điểm/nhận xét của Editor cũ.
5. Kiểm tra quyền theo chủ phiếu hiện tại cho các endpoint chấm mù về sau.

Tích hợp Iter2 đã được kiểm tra bằng service/repository thật trên SQL Server trong các transaction rollback, không chỉ double. Việc nộp điểm UC36 ở Iter3 chưa có trong hợp đồng này; không tự mở rộng quyền sửa BF02/schema chỉ vì phần nháp đã được duyệt.

Schema hiện bắt buộc `assignment_id`, và phiếu đã nộp phải có `decision`. Tạo phiếu chưa nộp có thể giữ FK assignment gốc; không chèn quyết định ACCEPT giả để lách ràng buộc khi nộp. Phần nộp điểm UC36 là cổng phối hợp với BF02/DB của Iter3, không tự sửa schema trong Iter2.

## 9. Tiêu chí nghiệm thu

| Phần | Các ca bắt buộc |
|---|---|
| UC44 | Admin/khác role; ngày hiệu lực trùng/tương lai; giá trị ngoài miền; appeal cũ giữ rule_config_id; audit đủ old/new |
| UC45 | Head cùng/khác ban, Admin, Reporter/Editor; lọc theo bài/appeal/entity/thời gian; actor hệ thống; không có API sửa/xóa |
| UC41 | Độ lệch tuyệt đối, số lần so sánh, tỷ lệ đổi điểm, mẫu số 0; tháng không có dữ liệu; không đổi điểm bài; lọc ban |
| UC35 | Tác giả chính/đồng tác giả; đúng thời điểm hết hạn/sau hạn; gửi trùng kể cả đồng thời; tiêu chí sai/trùng; lý do rỗng; rollback nếu lưu lỗi |
| UC42 | BTV không tồn tại, không có lịch sử, khác ban; lọc tháng và phân trang; workload không nhầm với số phiếu đã nộp; không trả password/token |
| UC38 tối thiểu | Snapshot đúng; loại BTV gốc/khóa tài khoản/sai role; không có ứng viên; chỉ phân công một lần dù request đồng thời; không đổi bài/phân công gốc |
| UC43 | Có/không lý do; cùng/khác ban; BTV đã tham gia; người hiện tại; phiếu đã nộp/appeal đã giải quyết; hai thao tác đồng thời; rollback; audit/thông báo; quyền Editor cũ bị thu hồi theo hợp đồng |

Định nghĩa UC41: độ lệch trung bình lấy trung bình trị tuyệt đối của các so sánh hợp lệ; số appeal tính các appeal đã RESOLVED trong kỳ; tỷ lệ đổi điểm là số appeal có điểm hiệu lực khác điểm gốc chia tổng appeal đã giải quyết, nhân 100, mẫu số 0 trả 0. Không gán độ lệch giả cho người chưa có so sánh.

### Kiểm tra kỹ thuật khi triển khai

- Backend: `mvn -f server/pom.xml compile`, sau đó `mvn -f server/pom.xml test`.
- Integration Iter1 trên DB test phù hợp: `mvn -f server/pom.xml "-Dtest=*Test,BF03Iteration1IT" test`; các fixture phải rollback, không chạy seed/reset DB người dùng.
- Frontend: `npm run build` và `npm run lint` trong `client/`; lỗi có sẵn phải được báo riêng, không tiện tay sửa phần người khác.
- Chạy ứng dụng và gọi API bằng JWT thật; kiểm tra chuyển màn trên trình duyệt trước khi kết luận nghiệm thu UI. Nếu chỉ test service thì ghi đúng mức kiểm tra.
- SQL dữ liệu mẫu chỉ chỉnh khi được yêu cầu riêng. Khi cần tạo tình huống UC43, ưu tiên fixture rollback thay vì sửa appeal thật thành IN_REVIEW.

## 10. Cách giải thích khi trình bày

1. **Vì sao làm cấu hình và audit trước?** Luật phải xác định được và thao tác phải truy vết được trước khi mở rộng xử lý.
2. **Vì sao UC42 trước UC43?** Hồ sơ là chức năng đọc, hỗ trợ chọn người; giao lại là thao tác ghi có nhiều ràng buộc.
3. **Vì sao cần một phần UC38 sớm?** Không thể giao lại nếu chưa có phân công ban đầu. Đây là phụ thuộc nghiệp vụ, không phải làm toàn bộ Iter3 trước.
4. **Vì sao không sửa assignments?** Đó là phân công chấm gốc BF02; người chấm appeal thuộc phiếu APPEAL riêng.
5. **Vì sao không chỉ chặn nút trên frontend?** Người dùng có thể gọi API trực tiếp; backend phải kiểm tra quyền/trạng thái mỗi lần.
6. **Vì sao dùng transaction?** Tránh đổi người thành công nhưng thiếu audit/thông báo, hoặc hai Trưởng ban ghi đè nhau.
7. **Số liệu đến từ đâu?** Database qua API, không đọc trực tiếp data.sql. File SQL chỉ tạo dữ liệu ban đầu; các chỉ số lấy từ bản ghi nghiệp vụ hợp lệ.

## 11. Nghiệm thu phạm vi Iter1–2 — 21/10/2026

- Đọc lại checkpoint trên nhánh `VuNQ`, HEAD `551fc8a` sau merge main. Không làm lại code đã có; sửa ba finding còn mở bằng test trước. Không đổi schema/data.sql/dependency, không commit/push.
- Backend `compile` thành công. `mvn -f server/pom.xml "-Dtest=*Test,BF03Iteration1IT,BF03ReviewerProfileIT,BF03AppealManagementIT" test` đạt **133 test, 0 fail/error/skip**. Nhóm quản lý có 11 integration test, gồm 3 regression freshness: đã nộp, đổi chủ và nhận xét nháp vừa lưu. Các fixture ghi rollback; test khóa dùng hai transaction, không sửa bản ghi thật.
- Frontend `node --test tests/*.test.js`: **13/13**; `npm run build`: thành công, 130 module; lint exit 0 với **8 warning ngoài BF03** sau merge main. Không tiện sửa BF02/KPI.
- [bf03Filters.smoke.js](../../../client/tests/browser/bf03Filters.smoke.js) chạy qua Playwright CLI: **3 ca RED→GREEN**, đổi tháng/đổi trang giữ bộ lọc và audit chuyển từ URL có appealId về menu không còn form cũ. API giả lập riêng đường dẫn `/api/`, không chặn module `/src/api/`, không ghi DB. Ví dụ chạy khi Vite đang mở: `npx --yes --package @playwright/cli playwright-cli --session bf03-resume run-code --filename C:/Users/Admin/IdeaProjects/swp391/client/tests/browser/bf03Filters.smoke.js` sau lệnh `open http://127.0.0.1:5173`.
- Browser với backend thật: đăng nhập `duc.vm`, danh sách → chi tiết appeal → audit đúng mã → menu nhật ký bỏ scope → hồ sơ BTV; 5 endpoint GET thật trả 200. Không dùng khóa/JWT giả cho lượt này. Mật khẩu Trưởng ban đã được đặt lại ở yêu cầu riêng trước đó; phiên này không đổi tài khoản.
- Nhánh ghi phân công/giao lại kiểm tra bằng integration rollback; browser ghi thành công/409 từng được kiểm tra bằng response giả cho ID test, **không tuyên bố đã ghi appeal thật bằng browser**.
- Source/UI Iter3 vẫn để ngoài phạm vi: UC36/37 nộp chấm mù, UC39 chốt điểm, UC40 chiến dịch chấm chéo. Không tự triển khai để lấp placeholder.

### Nhật ký cũ — kết quả dưới đây là tại thời điểm từng bước 19/10/2026

### Cập nhật thực thi 19/10/2026 — lỗi status của UC44

- Được phép sửa file dùng chung `client/src/api/goiApi.js` để bổ sung `Error.status` cho lỗi HTTP. Không đổi message/JWT/body/dữ liệu thành công.
- Vì sao tác động đến UC44: khi chưa có luật hiệu lực, API trả 404. `AppealRuleConfigPage.taiDuLieu()` dựa vào `loi.status === 404` để hiển thị trạng thái trống và vẫn tải lịch sử. Trước sửa, status bị mất; sau sửa, nhánh xử lý đó nhận đúng 404.
- Source thực tế: [goiApi.js](../../../client/src/api/goiApi.js). Test mới: [appealRuleConfigApi.test.js](../../../client/tests/appealRuleConfigApi.test.js).
- Test đi qua API client thật, chỉ thay fetch/browser globals bằng môi trường kiểm thử: 9 ca đạt sau khi đã quan sát 6 ca thiếu status thất bại trước sửa. Frontend build thành công; lint exit 0 với 4 cảnh báo ngoài BF03.
- Chi tiết bằng chứng và bước còn lại: [kế hoạch UC44](../plans/2026-10-19-bf03-uc44-hardening-plan.md).
- Đây là kết quả riêng bước sửa status ban đầu. Kết quả kiểm chứng tiếp theo và UC42 nằm bên dưới; không sửa database/schema, chưa commit/push.

### Cập nhật kiểm chứng và UC42 — 19/10/2026

- Backend compile thành công; `mvn -f server/pom.xml "-Dtest=*Test,BF03Iteration1IT,BF03ReviewerProfileIT" test` đạt **114 test, 0 lỗi, 0 bỏ qua**. Integration test tạo fixture trong transaction rollback, không chạy lại SQL seed/schema.
- Frontend `node --test tests/*.test.js` đạt **11/11**, build thành công (118 module), lint exit 0 với 4 cảnh báo có sẵn ngoài BF03.
- Test UC44 bổ sung kiểm tra cấu hình tương lai không thay bản đang hiệu lực, appeal cũ giữ cấu hình và audit đủ old/new; giá trị không hợp lệ bị từ chối.
- UC42 có 9 unit test và 2 integration test: quyền/ban, dữ liệu rỗng, công thức, workload, tháng/phân trang, không ghi chỉ số/audit khi chỉ đọc, không lộ password.
- Trình duyệt thật: đăng nhập Admin/Reporter, mở active/history UC44, audit, directory → hồ sơ → đổi tháng; Reporter mở bài mẫu và kiểm tra chưa chọn tiêu chí. HTTP 404 active-config được giả lập riêng để kiểm tra trạng thái trống mà lịch sử vẫn tải.
- Không gửi appeal hoặc tạo cấu hình thật trong trình duyệt; nhánh ghi thành công kiểm tra bằng integration transaction rollback. Không coi kiểm tra này là nghiệm thu UC38/43.
- Còn lỗi chữ trong dữ liệu tên user/ban/role có sẵn; không tự sửa dữ liệu dùng chung. Nội dung bài/tiêu chí hiển thị có dấu.
- Đã dừng backend/Vite và đóng phiên trình duyệt do phiên kiểm chứng này mở. Các thay đổi chưa commit được giữ trên nhánh `VuNQ`.
