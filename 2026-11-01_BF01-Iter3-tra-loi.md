# Trả lời "BF-01 (Minh) – Cần chốt để làm nốt Iter 3"

> Ngày 01/11/2026 · Người trả lời: Quang (nhóm trưởng, BF-04).
> Trả lời theo số dòng trong file của Minh. Dòng nào ghi "OK" là dùng đúng cách Minh đề xuất.

## A. Cần nhóm / người khác làm hoặc quyết

| # | Trả lời |
|---|---|
| A1 | **OK**: `server/uploads/` (thêm vào `.gitignore`), tối đa 10 MB/file, ảnh chỉ JPG/PNG, dHash bằng `ImageIO` (không thêm thư viện). Cấu hình multipart trong `application.properties` là file dùng chung, PR ghi `[COMMON]` |
| A2 | **(b)**: thêm hàm chung trong `goiApi.js` tải file dạng blob kèm token. Không mở `/api/files/**` cho người chưa đăng nhập. PR ghi `[COMMON]` |
| A3 | **OK**: video nhập link, `file_hash` = SHA-256 của link đã chuẩn hóa (chữ thường, bỏ `/` cuối) |
| A4 | **OK**: Minh thêm `ForbiddenException` và hàm trả 403 trong `GlobalExceptionHandler`, PR ghi `[COMMON]` |
| A5 | **OK**: thêm `/articles/:id` vào nhóm route Biên tập viên và Trưởng ban, dùng lại `ArticleDetail.jsx`. Việc gắn link từ hàng chờ / phiếu chấm: Dương trả lời |
| A6 | Mình sẽ **giao người làm backend** luồng `admin` (sửa tham số, API, ghi `audit_logs`) và báo lại trong nhóm. **OK** các giới hạn: số tác giả 1–5, số ngày xác nhận ≥ 1, 0 < ngưỡng cảnh báo < ngưỡng chặn ≤ 100, dHash 0–64. Minh làm màn hình |
| A7 | **Đã thêm** sự kiện mẫu "Chuyển đổi số quốc gia 2026" (25/10–31/12/2026, `window_days` 7, `k_limit` 2, `exempt_score` 90). `k_limit` = 2 để demo dễ thấy bão hòa: bài 1–2 được 100%, 3–4 được 80%, từ bài 5 được 60%. Nhánh `feature/bf04-iter3-sample-event`. DB đã có dữ liệu thì chạy `database/migrations/BF04_iter3_sample_event.sql` |
| A8 | Dương làm (1) và (2) **trước khi** Minh code kiểm trùng. (3) giữ `MAJOR` |
| A9 | Chờ Dương xác nhận `layPhieuChamMoiNhat` có trả lần chấm `REVISION` không. Nếu không, Dương cấp 1 hàm đọc để Minh gọi |
| A10 | **OK** như đề xuất 01/11: (A5) cho luồng khác **đọc** thẳng repository BF-01, ghi phải qua `ArticleService`; (A6) 4 file `ArticleRead…` thuộc BF-03. Minh mở PR `[COMMON]` ghi 2 ngoại lệ này vào AGENTS mục 2, mình duyệt |
| A11 | **Minh nối API thật** cho trang chủ Phóng viên (3 API đã có). File dùng chung nên báo nhóm, PR ghi `[COMMON]` |

## B. Cách hiểu nghiệp vụ

| # | Trả lời |
|---|---|
| B1–B8 | **OK** cả |
| B9–B11 | **OK** |
| B12 | **Vẫn đếm** bài `REJECTED`. Bài đã nộp thật, hạng của các bài khác đã khóa rồi, đếm thì đơn giản và nhất quán |
| B13 | **OK**. Sửa câu BR-IN-03 trong `docs/SRS_BF01_Article.md` qua PR |
| B14 | **OK**. Gợi ý: kiểm "sự kiện còn mở" bằng cách xem `eventId` có trong `EventService.getOpenEvents()` không (đã lọc `is_active` và ngày hôm nay nằm trong `start_date`–`end_date`) |
| B15 | **OK**: BF-04 xét miễn giảm lúc chấm (và trong `estimateRoyalty`) |
| B16 | **OK**. Cách gọi: đếm hạng nếu nộp lúc này (B10), rồi gọi `EventService.getSaturationRate(eventId, rank)`, rồi gọi `RoyaltyGradingService.estimateRoyalty(declaredCategoryId, declaredNature, eventId, rate)`. Không chọn sự kiện thì truyền `eventId` = null, `rate` = null |
| B17 | **OK** |
| B18 | **OK** |
| B19 | Trưởng ban xem bài **cùng ban** (`articles.section_id` = ban của Trưởng ban), trạng thái khác `DRAFT`. Khớp với hàng chờ phân công và KPI theo ban |
| B20 | **OK** |
| B21–B23 | **OK** |

## C. Thứ tự làm

**OK** thứ tự của Minh. Bước 2 (sự kiện + bão hòa) không còn phải chờ A7.
