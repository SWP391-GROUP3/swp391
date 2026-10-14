-- noinspection SqlNoDataSourceInspectionForFile
/*
    BF-03 Iter1 safe migration.
    Chi bo sung cot va rang buoc cho BF-03, khong xoa bang hoac du lieu.
    Co the chay lai: moi thay doi deu kiem tra ton tai truoc khi thuc hien.
*/

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.appeals', N'U') IS NULL
        THROW 51000, N'Không tìm thấy bảng dbo.appeals.', 1;

    IF OBJECT_ID(N'dbo.appeal_rule_configs', N'U') IS NULL
        THROW 51001, N'Không tìm thấy bảng dbo.appeal_rule_configs.', 1;

    IF EXISTS (
        SELECT article_id
        FROM dbo.appeals
        GROUP BY article_id
        HAVING COUNT(*) > 1
    )
        THROW 51002, N'Đang có nhiều khiếu nại cho cùng một bài viết. Migration đã dừng và không thay đổi dữ liệu.', 1;

    IF EXISTS (
        SELECT effective_from
        FROM dbo.appeal_rule_configs
        GROUP BY effective_from
        HAVING COUNT(*) > 1
    )
        THROW 51003, N'Đang có nhiều cấu hình cùng ngày hiệu lực. Migration đã dừng và không thay đổi dữ liệu.', 1;

    IF COL_LENGTH(N'dbo.appeals', N'rule_config_id') IS NULL
        ALTER TABLE dbo.appeals ADD rule_config_id INT NULL;

    /* Gan cau hinh co hieu luc tai luc gui de khong thay doi luat cua appeal cu. */
    EXEC sys.sp_executesql N'
        UPDATE appeal
        SET rule_config_id = selected_config.id
        FROM dbo.appeals appeal
        OUTER APPLY (
            SELECT TOP (1) config.id
            FROM dbo.appeal_rule_configs config
            WHERE config.is_active = 1
              AND config.effective_from <= CAST(appeal.submitted_at AS DATE)
            ORDER BY config.effective_from DESC, config.id DESC
        ) selected_config
        WHERE appeal.rule_config_id IS NULL;';

    DECLARE @missing_rule_config_count INT;
    EXEC sys.sp_executesql
        N'SELECT @count = COUNT(*) FROM dbo.appeals WHERE rule_config_id IS NULL;',
        N'@count INT OUTPUT',
        @count = @missing_rule_config_count OUTPUT;

    IF @missing_rule_config_count > 0
        THROW 51004, N'Có khiếu nại chưa tìm được cấu hình lịch sử phù hợp. Migration đã rollback.', 1;

    IF EXISTS (
        SELECT 1
        FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.appeals')
          AND name = N'rule_config_id'
          AND is_nullable = 1
    )
        EXEC sys.sp_executesql N'ALTER TABLE dbo.appeals ALTER COLUMN rule_config_id INT NOT NULL;';

    IF OBJECT_ID(N'dbo.fk_appeals_rule_config', N'F') IS NULL
        EXEC sys.sp_executesql N'
            ALTER TABLE dbo.appeals
                ADD CONSTRAINT fk_appeals_rule_config
                FOREIGN KEY (rule_config_id) REFERENCES dbo.appeal_rule_configs(id);';

    IF NOT EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.appeals')
          AND name = N'uq_appeals_article'
    )
        ALTER TABLE dbo.appeals
            ADD CONSTRAINT uq_appeals_article UNIQUE (article_id);

    IF NOT EXISTS (
        SELECT 1
        FROM sys.key_constraints
        WHERE parent_object_id = OBJECT_ID(N'dbo.appeal_rule_configs')
          AND name = N'uq_appeal_rule_configs_effective'
    )
        ALTER TABLE dbo.appeal_rule_configs
            ADD CONSTRAINT uq_appeal_rule_configs_effective UNIQUE (effective_from);

    COMMIT TRANSACTION;
    PRINT N'BF-03 Iter1 migration hoàn tất; không có bảng hoặc dữ liệu nào bị xóa.';
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
