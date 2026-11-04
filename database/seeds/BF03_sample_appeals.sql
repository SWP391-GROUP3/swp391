-- noinspection SqlNoDataSourceInspectionForFile
-- Dev-only BF-03 fixtures: 3 khieu nai mau tren 3 bai [BF03-DEMO-01..03] (xem BF03_sample_appealable_articles.sql).
-- Chay: sqlcmd -S localhost,1433 -d swp391_kpi -E -C -f 65001 -b -i database\seeds\BF03_sample_appeals.sql
-- Khong reset du lieu; bai da co khieu nai thi bo qua. Can da chay migration BF03_median_rule.sql.
--   DEMO-01  SUBMITTED  cho Truong ban phan cong S1.
--   DEMO-02  IN_REVIEW  S0=77, S1=95 (lech 18 > T) -> man so sanh hien khoang 77..95, cho giao S2.
--   DEMO-03  RESOLVED   S0=82, S1=98, S2=90 -> trung vi 90 (MEDIAN_S0_S1_S2).
-- Khieu nai dau tien lay id=1 neu id 1 con trong, vi BF03Iteration1IT va BF03AppealManagementIT doc cung appeals.id=1.
SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    DECLARE @an INT   = (SELECT id FROM users WHERE username = 'an.nv');
    DECLARE @duc INT  = (SELECT id FROM users WHERE username = 'duc.vm');
    DECLARE @bt1 INT  = (SELECT id FROM users WHERE username = 'test.bt1');
    DECLARE @bt2 INT  = (SELECT id FROM users WHERE username = 'test.bt2');
    IF @an IS NULL OR @duc IS NULL OR @bt1 IS NULL OR @bt2 IS NULL
        THROW 51600, N'Thieu tai khoan mau (an.nv, duc.vm, test.bt1, test.bt2).', 1;
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
                   WHERE name = 'ck_appeal_resolutions_rule' AND definition LIKE '%MEDIAN_S0_S1_S2%')
        THROW 51601, N'Chua chay database/migrations/BF03_median_rule.sql.', 1;
    DECLARE @ruleId INT = (SELECT TOP (1) id FROM appeal_rule_configs
                           WHERE is_active = 1 AND effective_from <= CAST(SYSDATETIME() AS DATE)
                           ORDER BY effective_from DESC);
    IF @ruleId IS NULL
        THROW 51602, N'Chua co cau hinh khieu nai dang hieu luc.', 1;

    DECLARE @samples TABLE(seq INT PRIMARY KEY, title NVARCHAR(300), state VARCHAR(20),
                           s1 DECIMAL(5,2) NULL, s2 DECIMAL(5,2) NULL, reason NVARCHAR(500));
    INSERT INTO @samples VALUES
    (1, N'[BF03-DEMO-01] Người trẻ chung tay làm sạch kênh sau mưa lớn', 'SUBMITTED', NULL, NULL,
        N'Bài ghi nhận hiện trường trong điều kiện mưa lớn, đề nghị xem lại tiêu chí độ khó tác nghiệp.'),
    (2, N'[BF03-DEMO-02] Chợ dân sinh ứng dụng thanh toán không tiền mặt', 'IN_REVIEW', 95, NULL,
        N'Bài có số liệu khảo sát và dẫn chứng cụ thể, đề nghị xem lại tiêu chí chất lượng trình bày.'),
    (3, N'[BF03-DEMO-03] Lớp học miễn phí giúp người cao tuổi sử dụng dịch vụ công', 'RESOLVED', 98, 90,
        N'Bài có phỏng vấn học viên và người hướng dẫn, đề nghị xem lại tiêu chí tính thời sự.');

    DECLARE @seq INT = 1, @created INT = 0;
    DECLARE @title NVARCHAR(300), @state VARCHAR(20), @s1Score DECIMAL(5,2), @s2Score DECIMAL(5,2), @reason NVARCHAR(500);
    DECLARE @articleId INT, @s0 INT, @versionId INT, @assignmentId INT, @setId INT, @categoryId INT;
    DECLARE @announcedAt DATETIME2, @s0Score DECIMAL(5,2), @appealId INT, @s1 INT, @s2 INT;
    DECLARE @submittedAt DATETIME2, @resolvedAt DATETIME2, @median DECIMAL(5,2);

    WHILE @seq <= 3
    BEGIN
        SELECT @title = title, @state = state, @s1Score = s1, @s2Score = s2, @reason = reason
        FROM @samples WHERE seq = @seq;
        SET @articleId = (SELECT id FROM articles WHERE title = @title);
        IF @articleId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM appeals WHERE article_id = @articleId)
        BEGIN
            SELECT TOP (1) @s0 = a.id, @versionId = a.version_id, @assignmentId = a.assignment_id,
                           @setId = a.criteria_set_version_id, @categoryId = a.category_id,
                           @announcedAt = p.announced_at, @s0Score = a.total_score
            FROM scoring_attempts a JOIN score_approvals p ON p.scoring_attempt_id = a.id
            WHERE a.article_id = @articleId AND a.attempt_type = 'INITIAL' AND p.decision = 'APPROVED'
            ORDER BY a.attempt_no DESC;
            IF @s0 IS NULL
                THROW 51603, N'Bai mau chua co lan cham da cong bo.', 1;

            SET @submittedAt = DATEADD(MINUTE, 2, @announcedAt);
            SET @resolvedAt = CASE WHEN @state = 'RESOLVED' THEN DATEADD(MINUTE, 10, @announcedAt) END;

            IF @seq = 1 AND NOT EXISTS (SELECT 1 FROM appeals WHERE id = 1)
            BEGIN
                SET IDENTITY_INSERT appeals ON;
                INSERT INTO appeals (id, article_id, scoring_attempt_id, appellant_id, rule_config_id, status, submitted_at, resolved_at)
                VALUES (1, @articleId, @s0, @an, @ruleId, @state, @submittedAt, @resolvedAt);
                SET IDENTITY_INSERT appeals OFF;
                SET @appealId = 1;
            END
            ELSE
            BEGIN
                INSERT INTO appeals (article_id, scoring_attempt_id, appellant_id, rule_config_id, status, submitted_at, resolved_at)
                VALUES (@articleId, @s0, @an, @ruleId, @state, @submittedAt, @resolvedAt);
                SET @appealId = CAST(SCOPE_IDENTITY() AS INT);
            END;

            INSERT INTO appeal_criteria (appeal_id, criterion_id, reason)
            SELECT TOP (1) @appealId, id, @reason FROM criteria WHERE criteria_set_version_id = @setId ORDER BY sort_order;
            INSERT INTO audit_logs (actor_id, action_type, entity_type, entity_id, old_value, new_value, created_at)
            VALUES (@an, 'SUBMIT', 'appeals', @appealId, NULL, N'{"status":"SUBMITTED"}', @submittedAt);
            INSERT INTO notifications (user_id, article_id, message, created_at)
            SELECT user_id, @articleId, CONCAT(N'Có khiếu nại mới #', @appealId, N' về điểm bài viết #', @articleId, N'.'), @submittedAt
            FROM (SELECT @duc AS user_id) h;

            -- S1: chi tao khi khieu nai da duoc phan cong. Moi tieu chi cung mot diem nen tong = diem do (trong so cong 100).
            IF @s1Score IS NOT NULL
            BEGIN
                INSERT INTO scoring_attempts (article_id, version_id, assignment_id, appeal_id, reviewer_id, attempt_no,
                                              attempt_type, criteria_set_version_id, category_id, total_score, decision,
                                              opened_at, submitted_at)
                VALUES (@articleId, @versionId, @assignmentId, @appealId, @bt1,
                        (SELECT MAX(attempt_no) + 1 FROM scoring_attempts WHERE article_id = @articleId),
                        'APPEAL', @setId, @categoryId, @s1Score, NULL,
                        DATEADD(MINUTE, 4, @announcedAt), DATEADD(MINUTE, 6, @announcedAt));
                SET @s1 = CAST(SCOPE_IDENTITY() AS INT);
                INSERT INTO scoring_attempt_details (scoring_attempt_id, criterion_id, score, comment)
                SELECT @s1, id, @s1Score, NULL FROM criteria WHERE criteria_set_version_id = @setId;
                INSERT INTO audit_logs (actor_id, action_type, entity_type, entity_id, old_value, new_value, created_at)
                VALUES (@duc, 'ASSIGN', 'appeals', @appealId, N'{"status":"SUBMITTED"}',
                        CONCAT(N'{"status":"IN_REVIEW","attemptId":', @s1, N',"reviewerId":', @bt1,
                               N',"crossDepartment":false,"reason":null}'), DATEADD(MINUTE, 3, @announcedAt));
            END;

            -- S2: chi co khi khieu nai da RESOLVED (S1 lech qua T nen phai co nguoi thu ba cham).
            IF @s2Score IS NOT NULL
            BEGIN
                INSERT INTO scoring_attempts (article_id, version_id, assignment_id, appeal_id, reviewer_id, attempt_no,
                                              attempt_type, criteria_set_version_id, category_id, total_score, decision,
                                              opened_at, submitted_at)
                VALUES (@articleId, @versionId, @assignmentId, @appealId, @bt2,
                        (SELECT MAX(attempt_no) + 1 FROM scoring_attempts WHERE article_id = @articleId),
                        'APPEAL', @setId, @categoryId, @s2Score, NULL,
                        DATEADD(MINUTE, 7, @announcedAt), DATEADD(MINUTE, 9, @announcedAt));
                SET @s2 = CAST(SCOPE_IDENTITY() AS INT);
                INSERT INTO scoring_attempt_details (scoring_attempt_id, criterion_id, score, comment)
                SELECT @s2, id, @s2Score, NULL FROM criteria WHERE criteria_set_version_id = @setId;
                INSERT INTO audit_logs (actor_id, action_type, entity_type, entity_id, old_value, new_value, created_at)
                VALUES (@duc, 'ASSIGN', 'appeals', @appealId, NULL,
                        CONCAT(N'{"status":"IN_REVIEW","attemptId":', @s2, N',"reviewerId":', @bt2,
                               N',"crossDepartment":false,"reason":null}'), DATEADD(MINUTE, 6, @announcedAt));

                -- Trung vi cua S0, S1, S2 = S2 ke vao khoang [min(S0,S1), max(S0,S1)] (BR-QC-07)
                SET @median = CASE WHEN @s2Score < CASE WHEN @s0Score < @s1Score THEN @s0Score ELSE @s1Score END
                                        THEN CASE WHEN @s0Score < @s1Score THEN @s0Score ELSE @s1Score END
                                   WHEN @s2Score > CASE WHEN @s0Score > @s1Score THEN @s0Score ELSE @s1Score END
                                        THEN CASE WHEN @s0Score > @s1Score THEN @s0Score ELSE @s1Score END
                                   ELSE @s2Score END;
                INSERT INTO appeal_resolutions (appeal_id, rule_applied, effective_attempt_id, effective_score, finalized_by, finalized_at)
                VALUES (@appealId, 'MEDIAN_S0_S1_S2', @s2, @median, @duc, @resolvedAt);
                INSERT INTO audit_logs (actor_id, action_type, entity_type, entity_id, old_value, new_value, created_at)
                VALUES (@duc, 'FINALIZE', 'appeals', @appealId, N'{"status":"IN_REVIEW"}',
                        CONCAT(N'{"status":"RESOLVED","effectiveScore":', @median,
                               N',"rule":"MEDIAN_S0_S1_S2","downstreamStatus":"EFFECTIVE_SCORE_READY"}'), @resolvedAt);
                INSERT INTO notifications (user_id, article_id, message, created_at)
                VALUES (@an, @articleId, CONCAT(N'Khiếu nại #', @appealId, N' đã được xác nhận. Điểm hiệu lực: ', @median, N'.'), @resolvedAt);
            END;
            SET @created = @created + 1;
        END;
        SET @seq = @seq + 1;
        SET @s0 = NULL; SET @s1 = NULL; SET @s2 = NULL;
    END;
    COMMIT TRANSACTION;

    SELECT @created AS newly_created_appeals;
    SELECT a.id AS appeal_id, a.status, o.total_score AS S0,
           (SELECT STRING_AGG(CONCAT('phieu ', x.id, '=', x.total_score), ', ') WITHIN GROUP (ORDER BY x.attempt_no)
            FROM scoring_attempts x WHERE x.appeal_id = a.id) AS rescores,
           r.rule_applied, r.effective_score
    FROM appeals a JOIN scoring_attempts o ON o.id = a.scoring_attempt_id
    LEFT JOIN appeal_resolutions r ON r.appeal_id = a.id
    ORDER BY a.id;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
