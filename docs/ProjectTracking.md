# Project Tracking: theo dõi công việc theo Iter

> Chuyển từ `ProjectTracking (3).xlsx` ngày 13/10/2026 (sheet **Functions** và **Use Case**). **File Excel vẫn là bản gốc**: sửa Excel xong thì chuyển lại file này.
> - Cột **Dòng** là số dòng trong Excel, dùng để đối chiếu khi cập nhật.
> - Trạng thái: 68 việc **To Do**.
> - Cột **SRS** / **SDS** (mục tài liệu mô tả màn hình) trong Excel còn trống ở mọi dòng, nên không đưa vào bảng.

## Tổng quan

| Phần | Người phụ trách | Iter1 | Iter2 | Iter3 | Tổng |
|---|---|---|---|---|---|
| Chung | Quang | 5 | 0 | 0 | 5 |
| BF1 | Minh | 3 | 4 | 4 | 11 |
| BF2 | Dương | 8 | 7 | 6 | 21 |
| BF3 | Vũ | 4 | 2 | 4 | 10 |
| BF4 | Quang | 3 | 4 | 4 | 11 |
| BF5 | Khang | 2 | 4 | 4 | 10 |
| **Tổng** | | **25** | **21** | **22** | **68** |

`Chung` = các màn hình không thuộc luồng nào (trang chủ, đăng nhập, hồ sơ...).

## Phần của Quang (BF-04 + chức năng chung): nên làm gì trước

Thứ tự bên dưới xếp theo **phụ thuộc**: việc nào bước sau hoặc người khác cần dùng thì làm trước. Mã UC lấy theo SRS (`docs/SRS_BF04_Royalty.md`, `docs/SRS_Overview.md`).

### Iter1

1. **`RoyaltyCalculator`**: không có trong Excel, **đang làm**. Chấm bài (Iter2), khoá kỳ (Iter3) và màn *Royalty Estimate* của Minh (BF1, Iter3) đều dùng nó.
2. **Unit Price List** (UC-55, bảng `royalty_rates`): chưa có đơn giá thì không chấm xong được bài nào (UC-47 E1).
3. **Media Factor Range List** (UC-56, bảng `media_factors`): Iter2 chấm ảnh/video cần nó.
4. **Royalty Period List** (UC-49, bảng `royalty_periods`).
5. **User Login** (UC-69), rồi **Home Page** theo role. Không chặn ai: trong lúc chưa có đăng nhập, cả nhóm dùng tạm header `X-User-Id` (AGENTS.md mục 3.8).
6. **User Profile, Change Password**: SRS không có UC nào, nên làm sau cùng.
   - **User Register**: SRS **không có** chức năng đăng ký. SRS chỉ có Log In / Log Out (UC-69, 70). Dòng "User Register" trong SRS là dòng mẫu còn sót của template (dành cho Candidate). Nên bỏ dòng này khỏi Excel và thêm **Log Out (UC-70)**, hiện đang thiếu.

### Iter2

Cần bài **PUBLISHED đã có điểm duyệt**, nhưng màn *Publish Approval* của Dương (BF2) tận Iter3 mới xong. Vì vậy Iter2 test bằng dữ liệu mẫu trong `database/data.sql`.

1. **Pending Grading List** (UC-46)
2. **Article Royalty Grading** (UC-47): gọi `RoyaltyCalculator`.
3. **Media Royalty Grading** (UC-48)
4. **Royalty Period Detail**, chốt kỳ (UC-50)

### Iter3

1. **Period Statement** + trả kỳ về (UC-51, UC-52)
2. **Lock Period** (UC-53)
3. **Royalty Payment** (UC-54)
4. **My Royalties** (UC-57)

Phối hợp: *Royalty Estimate* của Minh (BF1, Iter3) gọi service nhuận bút của BF-04, không tự viết lại công thức (AGENTS.md mục 2).

### Chỗ Excel ghi lệch SRS / schema (BF-04): nên sửa lại trong Excel

| Dòng Excel | Excel đang ghi | Đúng theo SRS / schema |
|---|---|---|
| Functions 24, Use Case 52 | Thư ký *set/adjust* hệ số, kiểm theo khung hệ số thể loại | Hệ thống tự tính hệ số = hệ số chuẩn thể loại × điểm / 100. Thư ký chỉ xác nhận tính chất bài (BR-RY-01 mới) |
| Functions 21, Use Case 61 | Admin đặt hệ số **min/max** cho ảnh/video | Mỗi loại (`media_kind`) có **1 hệ số cố định** `media_factors.factor` (BR-RY-04, UC-56) |
| Functions 28, Use Case 58 | Lúc *Lock* hệ thống mới tính tiền và chia theo % | Tiền và các dòng chia cho từng tác giả có ngay **khi chấm xong** (UC-47). *Lock* chỉ khoá số liệu và tạo phiếu chi UNPAID (UC-53) |
| Functions 23, Use Case 51 | Bài đã đăng **của một kỳ** | Bài đã đăng **chưa chấm**, có cảnh báo hạn chi (UC-46). Bài chỉ vào kỳ khi chấm xong (BR-RY-05) |

## Iter1: 25 việc

### Chung · Quang (5)

| Dòng | Màn hình / chức năng | Loại | Use case | Bảng | Trạng thái | Ghi chú |
|---|---|---|---|---|---|---|
| 2 | Home Page | Screen |  | C, R | To Do |  |
| 3 | User Login | Screen |  |  | To Do |  |
| 4 | User Register | Screen |  |  | To Do |  |
| 5 | User Profile | Screen | .. |  | To Do |  |
| 6 | Change Password | Screen | .. |  | To Do |  |

### BF1 · Minh (3)

| Dòng | Màn hình / chức năng | Loại | Use case | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 8 | Intake Parameters | Screen | Configure Intake Parameters | To Do | Admin: set max authors per work, co-author confirmation deadline, content-duplication thresholds (20% / 50%) and image-hash threshold |
| 9 | My Works | Screen | View My Works | To Do | Reporter: list works as main author or co-author, filter by status, tab for works waiting for my confirmation |
| 10 | Work Details | Screen | View Work Details; Withdraw Work | To Do | Reporter: view content, locked declarations, co-author confirmation status and status history; withdraw before assignment |

### BF2 · Dương (8)

| Dòng | Màn hình / chức năng | Loại | Use case | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 54 | Criteria Sets by Category | Screen | View Criteria Sets | To Do | The Admin views the criteria sets of every category, with version number, status (DRAFT / ACTIVE / RETIRED) and the weight of each criterion. |
| 55 | Criteria Sets by Category | Screen | Create Criteria Set Version | To Do | The Admin creates a new version of the criteria set of a category (criterion names and percentage weights). A version that has been used is never edited; a change always creates a new version. |
| 56 | Criteria Sets by Category | Screen | Validate Total Criteria Weight | To Do | The system checks that the weights of all criteria in a version add up to 100% (each weight > 0) before the version can be saved. |
| 57 | Criteria Sets by Category | Screen | Activate Criteria Set Version | To Do | The Admin sets a saved version as the active version of its category. The version used for a scoring is the one active at the moment the Editor opens the scoring sheet. |
| 58 | Assignment Queue | Screen | View Assignment Queue | To Do | The Head of Department views the articles in SUBMITTED status that are waiting for an Editor, with category, author and submission date. |
| 59 | Assign Editor | Screen | View Suggested Editors | To Do | The system suggests Editors for an article: those currently scoring the fewest articles first, with priority to Editors who have already scored the same category. |
| 60 | Assign Editor | Screen | Assign Editor to Article | To Do | The Head of Department selects an Editor for the article. The system saves the assignment, changes the status to ASSIGNED and notifies the Editor. |
| 61 | Editor Work Queue | Screen | View My Assigned Articles | To Do | The Editor views the articles assigned to them, including articles returned to them for re-scoring, with status and assignment date. |

### BF3 · Vũ (4)

| Dòng | Màn hình / chức năng | Loại | Use case | Bảng | Trạng thái | Ghi chú |
|---|---|---|---|---|---|---|
| 43 | Submit & Track Appeal Screen | Screen | Submit Quality Appeal | kpi_periods, kpi_targets, users | To Do | Reporter: view published article score breakdown, select appealed criteria, submit quality appeal within 7 days with detailed justification reason |
| 44 | Configure Appeal Rules & Scoring Scale Screen | Screen | Configure Appeal Rules & Scoring Parameters | kpi_periods, kpi_progress, kpi_snapshots | To Do | Admin: configure system appeal parameters (7-day window, 1 attempt limit, > 5-point threshold for 3rd review, score scale) |
| 45 | System Audit Log / Grading Audit Log Screen | Screen | Audit System & Grading Logs | kpi_periods, kpi_progress, kpi_snapshots | To Do | Admin: maintain complete immutable audit trail of evaluation attempts, score updates, and workflow state transitions while keeping old scores hidden from evaluators |
| 46 | Reviewer Score Deviation Report Screen | Screen | Analyze Reviewer Score Deviations | articles, reader_interactions, categories | To Do | Head of Editorial Dept: analyze reviewer grading strictness/leniency metrics, detect high variance flags between original and re-evaluation scores |

### BF4 · Quang (3)

| Dòng | Màn hình / chức năng | Loại | Use case | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 20 | Unit Price List | Screen | Manage Unit Prices | To Do | Accountant: view price history, add new unit price with effective date (master data for royalty calculation) |
| 21 | Media Factor Range List | Screen | Manage Media Factor Ranges | To Do | Admin: configure min/max factor for image/video (master data, needed before grading media) |
| 22 | Royalty Period List | Screen | View Royalty Periods | To Do | Editorial Secretary, Accountant: list monthly periods with status, graded/ungraded count, totals |

### BF5 · Khang (2)

| Dòng | Màn hình / chức năng | Loại | Use case | Bảng | Trạng thái | Ghi chú |
|---|---|---|---|---|---|---|
| 32 | KPI Benchmarks Config Screen | Screen | Configure KPI Benchmarks (UC-58) | kpi_periods, kpi_targets, users | To Do | Admin / Head: Configure baseline benchmark rules (min_articles, min_avg_score) by seniority level. |
| 33 | Monthly Target Setting Screen | Screen | Set Monthly Targets | kpi_periods, kpi_progress, departments | To Do | Head of Dept: Set monthly quotas per reporter or apply bulk defaults by seniority level. |

## Iter2: 21 việc

### BF1 · Minh (4)

| Dòng | Màn hình / chức năng | Loại | Use case | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 11 | Work Editor | Screen | Create and Edit Work; Declare Co-authors and Shares; Submit Work | To Do | Reporter: edit content, declare category, nature and media author/source, co-authors (max 5, total 100%), save draft, submit |
| 12 | Co-authorship Confirmation | Screen | Confirm Co-authorship | To Do | Reporter (co-author): view work and own share, accept or reject |
| 13 | Revise and Resubmit | Screen | Revise and Resubmit Work | To Do | Reporter: edit returned work with editor's comments; resubmit directly if content-only, reconfirm if authors or shares changed |
| 14 | Co-author Confirmation Timeout | Function | Confirm Co-authorship | To Do | Background job: return work to Draft when a co-author has not replied within 3 days |

### BF2 · Dương (7)

| Dòng | Màn hình / chức năng | Loại | Use case | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 62 | Editor Work Queue | Screen | Open Article for Scoring | To Do | The Editor opens an assigned article and its scoring sheet. The system loads the criteria set version that is active at that moment and locks it for this scoring. |
| 63 | Editor Work Queue | Screen | Change Article Category | To Do | Before scoring, the Editor may change the category the Reporter declared. The system logs who changed it, from which category and to which. After scoring, the category is locked. |
| 64 | Editor Work Queue | Screen | Score by Criteria | To Do | The Editor enters a score (0–100) for each criterion of the sheet. A comment is mandatory for every criterion scored below 50. |
| 65 | Editor Work Queue | Screen | Calculate Total Score | To Do | The system calculates the total score of the article = Σ (criterion score × criterion weight), on a scale of 100. |
| 66 | Editor Work Queue | Screen | Accept Article and Submit Score | To Do | The Editor chooses Accept and submits the scoring sheet. The system saves the scoring attempt, locks it and changes the status to PENDING_APPROVAL. |
| 67 | Editor Work Queue | Screen | Return Article for Revision | To Do | The Editor returns the article to the Reporter with a mandatory comment. The system saves the attempt, changes the status to REVISION_REQUESTED and notifies the Reporter (back to Flow 01). |
| 68 | Editor Work Queue | Screen | Reject Article | To Do | The Editor rejects the article with a mandatory reason. The system saves the attempt, changes the status to REJECTED (final) and notifies the Reporter. |

### BF3 · Vũ (2)

| Dòng | Màn hình / chức năng | Loại | Use case | Bảng | Trạng thái | Ghi chú |
|---|---|---|---|---|---|---|
| 47 | Reviewer Profile / Details Screen | Screen | View Reviewer Profile & Grading History | articles, reader_interactions, event_tags | To Do | Head of Editorial Dept: inspect reviewer grading reliability, appeal frequency, deviation flags, and workload distribution |
| 48 | Manual Re-assignment / Exception Screen | Screen | Manual Re-assignment & Exception Handling | kpi_periods, kpi_progress, kpi_snapshots | To Do | Head of Editorial Dept / Admin: manually assign qualified editor from another department when home department lacks eligible blind reviewers |

### BF4 · Quang (4)

| Dòng | Màn hình / chức năng | Loại | Use case | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 23 | Pending Grading List | Screen | View Articles Pending Grading | To Do | Editorial Secretary: published articles of a period, filter by graded status, section, genre |
| 24 | Article Royalty Grading | Screen | Grade Article Royalty | To Do | Editorial Secretary: set/adjust factor for written part, validated against genre factor range |
| 25 | Media Royalty Grading | Screen | Grade Media Royalty | To Do | Editorial Secretary: set factor for each image/video of an article (uses Media Factor Ranges) |
| 26 | Royalty Period Detail | Screen | Close Royalty Period | To Do | Editorial Secretary: check all articles graded, then close period and send to Accountant (Open -> Closed) |

### BF5 · Khang (4)

| Dòng | Màn hình / chức năng | Loại | Use case | Bảng | Trạng thái | Ghi chú |
|---|---|---|---|---|---|---|
| 34 | Automated Aggregation Job | Batch Job | Trigger Automated KPI Aggregation | kpi_progress, articles, kpi_targets | To Do | Backend Job: Automatically scan PUBLISHED articles, apply co-authorship weight splits (0.6 / 0.4), and aggregate score progress. |
| 35 | Department KPI Dashboard | Screen | View Department KPI Board | kpi_targets, kpi_progress, articles | To Do | Head of Dept: View overall department completion rates and reporter progress metrics filtered by period. |
| 36 | Reporter KPI Details Screen | Screen | View Reporter KPI Details | reader_interactions, articles, csv_import_logs | To Do | Head of Dept: Inspect individual published works, effective quality scores, and co-author split ratios (0.6 / 0.4). |
| 37 | Personal KPI & Progress Screen | Screen | View Personal KPI & Progress, View Final KPI Result | articles, reader_interactions, categories | To Do | Reporter: Track assigned monthly targets, accumulated converted articles, view metrics, and final Pass / Not Pass status. |

## Iter3: 22 việc

### BF1 · Minh (4)

| Dòng | Màn hình / chức năng | Loại | Use case | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 15 | Content Duplication Result | Screen | Check Content Duplication | To Do | Reporter: overlap % per matched work (5-syllable shingles); under 20% ok, 20-50% needs explanation, over 50% with Original is blocked |
| 16 | Duplicate Passage Comparison | Screen | Explain Content Duplication | To Do | Reporter: side-by-side view of overlapping passages, enter explanation |
| 17 | Media Duplication Result | Screen | Check Media Duplication | To Do | Reporter: each image/video marked new, reused or external (dHash <= 5 bits); reused and external media earn no royalty |
| 18 | Royalty Estimate | Screen | Estimate Royalty | To Do | Reporter: estimated royalty before submitting, using the Flow 04 calculation |

### BF2 · Dương (6)

| Dòng | Màn hình / chức năng | Loại | Use case | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 69 | Score Approval | Screen | View Approval Queue | To Do | The Head of Department views the scoring results waiting for approval (status PENDING_APPROVAL). |
| 70 | Score Approval | Screen | Review Scoring Details | To Do | The Head of Department opens a scoring result to see the score of each criterion, the comments and the total score before deciding. |
| 71 | Score Approval | Screen | Approve Scoring Result | To Do | The Head of Department approves the scoring result. The system publishes the score (status SCORE_PUBLISHED), starts the 7-day complaint deadline (Flow 03) and notifies the Reporter. |
| 72 | Score Approval | Screen | Request Re-scoring | To Do | The Head of Department does not approve the score and sends the article back to the same Editor with a reason. The new score replaces the old one; both attempts are kept. |
| 73 | Publish Approval | Screen | Approve Article for Publishing | To Do | The Head of Department approves the article for publishing. The system records the publish date (status PUBLISHED). This date decides the unit rate and the payment deadline in Flow 04. |
| 74 | Scoring Result by Criterion (*) | Screen | View Scoring Result by Criterion | To Do | The main author or a co-author views the published score of their article, criterion by criterion, with the Editor's comments. |

### BF3 · Vũ (4)

| Dòng | Màn hình / chức năng | Loại | Use case | Bảng | Trạng thái | Ghi chú |
|---|---|---|---|---|---|---|
| 49 | Re-evaluation Scorecard Screen (Blind Review) | Screen | Perform Blind Re-Evaluation; Perform Third Blind Review | kpi_periods, kpi_progress, departments | To Do | Re-evaluating Editor: evaluate assigned article blindly without seeing past scores/comments, applying original criteria version and category |
| 50 | Complaint List & Management Screen | Screen | Monitor and Manage Appeals | kpi_progress, articles, kpi_targets | To Do | Head of Editorial Dept: track active/resolved appeal queue, monitor blind review reviewer assignments and resolution progress |
| 51 | Score Comparison & Evaluation Detail Screen | Screen | Compare Evaluation Scores & Variance Rules | kpi_targets, kpi_progress, articles | To Do | Head of Editorial Dept: view side-by-side criteria breakdown across rounds; system applies variance rules (Higher/Equal -> new score; Lower <= 5 -> keep old; Lower > 5 -> trigger 3rd review & median) to finalize effective score |
| 52 | Setup Cross-Review Batch Screen | Screen | Setup Cross-Review Batch | reader_interactions, articles, csv_import_logs | To Do | Head of Editorial Dept: configure peer cross-review sampling campaigns to measure reviewer grading variance without affecting official scores |

### BF4 · Quang (4)

| Dòng | Màn hình / chức năng | Loại | Use case | Trạng thái | Ghi chú |
|---|---|---|---|---|---|
| 27 | Period Statement | Screen | Review Period Statement; Return Royalty Period | To Do | Accountant: review factors and estimated totals of a closed period; return to Editorial Secretary with reason |
| 28 | Lock Period & Royalty Calculation | Screen | Lock Royalty Period | To Do | Accountant: lock period; system calculates amounts (factor x unit price) and splits by co-author share % |
| 29 | Royalty Payment | Screen | Record Royalty Payment | To Do | Accountant: mark each person as paid; period becomes Paid when everyone is paid |
| 30 | My Royalties | Screen | View My Royalties | To Do | Reporter: own royalties per period (articles, share %, media amounts); needs locked period data |

### BF5 · Khang (4)

| Dòng | Màn hình / chức năng | Loại | Use case | Bảng | Trạng thái | Ghi chú |
|---|---|---|---|---|---|---|
| 38 | Reader Data Import Screen | Screen | Import Reader Data (CSV), View Import Logs |  | To Do | Admin: Upload daily CSV reader metrics (overwrite existing entries) + tab for import audit history. |
| 39 | Genre Popularity Report Screen | Screen | View Genre Popularity Report |  | To Do | Head of Dept: View genre-based analytics (mean/median views, Bayesian rating, royalty cost per 1,000 views, avg quality score). |
| 40 | Trending Events Report Screen | Screen | View Trending Events Report | articles, reader_interactions, event_tags | To Do | Head of Dept: Detect emerging event surges where views in the last 7 days exceed 2x the 4-week baseline average. |
| 41 | Monthly KPI Finalization Screen | Screen | Finalize Monthly KPI | kpi_periods, kpi_progress, kpi_snapshots | To Do | Head of Dept: Lock period, trigger automated Pass / Not Pass evaluations, and store permanent immutable KPI snapshots. |

## Sheet Use Case: 68 use case

Giữ nguyên nội dung Excel (tiếng Anh). Excel không ghi mã UC, mã chính thức xem trong các file `docs/SRS_BF0x_*.md`.

### BF1 (13)

| Dòng | Use case | Actor chính | Actor phụ | Mô tả |
|---|---|---|---|---|
| 2 | View My Works | Reporter |  | List works where I am the main author or a co-author, filter by status, and see works waiting for my confirmation |
| 3 | View Work Details | Reporter |  | View the content, the locked declarations, the confirmation status of each co-author and the status history of a work |
| 4 | Create and Edit Work | Reporter |  | Create or edit a draft work and declare its category, its nature (original / translation / compilation / repost) and the author or source of each image or video |
| 5 | Declare Co-authors and Shares | Reporter |  | Add up to 5 authors to a work and set each share; every share must be above 0% and the total must be 100% |
| 6 | Submit Work | Reporter | Header section | Submit the work; declarations are locked, duplication is checked, and the work enters the assignment queue once every co-author confirms |
| 7 | Check Content Duplication | Reporter |  | Compare the content with all non-draft works by 5-syllable shingles: under 20% is normal, 20–50% needs an explanation, over 50% with nature Original is blocked |
| 8 | Check Media Duplication | Reporter |  | Compare images by dHash (5 bits or less means duplicate) and videos by link or file hash; reused or external media earn no royalty |
| 9 | Explain Content Duplication | Reporter |  | Compare the overlapping passages and enter an explanation when the overlap is between 20% and 50% |
| 10 | Estimate Royalty | Reporter |  | Preview the estimated royalty of the work before submitting (Iter 3) |
| 11 | Revise and Resubmit Work | Reporter | Editor | Revise a returned work; content-only changes are resubmitted directly, changes to authors or shares need confirmation again |
| 12 | Withdraw Work | Reporter |  | Withdraw a submitted work before it is assigned; the work returns to Draft |
| 13 | Confirm Co-authorship | Reporter |  | As a co-author, accept or reject the declared share; the work returns to Draft if anyone rejects or does not reply within 3 days |
| 14 | Configure Intake Parameters | Admin |  | Set the maximum authors per work, the confirmation deadline, the duplication thresholds and the image-hash threshold |

### BF2 (21)

| Dòng | Use case | Actor chính | Actor phụ | Mô tả |
|---|---|---|---|---|
| 16 | View Criteria Sets | Admin |  | The Admin views the criteria sets of every category, with version number, status (DRAFT / ACTIVE / RETIRED) and the weight of each criterion. |
| 17 | Create Criteria Set Version | Admin |  | The Admin creates a new version of the criteria set of a category (criterion names and percentage weights). A version that has been used is never edited; a change always creates a new version. |
| 18 | Validate Total Criteria Weight | System |  | The system checks that the weights of all criteria in a version add up to 100% (each weight > 0) before the version can be saved. |
| 19 | Activate Criteria Set Version | Admin |  | The Admin sets a saved version as the active version of its category. The version used for a scoring is the one active at the moment the Editor opens the scoring sheet. |
| 20 | View Assignment Queue | Header of section |  | The Head of Department views the articles in SUBMITTED status that are waiting for an Editor, with category, author and submission date. |
| 21 | View Suggested Editors | System |  | The system suggests Editors for an article: those currently scoring the fewest articles first, with priority to Editors who have already scored the same category. |
| 22 | Assign Editor to Article | Header of section |  | The Head of Department selects an Editor for the article. The system saves the assignment, changes the status to ASSIGNED and notifies the Editor. |
| 23 | View My Assigned Articles | Editor |  | The Editor views the articles assigned to them, including articles returned to them for re-scoring, with status and assignment date. |
| 24 | Open Article for Scoring | Editor |  | The Editor opens an assigned article and its scoring sheet. The system loads the criteria set version that is active at that moment and locks it for this scoring. |
| 25 | Change Article Category | Editor |  | Before scoring, the Editor may change the category the Reporter declared. The system logs who changed it, from which category and to which. After scoring, the category is locked. |
| 26 | Score by Criteria | Editor |  | The Editor enters a score (0–100) for each criterion of the sheet. A comment is mandatory for every criterion scored below 50. |
| 27 | Calculate Total Score | System |  | The system calculates the total score of the article = Σ (criterion score × criterion weight), on a scale of 100. |
| 28 | Accept Article and Submit Score | Editor |  | The Editor chooses Accept and submits the scoring sheet. The system saves the scoring attempt, locks it and changes the status to PENDING_APPROVAL. |
| 29 | Return Article for Revision | Editor |  | The Editor returns the article to the Reporter with a mandatory comment. The system saves the attempt, changes the status to REVISION_REQUESTED and notifies the Reporter (back to Flow 01). |
| 30 | Reject Article | Editor |  | The Editor rejects the article with a mandatory reason. The system saves the attempt, changes the status to REJECTED (final) and notifies the Reporter. |
| 31 | View Approval Queue | Header of section |  | The Head of Department views the scoring results waiting for approval (status PENDING_APPROVAL). |
| 32 | Review Scoring Details | Header of section |  | The Head of Department opens a scoring result to see the score of each criterion, the comments and the total score before deciding. |
| 33 | Approve Scoring Result | Header of section |  | The Head of Department approves the scoring result. The system publishes the score (status SCORE_PUBLISHED), starts the 7-day complaint deadline (Flow 03) and notifies the Reporter. |
| 34 | Request Re-scoring | Header of section |  | The Head of Department does not approve the score and sends the article back to the same Editor with a reason. The new score replaces the old one; both attempts are kept. |
| 35 | Approve Article for Publishing | Header of section |  | The Head of Department approves the article for publishing. The system records the publish date (status PUBLISHED). This date decides the unit rate and the payment deadline in Flow 04. |
| 36 | View Scoring Result by Criterion | Reporter |  | The main author or a co-author views the published score of their article, criterion by criterion, with the Editor's comments. |

### BF3 (11)

| Dòng | Use case | Actor chính | Actor phụ | Entity (cột B–D) | Mô tả |
|---|---|---|---|---|---|
| 38 | Submit Quality Appeal | Reporter |  | Head of Editorial Department; System; Set monthly KPI quotas (minimum published articles, target views, quality benchmark) for reporters or apply bulk defaults by role. | Allows the main author to submit a formal quality appeal within 7 days of score publication (limited to 1 attempt per article), requiring specific criteria selection and detailed reasoning (ô cột I: `*`) |
| 39 | Perform Blind Re-Evaluation | Editor |  | Reporter; —; View assigned monthly individual targets and track real-time progress of published works and view count achievements throughout the cycle. | Assigned editor conducts an independent blind re-grading without seeing prior scores or comments, applying the same criteria version and category as the initial evaluation |
| 40 | Perform Third Blind Review & Median Resolution | Editor |  | Head of Editorial Department; —; Monitor the overall department-wide KPI dashboard displaying completion rates and progress metrics across all team reporters. | Triggered when the 2nd evaluation score is lower than the original score by more than 5 points, requiring a third independent blind review to calculate the median score across all three rounds |
| 41 | Monitor and Manage Appeals | Header section |  | Head of Editorial Department; —; Inspect granular performance details of an individual reporter, including individual published works, page views, and milestone rates. | Enables Department Head to monitor all submitted quality appeals, oversee reviewer assignments, and track resolution workflows |
| 42 | Compare Evaluation Scores & Variance Rules | Header section |  | Admin; —; Manually enter offline survey metrics, print-edition reader feedback, and external social engagement numbers for the current evaluation month. | Displays detailed comparison across evaluation rounds and applies decision rules to finalize the effective score |
| 43 | Setup Cross-Review Batch | Header section |  | Head of Editorial Department; —; Review consolidated analytical reports on organic readership trends, total aggregated views, and audience engagement statistics. | Configures peer cross-review campaigns to measure reviewer grading variance. Scores from cross-reviews are strictly analytical and do not alter official article scores |
| 44 | Analyze Reviewer Score Deviations | Header section |  | Head of Editorial Department; —; Review editorial throughput metrics, turnaround speed, and processing workload statistics of editors before finalizing evaluations. | Generates analytical reporting on reviewer grading consistency, updating deviation indices to maintain grading quality control |
| 45 | View Reviewer Profile & Grading History | Header section |  | Head of Editorial Department; System; Review aggregated system scores, enter qualitative assessments, assign final grade ranks (A/B/C), and finalize the monthly ranking form. | Displays comprehensive profiles and grading history of editors, tracking evaluation reliability, past appeal rates, and workload distribution |
| 46 | Manual Re-assignment & Exception Handling | Header section |  | Head of Editorial Department; —; Review editorial throughput metrics, turnaround speed, and processing workload statistics of editors before finalizing evaluations. | Handles exception cases when the home department lacks eligible editors, allowing manual assignment of qualified editors from other departments |
| 47 | Configure Appeal Rules & Scoring Parameters | Header section |  | Head of Editorial Department; System; Review aggregated system scores, enter qualitative assessments, assign final grade ranks (A/B/C), and finalize the monthly ranking form. | Configures system parameters governing complaint handling, appeal submission windows, score variance thresholds, and scoring scales |
| 48 | Audit System & Grading Logs | Header section |  |  | Maintains complete, immutable audit logs of all evaluation attempts, score updates, and workflow state changes while keeping old scores hidden from evaluators |

### BF4 (12)

| Dòng | Use case | Actor chính | Actor phụ | Mô tả |
|---|---|---|---|---|
| 50 | View Royalty Periods | Editorial Secretary | Accountant | View monthly periods with status, number of graded/ungraded articles and totals |
| 51 | View Articles Pending Grading | Editorial Secretary |  | List published articles of a period, filter by graded status, section, genre |
| 52 | Grade Article Royalty | Editorial Secretary |  | Set or adjust the royalty factor for the written part of an article |
| 53 | Grade Media Royalty | Editorial Secretary |  | Set the factor for each image/video of a published article |
| 54 | Close Royalty Period | Editorial Secretary |  | Confirm all articles are graded and send the period to the Accountant |
| 55 | Manage Unit Prices | Accountant |  | Add a new unit price with an effective date; view price history |
| 56 | Review Period Statement | Accountant |  | Check factors and estimated totals of a closed period |
| 57 | Return Royalty Period | Accountant |  | Send a closed period back to the Editorial Secretary with a reason |
| 58 | Lock Royalty Period | Accountant |  | Lock the period; the system calculates amounts and splits them per person |
| 59 | Record Royalty Payment | Accountant |  | Mark each person as paid; the period becomes Paid when everyone is paid |
| 60 | View My Royalties | Reporter |  | View own royalties per period: articles, share %, media amounts, total |
| 61 | Manage Media Factor Ranges | Admin |  | Set min–max factor for images and videos |

### BF5 (11)

| Dòng | Use case | Actor chính | Actor phụ | Entity (cột B–D) | Mô tả |
|---|---|---|---|---|---|
| 63 | Configure KPI Benchmarks | Admin / Head of Editorial Dept |  | Head of Editorial Department; System; Set monthly KPI quotas (minimum published articles, target views, quality benchmark) for reporters or apply bulk defaults by role. | Configure baseline benchmark criteria (minimum article quota, quality grade threshold, and neglected genre warning bounds) based on staff seniority or role. |
| 64 | Set Monthly Targets | Head of Editorial Department |  | Reporter; —; View assigned monthly individual targets and track real-time progress of published works and view count achievements throughout the cycle. | Set monthly KPI quotas (minimum published articles, target views, quality benchmark) for reporters or apply bulk defaults by role/seniority. |
| 65 | View Personal KPI & Progress | Reporter |  | Head of Editorial Department; —; Monitor the overall department-wide KPI dashboard displaying completion rates and progress metrics across all team reporters. | View assigned monthly individual targets and monitor real-time completion progress of published articles (calculated with co-author ratios) and projected pass/fail status throughout the cycle. |
| 66 | View Department KPI Board | Head of Editorial Department |  | Head of Editorial Department; —; Inspect granular performance details of an individual reporter, including individual published works, page views, and milestone rates. | Monitor the overall department-wide KPI dashboard displaying completion rates and progress metrics across all team reporters. |
| 67 | View Reporter KPI Details | Head of Editorial Department |  | Admin; —; Manually enter offline survey metrics, print-edition reader feedback, and external social engagement numbers for the current evaluation month. | Inspect granular performance details of an individual reporter, including published works, effective quality scores, co-author weightings, and milestone progress. |
| 68 | Import Reader Data (CSV) | Admin |  | Head of Editorial Department; —; Review consolidated analytical reports on organic readership trends, total aggregated views, and audience engagement statistics. | Upload daily reader engagement and interaction metrics via CSV file (page views, rating counts, average ratings); system automatically overwrites existing records for duplicate dates and articles. |
| 69 | View Genre Popularity Report | Head of Editorial Department |  | Head of Editorial Department; —; Review editorial throughput metrics, turnaround speed, and processing workload statistics of editors before finalizing evaluations. | Review analytical reports on audience preferences across genres (average/median views, Bayesian rating, royalty cost per 1,000 views, and average quality score). |
| 70 | View Trending Events Report | Head of Editorial Department |  | Head of Editorial Department; System; Review aggregated system scores, enter qualitative assessments, assign final grade ranks (A/B/C), and finalize the monthly ranking form. | Review the surge detection report highlighting topics or events where traffic over the last 7 days exceeds twice the baseline of the previous 4 weeks. |
| 71 | Finalize Monthly KPI | Head of Editorial Department |  | Head of Editorial Department; System; Set monthly KPI quotas (minimum published articles, target views, quality benchmark) for reporters or apply bulk defaults by role. | Conclude the monthly cycle, trigger automated Pass / Not Pass status evaluation based on established criteria, and capture a permanent KPI snapshot. |
| 72 | View Import Logs & History | Admin |  |  | Review execution logs of CSV reader data imports, including row-level success/error details for system auditability and verification. |
| 73 | Trigger Automated KPI Aggregation | Admin |  |  | Administrator triggers or monitors the scheduled background task to aggregate monthly published works, apply co-authorship weight splits (e.g., 0.6 / 0.4), and compute progress continuously. |

## Hướng dẫn điền (comment trong Excel)

- **Use Case!B1**: KienNT: Điền tên entity và bên dưới là các tương tác (C~Create, R~Read, U~Update, hoặc D~Delete) từ use case tới entity
- **Functions!C1**: KienNT: Điền tiên bảng và bên dưới là các tương tác (C~Create, R~Read, U~Update, D~Delete) từ màn hình/chức năng vào bảng
- **Functions!D1**: KienNT: Điền tiên bảng và bên dưới là các tương tác (C~Create, R~Read, U~Update, D~Delete) từ màn hình/chức năng vào bảng
- **Functions!I1**: KienNT: Phân tích yêu cầu cho màn hình/chức năng được mô tả trong phần nào của tài liệu SRS?
- **Functions!J1**: KienNT: Thiết kế cho màn hình/chức năng được mô tả trong phần nào của tài liệu SDS?
