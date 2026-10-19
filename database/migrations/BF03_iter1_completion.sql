-- noinspection SqlNoDataSourceInspectionForFile
-- Chi them cot diem so hieu luc, giu nguyen cac ban ghi va rang buoc cu.
SET XACT_ABORT ON;
BEGIN TRY
    BEGIN TRANSACTION;
    IF OBJECT_ID(N'dbo.appeal_resolutions', N'U') IS NULL
        THROW 51000, N'Khong tim thay bang appeal_resolutions.', 1;
    IF COL_LENGTH(N'dbo.appeal_resolutions', N'effective_score') IS NULL
        ALTER TABLE dbo.appeal_resolutions ADD effective_score DECIMAL(5,2) NULL;
    IF OBJECT_ID(N'dbo.ck_appeal_resolutions_score', N'C') IS NULL
        EXEC sys.sp_executesql N'ALTER TABLE dbo.appeal_resolutions
            ADD CONSTRAINT ck_appeal_resolutions_score CHECK (effective_score BETWEEN 0 AND 100);';
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
