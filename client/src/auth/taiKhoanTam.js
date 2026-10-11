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
 * Danh sach duoi day lay dung 8 nguoi trong database/data.sql (username, ho ten,
 * email, vai tro) de khi noi vao DB that thi du lieu khop san.
 * Moi nguoi dung 1 vai tro (users.role_id), van de dang mang cho tenVaiTro().
 */

const MAT_KHAU_CHUNG = '123456'

const TEN_VAI_TRO = {
  REPORTER:            'Phóng viên',
  EDITOR:              'Biên tập viên',
  HEAD_OF_DEPARTMENT:  'Trưởng ban',
  EDITORIAL_SECRETARY: 'Thư ký toà soạn',
  ACCOUNTANT:          'Kế toán',
  ADMIN:               'Quản trị hệ thống',
}

const DANH_SACH = [
  { username: 'an.nv',    hoTen: 'Nguyễn Văn An',  email: 'an.nv@toasoan.vn',    vaiTro: ['REPORTER'] },
  { username: 'binh.tt',  hoTen: 'Trần Thị Bình',  email: 'binh.tt@toasoan.vn',  vaiTro: ['REPORTER'] },
  { username: 'cuong.lh', hoTen: 'Lê Hoàng Cường', email: 'cuong.lh@toasoan.vn', vaiTro: ['EDITOR'] },
  { username: 'dung.pt',  hoTen: 'Phạm Thu Dung',  email: 'dung.pt@toasoan.vn',  vaiTro: ['EDITORIAL_SECRETARY'] },
  { username: 'duc.vm',   hoTen: 'Vũ Minh Đức',    email: 'duc.vm@toasoan.vn',   vaiTro: ['HEAD_OF_DEPARTMENT'] },
  { username: 'chi.hl',   hoTen: 'Hoàng Lan Chi',  email: 'chi.hl@toasoan.vn',   vaiTro: ['ACCOUNTANT'] },
  { username: 'hung.dq',  hoTen: 'Đỗ Quốc Hưng',   email: 'hung.dq@toasoan.vn',  vaiTro: ['REPORTER'] },
  { username: 'admin',    hoTen: 'Quản trị viên',  email: 'admin@toasoan.vn',    vaiTro: ['ADMIN'] },
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
