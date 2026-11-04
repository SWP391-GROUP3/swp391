-- noinspection SqlNoDataSourceInspectionForFile
-- BF03: them rule MEDIAN_S0_S1_S2 (trung vi S0, S1, S2) vao CHECK cua appeal_resolutions.
-- Giu nguyen bang va du lieu. Khong chay schema.sql/data.sql. CHO CHU DB DUYET truoc khi chay.
-- Chay trong swp391_kpi, co the chay lai.
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;
BEGIN TRY
    BEGIN TRANSACTION;
    IF DB_NAME() <> N'swp391_kpi'
        THROW 51000, N'Sai database. Migration chi ap dung cho swp391_kpi.', 1;
    IF OBJECT_ID(N'dbo.appeal_resolutions', N'U') IS NULL
       OR OBJECT_ID(N'dbo.ck_appeal_resolutions_rule', N'C') IS NULL
        THROW 51001, N'Thieu bang hoac CHECK hien tai. Khong thay doi du lieu.', 1;

    ALTER TABLE dbo.appeal_resolutions DROP CONSTRAINT ck_appeal_resolutions_rule;
    ALTER TABLE dbo.appeal_resolutions WITH CHECK ADD CONSTRAINT ck_appeal_resolutions_rule
        CHECK (rule_applied IN ('KEEP_ORIGINAL','USE_RESCORE','USE_S1','MEDIAN_S0_S1_S2',
                               'AVERAGE_S0_S2','AVERAGE_S1_S2','USE_S2_TIE'));
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
