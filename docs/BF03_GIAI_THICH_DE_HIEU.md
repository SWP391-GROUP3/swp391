# BF-03 — Giải thích dễ hiểu, kèm dòng code

> Bản này thay cho cách học "thuộc câu trả lời" ở [BF03_HOC_LUONG_CHO_SINH_VIEN.md](BF03_HOC_LUONG_CHO_SINH_VIEN.md). Mỗi phần chỉ có 3 thứ: **chuyện gì xảy ra**, **code nằm ở dòng nào**, **vì sao làm vậy**.
> Số dòng lấy từ nhánh `VuNQ` lúc viết. Sửa file thì dòng sẽ lệch vài số, cứ tìm theo tên hàm.
> Đường dẫn rút gọn: `S/` = `server/src/main/java/com/swp391/kpi/service/`, `C/` = `client/src/`.

---

## Cách lần theo trong code

Trong source đã có comment đánh dấu từng bước ở **cuối dòng code**, dạng:
`// [LUONG 1 - GUI KHIEU NAI] buoc 6/6 service: ...`

Mở IntelliJ / VS Code, bấm **Ctrl+Shift+F**, gõ rồi đọc kết quả theo thứ tự `buoc 1/…, 2/…`:

| Gõ tìm | Luồng | Ai bấm |
|---|---|---|
| `LUONG 1` | Gửi khiếu nại | Phóng viên |
| `LUONG 2` | Phân công (và giao lại) | Trưởng ban |
| `LUONG 3` | Chấm mù (lưu nháp / nộp phiếu) | Biên tập viên |
| `LUONG 4` | So sánh S0/S1/S2 và chốt điểm (có nhánh S2) | Trưởng ban |

Số dòng của các file không đổi vì comment nằm cuối dòng có sẵn, nên số dòng trong tài liệu này vẫn đúng.

---

## 0. Cả luồng BF-03 là chuyện gì (đọc đoạn này trước)

Bài của Phóng viên được chấm xong, ra điểm **S0**. Phóng viên không phục nên **khiếu nại**. Trưởng ban không tự sửa điểm, mà **giao cho một Biên tập viên (BTV) khác chấm lại từ đầu** — người này **không được biết S0**, nên gọi là *chấm mù*. Điểm chấm lại là **S1**.

Rồi hệ thống so S1 với S0:
- Hai điểm gần nhau → coi như S0 chấm đúng đoạn, lấy S1, xong.
- Hai điểm lệch nhiều → cần người thứ ba chấm **S2** để phân xử.

Cuối cùng Trưởng ban bấm **chốt**, điểm đó thành **điểm hiệu lực**.

Mọi thứ còn lại trong code (khoá, audit, thông báo...) chỉ để bảo vệ chuỗi này khỏi bị làm sai hoặc bị gian lận.

---

## 1. Một cú bấm đi qua những đâu (ví dụ: Phóng viên bấm "Gửi khiếu nại")

Nhớ 5 chặng, mỗi chặng một dòng:

| # | Chặng | Dòng code | Nó làm gì, nói bằng lời |
|---|---|---|---|
| 1 | Route | `C/routes/AppRoutes.jsx:233-234` | URL `/my-appeals/new` thì hiện trang `AppealCreatePage` |
| 2 | Trang bấm nút | `C/pages/appeal/AppealCreatePage.jsx:102` | `await guiKhieuNai(articleId, criteria)` |
| 3 | Hàm gọi API | `C/api/appealApi.js:30-31` | Gói thành `POST /api/appeals` rồi đưa cho `goiApi` |
| 4 | `goiApi` | `C/api/goiApi.js:26-35` | Gắn token vào header rồi `fetch`; `:47-52` nếu server báo lỗi thì ném `Error` tiếng Việt |
| 5 | Controller | `server/src/main/java/com/swp391/kpi/controller/AppealController.java:49-54` | Lấy id người dùng từ token (`:53`), gọi đúng 1 hàm service (`:54`) |

Hai điều hay bị hỏi:

**Vì sao Controller không có `if`?** Nhìn `AppealController.java:49-54`: chỉ 2 dòng làm việc. Mọi luật ("ai được khiếu nại, quá hạn chưa") nằm hết trong Service, nên muốn biết luật là gì chỉ cần mở một chỗ.

**Vì sao client không gửi "tôi là ai"?** Dòng `:53` lấy id từ **token JWT** (server đã ký). Nếu client được tự khai `userId` thì ai cũng giả làm Trưởng ban được.

---

## 2. Gửi khiếu nại — server tự hỏi 5 câu

Hàm chính: `S/AppealService.java:106` (`submitAppeal`). Đọc nó như một người bảo vệ gác cổng:

| Câu server hỏi | Dòng | Không đạt thì |
|---|---|---|
| Bạn có phải Phóng viên đang hoạt động không? | `:107` | chặn |
| Bạn có phải **tác giả chính** của bài không? | `:218-220` | "Chỉ tác giả chính được gửi khiếu nại." |
| Bài đã công bố điểm chưa? | `:221-230` | "Kết quả chấm chưa được công bố." |
| Còn trong hạn không? (ngày công bố + số ngày cho phép) | `:231-234` | "Thời hạn khiếu nại đã kết thúc." |
| Bài này khiếu nại rồi chưa? | `:208-210` | "Mỗi bài viết chỉ được khiếu nại một lần." |
| Tiêu chí chọn có thật sự thuộc phiếu chấm, có lý do không? | `:240-261` (`validateCriteria`) | báo lỗi từng loại |

Qua hết thì mới **lưu** (`:113-126`), rồi lưu các tiêu chí (`:128`), báo Trưởng ban (`:129`), ghi nhật ký (`:132`).

**Vì sao `:121-126` bắt lỗi trùng lần nữa, khi `:208` đã kiểm tra rồi?**
Tưởng tượng Phóng viên bấm nút hai lần cực nhanh. Cả hai request cùng qua được `:208` ("chưa có khiếu nại") vì chưa ai kịp lưu. Lúc này chỉ có **database** cản được, nhờ ràng buộc `uq_appeals_article UNIQUE (article_id)` (`database/schema.sql:564`). Request đến sau bị DB từ chối, và `try/catch` ở `:123` đổi cái lỗi SQL khó hiểu thành câu tiếng Việt. Nói gọn: **`:208` để báo lỗi đẹp, UNIQUE để chặn thật.**

**Vì sao luật (số ngày hạn) lấy từ `:110` và lưu `ruleConfigId` ở `:117`?**
Admin có thể đổi luật sau này. Khiếu nại đã gửi phải bị xét theo **luật lúc nó được gửi**, không phải luật mới. Nên mỗi khiếu nại ghi nhớ nó dùng bản luật nào.

---

## 3. Trưởng ban phân công — "giao cho ai" là chỗ khó nhất

Hàm: `S/AppealManagementService.java:104` (`assign`). Bốn bước:

1. `:105` — kiểm tra người bấm là Trưởng ban **đúng ban của bài** (hàm `scopedAppeal`, `:166-179`, chặn ở `:176-177`).
2. `:106` — chỉ phân công khi khiếu nại đang chờ (`SUBMITTED`). Giao rồi thì không giao lại bằng nút này.
3. `:108` — người được chọn phải **nằm trong danh sách hợp lệ** (`requireCandidate`, `:216-224`). Client chọn bừa một id ngoài danh sách là bị từ chối.
4. `:109` — tạo **phiếu chấm trống** cho BTV đó (`createDraft`).

### Danh sách "ai được chấm" (`candidates`, `:189-214`) — giải thích bằng ý

Mục tiêu: người chấm lại phải **chưa từng dính vào bài này**, nếu không thì không còn "mù".

- `:194-199` lập danh sách **loại trừ**: tất cả người đã có phiếu chấm bài này (S0, S1...) **và** những người từng bị giao rồi giao lại (đọc từ nhật ký).
- `:203-210` duyệt toàn bộ BTV, chỉ giữ người **đang hoạt động, đúng vai EDITOR, không nằm trong danh sách loại trừ**.
- `:206` đánh dấu người thuộc **ban khác** (`cross`).
- `:211` — **ưu tiên cùng ban**; chỉ khi cùng ban không còn ai mới đưa người ban khác ra.
- `:219` — chọn người ban khác (hoặc giao lại) thì **bắt buộc có lý do**.

### Phiếu mới có gì (`S/ScoringAppealService.java:182-203`, `createDraft`)

- `:184-189` lấy phiếu S0 và kiểm tra nó hợp lệ.
- `:196-201` **sao chép** bộ tiêu chí, phiên bản bài, thể loại từ S0 sang phiếu mới → người chấm lại dùng **đúng thước đo cũ**, chỉ khác người.
- Không gán điểm nào. Phiếu mới `total_score` rỗng, `submitted_at` rỗng. **Phân công là giao việc, chưa phải chấm.**

---

## 4. Chấm mù — "mù" nằm ở đâu

Câu hay bị hỏi: *"Sao chắc chắn BTV không thấy điểm cũ?"*

Trả lời bằng code: `S/ScoringAppealService.java:96-110` (`getComparisonCard`). Hàm này chỉ đọc `details.findByScoringAttemptId(id)` ở `:99` — tức **chỉ các dòng điểm của đúng phiếu này**. Response (`:107-109`) không có trường nào chứa S0, tên người chấm cũ, hay tiêu chí bị khiếu nại.

Điểm mấu chốt: **việc giấu thông tin làm ở server, không phải ẩn bằng CSS.** Ẩn bằng CSS thì bấm F12 là thấy.

Nhưng chính chặn quyền xem phiếu cũng phải đúng: `getOwnedScorecard` (`:85-93`) kiểm tra phiếu có **đúng là của người đang đăng nhập** không (`:90-91`), không thì 403.

### Lưu nháp / Nộp phiếu — `saveScorecard` (`:113-147`)

Đây là hàm xử lý cả hai nút. Chạy theo ý:

1. `:115-117` lấy phiếu (có khoá, xem mục 6), xác nhận **phiếu này đúng của bạn**.
2. `:118-124` đối chiếu điểm client gửi với **bộ tiêu chí thật** của phiếu. Tiêu chí lạ hoặc trùng thì từ chối (`:122`, `:136`).
3. `:137` **server tự tính tổng điểm** bằng `AppealScoreCalculator.weightedTotal` (xem dưới).
4. `:140` xoá điểm cũ, ghi điểm mới thay vào.
5. `:142` nếu là nút **Nộp** thì mới đóng dấu `submittedAt`. Nút **Lưu nháp** thì chưa → còn sửa được.

**Vì sao client đã tính tổng (`C/pages/appeal/blindScorecard.js:10`, `tinhTongTam`) mà server còn tính lại?**
Hàm ở client chỉ để **người chấm xem trước** (`BlindReviewPage.jsx:103` hiện "Tổng điểm tạm tính"). Client là máy của người dùng, họ sửa được. Con số **được lưu** luôn là con số server tính ở `:137`.

### Công thức tổng điểm — `S/AppealScoreCalculator.java:46-68`

Hiểu bằng ví dụ: 2 tiêu chí, trọng số 60% và 40%, điểm 80 và 50 → 80×60% + 50×40% = 68.

Các luật gác cổng trong hàm:
- `:65` tổng trọng số phải đúng 100%.
- `:60-62` nộp phiếu mà có tiêu chí dưới 50 điểm thì **bắt buộc có nhận xét**.
- `:66` nộp phải chấm đủ mọi tiêu chí (nháp thì thiếu cũng được, trả về `null`, `:67`).

---

## 5. So sánh và chốt điểm — công thức quyết định

Hàm cốt lõi, ngắn nhất mà quan trọng nhất: `S/AppealScoreCalculator.java:32-44` (`compare`). Đọc theo ý, đừng đọc theo cú pháp:

> Gọi **T** là "ngưỡng chênh lệch" (Admin cấu hình, mỗi khiếu nại giữ bản T lúc gửi).

**Trường hợp 1 — S1 gần S0** (`:34-37`): `|S1 − S0| ≤ T` → lấy **S1**, xong. Nếu lúc này đã có S2 thì lỗi (`:35`), vì không cần S2.

**Trường hợp 2 — lệch nhiều, chưa có S2** (`:38`): trả "cần chấm S2", **chưa có điểm** (`requiresSecondReview = true`). Trưởng ban phải phân công S2 (`AppealManagementService.java:120-129`).

**Trường hợp 3 — đã có S2** (`:39-43`): S2 là người phân xử. Xem S2 **gần ai hơn**:
- S2 gần S0 hơn (`:41`) → lấy trung bình (S0 + S2) / 2.
- S2 gần S1 hơn (`:42`) → lấy trung bình (S1 + S2) / 2.
- S2 cách đều hai bên (`:43`) → lấy S2.

Ví dụ để tự kiểm tra: S0 = 50, S1 = 90, S2 = 40, T = 10.
|90−50| = 40 > T nên cần S2. S2 cách S0 là 10, cách S1 là 50 → S2 gần S0 hơn → (50+40)/2 = **45**.
Để ý: điểm cuối có thể **thấp hơn** S0 (45 < 50). Đó là chủ ý của luật (BR-QC-07), khiếu nại không đảm bảo tăng điểm.

### Bấm "Xác nhận điểm hiệu lực" — `S/AppealReviewService.java:103-130` (`finalizeAppeal`)

- `:104` đúng Trưởng ban đúng ban, **có khoá** (mục 6).
- `:105` khiếu nại phải đang chấm lại, và **chưa có ai chốt** (`resolutions.findByAppealId`).
- `:107` **server tự tính lại** điểm đề xuất bằng chính `compare`, không tin client.
- `:109` client chỉ gửi `expectedAttemptId` ("tôi đang nhìn phiếu số mấy") để chắc là Trưởng ban không chốt trên dữ liệu đã cũ. **Client không gửi điểm.** Nếu client được gửi điểm thì Trưởng ban gõ điểm nào cũng được.
- `:112-113` còn thiếu S2 thì không cho chốt.
- `:114-119` ghi kết quả (điểm, luật áp dụng) và chuyển khiếu nại sang `RESOLVED`.
- `:122` nhật ký ghi `downstreamStatus = PENDING_INTEGRATION`: nghĩa là **tiền nhuận bút (BF-04) và KPI (BF-05) chưa tự cập nhật theo điểm mới**. Khi trình bày đừng nói là đã tự đồng bộ.

---

## 6. Khoá — phần khó nhất, nhưng chỉ cần nhớ một ý

**Tình huống:** Trưởng ban bấm "Giao lại" đúng lúc BTV bấm "Nộp phiếu". Nếu cả hai cùng sửa một phiếu một lúc thì dữ liệu hỏng: phiếu đã nộp mà người chấm lại bị đổi.

**Cách giải:** ai muốn sửa phiếu thì phải **xin khoá trước**; người đến sau đứng đợi người trước xong.

Ba chỗ cần biết:

- `S/ScoringAppealService.java:233-243` (`lockedDraft`): lấy phiếu có khoá. Dòng `:237` (`entityManager.refresh`) nạp lại dữ liệu **sau khi có khoá** — vì nếu không, có thể đang cầm bản cũ đọc từ trước khi chờ khoá.
- `:240-241` phiếu đã nộp thì không đổi người được.
- `S/ReviewerDeviationRefreshService.java:47` (`lockWorkflow`): khoá toàn bộ thao tác ghi của BF-03. Hơi "thô" (ghi chú ở dòng 25 đã nói thẳng), đổi lại **đơn giản và không sót**. Đây là cách làm đủ cho đồ án, không phải cách tối ưu cho hệ thống lớn.

**Vì sao `AppealReviewService.java:84-85` và `:104` phải khoá khiếu nại *trước* phiếu?** Mọi luồng khoá theo cùng một thứ tự (khiếu nại rồi mới tới phiếu) thì không bao giờ kẹt nhau (deadlock). Chỉ cần nhớ: *"khoá theo thứ tự cố định"*.

### Giao lại phiếu (`ScoringAppealService.java:206-222`)

Dòng `:209-211` là điểm hay: request mang theo `expectedReviewerId` ("tôi tưởng phiếu đang ở tay ông A"). Nếu thực tế đã sang tay người khác thì báo "Phân công đã thay đổi, tải lại". Gọi là **kiểm tra phiên bản dữ liệu**: tránh việc hai Trưởng ban cùng giao và ghi đè nhau mà không biết.
`:218` xoá điểm nháp, `:219` lưu bản nháp cũ vào nhật ký trước khi xoá.

---

## 7. Bài tự kiểm tra (không cần nhìn tài liệu)

Nếu trả lời được 5 câu này là hiểu luồng:

1. Vì sao S2 chỉ xuất hiện khi `|S1 − S0| > T`? (`AppealScoreCalculator.java:34,38`)
2. Chấm mù được bảo đảm ở đâu — server hay giao diện? (`ScoringAppealService.java:96-110`)
3. Vì sao điểm tổng tính ở cả client lẫn server, và cái nào được lưu? (`:137` và `blindScorecard.js:10`)
4. Vì sao `finalizeAppeal` không nhận điểm từ client? (`AppealReviewService.java:107-109`)
5. Hai Trưởng ban cùng bấm "Giao lại" thì chuyện gì xảy ra? (`ScoringAppealService.java:209-211` và `:237`)

---

**Chưa kiểm tra:** em chỉ đối chiếu số dòng với source, chưa chạy lại từng màn trong trình duyệt. Phần chấm chéo (UC-40) và hồ sơ BTV (UC-42) không có trong bản này; xem bài 3.3 và 2.3 của tài liệu cũ.
