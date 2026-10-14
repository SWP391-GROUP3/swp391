import { useState } from 'react'
import { dangNhapTam } from '../auth/taiKhoanTam.js'

export default function Login({ onDangNhap }) {
  const [email, setEmail]     = useState('')
  const [matKhau, setMatKhau] = useState('')
  const [loi, setLoi]         = useState('')
  const [dangGui, setDangGui] = useState(false)

  async function guiForm(e) {
    e.preventDefault()   // chan trinh duyet tai lai trang theo kieu form HTML co
    setLoi('')
    setDangGui(true)

    try {
      // >>> CHO DUY NHAT CAN THAY KHI NOI VAO SERVER THAT <<<
      const nguoiDung = await dangNhapTam(email, matKhau)
      onDangNhap(nguoiDung)
    } catch (err) {
      setLoi(err.message)
      setMatKhau('')       // xoa mat khau, giu lai email da go
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
          <label className="nhan" htmlFor="email">Email hoặc Tên tài khoản</label>
          <input
            className="o-nhap"
            id="email"
            type="text"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
            autoComplete="username"
            placeholder="Ví dụ: an.nv hoặc an.nv@toasoan.vn"
          />

          <label className="nhan" htmlFor="matKhau">Mật khẩu</label>
          <input
            className="o-nhap"
            id="matKhau"
            type="password"
            value={matKhau}
            onChange={(e) => setMatKhau(e.target.value)}
            required
            autoComplete="current-password"
          />

          <button className="nut-chinh" type="submit" disabled={dangGui}>
            {dangGui ? 'Đang vào...' : 'Đăng nhập'}
          </button>
        </form>

        {/* ==================================================================
            KHOI GOI Y BEN DUOI CHI DE CHAY THU
            XOA CUNG LUC VOI src/auth/taiKhoanTam.js
            Khong duoc de lot ra ban nop: no in thang mat khau len man hinh.
            ================================================================== */}
        <details className="goi-y">
          <summary>Tài khoản dùng thử (xoá khi có đăng nhập thật)</summary>
          <p>Mật khẩu chung cho tất cả: <code>123456</code></p>
          <ul>
            <li><code>an.nv@toasoan.vn</code> (hoặc <code>admin@toasoan.vn</code>) — Quản trị hệ thống (ADMIN)</li>
            <li><code>binh.tt@toasoan.vn</code> (hoặc <code>duc.vm@toasoan.vn</code>) — Trưởng ban (HEAD_OF_DEPARTMENT)</li>
            <li><code>duong.pt@toasoan.vn</code> (hoặc <code>cuong.lh@toasoan.vn</code>) — Biên tập viên (EDITOR)</li>
            <li><code>dung.pt@toasoan.vn</code> — Thư ký toà soạn</li>
            <li><code>chi.hl@toasoan.vn</code> — Kế toán</li>
            <li><code>hung.dq@toasoan.vn</code> — Phóng viên</li>
          </ul>
        </details>
      </div>
    </div>
  )
}
