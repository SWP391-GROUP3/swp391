-- noinspection SqlNoDataSourceInspectionForFile
-- BF04 Iter3: them 1 su kien mau con mo toi het nam 2026, giong data.sql (A7 file BF-01 ngay 01/11).
-- Su kien "Bao so 3" da ket thuc 20/09 nen GET /api/events/open tra rong, khong demo duoc chon su kien.
-- k_limit = 2 cho de thay bao hoa: bai 1-2 -> 100%, 3-4 -> 80%, tu bai 5 -> 60% (GBR-09).
-- Giu nguyen bang va du lieu, khong chay schema.sql/data.sql.
-- Chay trong swp391_kpi, co the chay lai (su kien da co thi khong them nua).
SET XACT_ABORT ON;
SET LOCK_TIMEOUT 15000;
BEGIN TRY
    BEGIN TRANSACTION;
    IF DB_NAME() <> N'swp391_kpi'
        THROW 51000, N'Sai database. Migration chi ap dung cho swp391_kpi.', 1;
    IF OBJECT_ID(N'dbo.events', N'U') IS NULL
        THROW 51001, N'Thieu bang events. Khong thay doi du lieu.', 1;
    -- created_by = 4 (Thu ky toa soan dung.pt trong data.sql), giong su kien mau so 1
    IF NOT EXISTS (SELECT 1 FROM dbo.users WHERE id = 4)
        THROW 51002, N'Chua co user id 4 (data.sql). Khong thay doi du lieu.', 1;

    IF NOT EXISTS (SELECT 1 FROM dbo.events WHERE name = N'Chuyển đổi số quốc gia 2026')
        INSERT INTO dbo.events (name, start_date, end_date, window_days, k_limit, exempt_score, created_by)
        VALUES (N'Chuyển đổi số quốc gia 2026', '2026-10-25', '2026-12-31', 7, 2, 90, 4);
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
