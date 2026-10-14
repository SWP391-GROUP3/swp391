import { useState } from 'react'
import { dangNhap } from '../api/authApi.js'
import './Login.css'

/**
 * Tai khoan mau - chep tu database/data.sql, mat khau chung 123456.
 * CHI HIEN KHI CHAY DEV (npm run dev): import.meta.env.DEV cua Vite = true luc dev,
 * = false khi npm run build -> ban build de nop/deploy tu an, khong lo mat khau.
 */
const MAT_KHAU_MAU = '123456'
const TAI_KHOAN_MAU = [
  { username: 'an.nv',    email: 'an.nv@toasoan.vn',    vaiTro: 'Phóng viên (Thời sự)' },
  { username: 'binh.tt',  email: 'binh.tt@toasoan.vn',  vaiTro: 'Phóng viên (Thời sự)' },
  { username: 'hung.dq',  email: 'hung.dq@toasoan.vn',  vaiTro: 'Phóng viên (Kinh tế)' },
  { username: 'cuong.lh', email: 'cuong.lh@toasoan.vn', vaiTro: 'Biên tập viên (Thời sự)' },
  { username: 'giang.nt', email: 'giang.nt@toasoan.vn', vaiTro: 'Biên tập viên (Kinh tế)' },
  { username: 'duc.vm',   email: 'duc.vm@toasoan.vn',   vaiTro: 'Trưởng ban (Thời sự)' },
  { username: 'dung.pt',  email: 'dung.pt@toasoan.vn',  vaiTro: 'Thư ký toà soạn' },
  { username: 'chi.hl',   email: 'chi.hl@toasoan.vn',   vaiTro: 'Kế toán' },
  { username: 'admin',    email: 'admin@toasoan.vn',    vaiTro: 'Quản trị hệ thống' },
]

/**
 * Man dang nhap (UC-69). Nhap email HOAC ten dang nhap + mat khau.
 */
export default function Login({ onDangNhap }) {
  const [tenDangNhap, setTenDangNhap] = useState('')
  const [matKhau, setMatKhau]         = useState('')
  const [loi, setLoi]                 = useState('')
  const [dangGui, setDangGui]         = useState(false)

  // Bam 1 tai khoan mau -> dien san ca 2 o, chi con bam "Dang nhap"
  function chonTaiKhoanMau(username) {
    setTenDangNhap(username)
    setMatKhau(MAT_KHAU_MAU)
    setLoi('')
  }

  async function guiForm(e) {
    e.preventDefault()   // chan trinh duyet tai lai trang theo kieu form HTML co
    setLoi('')
    setDangGui(true)

    try {
      const phien = await dangNhap(tenDangNhap.trim(), matKhau)
      onDangNhap(phien)
    } catch (err) {
      setLoi(err.message)
      setMatKhau('')       // xoa mat khau, giu lai ten dang nhap da go
    } finally {
      setDangGui(false)
    }
  }

  return (
    <div className="trang-login">
      <div className="the-login">
        <h1 className="the-login__tieu-de">Hệ thống KPI &amp; Nhuận bút</h1>
        <p className="the-login__phu-de">Đăng nhập để tiếp tục</p>

        {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}

        <form onSubmit={guiForm} noValidate>
          <label className="nhan" htmlFor="tenDangNhap">Email hoặc tên đăng nhập</label>
          <input
            className="o-nhap"
            id="tenDangNhap"
            name="username"
            type="text"
            value={tenDangNhap}
            onChange={(e) => setTenDangNhap(e.target.value)}
            required
            autoFocus
            autoComplete="username"
            spellCheck={false}
            placeholder="ten.abc@toasoan.vn"
          />

          <label className="nhan" htmlFor="matKhau">Mật khẩu</label>
          <input
            className="o-nhap"
            id="matKhau"
            name="password"
            type="password"
            value={matKhau}
            onChange={(e) => setMatKhau(e.target.value)}
            required
            autoComplete="current-password"
          />

          <button className="nut-chinh" type="submit" disabled={dangGui}>
            {dangGui ? 'Đang vào…' : 'Đăng nhập'}
          </button>
        </form>

        {import.meta.env.DEV && (
          <details className="the-login__goi-y" open>
            <summary>Tài khoản mẫu (chỉ hiện khi chạy dev)</summary>
            <p>
              Mật khẩu chung: <code>{MAT_KHAU_MAU}</code>. Bấm 1 dòng để điền sẵn,
              đăng nhập bằng email hoặc tên đăng nhập đều được.
            </p>
            <ul>
              {TAI_KHOAN_MAU.map((tk) => (
                <li key={tk.username}>
                  <button type="button" onClick={() => chonTaiKhoanMau(tk.username)}>
                    <code>{tk.username}</code>
                    <span className="the-login__goi-y-email">{tk.email}</span>
                    <span className="the-login__goi-y-vai-tro">{tk.vaiTro}</span>
                  </button>
                </li>
              ))}
            </ul>
          </details>
        )}
      </div>
    </div>
  )
}
