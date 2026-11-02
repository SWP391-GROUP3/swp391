-- noinspection SqlNoDataSourceInspectionForFile
/* ==========================================================================
   BF-04 - Dữ liệu mẫu nhuận bút để test / demo (chỉ dùng cho dev)
   Chạy SAU schema.sql + data.sql, hoặc chạy thẳng trên DB đang có dữ liệu.
   sqlcmd: sqlcmd -S localhost -U sa -P <mk> -d swp391_kpi -C -f 65001 -i database/seeds/BF04_sample_royalty.sql

   Tạo 17 bài PUBLISHED (đủ chuỗi BF-01/02: tác giả, bản nộp, phân công,
   lần chấm, duyệt điểm, lịch sử) chia 4 nhóm:
     H7   (3 bài)  kỳ 7/2026 PAID   : đã chấm, đã khoá, mọi phiếu chi PAID
     H8   (3 bài)  kỳ 8/2026 LOCKED : đã khoá, phiếu chi An PAID, Bình + Hùng UNPAID
     OPEN (3 bài)  đã chấm vào kỳ OPEN cũ nhất (DB mới = kỳ 9, DB đang test = kỳ 10)
                   -> để test chốt kỳ (UC-50), trả kỳ (UC-52), khoá kỳ (UC-53)
     PEND (8 bài)  đã duyệt điểm, CHƯA chấm nhuận bút -> test UC-46/47/48

   Mọi con số tính bằng ĐÚNG công thức của RoyaltyCalculator (BR-RY-01..04,
   GBR-10), tham số đọc từ system_parameters, đơn giá theo ngày đăng.
   Chỉ dùng 2 thể loại đã có bộ tiêu chí ACTIVE: Tin sâu (2.00), Bài phản ánh (5.00).

   Chạy lại: thấy bài mẫu đầu tiên đã có thì bỏ qua toàn bộ, không tạo trùng.
   Kỳ 7 hoặc 8/2026 đã có sẵn thì dừng báo lỗi (không ghi đè kỳ của bạn).
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

    /* ---------- 0. Người dùng, cấu hình cần có (lấy theo username, không đoán id) ---------- */
    DECLARE @an INT    = (SELECT id FROM users WHERE username = 'an.nv');      -- PV Thời sự
    DECLARE @binh INT  = (SELECT id FROM users WHERE username = 'binh.tt');    -- PV Thời sự
    DECLARE @hung INT  = (SELECT id FROM users WHERE username = 'hung.dq');    -- PV Kinh tế
    DECLARE @cuong INT = (SELECT id FROM users WHERE username = 'cuong.lh');   -- BTV Thời sự
    DECLARE @giang INT = (SELECT id FROM users WHERE username = 'giang.nt');   -- BTV Kinh tế
    DECLARE @duc INT   = (SELECT id FROM users WHERE username = 'duc.vm');     -- Trưởng ban
    DECLARE @dung INT  = (SELECT id FROM users WHERE username = 'dung.pt');    -- Thư ký toà soạn
    DECLARE @chi INT   = (SELECT id FROM users WHERE username = 'chi.hl');     -- Kế toán
    IF @an IS NULL OR @binh IS NULL OR @hung IS NULL OR @cuong IS NULL OR @giang IS NULL
       OR @duc IS NULL OR @dung IS NULL OR @chi IS NULL
        THROW 51400, N'Thiếu tài khoản mẫu của data.sql (an.nv, binh.tt, hung.dq, cuong.lh, giang.nt, duc.vm, dung.pt, chi.hl).', 1;

    DECLARE @event INT = (SELECT TOP (1) id FROM events WHERE is_active = 1 ORDER BY id);
    IF @event IS NULL
        THROW 51401, N'Chưa có sự kiện nào trong bảng events (data.sql tạo sự kiện Bão số 3).', 1;

    IF NOT EXISTS (SELECT 1 FROM criteria_set_versions WHERE category_id = 2 AND status = 'ACTIVE')
       OR NOT EXISTS (SELECT 1 FROM criteria_set_versions WHERE category_id = 3 AND status = 'ACTIVE')
        THROW 51402, N'Thể loại Tin sâu (2) và Bài phản ánh (3) phải có bộ tiêu chí ACTIVE.', 1;

    -- Tham số BF-04, đọc 1 lần (giống RoyaltyCalculator đọc system_parameters)
    DECLARE @minFinal DECIMAL(5,2) = (SELECT CAST(param_value AS DECIMAL(5,2)) FROM system_parameters WHERE param_key = 'MIN_FINAL_FACTOR');
    DECLARE @roundUnit DECIMAL(18,0) = (SELECT CAST(param_value AS DECIMAL(18,0)) FROM system_parameters WHERE param_key = 'ROUNDING_UNIT');
    IF @minFinal IS NULL OR @roundUnit IS NULL
        THROW 51403, N'Thiếu tham số MIN_FINAL_FACTOR hoặc ROUNDING_UNIT.', 1;

    -- Kỳ OPEN cũ nhất = kỳ mà Thư ký chấm bài sẽ vào (BR-RY-05, giống findOpenPeriod())
    DECLARE @openPeriod INT = (SELECT TOP (1) id FROM royalty_periods WHERE status = 'OPEN'
                               ORDER BY period_year, period_month);
    IF @openPeriod IS NULL
        THROW 51404, N'Chưa có kỳ nhuận bút OPEN nào.', 1;

    IF EXISTS (SELECT 1 FROM articles WHERE title = N'Ngập úng kéo dài ở khu đô thị mới phía Tây')
    BEGIN
        PRINT N'Dữ liệu mẫu BF-04 đã có, bỏ qua.';
        COMMIT TRANSACTION;
        RETURN;
    END;

    IF EXISTS (SELECT 1 FROM royalty_periods WHERE period_year = 2026 AND period_month IN (7, 8))
        THROW 51405, N'Kỳ 7 hoặc 8/2026 đã có sẵn, seed không ghi đè. Xoá 2 kỳ đó hoặc chạy trên DB mới.', 1;

    /* ---------- 1. Kỳ cũ (lịch sử, UC-49) ---------- */
    INSERT INTO royalty_periods (period_year, period_month, status, locked_by, locked_at, paid_at)
    VALUES (2026, 7, 'PAID', @chi, '2026-08-05 09:00', '2026-08-10 15:00');
    DECLARE @period7 INT = CAST(SCOPE_IDENTITY() AS INT);

    INSERT INTO royalty_periods (period_year, period_month, status, locked_by, locked_at)
    VALUES (2026, 8, 'LOCKED', @chi, '2026-09-05 09:00');
    DECLARE @period8 INT = CAST(SCOPE_IDENTITY() AS INT);

    /* ---------- 2. Danh sách bài mẫu ----------
       s1..s4 = điểm từng tiêu chí theo sort_order. Tin sâu có 3 tiêu chí (40/35/25),
       Bài phản ánh có 4 tiêu chí (30/20/30/20). Số tiền ghi ở cuối dòng là kết quả
       mong đợi (tiền phần bài viết), dùng để đối chiếu với màn hình. */
    DECLARE @articles TABLE (
        seq INT PRIMARY KEY, grp VARCHAR(4), title NVARCHAR(300),
        main_id INT, co1_id INT, co1_share DECIMAL(5,2), co2_id INT, co2_share DECIMAL(5,2),
        category_id INT, nature VARCHAR(20), sat_rank INT, sat_rate DECIMAL(5,2),
        published_at DATETIME2, editor_id INT,
        s1 DECIMAL(5,2), s2 DECIMAL(5,2), s3 DECIMAL(5,2), s4 DECIMAL(5,2));

    INSERT INTO @articles VALUES
    -- Kỳ 7/2026 (PAID)
    ( 1, 'H7',   N'Ngập úng kéo dài ở khu đô thị mới phía Tây',
         @an,   NULL, NULL, NULL, NULL, 3, 'ORIGINAL',    NULL, NULL, '2026-07-10 09:00', @cuong, 80, 75, 85, 70),   -- 78.50 -> 3.93 -> 919.000 (+ ảnh 234.000)
    ( 2, 'H7',   N'Giá vé tàu hè tăng nhẹ so với năm ngoái',
         @binh, NULL, NULL, NULL, NULL, 2, 'ORIGINAL',    NULL, NULL, '2026-07-22 09:00', @cuong, 85, 80, 75, NULL), -- 80.75 -> 1.62 -> 379.000
    ( 3, 'H7',   N'Tổng hợp: thị trường bất động sản nửa đầu năm 2026',
         @hung, NULL, NULL, NULL, NULL, 3, 'COMPILATION', NULL, NULL, '2026-07-15 09:00', @giang, 70, 70, 80, 60),   -- 71.00 -> 3.55 x 50% = 1.78 -> 416.000
    -- Kỳ 8/2026 (LOCKED)
    ( 4, 'H8',   N'WHO khuyến cáo tiêm nhắc vắc xin cúm mùa',
         @an,   NULL, NULL, NULL, NULL, 2, 'TRANSLATION', NULL, NULL, '2026-08-12 09:00', @cuong, 90, 85, 80, NULL), -- 85.75 -> 1.72 x 70% = 1.20 -> 280.000
    ( 5, 'H8',   N'Chợ đêm ven sông ùn tắc mỗi cuối tuần',
         @binh, @an, 30, NULL, NULL, 3, 'ORIGINAL',    NULL, NULL, '2026-08-20 09:00', @cuong, 85, 90, 80, 75),   -- 82.50 -> 4.13 -> 966.000: An 289.000, Bình 677.000 (+ video 702.000)
    ( 6, 'H8',   N'Doanh nghiệp xuất khẩu gạo lo thiếu container',
         @hung, NULL, NULL, NULL, NULL, 2, 'ORIGINAL',    NULL, NULL, '2026-08-25 09:00', @giang, 70, 65, 72, NULL), -- 68.75 -> 1.38 -> 322.000
    -- Đã chấm, nằm trong kỳ OPEN
    ( 7, 'OPEN', N'Cầu tạm ở xã Tân Lập xuống cấp sau mưa lũ',
         @binh, NULL, NULL, NULL, NULL, 3, 'ORIGINAL',    NULL, NULL, '2026-09-18 09:00', @cuong, 85, 80, 80, 75),   -- 80.50 -> 4.03 -> 943.000 (+ ảnh 234.000)
    ( 8, 'OPEN', N'Ngân hàng đồng loạt giảm lãi suất cho vay mua nhà',
         @hung, NULL, NULL, NULL, NULL, 2, 'ORIGINAL',    NULL, NULL, '2026-09-21 09:00', @giang, 75, 70, 80, NULL), -- 74.50 -> 1.49 -> 348.000
    ( 9, 'OPEN', N'Hàng nghìn hộ dân vùng ven biển sơ tán tránh bão',
         @an,   @hung, 30, NULL, NULL, 3, 'ORIGINAL',  11, 80,  '2026-09-16 09:00', @cuong, 90, 85, 85, 85),   -- 86.50 (< 90, bị bão hoà 80%) -> 4.33 x 80% = 3.46 -> 809.000: Hùng 242.000, An 567.000
    -- Chờ chấm nhuận bút (UC-46). Hạn chi = ngày đăng + 30 ngày.
    (10, 'PEND', N'Xe buýt nhanh BRT vắng khách giờ thấp điểm',
         @an,   NULL, NULL, NULL, NULL, 2, 'ORIGINAL',    NULL, NULL, '2026-09-03 09:00', @cuong, 80, 78, 70, NULL), -- 76.80 -> 1.54 -> 360.000, QUÁ HẠN
    (11, 'PEND', N'Người dân dọn bùn sau khi nước rút',
         @binh, NULL, NULL, NULL, NULL, 3, 'ORIGINAL',    12, 80,  '2026-09-14 09:00', @cuong, 80, 75, 80, 75),   -- 78.00 -> 3.90 x bão hoà 80% = 3.12 -> 730.000
    (12, 'PEND', N'Điều tra: hàng cứu trợ bão bị giữ lại ở kho trung chuyển',
         @an,   @hung, 50, NULL, NULL, 3, 'ORIGINAL',  14, 60,  '2026-09-19 09:00', @cuong, 95, 90, 90, 90),   -- 91.50 >= 90 MIỄN bão hoà -> 4.58 -> 1.071.000: Hùng 535.000, An 536.000 (dư lẻ về tác giả chính)
    (13, 'PEND', N'Tổng hợp ý kiến doanh nghiệp về dự thảo luật thuế',
         @hung, NULL, NULL, NULL, NULL, 3, 'COMPILATION', NULL, NULL, '2026-09-25 09:00', @giang, 80, 80, 80, 80),   -- 80.00 -> 4.00 x 50% = 2.00 -> 468.000
    (14, 'PEND', N'Đăng lại: lịch thi tốt nghiệp THPT năm 2027',
         @binh, NULL, NULL, NULL, NULL, 2, 'REPOST',      NULL, NULL, '2026-10-02 09:00', @cuong, 60, 65, 70, NULL), -- 64.25 -> 1.29 x 30% = 0.39 -> TỐI THIỂU 1.00 -> 234.000, gần hạn
    (15, 'PEND', N'Phố đi bộ mới thu hút đông khách dịp cuối tuần',
         @an,   @binh, 25, @hung, 15, 3, 'ORIGINAL',   NULL, NULL, '2026-10-06 09:00', @cuong, 85, 85, 90, 80),   -- 85.50 -> 4.28 -> 1.001.000: Bình 250.000, Hùng 150.000, An 601.000 (+ ảnh, video, đồ hoạ)
    (16, 'PEND', N'Bloomberg: châu Á dẫn đầu tăng trưởng thương mại điện tử',
         @hung, NULL, NULL, NULL, NULL, 3, 'TRANSLATION', NULL, NULL, '2026-10-20 09:00', @giang, 75, 80, 70, 75),   -- 74.50 -> 3.73 x 70% = 2.61 -> 610.000 (1 ảnh mới, 1 ảnh tái sử dụng)
    (17, 'PEND', N'Thủ đô khai trương tuyến metro số 3',
         @an,   NULL, NULL, NULL, NULL, 2, 'ORIGINAL',    NULL, NULL, '2026-10-27 09:00', @cuong, 95, 90, 92, NULL); -- 92.50 -> 1.85 -> 432.000

    /* ---------- 3. Ảnh / video ----------
       match_seq: ảnh bị trùng (theo seq của bảng này). Ảnh 9 gần giống ảnh 1 (bài 1
       đã trả nhuận bút ở kỳ 7) -> tái sử dụng, không tính tiền (BR-RY-04).
       external_source khác NULL -> nguồn ngoài, không tính tiền. */
    DECLARE @media TABLE (
        seq INT PRIMARY KEY, article_seq INT, author_id INT, media_type VARCHAR(10), media_kind VARCHAR(20),
        external_source NVARCHAR(200), hash_result VARCHAR(10), match_seq INT, distance_bits TINYINT,
        url NVARCHAR(500), caption NVARCHAR(300), attachment_id INT NULL);

    INSERT INTO @media (seq, article_seq, author_id, media_type, media_kind, external_source, hash_result, match_seq, distance_bits, url, caption) VALUES
    (1,  1, @an,   'IMAGE', 'PHOTO',   NULL,               'NEW',     NULL, NULL, N'/uploads/2026/07/ngap-do-thi-tay-1.jpg',   N'Nước ngập ngang bánh xe tại khu đô thị'),
    (2,  5, @binh, 'VIDEO', 'VIDEO',   NULL,               'NEW',     NULL, NULL, N'/uploads/2026/08/cho-dem-un-tac.mp4',      N'Video ùn tắc trước cổng chợ đêm'),
    (3,  5, @an,   'IMAGE', 'PHOTO',   N'Cổng thông tin quận', 'NEW', NULL, NULL, N'/uploads/2026/08/so-do-cho-dem.jpg',       N'Sơ đồ khu chợ (nguồn: cổng thông tin quận)'),
    (4,  7, @binh, 'IMAGE', 'PHOTO',   NULL,               'NEW',     NULL, NULL, N'/uploads/2026/09/cau-tam-tan-lap.jpg',     N'Mặt cầu tạm bị sụt lún'),
    (5, 15, @binh, 'IMAGE', 'PHOTO',   NULL,               'NEW',     NULL, NULL, N'/uploads/2026/10/pho-di-bo-1.jpg',         N'Phố đi bộ đông khách tối thứ Bảy'),
    (6, 15, @an,   'VIDEO', 'VIDEO',   NULL,               'NEW',     NULL, NULL, N'/uploads/2026/10/pho-di-bo-flycam.mp4',    N'Flycam toàn cảnh phố đi bộ'),
    (7, 15, @hung, 'IMAGE', 'GRAPHIC', NULL,               'NEW',     NULL, NULL, N'/uploads/2026/10/pho-di-bo-ban-do.png',    N'Đồ hoạ các tuyến phố cấm xe'),
    (8, 16, @hung, 'IMAGE', 'PHOTO',   NULL,               'NEW',     NULL, NULL, N'/uploads/2026/10/kho-hang-tmdt.jpg',       N'Kho hàng của một sàn thương mại điện tử'),
    (9, 16, @hung, 'IMAGE', 'PHOTO',   NULL,               'SIMILAR', 1,    3,    N'/uploads/2026/10/ngap-minh-hoa.jpg',       N'Ảnh minh hoạ dùng lại');

    /* ---------- 4. Tạo từng bài theo đúng vòng đời BF-01 -> BF-02 -> (BF-04 nếu đã chấm) ----------
       Mốc thời gian tính lùi từ ngày đăng D (09:00): tạo D-5 08:00, nộp D-5 10:00,
       phân công D-4 09:00, chấm D-3 09:00, duyệt + công bố điểm D-2 08:05,
       Thư ký chấm nhuận bút D+2 10:00. */
    DECLARE @seq INT = 1, @maxSeq INT = (SELECT MAX(seq) FROM @articles);
    DECLARE @grp VARCHAR(4), @title NVARCHAR(300), @mainId INT, @co1 INT, @co1Share DECIMAL(5,2),
            @co2 INT, @co2Share DECIMAL(5,2), @cat INT, @nature VARCHAR(20), @satRank INT, @satRate DECIMAL(5,2),
            @pub DATETIME2, @editor INT, @s1 DECIMAL(5,2), @s2 DECIMAL(5,2), @s3 DECIMAL(5,2), @s4 DECIMAL(5,2);
    DECLARE @articleId INT, @versionId INT, @assignmentId INT, @attemptId INT, @setId INT,
            @total DECIMAL(5,2), @hasCoAuthor BIT, @firstStatus VARCHAR(20);
    DECLARE @periodId INT, @rateId INT, @unitPrice DECIMAL(18,0), @baseFactor DECIMAL(5,2),
            @suggested DECIMAL(5,2), @effSat DECIMAL(5,2), @natureRate DECIMAL(5,2), @final DECIMAL(5,2),
            @amount DECIMAL(18,0), @co1Part DECIMAL(18,0), @co2Part DECIMAL(18,0), @gradingId INT, @gradedAt DATETIME2;

    WHILE @seq <= @maxSeq
    BEGIN
        SELECT @grp = grp, @title = title, @mainId = main_id, @co1 = co1_id, @co1Share = co1_share,
               @co2 = co2_id, @co2Share = co2_share, @cat = category_id, @nature = nature,
               @satRank = sat_rank, @satRate = sat_rate, @pub = published_at, @editor = editor_id,
               @s1 = s1, @s2 = s2, @s3 = s3, @s4 = s4
        FROM @articles WHERE seq = @seq;
        SET @hasCoAuthor = CASE WHEN @co1 IS NULL THEN 0 ELSE 1 END;
        SET @firstStatus = CASE WHEN @hasCoAuthor = 1 THEN 'PENDING_CONFIRMATION' ELSE 'SUBMITTED' END;

        -- 4.1 Bài + tác giả + bản nộp (BF-01)
        INSERT INTO articles (author_id, section_id, declared_category_id, category_id, event_id, approved_by,
                              title, content, declared_nature, status, saturation_rank, saturation_rate,
                              created_at, updated_at, submitted_at, published_at)
        VALUES (@mainId, (SELECT section_id FROM users WHERE id = @mainId), @cat, @cat,
                CASE WHEN @satRank IS NULL THEN NULL ELSE @event END, @duc,
                @title, N'Bản đã biên tập: ' + @title, @nature, 'PUBLISHED', @satRank, @satRate,
                DATEADD(MINUTE, -7260, @pub), @pub, DATEADD(MINUTE, -7140, @pub), @pub);
        SET @articleId = CAST(SCOPE_IDENTITY() AS INT);

        -- Tác giả chính nhận phần còn lại để tổng = 100% (GBR-01)
        INSERT INTO article_authors (article_id, user_id, share_percent, confirm_status, confirm_deadline, responded_at)
        VALUES (@articleId, @mainId, 100 - ISNULL(@co1Share, 0) - ISNULL(@co2Share, 0), 'ACCEPTED', NULL, DATEADD(MINUTE, -7140, @pub));
        IF @co1 IS NOT NULL
            INSERT INTO article_authors (article_id, user_id, share_percent, confirm_status, confirm_deadline, responded_at)
            VALUES (@articleId, @co1, @co1Share, 'ACCEPTED', DATEADD(MINUTE, -2820, @pub), DATEADD(MINUTE, -6840, @pub));
        IF @co2 IS NOT NULL
            INSERT INTO article_authors (article_id, user_id, share_percent, confirm_status, confirm_deadline, responded_at)
            VALUES (@articleId, @co2, @co2Share, 'ACCEPTED', DATEADD(MINUTE, -2820, @pub), DATEADD(MINUTE, -6840, @pub));

        INSERT INTO article_versions (article_id, version_no, kind, title, content, change_type, created_by, created_at)
        VALUES (@articleId, 1, 'SUBMISSION', @title, N'Bản PV nộp: ' + @title, 'INITIAL', @mainId, DATEADD(MINUTE, -7140, @pub));
        SET @versionId = CAST(SCOPE_IDENTITY() AS INT);

        -- Ảnh/video gắn bản nộp. Mã băm sinh từ url cho khỏi viết tay 64 ký tự.
        INSERT INTO article_attachments (article_id, version_id, author_id, media_type, media_kind, external_source, url,
                                         file_hash, dhash, hash_result, distance_bits, matched_attachment_id, caption, created_at)
        SELECT @articleId, @versionId, m.author_id, m.media_type, m.media_kind, m.external_source, m.url,
               LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', m.url), 2)),
               CASE WHEN m.media_type = 'IMAGE' THEN LOWER(LEFT(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', m.url + N'#d'), 2), 16)) END,
               m.hash_result, m.distance_bits, matched.attachment_id, m.caption, DATEADD(MINUTE, -7140, @pub)
        FROM @media m LEFT JOIN @media matched ON matched.seq = m.match_seq
        WHERE m.article_seq = @seq;

        UPDATE m SET attachment_id = a.id
        FROM @media m JOIN article_attachments a ON a.url = m.url AND a.article_id = @articleId
        WHERE m.article_seq = @seq;

        -- 4.2 Phân công, chấm điểm, duyệt điểm (BF-02)
        INSERT INTO assignments (article_id, editor_id, assigned_by, assigned_at, was_suggested)
        VALUES (@articleId, @editor, @duc, DATEADD(MINUTE, -5760, @pub), 1);
        SET @assignmentId = CAST(SCOPE_IDENTITY() AS INT);

        SET @setId = (SELECT id FROM criteria_set_versions WHERE category_id = @cat AND status = 'ACTIVE');
        INSERT INTO scoring_attempts (article_id, version_id, assignment_id, reviewer_id, attempt_no, attempt_type,
                                      criteria_set_version_id, category_id, total_score, decision, decision_note,
                                      opened_at, submitted_at)
        VALUES (@articleId, @versionId, @assignmentId, @editor, 1, 'INITIAL', @setId, @cat, NULL, NULL, NULL,
                DATEADD(MINUTE, -4350, @pub), NULL);
        SET @attemptId = CAST(SCOPE_IDENTITY() AS INT);

        INSERT INTO scoring_attempt_details (scoring_attempt_id, criterion_id, score, comment)
        SELECT @attemptId, c.id,
               CASE c.sort_order WHEN 1 THEN @s1 WHEN 2 THEN @s2 WHEN 3 THEN @s3 ELSE @s4 END, NULL
        FROM criteria c WHERE c.criteria_set_version_id = @setId;

        -- total = tổng (điểm x trọng số / 100), giống cách BTV nộp phiếu chấm
        SELECT @total = CAST(ROUND(SUM(d.score * c.weight / 100), 2) AS DECIMAL(5,2))
        FROM scoring_attempt_details d JOIN criteria c ON c.id = d.criterion_id
        WHERE d.scoring_attempt_id = @attemptId;

        UPDATE scoring_attempts
        SET total_score = @total, decision = 'ACCEPT', decision_note = N'Đạt yêu cầu đăng.',
            submitted_at = DATEADD(MINUTE, -4320, @pub)
        WHERE id = @attemptId;

        INSERT INTO score_approvals (scoring_attempt_id, decision, decided_by, decided_at, announced_at)
        VALUES (@attemptId, 'APPROVED', @duc, DATEADD(MINUTE, -2940, @pub), DATEADD(MINUTE, -2935, @pub));

        INSERT INTO article_status_logs (article_id, actor_id, action, from_status, to_status, note, created_at) VALUES
            (@articleId, @mainId, 'CREATE', NULL, 'DRAFT', NULL, DATEADD(MINUTE, -7260, @pub)),
            (@articleId, @mainId, 'SUBMIT', 'DRAFT', @firstStatus, NULL, DATEADD(MINUTE, -7140, @pub));
        IF @hasCoAuthor = 1
            INSERT INTO article_status_logs (article_id, actor_id, action, from_status, to_status, note, created_at)
            VALUES (@articleId, ISNULL(@co2, @co1), 'CONFIRM', 'PENDING_CONFIRMATION', 'SUBMITTED', NULL, DATEADD(MINUTE, -6840, @pub));
        INSERT INTO article_status_logs (article_id, actor_id, action, from_status, to_status, note, created_at) VALUES
            (@articleId, @duc,    'ASSIGN',        'SUBMITTED',        'ASSIGNED',         NULL, DATEADD(MINUTE, -5760, @pub)),
            (@articleId, @editor, 'SUBMIT_SCORE',  'ASSIGNED',         'PENDING_APPROVAL', NULL, DATEADD(MINUTE, -4320, @pub)),
            (@articleId, @duc,    'APPROVE_SCORE', 'PENDING_APPROVAL', 'SCORE_PUBLISHED',  NULL, DATEADD(MINUTE, -2935, @pub)),
            (@articleId, @duc,    'PUBLISH',       'SCORE_PUBLISHED',  'PUBLISHED',        NULL, @pub);

        -- 4.3 Phiếu chấm nhuận bút (chỉ nhóm H7, H8, OPEN). Bài PEND để Thư ký tự chấm.
        IF @grp <> 'PEND'
        BEGIN
            SET @periodId = CASE @grp WHEN 'H7' THEN @period7 WHEN 'H8' THEN @period8 ELSE @openPeriod END;
            SET @gradedAt = DATEADD(MINUTE, 2940, @pub);

            -- Đơn giá hiệu lực tại ngày đăng (BR-RY-03, GBR-08)
            SET @rateId = NULL;
            SELECT TOP (1) @rateId = id, @unitPrice = amount FROM royalty_rates
            WHERE effective_from <= CAST(@pub AS DATE) ORDER BY effective_from DESC;
            IF @rateId IS NULL
                THROW 51406, N'Không có đơn giá hiệu lực tại ngày đăng của bài mẫu.', 1;

            SET @baseFactor = (SELECT base_factor FROM genres WHERE id = @cat);
            SET @natureRate = (SELECT CAST(param_value AS DECIMAL(5,2)) FROM system_parameters
                               WHERE param_key = 'NATURE_RATE_' + @nature);
            -- BR-RY-01: hệ số = hệ số chuẩn x điểm / 100, giữ 2 chữ số (ROUND = HALF_UP với số dương)
            SET @suggested = ROUND(@baseFactor * @total / 100, 2);
            -- GBR-09: không thuộc sự kiện hoặc điểm >= exempt_score -> 100%
            SET @effSat = CASE WHEN @satRate IS NULL THEN 100
                               WHEN @total >= (SELECT exempt_score FROM events WHERE id = @event) THEN 100
                               ELSE @satRate END;
            -- BR-RY-02: hệ số cuối, tối thiểu MIN_FINAL_FACTOR
            SET @final = ROUND(@suggested * @effSat * @natureRate / 10000, 2);
            IF @final < @minFinal SET @final = @minFinal;
            -- BR-RY-03: tiền làm tròn XUỐNG tới ROUNDING_UNIT
            SET @amount = FLOOR(@final * @unitPrice / @roundUnit) * @roundUnit;

            INSERT INTO royalty_gradings (scoring_attempt_id, article_id, version_no, period_id, rate_id, category_id,
                                          score_used, tier_rate, base_factor, suggested_factor, applied_factor, raise_reason,
                                          saturation_rate, nature, nature_rate, final_factor, unit_price, amount,
                                          graded_by, graded_at)
            VALUES (@attemptId, @articleId, 1, @periodId, @rateId, @cat,
                    @total, @total, @baseFactor, @suggested, @suggested, NULL,
                    @effSat, @nature, @natureRate, @final, @unitPrice, @amount,
                    @dung, @gradedAt);
            SET @gradingId = CAST(SCOPE_IDENTITY() AS INT);

            -- GBR-10: đồng tác giả làm tròn xuống 1.000đ, tác giả chính nhận phần còn lại
            SET @co1Part = CASE WHEN @co1 IS NULL THEN 0 ELSE FLOOR(@amount * @co1Share / 100 / @roundUnit) * @roundUnit END;
            SET @co2Part = CASE WHEN @co2 IS NULL THEN 0 ELSE FLOOR(@amount * @co2Share / 100 / @roundUnit) * @roundUnit END;
            INSERT INTO royalty_lines (period_id, grading_id, user_id, line_type, share_percent, amount, created_at)
            VALUES (@periodId, @gradingId, @mainId, 'ARTICLE', 100 - ISNULL(@co1Share, 0) - ISNULL(@co2Share, 0),
                    @amount - @co1Part - @co2Part, @gradedAt);
            IF @co1 IS NOT NULL
                INSERT INTO royalty_lines (period_id, grading_id, user_id, line_type, share_percent, amount, created_at)
                VALUES (@periodId, @gradingId, @co1, 'ARTICLE', @co1Share, @co1Part, @gradedAt);
            IF @co2 IS NOT NULL
                INSERT INTO royalty_lines (period_id, grading_id, user_id, line_type, share_percent, amount, created_at)
                VALUES (@periodId, @gradingId, @co2, 'ARTICLE', @co2Share, @co2Part, @gradedAt);

            -- BR-RY-04: ảnh/video được trả = hệ số loại x đơn giá. Bỏ ảnh nguồn ngoài và ảnh
            -- trùng với ảnh đã từng được trả (giống findNoRoyaltyReason()).
            INSERT INTO royalty_media_items (grading_id, attachment_id, factor, unit_price, amount)
            SELECT @gradingId, m.attachment_id, f.factor, @unitPrice, FLOOR(f.factor * @unitPrice / @roundUnit) * @roundUnit
            FROM @media m
            JOIN media_factors f ON f.media_kind = m.media_kind
            LEFT JOIN @media matched ON matched.seq = m.match_seq
            WHERE m.article_seq = @seq
              AND m.external_source IS NULL
              AND NOT (m.hash_result IN ('DUPLICATE', 'SIMILAR')
                       AND EXISTS (SELECT 1 FROM royalty_media_items x WHERE x.attachment_id = matched.attachment_id));

            INSERT INTO royalty_lines (period_id, grading_id, media_item_id, user_id, line_type, share_percent, amount, created_at)
            SELECT @periodId, @gradingId, i.id, a.author_id, 'MEDIA', NULL, i.amount, @gradedAt
            FROM royalty_media_items i JOIN article_attachments a ON a.id = i.attachment_id
            WHERE i.grading_id = @gradingId;
        END;

        SET @seq = @seq + 1;
    END;

    /* ---------- 5. Phiếu chi của kỳ đã khoá (UC-53, UC-54) ----------
       Mỗi người 1 phiếu, gross = tổng dòng tiền. Kỳ cũ không ai nợ nên debt_in = 0,
       paid_amount = gross, debt_out = 0 (ck_royalty_payments_amount). */
    INSERT INTO royalty_payments (period_id, user_id, gross_amount, debt_in, paid_amount, debt_out, status, paid_by, paid_at)
    SELECT @period7, user_id, SUM(amount), 0, SUM(amount), 0, 'PAID', @chi, '2026-08-10 15:00'
    FROM royalty_lines WHERE period_id = @period7 GROUP BY user_id;

    -- Kỳ 8: mới chi cho An, Bình và Hùng chưa nhận -> kỳ còn LOCKED (chưa PAID)
    INSERT INTO royalty_payments (period_id, user_id, gross_amount, debt_in, paid_amount, debt_out, status, paid_by, paid_at)
    SELECT @period8, user_id, SUM(amount), 0, SUM(amount), 0,
           CASE WHEN user_id = @an THEN 'PAID' ELSE 'UNPAID' END,
           CASE WHEN user_id = @an THEN @chi END,
           CASE WHEN user_id = @an THEN CAST('2026-09-10 15:00' AS DATETIME2) END
    FROM royalty_lines WHERE period_id = @period8 GROUP BY user_id;

    UPDATE l SET payment_id = p.id
    FROM royalty_lines l JOIN royalty_payments p ON p.period_id = l.period_id AND p.user_id = l.user_id
    WHERE l.period_id IN (@period7, @period8);

    INSERT INTO audit_logs (actor_id, action_type, entity_type, entity_id, old_value, new_value, created_at) VALUES
        (@chi, 'LOCK', 'royalty_periods', @period7, N'{"status":"CLOSED"}', N'{"status":"LOCKED"}', '2026-08-05 09:00'),
        (@chi, 'PAY',  'royalty_periods', @period7, N'{"status":"LOCKED"}', N'{"status":"PAID"}',   '2026-08-10 15:00'),
        (@chi, 'LOCK', 'royalty_periods', @period8, N'{"status":"CLOSED"}', N'{"status":"LOCKED"}', '2026-09-05 09:00');

    COMMIT TRANSACTION;

    /* ---------- 6. Kết quả để đối chiếu với màn hình ---------- */
    SELECT p.period_month AS thang, p.status,
           (SELECT COUNT(DISTINCT g.article_id) FROM royalty_gradings g WHERE g.period_id = p.id) AS so_bai,
           (SELECT ISNULL(SUM(l.amount), 0) FROM royalty_lines l WHERE l.period_id = p.id) AS tong_tien
    FROM royalty_periods p ORDER BY p.period_year, p.period_month;

    SELECT a.id, a.title, CAST(a.published_at AS DATE) AS ngay_dang,
           DATEADD(DAY, 30, CAST(a.published_at AS DATE)) AS han_chi, s.total_score AS diem
    FROM articles a JOIN scoring_attempts s ON s.article_id = a.id
    WHERE a.status = 'PUBLISHED'
      AND NOT EXISTS (SELECT 1 FROM royalty_gradings g WHERE g.article_id = a.id)
    ORDER BY a.published_at;

    PRINT N'Nạp dữ liệu mẫu BF-04 thành công.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
