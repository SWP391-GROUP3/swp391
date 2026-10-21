# BF03 UC44 Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Project instructions override automatic commits, continuous execution and delegation: work on VuNQ, no commit/push without request, one small code step then report.

**Goal:** Kiểm chứng UC44 và sửa lỗi làm mất HTTP status khiến màn cấu hình không nhận ra trường hợp chưa có bản luật hiệu lực.

**Architecture:** Giữ Controller → Service → Repository và API client qua goiApi hiện có. Bổ sung metadata status vào Error, không đổi URL, payload, message hay cách gắn JWT; không tạo API client riêng cho BF03.

**Tech Stack:** Spring Boot 3.5, Java 17, JUnit 5, React 19, JavaScript, Node built-in test runner, Vite, SQL Server.

**Spec:** [Thiết kế BF03 Iter1–Iter2](../specs/2026-10-19-bf03-iter1-iter2-design.md), các mục 3, 4.2, 6 và 9. Kế hoạch này chỉ chi tiết mốc UC44; các mốc UC45/41/35/42/38/43 không tự được triển khai trong bước hiện tại.

## Global Constraints

- Nhánh thực hiện: `VuNQ`.
- Không sửa `database/schema.sql`, không đổi `ddl-auto=validate`, không thêm thư viện, không tự chạy lại `data.sql` trên DB đang dùng.
- Không sửa entity/repository/service của BF02, cấu hình bảo mật hay service dùng chung nếu chưa được chấp thuận riêng cho đúng điểm tích hợp.
- Không tự commit/push. Mỗi bước code nhỏ có kiểm tra và báo kết quả trước khi sang bước tiếp theo theo AGENTS.md.
- Phải hoàn tất đọc Bước 0 trước sửa source đầu tiên; không cần đổi bố cục UI cho lỗi này.

## Review Focus

1. API trả 404: Error giữ status 404 để màn UC44 nhận ra chưa có cấu hình; không biến mọi 404 thành thành công.
2. API trả 400 kèm errors: giữ status 400 và thông báo từng trường bằng tiếng Việt.
3. API trả 401 khi có token: vẫn xóa phiên/chuyển về login, đồng thời giữ status 401 trong Error.
4. API trả 500 với body không phải JSON: vẫn trả lỗi dự phòng, không vỡ ở bước đọc JSON và giữ status 500.
5. API trả 200/201 thành công: vẫn trả json.data, giữ token và serialize body như cũ.

## Kết quả khảo sát trước triển khai

- [x] Đọc `AppealRuleConfigPage.jsx`, `appealRuleConfigApi.js`, `goiApi.js`, DTO và test service UC44.
- [x] Chạy probe Node qua API client thực tế với fetch trả HTTP 404. Assert error.status === 404 thất bại vì actual undefined.
- [x] Xác định chuỗi gọi: Page bắt loi.status === 404 → getActiveConfig → goiApi chỉ throw new Error(message), không gắn status.
- [x] Chấp thuận sửa file dùng chung `client/src/api/goiApi.js`: người dùng trả lời “Cho phép”. Chỉ bổ sung Error.status, giữ nguyên hành vi khác.

### Task 1: Giữ HTTP status mà không đổi hợp đồng API cũ

**Files:**
- Create: `client/tests/appealRuleConfigApi.test.js`.
- Modify, chỉ sau chấp thuận: `client/src/api/goiApi.js`.
- Reference only: `client/src/api/appealRuleConfigApi.js`, `client/src/pages/admin/AppealRuleConfigPage.jsx`.

**Interfaces:**
- Consumes: `goiApi(duongDan, tuyChon = {})`, `getActiveConfig()`, `getAllConfigs()`, `createConfig(data)`.
- Produces: thành công vẫn trả json.data; lỗi vẫn là Error với message cũ, thêm thuộc tính số status từ HTTP response. Lỗi mạng không có response thì không tự đặt HTTP status.

- [x] Step 1: Dùng node:test và node:assert/strict, tạo regression `missingActiveConfigPreserves404`. Gọi getActiveConfig với fetch giả HTTP 404; assert Error.status = 404 và message giữ nguyên. Tự khôi phục fetch/sessionStorage/window sau mỗi ca, không chạy song song các ca sửa global.
- [x] Step 2: Chạy `node --test tests/appealRuleConfigApi.test.js` trong client. Xác nhận ca 404 thất bại vì status undefined, tương ứng probe đã quan sát.
- [x] Step 3: Trong goiApi, ở hai nhánh lỗi có HTTP response (401 hết phiên và nhánh lỗi chung), tạo Error giữ message hiện tại, gán status = res.status rồi throw. Không sửa hàm đọc JSON, URL hoặc cơ chế phiên.
- [x] Step 4: Bổ sung `validationKeeps400AndFieldMessages`, `expiredSessionKeeps401AndRedirects`, `nonJsonFailureKeeps500AndFallback`, `successKeepsDataJwtAndRequestBody`; kiểm chứng đúng năm điều kiện Review Focus. Assert token/JSON payload trên request, trạng thái phiên và URL chuyển hướng; không in token thật.
- [x] Step 5: Chạy toàn bộ `node --test tests/*.test.js` nếu thư mục chỉ có các test tương thích Node; nếu repo đã có runner khác, dùng runner hiện có và ghi lại lệnh. Chạy `npm run build`, `npm run lint`; báo lỗi có sẵn riêng. Không thêm dependency.
- [x] Step 6: Cập nhật hướng dẫn với kết quả thực tế. Dừng báo cáo bước này; chưa commit/push và chưa tự sang Task 2.

### Task 2: Kiểm chứng nghiệp vụ UC44 ở backend

**Files:**
- Inspect: `server/src/main/java/com/swp391/kpi/service/AppealRuleConfigService.java`, `dto/AppealRuleConfigCreateRequest.java`, `repository/AppealRuleConfigRepository.java` cùng package gốc.
- Test: `server/src/test/java/com/swp391/kpi/service/AppealRuleConfigServiceTest.java`.
- Integration reference: `server/src/test/java/com/swp391/kpi/BF03Iteration1IT.java`.

**Interfaces:** giữ getActiveConfig(currentUserId), getAllConfigs(currentUserId), createConfig(request, currentUserId). Chưa quyết định sửa service: chỉ sửa nếu có test chỉ ra sai so với SRS.

- [ ] Step 1: Kiểm tra test hiện có, xác định ca còn thiếu: Admin/khác role, chọn bản hiệu lực thay vì bản tương lai, trùng ngày, old/new audit, không sửa entity bản cũ.
- [ ] Step 2: Bổ sung từng ca còn thiếu vào đúng test hiện có, mỗi lượt một ca. Không đổi test để chấp nhận lỗi production; test phải assert dữ liệu/quyền thực tế, không chỉ verify mock có tồn tại.
- [ ] Step 3: Chạy `mvn -f server/pom.xml compile` và `mvn -f server/pom.xml test`. Khi có DB test tương thích, chạy riêng `mvn -f server/pom.xml "-Dtest=*Test,BF03Iteration1IT" test`; fixture phải rollback.
- [ ] Step 4: Nếu cần sửa service/DTO, cập nhật bước sửa chính xác sau khi đọc lỗi và xác nhận root cause; không mở rộng sang business rule hoặc schema khác.

### Task 3: Nghiệm thu màn UC44 và cập nhật hướng dẫn

**Files:** sửa mô tả kết quả trong spec cùng thư mục docs; không tự đổi UI nếu chưa có defect.

- [ ] Step 1: Trên môi trường test, xác nhận Admin xem active/history, tạo bản tương lai, bản hiện tại vẫn được chọn; không tạo cấu hình vào DB người dùng khi chưa được phép.
- [ ] Step 2: Kiểm tra trạng thái không có bản hiệu lực bằng response giả ở test/UI hoặc DB test riêng, không tắt cấu hình thật để thử.
- [ ] Step 3: Xác nhận Reporter/Editor/Head không tạo cấu hình; appeal đã gửi không đổi rule_config_id khi tạo bản luật mới.
- [ ] Step 4: Ghi rõ kiểm tra nào đã chạy, môi trường, kết quả, file thực sự đổi; chỉ đánh dấu UC44 đạt khi đủ bằng chứng. Sau đó mới chuyển mốc UC45 trong thiết kế đã duyệt.

## Handoff

Người dùng đã cho phép sửa goiApi theo đúng phạm vi giữ HTTP status và sau đó yêu cầu làm liên tục, không dừng mỗi bước. Task 2–3 đã được kiểm chứng tiếp theo như nhật ký bên dưới; các checkbox chưa đánh dấu ở trên giữ lại mô tả kế hoạch gốc, không có nghĩa mọi thao tác chính xác trong kế hoạch đều đã được thực hiện. Không tạo subagent, không commit/push tự động. Cổng xin quyền sửa BF02/service dùng chung vẫn giữ nguyên.

## Nhật ký Task 1 — 19/10/2026

- Source thay đổi duy nhất: `client/src/api/goiApi.js`, thêm status vào Error ở nhánh 401 hết phiên và nhánh lỗi HTTP chung. Không đổi logic xóa phiên, redirect, message, URL, payload hoặc dữ liệu trả về.
- Test mới: `client/tests/appealRuleConfigApi.test.js`, dùng Node built-in, không thêm dependency.
- RED: 9 test chạy, 6 thất bại vì Error thiếu status, 3 ca thành công/mạng đã đạt.
- GREEN: `node --test tests/*.test.js` trong client → 9/9 đạt, không skip.
- `npm run build` → thành công, 115 module.
- `npm run lint` → exit 0, 4 cảnh báo có sẵn: EditorTaskQueue (dependency hook), CriteriaManagement (hai biến/tham số chưa dùng), MonthlyTargetSetting (tham số chưa dùng). Không sửa phần ngoài scope.
- Chưa chạy nghiệm thu màn hình bằng trình duyệt hoặc API backend thật trong Task 1. Không coi kết quả này là hoàn tất toàn bộ UC44.
- Không thay schema/database, không tạo commit/push. Bước sau là kiểm chứng nghiệp vụ backend UC44 theo Task 2 khi người dùng đồng ý tiếp tục.

## Kết quả Task 2–3 và kiểm chứng tổng hợp — 19/10/2026

- Đọc lại service/DTO/test UC44 hiện có; không tìm thấy lý do cần đổi production backend Iter1 trong phạm vi đã kiểm tra.
- Bổ sung 2 ca integration trong `BF03Iteration1IT`: cấu hình tương lai không thay bản hiệu lực/không đổi binding appeal cũ/audit old-new; từ chối giá trị cấu hình và khoảng ngày/tháng không hợp lệ. Dữ liệu kiểm thử rollback.
- Backend compile thành công. Lệnh `mvn -f server/pom.xml "-Dtest=*Test,BF03Iteration1IT,BF03ReviewerProfileIT" test` đạt 114 test, không fail/error/skip, gồm cả test UC42 mới.
- Trình duyệt đăng nhập Admin xem active/history UC44; giả lập riêng GET active trả 404 để xác nhận trạng thái chưa có cấu hình và lịch sử vẫn hiển thị. Không tạo cấu hình vào DB thật bằng trình duyệt; kiểm tra ghi bằng integration rollback.
- Quyền và binding appeal kiểm tra bằng bộ test backend; không tuyên bố đã thao tác tạo cấu hình trên trình duyệt bằng từng vai trò.
- Frontend hiện đạt 11 test, build thành công, lint chỉ còn 4 cảnh báo ngoài BF03. Không sửa schema/data.sql, không commit/push.
- Công việc tiếp theo không còn là sửa status UC44: UC42 đã có, UC38/UC43 chờ quyền cho điểm tích hợp BF02 và thông báo riêng người dùng. Xem [checkpoint](../checkpoints/2026-10-19-bf03-iter1-iter2-checkpoint.md).

