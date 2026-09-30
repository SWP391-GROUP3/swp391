import { useState } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Login from './pages/Login.jsx'
import Main from './pages/Main.jsx'

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

  return (
    <Routes>
      {/* Vao "/" thi day sang /main; chua dang nhap thi /main lai day tiep ve /login */}
      <Route path="/" element={<Navigate to="/main" replace />} />

      <Route
        path="/login"
        element={
          // Da dang nhap roi thi khong cho xem lai form nua
          nguoiDung
            ? <Navigate to="/main" replace />
            : <Login onDangNhap={dangNhap} />
        }
      />

      <Route
        path="/main"
        element={
          // Cho chan: chua dang nhap la bi da ve /login.
          // replace = khong luu vao lich su, de bam Back khong quay lai duoc.
          nguoiDung
            ? <Main nguoiDung={nguoiDung} onDangXuat={dangXuat} />
            : <Navigate to="/login" replace />
        }
      />

      {/* Duong dan la - nem ve trang chu */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
