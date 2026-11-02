# Trả lời "BF-01 (Minh) gửi Quang – Những điểm còn lại để hoàn thiện Iter 3"

> Ngày 02/11/2026 · Người trả lời: Quang (nhóm trưởng, BF-04).
> Trả lời theo số dòng trong file của Minh. Dòng nào ghi "OK" là dùng đúng cách Minh đề xuất.

## Trả lời

| # | Trả lời |
|---|---|
| Q1 | **Quang làm** backend UC-13, **xong trước 03/11/2026** (nhánh `feature/admin-intake-parameters`). API **khác đề xuất**: không làm theo từng `paramKey`, mà gửi/nhận **cả 6 tham số trong 1 object** (chi tiết ở mục dưới). Minh làm màn hình theo API này |
| Q2 | **Cho Admin sửa** `SHINGLE_SIZE`, giới hạn **3–10 âm tiết** (mẫu 5). Code kiểm trùng phải đọc `SHINGLE_SIZE` qua `SystemParameterService`, không viết cứng số 5 |
| Q3 | **(a)**: dùng `new ResponseStatusException(HttpStatus.FORBIDDEN, "...")` như các luồng khác, **không sửa `exception/`**, không cần PR `[COMMON]`. Message tiếng Việt có dấu. **Thay cho câu trả lời A4 ngày 01/11** (bỏ `ForbiddenException`) |
| Q4 | **OK** thứ tự mới: 2 → 3 → 4 → 5 → 1 (kiểm trùng, khi Dương xong) → 6. Quang sẽ nhắc Dương mở PR D1 và làm D2 |
| Q5 | **OK** tách 3 PR: PR-1 tài liệu mở ngay; PR-2 đi cùng UC-02; PR-3 đi cùng upload. Mỗi PR ghi `[COMMON]` |
| Q6 | **OK**: ghép từ kiểu có sẵn. Không hiện toàn bộ bài của người khác (B7) |
| Q7 | **OK**: `GET /api/articles/{id}/attachments/{attachmentId}/file`, người được xem giống B8 |

## API tham số nhập bài (Q1)

Chỉ tài khoản `ADMIN` gọi được (cả GET và PUT), vai trò khác nhận 403.

**`GET /api/system-parameters/intake`**: lấy 6 tham số hiện tại.

**`PUT /api/system-parameters/intake`**: Admin sửa, body gửi **đủ 6 trường** (trường không đổi thì gửi giá trị cũ). Trả về 6 tham số sau khi sửa.

```json
{
  "maxAuthorsPerArticle": 5,
  "coAuthorConfirmDays": 3,
  "dupWarnPercent": 20,
  "dupBlockPercent": 50,
  "dhashMaxBits": 5,
  "shingleSize": 5
}
```

| Trường | `param_key` | Giới hạn |
|---|---|---|
| `maxAuthorsPerArticle` | `MAX_AUTHORS_PER_ARTICLE` | 1–5 |
| `coAuthorConfirmDays` | `CO_AUTHOR_CONFIRM_DAYS` | ≥ 1 |
| `dupWarnPercent` | `DUP_WARN_PERCENT` | 1–100, phải **nhỏ hơn** `dupBlockPercent` |
| `dupBlockPercent` | `DUP_BLOCK_PERCENT` | 1–100 |
| `dhashMaxBits` | `DHASH_MAX_BITS` | 0–64 |
| `shingleSize` | `SHINGLE_SIZE` | 3–10 |

Lỗi trả về:
- Sai giới hạn 1 ô → **400**, `errors` có tên trường và message, vd `{ "shingleSize": "Độ dài mảnh so trùng phải từ 3 âm tiết trở lên" }`. Hiện message dưới đúng ô đó.
- Ngưỡng cảnh báo ≥ ngưỡng chặn → **409**, message "Ngưỡng cảnh báo trùng lặp phải nhỏ hơn ngưỡng chặn."

Tham số nào thật sự đổi giá trị thì backend ghi 1 dòng `audit_logs` (giá trị cũ → mới). Giá trị mới chỉ áp dụng cho bài nộp **sau** khi sửa, kết quả kiểm trùng đã lưu của bài cũ không tính lại.

## Để Minh biết

- Lỗ hổng `GET /api/scoring-attempts/article/{articleId}/latest` không kiểm quyền: Quang sẽ theo dõi cùng Dương.
