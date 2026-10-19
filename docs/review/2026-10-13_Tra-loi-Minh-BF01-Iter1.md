# Trả lời Minh – Những việc cần chốt trước khi code BF-01 Iter 1

> Ngày 13/10/2026 · Trả lời file `2026-10-13_Can-chot-truoc-khi-code-BF01-Iter1.md`.
> **Trước khi code tiếp:** gộp `main` mới nhất vào nhánh `MinhTH`. `main` vừa có khung trang Phóng viên (PR #10), bộ tính nhuận bút + `SystemParameter` (PR #9) và bản sửa conflict `Main.jsx` (PR #12).
>
> ```bash
> git fetch origin
> git merge origin/main
> ```

---

## A. Mình trả lời

| # | Vấn đề | Trả lời |
|---|---|---|
| A1 | Gắn màn BF-01 vào app | **Không sửa `Main.jsx` nữa.** Đã có khung trang Phóng viên (sidebar + thanh trên) và route sẵn trong `client/src/routes/AppRoutes.jsx`: `/my-articles` (UC-01), `/my-articles/:id` (UC-02), `/my-articles/new` (UC-03), `/co-author-invites` (UC-12). Hiện các route này trỏ tới trang tạm `ComingSoon`. Minh xoá dòng tương ứng trong mảng `TRANG_PV_CHUA_LAM` và khai báo `<Route>` trỏ tới màn của Minh, đặt **trong nhóm Phóng viên** (ngay dưới route `/main` của Phóng viên). Màn sẽ tự nằm trong khung, **không cần tự làm header/menu**. |
| A2 | Thiếu `id` cho header `X-User-Id` | Mình thêm `id` vào `client/src/auth/taiKhoanTam.js` theo thứ tự trong `data.sql`: an.nv=1, binh.tt=2, cuong.lh=3, dung.pt=4, duc.vm=5, chi.hl=6, hung.dq=7, admin=8, giang.nt=9 (thêm luôn giang.nt đang thiếu). Trong lúc chờ, Minh tra tạm theo username như đề xuất cũng được, nhớ ghi `TODO(auth)`. |
| A3 | Ai được xem chi tiết bài | **OK.** Iter 1 chỉ tác giả chính + đồng tác giả. Thêm BTV/Trưởng ban khi có `AssignmentService`, `User`. |
| A4 | Thông báo khi rút bài | **OK, Iter 1 không gửi.** `NotificationService` xem mục C2. |
| A5 | PR vào `main` | Gộp `main` vào `MinhTH` trước (lệnh ở đầu file), chạy lại app, rồi mở PR. Không push thẳng `main`. |

## B. Mặc định của Minh – OK, kèm vài điểm chỉnh

| # | Trả lời |
|---|---|
| B1 | OK, chỉnh 2 chỗ: (1) **lọc nhiều trạng thái cùng lúc** (xem B3); (2) **"Chờ tôi xác nhận" làm thành trang riêng `/co-author-invites` (UC-12)** thay vì tab trong "Tác phẩm của tôi", vì menu Phóng viên đã có mục "Xác nhận đồng tác giả" và trang chủ cũng link tới đó. Bỏ tham số `waitingMyConfirmation` khỏi API danh sách. |
| B2 | OK. |
| B3 | OK, thêm: `status` nhận **nhiều giá trị ngăn bằng dấu phẩy**. Trang chủ đang link `/my-articles?status=ASSIGNED,PENDING_APPROVAL,SCORE_PUBLISHED` (nhóm "Đang chấm"). Spring tự tách dấu phẩy nếu khai báo `@RequestParam(required = false) List<ArticleStatus> status`. Không truyền `status` thì lấy tất cả. |
| B4 | OK. |
| B5 | OK, file SQL ngoài repo. |
| B6 | OK. Đề nghị để bảng chữ trạng thái ở **một file dùng chung** `client/src/utils/trangThaiBai.js` (vd `export const TEN_TRANG_THAI = { DRAFT: 'Nháp', ... }`) để trang chủ và màn của Minh hiện **cùng một chữ**. Hiện trang chủ ghi "Trả về sửa", Minh ghi "Trả về – cần sửa": chốt theo bản của Minh, mình sửa trang chủ theo. |
| B7 | OK, để chung file với B6. |
| B8 | OK, đúng quy ước mới. Xem **AGENTS.md mục 4–5** (vừa cập nhật): (1) CSS để cạnh component (`MyWorks.jsx` + `MyWorks.css`), tên class có **tiền tố riêng** của màn (vd `.ds-bai__...`) vì CSS áp dụng cả trang; (2) màu chỉ dùng biến `--fm-*` trong `index.css`; (3) dùng lại `components/common/Icon.jsx`, `utils/formatDate.js` (`formatDate`, `daysUntil`), class chung `.nut-nhe`, `.cham cham--<màu>`, `.an-di`; (4) tên hàm/biến React tiếng Việt không dấu, field API giữ tên tiếng Anh của DTO. |

## C. Việc cần có trước Iter 2

| # | Việc | Tình trạng |
|---|---|---|
| C1 | `SystemParameter`, `User`, `Genre` | **Đọc tham số đã có trên `main`** (PR #9): gọi `SystemParameterService.getValuesByFlow("BF-01")` → `Map` key → value (vd `"MAX_AUTHORS_PER_ARTICLE"` → `"5"`, tự đổi sang số). **Chưa có phần sửa** (UC-13 cần): hàm cập nhật + ghi `updated_by/updated_at` + `audit_logs` + API `GET/PUT /api/system-parameters`. `User`, `Genre` chưa có. Người làm admin chưa chốt (AGENTS mục 9 #5) → bàn trong nhóm. |
| C2 | `NotificationService`, `@EnableScheduling`, thư mục `uploads/` | Chưa có, cần chốt người làm cùng C1 (đều là file dùng chung). Nút chuông trên khung Phóng viên cũng đang chờ cái này. |
| C3 | `article_status_logs.actor_id` cho job hết hạn | **Đề xuất:** cho phép `NULL` = job hệ thống, giống `audit_logs.actor_id` đang làm (nhất quán, đổi DB nhỏ, PR "CÓ ĐỔI DB"). Cách thay thế không đổi DB: dùng tài khoản Admin (id 8). Cần nhóm / chủ DB chốt. |
| C4 | Hàm `ArticleService` cho BF-02 | Minh + Dương thống nhất. **Thêm:** trang chủ Phóng viên đang dùng dữ liệu giả, cần 3 hàm đọc từ `ArticleService` (+ API): (1) đếm bài của tác giả theo `status`; (2) bài `REVISION_REQUESTED` kèm `note` mới nhất trong `article_status_logs`; (3) lời mời đồng tác giả `PENDING` của mình kèm `confirm_deadline`. Dạng dữ liệu xem `client/src/api/reporterHomeApi.js`. |
| C5 | Khoá mức bão hoà 100/80/60% | Phần của mình (BF-04). Hiện chưa có chỗ lưu và luật chưa rõ (bài thứ mấy 80%, thứ mấy 60% so với `k_limit`). Mình chốt luật + thêm tham số rồi báo. Iter 1 không nộp bài nên chưa chặn Minh. |

## Thêm – thay đổi DB sắp tới đụng bảng của Minh

PR `[DB]` của BF-04 sẽ thêm khoá ngoại `article_attachments.media_kind → media_factors(media_kind)` (để loại ảnh/video luôn có hệ số). Bảng `article_attachments` là của BF-01 nên báo trước; Minh thấy vướng thì nói.
