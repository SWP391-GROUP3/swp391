-- noinspection SqlNoDataSourceInspectionForFile
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

INSERT INTO genres (code, name, nd_group, base_factor) VALUES
    ('TIN_VAN',   N'Tin vắn',           N'Tin',                    1.00),   -- 1
    ('TIN_SAU',   N'Tin sâu',           N'Tin',                    2.00),   -- 2
    ('PHAN_ANH',  N'Bài phản ánh',      N'Bài viết',               5.00),   -- 3
    ('PHONG_VAN', N'Bài phỏng vấn',     N'Bài viết',               6.00),   -- 4
    ('BINH_LUAN', N'Bình luận',         N'Bình luận, chính luận',  4.00),   -- 5
    ('PHONG_SU',  N'Phóng sự điều tra', N'Phóng sự, điều tra',    10.00);   -- 6

INSERT INTO media_factors (media_kind, factor) VALUES
    ('PHOTO',   1.00),
    ('GRAPHIC', 1.50),
    ('VIDEO',   3.00);

INSERT INTO system_parameters (param_key, param_value, unit, flow, updated_by) VALUES
    ('MAX_AUTHORS_PER_ARTICLE',     N'5',    N'người', 'BF-01', 8),
    ('CO_AUTHOR_CONFIRM_DAYS',      N'3',    N'ngày',  'BF-01', 8),
    ('SHINGLE_SIZE',                N'5',    N'âm tiết', 'BF-01', 8),
    ('DHASH_MAX_BITS',              N'5',    N'bit',   'BF-01', 8),
    ('DUP_WARN_PERCENT',            N'20',   N'%',     'BF-01', 8),
    ('DUP_BLOCK_PERCENT',           N'50',   N'%',     'BF-01', 8),
    ('NATURE_RATE_ORIGINAL',        N'100',  N'%',     'BF-04', 8),
    ('NATURE_RATE_TRANSLATION',     N'70',   N'%',     'BF-04', 8),
    ('NATURE_RATE_COMPILATION',     N'50',   N'%',     'BF-04', 8),
    ('NATURE_RATE_REPOST',          N'30',   N'%',     'BF-04', 8),
    ('MIN_FINAL_FACTOR',            N'1',    NULL,     'BF-04', 8),
    ('ROUNDING_UNIT',               N'1000', N'VND',   'BF-04', 8),
    ('UNIT_PRICE_PERCENT',          N'10',   N'%',     'BF-04', 8),   -- đơn giá 1 hệ số = lương cơ sở x 10%
    ('PAYMENT_DEADLINE_DAYS',       N'30',   N'ngày',  'BF-04', 8),   -- hạn chi = ngày đăng + 30 ngày (UC-46)
    ('PAYMENT_WARN_DAYS',           N'7',    N'ngày',  'BF-04', 8);   -- còn <= 7 ngày tới hạn thì cảnh báo

/* ---------- ROYALTY: danh mục ---------- */

INSERT INTO events (name, start_date, end_date, window_days, k_limit, exempt_score, created_by) VALUES
    (N'Bão số 3 năm 2026', '2026-09-05', '2026-09-20', 7, 10, 90, 4);   -- 1

/* 1 hệ số = 10% lương cơ sở 2.340.000đ. */
INSERT INTO royalty_rates (base_salary, amount, effective_from, created_by) VALUES
    (2340000, 234000, '2026-01-01', 6);   -- 1

INSERT INTO royalty_periods (period_year, period_month, status) VALUES
    (2026, 9, 'OPEN');   -- 1

/* ---------- ARTICLE: bài mẫu đủ 3 trạng thái ---------- */

/* Bài 1: PUBLISHED, thuộc sự kiện bão (xếp thứ 3 <= k_limit nên không giảm),
          có đồng tác giả, đã chấm điểm, đã chấm nhuận bút.
   Bài 2: DRAFT, chưa nộp.
   Bài 3: SUBMITTED, chờ Trưởng ban phân công BTV.
   Bài 4, 5: PUBLISHED, đã duyệt điểm nhưng CHƯA chấm nhuận bút, để demo
          UC-46 (danh sách chờ chấm) và UC-47/48 (chấm bài + ảnh):
     Bài 4: 1 tác giả, Tin sâu, bài DỊCH (70%), điểm 84.50, đăng 22/09
            -> hạn chi 22/10, gần hạn nên có cảnh báo.
     Bài 5: 2 tác giả 60/40, Bài phản ánh, điểm 82.50, đăng 12/10, có 3 ảnh:
            1 ảnh mới (được tính tiền), 1 ảnh nguồn ngoài và 1 ảnh trùng ảnh
            bài 1 đã trả nhuận bút (2 ảnh này không tính tiền, BR-RY-04).
     Bài 6: 1 tác giả, Tin sâu, bài GỐC, điểm 75.00, đăng 08/09
            -> hạn chi 08/10, ĐÃ QUÁ HẠN (nhãn đỏ ở UC-46).
            Hệ số 2.00 x 75 / 100 = 1.50 -> 1.50 x 234.000 = 351.000đ.
            Đăng trong tháng 9 nên phải chấm xong mới chốt được kỳ 9 (UC-50 E1). */
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
        '2026-09-27 07:30', '2026-09-27 08:00', '2026-09-27 08:00', NULL),
    (2, 1, 2, 2, NULL, 5, N'Liên Hợp Quốc cảnh báo nguy cơ thiếu lương thực toàn cầu',
        N'Bản dịch đã biên tập...', 'TRANSLATION', 'PUBLISHED', NULL, NULL,
        '2026-09-17 08:00', '2026-09-22 09:00', '2026-09-17 10:00', '2026-09-22 09:00'),
    (1, 1, 3, 3, NULL, 5, N'Đường dân sinh xuống cấp sau mùa mưa ở xã ven đô',
        N'Nội dung bản đã biên tập...', 'ORIGINAL', 'PUBLISHED', NULL, NULL,
        '2026-10-05 08:00', '2026-10-12 09:00', '2026-10-05 10:00', '2026-10-12 09:00'),
    (1, 1, 2, 2, NULL, 5, N'Hà Nội mở thêm 3 tuyến xe buýt điện từ đầu tháng 9',
        N'Nội dung bản nộp...', 'ORIGINAL', 'PUBLISHED', NULL, NULL,
        '2026-09-01 08:00', '2026-09-08 09:00', '2026-09-01 10:00', '2026-09-08 09:00');

/* Tác giả chính ACCEPTED ngay; đồng tác giả đã xác nhận trong hạn 3 ngày. */
INSERT INTO article_authors (article_id, user_id, share_percent, confirm_status, confirm_deadline, responded_at) VALUES
    (1, 1,  70.00, 'ACCEPTED', NULL,               '2026-09-10 10:00'),
    (1, 2,  30.00, 'ACCEPTED', '2026-09-13 10:00', '2026-09-10 11:00'),
    (2, 2, 100.00, 'ACCEPTED', NULL,               '2026-09-25 14:00'),
    (3, 1, 100.00, 'ACCEPTED', NULL,               '2026-09-27 08:00'),
    (4, 2, 100.00, 'ACCEPTED', NULL,               '2026-09-17 10:00'),
    (5, 1,  60.00, 'ACCEPTED', NULL,               '2026-10-05 10:00'),
    (5, 2,  40.00, 'ACCEPTED', '2026-10-08 10:00', '2026-10-05 15:00'),
    (6, 1, 100.00, 'ACCEPTED', NULL,               '2026-09-01 10:00');

INSERT INTO article_versions (article_id, version_no, kind, title, content, change_type, created_by, created_at) VALUES
    (1, 1, 'SUBMISSION', N'Người dân khu Bắc Hà khốn khổ vì ngập sau bão',
        N'Nội dung bản PV nộp...', 'INITIAL', 1, '2026-09-10 10:00'),           -- 1
    (1, 2, 'EDITED',     N'Người dân khu Bắc Hà khốn khổ vì ngập sau bão',
        N'Nội dung bản đã biên tập...', 'MINOR', 3, '2026-09-12 16:00'),        -- 2
    (3, 1, 'SUBMISSION', N'Giá xăng giảm lần thứ ba liên tiếp',
        N'Nội dung bản nộp...', 'INITIAL', 1, '2026-09-27 08:00'),              -- 3
    (4, 1, 'SUBMISSION', N'Liên Hợp Quốc cảnh báo nguy cơ thiếu lương thực toàn cầu',
        N'Bản dịch PV nộp...', 'INITIAL', 2, '2026-09-17 10:00'),               -- 4
    (5, 1, 'SUBMISSION', N'Đường dân sinh xuống cấp sau mùa mưa ở xã ven đô',
        N'Nội dung bản PV nộp...', 'INITIAL', 1, '2026-10-05 10:00'),           -- 5
    (5, 2, 'EDITED',     N'Đường dân sinh xuống cấp sau mùa mưa ở xã ven đô',
        N'Nội dung bản đã biên tập...', 'MINOR', 3, '2026-10-07 16:00'),        -- 6
    (6, 1, 'SUBMISSION', N'Hà Nội mở thêm 3 tuyến xe buýt điện từ đầu tháng 9',
        N'Nội dung bản nộp...', 'INITIAL', 1, '2026-09-01 10:00');              -- 7

/* Ảnh của PV Bình (đồng tác giả) chụp, không trùng ảnh nào. */
INSERT INTO article_attachments (article_id, version_id, author_id, media_type, media_kind, url,
                                 file_hash, dhash, hash_result, caption) VALUES
    (1, 1, 2, 'IMAGE', 'PHOTO', N'/uploads/2026/09/bac-ha-ngap-1.jpg',
        '3b7e2f9c41d08a65e1c4b92d7f306a8e5c19b04d72e6f3a18c5d90b47e2a6f1c',
        'c3a1e0f07b1d2c4e', 'NEW', N'Nước ngập ngang gối tại ngõ 12');   -- 1

/* Ảnh bài 5, gắn bản nộp (version 5):
   2: ảnh mới của PV Bình -> được tính tiền.
   3: đồ hoạ lấy từ nguồn ngoài -> không tính tiền.
   4: ảnh trùng y hệt ảnh 1 (bài 1, đã trả nhuận bút) -> tái sử dụng, không tính tiền. */
INSERT INTO article_attachments (article_id, version_id, author_id, media_type, media_kind, external_source, url,
                                 file_hash, dhash, hash_result, distance_bits, matched_attachment_id, caption) VALUES
    (5, 5, 2, 'IMAGE', 'PHOTO', NULL, N'/uploads/2026/10/duong-xuong-cap-1.jpg',
        '9d4c1a7e52b0f3866e2d18c5a9f04b7d31e6c82a5f9b07d4e1c3a68f2b5d90e7',
        'a5c3e1f0d2b49786', 'NEW', NULL, NULL, N'Mặt đường bong tróc tại thôn Đông'),       -- 2
    (5, 5, 1, 'IMAGE', 'GRAPHIC', N'Sở Giao thông vận tải', N'/uploads/2026/10/ban-do-tuyen-duong.png',
        '4f7a2c9e1b8d3065a2e7c4f19b0d6e38c5a17f2d9e4b60c8a3f15e7d2c9b04a6',
        NULL, 'NEW', NULL, NULL, N'Bản đồ tuyến đường (nguồn: Sở GTVT)'),                 -- 3
    (5, 5, 1, 'IMAGE', 'PHOTO', NULL, N'/uploads/2026/10/ngap-dung-lai.jpg',
        '3b7e2f9c41d08a65e1c4b92d7f306a8e5c19b04d72e6f3a18c5d90b47e2a6f1c',
        'c3a1e0f07b1d2c4e', 'DUPLICATE', 0, 1, N'Ảnh minh hoạ dùng lại từ bài cũ');        -- 4

INSERT INTO duplication_checks (article_id, version_id, max_overlap_percent, result, explanation, checked_at) VALUES
    (1, 1, 0.00, 'NORMAL', N'Không có đoạn trùng với bài đã đăng.',              '2026-09-10 10:01'),   -- 1
    (3, 3, 8.00, 'NORMAL', N'Trùng một câu trích dẫn số liệu, dưới ngưỡng 20%.', '2026-09-27 08:01');   -- 2

INSERT INTO duplication_matches (check_id, matched_article_id, overlap_percent, passages) VALUES
    (2, 1, 8.00, N'"...theo số liệu của Sở Công Thương..."');

/* ---------- ASSESSMENT ---------- */

INSERT INTO assignments (article_id, editor_id, assigned_by, assigned_at, was_suggested) VALUES
    (1, 3, 5, '2026-09-10 14:00', 1),   -- 1
    (4, 3, 5, '2026-09-17 14:00', 1),   -- 2
    (5, 3, 5, '2026-10-06 09:00', 1),   -- 3
    (6, 3, 5, '2026-09-01 14:00', 1);   -- 4

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
        N'Đề tài tốt, cần thêm ý kiến cơ quan chức năng.', '2026-09-11 08:30', '2026-09-11 09:00'),   -- 1
    -- Bài 4 (Tin sâu, bộ tiêu chí 2): 85*40% + 80*35% + 90*25% = 34 + 28 + 22.5 = 84.50
    (4, 4, 2, 3, 1, 'INITIAL', 2, 2, 84.50, 'ACCEPT',
        N'Bản dịch chính xác, trình bày tốt.', '2026-09-18 08:30', '2026-09-18 09:00'),               -- 2
    -- Bài 5 (Bài phản ánh, bộ tiêu chí 1): 90*30% + 80*20% + 85*30% + 70*20% = 27 + 16 + 25.5 + 14 = 82.50
    (5, 5, 3, 3, 1, 'INITIAL', 1, 3, 82.50, 'ACCEPT',
        N'Phát hiện vấn đề dân sinh, có số liệu.', '2026-10-07 08:30', '2026-10-07 09:00'),           -- 3
    -- Bài 6 (Tin sâu, bộ tiêu chí 2): 80*40% + 70*35% + 74*25% = 32 + 24.5 + 18.5 = 75.00
    (6, 7, 4, 3, 1, 'INITIAL', 2, 2, 75.00, 'ACCEPT',
        N'Thông tin đầy đủ, cần cập nhật số liệu hành khách.', '2026-09-02 08:30', '2026-09-02 09:00'); -- 4

INSERT INTO scoring_attempt_details (scoring_attempt_id, criterion_id, score, comment) VALUES
    (1, 1, 80.00, NULL),
    (1, 2, 70.00, NULL),
    (1, 3, 90.00, N'Viết rõ ràng, có nhân chứng.'),
    (1, 4, 60.00, N'Đăng chậm hơn báo khác 1 ngày.'),
    (2, 5, 85.00, NULL),
    (2, 6, 80.00, NULL),
    (2, 7, 90.00, NULL),
    (3, 1, 90.00, NULL),
    (3, 2, 80.00, NULL),
    (3, 3, 85.00, NULL),
    (3, 4, 70.00, NULL),
    (4, 5, 80.00, NULL),
    (4, 6, 70.00, NULL),
    (4, 7, 74.00, NULL);

INSERT INTO score_approvals (scoring_attempt_id, decision, decided_by, decided_at, announced_at) VALUES
    (1, 'APPROVED', 5, '2026-09-12 08:00', '2026-09-12 08:05'),
    (2, 'APPROVED', 5, '2026-09-19 08:00', '2026-09-19 08:05'),
    (3, 'APPROVED', 5, '2026-10-08 08:00', '2026-10-08 08:05'),
    (4, 'APPROVED', 5, '2026-09-03 08:00', '2026-09-03 08:05');

/* ---------- APPEAL ---------- */

INSERT INTO appeal_rule_configs (appeal_window_days, max_appeals_per_article, variance_threshold, effective_from, updated_by) VALUES
    (7, 1, 5.00, '2026-01-01', 8);

/* Chưa tạo sẵn khiếu nại để có thể demo UC-35 bằng bài 1. */

INSERT INTO cross_review_batches (batch_name, period_start, period_end, created_by, status, created_at) VALUES
    (N'Chấm chéo tháng 09/2026', '2026-09-01', '2026-09-30', 5, 'OPEN', '2026-09-20 08:00');   -- 1

/* BTV Kinh tế chấm chéo bài 1 của ban Thời sự, chưa chấm. */
INSERT INTO cross_review_tasks (batch_id, article_id, reviewer_id, original_scoring_id, status) VALUES
    (1, 1, 9, 1, 'PENDING');

INSERT INTO reviewer_deviation_indices (editor_id, period, avg_deviation, appeal_count, overturn_rate, workload_count, last_updated) VALUES
    (3, '2026-09-01', 0.00, 0, 0.00, 1, '2026-09-20 00:00');

/* ---------- ROYALTY: phiếu chấm bài 1 ----------
   Công thức BR-RY-01..03 (nhóm chốt 13/10/2026, bỏ chia bậc điểm):
   tier_rate = điểm = 77.00
   suggested = applied = 5.00 (Bài phản ánh) * 77 / 100 = 3.85
   final = 3.85 * 100% (bão hoà) * 100% (ORIGINAL) = 3.85
   amount = 3.85 * 234.000 = 900.900 -> làm tròn xuống 1.000đ = 900.000đ */
INSERT INTO royalty_gradings (scoring_attempt_id, article_id, version_no, period_id, rate_id, category_id,
                              score_used, tier_rate, base_factor, suggested_factor, applied_factor, raise_reason,
                              saturation_rate, nature, nature_rate, final_factor, unit_price, amount,
                              graded_by, graded_at) VALUES
    (1, 1, 1, 1, 1, 3,
        77.00, 77.00, 5.00, 3.85, 3.85, NULL,
        100.00, 'ORIGINAL', 100.00, 3.85, 234000, 900000,
        4, '2026-09-16 10:00');   -- 1

/* Ảnh PHOTO hệ số 1.00 -> 234.000đ cho tác giả ảnh. */
INSERT INTO royalty_media_items (grading_id, attachment_id, factor, unit_price, amount) VALUES
    (1, 1, 1.00, 234000, 234000);   -- 1

/* Kỳ còn OPEN nên payment_id = NULL.
   Phần bài 900.000đ chia 70/30 (GBR-10): đồng tác giả Bình 30% = 270.000,
   tác giả chính An nhận phần còn lại 630.000 -> 630.000 + 270.000 = 900.000 (khớp đúng). */
INSERT INTO royalty_lines (period_id, grading_id, media_item_id, user_id, line_type, share_percent, amount, created_at) VALUES
    (1, 1, NULL, 1, 'ARTICLE', 70.00, 630000, '2026-09-16 10:00'),
    (1, 1, NULL, 2, 'ARTICLE', 30.00, 270000, '2026-09-16 10:00'),
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
    (5, 3, N'Có bài mới chờ phân công biên tập viên.',                               0, '2026-09-27 08:00');

INSERT INTO audit_logs (actor_id, action_type, entity_type, entity_id, old_value, new_value, created_at) VALUES
    (8, 'UPDATE', 'system_parameters', 1, N'{"param_value":"4"}', N'{"param_value":"5"}', '2026-09-01 08:00'),
    (4, 'CREATE', 'royalty_gradings',  1, NULL, N'{"final_factor":3.85,"amount":900000}', '2026-09-16 10:00');

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
    (3, 1, 'SUBMIT',        'DRAFT',                'SUBMITTED',            NULL,              '2026-09-27 08:00'),
    (4, 2, 'CREATE',        NULL,                   'DRAFT',                NULL,              '2026-09-17 08:00'),
    (4, 2, 'SUBMIT',        'DRAFT',                'SUBMITTED',            NULL,              '2026-09-17 10:00'),
    (4, 5, 'ASSIGN',        'SUBMITTED',            'ASSIGNED',             N'Giao BTV Cường', '2026-09-17 14:00'),
    (4, 3, 'SUBMIT_SCORE',  'ASSIGNED',             'PENDING_APPROVAL',     NULL,              '2026-09-18 09:00'),
    (4, 5, 'APPROVE_SCORE', 'PENDING_APPROVAL',     'SCORE_PUBLISHED',      NULL,              '2026-09-19 08:05'),
    (4, 5, 'PUBLISH',       'SCORE_PUBLISHED',      'PUBLISHED',            NULL,              '2026-09-22 09:00'),
    (5, 1, 'CREATE',        NULL,                   'DRAFT',                NULL,              '2026-10-05 08:00'),
    (5, 1, 'SUBMIT',        'DRAFT',                'PENDING_CONFIRMATION', NULL,              '2026-10-05 10:00'),
    (5, 2, 'CONFIRM',       'PENDING_CONFIRMATION', 'SUBMITTED',            NULL,              '2026-10-05 15:00'),
    (5, 5, 'ASSIGN',        'SUBMITTED',            'ASSIGNED',             N'Giao BTV Cường', '2026-10-06 09:00'),
    (5, 3, 'SUBMIT_SCORE',  'ASSIGNED',             'PENDING_APPROVAL',     NULL,              '2026-10-07 09:00'),
    (5, 5, 'APPROVE_SCORE', 'PENDING_APPROVAL',     'SCORE_PUBLISHED',      NULL,              '2026-10-08 08:05'),
    (5, 5, 'PUBLISH',       'SCORE_PUBLISHED',      'PUBLISHED',            NULL,              '2026-10-12 09:00'),
    (6, 1, 'CREATE',        NULL,                   'DRAFT',                NULL,              '2026-09-01 08:00'),
    (6, 1, 'SUBMIT',        'DRAFT',                'SUBMITTED',            NULL,              '2026-09-01 10:00'),
    (6, 5, 'ASSIGN',        'SUBMITTED',            'ASSIGNED',             N'Giao BTV Cường', '2026-09-01 14:00'),
    (6, 3, 'SUBMIT_SCORE',  'ASSIGNED',             'PENDING_APPROVAL',     NULL,              '2026-09-02 09:00'),
    (6, 5, 'APPROVE_SCORE', 'PENDING_APPROVAL',     'SCORE_PUBLISHED',      NULL,              '2026-09-03 08:05'),
    (6, 5, 'PUBLISH',       'SCORE_PUBLISHED',      'PUBLISHED',            NULL,              '2026-09-08 09:00');
GO

/* ---------- BF-03: ba bai mau de gui khieu nai (UC-35) ----------
   Giu nguyen ID cua du lieu mau phia tren; ID bai moi lay bang SCOPE_IDENTITY().
   Diem duoc cong bo theo luc nap du lieu de con han khieu nai.
   Khong tao san appeal cho ba bai nay. Bo qua bai da co cung tieu de.
   Toan bo data.sql chi chay tren DB moi; DB da co du lieu thi dung seed rieng:
   database/seeds/BF03_sample_appealable_articles.sql. */
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id
                   WHERE u.id=1 AND u.username='an.nv' AND u.section_id=1
                     AND u.status='ACTIVE' AND r.code='REPORTER')
        THROW 51300, 'Expected active reporter an.nv (id=1, section=1) was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id
                   WHERE u.id=3 AND u.section_id=1 AND u.status='ACTIVE' AND r.code='EDITOR')
        THROW 51301, 'Expected active editor (id=3, section=1) was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM users u JOIN roles r ON r.id=u.role_id
                   WHERE u.id=5 AND u.section_id=1 AND u.status='ACTIVE' AND r.code='HEAD_OF_DEPARTMENT')
        THROW 51302, 'Expected active head (id=5, section=1) was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM criteria_set_versions WHERE id=1 AND category_id=3 AND status='ACTIVE')
       OR (SELECT COALESCE(SUM(weight),0) FROM criteria WHERE criteria_set_version_id=1) <> 100
        THROW 51303, 'The expected active criteria set with total weight 100 was not found.', 1;
    IF NOT EXISTS (SELECT 1 FROM appeal_rule_configs
                   WHERE is_active=1 AND effective_from<=CAST(SYSDATETIME() AS DATE))
        THROW 51304, 'No effective appeal rule configuration was found.', 1;

    DECLARE @samples TABLE(sequence_no INT PRIMARY KEY, title NVARCHAR(300),
                           content NVARCHAR(MAX), base_score DECIMAL(5,2));
    INSERT INTO @samples VALUES
    (1, N'[BF03-DEMO-01] Người trẻ chung tay làm sạch kênh sau mưa lớn',
        N'Sáng cuối tuần, các nhóm tình nguyện phối hợp với người dân thu gom rác tại tuyến kênh trong khu dân cư.
Bài viết ghi nhận hoạt động tại hiện trường, phỏng vấn người dân và đại diện nhóm tình nguyện.
Tác giả đề nghị xem xét lại tiêu chí tính phát hiện và độ khó tác nghiệp vì việc thu thập thông tin diễn ra trong điều kiện mưa lớn.', 60),
    (2, N'[BF03-DEMO-02] Chợ dân sinh ứng dụng thanh toán không tiền mặt',
        N'Nhiều tiểu thương tại chợ dân sinh bắt đầu sử dụng mã QR để phục vụ người mua.
Phóng viên khảo sát các quầy hàng, đối chiếu phản hồi của người mua với ý kiến ban quản lý chợ.
Tác giả có thể khiếu nại tiêu chí chất lượng trình bày vì bài đã bổ sung số liệu khảo sát và dẫn chứng cụ thể.', 65),
    (3, N'[BF03-DEMO-03] Lớp học miễn phí giúp người cao tuổi sử dụng dịch vụ công',
        N'Một lớp học tại nhà văn hóa hướng dẫn người cao tuổi tra cứu và nộp hồ sơ dịch vụ công trực tuyến.
Bài viết có phỏng vấn học viên, người hướng dẫn và ghi nhận các khó khăn trong quá trình thực hành.
Tác giả có thể đề nghị xem xét tiêu chí tính thời sự và giá trị thông tin của bài.', 70);

    DECLARE @sequence INT=1, @article_id INT, @version_id INT, @assignment_id INT, @attempt_id INT;
    DECLARE @title NVARCHAR(300), @content NVARCHAR(MAX), @base_score DECIMAL(5,2);
    DECLARE @announced_at DATETIME2, @total_score DECIMAL(5,2), @created_count INT=0;

    WHILE @sequence<=3
    BEGIN
        SELECT @title=title, @content=content, @base_score=base_score
        FROM @samples WHERE sequence_no=@sequence;
        IF NOT EXISTS (SELECT 1 FROM articles WITH (UPDLOCK, HOLDLOCK) WHERE title=@title)
        BEGIN
            SET @announced_at=DATEADD(MINUTE,-15*@sequence,SYSDATETIME());
            INSERT INTO articles(author_id,section_id,declared_category_id,category_id,title,content,
                                 status,submitted_at,created_at,updated_at)
            VALUES(1,1,3,3,@title,@content,'SCORE_PUBLISHED',
                   DATEADD(HOUR,-3,@announced_at),DATEADD(HOUR,-4,@announced_at),@announced_at);
            SET @article_id=CAST(SCOPE_IDENTITY() AS INT);
            INSERT INTO article_authors(article_id,user_id,share_percent,confirm_status,responded_at)
            VALUES(@article_id,1,100,'ACCEPTED',DATEADD(HOUR,-4,@announced_at));
            INSERT INTO article_versions(article_id,version_no,kind,title,content,created_by,created_at)
            VALUES(@article_id,1,'SUBMISSION',@title,@content,1,DATEADD(HOUR,-3,@announced_at));
            SET @version_id=CAST(SCOPE_IDENTITY() AS INT);
            INSERT INTO assignments(article_id,editor_id,assigned_by,assigned_at)
            VALUES(@article_id,3,5,DATEADD(HOUR,-2,@announced_at));
            SET @assignment_id=CAST(SCOPE_IDENTITY() AS INT);
            INSERT INTO scoring_attempts(article_id,version_id,assignment_id,reviewer_id,attempt_no,
                                         attempt_type,criteria_set_version_id,category_id,total_score,
                                         decision,opened_at,submitted_at)
            VALUES(@article_id,@version_id,@assignment_id,3,1,'INITIAL',1,3,NULL,NULL,
                   DATEADD(HOUR,-2,@announced_at),NULL);
            SET @attempt_id=CAST(SCOPE_IDENTITY() AS INT);
            INSERT INTO scoring_attempt_details(scoring_attempt_id,criterion_id,score,comment)
            SELECT @attempt_id,id,@base_score+5*sort_order,
                   CASE sort_order
                     WHEN 1 THEN N'Bài có góc tiếp cận riêng; cần làm rõ hơn phát hiện mới.'
                     WHEN 2 THEN N'Có ghi nhận hiện trường; cần bổ sung thông tin về điều kiện tác nghiệp.'
                     WHEN 3 THEN N'Bố cục rõ ràng, dẫn chứng phù hợp; có thể làm gọn một số đoạn.'
                     ELSE N'Thông tin có giá trị thời sự; cần nêu rõ thời điểm khảo sát.'
                   END
            FROM criteria WHERE criteria_set_version_id=1;
            SELECT @total_score=CAST(ROUND(SUM(d.score*c.weight/100),2) AS DECIMAL(5,2))
            FROM scoring_attempt_details d JOIN criteria c ON c.id=d.criterion_id
            WHERE d.scoring_attempt_id=@attempt_id;
            UPDATE scoring_attempts SET total_score=@total_score,decision='ACCEPT',
                                        submitted_at=DATEADD(HOUR,-1,@announced_at)
            WHERE id=@attempt_id;
            INSERT INTO score_approvals(scoring_attempt_id,decision,decided_by,decided_at,announced_at)
            VALUES(@attempt_id,'APPROVED',5,DATEADD(MINUTE,-5,@announced_at),@announced_at);
            INSERT INTO article_status_logs(article_id,actor_id,action,from_status,to_status,note,created_at)
            VALUES
            (@article_id,1,'CREATE',NULL,'DRAFT',N'Bài mẫu BF-03 để thử gửi khiếu nại.',DATEADD(HOUR,-4,@announced_at)),
            (@article_id,1,'SUBMIT','DRAFT','SUBMITTED',NULL,DATEADD(HOUR,-3,@announced_at)),
            (@article_id,5,'ASSIGN','SUBMITTED','ASSIGNED',NULL,DATEADD(HOUR,-2,@announced_at)),
            (@article_id,3,'SUBMIT_SCORE','ASSIGNED','PENDING_APPROVAL',NULL,DATEADD(HOUR,-1,@announced_at)),
            (@article_id,5,'APPROVE_SCORE','PENDING_APPROVAL','SCORE_PUBLISHED',NULL,@announced_at);
            INSERT INTO notifications(user_id,article_id,message,created_at)
            VALUES(1,@article_id,N'Bài mẫu đã được công bố điểm. Bạn có thể gửi khiếu nại trong thời hạn cấu hình.',@announced_at);
            INSERT INTO audit_logs(actor_id,action_type,entity_type,entity_id,old_value,new_value)
            VALUES(NULL,'CREATE','articles',@article_id,NULL,
                   (SELECT N'BF03 sample seed' AS source,@title AS title,@attempt_id AS scoringAttemptId,
                           @total_score AS totalScore,'SCORE_PUBLISHED' AS status FOR JSON PATH,WITHOUT_ARRAY_WRAPPER));
            SET @created_count=@created_count+1;
        END;
        SET @sequence=@sequence+1;
    END;
    COMMIT TRANSACTION;
    SELECT @created_count AS newly_created_articles;
    SELECT a.id,a.title,s.total_score,p.announced_at,
           DATEADD(DAY,cfg.appeal_window_days,p.announced_at) AS appeal_deadline,
           CASE WHEN EXISTS(SELECT 1 FROM appeals x WHERE x.article_id=a.id) THEN 1 ELSE 0 END AS already_appealed
    FROM articles a JOIN scoring_attempts s ON s.article_id=a.id
    JOIN score_approvals p ON p.scoring_attempt_id=s.id
    CROSS APPLY(SELECT TOP(1) appeal_window_days FROM appeal_rule_configs
                WHERE is_active=1 AND effective_from<=CAST(SYSDATETIME() AS DATE)
                ORDER BY effective_from DESC) cfg
    WHERE a.title IN(SELECT title FROM @samples)
    ORDER BY a.id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT>0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

PRINT N'Nạp dữ liệu mẫu thành công.';
GO
