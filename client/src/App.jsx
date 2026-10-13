import { useState } from 'react'
import AppRoutes from './routes/AppRoutes.jsx'

/** Khoa luu nguoi dung trong sessionStorage. */
const KHOA_PHIEN = 'nguoiDung'

/**
 * Doc lai nguoi dung da dang nhap tu sessionStorage.
 *
 * Vi sao can: React giu state trong bo nho, F5 mot cai la mat sach. Khong co
 * buoc nay thi cu tai lai trang la bi da ve man hinh dang nhap.
 *
 * Vi sao dung sessionStorage chu khong phai localStorage: sessionStorage tu
 * xoa khi dong tab, giong cach HttpSession het han. localStorage thi nam lai
 * mai tren may - dung o phong may truong thi nguoi sau mo ra van thay tai
 * khoan nguoi truoc.
 */
function docPhien() {
  try {
    const raw = sessionStorage.getItem(KHOA_PHIEN)
    return raw ? JSON.parse(raw) : null
  } catch {
    // JSON hong (bi sua tay, hoac doi cau truc du lieu) - coi nhu chua dang nhap
    return null
  }
}

/**
 * Goc cua app: chi giu trang thai dang nhap.
 * Khai bao route nam o routes/AppRoutes.jsx (AGENTS.md muc 4).
 */
export default function App() {
  const [nguoiDung, setNguoiDung] = useState(docPhien)

  function dangNhap(tk) {
    sessionStorage.setItem(KHOA_PHIEN, JSON.stringify(tk))
    setNguoiDung(tk)
  }

  function dangXuat() {
    sessionStorage.removeItem(KHOA_PHIEN)
    setNguoiDung(null)
  }

  return <AppRoutes nguoiDung={nguoiDung} onDangNhap={dangNhap} onDangXuat={dangXuat} />
}
