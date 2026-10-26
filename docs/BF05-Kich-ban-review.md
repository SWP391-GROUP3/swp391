---
title: BF-05 -- Học thuộc để review
---

> **Cách học:** mỗi mục là 1 câu thầy hay hỏi. Học **ý in đậm và mẹo nhớ** trước, sau đó tập nói **câu trả lời mẫu** thành tiếng. Số dòng code nằm ở **Phụ lục**, chỉ để mở sẵn trước buổi review, không cần thuộc.
>
> Thứ tự thầy hỏi: Luồng → Iter → Use case ở đâu trên swimlane / activity → Màn hình ở đâu → Bấm nút thì code nào chạy → Logic code.

# 0. Năm ý phải thuộc đầu tiên

1. BF-05 là luồng **quản trị & đánh giá KPI tháng**: Trưởng ban thiết lập định mức chuẩn → giao chỉ tiêu tháng cho phóng viên → hệ thống tổng hợp tự động (quy đổi bài viết theo tỷ lệ đồng tác giả & điểm chất lượng hiệu lực) → Trưởng ban giám sát trên Bảng KPI toàn ban → Phóng viên tự theo dõi tiến độ & kết quả cá nhân.

2. Sơ đồ Activity / Swimlane có **3 làn**: Trưởng ban biên tập (Head of Editorial Department) · Phóng viên (Reporter) · Quản trị viên (Admin).

3. Đánh giá KPI theo **quy tắc nhị phân (Binary) PASS / NOT_PASS** (BR-KPI-05): ĐẠT khi cả 2 tiêu chí (`actual_articles >= min_published` VÀ `actual_avg_score >= min_avg_score`) đều thỏa mãn. **Nghiêm cấm xếp hạng A/B/C/D**.

4. Quy đổi số bài viết **tính chặt chẽ theo tỷ lệ đồng tác giả** (BR-KPI-02, GBR-13): bài viết 60/40 thì tác giả chính được 0.6 bài, đồng tác giả được 0.4 bài. Điểm chất lượng lấy từ điểm hiệu lực sau chấm/khiếu nại (BF-02/BF-03). Số liệu tương tác độc giả từ CSV (UC-63) **chỉ dùng cho phân tích/báo cáo, tuyệt đối không dùng để xét PASS/NOT_PASS** (BR-KPI-04, GBR-13).

5. Em đã làm xong các chức năng cốt lõi: Định mức chuẩn (UC-58), Giao chỉ tiêu tháng (UC-59), Bảng KPI toàn ban (UC-61), Chi tiết KPI phóng viên (UC-62), Tiến độ KPI cá nhân (UC-60/UC-67), Job tổng hợp tự động (Automated Aggregation) và Unit test tính toán độc lập `KpiCalculatorTest`.

---

# 1. "Em giải thích luồng theo swimlane / activity diagram"

**Mẹo nhớ 5 bước: Định mức → Giao chỉ tiêu → Phóng viên xem → Tổng hợp & Giám sát → Đóng kỳ**

1. **Định mức:** Đầu kỳ hoặc khi cần điều chỉnh, Trưởng ban vào cấu hình định mức chuẩn theo ngạch phóng viên (Tập sự, Sơ cấp, Kỳ cựu).
2. **Giao chỉ tiêu:** Đầu tháng, Trưởng ban giao chỉ tiêu cho từng phóng viên (số bài xuất bản tối thiểu `min_published`, điểm chất lượng trung bình tối thiểu `min_avg_score`). Có thể chọn "Áp dụng định mức chuẩn" để điền nhanh hàng loạt. Mỗi phóng viên chỉ có duy nhất 1 bộ chỉ tiêu trong 1 tháng (BR-KPI-01).
3. **Phóng viên xem:** Phóng viên mở "KPI cá nhân" để xem chỉ tiêu được giao, theo dõi tiến độ số bài viết đã xuất bản trong tháng (tính theo tỷ lệ tác giả) và điểm trung bình hiện tại.
4. **Tổng hợp & Giám sát:** Hệ thống chạy job tổng hợp tự động (hoặc Trưởng ban bấm chạy tổng hợp trên web): quét các bài `PUBLISHED` trong tháng, nhân tỷ lệ đồng tác giả (0.6 / 0.4), tính điểm trung bình các bài đã có điểm. Trưởng ban theo dõi toàn diện trên Bảng KPI của ban; nếu cần xem sâu thì bấm vào từng phóng viên để xem danh sách bài viết chi tiết.
5. **Đóng kỳ (Snapshot):** Cuối tháng, Trưởng ban chốt kỳ KPI (Finalize). Hệ thống sinh bản ghi bất biến `kpi_snapshots` với trạng thái `FINALIZED`, gửi thông báo kết quả chính thức cho phóng viên (BR-KPI-06).

**Câu trả lời mẫu (nói liền một hơi):**

> "Dạ, BF-05 là luồng quản trị KPI và báo cáo. Đầu chu kỳ, Trưởng ban cấu hình định mức chuẩn theo ngạch rồi giao chỉ tiêu tháng cho từng phóng viên trong ban. Trong tháng, phóng viên có thể tự theo dõi tiến độ cá nhân, bài viết được quy đổi chính xác theo tỷ lệ đồng tác giả như 0.6 và 0.4 bài. Hệ thống tổng hợp tự động số liệu để Trưởng ban giám sát trên Bảng KPI toàn ban và soi chi tiết từng phóng viên. Kết quả đánh giá chỉ có ĐẠT hoặc KHÔNG ĐẠT chứ không chia hạng A/B/C/D. Cuối kỳ, Trưởng ban chốt kỳ để lưu bản ghi snapshot bất biến."

---

# 2. "Iter 1, 2, 3 em làm những gì?"

**Mẹo: Iter 1 cấu hình, giao chỉ tiêu, bảng KPI & theo dõi cá nhân · Iter 2 chốt kỳ & snapshot · Iter 3 báo cáo chuyên sâu & import CSV.**

- **Iter 1 (Đã hoàn thiện):**
  - **UC-58:** Màn hình & API Cấu hình định mức chuẩn theo ngạch (`/kpi-benchmarks`).
  - **UC-59:** Màn hình & API Giao chỉ tiêu tháng (`/kpi-targets`): thêm, sửa, xoá, áp dụng nhanh định mức chuẩn.
  - **UC-61:** Bảng theo dõi KPI ban biên tập thời gian thực (`/kpi-board`) và nút kích hoạt job tổng hợp tự động (`/api/kpi-snapshots/aggregate`).
  - **UC-62:** Màn hình Chi tiết KPI phóng viên (`/kpi-board/:reporterId`) hiển thị từng bài viết, hệ số quy đổi và lượt xem tích lũy.
  - **UC-60 & UC-67:** Màn hình KPI cá nhân của phóng viên (`/my-kpi`) hiển thị tiến độ và kết quả PASS/NOT_PASS.
  - **Thuật toán & Unit test:** Bộ tính toán thuần `KpiCalculator` và unit test JUnit 5 `KpiCalculatorTest` bao phủ toàn bộ luật BR-KPI-02 và BR-KPI-05.

- **Iter 2 (Kế hoạch tiếp theo):**
  - **UC-66:** Chức năng chốt kỳ KPI tháng (Finalize Monthly KPI) chuyển trạng thái snapshot sang `FINALIZED`, khoá kỳ vĩnh viễn (BR-KPI-06) và phát thông báo.
  - **UC-63:** Chức năng Import số liệu độc giả từ file CSV (`reader_interactions`, `csv_import_logs`) cho Admin.

- **Iter 3:**
  - **UC-64:** Báo cáo mức độ quan tâm thể loại & chi phí nhuận bút / 1.000 lượt xem (BR-KPI-04).
  - **UC-65:** Báo cáo phát hiện sự kiện nóng (Trending events surge detection).
  - **UC-68:** Xuất báo cáo lịch sử KPI nhiều kỳ ra định dạng Excel/PDF.

---

# 3. "Use case này nằm ở đoạn nào trên sơ đồ?"

**Mẹo:** Bám sát luồng: Định mức (UC-58) → Giao chỉ tiêu (UC-59) → Phóng viên xem (UC-60) → Bảng ban (UC-61) → Soi chi tiết (UC-62) → Chốt kỳ (UC-66).

| Bước | Hoạt động trên Activity / Swimlane | Use Case |
|---|---|---|
| **Cấu hình** | Head: Configure KPI Rules | **UC-58** Configure KPI Benchmark Rules |
| **Giao chỉ tiêu** | Head: Set Monthly Targets | **UC-59** Set Monthly Targets |
| **Phóng viên xem** | Reporter: View Personal KPI and Targets / Progress | **UC-60** View Personal KPI & Progress |
| **Bảng tổng hợp** | Head: View Department KPI Board | **UC-61** View Department KPI Board |
| **Soi chi tiết** | Head: Need a closer look? → Yes → View Reporter KPI Details | **UC-62** View Reporter KPI Details (Extend UC-61) |
| **Đọc giả (CSV)** | Admin: Import Reader Data (CSV) | **UC-63** Import Reader Data (CSV) |
| **Báo cáo** | Head: View Genre & Trending Reports | **UC-64** View Genre Popularity & Cost Report<br>**UC-65** View Trending Events Report |
| **Chốt kỳ** | Head: Finalize Monthly KPI | **UC-66** Finalize Monthly KPI |
| **Xem kết quả** | Reporter: View Final KPI Result | **UC-67** View Final KPI Result |
| **Xuất báo cáo** | Head: Export KPI Historical Report | **UC-68** Export KPI Historical Report |

**Câu trả lời mẫu:**

> "Dạ, UC-62 Xem chi tiết KPI phóng viên là nhánh rẽ *Need a closer look? → Yes* từ Bảng KPI toàn ban (UC-61). Trên Use Case Diagram, UC-62 là quan hệ `«Extend»` mở rộng từ UC-61."

---

# 4. "Màn hình này là đoạn nào trên luồng?"

| Màn hình | Route trên Client | Thuộc Use Case | Đối tượng sử dụng |
|---|---|---|---|
| **Định mức KPI chuẩn** | `/kpi-benchmarks` | UC-58 | Trưởng ban / Admin |
| **Giao chỉ tiêu tháng** | `/kpi-targets` | UC-59 | Trưởng ban |
| **Bảng KPI của ban** | `/kpi-board` | UC-61 | Trưởng ban |
| **Chi tiết KPI phóng viên** | `/kpi-board/:reporterId` | UC-62 | Trưởng ban |
| **KPI cá nhân** | `/my-kpi` | UC-60, UC-67 | Phóng viên |

**Mẹo đọc trạng thái & số liệu:**
- `min_published`: Số bài chỉ tiêu (INT).
- `actual_articles`: Số bài thực tế sau khi đã cộng dồn tỷ lệ đồng tác giả (ví dụ: 1 bài 100% + 1 bài 60% + 1 bài 40% = 2 bài quy đổi).
- `actual_avg_score`: Điểm trung bình chỉ tính trên các bài *đã có điểm chất lượng*; bài chưa có điểm thì tạm thời bỏ qua để không kéo tụt điểm của phóng viên.
- `status`: `DRAFT` (kỳ đang diễn ra, số liệu tính động) · `FINALIZED` (Trưởng ban đã chốt, số liệu đóng băng vĩnh viễn).
- Trạng thái đánh giá: `PASS` (ĐẠT - màu xanh) · `NOT_PASS` (CHƯA ĐẠT - màu đỏ).

---

# 5. "Bấm nút này thì code nào chạy, mở ra màn tiếp theo?"

**Thuộc chuỗi chuẩn:**

**Link / NavLink → AppRoutes → Component Trang → useEffect → kpiApi → Controller → Service → Repository**

**Câu trả lời mẫu:**

> "Dạ, hệ thống dùng React Single Page Application. Khi bấm vào menu hoặc link, React Router cập nhật URL mà không reload trang. Component tương ứng được render, hook `useEffect` kích hoạt hàm trong `kpiApi.js` để gửi HTTP request kèm JWT token trong header `Authorization`. Ở backend, Controller đón request, kiểm tra `@Valid` hoặc lấy ID từ token rồi chuyển sang Service. Service gọi máy tính toán `KpiCalculator` và Repository truy vấn SQL Server, trả dữ liệu qua DTO về cho client render giao diện."

| Thao tác / Nút bấm | Route / URL đổi | Hàm gọi ở Frontend | API & Hàm ở Backend |
|---|---|---|---|
| Menu **Bảng KPI của ban** | `/kpi-board` | DepartmentKpiDashboard → `layBangKpiBan(nam, thang)` | `GET /api/kpi-snapshots/dashboard` → `getDepartmentDashboard` |
| Bấm **Chạy tổng hợp tự động** | Không đổi URL | DepartmentKpiDashboard → `tongHopKpiThang(nam, thang)` | `POST /api/kpi-snapshots/aggregate` → `triggerAggregation` |
| Bấm **Xem chi tiết** (tên phóng viên) | `/kpi-board/:reporterId` | ReporterKpiDetails → `layChiTietKpiPhongVien(id, nam, thang)` | `GET /api/kpi-snapshots/reporters/{id}/details` → `getReporterKpiDetail` |
| Menu **KPI cá nhân** (Phóng viên) | `/my-kpi` | PersonalKpiProgress → `layChiTietKpiPhongVien(nguoiDung.id, nam, thang)` | `GET /api/kpi-snapshots/reporters/{id}/details` → `getReporterKpiDetail` |
| Menu **Giao chỉ tiêu tháng** | `/kpi-targets` | MonthlyTargetSetting → `layChiTieuTheoThang(nam, thang)` | `GET /api/kpi-targets` → `getTargets` |
| Bấm **Giao chỉ tiêu mới** (Modal) | Modal popup | MonthlyTargetSetting → `taoChiTieu(duLieu)` | `POST /api/kpi-targets` → `createTarget` |
| Bấm **Áp dụng định mức** | Điền sẵn form | MonthlyTargetSetting → đọc state `dinhMucChuan` | Đã tải từ `GET /api/kpi-targets/benchmarks` lúc mount |
| Menu **Định mức chuẩn** | `/kpi-benchmarks` | KpiBenchmarksConfig → `layDinhMucChuan()` | `GET /api/kpi-targets/benchmarks` → `getBenchmarks` |
| Bấm **Lưu thay đổi định mức** | Không đổi URL | KpiBenchmarksConfig → `capNhatDinhMucChuan(danhSach)` | `PUT /api/kpi-targets/benchmarks` → `updateBenchmarks` |

---

# 6. "Giải thích logic code"

### 1. Thuật toán quy đổi bài viết theo tỷ lệ đồng tác giả (BR-KPI-02 & GBR-13)
- **Vấn đề:** Nếu một bài viết có 2 người cùng viết (tỷ lệ 60% và 40%), không thể tính mỗi người 1 bài nguyên vẹn vì sẽ thổi phồng sản lượng thật của tòa soạn.
- **Cách xử lý:** 
  - Trong `KpiCalculator.calculateConvertedArticles()`: duyệt qua từng bài `PUBLISHED` trong tháng của phóng viên, lấy `share_percent / 100` để ra số bài quy đổi (ví dụ: 60% = 0.6 bài, 40% = 0.4 bài).
  - Khi lưu vào cột `actual_articles` (kiểu `INT` theo `schema.sql`), hệ thống làm tròn số học chuẩn `HALF_UP`.
  - Trên màn hình hiển thị, hệ thống tính bằng `calculateExactConvertedArticles()` giữ 2 chữ số thập phân (ví dụ: `2.60 bài`) để phóng viên thấy minh bạch công sức của mình.

### 2. Thuật toán tính điểm chất lượng trung bình (`actual_avg_score`)
- **Nguyên tắc:** Chỉ tính trung bình cộng trên các bài viết **đã có điểm chất lượng hiệu lực** (`effectiveScore != null`).
- Nếu trong tháng có 3 bài nhưng mới có 2 bài chấm xong (80 điểm và 90 điểm), bài thứ 3 đang chờ duyệt thì điểm TB = `(80 + 90) / 2 = 85.00`.
- Nếu chưa có bài nào có điểm, trả về `null` (khớp hoàn toàn với trường `actual_avg_score NULL` trong bảng `kpi_snapshots`).

### 3. Quy tắc xét ĐẠT / KHÔNG ĐẠT nhị phân (BR-KPI-05)
- **Luật:** Chỉ có 2 trạng thái: `PASS` hoặc `NOT_PASS`.
- **Điều kiện:** Phóng viên chỉ được xét `PASS` khi đồng thời thỏa mãn:
  1. `actual_articles >= min_published` (đạt hoặc vượt số bài quy đổi tối thiểu)
  2. `actual_avg_score != null` VÀ `actual_avg_score >= min_avg_score` (đạt hoặc vượt sàn điểm chất lượng)
- Thiếu bất kỳ điều kiện nào (kể cả chưa có bài nào có điểm) đều trả về `NOT_PASS`.
- Nghiêm cấm chia xếp loại A, B, C, D để đảm bảo tính công bằng và đúng chuẩn mực đánh giá.

### 4. Đảm bảo tính bất biến của Snapshot (BR-KPI-06)
- Bảng `kpi_snapshots` hỗ trợ 2 trạng thái:
  - `DRAFT`: Số liệu tạm tính trong chu kỳ, job tổng hợp có thể tính toán lại và cập nhật nhiều lần.
  - `FINALIZED`: Khi Trưởng ban chốt kỳ, bản ghi được gắn `finalized_by` và `finalized_at`. Khi đã ở trạng thái `FINALIZED`, job tổng hợp **tuyệt đối không được ghi đè hay cập nhật**, giữ nguyên dữ liệu lịch sử để làm báo cáo tài chính và lưu trữ.

### 5. Tách biệt tầng tính toán thuần (Pure Logic Separation)
- Toàn bộ công thức tính toán bài quy đổi, điểm trung bình và kiểm tra PASS/NOT_PASS được đặt riêng trong class `KpiCalculator`.
- Class này không có `@Service`, không tiêm phụ thuộc DB, giúp viết Unit Test độc lập hoàn toàn trên JUnit 5 mà không cần khởi động Spring context hay giả lập cơ sở dữ liệu.

---

# 7. Câu hỏi thêm -- mỗi câu trả lời 1 ý

- **Vì sao số liệu độc giả (CSV) không được dùng để tính KPI đạt/không đạt?**
  > Trả lời: Theo BR-KPI-04 và GBR-13, số liệu view và đánh giá của độc giả từ bên ngoài chỉ dùng cho phân tích xu hướng và báo cáo tài chính nhuận bút, không dùng để đánh giá năng lực phóng viên vì view phụ thuộc vào độ giật gân của chủ đề, có thể gây sai lệch chất lượng chuyên môn.

- **Vì sao phóng viên viết bài chung chỉ được tính 0.6 hoặc 0.4 bài?**
  > Trả lời: Theo BR-KPI-02, tỷ lệ công nhận bài phải khớp với tỷ lệ đóng góp bản quyền đã được các tác giả cam kết ở luồng BF-01 để phản ánh chính xác khối lượng công việc.

- **Bảng `kpi_benchmarks` trong SRS mục UC-58 sao trong `schema.sql` không có?**
  > Trả lời: Đây là Vấn đề đã biết #6 trong `SRS_Overview.md`. Do schema chưa thiết kế bảng riêng cho benchmarks, nhóm đã xử lý bằng danh sách cấu hình mẫu chuẩn trong service và chuẩn bị phương án chuyển sang bảng `system_parameters`.

- **Cột `target_views` trong SRS mục UC-59 nằm ở đâu trong database?**
  > Trả lời: Trong `schema.sql`, bảng `kpi_targets` chỉ quản lý 2 chỉ tiêu cốt lõi là `min_published` và `min_avg_score`, không có cột `target_views` vì lượt xem không tham gia vào điều kiện xét PASS/NOT_PASS (BR-KPI-04).

- **Khác biệt giữa bản ghi DRAFT và FINALIZED trong `kpi_snapshots` là gì?**
  > Trả lời: `DRAFT` là kết quả tổng hợp động giữa kỳ có thể tính lại; `FINALIZED` là bản chốt cuối kỳ có người ký nhận (`finalized_by`), thời điểm (`finalized_at`) và là bất biến theo BR-KPI-06.

- **Vì sao không dùng vòng lặp query từng phóng viên khi tải Dashboard?**
  > Trả lời: Để tránh lỗi N+1 query. Service tải trước toàn bộ bài viết và tỷ lệ tác giả trong tháng bằng các câu query tập trung rồi gom nhóm bằng `Map` trên bộ nhớ.

---

# 8. Thầy bảo "demo thử..."

| Thầy bảo | Em làm |
|---|---|
| **"Đăng nhập Trưởng ban và mở bảng KPI"** | Đăng nhập tài khoản Trưởng ban `duc.vm` / `123456` → Bấm menu **Bảng KPI của ban** (`/kpi-board`). |
| **"Chạy tính toán tổng hợp số liệu tháng 9/2026"** | Chọn tháng 9/2026 → Bấm nút **Chạy tổng hợp tự động** → Hệ thống tính toán và cập nhật số bài quy đổi, điểm TB và trạng thái ĐẠT / CHƯA ĐẠT. |
| **"Xem chi tiết 1 phóng viên"** | Trên bảng KPI, bấm vào tên hoặc nút **Xem chi tiết** của phóng viên Nguyễn Văn An (`/kpi-board/1`) → Chỉ ra danh sách bài viết, tỷ lệ đồng tác giả và điểm chất lượng từng bài. |
| **"Giao chỉ tiêu mới cho phóng viên"** | Mở menu **Giao chỉ tiêu tháng** (`/kpi-targets`) → Bấm **Giao chỉ tiêu mới** → Chọn phóng viên, bấm **Áp dụng định mức chuẩn** (tự điền số bài và điểm) → Bấm **Lưu chỉ tiêu**. |
| **"Xem định mức chuẩn theo ngạch"** | Mở menu **Định mức chuẩn** (`/kpi-benchmarks`) → Trình bày các mức: Tập sự (4 bài, 60 điểm), Sơ cấp (6 bài, 65 điểm), Kỳ cựu (8 bài, 70 điểm). |
| **"Đăng nhập Phóng viên xem tiến độ cá nhân"** | Đăng xuất → Đăng nhập tài khoản Phóng viên `an.nv` / `123456` → Bấm menu **KPI cá nhân** (`/my-kpi`) → Cho thầy thấy thẻ chỉ tiêu giao, thanh tiến độ và danh sách bài xuất bản. |

---

# 9. Hạn chế -- nói thẳng nếu bị hỏi

| Hạn chế | Lý do & Hướng xử lý |
|---|---|
| Bảng `kpi_benchmarks` lưu in-memory / mẫu cấu hình | Schema DB chưa có bảng này (Vấn đề đã biết #6 trong `SRS_Overview.md`). Sẽ chuyển sang bảng `system_parameters` khi luồng Admin hoàn thiện. |
| Chỉ tiêu chưa có số lượt xem (`target_views`) | Bảng `kpi_targets` trong `schema.sql` không thiết kế cột này vì lượt xem không tham gia vào tiêu chuẩn xét PASS/NOT_PASS. |
| Các báo cáo đồ thị (UC-64, UC-65, UC-68) chưa tích hợp giao diện | Nằm trong phạm vi Iter 3 sau khi luồng Import CSV của Admin (UC-63) và Nhuận bút (BF-04) hoàn thành để có đủ dữ liệu tính chi phí/1.000 view. |
| Nút "Chốt kỳ KPI" (UC-66) đang để quyền thao tác qua API DRAFT/FINALIZED | Đang hoàn thiện modal xác nhận chốt kỳ trên giao diện web cho Trưởng ban trong Iter tiếp theo. |

---

# Phụ lục -- Chuẩn bị trước buổi review (không cần thuộc)

- **Môi trường chạy:**
  - Backend: `mvn -f server/pom.xml spring-boot:run` (chạy trên cổng `8080`)
  - Frontend: `npm --prefix client run dev` (chạy trên cổng `5173`)
  - Mở trình duyệt: `http://localhost:5173`
- **Tài khoản kiểm thử sẵn trong `data.sql` (mật khẩu chung `123456`):**
  - **Trưởng ban Thời sự:** `duc.vm` (id = 5, role = `HEAD_OF_DEPARTMENT`)
  - **Phóng viên 1:** `an.nv` (id = 1, role = `REPORTER`)
  - **Phóng viên 2:** `binh.tt` (id = 2, role = `REPORTER`)
- **Dữ liệu mẫu trong database:**
  - Đã có chỉ tiêu tháng 9/2026 trong `kpi_targets` cho `an.nv` (8 bài, 70 điểm) và `binh.tt` (6 bài, 65 điểm).
  - Đã có snapshot tháng 8/2026 (`FINALIZED`) và tháng 9/2026 (`DRAFT`) trong `kpi_snapshots`.
- **Mở sẵn 2 tab hình minh hoạ:**
  - Sơ đồ Use Case BF-05: `docs/srs-images/uc-bf05-kpi.png`
  - Sơ đồ Activity BF-05: `docs/srs-images/bf05-activity.png`
- **Các file quan trọng mở sẵn trên VS Code:**
  - Frontend: `AppRoutes.jsx`, `HeadLayout.jsx`, `ReporterLayout.jsx`, `DepartmentKpiDashboard.jsx`, `PersonalKpiProgress.jsx`, `ReporterKpiDetails.jsx`, `MonthlyTargetSetting.jsx`, `KpiBenchmarksConfig.jsx`, `kpiApi.js`.
  - Backend: `KpiTargetController.java`, `KpiSnapshotController.java`, `KpiTargetService.java`, `KpiAggregationService.java`, `KpiCalculator.java`, `KpiCalculatorTest.java`, `schema.sql`.

### Bảng số dòng để tra cứu nhanh khi thầy hỏi

| File | Vị trí / Dòng quan trọng |
|---|---|
| **AppRoutes.jsx** | Route `/my-kpi` :174 · Route `/kpi-board` :190 · Route `/kpi-board/:reporterId` :191 · Route `/kpi-targets` :192 · Route `/kpi-benchmarks` :193 |
| **HeadLayout.jsx** | Menu Bảng KPI ban :39 · Giao chỉ tiêu tháng :41 · Định mức chuẩn :43 · Báo cáo :45 |
| **ReporterLayout.jsx** | Menu KPI cá nhân :33 |
| **DepartmentKpiDashboard.jsx** | `useEffect` :25 · `taiDuLieuDashboard` :29 · Nút Chạy tổng hợp `xuLyChayTongHop` :43 · Bảng danh sách phóng viên :115 |
| **PersonalKpiProgress.jsx** | `useEffect` :24 · `taiKpiCaNhan` :30 · Thẻ tổng quan chỉ tiêu & PASS/NOT_PASS :65 · Danh sách bài viết :130 |
| **ReporterKpiDetails.jsx** | `useEffect` :33 · `taiChiTietKpi` :39 · Bảng danh sách bài xuất bản & tỷ lệ quy đổi :120 |
| **MonthlyTargetSetting.jsx** | `layDinhMucChuan` :43 · `taiDanhSachChiTieu` :53 · `apDungDinhMuc` :87 · `xuLyLuuForm` :92 · `xuLyXoa` :129 |
| **KpiBenchmarksConfig.jsx** | `taiDuLieu` :27 · `xuLyLuu` :49 |
| **kpiApi.js** | `layDinhMucChuan` :21 · `capNhatDinhMucChuan` :28 · `layChiTieuTheoThang` :35 · `taoChiTieu` :43 · `capNhatChiTieu` :50 · `xoaChiTieu` :57 · `layBangKpiBan` :64 · `tongHopKpiThang` :71 · `layChiTietKpiPhongVien` :78 |
| **KpiTargetController.java** | `getBenchmarks` :51 · `updateBenchmarks` :61 · `getTargets` :73 · `getTargetById` :86 · `createTarget` :96 · `updateTarget` :112 · `deleteTarget` :125 |
| **KpiSnapshotController.java** | `getDepartmentDashboard` :41 · `getReporterKpiDetail` :54 · `triggerAggregation` :68 |
| **KpiCalculator.java** | `calculateConvertedArticles` (quy đổi đồng tác giả) :39 · `calculateExactConvertedArticles` :57 · `calculateAverageScore` (điểm TB) :77 · `evaluatePassStatus` (PASS/NOT_PASS) :104 |
| **KpiCalculatorTest.java** | `testCalculateConvertedArticles` :19 · `testCalculateAverageScore` :40 · `testEvaluatePassStatus` :69 |
| **KpiTargetService.java** | `getTargetsByPeriod` :43 · `getDefaultBenchmarks` :73 · `createTarget` :91 · `updateTarget` :121 |
| **KpiAggregationService.java** | `aggregateMonthlyKpi` :68 · `getDepartmentDashboard` :151 · `getReporterKpiDetail` :256 |
| **schema.sql** | Bảng `kpi_targets` :874 · Bảng `kpi_snapshots` :891 · Bảng `csv_import_logs` :913 · Bảng `reader_interactions` :926 |
