/* ==========================================================================
   SWP391 - Dữ liệu mẫu
   Chạy SAU schema.sql. Chỉ dùng để dev và demo.

   TÀI KHOẢN MẪU: mọi user bên dưới có mật khẩu 123456. Đăng nhập bằng email
   hoặc username. Cột password_hash lưu BCrypt hash của "123456" (tạo bằng
   BCryptPasswordEncoder.encode("123456")), KHÔNG lưu mật khẩu thô.
   Cả 9 dòng dùng chung 1 hash cho gọn - chỉ dùng cho dữ liệu mẫu.

   schema.sql vừa tạo lại bảng nên IDENTITY bắt đầu từ 1: các id tham chiếu
   bên dưới (role_id = 1, author_id = 1, ...) dựa vào thứ tự INSERT.
   ========================================================================== */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

USE swp391_kpi;
GO

/* ---------- ADMIN ---------- */

INSERT INTO roles (code, name) VALUES
    ('REPORTER',            N'Phóng viên'),          -- 1
    ('EDITOR',              N'Biên tập viên'),       -- 2
    ('HEAD_OF_DEPARTMENT',  N'Trưởng ban'),          -- 3
    ('EDITORIAL_SECRETARY', N'Thư ký toà soạn'),     -- 4
    ('ACCOUNTANT',          N'Kế toán'),             -- 5
    ('ADMIN',               N'Quản trị hệ thống');   -- 6

INSERT INTO sections (code, name) VALUES
    ('THOISU',  N'Ban Thời sự'),             -- 1
    ('KINHTE',  N'Ban Kinh tế'),             -- 2
    ('VANHOA',  N'Ban Văn hoá - Xã hội'),    -- 3
    ('THETHAO', N'Ban Thể thao');            -- 4

/* PV / BTV / Trưởng ban có ban. TKTS, Kế toán, Admin: section_id = NULL. */
INSERT INTO users (username, email, password_hash, full_name, role_id, section_id) VALUES
    ('an.nv',    'an.nv@toasoan.vn',    '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm', N'Nguyễn Văn An',  1, 1),     -- 1 PV Thời sự
    ('binh.tt',  'binh.tt@toasoan.vn',  '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm', N'Trần Thị Bình',  1, 1),     -- 2 PV Thời sự
    ('cuong.lh', 'cuong.lh@toasoan.vn', '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm', N'Lê Hoàng Cường', 2, 1),     -- 3 BTV Thời sự
    ('dung.pt',  'dung.pt@toasoan.vn',  '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm', N'Phạm Thu Dung',  4, NULL),  -- 4 TKTS
    ('duc.vm',   'duc.vm@toasoan.vn',   '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm', N'Vũ Minh Đức',    3, 1),     -- 5 Trưởng ban Thời sự
    ('chi.hl',   'chi.hl@toasoan.vn',   '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm', N'Hoàng Lan Chi',  5, NULL),  -- 6 Kế toán
    ('hung.dq',  'hung.dq@toasoan.vn',  '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm', N'Đỗ Quốc Hưng',   1, 2),     -- 7 PV Kinh tế
    ('admin',    'admin@toasoan.vn',    '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm', N'Quản trị viên',  6, NULL),  -- 8 Admin
    ('giang.nt', 'giang.nt@toasoan.vn', '$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm', N'Ngô Thu Giang',  2, 2);     -- 9 BTV Kinh tế

INSERT INTO genres (code, name, nd_group, base_factor, max_factor) VALUES
    ('TIN_VAN',   N'Tin vắn',           N'Tin',                    1.00,  2.00),   -- 1
    ('TIN_SAU',   N'Tin sâu',           N'Tin',                    2.00,  4.00),   -- 2
    ('PHAN_ANH',  N'Bài phản ánh',      N'Bài viết',               5.00,  7.00),   -- 3
    ('PHONG_VAN', N'Bài phỏng vấn',     N'Bài viết',               6.00,  8.00),   -- 4
    ('BINH_LUAN', N'Bình luận',         N'Bình luận, chính luận',  4.00,  6.00),   -- 5
    ('PHONG_SU',  N'Phóng sự điều tra', N'Phóng sự, điều tra',    10.00, 15.00);   -- 6

INSERT INTO media_factors (media_kind, factor, max_factor) VALUES
    ('PHOTO',   1.00, 2.00),
    ('GRAPHIC', 1.50, 3.00),
    ('VIDEO',   3.00, 5.00);

INSERT INTO system_parameters (param_key, param_value, unit, flow, updated_by) VALUES
    ('MAX_AUTHORS_PER_ARTICLE',     N'5',    N'người', 'BF-01', 8),
    ('CO_AUTHOR_CONFIRM_DAYS',      N'3',    N'ngày',  'BF-01', 8),
    ('SHINGLE_SIZE',                N'5',    N'âm tiết', 'BF-01', 8),
    ('DHASH_MAX_BITS',              N'5',    N'bit',   'BF-01', 8),
    ('DUP_WARN_PERCENT',            N'20',   N'%',     'BF-01', 8),
    ('DUP_BLOCK_PERCENT',           N'50',   N'%',     'BF-01', 8),
    ('SCORE_TIER_HIGH',             N'90',   N'điểm',  'BF-04', 8),
    ('SCORE_TIER_MID',              N'70',   N'điểm',  'BF-04', 8),
    ('TIER_RATE_HIGH',              N'100',  N'%',     'BF-04', 8),
    ('TIER_RATE_MID',               N'80',   N'%',     'BF-04', 8),
    ('TIER_RATE_LOW',               N'60',   N'%',     'BF-04', 8),
    ('NATURE_RATE_ORIGINAL',        N'100',  N'%',     'BF-04', 8),
    ('NATURE_RATE_TRANSLATION',     N'70',   N'%',     'BF-04', 8),
    ('NATURE_RATE_COMPILATION',     N'50',   N'%',     'BF-04', 8),
    ('NATURE_RATE_REPOST',          N'30',   N'%',     'BF-04', 8),
    ('MIN_FINAL_FACTOR',            N'1',    NULL,     'BF-04', 8),
    ('ROUNDING_UNIT',               N'1000', N'VND',   'BF-04', 8);

/* ---------- ROYALTY: danh mục ---------- */

INSERT INTO events (name, start_date, end_date, window_days, k_limit, exempt_score, created_by) VALUES
    (N'Bão số 3 năm 2026', '2026-09-05', '2026-09-20', 7, 10, 90, 4);   -- 1

/* 1 hệ số = 10% lương cơ sở 2.340.000đ. */
INSERT INTO royalty_rates (base_salary, amount, effective_from, legal_ref, created_by) VALUES
    (2340000, 234000, '2026-01-01', N'Nghị định 18/2014/NĐ-CP, quy chế nhuận bút toà soạn', 6);   -- 1

INSERT INTO royalty_periods (period_year, period_month, status) VALUES
    (2026, 9, 'OPEN');   -- 1

/* ---------- ARTICLE: bài mẫu đủ 3 trạng thái ---------- */

/* Bài 1: PUBLISHED, thuộc sự kiện bão (xếp thứ 3 <= k_limit nên không giảm),
          có đồng tác giả, đã chấm điểm, đã chấm nhuận bút.
   Bài 2: DRAFT, chưa nộp.
   Bài 3: SUBMITTED, chờ Trưởng ban phân công BTV. */
INSERT INTO articles (author_id, section_id, declared_category_id, category_id, event_id, approved_by,
                      title, content, declared_nature, status, saturation_rank, saturation_rate,
                      created_at, updated_at, submitted_at, published_at) VALUES
    (1, 1, 3, 3, 1, 5, N'Người dân khu Bắc Hà khốn khổ vì ngập sau bão',
        N'Nội dung bản đã biên tập...', 'ORIGINAL', 'PUBLISHED', 3, 100.00,
        '2026-09-10 08:00', '2026-09-15 09:00', '2026-09-10 10:00', '2026-09-15 09:00'),
    (2, 1, 1, 1, NULL, NULL, N'Khai mạc hội chợ hàng Việt 2026',
        N'Bản nháp...', 'ORIGINAL', 'DRAFT', NULL, NULL,
        '2026-09-25 14:00', '2026-09-25 14:00', NULL, NULL),
    (1, 1, 2, 2, NULL, NULL, N'Giá xăng giảm lần thứ ba liên tiếp',
        N'Nội dung bản nộp...', 'ORIGINAL', 'SUBMITTED', NULL, NULL,
        '2026-09-27 07:30', '2026-09-27 08:00', '2026-09-27 08:00', NULL);

/* Tác giả chính ACCEPTED ngay; đồng tác giả đã xác nhận trong hạn 3 ngày. */
INSERT INTO article_authors (article_id, user_id, share_percent, confirm_status, confirm_deadline, responded_at) VALUES
    (1, 1,  70.00, 'ACCEPTED', NULL,               '2026-09-10 10:00'),
    (1, 2,  30.00, 'ACCEPTED', '2026-09-13 10:00', '2026-09-10 11:00'),
    (2, 2, 100.00, 'ACCEPTED', NULL,               '2026-09-25 14:00'),
    (3, 1, 100.00, 'ACCEPTED', NULL,               '2026-09-27 08:00');

INSERT INTO article_versions (article_id, version_no, kind, title, content, change_type, created_by, created_at) VALUES
    (1, 1, 'SUBMISSION', N'Người dân khu Bắc Hà khốn khổ vì ngập sau bão',
        N'Nội dung bản PV nộp...', 'INITIAL', 1, '2026-09-10 10:00'),           -- 1
    (1, 2, 'EDITED',     N'Người dân khu Bắc Hà khốn khổ vì ngập sau bão',
        N'Nội dung bản đã biên tập...', 'MINOR', 3, '2026-09-12 16:00'),        -- 2
    (3, 1, 'SUBMISSION', N'Giá xăng giảm lần thứ ba liên tiếp',
        N'Nội dung bản nộp...', 'INITIAL', 1, '2026-09-27 08:00');              -- 3

/* Ảnh của PV Bình (đồng tác giả) chụp, không trùng ảnh nào. */
INSERT INTO article_attachments (article_id, version_id, author_id, media_type, media_kind, url,
                                 file_hash, dhash, hash_result, caption) VALUES
    (1, 1, 2, 'IMAGE', 'PHOTO', N'/uploads/2026/09/bac-ha-ngap-1.jpg',
        '3b7e2f9c41d08a65e1c4b92d7f306a8e5c19b04d72e6f3a18c5d90b47e2a6f1c',
        'c3a1e0f07b1d2c4e', 'NEW', N'Nước ngập ngang gối tại ngõ 12');   -- 1

INSERT INTO duplication_checks (article_id, version_id, max_overlap_percent, result, explanation, checked_at) VALUES
    (1, 1, 0.00, 'NORMAL', N'Không có đoạn trùng với bài đã đăng.',              '2026-09-10 10:01'),   -- 1
    (3, 3, 8.00, 'NORMAL', N'Trùng một câu trích dẫn số liệu, dưới ngưỡng 20%.', '2026-09-27 08:01');   -- 2

INSERT INTO duplication_matches (check_id, matched_article_id, overlap_percent, passages) VALUES
    (2, 1, 8.00, N'"...theo số liệu của Sở Công Thương..."');

/* ---------- ASSESSMENT ---------- */

INSERT INTO assignments (article_id, editor_id, assigned_by, assigned_at, was_suggested) VALUES
    (1, 3, 5, '2026-09-10 14:00', 1);   -- 1

/* Mỗi thể loại có 1 bộ tiêu chí ACTIVE, tổng trọng số = 100. */
INSERT INTO criteria_set_versions (category_id, version_no, status, created_by, created_at, activated_by, activated_at) VALUES
    (3, 1, 'ACTIVE', 8, '2026-09-01 08:00', 8, '2026-09-01 08:30'),   -- 1 Bài phản ánh
    (2, 1, 'ACTIVE', 8, '2026-09-01 08:00', 8, '2026-09-01 08:30');   -- 2 Tin sâu

INSERT INTO criteria (criteria_set_version_id, name, weight, sort_order) VALUES
    (1, N'Tính phát hiện, độc quyền',  30.00, 1),   -- 1
    (1, N'Độ khó khi tác nghiệp',      20.00, 2),   -- 2
    (1, N'Chất lượng viết, trình bày', 30.00, 3),   -- 3
    (1, N'Tính thời sự',               20.00, 4),   -- 4
    (2, N'Độ chính xác thông tin',     40.00, 1),   -- 5
    (2, N'Tính thời sự',               35.00, 2),   -- 6
    (2, N'Cách viết, trình bày',       25.00, 3);   -- 7

/* Lần chấm 1 của bài 1 trên bản nộp (version 1):
   80*30% + 70*20% + 90*30% + 60*20% = 24 + 14 + 27 + 12 = 77.00 */
INSERT INTO scoring_attempts (article_id, version_id, assignment_id, reviewer_id, attempt_no, attempt_type,
                              criteria_set_version_id, category_id, total_score, decision, decision_note,
                              opened_at, submitted_at) VALUES
    (1, 1, 1, 3, 1, 'INITIAL', 1, 3, 77.00, 'ACCEPT',
        N'Đề tài tốt, cần thêm ý kiến cơ quan chức năng.', '2026-09-11 08:30', '2026-09-11 09:00');   -- 1

INSERT INTO scoring_attempt_details (scoring_attempt_id, criterion_id, score, comment) VALUES
    (1, 1, 80.00, NULL),
    (1, 2, 70.00, NULL),
    (1, 3, 90.00, N'Viết rõ ràng, có nhân chứng.'),
    (1, 4, 60.00, N'Đăng chậm hơn báo khác 1 ngày.');

INSERT INTO score_approvals (scoring_attempt_id, decision, decided_by, decided_at, announced_at) VALUES
    (1, 'APPROVED', 5, '2026-09-12 08:00', '2026-09-12 08:05');

/* ---------- APPEAL ---------- */

INSERT INTO appeal_rule_configs (appeal_window_days, max_appeals_per_article, variance_threshold, effective_from, updated_by) VALUES
    (7, 1, 10.00, '2026-01-01', 8);

/* PV An khiếu nại điểm "Tính thời sự" của bài 1, đang chờ xử lý. */
INSERT INTO appeals (article_id, scoring_attempt_id, appellant_id, status, submitted_at) VALUES
    (1, 1, 1, 'SUBMITTED', '2026-09-13 09:00');   -- 1

INSERT INTO appeal_criteria (appeal_id, criterion_id, reason) VALUES
    (1, 4, N'Bài đăng ngay trong ngày bão đổ bộ, không chậm hơn báo khác.');

INSERT INTO cross_review_batches (batch_name, period_start, period_end, created_by, status, created_at) VALUES
    (N'Chấm chéo tháng 09/2026', '2026-09-01', '2026-09-30', 5, 'OPEN', '2026-09-20 08:00');   -- 1

/* BTV Kinh tế chấm chéo bài 1 của ban Thời sự, chưa chấm. */
INSERT INTO cross_review_tasks (batch_id, article_id, reviewer_id, original_scoring_id, status) VALUES
    (1, 1, 9, 1, 'PENDING');

INSERT INTO reviewer_deviation_indices (editor_id, period, avg_deviation, appeal_count, overturn_rate, workload_count, last_updated) VALUES
    (3, '2026-09-01', 0.00, 1, 0.00, 1, '2026-09-20 00:00');

/* ---------- ROYALTY: phiếu chấm bài 1 ----------
   score 77 -> bậc 70-89 -> tier_rate 80%
   suggested = 5.00 * 80% = 4.00, TKTS nâng lên 5.00 (có lý do)
   final = 5.00 * 100% (bão hoà) * 100% (ORIGINAL) = 5.00
   amount = 5.00 * 234.000 = 1.170.000đ */
INSERT INTO royalty_gradings (scoring_attempt_id, article_id, version_no, period_id, rate_id, category_id,
                              score_used, tier_rate, base_factor, suggested_factor, applied_factor, raise_reason,
                              saturation_rate, nature, nature_rate, final_factor, unit_price, amount,
                              graded_by, graded_at) VALUES
    (1, 1, 1, 1, 1, 3,
        77.00, 80.00, 5.00, 4.00, 5.00, N'Phản ánh kịp thời vấn đề dân sinh sau bão.',
        100.00, 'ORIGINAL', 100.00, 5.00, 234000, 1170000,
        4, '2026-09-16 10:00');   -- 1

/* Ảnh PHOTO hệ số 1.00 -> 234.000đ cho tác giả ảnh. */
INSERT INTO royalty_media_items (grading_id, attachment_id, factor, unit_price, amount) VALUES
    (1, 1, 1.00, 234000, 234000);   -- 1

/* Kỳ còn OPEN nên payment_id = NULL.
   Phần bài 1.170.000đ chia 70/30: 819.000 + 351.000 = 1.170.000 (khớp đúng). */
INSERT INTO royalty_lines (period_id, grading_id, media_item_id, user_id, line_type, share_percent, amount, created_at) VALUES
    (1, 1, NULL, 1, 'ARTICLE', 70.00, 819000, '2026-09-16 10:00'),
    (1, 1, NULL, 2, 'ARTICLE', 30.00, 351000, '2026-09-16 10:00'),
    (1, 1, 1,    2, 'MEDIA',   NULL,  234000, '2026-09-16 10:00');

/* ---------- KPI ---------- */

INSERT INTO kpi_targets (reporter_id, target_year, target_month, min_published, min_avg_score, set_by) VALUES
    (1, 2026, 9, 8, 70.00, 5),
    (2, 2026, 9, 6, 65.00, 5);

INSERT INTO kpi_snapshots (reporter_id, period_year, period_month, actual_articles, actual_avg_score,
                           status, notes, finalized_by, finalized_at) VALUES
    (1, 2026, 8, 7, 74.50, 'FINALIZED', N'Đạt 7/8 bài, điểm TB vượt chỉ tiêu.', 5, '2026-09-01 09:00'),
    (1, 2026, 9, 1, 77.00, 'DRAFT',     NULL,                                   NULL, NULL);

INSERT INTO csv_import_logs (file_name, total_records, success_records, imported_by, imported_at) VALUES
    (N'reader_2026-09-16.csv', 2, 2, 4, '2026-09-17 07:00');   -- 1

INSERT INTO reader_interactions (article_id, record_date, page_views, rating_count, rating_sum, import_log_id) VALUES
    (1, '2026-09-15', 5230, 40, 172, 1),
    (1, '2026-09-16', 2110, 15,  63, 1);

/* ---------- COMMON ---------- */

INSERT INTO notifications (user_id, article_id, message, is_read, created_at) VALUES
    (1, 1, N'Bài "Người dân khu Bắc Hà khốn khổ vì ngập sau bão" đã được xuất bản.', 1, '2026-09-15 09:00'),
    (2, 1, N'Bạn là đồng tác giả của bài vừa được xuất bản.',                        0, '2026-09-15 09:00'),
    (5, 1, N'Có khiếu nại mới về điểm bài của PV Nguyễn Văn An.',                    0, '2026-09-13 09:00'),
    (5, 3, N'Có bài mới chờ phân công biên tập viên.',                               0, '2026-09-27 08:00');

INSERT INTO audit_logs (actor_id, action_type, entity_type, entity_id, old_value, new_value, created_at) VALUES
    (8, 'UPDATE', 'system_parameters', 1, N'{"param_value":"4"}', N'{"param_value":"5"}', '2026-09-01 08:00'),
    (4, 'CREATE', 'royalty_gradings',  1, NULL, N'{"final_factor":5.00,"amount":1170000}', '2026-09-16 10:00');

/* Lịch sử bài: theo vòng đời DRAFT -> (PENDING_CONFIRMATION nếu có đồng tác giả)
   -> SUBMITTED -> ASSIGNED -> PENDING_APPROVAL -> SCORE_PUBLISHED -> PUBLISHED.
   Bài 1 có đồng tác giả: PV Bình xác nhận lúc 11:00 thì bài mới sang SUBMITTED. */
INSERT INTO article_status_logs (article_id, actor_id, action, from_status, to_status, note, created_at) VALUES
    (1, 1, 'CREATE',        NULL,                   'DRAFT',                NULL,              '2026-09-10 08:00'),
    (1, 1, 'SUBMIT',        'DRAFT',                'PENDING_CONFIRMATION', NULL,              '2026-09-10 10:00'),
    (1, 2, 'CONFIRM',       'PENDING_CONFIRMATION', 'SUBMITTED',            NULL,              '2026-09-10 11:00'),
    (1, 5, 'ASSIGN',        'SUBMITTED',            'ASSIGNED',             N'Giao BTV Cường', '2026-09-10 14:00'),
    (1, 3, 'SUBMIT_SCORE',  'ASSIGNED',             'PENDING_APPROVAL',     NULL,              '2026-09-11 09:00'),
    (1, 5, 'APPROVE_SCORE', 'PENDING_APPROVAL',     'SCORE_PUBLISHED',      NULL,              '2026-09-12 08:05'),
    (1, 5, 'PUBLISH',       'SCORE_PUBLISHED',      'PUBLISHED',            NULL,              '2026-09-15 09:00'),
    (2, 2, 'CREATE',        NULL,                   'DRAFT',                NULL,              '2026-09-25 14:00'),
    (3, 1, 'CREATE',        NULL,                   'DRAFT',                NULL,              '2026-09-27 07:30'),
    (3, 1, 'SUBMIT',        'DRAFT',                'SUBMITTED',            NULL,              '2026-09-27 08:00');
GO

PRINT N'Nạp dữ liệu mẫu thành công.';
GO
