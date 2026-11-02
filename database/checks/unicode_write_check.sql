-- noinspection SqlNoDataSourceInspectionForFile
-- Luu file bang UTF-8. Chi thu tren bien bang, khong sua du lieu that.
USE swp391_kpi;
SET NOCOUNT ON;

DECLARE @expected NVARCHAR(200) = CONVERT(NVARCHAR(200),
    0x54006900BF1E6E006700200056006900C71E74003A0020004E00670075007900C51E6E002000560003016E00200041006E002C0020001001B71E6E0067002C00200054007200B001DF1E6E0067002000620061006E002C0020006B0068006900BF1E750020006E00A11E6900);
DECLARE @probe TABLE (content NVARCHAR(200));

-- Tien to N giu Unicode truoc khi SQL Server gan gia tri vao cot NVARCHAR.
INSERT INTO @probe (content)
VALUES (N'Tiếng Việt: Nguyễn Văn An, Đặng, Trưởng ban, khiếu nại');

IF EXISTS (
    SELECT 1 FROM @probe
    WHERE CONVERT(VARBINARY(MAX), content) <> CONVERT(VARBINARY(MAX), @expected)
)
    THROW 51120, 'Unicode check failed. Reopen this SQL file with UTF-8 encoding.', 1;

SELECT content AS unicode_roundtrip, N'PASS' AS result FROM @probe;

-- Mau ghi dung cho cot NVARCHAR: VALUES (N'Noi dung tieng Viet co dau').
-- Trong Java: dung tham so PreparedStatement/JPA, khong noi chuoi SQL.
