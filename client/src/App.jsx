import { useState } from 'react'
import AppRoutes from './routes/AppRoutes.jsx'
import { docPhien, luuPhien, xoaPhien } from './auth/phienDangNhap.js'

/**
 * Goc cua app: chi giu trang thai dang nhap.
 * Token + thong tin nguoi dung luu trong sessionStorage (auth/phienDangNhap.js),
 * F5 van con. Khai bao route nam o routes/AppRoutes.jsx (AGENTS.md muc 4).
 */
export default function App() {
  // Ham trong useState chi chay 1 lan luc mo app: doc lai phien cu neu co
  const [nguoiDung, setNguoiDung] = useState(() => docPhien()?.nguoiDung ?? null)

  // phien = { token, nguoiDung } tu authApi.dangNhap()
  function dangNhap(phien) {
    luuPhien(phien)
    setNguoiDung(phien.nguoiDung)
  }

  // UC-70: server khong luu phien (JWT), xoa token o trinh duyet la xong
  function dangXuat() {
    xoaPhien()
    setNguoiDung(null)
  }

  return <AppRoutes nguoiDung={nguoiDung} onDangNhap={dangNhap} onDangXuat={dangXuat} />
}
