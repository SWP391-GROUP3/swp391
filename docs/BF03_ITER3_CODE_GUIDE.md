# BF03 Iter3 — Hướng dẫn luồng màn hình và source code

Muốn lần theo **từng nút → handler → API → controller → service → repository** cho cả Iter1–3, đọc [BF03_FLOW_AND_METHOD_GUIDE.md](BF03_FLOW_AND_METHOD_GUIDE.md).

## 1. Phạm vi và cách chạy

Iter3 hoàn thiện phần BF03 của UC36–40, tiếp nối Iter1–2 trên nhánh `VuNQ`. Backend Spring Boot/Java17, frontend React/Vite, dữ liệu từ SQL Server qua REST API. Frontend không đọc `data.sql`.

**Có thay đổi DB:** với database hiện có, chỉ chạy `database/migrations/BF03_iter3_safe.sql`, không chạy lại `schema.sql` hoặc `data.sql`. Migration đã được thử rollback rồi áp dụng trên database chung ngày26/10/2026, số dòng trước/sau giữ nguyên. File có transaction, kiểm tra DB/bảng/cột/CHECK và có thể chạy lại.

Chạy backend bằng `mvn -f server/pom.xml spring-boot:run`, frontend bằng `npm run dev` trong `client/`, rồi mở `http://localhost:5173`. Máy cần cấu hình local đã có; không commit mật khẩu hoặc `application-local.properties`.

**Lưu ý baseline sau merge main:** test BF04 `RoyaltyGradingServiceTest` đang có5 lỗi biên dịch vì gọi chữ ký cũ. Khi lỗi này chặn `spring-boot:run` ở testCompile, có thể chạy tạm `mvn -f server/pom.xml -Dmaven.test.skip=true spring-boot:run`. Đây chỉ là bỏ bước test khi mở server, không có nghĩa test BF04 đã được sửa.

## 2. Đi từng màn như thế nào?

### Head — quản lý khiếu nại, giao S1/S2 và chốt

1. Đăng nhập Trưởng ban, chọn **Khiếu nại** (`/appeals`). Bộ lọc và phân trang là màn Iter2 đã có.
2. Mở `/appeals/:id`: xem lý do, chọn BTV và **Phân công**. Phiếu đầu là S1. Giao khác ban chỉ được dùng khi không còn ứng viên cùng ban, bắt buộc lý do.
3. Editor nộp xong, mở **So sánh & chốt điểm** (`/appeals/:id/comparison`). Bảng gồm từng tiêu chí, trọng số, điểm/nhận xét S0, S1 và S2 nếu có.
4. Nếu `ABS(S1-S0) <= T`, hệ thống đề xuất S1; Head bấm **Xác nhận điểm hiệu lực**.
5. Nếu vượt T, chọn người chấm S2 tại màn so sánh. Người chấm gốc, người đã chấm S1 và người đã bị giao lại đều bị loại. Không có ứng viên thì giữ trạng thái chờ, không tạo kết quả giả.
6. Sau khi S2 nộp, Head kiểm tra đề xuất rồi xác nhận. Appeal thành RESOLVED, kết quả chỉ đọc. Không thể tạo S3 hay chốt lại.
7. Nút **Xem nhật ký** mở audit đúng appeal. Giao lại phiếu S1/S2 chưa nộp vẫn thực hiện tại màn chi tiết, giữ nguyên số vòng.

### Editor — chấm mù

1. Chọn **Chấm lại & chấm chéo** (`/re-reviews`). Danh sách chỉ có phiếu được giao cho tài khoản hiện tại.
2. Mở `/re-reviews/:id`: bên trái là nội dung đúng phiên bản đã chấm gốc, bên phải là bộ tiêu chí/trọng số đã gắn.
3. Nhập điểm0–100. Mọi tiêu chí dưới50 phải có nhận xét khi nộp.
4. **Lưu nháp** cho phép chưa đủ tiêu chí; quay lại vẫn đọc được nháp của mình. **Nộp phiếu chấm** yêu cầu đủ dữ liệu và khóa phiếu.
5. Sau nộp chỉ xem điểm/nhận xét của chính phiếu mình. Người đã bị giao lại mất quyền đọc và ghi phiếu.

Chấm mù được thực hiện ở **response backend**: không trả điểm/nhận xét cũ, reviewer cũ, tiêu chí bị khiếu nại, lý do hay kết quả so sánh. Không chỉ ẩn bằng CSS.

### Head và Editor — đợt chấm chéo UC40

1. Head mở **Đợt chấm chéo** (`/cross-reviews`), chọn khoảng ngày và bấm **Tìm bài đã chấm**.
2. Nhập tên đợt duy nhất, chọn người chấm cho các bài muốn lấy mẫu. Mỗi bài chỉ được chọn một lần trong một đợt. Để người chấm trống để bỏ qua bài.
3. **Tạo đợt chấm chéo** tạo batch OPEN và task PENDING trong cùng transaction; Editor nhận thông báo.
4. Editor mở task từ `/re-reviews`, vào `/cross-review-tasks/:id`, chấm theo snapshot và nộp. Task chấm chéo không có lưu nháp; tổng điểm và độ lệch lưu ở bảng phân tích, chi tiết đã nộp được giữ trong audit.
5. Head xem `/cross-reviews/:id`: điểm phân tích, độ lệch có dấu và tiến độ. Có thể gỡ task PENDING; việc gỡ được audit và thông báo.
6. Chỉ **Đóng đợt** khi không còn task PENDING. Đợt CLOSED và task DONE không được sửa/nộp lại. Nhật ký đợt mở đúng bộ lọc batch.

**Chấm chéo không thay điểm bài hoặc kết quả appeal.** Ví dụ bài gốc70, chấm chéo80: lưu độ lệch+10, điểm chính thức vẫn lấy theo kết quả công bố/appeal.

### Reporter và đồng tác giả

- Reporter xem `/my-appeals`: sau RESOLVED có điểm hiệu lực và thời điểm chốt, không chỉ có nhãn trạng thái.
- API `GET /api/articles/{articleId}/effective-score` cho tác giả chính/đồng tác giả xem điểm số hiệu lực; Head chỉ xem bài trong ban, Admin được xem. Editor chấm mù bị từ chối API này.
- UI BF01 cho đồng tác giả chưa được sửa trong phạm vi này; chủ BF01 có thể gọi endpoint trên khi nối phần xem kết quả bài.

## 3. Công thức cần giải thích khi trình bày

`S0` là tổng gốc; `S1`, `S2` là hai đợt chấm lại; `T` đọc từ cấu hình đã gắn với appeal, không lấy cấu hình mới nhất mỗi lần chấm.

| Điều kiện | Điểm đề xuất | Mã luật mới |
|---|---|---|
| ABS(S1-S0)<=T | S1 | USE_S1 |
| VượtT, chưa có S2 | Chờ S2, không chốt | chưa có |
| ABS(S2-S0)<ABS(S2-S1) | (S0+S2)/2 | AVERAGE_S0_S2 |
| ABS(S2-S1)<ABS(S2-S0) | (S1+S2)/2 | AVERAGE_S1_S2 |
| Hai khoảng cách bằng nhau | S2 | USE_S2_TIE |

Tổng điểm = `SUM(score * weight /100)`. Tính bằng `BigDecimal`, chỉ làm tròn tổng/trung bình ở cuối bằng HALF_UP, hai chữ số thập phân. Điểm có thể giảm:70/90/40 dẫn đến55. Không lấy trung vị và không giữ sàn S0.

## 4. Source code tác động và ý nghĩa

Các tên backend bên dưới thuộc `server/src/main/java/com/swp391/kpi/`.

| File | Vai trò / hàm cần đọc |
|---|---|
| `service/AppealScoreCalculator.java` | Class thường, không Spring/DB. `weightedTotal()` kiểm tra trọng số, điểm và comment; `compare()` quyết định ngưỡng/trọng tài |
| `entity/AppealResolutionRule.java` | Enum bốn luật mới và hai mã lịch sử |
| `service/ScoringAppealService.java` | Hợp đồng BF02: `getComparisonCard()` đọc snapshot; `getOwnedScorecard()` kiểm tra chủ dưới khóa; `saveScorecard()` tính điểm, lưu detail và khóa; `createSecondDraft()` giữ snapshot gốc |
| `service/ArticleReadService.java`, `repository/ArticleReadRepository.java` | BF01 cung cấp `getVersion(articleId,versionId)` và `isAuthor()`; BF03 không tự query repository bài viết |
| `dto/ArticleVersionReadResponse.java` | Snapshot chỉ gồm ID, title, content; không trả entity/JPA |
| `dto/AppealScorecardRequest.java` | Client chỉ gửi criterionId/score/comment. Không có trường để client tự khai tổng hoặc quyết định xuất bản |
| `dto/AppealBlindScorecardResponse.java` | Dữ liệu an toàn cho Editor; không có reviewer/appeal reasons/điểm cũ |
| `service/AppealReviewService.java` | Điều phối queue/card/save, so sánh, finalize và endpoint điểm hiệu lực; kiểm tra role, scope và lifecycle |
| `controller/AppealReviewController.java` | Nhận JWT/DTO hợp lệ, gọi một hàm service và trả ApiResponse |
| `service/AppealManagementService.java` | Mở rộng Iter2 bằng `assignSecond()`/`requiresSecond()`; giữ quy tắc ứng viên và giao lại |
| `controller/AppealManagementController.java` | Thêm POST assign-second, các API cũ giữ nguyên |
| `dto/AppealComparisonResponse.java`, `AppealFinalizeRequest.java` | DTO Head và expectedAttemptId để từ chối xác nhận trên phiếu đã thay đổi |
| `entity/AppealResolution.java` | Lưu effectiveScore/ruleApplied/finalizedBy/finalizedAt; effectiveAttemptId chỉ tham chiếu S1/S2 |
| `service/AppealEffectiveScoreService.java` | Nguồn điểm số hiệu lực cho các luồng khác. Chưa RESOLVED thì dùng điểm công bố; lịch sử NULL đọc điểm effective_attempt_id qua BF02 |
| `service/AppealService.java`, `dto/AppealResponse.java` | Bổ sung kết quả đã chốt cho Reporter |
| `entity/CrossReviewBatch.java`, `CrossReviewBatchStatus.java` | Ánh xạ batch và OPEN/CLOSED; task/entity có sẵn tiếp tục dùng |
| `repository/CrossReviewBatchRepository.java`, `CrossReviewTaskRepository.java` | Query đợt/task, khóa row và khoảng tên để chống tạo trùng tên |
| `service/CrossReviewService.java` | Toàn bộ create/candidate/submit/remove/close; `blind()` bỏ điểm/nhận xét gốc khỏi response Editor |
| `controller/CrossReviewController.java` | REST API riêng cho Head và Editor, quyền vẫn kiểm ở service |
| `service/ReviewerDeviationRefreshService.java` | Tính lại chỉ số sau finalize/submit cross; `lockWorkflow()` dùng transaction application lock của SQL Server |
| `service/AppealAuditQueryService.java` | Head đọc audit trong ban; vẫn tìm được scope từ snapshot sau khi task bị gỡ |

Frontend:

| File dưới `client/src/` | Vai trò |
|---|---|
| `api/appealApi.js`, `api/crossReviewApi.js` | Chỉ dùng `goiApi`, tự mang JWT và giữ mã409/403 cho UI |
| `pages/appeal/BlindReviewQueuePage.jsx` | Hai nhóm phiếu của Editor: appeal và chấm chéo |
| `pages/appeal/BlindReviewPage.jsx` | Dùng chung form chấm mù cho hai loại task; chấm chéo không có draft |
| `pages/appeal/blindScorecard.js` | Tổng tạm tính theo phần trăm và kiểm tra nhập trước nộp; server vẫn là nơi quyết định điểm |
| `pages/appeal/AppealComparisonPage.jsx` | Bảng S0/S1/S2, giao S2 và xác nhận đề xuất server; không gửi điểm cuối tự chọn |
| `pages/appeal/CrossReviewBatchPage.jsx`, `CrossReviewBatchDetailPage.jsx` | Tạo đợt/chọn mẫu và theo dõi/đóng/gỡ task |
| `pages/appeal/ReviewWorkflow.css` | Style có tiền tố luong-qc, dùng biến --fm-* và mẫu BF03 hiện có |
| `pages/appeal/AppealDetailPage.jsx`, `MyAppealsPage.jsx`, `AuditLogPage.jsx` | Nối màn so sánh, hiện điểm Reporter và nhận scope batch từ URL |
| `routes/AppRoutes.jsx` | Thay ComingSoon của BF03 bằng các route đã triển khai; menu cũ đã có nên không sửa layout chung |

## 5. Transaction, khóa và audit

- Các workflow ghi phiếu/phân công/chấm chéo và tính chỉ số lấy application lock `BF03_SCORE_WRITES` trước, rồi khóa appeal → attempt hoặc batch → task. Transaction kết thúc thì SQL Server tự nhả khóa.
- Khóa ghi toàn BF03 là lựa chọn đơn giản cho quy mô bài tập, tránh hai workflow vừa giữ row riêng vừa quét toàn kỳ để tính chỉ số. Chờ quá15giây trả thông báo thử lại. Khi lượng ghi lớn nên tách quá trình tổng hợp; không thêm hàng đợi/cache vào bài hiện tại.
- GET phiếu chấm mù cũng giữ khóa appeal → attempt đến khi kiểm tra chủ và đọc detail xong, tránh A đang đọc thì Head giao B rồi A nhận điểm riêng của B.
- Sau khi có khóa phải `EntityManager.refresh()` để không tin entity đã cache trước đó trong persistence context.
- Nộp phiếu, đổi lifecycle, audit và notification cùng transaction; lỗi thông báo làm rollback. Finalize cần expectedAttemptId đúng phiếu mới nhất đã nộp.
- Audit chỉ thêm dòng. Khi gỡ task PENDING vẫn giữ snapshot bài/người được giao để Head tra cứu lịch sử; không xóa audit.

## 6. API chính

| API | Người dùng / mục đích |
|---|---|
| GET `/api/appeal-reviews` | Editor: queue của mình |
| GET/PUT `/api/appeal-reviews/{id}` | Đọc phiếu / lưu nháp |
| POST `/api/appeal-reviews/{id}/submit` | Nộp và khóa |
| GET `/api/appeals/{id}/comparison` | Head cùng ban: đối chiếu |
| POST `/api/appeals/{id}/assign-second` | Giao S2 khi thật sự cần |
| POST `/api/appeals/{id}/finalize` | Chốt đề xuất với expectedAttemptId |
| GET `/api/articles/{id}/effective-score` | Điểm số cho người có quyền xem kết quả |
| GET/POST `/api/cross-review-batches` | Head: danh sách/tạo đợt |
| GET `/api/cross-review-batches/candidates` | Lấy mẫu theo fromDate/toDate |
| GET `/api/cross-review-batches/{id}` | Chi tiết đợt |
| POST `/api/cross-review-batches/{id}/close` | Đóng khi hết PENDING |
| DELETE `/api/cross-review-batches/{id}/tasks/{taskId}` | Gỡ task chưa nộp |
| GET `/api/cross-review-tasks` | Editor: task của mình |
| GET `/api/cross-review-tasks/{id}` | Phiếu chấm chéo an toàn |
| POST `/api/cross-review-tasks/{id}/submit` | Lưu tổng phân tích/độ lệch |

## 7. Điểm tích hợp chưa làm thay chủ luồng

BF03 đã cung cấp `AppealEffectiveScoreService.getEffectiveScore(articleId,publishedScore)`. Người gọi lấy điểm công bố từ BF02 rồi dùng giá trị trả về để tính. **Không lấy điểm trung bình từ effective_attempt_id**, vì FK chỉ trỏ một trong hai phiếu.

BF04 hiện vẫn dùng đường đọc điểm công bố cũ; chưa tự tính lại OPEN hoặc tạo ADJUSTMENT cho LOCKED/PAID. BF05 chưa tự cập nhật DRAFT/chặn finalize. Cần chủ hai luồng chốt hợp đồng, đặc biệt kỳ CLOSED. Audit FINALIZE ghi `downstreamStatus=PENDING_INTEGRATION`; không ghi nhãn đã đồng bộ giả và không sửa dữ liệu tiền/KPI.

## 8. Kiểm chứng và mẫu demo

- `AppealScoreCalculatorTest`:11 ca tính toán thuần.
- `BF03Iteration3IT`:14 ca API/DB gồm quyền, draft/nộp, snapshot, S1/S2, finalize trùng, rollback thông báo, rule đã gắn, kết quả Reporter/đồng tác giả, cross-review/ngày/trùng bài/khác ban và audit lịch sử.
- `BF03ConcurrencyIT`:hai ca dùng kết nối/transaction độc lập, fixture commit riêng để các thread nhìn thấy rồi cleanup theo đúng ID do test tạo. Test không sửa appeal thật của người dùng.
- Ba finding từ reviewer đã có RED trước sửa: owned-read race, đồng thời refresh ở hai batch, và điểm xác nhận NULL lịch sử. Sau sửa, nhóm18 test liên quan đạt0fail/error.
- Suite kiểm chứng cuối:158test đạt, chỉ loại file test BF04 không biên dịch đã nêu ở mục1. Test báo cáo Iter1 được đổi sang fixture BTV riêng để không phụ thuộc dữ liệu thật đang có trong tháng. Backend chuẩn compile258source thành công. Frontend17test/build đạt, lint8warning ngoài BF03.
- Frontend có test API/JWT/409 và tổng tạm tính; browser `client/tests/browser/bf03Iteration3.smoke.js` kiểm tra ba ca bằng API giả lập, không coi đó là ghi DB thật.
- Luồng trình duyệt với JWT và API thật đã đi qua S1=90, S2=40, chốt55; tạo/nộp/đóng chấm chéo80, lệch+10, Reporter đọc kết quả. Mẫu riêng: article215, appeal210, S1 phiếu323, S2 phiếu347, batch39, cross task32. Các ID chỉ là dữ liệu của máy hiện tại.
- Mẫu có nhãn BF03-ITER3-DEMO/BF03-ITER3-LIVE; giữ lại để xem kết quả. Ba BTV mẫu có username `bf03.i3.s1.5877633`, `bf03.i3.s2.5877633`, `bf03.i3.cross.5877633`, dùng mật khẩu mẫu chung như màn Login. Không đổi tài khoản/mật khẩu cũ.
- Một số tên tài khoản/ban/role của dữ liệu cũ bị lỗi chữ từ trước; không tự sửa dữ liệu dùng chung ngoài phạm vi. Nội dung/nhận xét nghiệp vụ mới lưu UTF-8.
- Kết quả chạy cuối và các giới hạn build/test được cập nhật tại `docs/superpowers/checkpoints/2026-10-26-bf03-iter3-checkpoint.md`.
