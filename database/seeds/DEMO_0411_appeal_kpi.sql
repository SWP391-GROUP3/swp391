-- noinspection SqlNoDataSourceInspectionForFile
/* ==========================================================================
   DEMO 04/11/2026 - dữ liệu bổ sung cho kịch bản demo cả dự án (chỉ dùng cho dev / demo)
   Thứ tự chạy trên DB mới:
     1. database/schema.sql
     2. database/data.sql
     3. database/seeds/BF04_sample_royalty.sql      (kỳ 7 PAID, kỳ 8 LOCKED có phiếu UNPAID, bài chờ chấm)
     4. database/seeds/BF04_sample_one_reporter.sql (PV Hưng có 4 bài chờ chấm)
     5. file này
   sqlcmd: sqlcmd -S localhost -U sa -P <mk> -d swp391_kpi -C -f 65001 -i database/seeds/DEMO_0411_appeal_kpi.sql

   File này thêm:
     A. 1 khiếu nại ĐÃ RESOLVED trên bài "[BF03-DEMO-03]" (của data.sql):
        S0 = 82.00 (BTV Cường chấm), S1 = 85.50 (BTV test.bt1 chấm mù),
        |S1 - S0| = 3.50 <= ngưỡng 5 -> dùng S1 (BR-QC-06), Trưởng ban Đức đã xác nhận.
        Bài vẫn SCORE_PUBLISHED: lúc demo Trưởng ban duyệt đăng (UC-33), Thư ký chấm nhuận bút
        -> phiếu nhuận bút phải dùng 85.50 chứ không phải 82.00 (BR-QC-10).
        Hai bài [BF03-DEMO-01], [BF03-DEMO-02] vẫn để trống để demo gửi khiếu nại trực tiếp (UC-35).
     B. Chỉ tiêu KPI tháng 10 và 11/2026 cho 3 phóng viên (trang KPI mặc định mở tháng hiện tại).

   Chạy lại: thấy đã có khiếu nại / chỉ tiêu thì bỏ qua, không tạo trùng.
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

    /* ---------- 0. Người dùng cần có (lấy theo username, không đoán id) ---------- */
    DECLARE @an INT    = (SELECT id FROM users WHERE username = 'an.nv');      -- PV, tác giả chính bài DEMO-03
    DECLARE @binh INT  = (SELECT id FROM users WHERE username = 'binh.tt');    -- PV Thời sự
    DECLARE @hung INT  = (SELECT id FROM users WHERE username = 'hung.dq');    -- PV Kinh tế
    DECLARE @duc INT   = (SELECT id FROM users WHERE username = 'duc.vm');     -- Trưởng ban Thời sự
    DECLARE @blind INT = (SELECT id FROM users WHERE username = 'test.bt1');   -- BTV chấm mù (không phải BTV gốc)
    IF @an IS NULL OR @binh IS NULL OR @hung IS NULL OR @duc IS NULL OR @blind IS NULL
        THROW 51500, N'Thiếu tài khoản mẫu của data.sql (an.nv, binh.tt, hung.dq, duc.vm, test.bt1).', 1;

    DECLARE @ruleId INT = (SELECT TOP (1) id FROM appeal_rule_configs
                           WHERE is_active = 1 AND effective_from <= CAST(SYSDATETIME() AS DATE)
                           ORDER BY effective_from DESC);
    IF @ruleId IS NULL
        THROW 51501, N'Chưa có cấu hình khiếu nại đang hiệu lực (appeal_rule_configs).', 1;

    /* ---------- A. Khiếu nại đã RESOLVED trên bài [BF03-DEMO-03] ---------- */
    DECLARE @articleId INT = (SELECT id FROM articles
                              WHERE title = N'[BF03-DEMO-03] Lớp học miễn phí giúp người cao tuổi sử dụng dịch vụ công');
    IF @articleId IS NULL
        THROW 51502, N'Không thấy bài [BF03-DEMO-03] của data.sql.', 1;

    IF NOT EXISTS (SELECT 1 FROM appeals WHERE article_id = @articleId)
    BEGIN
        -- S0: lần chấm INITIAL đã duyệt + công bố của bài
        DECLARE @s0 INT, @versionId INT, @assignmentId INT, @setId INT, @categoryId INT, @announcedAt DATETIME2;
        SELECT TOP (1) @s0 = a.id, @versionId = a.version_id, @assignmentId = a.assignment_id,
                       @setId = a.criteria_set_version_id, @categoryId = a.category_id, @announcedAt = p.announced_at
        FROM scoring_attempts a JOIN score_approvals p ON p.scoring_attempt_id = a.id
        WHERE a.article_id = @articleId AND a.attempt_type = 'INITIAL' AND p.decision = 'APPROVED'
        ORDER BY a.attempt_no DESC;
        IF @s0 IS NULL
            THROW 51503, N'Bài [BF03-DEMO-03] chưa có lần chấm đã công bố.', 1;

        -- Khiếu nại gửi 10 phút sau khi công bố điểm (còn trong hạn 7 ngày)
        DECLARE @submittedAt DATETIME2 = DATEADD(MINUTE, 10, @announcedAt);
        DECLARE @resolvedAt DATETIME2 = DATEADD(MINUTE, 40, @announcedAt);
        DECLARE @appealId INT, @s1 INT;

        INSERT INTO appeals (article_id, scoring_attempt_id, appellant_id, rule_config_id, status, submitted_at, resolved_at)
        VALUES (@articleId, @s0, @an, @ruleId, 'RESOLVED', @submittedAt, @resolvedAt);
        SET @appealId = CAST(SCOPE_IDENTITY() AS INT);

        -- Tiêu chí bị khiếu nại: tiêu chí đầu tiên của bộ (Tính phát hiện, độc quyền)
        INSERT INTO appeal_criteria (appeal_id, criterion_id, reason)
        SELECT TOP (1) @appealId, id, N'Bài là phóng sự đầu tiên ghi nhận lớp học này, đề nghị xem lại tính phát hiện.'
        FROM criteria WHERE criteria_set_version_id = @setId ORDER BY sort_order;

        -- S1: lần chấm mù do khiếu nại, cùng bản bài, thể loại và bộ tiêu chí với S0 (BR-QC-05).
        -- APPEAL chỉ chấm điểm, không có quyết định xuất bản (ck_scoring_attempts_submitted).
        INSERT INTO scoring_attempts (article_id, version_id, assignment_id, appeal_id, reviewer_id, attempt_no,
                                      attempt_type, criteria_set_version_id, category_id, total_score, decision,
                                      opened_at, submitted_at)
        VALUES (@articleId, @versionId, @assignmentId, @appealId, @blind,
                (SELECT MAX(attempt_no) + 1 FROM scoring_attempts WHERE article_id = @articleId),
                'APPEAL', @setId, @categoryId, 85.50, NULL,
                DATEADD(MINUTE, 15, @announcedAt), DATEADD(MINUTE, 30, @announcedAt));
        SET @s1 = CAST(SCOPE_IDENTITY() AS INT);

        -- Bộ tiêu chí Bài phản ánh 30/20/30/20: 80*30% + 90*20% + 85*30% + 90*20% = 24 + 18 + 25.5 + 18 = 85.50
        INSERT INTO scoring_attempt_details (scoring_attempt_id, criterion_id, score, comment)
        SELECT @s1, id,
               CASE sort_order WHEN 1 THEN 80 WHEN 2 THEN 90 WHEN 3 THEN 85 ELSE 90 END,
               NULL
        FROM criteria WHERE criteria_set_version_id = @setId;

        -- |85.50 - 82.00| = 3.50 <= 5 -> dùng S1 (BR-QC-06), Trưởng ban xác nhận
        INSERT INTO appeal_resolutions (appeal_id, rule_applied, effective_attempt_id, effective_score, finalized_by, finalized_at)
        VALUES (@appealId, 'USE_S1', @s1, 85.50, @duc, @resolvedAt);

        INSERT INTO audit_logs (actor_id, action_type, entity_type, entity_id, old_value, new_value, created_at)
        VALUES (@an,  'SUBMIT',   'appeals', @appealId, NULL, N'{"status":"SUBMITTED"}', @submittedAt),
               (@duc, 'FINALIZE', 'appeals', @appealId, N'{"status":"IN_REVIEW"}',
                N'{"status":"RESOLVED","effectiveScore":85.50,"rule":"USE_S1","downstreamStatus":"EFFECTIVE_SCORE_READY"}', @resolvedAt);

        INSERT INTO notifications (user_id, article_id, message, created_at)
        VALUES (@an, @articleId, N'Khiếu nại bài [BF03-DEMO-03] đã được xác nhận. Điểm hiệu lực: 85.50.', @resolvedAt);
    END;

    /* ---------- B. Chỉ tiêu KPI tháng 10 và 11/2026 ---------- */
    DECLARE @month TINYINT = 10;
    WHILE @month <= 11
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM kpi_targets WHERE reporter_id = @an AND target_year = 2026 AND target_month = @month)
            INSERT INTO kpi_targets (reporter_id, target_year, target_month, min_published, min_avg_score, set_by)
            VALUES (@an, 2026, @month, 2, 75.00, @duc);
        IF NOT EXISTS (SELECT 1 FROM kpi_targets WHERE reporter_id = @binh AND target_year = 2026 AND target_month = @month)
            INSERT INTO kpi_targets (reporter_id, target_year, target_month, min_published, min_avg_score, set_by)
            VALUES (@binh, 2026, @month, 1, 70.00, @duc);
        IF NOT EXISTS (SELECT 1 FROM kpi_targets WHERE reporter_id = @hung AND target_year = 2026 AND target_month = @month)
            INSERT INTO kpi_targets (reporter_id, target_year, target_month, min_published, min_avg_score, set_by)
            VALUES (@hung, 2026, @month, 2, 70.00, @duc);
        SET @month = @month + 1;
    END;

    COMMIT TRANSACTION;

    SELECT a.id AS appeal_id, a.status, r.rule_applied, r.effective_score
    FROM appeals a JOIN appeal_resolutions r ON r.appeal_id = a.id
    WHERE a.article_id = @articleId;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO

PRINT N'Nạp dữ liệu demo 04/11 thành công.';
GO
