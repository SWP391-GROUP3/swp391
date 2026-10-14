import { useState } from 'react'
import { doiMatKhau } from '../../api/userApi.js'

// Giong UserPasswordChangeRequest.MIN_PASSWORD_LENGTH o server
const DO_DAI_TOI_THIEU = 6

/**
 * The "Đổi mật khẩu" tren trang Ho so. Phai nhap dung mat khau hien tai.
 * Doi xong van giu dang nhap (token khong phu thuoc mat khau).
 */
export default function ChangePasswordForm() {
  const [matKhauCu, setMatKhauCu] = useState('')
  const [matKhauMoi, setMatKhauMoi] = useState('')
  const [nhapLai, setNhapLai] = useState('')
  const [dangLuu, setDangLuu] = useState(false)
  const [loi, setLoi] = useState('')
  const [thongBao, setThongBao] = useState('')

  async function guiForm(event) {
    event.preventDefault()
    setLoi('')
    setThongBao('')

    // Kiem nhanh o trinh duyet; server van kiem lai
    if (!matKhauCu || !matKhauMoi || !nhapLai) {
      setLoi('Vui lòng nhập đủ 3 ô mật khẩu.')
      return
    }
    if (matKhauMoi.length < DO_DAI_TOI_THIEU) {
      setLoi(`Mật khẩu mới phải có ít nhất ${DO_DAI_TOI_THIEU} ký tự.`)
      return
    }
    if (matKhauMoi !== nhapLai) {
      setLoi('Mật khẩu nhập lại không khớp với mật khẩu mới.')
      return
    }

    setDangLuu(true)
    try {
      await doiMatKhau(matKhauCu, matKhauMoi, nhapLai)
      setMatKhauCu('')
      setMatKhauMoi('')
      setNhapLai('')
      setThongBao('Đã đổi mật khẩu. Lần đăng nhập sau dùng mật khẩu mới.')
    } catch (err) {
      setLoi(err.message)   // vd "Mật khẩu hiện tại không đúng."
    } finally {
      setDangLuu(false)
    }
  }

  return (
    <section className="ho-so__the" aria-labelledby="ho-so-doi-mat-khau">
      <h2 id="ho-so-doi-mat-khau" className="ho-so__tieu-de-the">Đổi mật khẩu</h2>

      <form className="ho-so__form" onSubmit={guiForm} noValidate>
        {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
        {thongBao && <p className="thong-bao-thanh-cong" role="status">{thongBao}</p>}

        <label className="ho-so__truong">
          Mật khẩu hiện tại
          <input
            className="ho-so__o-nhap" type="password" name="currentPassword"
            autoComplete="current-password" required
            value={matKhauCu} onChange={(event) => setMatKhauCu(event.target.value)}
          />
        </label>
        <label className="ho-so__truong">
          Mật khẩu mới
          <input
            className="ho-so__o-nhap" type="password" name="newPassword"
            autoComplete="new-password" required
            value={matKhauMoi} onChange={(event) => setMatKhauMoi(event.target.value)}
          />
          <span className="ho-so__goi-y">Ít nhất {DO_DAI_TOI_THIEU} ký tự, khác mật khẩu hiện tại.</span>
        </label>
        <label className="ho-so__truong">
          Nhập lại mật khẩu mới
          <input
            className="ho-so__o-nhap" type="password" name="confirmPassword"
            autoComplete="new-password" required
            value={nhapLai} onChange={(event) => setNhapLai(event.target.value)}
          />
        </label>

        <div className="ho-so__hang-nut">
          <button type="submit" className="ho-so__nut-chinh" disabled={dangLuu}>
            {dangLuu ? 'Đang lưu…' : 'Đổi mật khẩu'}
          </button>
        </div>
      </form>
    </section>
  )
}
