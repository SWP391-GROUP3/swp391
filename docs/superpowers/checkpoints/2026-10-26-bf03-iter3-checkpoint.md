# BF03 Iter3 — checkpoint

Plan: `docs/superpowers/plans/2026-10-26-bf03-iter3-plan.md`.
Base: `40386e9`, branch `VuNQ`. Đề xuất đã được người dùng duyệt, gồm migration, tích hợp BF02/snapshot và route chung tối thiểu.

## Tiến độ

- [x] Khảo sát và duyệt thiết kế.
- [x] Kế hoạch chi tiết; giữ workspace hiện có theo lựa chọn VuNQ.
- [x] Task1 schema/calculator:11 unit test; migration thử rollback rồi apply, số dòng trước/sau giữ nguyên; CHECK trusted.
- [x] Task2 snapshot/scorecard: contract và nộp chấm mù qua integration rollback.
- [x] Task3 workflow36–39: S1/S2, finalize, canonical score, chỉ số và kết quả Reporter.
- [x] Task4 cross-review40: create/submit/remove/close, audit giữ liên kết sau gỡ task.
- [x] Task5 frontend:17 node test, build140 modules; browser giả lập3 ca và luồng JWT/backend thật đã đạt.
- [x] Task6 kiểm chứng/hướng dẫn/review trong phạm vi BF03. Tích hợp ghi BF04/BF05 vẫn theo phụ thuộc đã duyệt, không đánh dấu hoàn tất phần đó.

## Ràng buộc và phụ thuộc

- Không commit/push, không reset DB, không đổi mật khẩu/cấu hình/dependency.
- BF04/BF05: chỉ cung cấp điểm hiệu lực; chưa được phép sửa nghiệp vụ tài chính/KPI. Không tuyên bố đã đồng bộ tự động nếu chưa có hợp đồng.
- Usage lúc13:50 ngày26/10:50% đã dùng cửa sổ5h,8% đã dùng tuần, chưa đến ngưỡng dừng còn10%.

## Bằng chứng và giới hạn hiện tại

- Unit calculator:11/11. Integration Iter3 mới ban đầu5 ca, thêm chấm chéo/lịch sử thành8 ca; vòng tiếp theo bổ sung quyền/rollback và đang chạy suite.
- Frontend node test mới4 ca RED→GREEN; build140 modules; lint8 warning ngoài BF03.
- Browser `bf03Iteration3.smoke.js`:3 ca GREEN, một POST bị intercept, không ghi DB. Audit URL thiếu scope đã quan sát RED rồi sửa.
- Maven test chuẩn bị chặn ở testCompile bởi5 lỗi có sẵn trong `RoyaltyGradingServiceTest` sau main:4 lần gọi previewGrading bằng chữ ký cũ và setNature đã bị bỏ. Không sửa file BF04.
- Kiểm chứng riêng dùng `server/target/bf03-verification-pom.xml` (ignored), giữ dependencies/source/resources hiện có, chỉ loại file test BF04 không compile. Không thay server/pom.xml hoặc che lỗi suite chuẩn.

## Kết quả cuối — 26/10/2026

- Backend chuẩn `mvn -f server/pom.xml compile`: BUILD SUCCESS,258 source Java. `ddl-auto=validate` giữ nguyên; server thật khởi động thành công với schema hiện tại.
- Suite kiểm chứng cuối qua POM ignored: `-Dtest=*Test,BF03Iteration1IT,BF03ReviewerProfileIT,BF03AppealManagementIT,BF03Iteration3IT,BF03ConcurrencyIT`: **158 test,0fail/error/skip**, log `server/target/bf03-iter3-final-suite.log`. Iter3 có14ca API/DB và2ca concurrency; thêm kiểm tra ngày/trùng bài/Head khác ban trước lần chạy cuối.
- Đã chạy lại `mvn -f server/pom.xml test`: vẫn bị5 lỗi testCompile có sẵn trong `RoyaltyGradingServiceTest` tại145,174,190,205,375. Không sửa BF04 và không tuyên bố suite chuẩn đã xanh.
- Trong lần chạy sau khi thêm mẫu thật, test báo cáo Iter1 RED vì dùng reviewer3/dữ liệu tháng chung. Sửa fixture tạo BTV riêng và lọc đúng BTV, không thay expected6.25 hay công thức. Suite157 sau đó GREEN.
- Frontend:17/17 test, `npm run build`140 modules thành công; `npm run lint`exit0,8 warning ngoài BF03 như baseline. `git diff --check` sạch.
- Browser giả lập: `client/tests/browser/bf03Iteration3.smoke.js`3ca,1POST bị intercept,0ghi DB thật. Chứng minh audit URL nhận entityType/entityId, comment bắt buộc dưới50 và form khóa sau nộp.
- Browser thật: đăng nhập Head/3Editor mẫu/Reporter qua form, JWT thật; lưu nháp/nộp S1=90, giao/nộp S2=40, Head chốt55; tạo/nộp/đóng batch chấm chéo80 với deviation+10; Reporter đọc kết quả. Có các lần QA tiếp tục từ trạng thái đã ghi, không tạo lại/phủ lên phiếu đã nộp. Lần cuối của script tiếp tục4thao tác ghi còn lại; không coi đó là toàn bộ số thao tác cả luồng.
- Sau bản sửa review và restart backend, lượt browser chỉ đọc đạt: Head confirmed55, Editor đọc chính phiếu90, Editor xem endpoint điểm hiệu lực bị403, Reporter nhận55. Log `server/target/bf03-iter3-final-readonly.log`.
- Dữ liệu mẫu riêng được giữ: article215,appeal210,S1=323,S2=347,batch39,crossTask32; ba username trong hướng dẫn. Không đổi mật khẩu/tài khoản cũ. Đã sửa encoding của chính mẫu mới do Java source launcher trên Windows cần `-Dfile.encoding=UTF-8`; không sửa dữ liệu tên user/ban bị lỗi chữ từ trước.
- Sau cleanup toàn bộ fixture concurrency: users12(9cũ+3mẫu),articles7(6cũ+1mẫu),versions7,attempts7,details28,appeals2,resolutions1. Royalty gradings1/lines3 và KPI snapshots2 giữ nguyên; các CHECK không disabled/untrusted.
- Hướng dẫn: `docs/BF03_ITER3_CODE_GUIDE.md`. SRS BF03/Overview đã đồng bộ enum, CHECK và GBR07; `data.sql`đã là5từ main nên không sửa.
- Tại lúc nghiệm thu trước yêu cầu commit/push: VuNQ tạiHEAD40386e9 với thay đổi local. Artifacts/log/helper/POM riêng nằm trong ignored `server/target`; `.playwright-cli` chỉ ignore trong `.git/info/exclude`, không đổi .gitignore chung.
- Đã dừng backend/Vite và đóng các phiên browser do QA mở; không giữ cổng8080/5173. Người dùng khởi động theo hướng dẫn để xem web.
- Kiểm tra cuối:65file mới/sửa đều giải mã UTF-8 hợp lệ; `git diff --check` sạch. Usage59%đã dùng cửa sổ5h,9%đã dùng tuần, chưa chạm ngưỡng còn10%.

## Review độc lập và quyết định

Reviewer `review_bf03_iter3` đọc diff/new product files, không ghi file hay chạy DB. Ba finding Important đều được sửa một lượt với RED→GREEN:

1. Owned GET đọc điểm sau khi chủ đã đổi: `BF03ConcurrencyIT.ownedReadAndReassignmentAreAtomicAcrossConnections` dùng hai kết nối, giữ reader giữa owner-check và detail. Sau sửa, writer phải chờ reader hoàn thành; điểm người mới không lọt sang người cũ.
2. Hai batch cùng refresh có thể deadlock/ghi chỉ số thiếu: `twoCrossSubmissionsSerializeBeforeWorkflowLocksAndKeepCompleteAggregate` tái hiện refresh thứhai chạy xen khi workflow đầu chưa xong. Sau sửa, tuần tự trước row locks, cảhai nộp thành công, chỉ số cuối15.00 cho BTV fixture riêng.
3. Head thấy NULL ở resolution lịch sử: regression comparison confirmedScore70 từ effective_attempt_id; dùng cùng fallback của provider.

Ruling: dùng application lock toàn BF03 trong transaction trước các workflow ghi điểm/phân công/chấm chéo và refresh. Đơn giản, không cần queue/cache/thư viện; chi phí là tuần tự hóa ghi và có thể trả409 sau15giây chờ, phải tách tổng hợp nếu tải cao.

Ruling: không mở rộng BF04/BF05 để che phụ thuộc. Nếu chủ luồng chưa gọi provider, màn tính tiền/KPI cũ vẫn đọc điểm cũ; vì vậy ghi PENDING_INTEGRATION rõ trong audit và hướng dẫn. Chi phí: chưa có đồng bộ tự động/điều chỉnh tài chính từ đầu đến cuối.

Ruling: không sửa test BF04 của người khác. Chi phí: Maven test chuẩn vẫn chưa chạy trọn suite dù compile/app và suite kiểm chứng riêng đạt.

Reviewer không chứng nhận DB thật, test/build, migration, browser, ảnh thiết kế, endpoint ngoài phạm vi. Coordinator đã kiểm chứng DB/test/browser nêu trên; giao diện theo mẫu BF03 được duyệt vì không có ui-images; không tuyên bố đã audit bảo mật toàn app. Không có Minor bị bỏ lại từ lượt review này.

## Yêu cầu commit/push và hướng dẫn tổng hợp

Người dùng sau đó yêu cầu commit/push lên VuNQ và thêm hướng dẫn toàn BF3. Đã tạo `docs/BF03_FLOW_AND_METHOD_GUIDE.md`, chỉ rõ từng nút/URL/handler/API/controller/service/repository, các method cần hiểu, breakpoint và giới hạn tích hợp.

Trước commit đã fetch: local VuNQ có11commit main chưa push, remote VuNQ không có commit mới cần lấy. Kiểm chứng lại: backend compile thành công; suite riêng158/158; frontend17/17, build140modules, lint8warning cũ ngoài BF03. Log lần này: `server/target/bf03-prepush-compile.log`, `server/target/bf03-prepush-suite.log`. Không chạy SQL seed/reset hoặc thay database trong thao tác commit/push; integration test vẫn dùng fixture rollback/cleanup riêng.

Commit/push lần này bao gồm toàn bộ phần Iter3 đã nghiệm thu cùng các tài liệu, không có file local/secret/build. SHA thực tế xem Git log và kết quả push trong chat; không ghi SHA dự đoán vào source.
