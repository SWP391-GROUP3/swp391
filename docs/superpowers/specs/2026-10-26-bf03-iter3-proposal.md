# BF03 Iter 3 — Đề xuất triển khai và điểm cần chốt

Ngày: 26/10/2026. Nhánh: `VuNQ`, sau khi cập nhật `origin/main` tại `40386e9`.

Trạng thái ban đầu: đề xuất để duyệt. Người dùng đã đồng ý ngày26/10/2026; đã triển khai phần BF03. Bằng chứng cuối xem `../checkpoints/2026-10-26-bf03-iter3-checkpoint.md`, hướng dẫn xem `../../BF03_ITER3_CODE_GUIDE.md`.

## 1. Mục tiêu

Hoàn thiện BF03 cả backend và frontend theo SRS BF03 hiện tại:

| UC | Kết quả cần có |
|---|---|
| 36 | Editor xem đúng phiên bản bài và tiêu chí; lưu nháp, nộp và khóa S1 |
| 37 | Khi ABS(S1-S0) vượt ngưỡng đã gắn, Head giao S2 cho người độc lập; không có S3 |
| 38 | Mở rộng hàng chờ/chi tiết đã có để theo dõi các giai đoạn đến RESOLVED |
| 39 | Head xem so sánh từng tiêu chí, kiểm tra đề xuất và xác nhận điểm số hiệu lực |
| 40 | Head tạo đợt chấm chéo; Editor nộp kết quả; đóng đợt khi hết task PENDING |

Giữ nguyên Iter1–2, không sửa nội dung bài, điểm chấm gốc, trạng thái xuất bản hoặc dữ liệu tài chính đã khóa. Tiếp tục cập nhật hướng dẫn luồng màn hình, ý nghĩa code và bằng chứng kiểm chứng.

## 2. Hiện trạng đã kiểm tra

- `AppealManagementService`: có hàng chờ, chi tiết, phân công đầu và giao lại. Hiện dừng chọn ứng viên khi phiếu hiện tại đã nộp, nên chưa phân công S2 được.
- `ScoringAppealService`: có tạo nháp, giao lại và kiểm tra chủ phiếu dưới khóa; chưa có API chấm mù, lưu nháp hoặc nộp. `createDraft()` hiện từ chối mọi phiếu APPEAL tiếp theo của cùng appeal.
- `AppealResolution`: đã ánh xạ `effective_score`, nhưng chưa có `ruleApplied` và `finalizedBy` để ghi một kết quả hoàn chỉnh.
- `ReviewerDeviationRefreshService`: đã đọc điểm số hiệu lực của resolution, có fallback cho dữ liệu cũ và đọc task chấm chéo đã hoàn tất.
- `ScoringReadService.getPublishedAttempt()`: hiện đọc điểm công bố của BF02, chưa cung cấp điểm số hiệu lực sau khiếu nại.
- `AppRoutes.jsx`: `/re-reviews`, `/re-reviews/:id`, `/cross-reviews` vẫn là màn ComingSoon.
- Không thấy thư mục `docs/ui-images/`. Đề xuất dùng lại bố cục, CSS, bảng và form BF03 hiện có theo yêu cầu trước đây của người dùng; không xây phong cách mới.

## 3. Mâu thuẫn cần quyết định trước khi code

### 3.1 Phiếu APPEAL và quyết định xuất bản

`database/schema.sql`, CHECK `ck_scoring_attempts_submitted`, yêu cầu mọi phiếu đã nộp có `decision`. SRS BF03 chỉ yêu cầu chấm điểm, không yêu cầu Editor khiếu nại quyết định ACCEPT/REVISION/REJECT.

Đề xuất: phiếu APPEAL đã nộp phải có `total_score`, nhưng `decision` để NULL; phiếu INITIAL/RESCORE giữ nguyên yêu cầu có quyết định. Giữ `assignment_id` trỏ phân công gốc như Iter2, không tạo phân công xuất bản giả.

### 3.2 Mã luật kết quả còn theo thiết kế cũ

Schema/comment hiện chỉ cho `KEEP_ORIGINAL`, `USE_RESCORE`, trong khi BR-QC-06/07 quy định S1 hoặc trọng tài S2. Đề xuất bổ sung bốn mã:

- `USE_S1`: ABS(S1-S0) <= T, điểm cuối = S1.
- `AVERAGE_S0_S2`: ABS(S2-S0) < ABS(S2-S1), điểm cuối = (S0+S2)/2.
- `AVERAGE_S1_S2`: ABS(S2-S1) < ABS(S2-S0), điểm cuối = (S1+S2)/2.
- `USE_S2_TIE`: hai khoảng cách bằng nhau, điểm cuối = S2.

Giữ hai mã cũ để đọc dữ liệu lịch sử. Kết quả mới bắt buộc có `effective_score`; trung bình dùng HALF_UP, hai chữ số thập phân. `effective_attempt_id` giữ vai trò phiếu tham chiếu: S1 cho USE_S1, S2 cho các nhánh trọng tài; các bên tính tiền/KPI phải đọc `effective_score`, không suy ra điểm cuối chỉ từ FK này.

SRS Overview GBR-07 và một số comment còn mô tả trung vị/giữ điểm gốc. Đề xuất áp dụng SRS BF03 đã chốt, đồng bộ mô tả liên quan khi được phép; không tự chọn một công thức thứ ba.

### 3.3 Phạm vi quyền thay đổi

Quyền dùng database chung đã được người dùng cho phép. Việc sửa schema và mở rộng code BF02/file dùng chung vẫn cần xác nhận rõ theo AGENTS.md và giới hạn trước đây không sửa schema.

Đề xuất cho phép đúng các điểm sau:

1. Cập nhật hai CHECK ở trên trong `schema.sql`, kèm migration ALTER có transaction, kiểm tra cấu trúc và dữ liệu trước khi đổi; không DROP TABLE, TRUNCATE hay chạy lại seed/schema.
2. Mở rộng `ScoringAppealService`, DTO/repository BF02 phục vụ snapshot, nháp, nộp S1/S2 và đọc nội dung đúng `version_id`. Không sửa luồng chấm gốc.
3. Gắn các route BF03 trong `client/src/routes/AppRoutes.jsx`; chỉ bổ sung menu nếu menu hiện tại thiếu.
4. Dùng service audit/thông báo hiện có; chỉ sửa service chung nếu khảo sát chứng minh thiếu hợp đồng, nêu rõ trước khi sửa.

## 4. Cách triển khai được đề xuất

Tái sử dụng Controller → Service → Repository, JWT, `goiApi`, transaction và cơ chế khóa đã có. Không thêm thư viện, kiến trúc hoặc entity thứ hai ánh xạ bảng BF02.

So với cách ép lưu ACCEPT và dùng hai mã luật cũ, sửa CHECK và bổ sung mã luật phản ánh đúng nghiệp vụ, tránh lẫn quyết định xuất bản với điểm khiếu nại. Tách một hệ thống phiếu chấm BF03 mới sẽ trùng BF02 và cần thay đổi DB lớn hơn, nên không đề xuất.

Thứ tự dự kiến sau khi các điểm mục 3 được duyệt:

1. Kiểm tra read-only cấu trúc database thật; chuẩn bị và kiểm chứng migration cộng dồn, giữ dữ liệu cũ.
2. Viết calculator thuần cùng test cho trọng số, ngưỡng và trọng tài S2.
3. Hoàn thiện hợp đồng BF02 cho snapshot/nháp/nộp; test quyền, dữ liệu và khóa.
4. Mở API cùng màn chấm mù của Editor; chỉ trả nội dung đúng phiên bản, tiêu chí và nháp của chính người đang được giao.
5. Mở rộng UC38 để giao S2, so sánh UC39 và xác nhận một lần; audit/thông báo trong cùng transaction.
6. Hoàn thiện UC40 từ tạo đợt, phân công, chấm đến đóng; kết quả chỉ phục vụ phân tích.
7. Cập nhật báo cáo, hồ sơ và kết quả hiển thị cho Reporter; bổ sung hướng dẫn nguồn code và luồng màn.
8. Compile, unit/integration test và kiểm chứng trình duyệt với JWT thật. Chỉ kết luận hoàn tất phần đã có bằng chứng.

Đây là thứ tự công việc, chưa phải kế hoạch hàm/file chi tiết đã được phê duyệt.

## 5. Luồng màn hình và hợp đồng dự kiến

| Người dùng | Luồng |
|---|---|
| Head | `/appeals` → `/appeals/:id` → phân công → so sánh → giao S2 nếu cần → xác nhận → audit |
| Editor | `/re-reviews` → `/re-reviews/:id` → đọc bài/tiêu chí → lưu nháp → nộp → chỉ đọc phiếu của mình |
| Head | `/cross-reviews` → tạo đợt → chọn bài/người chấm → theo dõi → đóng đợt |
| Editor | danh sách chấm lại/chấm chéo → task chấm chéo → nộp kết quả phân tích |
| Reporter | Khiếu nại của tôi → theo dõi trạng thái và điểm hiệu lực đã được Head xác nhận |

API cụ thể sẽ được chốt theo convention hiện tại trong kế hoạch chi tiết. Không dùng DTO chi tiết Head cho Editor: phải ẩn điểm/nhận xét cũ, reviewer cũ, tiêu chí bị khiếu nại, lý do và kết quả so sánh ở phía server.

## 6. Điểm tích hợp ngoài BF03

- Cung cấp service điểm hiệu lực: dùng `appeal_resolutions.effective_score` khi RESOLVED; khi còn xử lý dùng điểm công bố cũ.
- BF04 hiện dùng `ScoringReadService`. Không tự mở rộng quyền sửa toàn bộ BF04 chỉ vì được dùng database chung.
- Tính lại tiền cho OPEN, bút toán ADJUSTMENT cho LOCKED/PAID và cách xử lý CLOSED cần hợp đồng với chủ BF04. Không ghi trực tiếp vào bảng tài chính để đánh dấu hoàn thành BF03.
- BF05 cần nhận điểm mới cho DRAFT và giữ FINALIZED bất biến; xác định service tích hợp trước khi ghi.
- Nếu các hợp đồng chưa có, phải ghi rõ phần tích hợp còn chờ và không tuyên bố Iter3 đầy đủ từ đầu đến cuối. Có thể cung cấp đầu ra BF03 và test hợp đồng bằng double trong lúc chờ chủ luồng.

## 7. Kiểm chứng bắt buộc

- S1: đủ tiêu chí, ID thuộc bộ đúng phiên bản, điểm 0–100, comment bắt buộc dưới 50; tổng trọng số và HALF_UP đúng; không nhận tổng do client tự khai.
- Ngưỡng: bằng T không tạo S2; tăng/giảm vượt T đều bắt buộc S2; không cho vòng thứ ba.
- Trọng tài: đầy đủ ba nhánh, không dùng median và không chặn điểm thấp hơn S0.
- Blind: kiểm tra response JSON, không chỉ ẩn trên UI; người khác/Editor cũ sau giao lại không đọc/ghi được phiếu.
- Đồng thời: khóa theo một thứ tự thống nhất; nộp/giao lại/chốt trùng chỉ một request thành công; lỗi audit/thông báo rollback.
- Cross-review: trùng bài trong đợt, người chấm gốc/tài khoản khóa, task chưa hoàn tất, đóng khi còn PENDING; không đổi điểm chính thức.
- Regression Iter1–2 và test service thật với fixture rollback. Không chạy lại `data.sql` trên database chung.
- Kiểm tra mức sử dụng; còn <=10% thì dừng an toàn và cập nhật checkpoint theo yêu cầu trước.

## 8. Tình trạng tại lúc gửi đề xuất

Chỉ thêm tài liệu đề xuất này. Chưa sửa code, schema hoặc database thật; chưa commit/push. Cần chốt mục 3 trước khi xây dựng chức năng.

Sau đó người dùng đã duyệt toàn bộ đề xuất; mục này giữ để phân biệt thời điểm trước duyệt với kết quả thực thi trong checkpoint.
