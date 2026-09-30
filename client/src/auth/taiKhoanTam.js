/**
 * ===========================================================================
 * TAI KHOAN GIA - CHI DE CHAY THU GIAO DIEN. PHAI XOA FILE NAY.
 * ===========================================================================
 * Mat khau nam thang trong ma nguon. Ai mo DevTools cung doc duoc, va code
 * React thi deu bi tai ve may nguoi dung. Khong duoc de file nay ton tai
 * den luc nop bai hay deploy.
 *
 * Khi nguoi phu trach dang nhap noi xong voi server:
 *   1. Xoa file nay va khoi "Tai khoan dung thu" trong Login.jsx
 *   2. Trong Login.jsx, thay loi goi dangNhapTam(...) bang:
 *        const res  = await fetch('/api/auth/login', {
 *          method:  'POST',
 *          headers: { 'Content-Type': 'application/json' },
 *          body:    JSON.stringify({ email, matKhau }),
 *        })
 *        if (!res.ok) throw new Error('Email hoac mat khau khong dung')
 *        const nguoiDung = await res.json()
 *
 * Danh sach duoi day lay dung 7 nguoi trong database/data.sql (ma NV, ho ten,
 * email, vai tro) de khi noi vao DB that thi du lieu khop san.
 */

const MAT_KHAU_CHUNG = '123456'

const TEN_VAI_TRO = {
  PHONG_VIEN:      'Phóng viên',
  BIEN_TAP_VIEN:   'Biên tập viên',
  THU_KY_TOA_SOAN: 'Thư ký toà soạn',
  TONG_BIEN_TAP:   'Tổng biên tập',
  KE_TOAN:         'Kế toán',
  QUAN_TRI:        'Quản trị hệ thống',
}

const DANH_SACH = [
  { maNV: 'NV001', hoTen: 'Nguyễn Văn An',  email: 'an.nv@toasoan.vn',    vaiTro: ['PHONG_VIEN'] },
  { maNV: 'NV002', hoTen: 'Trần Thị Bình',  email: 'binh.tt@toasoan.vn',  vaiTro: ['PHONG_VIEN'] },
  { maNV: 'NV003', hoTen: 'Lê Hoàng Cường', email: 'cuong.lh@toasoan.vn', vaiTro: ['BIEN_TAP_VIEN'] },
  { maNV: 'NV004', hoTen: 'Phạm Thu Dung',  email: 'dung.pt@toasoan.vn',  vaiTro: ['THU_KY_TOA_SOAN'] },
  { maNV: 'NV005', hoTen: 'Vũ Minh Đức',    email: 'duc.vm@toasoan.vn',   vaiTro: ['TONG_BIEN_TAP'] },
  { maNV: 'NV006', hoTen: 'Hoàng Lan Chi',  email: 'chi.hl@toasoan.vn',   vaiTro: ['KE_TOAN', 'QUAN_TRI'] },
  { maNV: 'CTV01', hoTen: 'Đỗ Quốc Hưng',   email: 'hung.dq@gmail.com',   vaiTro: ['PHONG_VIEN'] },
]

/** Ten vai tro tieng Viet, ghep bang dau phay - de hien ra man hinh. */
export function tenVaiTro(vaiTro = []) {
  return vaiTro.map((ma) => TEN_VAI_TRO[ma] ?? ma).join(', ')
}

/**
 * Gia lap loi goi API dang nhap.
 * Tra ve Promise giong het fetch that, de sau nay thay bang fetch thi
 * phan con lai cua Login.jsx khong phai sua gi.
 *
 * @throws {Error} khi email hoac mat khau sai
 */
export async function dangNhapTam(email, matKhau) {
  // Cho 300ms cho giong do tre mang that, de thay duoc trang thai "Dang vao..."
  await new Promise((r) => setTimeout(r, 300))

  const canEmail = (email ?? '').trim().toLowerCase()
  const nguoiDung = DANH_SACH.find((tk) => tk.email.toLowerCase() === canEmail)

  if (!nguoiDung || matKhau !== MAT_KHAU_CHUNG) {
    throw new Error('Email hoặc mật khẩu không đúng.')
  }
  return nguoiDung
}
