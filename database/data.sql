/* ==========================================================================
   SWP391 - Dữ liệu mẫu
   Chạy SAU schema.sql. Chỉ dùng để dev và demo.

   LƯU Ý mật khẩu: cột password_hash phải chứa BCrypt hash, không phải plain text.
   Chuỗi bên dưới là PLACEHOLDER. Người phụ trách auth thay bằng hash thật
   (BCryptPasswordEncoder.encode("123456")) khi làm chức năng đăng nhập.

   schema.sql vừa tạo lại bảng nên IDENTITY bắt đầu từ 1: các id tham chiếu
   bên dưới (role_id = 1, author_id = 1, ...) dựa vào thứ tự INSERT.
   ========================================================================== */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

USE swp391_kpi;
GO

/* ---------- Nền tảng ---------- */

INSERT INTO roles (code, name) VALUES
    ('PHONG_VIEN',      N'Phóng viên'),          -- 1
    ('BIEN_TAP_VIEN',   N'Biên tập viên'),       -- 2
    ('TRUONG_BAN',      N'Trưởng ban'),          -- 3
    ('THU_KY_TOA_SOAN', N'Thư ký toà soạn'),     -- 4
    ('KE_TOAN',         N'Kế toán'),             -- 5
    ('QUAN_TRI',        N'Quản trị hệ thống');   -- 6

INSERT INTO sections (code, name) VALUES
    ('THOISU',  N'Ban Thời sự'),             -- 1
    ('KINHTE',  N'Ban Kinh tế'),             -- 2
    ('VANHOA',  N'Ban Văn hoá - Xã hội'),    -- 3
    ('THETHAO', N'Ban Thể thao');            -- 4

/* PV / BTV / Trưởng ban có ban. TKTS, Kế toán, Admin: section_id = NULL. */
INSERT INTO users (username, email, password_hash, full_name, role_id, section_id) VALUES
    ('an.nv',    'an.nv@toasoan.vn',    'REPLACE_WITH_BCRYPT_HASH', N'Nguyễn Văn An',  1, 1),     -- 1 PV Thời sự
    ('binh.tt',  'binh.tt@toasoan.vn',  'REPLACE_WITH_BCRYPT_HASH', N'Trần Thị Bình',  1, 1),     -- 2 PV Thời sự
    ('cuong.lh', 'cuong.lh@toasoan.vn', 'REPLACE_WITH_BCRYPT_HASH', N'Lê Hoàng Cường', 2, 1),     -- 3 BTV Thời sự
    ('dung.pt',  'dung.pt@toasoan.vn',  'REPLACE_WITH_BCRYPT_HASH', N'Phạm Thu Dung',  4, NULL),  -- 4 TKTS
    ('duc.vm',   'duc.vm@toasoan.vn',   'REPLACE_WITH_BCRYPT_HASH', N'Vũ Minh Đức',    3, 1),     -- 5 Trưởng ban Thời sự
    ('chi.hl',   'chi.hl@toasoan.vn',   'REPLACE_WITH_BCRYPT_HASH', N'Hoàng Lan Chi',  5, NULL),  -- 6 Kế toán
    ('hung.dq',  'hung.dq@toasoan.vn',  'REPLACE_WITH_BCRYPT_HASH', N'Đỗ Quốc Hưng',   1, 2),     -- 7 PV Kinh tế
    ('admin',    'admin@toasoan.vn',    'REPLACE_WITH_BCRYPT_HASH', N'Quản trị viên',  6, NULL);  -- 8 Admin

INSERT INTO genres (code, name, min_factor, max_factor) VALUES
    ('TIN_VAN',   N'Tin vắn',           1.00,  2.00),   -- 1
    ('TIN_SAU',   N'Tin sâu',           2.00,  4.00),   -- 2
    ('PHAN_ANH',  N'Bài phản ánh',      4.00,  7.00),   -- 3
    ('PHONG_VAN', N'Bài phỏng vấn',     5.00,  8.00),   -- 4
    ('BINH_LUAN', N'Bình luận',         3.00,  6.00),   -- 5
    ('PHONG_SU',  N'Phóng sự điều tra', 8.00, 15.00);   -- 6

INSERT INTO media_factors (media_type, min_factor, max_factor) VALUES
    ('IMAGE', 0.50, 2.00),
    ('VIDEO', 1.00, 5.00);

/* ---------- Chất lượng: tổng trọng số = 100 ---------- */

INSERT INTO quality_criteria (name, weight_percent) VALUES
    (N'Tính phát hiện, độc quyền',  30.00),   -- 1
    (N'Độ khó khi tác nghiệp',      20.00),   -- 2
    (N'Chất lượng viết, trình bày', 30.00),   -- 3
    (N'Tính thời sự',               20.00);   -- 4

/* ---------- Nhuận bút: đơn giá và kỳ ---------- */

INSERT INTO unit_prices (amount, effective_from, created_by) VALUES
    (150000, '2026-01-01', 6);

INSERT INTO royalty_periods (period_year, period_month, status) VALUES
    (2026, 9, 'OPEN');   -- 1

/* ---------- Bài mẫu: đủ 3 trạng thái để dev màn hình ---------- */

/* Bài 1: đã PUBLISHED, có đồng tác giả, đã chấm chất lượng và chấm nhuận bút.
   Bài 2: DRAFT, chưa nộp.
   Bài 3: SUBMITTED, chờ Trưởng ban phân công BTV. */
INSERT INTO articles (author_id, section_id, genre_id, editor_id, approved_by, title, content,
                      status, created_at, updated_at, submitted_at, published_at) VALUES
    (1, 1, 3, 3, 5, N'Người dân khu Bắc Hà khốn khổ vì ngập sau mưa lớn',
        N'Nội dung bản đã biên tập...', 'PUBLISHED',
        '2026-09-10 08:00', '2026-09-15 09:00', '2026-09-10 10:00', '2026-09-15 09:00'),
    (2, 1, 1, NULL, NULL, N'Khai mạc hội chợ hàng Việt 2026',
        N'Bản nháp...', 'DRAFT',
        '2026-09-25 14:00', '2026-09-25 14:00', NULL, NULL),
    (1, 1, 2, NULL, NULL, N'Giá xăng giảm lần thứ ba liên tiếp',
        N'Nội dung bản nộp...', 'SUBMITTED',
        '2026-09-27 07:30', '2026-09-27 08:00', '2026-09-27 08:00', NULL);

INSERT INTO article_authors (article_id, user_id, share_percent) VALUES
    (1, 1, 70.00),
    (1, 2, 30.00),
    (2, 2, 100.00),
    (3, 1, 100.00);

INSERT INTO article_versions (article_id, version_no, kind, title, content, created_by, created_at) VALUES
    (1, 1, 'SUBMISSION', N'Người dân khu Bắc Hà khốn khổ vì ngập sau mưa lớn',
        N'Nội dung bản PV nộp...', 1, '2026-09-10 10:00'),                 -- 1
    (1, 2, 'EDITED',     N'Người dân khu Bắc Hà khốn khổ vì ngập sau mưa lớn',
        N'Nội dung bản đã biên tập...', 3, '2026-09-12 16:00'),            -- 2
    (3, 1, 'SUBMISSION', N'Giá xăng giảm lần thứ ba liên tiếp',
        N'Nội dung bản nộp...', 1, '2026-09-27 08:00');                    -- 3

INSERT INTO article_attachments (article_id, version_id, author_id, media_type, url, caption) VALUES
    (1, 1, 2, 'IMAGE', N'/uploads/2026/09/bac-ha-ngap-1.jpg', N'Nước ngập ngang gối tại ngõ 12');

INSERT INTO article_status_logs (article_id, actor_id, action, from_status, to_status, note, created_at) VALUES
    (1, 1, 'CREATE',        NULL,               'DRAFT',            NULL, '2026-09-10 08:00'),
    (1, 1, 'SUBMIT',        'DRAFT',            'SUBMITTED',        NULL, '2026-09-10 10:00'),
    (1, 5, 'ASSIGN',        'SUBMITTED',        'ASSIGNED',         N'Giao BTV Cường', '2026-09-10 14:00'),
    (1, 3, 'START_EDIT',    'ASSIGNED',         'EDITING',          NULL, '2026-09-11 08:30'),
    (1, 3, 'SEND_APPROVAL', 'EDITING',          'PENDING_APPROVAL', NULL, '2026-09-12 16:00'),
    (1, 5, 'PUBLISH',       'PENDING_APPROVAL', 'PUBLISHED',        NULL, '2026-09-15 09:00'),
    (2, 2, 'CREATE',        NULL,               'DRAFT',            NULL, '2026-09-25 14:00'),
    (3, 1, 'CREATE',        NULL,               'DRAFT',            NULL, '2026-09-27 07:30'),
    (3, 1, 'SUBMIT',        'DRAFT',            'SUBMITTED',        NULL, '2026-09-27 08:00');

/* Phiếu chấm bài 1 trên bản nộp (version 1):
   8*30% + 7*20% + 9*30% + 6*20% = 2.4 + 1.4 + 2.7 + 1.2 = 7.70 */
INSERT INTO quality_reviews (article_id, version_id, editor_id, total_score, comment, created_at) VALUES
    (1, 1, 3, 7.70, N'Đề tài tốt, cần thêm ý kiến cơ quan chức năng.', '2026-09-11 09:00');

INSERT INTO quality_review_items (review_id, criterion_id, weight_percent, score) VALUES
    (1, 1, 30.00, 8.00),
    (1, 2, 20.00, 7.00),
    (1, 3, 30.00, 9.00),
    (1, 4, 20.00, 6.00);

/* Kỳ 09/2026 còn OPEN nên unit_price và amount chưa điền. */
INSERT INTO royalty_gradings (article_id, period_id, graded_by, suggested_factor, factor, graded_at) VALUES
    (1, 1, 4, 5.50, 6.00, '2026-09-16 10:00');

INSERT INTO royalty_media_items (grading_id, attachment_id, factor) VALUES
    (1, 1, 1.00);

/* ---------- KPI tháng 09/2026 do Trưởng ban Thời sự đặt ---------- */

INSERT INTO kpi_targets (reporter_id, target_year, target_month, min_published, min_avg_score, set_by) VALUES
    (1, 2026, 9, 8, 7.00, 5),
    (2, 2026, 9, 6, 6.50, 5);

INSERT INTO notifications (user_id, article_id, message, is_read, created_at) VALUES
    (1, 1, N'Bài "Người dân khu Bắc Hà khốn khổ vì ngập sau mưa lớn" đã được xuất bản.', 1, '2026-09-15 09:00'),
    (2, 1, N'Bạn là đồng tác giả của bài vừa được xuất bản.',                         0, '2026-09-15 09:00'),
    (5, 3, N'Có bài mới chờ phân công biên tập viên.',                                 0, '2026-09-27 08:00');
GO

PRINT N'Nạp dữ liệu mẫu thành công.';
GO
