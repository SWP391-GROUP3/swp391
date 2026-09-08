/* ==========================================================================
   SWP391 - Dữ liệu mẫu
   Chạy SAU schema.sql. Chỉ dùng để dev và demo.

   LƯU Ý mật khẩu: cột matKhau phải chứa BCrypt hash, không phải plain text.
   Chuỗi bên dưới là PLACEHOLDER. Người phụ trách auth thay bằng hash thật
   (BCryptPasswordEncoder.encode("123456")) khi làm chức năng đăng nhập.
   ========================================================================== */

USE swp391_kpi;
GO

/* ---------- Cụm 1: nhân sự & tổ chức ---------- */

INSERT INTO Ban (ma, ten) VALUES
    ('THOISU',  N'Ban Thời sự'),
    ('KINHTE',  N'Ban Kinh tế'),
    ('VANHOA',  N'Ban Văn hoá - Xã hội'),
    ('THETHAO', N'Ban Thể thao');

INSERT INTO ChuyenMuc (ma, ten) VALUES
    ('THOISU',   N'Thời sự'),
    ('KINHTE',   N'Kinh tế'),
    ('XAHOI',    N'Xã hội'),
    ('THETHAO',  N'Thể thao'),
    ('GIAITRI',  N'Giải trí');

INSERT INTO VaiTro (ma, ten) VALUES
    ('PHONG_VIEN',        N'Phóng viên'),
    ('BIEN_TAP_VIEN',     N'Biên tập viên'),
    ('THU_KY_TOA_SOAN',   N'Thư ký toà soạn'),
    ('TONG_BIEN_TAP',     N'Tổng biên tập'),
    ('KE_TOAN',           N'Kế toán'),
    ('QUAN_TRI',          N'Quản trị hệ thống');

INSERT INTO NguoiDung (maNV, hoTen, email, matKhau, banId, laCongTacVien) VALUES
    ('NV001', N'Nguyễn Văn An',   'an.nv@toasoan.vn',   'REPLACE_WITH_BCRYPT_HASH', 1, 0),
    ('NV002', N'Trần Thị Bình',   'binh.tt@toasoan.vn', 'REPLACE_WITH_BCRYPT_HASH', 1, 0),
    ('NV003', N'Lê Hoàng Cường',  'cuong.lh@toasoan.vn','REPLACE_WITH_BCRYPT_HASH', 2, 0),
    ('NV004', N'Phạm Thu Dung',   'dung.pt@toasoan.vn', 'REPLACE_WITH_BCRYPT_HASH', 3, 0),
    ('NV005', N'Vũ Minh Đức',     'duc.vm@toasoan.vn',  'REPLACE_WITH_BCRYPT_HASH', 1, 0),
    ('NV006', N'Hoàng Lan Chi',   'chi.hl@toasoan.vn',  'REPLACE_WITH_BCRYPT_HASH', 2, 0),
    ('CTV01', N'Đỗ Quốc Hưng',    'hung.dq@gmail.com',  'REPLACE_WITH_BCRYPT_HASH', NULL, 1);

/* NV001-002: phóng viên | NV003: BTV | NV004: thư ký toà soạn
   NV005: tổng biên tập  | NV006: kế toán + quản trị | CTV01: cộng tác viên */
INSERT INTO NguoiDung_VaiTro (nguoiDungId, vaiTroId) VALUES
    (1,1), (2,1), (3,2), (4,3), (5,4), (6,5), (6,6), (7,1);

/* ---------- Cụm 2: thể loại ---------- */

INSERT INTO TheLoai (ma, ten, laTin) VALUES
    ('TIN_VAN',      N'Tin vắn',              1),
    ('TIN_SAU',      N'Tin sâu',              1),
    ('PHAN_ANH',     N'Bài phản ánh',         0),
    ('PHONG_VAN',    N'Bài phỏng vấn',        0),
    ('PHONG_SU',     N'Phóng sự điều tra',    0),
    ('ANH_BAO_CHI',  N'Ảnh báo chí',          0),
    ('INFOGRAPHIC',  N'Infographic',          0);

/* ---------- Cụm 3: tiêu chí chấm điểm (tổng trọng số = 1) ---------- */

INSERT INTO TieuChiChamDiem (ten, trongSo) VALUES
    (N'Tính phát hiện, độc quyền',  0.3000),
    (N'Độ khó khi tác nghiệp',      0.2000),
    (N'Chất lượng viết, trình bày', 0.3000),
    (N'Tính thời sự',               0.2000);

/* ---------- Cụm 4: barem áp dụng từ 01/2026 ---------- */

INSERT INTO BaremNhuanBut (ten, hieuLucTu, hieuLucDen, giaTriDonViHeSo, trangThai)
VALUES (N'Barem nhuận bút 2026', '2026-01-01', NULL, 150000, 'DA_BAN_HANH');

INSERT INTO HeSoTheLoai (baremId, theLoaiId, heSo) VALUES
    (1, 1,  1.0000),   -- Tin vắn
    (1, 2,  2.0000),   -- Tin sâu
    (1, 3,  5.0000),   -- Bài phản ánh
    (1, 4,  6.0000),   -- Bài phỏng vấn
    (1, 5, 10.0000),   -- Phóng sự điều tra
    (1, 6,  3.0000),   -- Ảnh báo chí
    (1, 7,  4.0000);   -- Infographic

INSERT INTO HeSoChatLuong (baremId, xepLoai, heSo) VALUES
    (1, 'A', 1.3000),
    (1, 'B', 1.0000),
    (1, 'C', 0.7000),
    (1, 'D', 0.4000);

INSERT INTO HeSoViTri (baremId, viTriDang, heSo) VALUES
    (1, 'TRANG_NHAT',  1.2000),
    (1, 'TRANG_TRONG', 1.0000),
    (1, 'TRANG_CUOI',  0.9000);

INSERT INTO TyLeThuLao (baremId, vaiTroThamGia, tyLePhanTram) VALUES
    (1, 'BIEN_TAP',       10.0000),
    (1, 'HOA_SI',          5.0000),
    (1, 'PHONG_VIEN_ANH',  8.0000);

INSERT INTO MucThuongKPI (baremId, tyLeHoanThanhTu, tyLeHoanThanhDen, xepLoaiKy, heSoThuong) VALUES
    (1, 120.0000, NULL,     'A', 1.2000),
    (1, 100.0000, 120.0000, 'B', 1.0000),
    (1,  80.0000, 100.0000, 'C', 0.9000),
    (1,   0.0000,  80.0000, 'D', 0.8000);

/* ---------- Cụm 5: định mức & kỳ tính ---------- */

INSERT INTO DinhMuc (banId, nguoiDungId, soLuongTin, soLuongBai, hieuLucTu) VALUES
    (1, NULL, 20, 8, '2026-01-01'),   -- Ban Thời sự
    (2, NULL, 15, 6, '2026-01-01'),   -- Ban Kinh tế
    (3, NULL, 12, 6, '2026-01-01'),   -- Ban Văn hoá
    (4, NULL, 18, 5, '2026-01-01');   -- Ban Thể thao

/* Ghi đè cá nhân: NV002 đi học nửa kỳ, giảm định mức */
INSERT INTO DinhMuc (banId, nguoiDungId, soLuongTin, soLuongBai, hieuLucTu) VALUES
    (NULL, 2, 10, 4, '2026-01-01');

INSERT INTO KyTinh (thang, nam, quyNhuanBut, batTranQuy, baremId, trangThai) VALUES
    (1, 2026, 480000000, 1, 1, 'DA_CHOT'),
    (2, 2026, 480000000, 1, 1, 'MO');

GO

PRINT N'Nạp dữ liệu mẫu thành công.';
GO
