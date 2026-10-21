# Checkpoint BF03 Iter1–Iter2 — 19/10/2026

## CẬP NHẬT MỚI NHẤT — 21/10/2026: hoàn tất phần còn dở Iter1–2

Mục này thay thế trạng thái dừng 19/10 bên dưới. Tiếp tục trên `VuNQ`, HEAD đầu phiên `551fc8a` (đã merge main), working tree sạch trước sửa. Không làm lại các chức năng đã tồn tại.

- Ba finding còn mở đã xử lý: refresh draft dưới `PESSIMISTIC_WRITE` trước kiểm tra/snapshot; giữ navigation state khi đổi tháng/trang UC42; reset nội dung/form audit theo query key khi chuyển scope URL.
- RED→GREEN: ca cũ phiếu vừa nộp vẫn bị giao lại; thêm hai ca đổi chủ và nhận xét nháp mới nhất (đều quan sát RED trước sửa). `BF03AppealManagementIT` hiện có 11 ca đạt. Không bỏ/disable test đỏ cũ.
- Regression browser mới `client/tests/browser/bf03Filters.smoke.js`: 3 ca từng RED vì mất bộ lọc/giữ scope cũ, nay GREEN `{passed:3,writes:0}`. API đọc được giả lập để có hai trang lịch sử; chỉ match `/api/`, không chặn module `/src/api/`. Không cần thêm dependency.
- Backend compile thành công; full suite `mvn -f server/pom.xml "-Dtest=*Test,BF03Iteration1IT,BF03ReviewerProfileIT,BF03AppealManagementIT" test` đạt **133 test, 0 fail/error/skip**. Log `server/target/bf03-resume-suite.log`; test mới dùng fixture rollback, không thay seed/schema.
- Node `node --test tests/*.test.js` tại client: **13/13**; frontend build thành công (130 module). Lint exit 0, **8 warning ngoài BF03** ở flow2/KPI sau merge main, không có warning mới trong phần sửa.
- Browser backend thật: đăng nhập bằng form `duc.vm`, danh sách → chi tiết → audit theo appeal → menu bỏ scope → hồ sơ BTV; 5 GET thật trả 200, không mock login/JWT. Không ghi nghiệp vụ thật. Mật khẩu đã sửa theo yêu cầu riêng trước đó vẫn dùng được; phiên 21/10 không đổi mật khẩu hoặc tài khoản.
- API ghi phân công/giao lại đã kiểm tra qua integration rollback; browser ghi giả lập của phiên trước không được mô tả thành browser ghi DB thật. Giữ ranh giới này trong nghiệm thu.
- Không đổi `database/schema.sql`, `database/data.sql`, config, thư viện hoặc source luồng khác ngoài điểm tích hợp BF02 đã cho phép. Chưa commit/push.

### Source thay đổi trong phiên tiếp tục

- `server/src/main/java/com/swp391/kpi/service/ScoringAppealService.java`: inject EntityManager, refresh dưới khóa trong `lockedDraft` — cùng một nơi bảo vệ cả giao lại và kiểm tra người được giao.
- `server/src/test/java/com/swp391/kpi/BF03AppealManagementIT.java`: hai regression freshness mới; giữ ca đã nộp cũ.
- `server/src/test/java/com/swp391/kpi/service/ScoringAppealServiceTest.java`: truyền EntityManager mock vào constructor; hành vi DB kiểm tra bằng IT thật.
- `client/src/pages/appeal/ReviewerProfilePage.jsx`: giữ `location.state` trên cả đổi tháng, trang trước và trang sau.
- `client/src/pages/appeal/AuditLogPage.jsx`: nội dung có query key; đổi scope reset form/data/error và cleanup request đang tải của scope cũ.
- `client/tests/browser/bf03Filters.smoke.js`: callback chạy bằng Playwright CLI, không dùng framework hoặc dependency mới.
- [Tài liệu thiết kế/luồng/source map](../specs/2026-10-19-bf03-iter1-iter2-design.md) đã cập nhật source thực tế UC38 tối thiểu, UC42/43 và bằng chứng; không còn coi các file đó là dự kiến.

### Phạm vi kết thúc

Iter1: UC35/41/44/45. Iter2: UC42/43 và phần phân công tối thiểu UC38 làm phụ thuộc. UC36/37/39/40 và quản lý kết quả đầy đủ UC38 vẫn thuộc Iter3; không tự triển khai placeholder. Các finding review trước đã có regression; không dispatch lại reviewer để đọc lại cùng diff. Không còn lỗi mở từ lượt review 19/10 trong phạm vi này. Những cảnh báo lint và dữ liệu tên user/ban lỗi mã hóa có sẵn ngoài phạm vi không được sửa tiện tay.

## LỊCH SỬ 19/10/2026 — dừng theo ngưỡng usage 9%

Mục này thay thế trạng thái cũ bên dưới. Người dùng đã cho phép tích hợp BF02 và NotificationService. **Không còn chờ quyền. Chưa được đánh dấu Iter2 hoàn tất hoặc triển khai như bản đã nghiệm thu.**

- Usage đo cuối: primary dùng 91%, còn 9%; secondary dùng 25%, còn 75%. Dừng theo yêu cầu người dùng, không tiếp tục sửa code trong phiên này.
- Nhánh vẫn `VuNQ`, HEAD `acba541`; toàn bộ thay đổi giữ nguyên chưa commit/push. Không stash/reset/xóa source, không sửa schema.sql/data.sql hoặc mật khẩu DB.
- Đã đóng browser `bf03-assignment`, xóa sessionStorage kiểm thử; dừng backend PID 23120 và Vite PID 48260 do phiên này mở sau khi kiểm tra command line. Backend test dùng khóa JWT test qua tham số chạy, không sửa cấu hình local.

### Đã bổ sung sau khi được phép

- `ScoringAppealService`, DTO `ScoringAppealAttemptResponse`, query khóa tại `ScoringAttemptRepository`: tạo phiếu APPEAL trắng đúng snapshot gốc, giao lại cùng phiếu/số vòng, audit giữ điểm/nhận xét nháp cũ trước khi xóa khỏi phiếu, kiểm tra người đang sở hữu. Không sửa assignment gốc, điểm chính thức hoặc trạng thái bài.
- `NotificationService.notifyUser`: thông báo một người, cùng transaction với thao tác phân công/giao lại; nội dung không chứa điểm/lý do khiếu nại.
- `AppealManagementService`, controller và DTO Assign/Reassign/Candidate/Management; khóa appeal, kiểm tra Head cùng ban, trạng thái, expected reviewer/attempt; ưu tiên cùng ban, khác ban chỉ khi không còn ứng viên cùng ban và phải có lý do; lịch sử người cũ lấy thêm từ audit.
- UC42 thêm `pendingAppealReviewCount`, nên workload hiện có cả nháp APPEAL đang chờ (thay thế giới hạn cũ ghi bên dưới).
- Hai màn `AppealManagementPage`, `AppealDetailPage` và CSS cạnh trang; API client bổ sung vào appealApi; AppRoutes thay placeholder Head `/appeals` và `/appeals/:id`.
- AuditLogPage nhận appealId từ link chi tiết; AppealAuditQueryService lọc được BTV mới/cũ trong log phân công/giao lại kể cả phiếu chưa nộp.

### Bằng chứng và lỗi chưa sửa

- Backend compile thành công. **Lần suite xanh gần nhất: 126 test**, 0 fail/error/skip, log `server/target/bf03-iter12-final.log` (13:35:54). Bao gồm 8 integration test quản lý: snapshot, quyền/ban, cùng/khác ban, stale owner, dữ liệu nháp/audit/thông báo, rollback khi notify lỗi ở cả assign và reassign, khóa thực qua hai transaction, resolved/locked/wrong role.
- Sau review đã thêm regression thứ 9: `BF03AppealManagementIT.lockingReloadsDraftAlreadyCachedBeforeAnotherWriterSubmittedIt`. **Lần chạy mới nhất ĐỎ: 1 test, 1 failure**: mong BusinessException nhưng không có. Không được báo suite hiện tại xanh. Log `server/target/bf03-stale-lock-red.log`; test tự rollback.
- Nguyên nhân Important đã xác nhận: `AppealManagementService.currentAttempt()` load draft vào persistence context trước lúc lấy khóa; query PESSIMISTIC_WRITE không tự refresh entity đã cache. Nếu scorer lưu/nộp ở giữa, `ScoringAppealService.lockedDraft()` có thể kiểm tra dữ liệu cũ và xóa dữ liệu mới khi giao lại. Regression preload entity → JDBC cập nhật submitted_at/score/note → reassignDraft đã chứng minh thiếu recheck. **Chưa sửa vì usage chạm ngưỡng.**
- Hướng sửa tiếp: inject EntityManager vào ScoringAppealService, refresh draft với `LockModeType.PESSIMISTIC_WRITE` trước mọi kiểm tra/snapshot; cập nhật constructor unit test và chạy regression RED→GREEN rồi toàn suite. Rà lại thứ tự khóa/các caller `requireOwnedDraft`. Không bỏ/disable test đỏ để báo đạt.
- Review còn 2 lỗi UI cần test trước/sửa: (1) ReviewerProfilePage đổi tháng/trang làm mất `location.state.boLoc`, quay lại báo cáo mất bộ lọc; truyền state khi `doiThamSo`. (2) AuditLogPage đổi URL từ `?appealId=...` về menu `/audit-logs` giữ form cũ nhưng load toàn bộ log; đồng bộ query và form (có thể remount nội dung theo search hoặc quản lý filter theo URL), tránh hiển thị sai phạm vi. Reviewer gọi Minor; đây là phần luồng đã hứa nên cần xử lý trước nghiệm thu.
- Frontend mới nhất **13/13 Node test**, build 122 module thành công, lint 4 warning ngoài BF03, không warning mới BF03. git diff --check sạch; schema/data không đổi.
- Browser đọc API thật bằng JWT test Head: danh sách → chi tiết seed appeal1 → ứng viên khác ban, bắt buộc lý do; link audit áp dụng appealId (RED→GREEN). Không thay tài khoản chung. Login Head mẫu lỗi vì hash không phải BCrypt — ngoài scope, chưa sửa.
- Browser nhánh ghi: API đọc thật, sau đó chặn riêng ID giả 900001 để mô phỏng assign/reassign/409. Kết quả `{passed:true, interceptedWrites:2, realDatabaseWrites:0}`; không coi đây là browser ghi DB thật. Scripts/snapshots ngoài Git tại thư mục visualization đã ghi dưới đây; các mock đã gỡ.

### Rà soát độc lập và cách tiếp tục

Reviewer `/root/review_bf03_iter12` chỉ đọc, không sửa DB/source: không Critical, 1 Important freshness của khóa, 2 Minor filter như trên. Không cần gửi lại reviewer sau fix; viết regression, sửa một lượt rồi chạy full suite. Các mục reviewer không xét: UC36 tương lai ngoài scope; Figma/browser do primary kiểm tra; tài liệu đang cập nhật, seed encoding/password, lint ngoài BF03 và đọc tổng hợp in-memory do quy mô môn học. Quyết định giữ các giới hạn này: không mở Iter3 hoặc sửa dữ liệu người khác; chi phí là các vấn đề seed vẫn còn khi demo login.

Lệnh tiếp tục: `mvn -f server/pom.xml "-Dtest=*Test,BF03Iteration1IT,BF03ReviewerProfileIT,BF03AppealManagementIT" test`; phải đạt cả regression thứ 9. Chạy Node tests/build/lint, kiểm tra lại browser filter; cập nhật thiết kế/source map cuối cùng, chỉ đánh dấu đủ Iter1–2 khi các lỗi trên được xử lý. Không commit/push khi chưa được yêu cầu.

## Trạng thái an toàn

- Repo: `C:\Users\Admin\IdeaProjects\swp391`; nhánh `VuNQ`; HEAD `acba541`.
- Source và test mới đang nằm trong working tree, chưa commit/push; không stash hoặc xóa thay đổi.
- Không sửa `database/schema.sql`, `database/data.sql`, không reset/seed DB, không đổi `ddl-auto`, không thêm thư viện.
- Backend/Vite và phiên trình duyệt do phiên kiểm chứng này mở đã dừng/đóng.
- Dừng vì cần quyền sửa phần thuộc BF02/service dùng chung, không phải vì usage đã còn 10%. Lần đo cuối phiên: cửa sổ chính dùng 43% (còn 57%), cửa sổ thứ hai dùng 17% (còn 83%). Kiểm tra lại usage trước khi tiếp tục.

## Đã thực hiện

1. UC44: sửa `goiApi.js` giữ `Error.status` ở hai nhánh lỗi HTTP, theo chấp thuận riêng; giữ message/JWT/payload như cũ. Trạng thái active-config 404 không làm mất lịch sử cấu hình.
2. Iter1: bổ sung kiểm chứng cấu hình tương lai, binding appeal cũ, audit old/new và validation; chạy hồi quy test UC35/41/44/45 hiện có. Không viết lại backend đã hoạt động.
3. UC42: directory BTV độc lập với chỉ số; hồ sơ, lịch sử chấm phân trang/lọc tháng, chấm chéo và chỉ số chỉ đọc; Head cùng ban/Admin các ban. Không có so sánh thì hiển thị rỗng.
4. UI: liên kết từ báo cáo/directory sang hồ sơ; tháng/trang nằm trên URL; quay lại giữ bộ lọc báo cáo. Tái dùng kiểu trang BF03 hiện có.

Workload UC42 hiện gồm phân công gốc có bài `ASSIGNED` và chấm chéo `PENDING`. **Chưa có số phiếu APPEAL đang chờ**, vì chưa có service BF02 cung cấp dữ liệu nháp. Không xem đây là tổng mọi loại công việc.

## Bản đồ source mới/thay đổi

- `client/src/api/goiApi.js`: giữ status lỗi HTTP (file dùng chung đã được cho phép).
- `client/tests/appealRuleConfigApi.test.js`: 9 ca hồi quy API client.
- `client/src/api/reviewerProfileApi.js`, `client/tests/reviewerProfileApi.test.js`: URL/JWT và lỗi UC42, 2 ca.
- `client/src/pages/appeal/ReviewerProfilePage.jsx` và `.css`: màn hồ sơ.
- `client/src/pages/appeal/ReviewerDeviationPage.jsx`: directory và đường dẫn hồ sơ.
- `client/src/routes/AppRoutes.jsx`: thay placeholder hồ sơ Head/Admin, bỏ route Admin trùng; không sửa route các luồng khác.
- `server/src/main/java/com/swp391/kpi/controller/ReviewerProfileController.java`: 3 endpoint directory/hồ sơ/lịch sử, actor từ JWT.
- `server/src/main/java/com/swp391/kpi/service/ReviewerProfileService.java`: quyền, tháng/phân trang, tổng hợp dữ liệu qua service BF02, không ghi chỉ số.
- DTO: `ReviewerDirectoryResponse`, `ReviewerProfileResponse`, `ReviewerScoringHistoryResponse`, `ReviewerCrossReviewResponse` trong `server/src/main/java/com/swp391/kpi/dto/`.
- `server/src/test/java/com/swp391/kpi/service/ReviewerProfileServiceTest.java`: 9 unit test.
- `server/src/test/java/com/swp391/kpi/BF03ReviewerProfileIT.java`: 2 integration test, rollback.
- `server/src/test/java/com/swp391/kpi/BF03Iteration1IT.java`: bổ sung 2 ca, tổng 7 ca.

Luồng màn hình và ý nghĩa từng tầng: [thiết kế/source map](../specs/2026-10-19-bf03-iter1-iter2-design.md).

## Bằng chứng kiểm tra

| Kiểm tra | Kết quả |
|---|---|
| `mvn -f server/pom.xml compile` | Thành công |
| `mvn -f server/pom.xml "-Dtest=*Test,BF03Iteration1IT,BF03ReviewerProfileIT" test` | 114 test, 0 fail/error/skip |
| `node --test tests/*.test.js` tại client | 11/11 đạt |
| `npm run build` tại client | Thành công, 118 module |
| `npm run lint` tại client | Exit 0, 4 warning có sẵn ngoài BF03 |
| `git diff --check` | Không lỗi whitespace |
| Diff schema.sql/data.sql | Không thay đổi |

Maven trên máy: `C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/plugins/maven-plugin/lib/maven3/bin/mvn.cmd`.

Trình duyệt thật kiểm tra: Admin đăng nhập → UC44 active/history → directory → hồ sơ Editor → đổi tháng → audit; Reporter đăng nhập → chọn bài mẫu → xem tiêu chí/điểm → thử gửi thiếu tiêu chí. Không tạo appeal hoặc cấu hình thật trong trình duyệt. Nhánh ghi thành công được kiểm tra bằng integration transaction rollback, không phải một lượt browser end-to-end ghi thật.

Mô phỏng riêng response 404 cho GET active-config trên trình duyệt xác nhận trạng thái trống và lịch sử cùng hiển thị; đã bỏ mock/đóng phiên sau thử. Snapshot nằm ngoài Git tại `C:/Users/Admin/.codex/visualizations/2026/10/14/01a137cf-5414-7242-8355-1dbd70c622fb/.playwright-cli/`.

Cảnh báo ngoài phạm vi: 4 lint warning ở `flow2/EditorTaskQueue`, `flow2/CriteriaManagement` (2), `kpi/MonthlyTargetSetting`; dữ liệu tên user/role/ban đang có mojibake. Không tự sửa phần người khác/dữ liệu dùng chung.

## Chưa hoàn tất và quyền cần xin

UC38 tối thiểu và UC43 chưa có API/màn phân công/giao lại thật. Thiết kế được duyệt không tự cho phép sửa code thuộc người khác theo AGENTS.md.

Cần người dùng cho phép đúng hai điểm:

1. **BF02:** bổ sung service tích hợp dự kiến `ScoringAppealService` (và phương thức repository cần thiết sau khảo sát) để đọc snapshot gốc, đọc/tạo phiếu APPEAL chưa nộp, khóa và giao lại có kiểm tra người cũ/trạng thái. Không tạo entity BF03 trùng bảng, không sửa phân công gốc hoặc điểm chính thức. Dữ liệu nháp cũ phải được xử lý an toàn và lưu audit, không để người mới nhìn thấy điểm/nhận xét nháp của người cũ.
2. **Dùng chung:** bổ sung hàm gửi thông báo riêng một người trong `NotificationService`, thay vì gửi cả role; thông báo giao lại cho BTV cũ/mới không chứa điểm cũ hoặc lý do khiếu nại.

Sau khi được phép: đọc lại schema/comment và service BF02 liên quan; chốt hợp đồng nhỏ, viết test trước; triển khai lần lượt hàng chờ/chi tiết/ứng viên, phân công ban đầu rồi giao lại. Khóa appeal và phiếu trong transaction, kiểm tra trạng thái/expected owner sau khóa; audit/thông báo rollback cùng thao tác. Head chỉ quản lý ban của bài; ngoại lệ khác ban phải đúng điều kiện/lý do. Không tăng số vòng chấm khi giao lại. UC36/37/39/40 vẫn là Iter3.

Chỉ đánh dấu toàn bộ Iter2 hoàn tất sau test đồng thời/rollback/quyền và kiểm chứng API/UI của UC38/43. Không commit/push nếu chưa có yêu cầu mới.
