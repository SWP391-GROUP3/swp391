-- noinspection SqlNoDataSourceInspectionForFile
-- BF03 Iter3: cap nhat CHECK, giu nguyen bang va du lieu. Khong chay schema.sql/data.sql.
-- Duoc phe duyet ngay 26/10/2026. Chay trong swp391_kpi, co the chay lai.
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;
BEGIN TRY
    BEGIN TRANSACTION;
    IF DB_NAME() <> N'swp391_kpi'
        THROW 51000, N'Sai database. Migration chi ap dung cho swp391_kpi.', 1;
    IF OBJECT_ID(N'dbo.scoring_attempts', N'U') IS NULL
       OR OBJECT_ID(N'dbo.appeal_resolutions', N'U') IS NULL
       OR COL_LENGTH(N'dbo.appeal_resolutions', N'effective_score') IS NULL
        THROW 51001, N'Thieu bang/cot nen tang BF03. Khong thay doi du lieu.', 1;
    IF OBJECT_ID(N'dbo.ck_scoring_attempts_submitted', N'C') IS NULL
       OR OBJECT_ID(N'dbo.ck_appeal_resolutions_rule', N'C') IS NULL
        THROW 51002, N'CHECK hien tai khong dung ten trong schema.sql. Can kiem tra truoc.', 1;

    ALTER TABLE dbo.scoring_attempts DROP CONSTRAINT ck_scoring_attempts_submitted;
    ALTER TABLE dbo.scoring_attempts WITH CHECK ADD CONSTRAINT ck_scoring_attempts_submitted
        CHECK (submitted_at IS NULL OR (total_score IS NOT NULL
               AND (attempt_type = 'APPEAL' OR decision IS NOT NULL)));
    ALTER TABLE dbo.appeal_resolutions DROP CONSTRAINT ck_appeal_resolutions_rule;
    ALTER TABLE dbo.appeal_resolutions WITH CHECK ADD CONSTRAINT ck_appeal_resolutions_rule
        CHECK (rule_applied IN ('KEEP_ORIGINAL','USE_RESCORE','USE_S1',
                               'AVERAGE_S0_S2','AVERAGE_S1_S2','USE_S2_TIE'));
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
