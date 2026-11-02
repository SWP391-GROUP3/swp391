-- noinspection SqlNoDataSourceInspectionForFile
-- BF04 Iter3: them 2 tham so ty le bao hoa su kien (GBR-09, BR-IN-03), giong data.sql.
-- Nhom chot 01/11/2026: moi bac k_limit bai. Hang 1..k -> 100%, k+1..2k -> 80%, tu 2k+1 -> 60%.
-- Giu nguyen bang va du lieu, khong chay schema.sql/data.sql.
-- Chay trong swp391_kpi, co the chay lai (tham so da co thi khong them nua).
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;
BEGIN TRY
    BEGIN TRANSACTION;
    IF DB_NAME() <> N'swp391_kpi'
        THROW 51000, N'Sai database. Migration chi ap dung cho swp391_kpi.', 1;
    IF OBJECT_ID(N'dbo.system_parameters', N'U') IS NULL
        THROW 51001, N'Thieu bang system_parameters. Khong thay doi du lieu.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.system_parameters WHERE param_key = 'SATURATION_RATE_LEVEL_2')
        INSERT INTO dbo.system_parameters (param_key, param_value, unit, flow)
        VALUES ('SATURATION_RATE_LEVEL_2', N'80', N'%', 'BF-04');
    IF NOT EXISTS (SELECT 1 FROM dbo.system_parameters WHERE param_key = 'SATURATION_RATE_LEVEL_3')
        INSERT INTO dbo.system_parameters (param_key, param_value, unit, flow)
        VALUES ('SATURATION_RATE_LEVEL_3', N'60', N'%', 'BF-04');
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
