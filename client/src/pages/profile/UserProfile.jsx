import { useEffect, useState } from 'react'
import { capNhatHoSo, layHoSo } from '../../api/userApi.js'
import { chuCaiDau } from '../../utils/chuCaiDau.js'
import { formatDate } from '../../utils/formatDate.js'
import ChangePasswordForm from './ChangePasswordForm.jsx'
import '../../styles/UserProfile.css'

const TEN_TRANG_THAI = {
  ACTIVE: 'Đang hoạt động',
  LOCKED: 'Đã khoá',
}

/**
 * "Hồ sơ của tôi" - moi vai tro, mo tu menu avatar o thanh tren.
 * Trai: thong tin ca nhan (xem / sua ho ten + email). Phai: doi mat khau.
 * Vai tro, ban, username do Admin quan ly nen chi hien, khong sua.
 *
 * - onCapNhatNguoiDung: bao App.jsx ten/email moi de thanh tren doi theo ngay
 */
export default function UserProfile({ onCapNhatNguoiDung }) {
  const [hoSo, setHoSo] = useState(null)        // null = dang tai
  const [loiTai, setLoiTai] = useState('')
  const [dangSua, setDangSua] = useState(false)
  const [thongBao, setThongBao] = useState('')

  // Luon tai lai tu server (khong dung ban luu trong phien) de thay du lieu moi nhat
  useEffect(() => {
    let conHienThi = true
    layHoSo()
      .then((duLieu) => { if (conHienThi) setHoSo(duLieu) })
      .catch((err) => { if (conHienThi) setLoiTai(err.message) })
    return () => { conHienThi = false }
  }, [])

  function luuXong(hoSoMoi) {
    setHoSo(hoSoMoi)
    setDangSua(false)
    setThongBao('Đã cập nhật hồ sơ.')
    onCapNhatNguoiDung({ hoTen: hoSoMoi.fullName, email: hoSoMoi.email })
  }

  function batDauSua() {
    setThongBao('')
    setDangSua(true)
  }

  return (
    <div>
      <div className="ho-so__dau">
        <h1 className="ho-so__tieu-de">Hồ sơ của tôi</h1>
        <p className="ho-so__phu-de">Xem thông tin tài khoản, sửa họ tên, email và đổi mật khẩu.</p>
      </div>

      {loiTai && <p className="thong-bao-loi" role="alert">{loiTai}</p>}
      {!hoSo && !loiTai && <p className="ho-so__trong">Đang tải…</p>}

      {hoSo && (
        <div className="ho-so__luoi">
          <section className="ho-so__the" aria-labelledby="ho-so-thong-tin">
            <div className="ho-so__danh-thiep">
              <span className="ho-so__avatar" aria-hidden="true">{chuCaiDau(hoSo.fullName)}</span>
              <div>
                <h2 id="ho-so-thong-tin" className="ho-so__ten">{hoSo.fullName}</h2>
                <span className="ho-so__vai-tro">{hoSo.roleName}</span>
              </div>
              {!dangSua && (
                <button type="button" className="nut-nhe ho-so__nut-sua" onClick={batDauSua}>
                  Sửa hồ sơ
                </button>
              )}
            </div>

            {thongBao && <p className="thong-bao-thanh-cong" role="status">{thongBao}</p>}

            {dangSua
              ? <EditProfileForm hoSo={hoSo} onLuuXong={luuXong} onHuy={() => setDangSua(false)} />
              : <ProfileDetails hoSo={hoSo} />}
          </section>

          <ChangePasswordForm />
        </div>
      )}
    </div>
  )
}

/** Bang thong tin chi doc. */
function ProfileDetails({ hoSo }) {
  return (
    <dl className="ho-so__ds">
      <div className="ho-so__dong"><dt>Tên đăng nhập</dt><dd>{hoSo.username}</dd></div>
      <div className="ho-so__dong"><dt>Họ tên</dt><dd>{hoSo.fullName}</dd></div>
      <div className="ho-so__dong"><dt>Email</dt><dd>{hoSo.email}</dd></div>
      <div className="ho-so__dong"><dt>Vai trò</dt><dd>{hoSo.roleName}</dd></div>
      <div className="ho-so__dong">
        <dt>Trạng thái</dt>
        <dd>{TEN_TRANG_THAI[hoSo.status] || hoSo.status}</dd>
      </div>
      <div className="ho-so__dong"><dt>Ngày tạo tài khoản</dt><dd>{formatDate(hoSo.createdAt)}</dd></div>
    </dl>
  )
}

/** Form sua ho ten + email. Server kiem lai (@Valid + email trung). */
function EditProfileForm({ hoSo, onLuuXong, onHuy }) {
  const [hoTen, setHoTen] = useState(hoSo.fullName)
  const [email, setEmail] = useState(hoSo.email)
  const [dangLuu, setDangLuu] = useState(false)
  const [loi, setLoi] = useState('')

  async function guiForm(event) {
    event.preventDefault()   // khong cho trinh duyet tai lai trang
    setLoi('')
    if (!hoTen.trim() || !email.trim()) {
      setLoi('Vui lòng nhập đủ họ tên và email.')
      return
    }

    setDangLuu(true)
    try {
      const hoSoMoi = await capNhatHoSo(hoTen.trim(), email.trim())
      onLuuXong(hoSoMoi)
    } catch (err) {
      setLoi(err.message)   // vd "Email này đã được tài khoản khác sử dụng."
    } finally {
      setDangLuu(false)
    }
  }

  return (
    <form className="ho-so__form" onSubmit={guiForm} noValidate>
      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}

      <label className="ho-so__truong">
        Họ tên
        <input
          className="ho-so__o-nhap" name="fullName" autoComplete="name"
          maxLength={100} required
          value={hoTen} onChange={(event) => setHoTen(event.target.value)}
        />
      </label>
      <label className="ho-so__truong">
        Email
        <input
          className="ho-so__o-nhap" type="email" name="email" autoComplete="email"
          spellCheck={false} maxLength={150} required
          value={email} onChange={(event) => setEmail(event.target.value)}
        />
      </label>

      <div className="ho-so__hang-nut">
        <button type="submit" className="nut-chinh-fm" disabled={dangLuu}>
          {dangLuu ? 'Đang lưu…' : 'Lưu thay đổi'}
        </button>
        <button type="button" className="nut-nhe" onClick={onHuy} disabled={dangLuu}>Huỷ</button>
      </div>
    </form>
  )
}
