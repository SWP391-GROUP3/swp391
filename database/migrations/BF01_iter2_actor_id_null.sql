-- noinspection SqlNoDataSourceInspectionForFile
-- BF01 Iter2: article_status_logs.actor_id cho phep NULL (NULL = job he thong tu chay,
-- vd qua han 3 ngay xac nhan dong tac gia - UC-12 A2). Giong schema.sql tu PR #39.
-- Nhom truong dong y ngay 24/10/2026 (A6). Giu nguyen bang va du lieu, khong chay schema.sql/data.sql.
-- Chay trong swp391_kpi, co the chay lai (cot da cho phep NULL thi khong lam gi).
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;
BEGIN TRY
    BEGIN TRANSACTION;
    IF DB_NAME() <> N'swp391_kpi'
        THROW 51000, N'Sai database. Migration chi ap dung cho swp391_kpi.', 1;
    IF COL_LENGTH(N'dbo.article_status_logs', N'actor_id') IS NULL
        THROW 51001, N'Thieu bang article_status_logs hoac cot actor_id. Khong thay doi du lieu.', 1;

    -- Chi doi khi cot con NOT NULL; khoa ngoai fk_article_status_logs_actor giu nguyen
    IF COLUMNPROPERTY(OBJECT_ID(N'dbo.article_status_logs'), N'actor_id', 'AllowsNull') = 0
        ALTER TABLE dbo.article_status_logs ALTER COLUMN actor_id INT NULL;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
