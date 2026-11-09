-- noinspection SqlNoDataSourceInspectionForFile
-- BF05: kpi_snapshots.actual_articles doi sang DECIMAL(5,2) de luu dung so bai quy doi dong tac gia (GBR-13).
-- Giu nguyen bang va du lieu hien co.
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;
BEGIN TRY
    BEGIN TRANSACTION;
    IF DB_NAME() <> N'swp391_kpi'
        THROW 51000, N'Sai database. Migration chi ap dung cho swp391_kpi.', 1;
    IF COL_LENGTH(N'dbo.kpi_snapshots', N'actual_articles') IS NULL
        THROW 51001, N'Thieu bang kpi_snapshots hoac cot actual_articles. Khong thay doi du lieu.', 1;

    ALTER TABLE dbo.kpi_snapshots ALTER COLUMN actual_articles DECIMAL(5,2) NOT NULL;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
