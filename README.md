# SWP391 — Hệ thống đánh giá chất lượng & quản lý nhuận bút

Mô phỏng khâu hậu kỳ toà soạn: phóng viên nộp bài → biên tập → duyệt xuất bản →
chấm chất lượng → tính nhuận bút theo kỳ → chốt sổ và lên bảng kê.

```
swp391/
├── database/     schema.sql + data.sql  (chạy trong SSMS)
├── server/       Spring Boot 3.5 + JPA + SQL Server   → cổng 8080
└── client/       Servlet/JSP + Tomcat 10.1            → cổng 8081
```

Client **không** kết nối database. Mọi truy cập dữ liệu đi qua REST API của server.

---

## 1. Cần cài sẵn

| Phần mềm | Bản | Ghi chú |
|---|---|---|
| JDK | 17 | `java -version` phải ra 17. Cả nhóm dùng **cùng một bản** |
| Maven | 3.9+ | hoặc dùng Maven tích hợp trong IntelliJ |
| SQL Server | 2019+ | bản Developer hoặc Express đều được |
| SSMS | mới nhất | để chạy file `.sql` |
| Tomcat | **10.1** | bắt buộc 10.1, **không dùng Tomcat 9** |
| IntelliJ IDEA Ultimate | | bản Community không chạy được JSP/Tomcat |

> Tomcat 10.1 dùng namespace `jakarta.servlet.*`. Tomcat 9 dùng `javax.servlet.*`
> và sẽ **không** chạy được code trong repo này.

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

Trong IntelliJ: `Run → Edit Configurations → + → Tomcat Server → Local`.
Chọn Tomcat 10.1, tab **Deployment** thêm artifact `kpi-client:war exploded`,
tab **Server** đặt HTTP port = **8081**.

Mở `http://localhost:8081/`. Nếu server đang chạy, trang chủ hiện khung xanh
"Đã kết nối server".

## 5. Chạy test

```bash
mvn -f server/pom.xml test
```

Bộ test của `PhanBoTien` chạy trên **hơn 10.000 bộ dữ liệu ngẫu nhiên** để kiểm
tra các bất biến (tổng tiền chia luôn khớp, không ai nhận số âm, kết quả tất định).
Không cần database, chạy xong trong dưới một giây.

---

## Bảng endpoint

Cả nhóm thống nhất bảng này **trước** khi code. Ai thêm endpoint thì sửa bảng
này trong cùng PR — người làm client dựa vào đây để làm giao diện song song,
không phải chờ server xong.

| Method | Đường dẫn | Mô tả | Vai trò | Người làm |
|---|---|---|---|---|
| GET | `/api/ping` | Kiểm tra server sống | công khai | ✅ xong |
| POST | `/api/auth/login` | Đăng nhập, trả JWT | công khai | |
| GET | `/api/tac-pham` | Danh sách bài, có lọc & phân trang | mọi vai trò | |
| POST | `/api/tac-pham` | Nộp bài mới | phóng viên | |
| PUT | `/api/tac-pham/{id}/trang-thai` | Chuyển trạng thái duyệt | BTV, TKTS | |
| POST | `/api/tac-pham/{id}/cham-diem` | Gửi phiếu chấm điểm | BTV, TKTS | |
| GET | `/api/ky-tinh` | Danh sách kỳ | TKTS, kế toán | |
| POST | `/api/ky-tinh/{id}/tinh-thu` | Tính thử, chưa ghi DB | TKTS | |
| POST | `/api/ky-tinh/{id}/chot` | Chốt kỳ, sinh bảng kê | tổng biên tập | |
| GET | `/api/bang-ke?kyId=&nguoiDungId=` | Xem bảng kê | kế toán, cá nhân | |
| GET | `/api/bao-cao/kpi?kyId=` | Báo cáo KPI theo kỳ | TKTS, TBT | |

---

## Quy tắc làm việc

**Nhánh.** Không push thẳng `main`. Tạo nhánh `feature/<việc>`, mở PR, một người
khác duyệt rồi mới merge.

```bash
git checkout -b feature/dang-nhap
```

**Sửa database.** Sửa `database/schema.sql` rồi báo cả nhóm chạy lại trong SSMS.
Không ai được sửa bảng trực tiếp bằng SSMS mà không sửa file — máy bạn chạy được
nhưng máy người khác thì không.

**Không đổi `ddl-auto`.** Đang để `validate`, tức Hibernate chỉ kiểm tra entity có
khớp schema hay không. Đổi sang `update` thì 5 người sửa entity song song sẽ làm
schema mỗi máy một khác, cột cũ không bị xoá, và đến tuần thứ ba không ai biết DB
thật đang chứa gì.

**Tiền luôn dùng `BigDecimal`.** Không `double`, không `float`. Nhân hệ số bằng
`double` sẽ ra `1193399.9999999998` và bảng kê lệch vài đồng mà không tìm ra
nguyên nhân.

**Package `royalty` không import Spring.** Đây là lõi tính nhuận bút, giữ thuần
để test chạy nhanh và chạy được hàng nghìn trường hợp. Cần dữ liệu gì thì truyền
vào qua tham số, không inject repository. Nhắc nhau khi review.

---

## Chia việc

| Người | Phụ trách |
|---|---|
| 1 | Dựng khung, `schema.sql`, cấu hình, deploy — xong sớm rồi chuyển sang báo cáo |
| 2 | Package `royalty`: engine tính nhuận bút + bộ test bất biến |
| 3 | Entity cụm 1–2, đăng nhập JWT, phân quyền |
| 4 | Entity cụm 3–5, REST controller theo bảng endpoint |
| 5 | Toàn bộ client Servlet/JSP |

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

**JSTL viết đúng cú pháp nhưng in ra nguyên chuỗi `<c:forEach ...>`**
Sai URI taglib. Bản jakarta dùng `jakarta.tags.core`, không phải
`http://java.sun.com/jsp/jstl/core`.

**Git báo PR sửa cả trăm dòng trong khi bạn chỉ sửa vài dòng**
Lệch kiểu xuống dòng. File `.gitattributes` đã xử lý việc này; nếu vẫn gặp thì
chạy `git config core.autocrlf false` rồi clone lại.
