# BF03 UC38 tối thiểu và UC43 — thực thi

Spec: [thiết kế đã duyệt](../specs/2026-10-19-bf03-iter1-iter2-design.md).

Người dùng đã cho phép điểm tích hợp BF02 và NotificationService. Tiếp tục trên working tree `VuNQ` đang có UC42, giữ mọi thay đổi chưa commit; không tạo worktree mới hoặc commit/push. Không đổi schema/data.sql, không thêm dependency, không sửa assignments/điểm gốc. Không làm UC36/37/39/40 của Iter3.

## Công việc và hợp đồng

1. Test trước, bổ sung `ScoringAppealService` và query khóa của `ScoringAttemptRepository`: đọc snapshot, tạo đúng một phiếu APPEAL nháp từ lần gốc; giao lại cùng phiếu dưới khóa với expected reviewer/attempt. Lưu bản nháp cũ vào audit trước khi xóa điểm nháp, không tăng số vòng. Cung cấp kiểm tra chủ phiếu cho Iter3.
2. Bổ sung `NotificationService.notifyUser`: chỉ một người nhận, cùng transaction với caller.
3. Test trước, bổ sung `AppealManagementService`, DTO và controller: Head cùng ban; danh sách/chi tiết/ứng viên, phân công SUBMITTED, giao lại IN_REVIEW; lý do bắt buộc khi giao lại/khác ban; loại mọi BTV đã tham gia (kể cả người cũ trong audit). Khác ban chỉ khi không còn ứng viên cùng ban.
4. Test API client trước; hai màn danh sách/chi tiết theo CSS BF03, thay placeholder trong AppRoutes. Không mở màn Head cho Editor/Admin.
5. Integration rollback: quyền, snapshot, race/stale, rollback, audit/thông báo, người cũ mất quyền; compile/test/build/lint, browser smoke không ghi dữ liệu thật; cập nhật source map/checkpoint.

## Ledger

- Pre-flight: bước 1 cung cấp DTO phiếu và thao tác khóa; bước 3 chỉ gọi service đó, không gọi repository BF02. Bước 2 tham gia transaction bước 3. Bước 4 dùng đúng DTO/URL bước 3.
- Ruling: tiếp tục working tree VuNQ theo yêu cầu người dùng; không tách worktree vì chứa công việc UC42 chưa commit cần tiếp nối.
- Quyền bảo mật kiểm ở service mỗi lần gọi; không sửa SecurityConfig dùng chung khi không cần thiết.
- Nhật ký thực thi sẽ bổ sung theo kết quả thật. Dừng an toàn khi một cửa sổ usage còn tối đa 10%.

## Ledger cập nhật cuối phiên

- Bước 1–4 đã có source, DTO/API/UI. Node test 13/13, build 122 module, 4 lint warning ngoài BF03. Suite xanh gần nhất 126 test; fixture ghi đều rollback, không schema/seed/commit/push.
- Browser đã kiểm tra GET thật và nhánh POST giả lập riêng ID test (không ghi DB thật). Head seed password không BCrypt; dùng JWT với khóa riêng trong tiến trình test, không đổi tài khoản.
- Ruling: các phân công/giao lại ghi audit vào entity `appeals` để query phạm vi theo ban, tránh mở endpoint đọc điểm cũ cho Editor. UC36 tương lai phải dùng kiểm tra chủ phiếu trong transaction và kiểm tra appeal status; chưa triển khai UC36 UI.
- Final review: reviewer độc lập chỉ đọc, không Critical; Important: entity JPA đã load trước khóa không được refresh, nguy cơ kiểm tra submittedAt cũ. Regression mới `lockingReloadsDraftAlreadyCachedBeforeAnotherWriterSubmittedIt` ĐỎ (1 failure); chưa sửa, không được coi suite hiện tại xanh.
- Ruling: hai finding filter được reviewer xếp Minor nhưng cần hoàn tất trước nghiệm thu vì thiết kế yêu cầu giữ bộ lọc khi quay lại và scope audit phải khớp form. Cần regression browser trước khi sửa: giữ state khi đổi tháng/trang UC42; đồng bộ form audit khi đổi URL/menu.
- Reviewer declined: UC36 endpoints, Figma/browser, docs đang cập nhật, seed password/mojibake, lint ngoài scope, in-memory aggregation. Giữ ngoài scope; chi phí là login Head mẫu chưa demo được bằng mật khẩu và Iter3 chưa có.
- Dừng an toàn: usage primary dùng 91%/còn 9%; giữ working tree và test đỏ làm điểm tiếp tục. Browser/session test và backend/Vite đã đóng/dừng. Xem checkpoint cho đúng source, lỗi và lệnh chạy tiếp. Chưa hoàn tất bước 5.
