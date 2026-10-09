# SWP391 — Hệ thống đánh giá chất lượng & quản lý nhuận bút

Mô phỏng khâu hậu kỳ toà soạn: phóng viên nộp bài → biên tập → duyệt xuất bản →
chấm chất lượng → tính nhuận bút theo kỳ → chốt sổ và lên bảng kê.

```
swp391/
├── AGENTS.md     quy tắc code chung cho cả nhóm và mọi AI (đọc trước khi code)
├── CLAUDE.md     chỉ import AGENTS.md cho Claude Code
├── .claude/      hook bắt Claude Code đọc tài liệu trước khi sửa code
├── docs/         SRS, SDS, coding plan, sơ đồ kiến trúc
├── database/     schema.sql + data.sql  (chạy trong SSMS)
├── server/       Spring Boot 3.5 + JPA + SQL Server   → cổng 8080
└── client/       React 19 + Vite 8                    → cổng 5173
```

Client **không** kết nối database. Mọi truy cập dữ liệu đi qua REST API của server.
Khi chạy dev, Vite chuyển mọi request `/api/...` sang server cổng 8080, nên server
không cần cấu hình CORS.

**Quy tắc code đầy đủ nằm ở [`AGENTS.md`](AGENTS.md).** README này chỉ hướng dẫn
cài đặt và chạy. Hai file mà khác nhau thì theo `AGENTS.md`.

---

## 1. Cần cài sẵn

| Phần mềm | Bản | Ghi chú |
|---|---|---|
| JDK | 17 | `java -version` phải ra 17. Cả nhóm dùng **cùng một bản** |
| Maven | 3.9+ | hoặc dùng Maven tích hợp trong IntelliJ |
| SQL Server | 2019+ | bản Developer hoặc Express đều được |
| SSMS | mới nhất | để chạy file `.sql` |
| Node.js | 20.19+ hoặc 22.12+ | Vite 8 bắt buộc. `node -v` để kiểm tra. Hook của Claude Code cũng chạy bằng Node |
| IntelliJ IDEA | | Community hoặc Ultimate đều được, client không còn dùng JSP/Tomcat |

## 2. Dựng database

Mở SSMS, kết nối tới máy của bạn, rồi chạy lần lượt:

1. `database/schema.sql` — tạo database `swp391_kpi` và toàn bộ bảng
2. `database/data.sql` — nạp dữ liệu mẫu (ban, thể loại, barem, định mức...)

File `schema.sql` chạy lại được nhiều lần: nó xoá sạch bảng cũ rồi tạo lại.
Chạy lại là **mất hết dữ liệu**, nên chỉ chạy khi schema có thay đổi.

**Mỗi người dùng SQL Server trên máy mình.** Không dùng chung một DB — chỉ cần
một người chạy nhầm là cả nhóm đứng.

## 3. Chạy server

Copy file cấu hình rồi sửa mật khẩu:

```bash
cp server/src/main/resources/application-local.properties.example server/src/main/resources/application-local.properties
```

Mở `application-local.properties`, sửa `spring.datasource.password` thành mật khẩu
`sa` của bạn. File này đã nằm trong `.gitignore` nên **không bị commit** — đúng như vậy.

```bash
mvn -f server/pom.xml spring-boot:run
```

Kiểm tra: mở `http://localhost:8080/api/ping`, phải thấy JSON có `"trangThai":"OK"`.

## 4. Chạy client

Lần đầu, hoặc mỗi khi `client/package.json` thay đổi, cài thư viện:

```bash
cd client
npm install
```

Chạy dev server (trong thư mục `client/`):

```bash
npm run dev
```

Mở `http://localhost:5173/`. Chưa đăng nhập thì trang tự chuyển về `/login`.

Trong code React chỉ gọi API qua các file trong `src/api/`, URL dạng `/api/...`,
**không** viết `http://localhost:8080`. Vite tự chuyển request sang server (cấu hình
ở `client/vite.config.js`), nên muốn gọi API thật thì server phải đang chạy.

Đăng nhập hiện là **giả** (`client/src/auth/taiKhoanTam.js`), tài khoản dùng thử
nằm ngay dưới form. Phải thay bằng API đăng nhập thật trước khi nộp bài.

Trước khi mở PR, chạy lint và build:

```bash
npm run lint
npm run build
```

## 5. Chạy test

```bash
mvn -f server/pom.xml test
```

Test logic tính toán thuần (chia tiền, tính điểm...) không cần database, chạy
nhanh. Hiện chưa có test nào: luồng `royalty` đang được làm lại từ đầu.

## 6. Dùng AI để code (Claude Code, Codex, Antigravity)

- Luôn mở AI **tại thư mục gốc `swp391/`**, để nó tự đọc `AGENTS.md` (Claude Code
  đọc qua `CLAUDE.md`). Mở trong `server/` hay `client/` thì AI không thấy quy tắc.
- **Trước khi code, AI phải đọc tài liệu** trong `docs/` và `database/schema.sql`
  theo danh sách ở mục *Bước 0* của `AGENTS.md`. Làm luồng nào thì đọc thêm tài
  liệu UC của luồng đó.
- Với **Claude Code**, việc này được hook `.claude/hooks/require-docs-read.js`
  cưỡng chế: chưa đọc đủ thì không sửa được file trong `server/src/` hay
  `client/src/`. Lần đầu mở project, Claude Code có thể hỏi có tin hook của
  project không — chọn đồng ý.
- **Codex / Antigravity** không có hook: khi giao việc, nhắc AI làm *Bước 0*
  trước và kiểm tra câu trả lời có dòng `Đã đọc: ...`.
- Thêm tài liệu UC/SRS cho luồng mới vào `docs/` → cập nhật bảng *Bước 0* trong
  `AGENTS.md` **và** `FLOW_DOCS` trong file hook, trong cùng một PR.

---

## Bảng endpoint

Cả nhóm thống nhất bảng này **trước** khi code. Ai thêm endpoint thì sửa bảng
này trong cùng PR — người làm client dựa vào đây để làm giao diện song song,
không phải chờ server xong. Đặt tên theo `AGENTS.md` mục 3.7: tiền tố `/api`,
danh từ tiếng Anh số nhiều, `kebab-case`; hành động nghiệp vụ là `POST /{id}/<việc>`.

| Method | Đường dẫn | Mô tả | Vai trò | Trạng thái |
|---|---|---|---|---|
| GET | `/api/ping` | Kiểm tra server sống | công khai | ✅ xong |
| GET | `/api/royalty-periods` | Danh sách kỳ nhuận bút | TKTS, kế toán | |
| POST | `/api/auth/login` | Đăng nhập, trả JWT | công khai | |
| GET | `/api/articles` | Danh sách bài, có lọc & phân trang | mọi vai trò | |
| POST | `/api/articles` | Nộp bài mới | phóng viên | |
| POST | `/api/articles/{id}/<hành động>` | Chuyển trạng thái duyệt (qua `ArticleService`) | BTV, TKTS | |
| POST | `/api/quality-reviews` | Gửi phiếu chấm điểm chất lượng | BTV, TKTS | |
| POST | `/api/royalty-periods/{id}/close` | Đóng kỳ, báo kế toán (UC-50) | TKTS | |
| POST | `/api/royalty-periods/{id}/lock` | Khoá kỳ, sinh bảng kê | tổng biên tập | |
| GET | `/api/royalty-lines?periodId=&userId=` | Xem bảng kê | kế toán, cá nhân | |
| GET | `/api/kpi-reports?periodId=` | Báo cáo KPI theo kỳ | TKTS, TBT | |

Các dòng chưa làm chỉ là tên dự kiến: người phụ trách luồng chốt lại tên cuối cùng
khi làm và sửa bảng trong cùng PR.

---

## Quy tắc làm việc

Tóm tắt các quy tắc hay quên. Đầy đủ xem [`AGENTS.md`](AGENTS.md).

**Nhánh.** Không push thẳng `main` hay `develop`. Tách nhánh `feature/<luồng>-<việc>`
từ `develop`, mở PR vào `develop`, một người khác duyệt rồi mới merge. `main` chỉ
nhận merge từ `develop` để demo.

```bash
git checkout develop
git pull origin develop
git checkout -b feature/bf04-tao-ky-nhuan-but
```

**Commit.** `[BF-04] Thêm API tạo kỳ nhuận bút`. Việc chung ghi `[COMMON]`, sửa DB
ghi `[DB]`.

**Sửa database.** Sửa `database/schema.sql` rồi báo cả nhóm chạy lại trong SSMS,
PR ghi **"CÓ ĐỔI DB"** ở đầu mô tả. Không ai được sửa bảng trực tiếp bằng SSMS mà
không sửa file — máy bạn chạy được nhưng máy người khác thì không.

**Không đổi `ddl-auto`.** Đang để `validate`, tức Hibernate chỉ kiểm tra entity có
khớp schema hay không. Đổi sang `update` thì 5 người sửa entity song song sẽ làm
schema mỗi máy một khác, cột cũ không bị xoá, và đến tuần thứ ba không ai biết DB
thật đang chứa gì.

**Tiền luôn dùng `BigDecimal`.** Không `double`, không `float`. Nhân hệ số bằng
`double` sẽ ra `1193399.9999999998` và bảng kê lệch vài đồng mà không tìm ra
nguyên nhân.

**Logic tính toán thuần không import Spring.** Các lớp tính toán (vd chia tiền
nhuận bút cho đồng tác giả) giữ thuần Java để test chạy nhanh và chạy được hàng nghìn trường hợp. Cần dữ liệu
gì thì truyền vào qua tham số, không inject repository. Phần controller/service/
repository của luồng vẫn dùng Spring bình thường.

---

## Chia việc

Backend **chia theo tầng**: mọi luồng dùng chung các thư mục `controller/`,
`service/`, `repository/`, `entity/`, `dto/`, `exception/`, `config/` trong
`com.swp391.kpi`. File thuộc luồng nào thì nhận ra bằng **tiền tố tên class**
(vd `RoyaltyPeriodService` thuộc `royalty`). Người phụ trách luồng nào chủ yếu sửa
file có tiền tố của luồng đó (bảng tiền tố đầy đủ ở `AGENTS.md` mục 2).

| Luồng | Tiền tố tên class (ví dụ) | Người phụ trách |
|---|---|---|
| Dùng chung — sửa phải báo cả nhóm | `ApiResponse`, `exception/`, `config/` | cả nhóm |
| `admin` — Nền tảng / Admin, đăng nhập | `User`, `Role`, `Genre`... | *chưa chốt* |
| `article` — BF-01 Viết & nộp bài | `Article...` | |
| `assessment` — BF-02 Phân công & đánh giá chất lượng | `Assignment...`, `ScoringAttempt...` | |
| `publishing` — BF-03 Duyệt đăng | `Publishing...` | |
| `royalty` — BF-04 Chấm & chi nhuận bút | `Royalty...` | |
| `kpi` — BF-05 KPI & báo cáo | `Kpi...` | |
| `client/` — Giao diện React (vẫn chia theo luồng trong `pages/`) | | |

---

## Lỗi hay gặp

**`The driver could not establish a secure connection ... SSL`**
Thiếu `trustServerCertificate=true` trong chuỗi kết nối. File `.example` đã có sẵn,
kiểm tra lại xem bạn đã copy đúng chưa.

**`Login failed for user 'sa'`**
SQL Server đang ở chế độ Windows Authentication. Trong SSMS: chuột phải server →
Properties → Security → chọn *SQL Server and Windows Authentication mode*, rồi
**restart dịch vụ SQL Server**. Không restart thì không ăn.

**`TCP/IP connection to the host ... failed`**
Chưa bật TCP/IP. Mở SQL Server Configuration Manager → SQL Server Network
Configuration → Protocols → TCP/IP → Enable, đặt port 1433 ở mục IPAll, rồi restart.

**`Schema-validation: missing table [...]`**
Entity không khớp schema. Chạy lại `database/schema.sql`, hoặc sửa entity cho khớp.
Đây chính là tác dụng của `ddl-auto=validate` — nó bắt lỗi ngay lúc khởi động
thay vì để sai âm thầm.

**`npm run dev` báo `Port 5173 is already in use`**
Một tiến trình `npm run dev` cũ vẫn đang chạy. Tắt nó đi (Ctrl+C ở terminal cũ),
đừng đổi cổng: cả nhóm cố định 5173 (`strictPort` trong `vite.config.js`).

**Git báo PR sửa cả trăm dòng trong khi bạn chỉ sửa vài dòng**
Lệch kiểu xuống dòng. File `.gitattributes` đã xử lý việc này; nếu vẫn gặp thì
chạy `git config core.autocrlf false` rồi clone lại.

**Claude Code báo `CHAN: chua doc tai lieu bat buoc truoc khi code`**
Đây là hook ở mục 6 đang hoạt động đúng. Bảo AI đọc các file được liệt kê trong
thông báo rồi làm tiếp.
