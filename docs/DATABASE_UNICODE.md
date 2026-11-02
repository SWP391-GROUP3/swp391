# Ghi tiếng Việt vào database

SQL Server của dự án đã dùng `NVARCHAR` cho các cột tiếng Việt. Kiểu này lưu
Unicode dù database có collation `SQL_Latin1_General_CP1_CI_AS`.
Không cần đổi collation hay chạy lại `schema.sql` để ghi tiếng Việt.

## Khi viết SQL

Lưu file bằng **UTF-8** và luôn thêm **`N` trước chuỗi có tiếng Việt**:

```sql
-- Ví dụ minh họa, chỉ chạy nếu thực sự cần sửa bản ghi này.
UPDATE users SET full_name = N'Nguyễn Văn An' WHERE id = 1;
```

`N'Nguyễn Văn An'` giữ nguyên Unicode. Chuỗi thiếu `N` có thể mất dấu
ngay khi SQL Server đọc câu lệnh, trước khi ghi vào cột `NVARCHAR`.
File UTF-8 và cột `NVARCHAR` không tự ngăn được bước chuyển mã này.

Trong VS Code, bấm tên encoding ở thanh trạng thái → **Save with Encoding**
→ **UTF-8 with BOM** cho file SQL (đúng `.editorconfig`, giúp công cụ Windows nhận mã hóa).
Nếu file đang hiển thị sai chữ, dùng **Reopen with Encoding**
đúng với mã hóa gốc trước khi lưu; lưu UTF-8 không tự phục hồi chữ đã hỏng.
Repo đã có `.editorconfig` quy định UTF-8 cho file SQL.

## Khi chạy ứng dụng

`server/src/main/resources/application.properties` cố định:

```properties
spring.datasource.hikari.data-source-properties.sendStringParametersAsUnicode=true
```

JPA/JDBC gửi tham số chuỗi bằng Unicode. Không nối chuỗi tiếng Việt để tự
tạo câu SQL; dùng tham số như code hiện tại. Khởi động lại server sau khi
đổi cấu hình. Giữ `spring.jpa.hibernate.ddl-auto=validate`.

## Kiểm tra

Mở và chạy [`unicode_write_check.sql`](../database/checks/unicode_write_check.sql).
Kết quả phải là `PASS` và hiển thị nguyên câu tiếng Việt. Script chỉ dùng
biến bảng tạm trong câu lệnh, không sửa bảng hay dữ liệu thật. So sánh theo
byte giúp phát hiện cả trường hợp công cụ đã mở file sai encoding.

Đã kiểm tra trên database ngày 26/10/2026: JDBC `setString`, `setNString` và
SQL literal `N'...'` đều ghi/đọc đúng; literal thiếu `N` làm mất dấu.
Phép thử JDBC dùng bảng tạm và rollback.

Một số giá trị cũ trong database đang có dạng `Nguyá»…n VÄƒn An`.
Đây là nội dung đã bị chuyển mã sai từ lần nhập trước. Cấu hình Unicode
không tự sửa các giá trị đó; cần bản gốc hoặc sửa dữ liệu có sao lưu riêng.
Lần thay đổi này giữ nguyên dữ liệu cũ, lịch sử, điểm số và cấu trúc bảng.
