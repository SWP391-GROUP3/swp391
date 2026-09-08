/* ==========================================================================
   SWP391 - Hệ thống đánh giá chất lượng (KPI) và quản lý nhuận bút
   Schema cho SQL Server 2019 trở lên.

   CÁCH DÙNG: mở file này trong SSMS, bấm Execute (F5).
   File re-runnable: xoá sạch bảng cũ rồi tạo lại từ đầu.
   Sửa schema thì sửa file này, commit, rồi báo cả nhóm chạy lại.
   ========================================================================== */

IF DB_ID('swp391_kpi') IS NULL
    CREATE DATABASE swp391_kpi;
GO

USE swp391_kpi;
GO

/* ---------- Xoá bảng theo thứ tự ngược khoá ngoại ---------- */
DROP TABLE IF EXISTS DieuChinh;
DROP TABLE IF EXISTS ChiTietBangKe;
DROP TABLE IF EXISTS BangKe;
DROP TABLE IF EXISTS KetQuaKPI;
DROP TABLE IF EXISTS DinhMuc;
DROP TABLE IF EXISTS KyTinh;
DROP TABLE IF EXISTS MucThuongKPI;
DROP TABLE IF EXISTS TyLeThuLao;
DROP TABLE IF EXISTS HeSoViTri;
DROP TABLE IF EXISTS HeSoChatLuong;
DROP TABLE IF EXISTS HeSoTheLoai;
DROP TABLE IF EXISTS BaremNhuanBut;
DROP TABLE IF EXISTS DiemChiTiet;
DROP TABLE IF EXISTS PhieuChamDiem;
DROP TABLE IF EXISTS TieuChiChamDiem;
DROP TABLE IF EXISTS LichSuTrangThai;
DROP TABLE IF EXISTS NguoiThamGia;
DROP TABLE IF EXISTS TacPham;
DROP TABLE IF EXISTS TheLoai;
DROP TABLE IF EXISTS NguoiDung_VaiTro;
DROP TABLE IF EXISTS NguoiDung;
DROP TABLE IF EXISTS VaiTro;
DROP TABLE IF EXISTS ChuyenMuc;
DROP TABLE IF EXISTS Ban;
GO

/* ==========================================================================
   CỤM 1 - NHÂN SỰ & TỔ CHỨC
   ========================================================================== */

CREATE TABLE Ban (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    ma      VARCHAR(20)   NOT NULL UNIQUE,
    ten     NVARCHAR(100) NOT NULL
);

CREATE TABLE ChuyenMuc (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    ma      VARCHAR(20)   NOT NULL UNIQUE,
    ten     NVARCHAR(100) NOT NULL
);

CREATE TABLE VaiTro (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    ma      VARCHAR(30)   NOT NULL UNIQUE,   -- PHONG_VIEN, BIEN_TAP_VIEN, ...
    ten     NVARCHAR(100) NOT NULL
);

CREATE TABLE NguoiDung (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    maNV            VARCHAR(20)   NOT NULL UNIQUE,
    hoTen           NVARCHAR(100) NOT NULL,
    email           VARCHAR(150)  NOT NULL UNIQUE,
    matKhau         VARCHAR(255)  NOT NULL,           -- BCrypt hash, KHÔNG lưu plain text
    banId           INT           NULL,
    laCongTacVien   BIT           NOT NULL DEFAULT 0, -- 1 = không ký HĐLĐ -> khấu trừ thuế 10%
    trangThai       VARCHAR(20)   NOT NULL DEFAULT 'HOAT_DONG',
    CONSTRAINT fk_nguoidung_ban FOREIGN KEY (banId) REFERENCES Ban(id),
    CONSTRAINT ck_nguoidung_tt  CHECK (trangThai IN ('HOAT_DONG','NGUNG'))
);

CREATE TABLE NguoiDung_VaiTro (
    nguoiDungId INT NOT NULL,
    vaiTroId    INT NOT NULL,
    PRIMARY KEY (nguoiDungId, vaiTroId),
    CONSTRAINT fk_ndvt_nd FOREIGN KEY (nguoiDungId) REFERENCES NguoiDung(id),
    CONSTRAINT fk_ndvt_vt FOREIGN KEY (vaiTroId)    REFERENCES VaiTro(id)
);

/* ==========================================================================
   CỤM 2 - TÁC PHẨM & QUY TRÌNH DUYỆT
   ========================================================================== */

CREATE TABLE TheLoai (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    ma      VARCHAR(30)   NOT NULL UNIQUE,
    ten     NVARCHAR(100) NOT NULL,
    laTin   BIT           NOT NULL DEFAULT 0   -- phân biệt "tin" và "bài" khi đối chiếu định mức
);

CREATE TABLE TacPham (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    maBai         VARCHAR(30)    NOT NULL UNIQUE,
    tieuDe        NVARCHAR(300)  NOT NULL,
    noiDung       NVARCHAR(MAX)  NULL,
    chuyenMucId   INT            NOT NULL,
    theLoaiId     INT            NOT NULL,
    banId         INT            NOT NULL,
    soTu          INT            NULL,
    viTriDang     VARCHAR(30)    NULL,          -- TRANG_NHAT, TRANG_TRONG, ...
    ngayNop       DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    hanChot       DATETIME2      NULL,          -- để tính KPI "trễ hạn"
    ngayXuatBan   DATETIME2      NULL,          -- quyết định bài thuộc kỳ nào
    trangThai     VARCHAR(30)    NOT NULL DEFAULT 'NHAP',
    CONSTRAINT fk_tacpham_cm  FOREIGN KEY (chuyenMucId) REFERENCES ChuyenMuc(id),
    CONSTRAINT fk_tacpham_tl  FOREIGN KEY (theLoaiId)   REFERENCES TheLoai(id),
    CONSTRAINT fk_tacpham_ban FOREIGN KEY (banId)       REFERENCES Ban(id),
    CONSTRAINT ck_tacpham_tt  CHECK (trangThai IN (
        'NHAP','CHO_BIEN_TAP','DANG_BIEN_TAP','CHO_DUYET',
        'DA_DUYET','DA_XUAT_BAN','DA_CHAM_DIEM','DA_CHOT_KY',
        'TRA_LAI','TU_CHOI'))
);
CREATE INDEX ix_tacpham_ngayxb    ON TacPham(ngayXuatBan);
CREATE INDEX ix_tacpham_trangthai ON TacPham(trangThai);

/* Một bài có nhiều người tham gia. tyLe CHỈ dùng cho 2 vai trò tác giả,
   và tổng tyLe của các tác giả trong 1 bài PHẢI bằng 100.
   Ràng buộc "tổng = 100" không viết được bằng CHECK của SQL Server,
   nên phải kiểm tra ở tầng service trước khi cho chuyển trạng thái. */
CREATE TABLE NguoiThamGia (
    id             INT IDENTITY(1,1) PRIMARY KEY,
    tacPhamId      INT           NOT NULL,
    nguoiDungId    INT           NOT NULL,
    vaiTroThamGia  VARCHAR(30)   NOT NULL,
    tyLe           DECIMAL(7,4)  NULL,          -- vd 60.0000 = 60%
    CONSTRAINT fk_ntg_tacpham FOREIGN KEY (tacPhamId)   REFERENCES TacPham(id),
    CONSTRAINT fk_ntg_nd      FOREIGN KEY (nguoiDungId) REFERENCES NguoiDung(id),
    CONSTRAINT uq_ntg         UNIQUE (tacPhamId, nguoiDungId, vaiTroThamGia),
    CONSTRAINT ck_ntg_vaitro  CHECK (vaiTroThamGia IN (
        'TAC_GIA_CHINH','DONG_TAC_GIA','BIEN_TAP','HOA_SI','PHONG_VIEN_ANH')),
    CONSTRAINT ck_ntg_tyle    CHECK (tyLe IS NULL OR (tyLe > 0 AND tyLe <= 100))
);
CREATE INDEX ix_ntg_nguoidung ON NguoiThamGia(nguoiDungId);

CREATE TABLE LichSuTrangThai (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    tacPhamId       INT           NOT NULL,
    tuTrangThai     VARCHAR(30)   NULL,
    denTrangThai    VARCHAR(30)   NOT NULL,
    nguoiThucHienId INT           NOT NULL,
    thoiGian        DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    ghiChu          NVARCHAR(500) NULL,
    CONSTRAINT fk_lstt_tacpham FOREIGN KEY (tacPhamId)       REFERENCES TacPham(id),
    CONSTRAINT fk_lstt_nd      FOREIGN KEY (nguoiThucHienId) REFERENCES NguoiDung(id)
);
CREATE INDEX ix_lstt_tacpham ON LichSuTrangThai(tacPhamId);

/* ==========================================================================
   CỤM 3 - CHẤM CHẤT LƯỢNG (KPI tầng bài)
   ========================================================================== */

CREATE TABLE TieuChiChamDiem (
    id       INT IDENTITY(1,1) PRIMARY KEY,
    ten      NVARCHAR(150) NOT NULL,
    trongSo  DECIMAL(7,4)  NOT NULL,      -- tổng trọng số nên bằng 1
    dangDung BIT           NOT NULL DEFAULT 1
);

CREATE TABLE PhieuChamDiem (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    tacPhamId   INT            NOT NULL,
    nguoiChamId INT            NOT NULL,
    ngayCham    DATETIME2      NOT NULL DEFAULT SYSDATETIME(),
    xepLoai     VARCHAR(2)     NULL,       -- A/B/C/D, tính từ DiemChiTiet
    laChotCuoi  BIT            NOT NULL DEFAULT 0,  -- TKTS chốt đè lên kết quả tự động
    lyDoChot    NVARCHAR(500)  NULL,       -- bắt buộc khi laChotCuoi = 1
    nhanXet     NVARCHAR(1000) NULL,
    CONSTRAINT fk_pcd_tacpham FOREIGN KEY (tacPhamId)   REFERENCES TacPham(id),
    CONSTRAINT fk_pcd_nd      FOREIGN KEY (nguoiChamId) REFERENCES NguoiDung(id),
    CONSTRAINT uq_pcd         UNIQUE (tacPhamId, nguoiChamId),
    CONSTRAINT ck_pcd_xeploai CHECK (xepLoai IS NULL OR xepLoai IN ('A','B','C','D'))
);

CREATE TABLE DiemChiTiet (
    id        INT IDENTITY(1,1) PRIMARY KEY,
    phieuId   INT          NOT NULL,
    tieuChiId INT          NOT NULL,
    diem      DECIMAL(5,2) NOT NULL,      -- thang 0-10
    CONSTRAINT fk_dct_phieu   FOREIGN KEY (phieuId)   REFERENCES PhieuChamDiem(id),
    CONSTRAINT fk_dct_tieuchi FOREIGN KEY (tieuChiId) REFERENCES TieuChiChamDiem(id),
    CONSTRAINT uq_dct         UNIQUE (phieuId, tieuChiId),
    CONSTRAINT ck_dct_diem    CHECK (diem >= 0 AND diem <= 10)
);

/* ==========================================================================
   CỤM 4 - BAREM (cấu hình có phiên bản)
   Barem đã BAN_HÀNH thì KHÔNG được sửa. Muốn đổi -> tạo bản mới.
   ========================================================================== */

CREATE TABLE BaremNhuanBut (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    ten             NVARCHAR(150) NOT NULL,
    hieuLucTu       DATE          NOT NULL,
    hieuLucDen      DATE          NULL,
    giaTriDonViHeSo DECIMAL(18,0) NOT NULL,   -- vd 150000
    trangThai       VARCHAR(20)   NOT NULL DEFAULT 'NHAP',
    CONSTRAINT ck_barem_tt CHECK (trangThai IN ('NHAP','DA_BAN_HANH'))
);

CREATE TABLE HeSoTheLoai (
    id        INT IDENTITY(1,1) PRIMARY KEY,
    baremId   INT           NOT NULL,
    theLoaiId INT           NOT NULL,
    heSo      DECIMAL(10,4) NOT NULL,
    CONSTRAINT fk_hstl_barem FOREIGN KEY (baremId)   REFERENCES BaremNhuanBut(id),
    CONSTRAINT fk_hstl_tl    FOREIGN KEY (theLoaiId) REFERENCES TheLoai(id),
    CONSTRAINT uq_hstl       UNIQUE (baremId, theLoaiId)
);

CREATE TABLE HeSoChatLuong (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    baremId INT           NOT NULL,
    xepLoai VARCHAR(2)    NOT NULL,
    heSo    DECIMAL(10,4) NOT NULL,
    CONSTRAINT fk_hscl_barem FOREIGN KEY (baremId) REFERENCES BaremNhuanBut(id),
    CONSTRAINT uq_hscl       UNIQUE (baremId, xepLoai),
    CONSTRAINT ck_hscl_xl    CHECK (xepLoai IN ('A','B','C','D'))
);

CREATE TABLE HeSoViTri (
    id        INT IDENTITY(1,1) PRIMARY KEY,
    baremId   INT           NOT NULL,
    viTriDang VARCHAR(30)   NOT NULL,
    heSo      DECIMAL(10,4) NOT NULL,
    CONSTRAINT fk_hsvt_barem FOREIGN KEY (baremId) REFERENCES BaremNhuanBut(id),
    CONSTRAINT uq_hsvt       UNIQUE (baremId, viTriDang)
);

CREATE TABLE TyLeThuLao (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    baremId       INT          NOT NULL,
    vaiTroThamGia VARCHAR(30)  NOT NULL,   -- BIEN_TAP, HOA_SI, PHONG_VIEN_ANH
    tyLePhanTram  DECIMAL(7,4) NOT NULL,   -- vd 10.0000 = 10% tiền bài
    CONSTRAINT fk_tltl_barem FOREIGN KEY (baremId) REFERENCES BaremNhuanBut(id),
    CONSTRAINT uq_tltl       UNIQUE (baremId, vaiTroThamGia)
);

CREATE TABLE MucThuongKPI (
    id               INT IDENTITY(1,1) PRIMARY KEY,
    baremId          INT           NOT NULL,
    tyLeHoanThanhTu  DECIMAL(7,4)  NOT NULL,   -- vd 120.0000
    tyLeHoanThanhDen DECIMAL(7,4)  NULL,       -- NULL = không giới hạn trên
    xepLoaiKy        VARCHAR(2)    NOT NULL,
    heSoThuong       DECIMAL(10,4) NOT NULL,   -- vd 1.2000
    CONSTRAINT fk_mtk_barem FOREIGN KEY (baremId) REFERENCES BaremNhuanBut(id)
);

/* ==========================================================================
   CỤM 5 - KỲ TÍNH & BẢNG KÊ
   ========================================================================== */

CREATE TABLE KyTinh (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    thang         INT           NOT NULL,
    nam           INT           NOT NULL,
    quyNhuanBut   DECIMAL(18,0) NOT NULL,
    batTranQuy    BIT           NOT NULL DEFAULT 1,
    heSoDieuChinh DECIMAL(12,8) NULL,        -- điền sau khi chốt
    baremId       INT           NOT NULL,
    baremSnapshot NVARCHAR(MAX) NULL,        -- JSON, chụp lúc chốt kỳ
    trangThai     VARCHAR(20)   NOT NULL DEFAULT 'MO',
    ngayChot      DATETIME2     NULL,
    nguoiChotId   INT           NULL,
    CONSTRAINT fk_kytinh_barem FOREIGN KEY (baremId)     REFERENCES BaremNhuanBut(id),
    CONSTRAINT fk_kytinh_nd    FOREIGN KEY (nguoiChotId) REFERENCES NguoiDung(id),
    CONSTRAINT uq_kytinh       UNIQUE (thang, nam),
    CONSTRAINT ck_kytinh_thang CHECK (thang BETWEEN 1 AND 12),
    CONSTRAINT ck_kytinh_tt    CHECK (trangThai IN ('MO','DANG_TINH','CHO_DUYET','DA_CHOT','DA_CHI'))
);

/* Định mức gắn theo Ban, cho phép ghi đè theo cá nhân.
   Dòng nào có nguoiDungId thì thắng dòng chỉ có banId. */
CREATE TABLE DinhMuc (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    banId       INT  NULL,
    nguoiDungId INT  NULL,
    soLuongTin  INT  NOT NULL DEFAULT 0,
    soLuongBai  INT  NOT NULL DEFAULT 0,
    hieuLucTu   DATE NOT NULL,
    CONSTRAINT fk_dinhmuc_ban      FOREIGN KEY (banId)       REFERENCES Ban(id),
    CONSTRAINT fk_dinhmuc_nd       FOREIGN KEY (nguoiDungId) REFERENCES NguoiDung(id),
    CONSTRAINT ck_dinhmuc_doituong CHECK (
        (banId IS NOT NULL AND nguoiDungId IS NULL) OR
        (banId IS NULL AND nguoiDungId IS NOT NULL))
);

CREATE TABLE KetQuaKPI (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    kyTinhId      INT           NOT NULL,
    nguoiDungId   INT           NOT NULL,
    soTin         INT           NOT NULL DEFAULT 0,
    soBai         INT           NOT NULL DEFAULT 0,
    tyLeHoanThanh DECIMAL(7,4)  NOT NULL DEFAULT 0,
    tyLeBaiLoaiA  DECIMAL(7,4)  NOT NULL DEFAULT 0,
    soLanTraLai   INT           NOT NULL DEFAULT 0,
    soLanTreHan   INT           NOT NULL DEFAULT 0,
    xepLoaiKy     VARCHAR(2)    NULL,
    heSoThuong    DECIMAL(10,4) NOT NULL DEFAULT 1,
    CONSTRAINT fk_kqkpi_ky FOREIGN KEY (kyTinhId)    REFERENCES KyTinh(id),
    CONSTRAINT fk_kqkpi_nd FOREIGN KEY (nguoiDungId) REFERENCES NguoiDung(id),
    CONSTRAINT uq_kqkpi    UNIQUE (kyTinhId, nguoiDungId)
);

CREATE TABLE BangKe (
    id                 INT IDENTITY(1,1) PRIMARY KEY,
    kyTinhId           INT           NOT NULL,
    nguoiDungId        INT           NOT NULL,
    tongTienTho        DECIMAL(18,0) NOT NULL DEFAULT 0,
    heSoThuongKPI      DECIMAL(10,4) NOT NULL DEFAULT 1,
    tienTruocDieuChinh DECIMAL(18,0) NOT NULL DEFAULT 0,
    heSoDieuChinh      DECIMAL(12,8) NOT NULL DEFAULT 1,
    tienSauDieuChinh   DECIMAL(18,0) NOT NULL DEFAULT 0,
    dieuChinhKyTruoc   DECIMAL(18,0) NOT NULL DEFAULT 0,
    thueTNCN           DECIMAL(18,0) NOT NULL DEFAULT 0,
    thucNhan           DECIMAL(18,0) NOT NULL DEFAULT 0,
    CONSTRAINT fk_bangke_ky FOREIGN KEY (kyTinhId)    REFERENCES KyTinh(id),
    CONSTRAINT fk_bangke_nd FOREIGN KEY (nguoiDungId) REFERENCES NguoiDung(id),
    CONSTRAINT uq_bangke    UNIQUE (kyTinhId, nguoiDungId)
);

CREATE TABLE ChiTietBangKe (
    id            INT IDENTITY(1,1) PRIMARY KEY,
    bangKeId      INT           NOT NULL,
    tacPhamId     INT           NOT NULL,
    vaiTroThamGia VARCHAR(30)   NOT NULL,
    heSoApDung    DECIMAL(10,4) NOT NULL,
    tienTho       DECIMAL(18,0) NOT NULL,
    tienThucNhan  DECIMAL(18,0) NOT NULL,
    CONSTRAINT fk_ctbk_bangke  FOREIGN KEY (bangKeId)  REFERENCES BangKe(id),
    CONSTRAINT fk_ctbk_tacpham FOREIGN KEY (tacPhamId) REFERENCES TacPham(id)
);
CREATE INDEX ix_ctbk_bangke ON ChiTietBangKe(bangKeId);

/* Kỳ đã chốt thì BẤT BIẾN. Mọi sai sót đều thành một dòng ở đây,
   cộng vào bảng kê của kỳ áp dụng, KHÔNG sửa lại kỳ gốc. */
CREATE TABLE DieuChinh (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    kyGocId     INT           NOT NULL,
    kyApDungId  INT           NOT NULL,
    tacPhamId   INT           NULL,
    nguoiDungId INT           NOT NULL,
    soTien      DECIMAL(18,0) NOT NULL,      -- âm = truy thu, dương = truy lĩnh
    lyDo        NVARCHAR(500) NOT NULL,
    nguoiTaoId  INT           NOT NULL,
    ngayTao     DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_dc_kygoc    FOREIGN KEY (kyGocId)     REFERENCES KyTinh(id),
    CONSTRAINT fk_dc_kyapdung FOREIGN KEY (kyApDungId)  REFERENCES KyTinh(id),
    CONSTRAINT fk_dc_tacpham  FOREIGN KEY (tacPhamId)   REFERENCES TacPham(id),
    CONSTRAINT fk_dc_nd       FOREIGN KEY (nguoiDungId) REFERENCES NguoiDung(id),
    CONSTRAINT fk_dc_nguoitao FOREIGN KEY (nguoiTaoId)  REFERENCES NguoiDung(id),
    CONSTRAINT ck_dc_khacky   CHECK (kyGocId <> kyApDungId)
);
GO

PRINT N'Tạo schema thành công. Chạy tiếp data.sql để có dữ liệu mẫu.';
GO
