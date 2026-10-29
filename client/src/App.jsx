import { useState } from 'react'
import AppRoutes from './routes/AppRoutes.jsx'
import { docPhien, luuPhien, xoaPhien } from './auth/phienDangNhap.js'

/**
 * Goc cua app: chi giu trang thai dang nhap.
 * Token + thong tin nguoi dung luu trong sessionStorage (auth/phienDangNhap.js),
 * F5 van con. Khai bao route nam o routes/AppRoutes.jsx (AGENTS.md muc 4).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  App()                           xem tom tat dau file
 *   21    dangNhap()                    phien = { token, nguoiDung } tu authApi.dangNhap()
 *   27    dangXuat()                    UC-70: server khong luu phien (JWT), xoa token o trinh duyet la xong
 *   34    capNhatNguoiDung()            Sua ho so xong (vd doi ho ten): cap nhat ca state lan phien da luu, de thanh tren doi ten ngay va F5 khong hi…
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

  // Sua ho so xong (vd doi ho ten): cap nhat ca state lan phien da luu,
  // de thanh tren doi ten ngay va F5 khong hien lai ten cu. Token giu nguyen.
  function capNhatNguoiDung(thayDoi) {
    const phien = docPhien()
    if (!phien) {
      return
    }
    const nguoiDungMoi = { ...phien.nguoiDung, ...thayDoi }
    luuPhien({ ...phien, nguoiDung: nguoiDungMoi })
    setNguoiDung(nguoiDungMoi)
  }

  return (
    <AppRoutes
      nguoiDung={nguoiDung}
      onDangNhap={dangNhap}
      onDangXuat={dangXuat}
      onCapNhatNguoiDung={capNhatNguoiDung}
    />
  )
}
