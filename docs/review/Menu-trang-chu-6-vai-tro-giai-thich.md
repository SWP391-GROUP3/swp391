# Menu + trang chủ cho 6 vai trò — giải thích chi tiết

> Nhánh `feature/admin-auth-login` (đã gộp main có PR #15 của Minh) · 14/10/2026
> Trước đây chỉ Phóng viên có khung trang + menu; các vai trò khác dùng trang `Main.jsx` dạng tab.
> Bây giờ **cả 6 vai trò** (Phóng viên, Biên tập viên, Thư ký toà soạn, Trưởng ban, Kế toán, Admin) đều có sidebar menu riêng và trang chủ riêng.
> Đọc theo thứ tự: **Phần 1 lý thuyết → Phần 2 luồng chạy → Phần 3 từng file → Phần 4 kiểm thử → Phần 5 câu hỏi khi review.**

---

## Mục lục

1. [Lý thuyết nền](#phần-1--lý-thuyết-nền)
2. [Luồng chạy từ đầu đến cuối](#phần-2--luồng-chạy-từ-đầu-đến-cuối)
3. [Giải thích từng file](#phần-3--giải-thích-từng-file)
4. [Kiểm thử đã chạy](#phần-4--kiểm-thử-đã-chạy)
5. [Câu hỏi thầy có thể hỏi](#phần-5--câu-hỏi-thầy-có-thể-hỏi)
6. [Hướng dẫn cho nhóm: thêm 1 trang mới](#phần-6--hướng-dẫn-cho-nhóm-thêm-1-trang-mới)
7. [Việc cả nhóm phải biết sau khi pull](#phần-7--việc-cả-nhóm-phải-biết-sau-khi-pull)
8. [Hạn chế còn lại](#phần-8--hạn-chế-còn-lại--việc-tiếp-theo)

---

## Phần 1 — Lý thuyết nền

### 1.1 Vì sao tách "khung trang" ra dùng chung

Trang nào sau khi đăng nhập cũng có 3 phần giống nhau: **sidebar trái** (logo, menu, nút Đăng xuất), **thanh trên** (ô tìm kiếm, chuông, tên + avatar), **vùng nội dung**. Chỉ khác nhau **danh sách mục menu**.

Nếu mỗi vai trò tự viết lại cả khung → 6 bản sao giống nhau 90%, sửa 1 chỗ (vd đổi màu sidebar) phải sửa 6 file. Nên tách:

```
AppLayout          = khung chung (vẽ sidebar, thanh trên, nội dung)
ReporterLayout     = AppLayout + menu Phóng viên + chip ví
EditorLayout       = AppLayout + menu Biên tập viên
...                  (mỗi vai trò 1 file nhỏ, chủ yếu là danh sách menu)
```

Đây là cách ghép component bình thường của React (**composition**): component cha truyền dữ liệu (`props`) và nội dung con (`children`) vào component chung. Không dùng kế thừa class hay pattern gì đặc biệt.

### 1.2 Route lồng nhau và `<Outlet />`

React Router cho phép 1 route **cha không có `path`**, chỉ dùng để vẽ khung:

```jsx
<Route element={<Layout><Outlet /></Layout>}>   ← route cha: vẽ khung
  <Route path="/main" element={<TrangChu />} />  ← route con: vẽ vào chỗ <Outlet />
  <Route path="/royalty-periods" element={...} />
</Route>
```

`<Outlet />` là "chỗ trống" trong khung để route con hiện vào. Khi bấm menu đổi trang, **chỉ phần Outlet thay đổi**, khung giữ nguyên → không vẽ lại sidebar, không tải lại số dư ví mỗi lần đổi trang.

### 1.3 `NavLink` — mục menu tự sáng

`NavLink` giống `Link` nhưng tự gắn class `active` + `aria-current="page"` khi URL đang trùng với nó. CSS chỉ cần viết `.khung-app__muc.active { ... }` là mục đang mở tự đổi màu, không phải tự so URL.

### 1.4 Component dùng chung cho trang chủ

Cả 6 trang chủ có cùng bố cục (lấy từ trang chủ Phóng viên đã làm theo Figma):

```
Xin chào, <tên>                         ← HomeFrame
Thứ Tư, 14 tháng 10, 2026
┌────────────────────────────┬──────────────┐
│ Chờ xử lý          [3]     │ <Cột đếm>    │ ← StatusCounter
│  • việc 1        [Nút]     │  • nhóm A  3 │
│  • việc 2        [Nút]     │  • nhóm B  1 │
├────────────────────────────┤              │
│ <Thẻ tóm tắt>  [nhãn]      │              │ ← SummaryCard
│  dòng 1 ............ số    │              │
└────────────────────────────┴──────────────┘
      ↑ PendingList
```

Quy tắc AGENTS.md mục 4: component **≥ 2 màn dùng** → để trong `components/common/`. 4 khối trên được 6 trang dùng nên chuyển lên đó.

**Chia việc giữa component chung và trang:**
- Component chung (`PendingList`, `StatusCounter`, `SummaryCard`) **chỉ vẽ**, không gọi API, không biết nghiệp vụ.
- Trang chủ từng vai trò (`EditorHome`, `SecretaryHome`...) **gọi API + đổi dữ liệu** thành dạng component cần (vd đổi `{ status, count }` thành `{ nhan, mau, so, duongDan }`).

### 1.5 Dữ liệu mẫu (giả lập API)

Hầu hết backend chưa có API cho trang chủ. Để vẫn demo được, mỗi trang chủ có 1 file `api/xxxHomeApi.js` trả **dữ liệu giả**, nhưng:
- Trả về **Promise** sau 300ms (giống gọi API thật, thấy được chữ "Đang tải…").
- Tên field đặt **đúng tên DTO tiếng Anh** backend sẽ trả, giá trị trạng thái **đúng chữ trong CHECK của `schema.sql`**.

→ Khi backend làm xong, chỉ cần đổi **ruột hàm** thành `goiApi('/api/...')`, trang **không phải sửa gì**.

Có **2 chỗ dùng API thật** vì backend đã có sẵn:

| Trang | Thẻ | API thật |
|---|---|---|
| Kế toán | Đơn giá nhuận bút | `GET /api/royalty-rates` (BF-04) |
| Admin | Quy tắc khiếu nại | `GET /api/appeal-rule-configs/active` (BF-03, Vũ) |

### 1.6 `useEffect` tải dữ liệu + cờ `conHienThi`

Mọi trang chủ tải dữ liệu theo cùng 1 mẫu (giống code đã có):

```jsx
useEffect(() => {
  let conHienThi = true                       // trang còn trên màn hình không
  layTrangChuThuKy()
    .then((ketQua) => { if (conHienThi) setDuLieu(ketQua) })
    .catch(() => { if (conHienThi) setLoi('Không tải được ...') })
  return () => { conHienThi = false }         // chạy khi rời trang
}, [])                                        // [] = chỉ chạy 1 lần khi trang hiện ra
```

Nếu người dùng bấm sang trang khác trước khi API trả về, `conHienThi = false` → không `setState` vào component đã bị gỡ.

### 1.7 `useId` — id không trùng

Mỗi khối có tiêu đề `<h2 id=...>` và `<section aria-labelledby=...>` để trình đọc màn hình đọc được tên khối. Vì `PendingList`/`SummaryCard` có thể xuất hiện nhiều lần, không viết cứng `id="tieu-de-viec"` được (trùng id). `useId()` của React sinh id riêng cho mỗi lần dùng.

---

## Phần 2 — Luồng chạy từ đầu đến cuối

### 2.1 Đăng nhập xong → thấy đúng menu + trang chủ

```
Login.jsx ── đăng nhập ──> App.jsx lưu nguoiDung = { ..., vaiTro: ['ACCOUNTANT'] }
                                │
                                ▼
AppRoutes.jsx:  vaiTro = nguoiDung.vaiTro[0]            -> 'ACCOUNTANT'
                khung  = KHUNG_THEO_VAI_TRO['ACCOUNTANT']
                       = { Layout: AccountantLayout, TrangChu: AccountantHome, trangChuaLam: [...] }
                                │
                                ▼
<Route element={ <RequireRole vaiTro="ACCOUNTANT"> <AccountantLayout> <Outlet/> ... }>
   /main                -> <AccountantHome />            (trang chủ)
   /royalty-periods     -> <ComingSoon "Kỳ nhuận bút"/> (chưa làm)
   ...
```

Mỗi user chỉ có 1 vai trò (`users.role_id`) nên lấy `vaiTro[0]`.

### 2.2 Bấm 1 mục menu

`NavLink to="/royalty-rates"` → URL đổi → React Router tìm route con khớp → vẽ vào `<Outlet />`. Khung không đổi.

### 2.3 Gõ URL của vai trò khác

Kế toán gõ `/assignments` (trang của Trưởng ban) → nhóm route của Kế toán **không có** path này → rơi vào `path="*"` → về `/` → về `/main` → trang chủ Kế toán.

> Đây chỉ là chặn ở **giao diện**. Backend vẫn phải tự chặn theo vai trò (403) — xem Phần 8.

### 2.4 Chưa đăng nhập / vừa đăng xuất

`nguoiDung = null` → `khung = undefined` → AppRoutes dùng nhánh dự phòng: route `/main` bọc `RequireRole` → `RequireRole` thấy chưa đăng nhập → về `/login`.

Nhánh dự phòng **bắt buộc phải có**: nếu thiếu, `/main` không khớp route nào → `*` → `/` → `/main` → `*`... **lặp vô hạn**.

---

## Phần 3 — Giải thích từng file

### 3.0 Danh sách thay đổi

| Nhóm | File | Loại | Ghi chú |
|---|---|---|---|
| Khung | `layouts/AppLayout.jsx` + `AppLayout.css` | Mới (CSS đổi tên từ `ReporterLayout.css`) | Khung chung |
| Khung | `layouts/ReporterLayout.jsx` + `.css` | Sửa | Chỉ còn menu PV + chip ví |
| Khung | `layouts/EditorLayout.jsx`, `SecretaryLayout.jsx`, `HeadLayout.jsx`, `AccountantLayout.jsx`, `AdminLayout.jsx` | Mới | Menu từng vai trò |
| Route | `routes/AppRoutes.jsx` | Sửa | Bảng `KHUNG_THEO_VAI_TRO`, gộp conflict với Minh |
| Dùng chung | `components/common/HomeFrame`, `PendingList`, `StatusCounter`, `SummaryCard` (.jsx + .css) | Mới | Khối trang chủ |
| Trang chủ | `pages/home/EditorHome.jsx`, `SecretaryHome.jsx`, `HeadHome.jsx`, `AccountantHome.jsx`, `AdminHome.jsx` | Mới | |
| Trang chủ | `pages/home/ReporterHome.jsx`, `TodoCard.jsx`, `KpiProgressCard.*` | Sửa | Dùng khối chung, giao diện không đổi |
| Trang chủ | `pages/home/ReporterHome.css`, `TodoCard.css` | Xoá / chuyển | Nội dung chuyển vào CSS của khối chung |
| API | `api/duLieuGia.js` | Mới | Hàm giả lập dùng chung |
| API | `api/editorHomeApi.js`, `secretaryHomeApi.js`, `headHomeApi.js`, `accountantHomeApi.js`, `adminHomeApi.js` | Mới | Dữ liệu mẫu |
| API | `api/royaltyApi.js` | Mới | API thật BF-04: danh sách đơn giá |
| API | `api/reporterHomeApi.js` | Sửa | Dùng hàm trong `duLieuGia.js` |
| Xoá | `pages/Main.jsx` | Xoá | Không vai trò nào dùng nữa |
| Tài liệu | `AGENTS.md` mục 4 | Sửa | Cây thư mục + quy tắc thêm route |

### 3.1 `layouts/AppLayout.jsx` — khung chung

Nhận props:

| Prop | Ý nghĩa |
|---|---|
| `nguoiDung` | để hiện họ tên, tên vai trò, chữ cái avatar |
| `onDangXuat` | hàm gọi khi bấm Đăng xuất |
| `tenMenu` | tên menu cho trình đọc màn hình (`aria-label`) |
| `nhomMenu` | `[{ tieuDe, cacMuc: [{ path, nhan, icon }] }]` — `tieuDe` rỗng = nhóm không tiêu đề |
| `phanThem` | thứ hiện thêm trên thanh trên, trước tên (Phóng viên truyền chip ví) |
| `children` | nội dung trang |

- Mục **"Trang chủ"** (`/main`) AppLayout **tự thêm** ở đầu, các vai trò không phải khai báo.
- Menu dài (Trưởng ban, Admin) chia **nhóm có tiêu đề** ("DUYỆT BÀI", "KPI & BÁO CÁO"...). Menu tự cuộn nếu dài hơn màn hình (`overflow-y: auto`).
- Màn hẹp (≤ 960px): sidebar thu còn icon; chữ được **ẩn kiểu `.an-di`** (vẫn đọc được bằng trình đọc màn hình) chứ không `display:none`; tiêu đề nhóm được thay bằng **đường kẻ ngang**.
- Class CSS tiền tố `khung-app__` (trước là `khung-pv__`, đổi vì giờ không riêng Phóng viên).

### 3.2 Menu của từng vai trò

Mỗi mục ghi UC trong comment. Nguyên tắc: **UC ghi Actor nào thì chỉ vai trò đó thấy**. UC chỉ là 1 bước bên trong màn (vd UC-24 chấm từng tiêu chí) thì không thành mục menu riêng.

**Phóng viên** (không đổi): Bài của tôi (UC-01), Soạn bài mới (UC-03), Xác nhận đồng tác giả (UC-12), Khiếu nại điểm (UC-35), Nhuận bút (UC-57), KPI cá nhân (UC-60, 67).

**Biên tập viên:**

| Mục | Path | UC |
|---|---|---|
| Bài được giao | `/assigned-articles` | UC-21 (màn chấm UC-22…28 mở từ đây) |
| Chấm lại & chấm chéo | `/re-reviews` | UC-36, 37 + phiếu `cross_review_tasks` |
| Lịch sử chấm | `/my-scorings` | UC-21 (bài đã chấm xong) |

**Thư ký toà soạn:**

| Mục | Path | UC |
|---|---|---|
| Chấm nhuận bút | `/royalty-grading` | UC-46 (màn chấm UC-47, 48 mở từ đây) |
| Kỳ nhuận bút | `/royalty-periods` | UC-49, 50 (kỳ bị trả về UC-52 cũng xem ở đây) |

**Trưởng ban** (3 nhóm):

| Nhóm | Mục | Path | UC |
|---|---|---|---|
| Duyệt bài | Phân công chấm | `/assignments` | 18, 19, 20 |
| | Duyệt điểm | `/score-approvals` | 29…32 |
| | Duyệt đăng | `/publishing` | 33 |
| Chất lượng chấm | Khiếu nại | `/appeals` | 38, 39, 37, 43 |
| | Đợt chấm chéo | `/cross-reviews` | 40 |
| | Độ lệch & hồ sơ BTV | `/reviewers` | 41, 42 |
| | Nhật ký chấm | `/audit-logs` | 45 |
| KPI & báo cáo | Bảng KPI của ban | `/kpi-board` | 61, 62, 66 |
| | Giao chỉ tiêu tháng | `/kpi-targets` | 59 — **trang của Khang** |
| | Định mức chuẩn | `/kpi-benchmarks` | 58 — **trang của Khang** |
| | Báo cáo | `/kpi-reports` | 64, 65, 68 |

**Kế toán:**

| Mục | Path | UC |
|---|---|---|
| Kỳ nhuận bút | `/royalty-periods` | 49, 51, 52, 53 (duyệt / trả / khoá trong màn chi tiết kỳ) |
| Chi trả | `/royalty-payments` | 54 |
| Đơn giá nhuận bút | `/royalty-rates` | 55 |

**Admin** (3 nhóm):

| Nhóm | Mục | Path | UC |
|---|---|---|---|
| Hệ thống | Tài khoản | `/users` | chưa có UC — theo mô tả Actor 1 |
| | Thể loại | `/genres` | chưa có UC — theo mô tả Actor 1 |
| | Tham số hệ thống | `/system-parameters` | 13 |
| Chấm điểm & khiếu nại | Bộ tiêu chí | `/criteria-sets` | 14…17 |
| | Cấu hình khiếu nại | `/appeal-rules` | 44 — **trang của Vũ** |
| | Hồ sơ biên tập viên | `/reviewers` | 42 |
| | Nhật ký hệ thống | `/audit-logs` | 45 |
| Nhuận bút & KPI | Hệ số ảnh/video | `/media-factors` | 56 |
| | Định mức KPI chuẩn | `/kpi-benchmarks` | 58 — **trang của Khang** |
| | Nhập số liệu độc giả | `/reader-imports` | 63 |

> "Cấu hình khiếu nại" (UC-44) **không** có trong menu Trưởng ban vì Actor của UC-44 là Admin.

**Chip ví tiền chỉ Phóng viên có**: theo `schema.sql` mỗi user 1 vai trò, các vai trò khác không viết bài nên không nhận nhuận bút.

### 3.3 `routes/AppRoutes.jsx` — bảng vai trò

```js
const KHUNG_THEO_VAI_TRO = {
  REPORTER:            { Layout: ReporterLayout,   TrangChu: ReporterHome,   trangChuaLam: TRANG_PV_CHUA_LAM },
  EDITOR:              { Layout: EditorLayout,     TrangChu: EditorHome,     trangChuaLam: TRANG_BTV_CHUA_LAM },
  ...
  ADMIN:               { Layout: AdminLayout,      TrangChu: AdminHome,      trangChuaLam: TRANG_ADMIN_CHUA_LAM },
}
```

- Trước đây mỗi vai trò phải viết 1 khối `if` riêng; giờ chỉ là **1 dòng trong bảng**. Thêm vai trò mới = thêm 1 dòng.
- `TRANG_xx_CHUA_LAM`: danh sách `{ path, ten }` của trang chưa làm → tự sinh route trỏ tới `ComingSoon` ("Chức năng này đang được xây dựng"), để bấm menu không bị đẩy về trang chủ.
- **Trang đã làm** khai báo riêng, theo vai trò:
  - Phóng viên: `/my-articles`, `/my-articles/:id` (**Minh, BF-01**)
  - Trưởng ban: `/kpi-targets`, `/kpi-benchmarks` (**Khang, BF-05**)
  - Admin: `/appeal-rules` (**Vũ, BF-03**), `/kpi-benchmarks` (**Khang**)
- `<Layout>` / `<TrangChu>`: lấy component từ bảng ra biến tên **viết hoa** (`const Layout = khung?.Layout`) vì JSX chỉ coi tên viết hoa là component.
- Cùng 1 path (vd `/kpi-benchmarks`, `/audit-logs`) có thể dùng cho 2 vai trò vì mỗi lần chỉ nhóm route của 1 vai trò tồn tại.

### 3.4 `components/common/` — 4 khối trang chủ

| Component | Props chính | Vẽ gì |
|---|---|---|
| `HomeFrame` | `nguoiDung`, `cotPhai`, `children`, `goiCaTen` | "Xin chào, <tên>" + ngày + lưới 2 cột (trái `children`, phải `cotPhai`) |
| `PendingList` | `danhSach` (null = đang tải), `loi` | Khối **"Chờ xử lý"**: số đếm đỏ, mỗi việc 1 dòng + nút. Có hạn chót thì hiện "còn N ngày", ≤ 1 ngày tô đỏ |
| `StatusCounter` | `tieuDe`, `cacNhom` (null = đang tải), `donVi`, `hienTong`, `loi` | Cột đếm bên phải, mỗi nhóm 1 thẻ bấm được |
| `SummaryCard` | `tieuDe`, `nhan`, `cacDong`, `lienKet`, `dangTai`, `loi` | Thẻ tóm tắt "nhãn ... số" + link "Xem chi tiết →" |

Hình dạng 1 việc trong `PendingList`:

```js
{ khoa, loai, mau, tieuDe, moTa, hanChot, duongDan, nut }
// mau: tên màu chấm (.cham--hong, .cham--cam...); hanChot: chuỗi ngày giờ hoặc null
```

- `hienTong={false}` khi các nhóm **chồng nhau hoặc khác đơn vị** (vd Thư ký: "Sắp tới hạn chi" nằm trong "Chờ chấm"; Trưởng ban: đếm cả bài lẫn khiếu nại) → cộng lại vô nghĩa nên ẩn tổng.
- `goiCaTen`: bình thường chào bằng **chữ cuối của họ tên** ("Nguyễn Văn An" → "An"). Tài khoản admin tên là "Quản trị viên" → chữ cuối là "viên" → sai, nên trang Admin truyền `goiCaTen` để chào bằng cả tên.
- Màn hẹp (≤ 1100px): 2 cột thành 1 cột, cột đếm tự **lên đầu** (`order: -1`), thẻ đếm xếp lưới.

Tên "Cần bạn xử lý" đã đổi thành **"Chờ xử lý"** theo yêu cầu.

### 3.5 Trang chủ từng vai trò — hiện gì, lấy từ đâu

| Vai trò | Cột đếm bên phải | Chờ xử lý | Thẻ tóm tắt |
|---|---|---|---|
| **Phóng viên** (không đổi) | Trạng thái bài (7 nhóm theo `articles.status`) | Mời đồng tác giả, bài bị trả về, bài còn hạn khiếu nại | KPI tháng này |
| **Biên tập viên** | Bài được giao: Chờ chấm, Chờ Trưởng ban duyệt, Đã trả PV sửa, Đã công bố điểm, Từ chối | Trưởng ban trả chấm lại (UC-32, kèm lý do), chấm lại do khiếu nại (UC-36, chấm mù), bài mới giao (UC-21), chấm chéo (UC-40) | Tháng này: bài đã chấm, điểm TB đã cho, bài bị khiếu nại |
| **Thư ký** | Chờ chấm nhuận bút, Sắp tới hạn chi, Đã chấm trong kỳ | Kỳ bị Kế toán trả về (UC-52, nếu có), bài chờ chấm + **"còn N ngày"** tới hạn chi (UC-46) | Kỳ nhuận bút tháng này: nhãn trạng thái kỳ, bài đã/chưa chấm, tổng tiền tạm tính |
| **Trưởng ban** | Chờ phân công, Đang chấm, Chờ duyệt điểm, Chờ duyệt đăng, Khiếu nại đang mở | Khiếu nại chờ xác nhận điểm trọng tài S2 (UC-37), chờ duyệt điểm (UC-29), chờ phân công (UC-18) | KPI của ban tháng này: dự kiến đạt / chưa đạt / chưa giao chỉ tiêu, nhãn "Chưa chốt"/"Đã chốt" |
| **Kế toán** | Kỳ nhuận bút năm nay theo trạng thái (OPEN / CLOSED / LOCKED / PAID) | Kỳ chờ duyệt bảng kê (UC-51), kỳ đã khoá còn người chưa chi (UC-54) | **Đơn giá nhuận bút — API THẬT**: lương cơ sở, đơn giá 1 hệ số, áp dụng từ; có đơn giá tương lai thì hiện thêm "Đơn giá mới từ …" |
| **Admin** | Tài khoản hoạt động / bị khoá, Thể loại đang dùng, Bộ tiêu chí nháp | Thể loại chưa có bộ tiêu chí ACTIVE (BTV không chấm được), bộ tiêu chí DRAFT chưa kích hoạt (UC-17), chưa nhập CSV độc giả hôm nay (UC-63) | **Quy tắc khiếu nại — API THẬT**: thời hạn, số lần tối đa, ngưỡng lệch, áp dụng từ |

Thứ tự việc trong "Chờ xử lý": **việc đang chặn người khác / gấp nhất lên trước** (vd Trưởng ban: khiếu nại đang treo điểm của PV → duyệt điểm → phân công).

**Tìm đơn giá đang áp dụng (Kế toán)** — `timDonGia()` trong `AccountantHome.jsx`:

```
API trả danh sách đơn giá, MỚI NHẤT TRƯỚC.
Duyệt từ đầu:
  - dòng có effectiveFrom > hôm nay  -> là đơn giá TƯƠNG LAI, nhớ lại (sapApDung)
  - gặp dòng đầu tiên effectiveFrom <= hôm nay -> đó là đơn giá ĐANG ÁP DỤNG, dừng
```

Đúng luật BR-RY-03: *đơn giá áp dụng = dòng có `effective_from` lớn nhất mà ≤ ngày cần tính*. Ngày dạng `"2026-07-01"` so sánh trực tiếp bằng chuỗi được vì cùng độ dài, thứ tự năm-tháng-ngày. "Hôm nay" lấy theo **giờ máy** (không dùng `toISOString()` vì đó là giờ UTC, từ 0h đến 7h sáng ở VN sẽ ra ngày hôm trước).

### 3.6 File API

- `api/duLieuGia.js`: 2 hàm dùng chung cho các file dữ liệu mẫu — `giaLapGoiApi(duLieu)` (trả Promise sau 300ms, trả **bản sao** bằng `structuredClone` để trang lỡ sửa object cũng không hỏng dữ liệu gốc) và `cachHomNay(soNgay)` (tạo ngày giờ tương đối để hạn chót lúc nào demo cũng hợp lý). Trước đây 2 hàm này nằm riêng trong `reporterHomeApi.js`.
- `api/xxxHomeApi.js` (5 file): mỗi file 1 hàm `layTrangChu...()`. Comment đầu hàm ghi rõ **mỗi field lấy từ bảng/cột nào, UC nào** → người làm backend biết phải viết query gì.
- `api/royaltyApi.js`: file API của luồng BF-04, hiện có `layDanhSachDonGia()` gọi `GET /api/royalty-rates` qua `goiApi` (tự gắn token).

### 3.7 Xoá `pages/Main.jsx`

Trước: Kế toán và Admin vào `Main.jsx` (thanh tab). Admin có 2 tab "Cấu hình định mức chuẩn" (Khang) và "Cấu hình khiếu nại" (Vũ); Kế toán không có tab nào. Giờ 2 tab đó là **2 mục menu Admin** trỏ tới **đúng component cũ** (`KpiBenchmarksConfig`, `AppealRuleConfigPage`) — code của Khang và Vũ **không bị sửa**. Không còn route nào dùng `Main.jsx` nên xoá.

Lấy lại nếu cần: `git checkout <commit trước> -- client/src/pages/Main.jsx`.

### 3.8 Gộp conflict với PR #15 của Minh

Khi `git pull origin main`, `AppRoutes.jsx` bị conflict vì Minh cũng sửa file này (thêm 2 trang BF-01). Cách gộp: **giữ cả 2 bên**
- Giữ bảng 6 vai trò của mình.
- Thêm 2 route thật của Minh vào phần "trang đã làm" của Phóng viên.
- Git tự bỏ 2 dòng `/my-articles`, `/my-articles/:id` khỏi `TRANG_PV_CHUA_LAM`.

Đã kiểm tra code của Minh không dùng class CSS nào bị đổi tên/xoá (`trang-chu-pv`, `viec-pv`...).

### 3.9 `AGENTS.md`

- Cây thư mục mục 4: `layouts/` (AppLayout + 1 file/vai trò), `components/common/` (thêm 4 khối trang chủ).
- Quy tắc 3 mục 4: route mới thêm thế nào (Phần 6 dưới đây); bỏ câu nhắc `Main.jsx`.

---

## Phần 4 — Kiểm thử đã chạy

### 4.1 Build + lint

- `npm run build`: **chạy được**.
- `npx oxlint src`: chỉ còn **1 cảnh báo cũ** — `MonthlyTargetSetting.jsx` (Khang) có prop `nguoiDung` không dùng. Không phải do phần này.
- `mvn -f server/pom.xml compile` sau khi gộp main: **chạy được**.

### 4.2 Trình duyệt (localhost:5173, đăng nhập bằng 6 tài khoản mẫu)

| Kiểm tra | Kết quả |
|---|---|
| Mỗi tài khoản thấy đúng menu của vai trò mình (`an.nv`, `cuong.lh`, `dung.pt`, `duc.vm`, `chi.hl`, `admin`) | Đạt |
| Trang chủ 6 vai trò hiện đủ: lời chào, cột đếm, "Chờ xử lý", thẻ tóm tắt; không có lỗi | Đạt |
| Kế toán: thẻ đơn giá lấy từ DB thật (lương cơ sở 2.340.000 ₫, đơn giá 234.000 ₫, từ 01/01/2026) | Đạt |
| Admin: thẻ quy tắc khiếu nại lấy từ DB thật (7 ngày, 1 lần/bài, ngưỡng 10) | Đạt |
| Trưởng ban mở "Giao chỉ tiêu tháng", "Định mức chuẩn" (Khang) — API trả 200 | Đạt |
| Admin mở "Cấu hình khiếu nại" (Vũ), "Định mức KPI chuẩn" (Khang) | Đạt |
| Phóng viên mở "Bài của tôi" → ra trang thật của Minh (không phải ComingSoon) | Đạt (API bài viết cần chạy lại server 8080) |
| Gõ URL của vai trò khác (vd BTV vào `/assignments`) → về `/main` | Đạt |
| Đăng xuất rồi vào `/main` → về `/login` | Đạt |
| Phóng viên: giao diện + chip ví như trước khi tách khung | Đạt |
| Màn rộng 1366px: 2 cột (695px + 320px), không cuộn ngang | Đạt |
| Màn tablet: sidebar chỉ còn icon, nhóm menu ngăn bằng đường kẻ | Đạt |
| Điện thoại 375px: 1 cột, cột đếm lên đầu, nút xuống dưới chữ, không cuộn ngang | Đạt |

---

## Phần 5 — Câu hỏi thầy có thể hỏi

**H: Sao không viết 6 layout riêng cho dễ?**
Đ: 6 layout giống nhau 90% (sidebar, thanh trên). Viết riêng thì sửa 1 chỗ phải sửa 6 file, dễ lệch nhau. Tách `AppLayout` dùng chung, mỗi vai trò chỉ khai báo danh sách menu.

**H: `<Outlet />` để làm gì?**
Đ: Là chỗ trống trong khung để route con hiện vào. Đổi trang chỉ thay phần Outlet, khung (sidebar, thanh trên) giữ nguyên, không tải lại.

**H: Ẩn menu ở frontend thì có an toàn không?**
Đ: Không. Đây chỉ là chặn giao diện cho đúng luồng. Ai cũng gọi thẳng API được, nên **backend vẫn phải chặn theo vai trò** (`hasRole(...)` trong `SecurityConfig`, trả 403). Phần này chưa làm (Phần 8).

**H: Số liệu trên trang chủ lấy ở đâu?**
Đ: Phần lớn là dữ liệu mẫu trong `api/xxxHomeApi.js` vì backend chưa có API. Tên field theo DTO, trạng thái theo CHECK của schema, nên khi có API chỉ đổi ruột hàm. Riêng đơn giá (Kế toán) và quy tắc khiếu nại (Admin) là API thật.

**H: Đơn giá "đang áp dụng" tính thế nào?**
Đ: Theo BR-RY-03: dòng có `effective_from` lớn nhất mà ≤ hôm nay. API trả mới nhất trước, duyệt đến dòng đầu tiên có ngày ≤ hôm nay là dừng; các dòng trước đó là đơn giá tương lai.

**H: Vì sao "Cấu hình khiếu nại" không có trong menu Trưởng ban?**
Đ: Actor của UC-44 là Admin. Menu theo đúng cột Actor của từng UC.

**H: "Tài khoản", "Thể loại" của Admin là UC nào?**
Đ: SRS chưa có UC riêng; lấy theo mô tả Actor 1 (SRS_Overview 4.1: Admin quản lý tài khoản, thể loại) và bảng `users`, `genres` thuộc luồng admin.

**H: `useEffect` có `return () => { conHienThi = false }` để làm gì?**
Đ: Nếu rời trang trước khi API trả về thì không `setState` vào component đã bị gỡ.

**H: Sao không hiện tổng ở cột đếm của Thư ký / Trưởng ban / Admin?**
Đ: Các nhóm chồng nhau (bài "sắp tới hạn" nằm trong "chờ chấm") hoặc khác đơn vị (bài vs khiếu nại vs tài khoản) — cộng lại ra số vô nghĩa.

---

## Phần 6 — Hướng dẫn cho nhóm: thêm 1 trang mới

Ví dụ: làm xong trang "Kỳ nhuận bút" (`RoyaltyPeriodList.jsx`) cho Thư ký.

1. Tạo `pages/royalty/RoyaltyPeriodList.jsx` (+ `.css` cạnh nó, class có tiền tố riêng).
2. Mở `routes/AppRoutes.jsx`:
   - **Xoá** dòng `{ path: '/royalty-periods', ten: 'Kỳ nhuận bút' }` trong `TRANG_TK_CHUA_LAM`.
   - **Thêm** route thật vào phần "Trang da lam":
     ```jsx
     {vaiTro === 'EDITORIAL_SECRETARY' && (
       <Route path="/royalty-periods" element={<RoyaltyPeriodList nguoiDung={nguoiDung} />} />
     )}
     ```
   - Nếu Kế toán cũng dùng trang đó → xoá dòng tương ứng trong `TRANG_KT_CHUA_LAM` và thêm route cho `ACCOUNTANT`.
3. Menu **không phải sửa** (mục menu đã trỏ sẵn `/royalty-periods`).
4. Trang chủ đang dùng dữ liệu mẫu → khi có API, sửa ruột hàm trong `api/xxxHomeApi.js` thành `goiApi(...)`.

Thêm mục menu mới → sửa mảng menu trong `layouts/<VaiTro>Layout.jsx` + thêm path vào `TRANG_xx_CHUA_LAM`.

---

## Phần 7 — Việc cả nhóm phải biết sau khi pull

| Ai | Việc |
|---|---|
| Cả nhóm | Tắt `npm run dev` rồi chạy lại (Vite đôi khi không nhận file đổi tên/chuyển chỗ). **Chạy lại server 8080** để có API bài viết của Minh. |
| Khang | 2 trang KPI không còn nằm trong tab `Main.jsx`: Trưởng ban mở ở `/kpi-targets`, `/kpi-benchmarks`; Admin mở ở `/kpi-benchmarks`. Code không bị sửa. |
| Vũ | Trang cấu hình khiếu nại giờ ở menu Admin `/appeal-rules`. Code không bị sửa; trang chủ Admin gọi `getActiveConfig()` của Vũ. |
| Minh | 2 trang BF-01 đã gộp vào route Phóng viên (Phần 3.8). Trang chủ PV vẫn dùng dữ liệu mẫu — có thể thay bằng `getMyStatusCounts`, `getMyRevisionRequests`, `getMyCoAuthorInvites` trong `articleApi.js`. |
| Người làm trang mới | Làm theo Phần 6. File dùng chung (`AppLayout`, `AppRoutes`, `components/common/`) sửa phải báo nhóm. |

---

## Phần 8 — Hạn chế còn lại / việc tiếp theo

1. **Chưa chặn theo vai trò ở backend (403).** Hiện mọi API chỉ cần đăng nhập. Cần thêm `.hasRole(...)` trong `SecurityConfig` cho từng API.
2. **Dữ liệu mẫu** trên hầu hết trang chủ — cần API thật của từng luồng.
3. **Hạn chi nhuận bút (BF-04):** SRS nói hạn chi tính từ ngày đăng (UC-33), nhưng `data.sql` chưa có tham số số ngày → "còn N ngày" trên trang Thư ký đang là số giả. Cần thêm vào `system_parameters` (PR `[DB]`).
4. **"Tài khoản", "Thể loại"** của Admin chưa có UC trong SRS — nếu nhóm không làm thì xoá 2 dòng trong `AdminLayout.jsx` và `TRANG_ADMIN_CHUA_LAM`.
5. **Avatar Admin hiện chữ "V"** (lấy chữ cái đầu của chữ cuối "Quản trị viên"). Không sai chức năng, chỉ chưa đẹp.
6. **Ô tìm kiếm và chuông thông báo** trên thanh trên chưa hoạt động (đã ghi TODO trong `AppLayout.jsx`).
7. Trang chủ 5 vai trò mới **không có ảnh Figma riêng** — dùng lại bố cục, màu, kiểu thẻ của trang chủ Phóng viên (đã làm theo Figma).
