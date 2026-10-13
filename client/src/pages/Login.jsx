import { useState } from 'react'
import { dangNhap } from '../api/authApi.js'

/**
 * Man dang nhap (UC-69). Nhap email HOAC ten dang nhap + mat khau.
 * Tai khoan mau (database/data.sql): mat khau chung 123456.
 */
export default function Login({ onDangNhap }) {
  const [tenDangNhap, setTenDangNhap] = useState('')
  const [matKhau, setMatKhau]         = useState('')
  const [loi, setLoi]                 = useState('')
  const [dangGui, setDangGui]         = useState(false)

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
      </div>
    </div>
  )
}
