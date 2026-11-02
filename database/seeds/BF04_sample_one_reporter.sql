-- noinspection SqlNoDataSourceInspectionForFile
/* ==========================================================================
   BF-04 - Dữ liệu mẫu: 1 phóng viên có 4 bài chờ chấm nhuận bút (chỉ dùng cho dev)
   Chạy SAU schema.sql + data.sql + BF04_sample_royalty.sql, hoặc thẳng trên DB đang có dữ liệu.
   sqlcmd: sqlcmd -S localhost -U sa -P <mk> -d swp391_kpi -C -f 65001 -i database/seeds/BF04_sample_one_reporter.sql

   PV Đỗ Quốc Hưng (hung.dq, ban Kinh tế). BF04_sample_royalty.sql đã cho Hưng
   2 bài chờ chấm; file này thêm 2 bài nữa -> Hưng có 4 bài chờ chấm (UC-46, 47, 48):
     (có sẵn) Tổng hợp ý kiến doanh nghiệp về dự thảo luật thuế
              Phản ánh (5.00), TỔNG HỢP 50%, 80.00 -> 4.00 x 50% = 2.00 -> 468.000đ, ĐÃ QUÁ HẠN
     (có sẵn) Bloomberg: châu Á dẫn đầu tăng trưởng thương mại điện tử
              Phản ánh (5.00), DỊCH 70%, 74.50 -> 3.73 x 70% = 2.61 -> 610.000đ
              + 1 ảnh mới 234.000đ, 1 ảnh tái sử dụng 0đ
     (mới)    E  Tiểu thương chợ Long Biên điêu đứng vì giá thuê sạp tăng
              Phản ánh (5.00), gốc, 83.00 -> 4.15 -> 971.100 -> 971.000đ
              Hưng 70% / Bình 30%: Bình FLOOR(291.300) = 291.000, Hưng nhận phần còn lại 680.000
              Đăng 05/10 -> hạn chi 04/11: SẮP TỚI HẠN (cảnh báo vàng).
     (mới)    F  Làng gốm Bát Tràng chuyển mình đón khách du lịch quốc tế
              Phản ánh (5.00), gốc, 74.00 -> 3.70 -> 865.800 -> 865.000đ
              + ảnh PHOTO 1.00 = 234.000đ + VIDEO 3.00 = 702.000đ (cả 2 của Hưng)
   Đơn giá 234.000đ. Thư ký chấm hết 4 bài: Hưng nhận 3.793.000đ, Bình thêm 291.000đ.

   Chạy lại: bài nào đã có (theo tiêu đề) thì bỏ qua, không tạo trùng.
   ========================================================================== */

SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
GO

USE swp391_kpi;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;

    /* ---------- 0. Người dùng, bộ tiêu chí cần có (lấy theo username, không đoán id) ---------- */
    DECLARE @hung INT  = (SELECT id FROM users WHERE username = 'hung.dq');    -- PV Kinh tế
    DECLARE @binh INT  = (SELECT id FROM users WHERE username = 'binh.tt');    -- PV Thời sự, đồng tác giả bài E
    DECLARE @giang INT = (SELECT id FROM users WHERE username = 'giang.nt');   -- BTV Kinh tế
    DECLARE @duc INT   = (SELECT id FROM users WHERE username = 'duc.vm');     -- Trưởng ban
    IF @hung IS NULL OR @binh IS NULL OR @giang IS NULL OR @duc IS NULL
        THROW 51410, N'Thiếu tài khoản mẫu của data.sql (hung.dq, binh.tt, giang.nt, duc.vm).', 1;

    -- Cả 2 bài là Bài phản ánh (thể loại 3): 4 tiêu chí 30/20/30/20
    DECLARE @setId INT = (SELECT id FROM criteria_set_versions WHERE category_id = 3 AND status = 'ACTIVE');
    IF @setId IS NULL
        THROW 51411, N'Thể loại Bài phản ánh (3) phải có bộ tiêu chí ACTIVE.', 1;

    DECLARE @hungSection INT = (SELECT section_id FROM users WHERE id = @hung);
    DECLARE @articleId INT, @versionId INT, @assignmentId INT, @attemptId INT;

    /* ---------- E. Bài gốc, đồng tác giả Hưng 70% / Bình 30% ---------- */
    IF NOT EXISTS (SELECT 1 FROM articles WHERE title = N'Tiểu thương chợ Long Biên điêu đứng vì giá thuê sạp tăng')
    BEGIN
        INSERT INTO articles (author_id, section_id, declared_category_id, category_id, approved_by, title, content,
                              declared_nature, status, created_at, updated_at, submitted_at, published_at)
        VALUES (@hung, @hungSection, 3, 3, @duc, N'Tiểu thương chợ Long Biên điêu đứng vì giá thuê sạp tăng',
                N'Giá thuê sạp tăng gần gấp đôi sau đợt đấu thầu mới khiến nhiều tiểu thương phải trả lại sạp...',
                'ORIGINAL', 'PUBLISHED', '2026-10-01 08:00', '2026-10-05 09:00', '2026-10-01 10:00', '2026-10-05 09:00');
        SET @articleId = CAST(SCOPE_IDENTITY() AS INT);

        -- Tác giả chính ACCEPTED ngay; PV Bình xác nhận trong hạn 3 ngày
        INSERT INTO article_authors (article_id, user_id, share_percent, confirm_status, confirm_deadline, responded_at)
        VALUES (@articleId, @hung, 70.00, 'ACCEPTED', NULL,               '2026-10-01 10:00'),
               (@articleId, @binh, 30.00, 'ACCEPTED', '2026-10-04 10:00', '2026-10-01 15:00');

        INSERT INTO article_versions (article_id, version_no, kind, title, content, change_type, created_by, created_at)
        VALUES (@articleId, 1, 'SUBMISSION', N'Tiểu thương chợ Long Biên điêu đứng vì giá thuê sạp tăng',
                N'Giá thuê sạp tăng gần gấp đôi sau đợt đấu thầu mới khiến nhiều tiểu thương phải trả lại sạp...',
                'INITIAL', @hung, '2026-10-01 10:00');
        SET @versionId = CAST(SCOPE_IDENTITY() AS INT);

        INSERT INTO assignments (article_id, editor_id, assigned_by, assigned_at, was_suggested)
        VALUES (@articleId, @giang, @duc, '2026-10-02 09:00', 1);
        SET @assignmentId = CAST(SCOPE_IDENTITY() AS INT);

        -- 80*30% + 80*20% + 90*30% + 80*20% = 24 + 16 + 27 + 16 = 83.00
        INSERT INTO scoring_attempts (article_id, version_id, assignment_id, reviewer_id, attempt_no, attempt_type,
                                      criteria_set_version_id, category_id, total_score, decision, decision_note,
                                      opened_at, submitted_at)
        VALUES (@articleId, @versionId, @assignmentId, @giang, 1, 'INITIAL', @setId, 3, 83.00, 'ACCEPT',
                N'Có khảo sát tiểu thương thực tế, trình bày mạch lạc.', '2026-10-03 08:30', '2026-10-03 09:00');
        SET @attemptId = CAST(SCOPE_IDENTITY() AS INT);

        INSERT INTO scoring_attempt_details (scoring_attempt_id, criterion_id, score, comment)
        SELECT @attemptId, c.id,
               CASE c.sort_order WHEN 1 THEN 80 WHEN 2 THEN 80 WHEN 3 THEN 90 ELSE 80 END, NULL
        FROM criteria c WHERE c.criteria_set_version_id = @setId;

        INSERT INTO score_approvals (scoring_attempt_id, decision, decided_by, decided_at, announced_at)
        VALUES (@attemptId, 'APPROVED', @duc, '2026-10-04 08:00', '2026-10-04 08:05');

        INSERT INTO article_status_logs (article_id, actor_id, action, from_status, to_status, note, created_at)
        VALUES (@articleId, @hung,  'CREATE',        NULL,                   'DRAFT',                NULL, '2026-10-01 08:00'),
               (@articleId, @hung,  'SUBMIT',        'DRAFT',                'PENDING_CONFIRMATION', NULL, '2026-10-01 10:00'),
               (@articleId, @binh,  'CONFIRM',       'PENDING_CONFIRMATION', 'SUBMITTED',            NULL, '2026-10-01 15:00'),
               (@articleId, @duc,   'ASSIGN',        'SUBMITTED',            'ASSIGNED',             NULL, '2026-10-02 09:00'),
               (@articleId, @giang, 'SUBMIT_SCORE',  'ASSIGNED',             'PENDING_APPROVAL',     NULL, '2026-10-03 09:00'),
               (@articleId, @duc,   'APPROVE_SCORE', 'PENDING_APPROVAL',     'SCORE_PUBLISHED',      NULL, '2026-10-04 08:05'),
               (@articleId, @duc,   'PUBLISH',       'SCORE_PUBLISHED',      'PUBLISHED',            NULL, '2026-10-05 09:00');
    END;

    /* ---------- F. Bài gốc, 1 tác giả, có 1 ảnh + 1 video đều được tính tiền ---------- */
    IF NOT EXISTS (SELECT 1 FROM articles WHERE title = N'Làng gốm Bát Tràng chuyển mình đón khách du lịch quốc tế')
    BEGIN
        INSERT INTO articles (author_id, section_id, declared_category_id, category_id, approved_by, title, content,
                              declared_nature, status, created_at, updated_at, submitted_at, published_at)
        VALUES (@hung, @hungSection, 3, 3, @duc, N'Làng gốm Bát Tràng chuyển mình đón khách du lịch quốc tế',
                N'Các xưởng gốm mở thêm khu trải nghiệm, doanh thu du lịch của làng nghề tăng mạnh so với cùng kỳ...',
                'ORIGINAL', 'PUBLISHED', '2026-10-23 08:00', '2026-10-28 09:00', '2026-10-23 10:00', '2026-10-28 09:00');
        SET @articleId = CAST(SCOPE_IDENTITY() AS INT);

        INSERT INTO article_authors (article_id, user_id, share_percent, confirm_status, responded_at)
        VALUES (@articleId, @hung, 100.00, 'ACCEPTED', '2026-10-23 10:00');

        INSERT INTO article_versions (article_id, version_no, kind, title, content, change_type, created_by, created_at)
        VALUES (@articleId, 1, 'SUBMISSION', N'Làng gốm Bát Tràng chuyển mình đón khách du lịch quốc tế',
                N'Các xưởng gốm mở thêm khu trải nghiệm, doanh thu du lịch của làng nghề tăng mạnh so với cùng kỳ...',
                'INITIAL', @hung, '2026-10-23 10:00');
        SET @versionId = CAST(SCOPE_IDENTITY() AS INT);

        -- Ảnh/video gắn bản nộp, không trùng, không lấy nguồn ngoài -> cả 2 được tính tiền (BR-RY-04).
        -- Mã băm sinh từ url cho khỏi viết tay 64 ký tự (giống BF04_sample_royalty.sql).
        INSERT INTO article_attachments (article_id, version_id, author_id, media_type, media_kind, url,
                                         file_hash, dhash, hash_result, caption, created_at)
        VALUES (@articleId, @versionId, @hung, 'IMAGE', 'PHOTO', N'/uploads/2026/10/bat-trang-xuong-gom.jpg',
                LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', N'/uploads/2026/10/bat-trang-xuong-gom.jpg'), 2)),
                LOWER(LEFT(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', N'/uploads/2026/10/bat-trang-xuong-gom.jpg#d'), 2), 16)),
                'NEW', N'Du khách học nặn gốm tại một xưởng ở Bát Tràng', '2026-10-23 10:00'),
               (@articleId, @versionId, @hung, 'VIDEO', 'VIDEO', N'/uploads/2026/10/bat-trang-phong-su.mp4',
                LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', N'/uploads/2026/10/bat-trang-phong-su.mp4'), 2)),
                NULL, 'NEW', N'Video phóng sự 3 phút về làng gốm', '2026-10-23 10:00');

        INSERT INTO assignments (article_id, editor_id, assigned_by, assigned_at, was_suggested)
        VALUES (@articleId, @giang, @duc, '2026-10-23 14:00', 1);
        SET @assignmentId = CAST(SCOPE_IDENTITY() AS INT);

        -- 70*30% + 75*20% + 80*30% + 70*20% = 21 + 15 + 24 + 14 = 74.00
        INSERT INTO scoring_attempts (article_id, version_id, assignment_id, reviewer_id, attempt_no, attempt_type,
                                      criteria_set_version_id, category_id, total_score, decision, decision_note,
                                      opened_at, submitted_at)
        VALUES (@articleId, @versionId, @assignmentId, @giang, 1, 'INITIAL', @setId, 3, 74.00, 'ACCEPT',
                N'Ảnh và video tốt, phần viết cần thêm số liệu doanh thu.', '2026-10-24 08:30', '2026-10-24 09:00');
        SET @attemptId = CAST(SCOPE_IDENTITY() AS INT);

        INSERT INTO scoring_attempt_details (scoring_attempt_id, criterion_id, score, comment)
        SELECT @attemptId, c.id,
               CASE c.sort_order WHEN 1 THEN 70 WHEN 2 THEN 75 WHEN 3 THEN 80 ELSE 70 END, NULL
        FROM criteria c WHERE c.criteria_set_version_id = @setId;

        INSERT INTO score_approvals (scoring_attempt_id, decision, decided_by, decided_at, announced_at)
        VALUES (@attemptId, 'APPROVED', @duc, '2026-10-26 08:00', '2026-10-26 08:05');

        INSERT INTO article_status_logs (article_id, actor_id, action, from_status, to_status, note, created_at)
        VALUES (@articleId, @hung,  'CREATE',        NULL,               'DRAFT',            NULL, '2026-10-23 08:00'),
               (@articleId, @hung,  'SUBMIT',        'DRAFT',            'SUBMITTED',        NULL, '2026-10-23 10:00'),
               (@articleId, @duc,   'ASSIGN',        'SUBMITTED',        'ASSIGNED',         NULL, '2026-10-23 14:00'),
               (@articleId, @giang, 'SUBMIT_SCORE',  'ASSIGNED',         'PENDING_APPROVAL', NULL, '2026-10-24 09:00'),
               (@articleId, @duc,   'APPROVE_SCORE', 'PENDING_APPROVAL', 'SCORE_PUBLISHED',  NULL, '2026-10-26 08:05'),
               (@articleId, @duc,   'PUBLISH',       'SCORE_PUBLISHED',  'PUBLISHED',        NULL, '2026-10-28 09:00');
    END;

    COMMIT TRANSACTION;

    /* ---------- Kết quả để đối chiếu với màn hình UC-46 ---------- */
    SELECT a.id, a.title, a.declared_nature, CAST(a.published_at AS DATE) AS ngay_dang,
           DATEADD(DAY, 30, CAST(a.published_at AS DATE)) AS han_chi, s.total_score AS diem
    FROM articles a JOIN scoring_attempts s ON s.article_id = a.id
    WHERE a.status = 'PUBLISHED' AND a.author_id = @hung
      AND NOT EXISTS (SELECT 1 FROM royalty_gradings g WHERE g.article_id = a.id)
    ORDER BY a.published_at;

    PRINT N'Nạp dữ liệu mẫu BF-04 (1 phóng viên 4 bài) thành công.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
