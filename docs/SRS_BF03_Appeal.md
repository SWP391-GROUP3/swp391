# SRS — BF-03 Kiểm soát chất lượng chấm (Quality control of scores)

> Chuyển từ `SRS.pdf`. Đọc kèm `docs/SRS_Overview.md` và `database/schema.sql` (bảng `appeals`, `appeal_criteria`, `appeal_resolutions`, `appeal_rule_configs`, `cross_review_batches`, `cross_review_tasks`, `reviewer_deviation_indices`, `audit_logs`).
>
> **Ghi chú (Vấn đề #5):** `AGENTS.md` mục 2 đang ghi BF-03 là *Duyệt đăng* (package `publishing`). SRS định nghĩa BF-03 là luồng khiếu nại / chấm mù dưới đây. Nhóm cần chốt package nào sở hữu luồng này.

- **Trigger (mục 2.3):** Phóng viên gửi khiếu nại.
- **End condition (mục 2.3):** Nhận khiếu nại và chấm lại, sau đó cập nhật chỉ số.

## Swimlane

![BF-03 swimlane](srs-images/bf03-swimlane.png)

Mô tả sơ đồ (3 làn: Reporter/Author | Editor | Head of Department):

1. Reporter: Submit a complaint regarding the scoring result.
2. Head: Receive and manage the complaint → Assign a different Editor to conduct a Blind Review.
3. Editor: Receive the Blind Review assignment → Independently Review the Article → Submit the review results to the Head.
4. Head: Review the Blind Review results → **Is this the final score?**
   - Yes (đã có lần chấm thứ 3) → Calculate the **median** of all three scoring rounds → Finalize the effective score.
   - No → Compare the original score with the reassessment results → **Score?**
     - Higher or Equal → Applying new Score.
     - Lower, difference 1–4 point → Keep the Original Score.
     - Lower, difference > 5 point → quay lại bước 2 (giao BTV khác chấm mù lần 3).
5. Mọi nhánh → Complete Audit Log recording & control reporting → Update the deviation indices for the editors → End.

> **Ghi chú:** sơ đồ ghi "1–4" và "> 5". Đúng 5 điểm không có trong sơ đồ. BR-QC-06 chỉ nói "more than 5 points". Cách xử lý chênh lệch ≤ 5 điểm do "active two-score resolution rule" cấu hình ở UC-44 quyết định (UC-39 bước 5).

## Use Case Diagram

![UC BF-03](srs-images/uc-bf03-quality-control.png)

- Department Head: Monitor and Manage Appeals, View Reviewer Profile & Grading History, Manual Re-assignment & Exception Handling, Configure Appeal Rules & Scoring Parameters.
- «extend» Monitor and Manage Appeals: Compare Evaluation Scores & Variance Rules, Setup Cross-Review Batch, Analyze Reviewer Score Deviations.
- «extend» Configure Appeal Rules & Scoring Parameters: Audit System & Grading Logs.
- Reporter: Submit Quality Appeal, Edit Article Content.
- Editor: Perform Blind Re-Evaluation, Perform Third Blind Review & Median Resolution.

## Use Cases (4.2.3)

Actor chính: Reporter, Head of ED, Editor · 11 use case · ID 35–45

| ID | Use Case | Feature | Use Case Description |
|---|---|---|---|
| 35 | Submit Quality Appeal | Submit & Track Appeal | Allows the Reporter to submit a formal quality appeal within 7 days of score publication (limited to 1 attempt per article), requiring specific criteria selection and detailed reasoning |
| 36 | Perform Blind Re-Evaluation | Re-evaluation Scorecard - Blind Review | Assigned editor conducts an independent blind re-grading without seeing prior scores or comments, applying the same criteria version and category as the initial evaluation |
| 37 | Perform Third Blind Review & Median Resolution | Re-evaluation Scorecard - Blind Review | Triggered when the 2nd evaluation score is lower than the original score by more than 5 points, requiring a third independent blind review to calculate the median score across all three rounds |
| 38 | Monitor and Manage Appeals | Complaint List & Management | Enables Department Head to monitor all submitted quality appeals, oversee reviewer assignments, and track resolution workflows |
| 39 | Compare Evaluation Scores & Variance Rules | Score Comparison & Evaluation Detail | Displays detailed comparison across evaluation rounds and applies decision rules to finalize the effective score |
| 40 | Setup Cross-Review Batch | Setup Cross-Review Batch | Configures peer cross-review campaigns to measure reviewer grading variance. Scores from cross-reviews are strictly analytical and do not alter official article scores |
| 41 | Analyze Reviewer Score Deviations | Reviewer Score Deviation Report | Generates analytical reporting on reviewer grading consistency, updating deviation indices to maintain grading quality control |
| 42 | View Reviewer Profile & Grading History | Reviewer Profile / Details | Displays comprehensive profiles and grading history of editors, tracking evaluation reliability, past appeal rates, and workload distribution |
| 43 | Manual Re-assignment & Exception Handling | Manual Re-assignment / Exception | Handles exception cases when the home department lacks eligible editors, allowing manual assignment of qualified editors from other departments |
| 44 | Configure Appeal Rules & Scoring Parameters | Configure Appeal Rules & Scoring Scale | Configures system parameters governing complaint handling, appeal submission windows, score variance thresholds, and scoring scales |
| 45 | Audit System & Grading Logs | System Audit Log / Grading Audit Log | Maintains complete, immutable audit logs of all evaluation attempts, score updates, and workflow state changes while keeping old scores hidden from evaluators |

## Use cases full dressed (II.1.2.3 — Flow 3)

### UC-35: Submit Quality Appeal

| Field | Value |
|---|---|
| Actor | Reporter (author of the article) |
| Trigger | The Reporter opens a published scoring result and clicks Submit Appeal. |
| Preconditions | The article is SCORE_PUBLISHED; the 7-day appeal window has not expired; no appeal has previously been submitted for this article. |
| Postconditions | A quality appeal is saved as SUBMITTED; the selected disputed criteria and detailed reason are recorded; the Head of Editorial Department is notified; an audit log entry is written. |
| Business Rules | BR-QC-01, 02 |

**Normal Flow**
1. System shows the published result by criterion, the Editor's comments, the publication time of the score, and the remaining appeal time.
2. Reporter selects one or more disputed criteria and enters a detailed reason for each disputed item.
3. Reporter clicks Submit Appeal.
4. System validates the deadline, the one-appeal limit, the selected criteria, and the required reasons (BR-QC-01, 02).
5. System saves the appeal as SUBMITTED, writes the audit log, and notifies the Head of Editorial Department.

**Alternative / Exceptions**
- A1 Reporter cancels before submitting: no appeal is created.
- E1 The 7-day window has expired: "The appeal period has ended."
- E2 An appeal already exists for the article: "Only one appeal is allowed per article."
- E3 No criterion is selected or a reason is blank: system identifies the missing information and does not submit the appeal.

### UC-36: Perform Blind Re-Evaluation

| Field | Value |
|---|---|
| Actor | Assigned Editor |
| Trigger | The assigned Editor opens a blind re-evaluation task. |
| Preconditions | The appeal is IN_REVIEW; the Editor is assigned to the appeal and did not perform the original scoring; the appealed article version and its criteria set version are available. |
| Postconditions | A second scoring attempt is saved and locked; the system records the total score and sends the result to variance comparison in UC-39; prior scores and comments remain hidden from the Editor. |
| Business Rules | BR-QC-03, 04, 05, 09 |

**Normal Flow**
1. System shows the appealed article version and the same category and criteria set version used in the original scoring, but hides all previous scores, comments, reviewer identities, and comparison results (BR-QC-03).
2. Editor independently enters a score from 0 to 100 for every criterion.
3. Editor enters a comment for every criterion scored below 50.
4. System recalculates the weighted total using the locked criteria weights (BR-QC-05).
5. Editor reviews and submits the re-evaluation.
6. System validates the scorecard, saves and locks the second attempt, writes the audit log, and continues to UC-39.

**Alternative / Exceptions**
- A1 Editor saves the scorecard as an incomplete draft and resumes it later; no comparison runs until submission.
- E1 A criterion is not scored or a required comment is missing: system lists the missing items and keeps the attempt open.
- E2 The task is assigned to another Editor or the attempt is already locked: the scorecard is read-only and cannot be submitted.

### UC-37: Perform Third Blind Review and Median Resolution

| Field | Value |
|---|---|
| Actor | Third assigned Editor; Head of Editorial Department |
| Trigger | UC-39 determines that the second evaluation score is lower than the original score by more than 5 points. |
| Preconditions | The original and second scoring attempts are locked; the appeal is IN_REVIEW; a third eligible Editor who did not perform either earlier evaluation can be assigned. |
| Postconditions | A third scoring attempt is saved and locked; the median of the three total scores becomes the final effective score; the appeal is RESOLVED; affected users and downstream processes are notified. |
| Business Rules | BR-QC-03, 04, 05, 06, 07, 09, 10 |

**Normal Flow**
1. System flags the appeal for a third blind review and creates a new review task (BR-QC-06).
2. Head of Department assigns a third eligible Editor using the same eligibility rules as UC-38.
3. System shows the third Editor the same article version, category, and criteria set version while hiding all prior scores, comments, reviewer identities, and variance information.
4. Third Editor completes and submits the scorecard under the same validation rules as UC-36.
5. System saves and locks the third attempt.
6. System calculates the median of the original, second, and third total scores and records it as the final effective score (BR-QC-07).
7. System resolves the appeal, writes the audit log, notifies the Reporter and Head of Department, and propagates the effective score to BF-04 and BF-05 where applicable (BR-QC-10).

**Alternative / Exceptions**
- A1 No eligible Editor is available in the home department: Head of Department uses UC-43 for cross-department assignment.
- E1 The third scorecard is incomplete: system lists the missing items and does not calculate the median.
- E2 The appeal was resolved by another valid operation before submission: the third attempt is shown read-only and no score is overwritten.

### UC-38: Monitor and Manage Appeals

| Field | Value |
|---|---|
| Actor | Head of Editorial Department |
| Trigger | The Head of Department opens the complaint list after a quality appeal is submitted. |
| Preconditions | The appeal exists in SUBMITTED or IN_REVIEW status; the article, appealed scoring attempt, and disputed criteria are available. |
| Postconditions | The appeal status and assignment are updated; an eligible Editor is assigned for blind re-evaluation; the assigned Editor is notified; the assignment is audited. |
| Business Rules | BR-QC-03, 04, 09 |

**Normal Flow**
1. System shows the appeal queue with article, Reporter, submission time, deadline status, assigned reviewer, and appeal status.
2. Head of Department opens an appeal and reviews the disputed criteria and the Reporter's reasons.
3. System lists eligible Editors who did not perform the original scoring and who have not reviewed the same appeal (BR-QC-03, 04).
4. Head of Department selects an Editor and confirms the assignment.
5. System sets the appeal to IN_REVIEW, creates a blind re-evaluation task, writes the audit log, and notifies the assigned Editor.

**Alternative / Exceptions**
- A1 No eligible Editor is available in the home department: Head of Department uses UC-43 to assign a qualified Editor from another department.
- A2 Head of Department filters the queue by status, date, department, article, or assigned reviewer.
- E1 The selected Editor performed an earlier evaluation of the same article: the assignment is refused.
- E2 The appeal has already been RESOLVED: it is displayed read-only.

### UC-39: Compare Evaluation Scores and Apply Variance Rules

| Field | Value |
|---|---|
| Actor | Head of Editorial Department; System |
| Trigger | A blind re-evaluation attempt is submitted and locked. |
| Preconditions | The original scoring attempt and at least one blind re-evaluation attempt are locked; the appeal is IN_REVIEW. |
| Postconditions | The variance rule is applied; either a third blind review is required or the appeal is finalized under the active appeal rules; the effective score and resolution history are recorded. |
| Business Rules | BR-QC-06, 07, 09, 10 |

**Normal Flow**
1. System displays the original and completed re-evaluation results by criterion to the Head of Department and calculates the total-score difference.
2. System applies the active variance threshold and resolution parameters from UC-44 (BR-QC-06).
3. If the second score is lower than the original score by more than 5 points, system requires UC-37 and keeps the appeal IN_REVIEW.
4. When three valid scores exist, system selects the median as the final effective score (BR-QC-07).
5. Otherwise, system finalizes the appeal using the active two-score resolution rule configured in UC-44.
6. System records the effective score, sets the appeal to RESOLVED, preserves every attempt, writes the audit log, and notifies the Reporter and Head of Department.
7. System sends the effective score to royalty and KPI calculations; if the related royalty period is already LOCKED, BF-04 creates an adjustment line in the next OPEN period (BR-QC-10).

**Alternative / Exceptions**
- A1 Head of Department opens the criterion-level comparison to inspect where the variance occurred before resolution.
- A2 The effective score is unchanged: system resolves the appeal without creating a downstream adjustment.
- E1 A required attempt is incomplete or not locked: comparison is not finalized.
- E2 The appeal is already RESOLVED: the comparison is read-only and cannot be run again.

Simple analytical, profile, configuration, exception-handling, cross-review, and audit use cases (UC-40, UC-41, UC-42, UC-43, UC-44, UC-45) are described in the Use Case Table (Section 4.2.3) only.

## Business Rules for Quality Control of Scores (BR-QC)

| BR-ID | Rule |
|---|---|
| BR-QC-01 | An appeal may be submitted only within 7 days after the score is published. Each article is limited to one appeal. |
| BR-QC-02 | An appeal must identify at least one disputed criterion and include a detailed reason. Submitting an appeal does not erase or modify the original scoring attempt. |
| BR-QC-03 | Every re-evaluation is strictly blind. The assigned Editor sees the appealed article version and the same category and criteria set version as the original attempt, but cannot see prior scores, comments, reviewer identities, comparison results, or appeal outcomes. |
| BR-QC-04 | A blind review must be performed by an eligible Editor who did not perform any earlier evaluation of the same appeal. If no eligible Editor exists in the home department, a qualified cross-department Editor may be assigned through UC-43. |
| BR-QC-05 | Blind re-evaluation uses the BF-02 scoring rules: each criterion score is from 0 to 100, comments are mandatory for criteria below 50, and the total score uses the locked criteria weights. |
| BR-QC-06 | If the second evaluation total is lower than the original total by more than 5 points, the system must trigger a third blind review. Variance thresholds and other appeal parameters are administered through UC-44 and every configuration change is audited. |
| BR-QC-07 | When three valid evaluation totals exist, the final effective score is the median of the original, second, and third totals. |
| BR-QC-08 | Cross-review campaign scores from UC-40 are analytical only. They may update reviewer deviation statistics but must never alter an article's official or effective score. |
| BR-QC-09 | All appeal submissions, reviewer assignments, scoring attempts, comparisons, score updates, configuration changes, and workflow transitions are stored in immutable audit logs. Historical scores remain stored but are hidden from blind reviewers. |
| BR-QC-10 | The resolved effective score is used by royalty and KPI processes. A LOCKED or PAID royalty period never changes; a score difference discovered after locking creates an adjustment line in the next OPEN period. |
| BR-QC-11 | The appeal lifecycle is SUBMITTED to IN_REVIEW to RESOLVED. A RESOLVED appeal is read-only and cannot be evaluated again. |

## Luật chung liên quan

GBR-06 (khiếu nại trong 7 ngày, 1 lần/bài, chấm mù), GBR-07 (lệch > 5 điểm → chấm lần 3, lấy trung vị), GBR-11 (kỳ đã khoá không đổi, sinh dòng điều chỉnh) — xem `SRS_Overview.md`.
