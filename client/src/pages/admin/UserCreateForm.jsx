import { useEffect, useState } from 'react'
import { createUser, getSections } from '../../api/adminApi.js'

/**
 * Form Admin tao tai khoan moi, hien ngay tren man "Tài khoản" (UserList).
 * Form tu kiem o trong / sai dinh dang truoc khi gui (kiemTraForm), moi lan bao 1 cau.
 * Luat nghiep vu (trung username/email, ban con hoat dong...) kiem o server, form hien loi server tra ve.
 *
 * CODE LIEN QUAN (Ctrl+P trong VS Code, dan duong dan de mo file):
 *   client/src/api/adminApi.js
 *   server/src/main/java/com/swp391/kpi/admin/controller/UserAdminController.java
 *   server/src/main/java/com/swp391/kpi/admin/service/UserAdminService.java   createUser(): kiem trung, luat ban, ma hoa mat khau
 * DUONG DI KHI MO FORM / BAM NUT (ham api -> URL -> Controller -> Service):
 *   Mo form: getSections() -> GET /api/sections
 *   [Tao tai khoan]: createUser() -> POST /api/users -> UserAdminService.createUser()
 *
 * MUC LUC (so dong - ten - tac dung)
 *   34  VAI_TRO                         Vai tro chon duoc (roles.code) va co phai thuoc ban khong
 *   44  FORM_TRONG                      Gia tri ban dau cua form
 *   54  hang so                         do dai mat khau toi thieu, mau ten dang nhap (khop server)
 *   61  kiemTraForm()                   tra cau loi dau tien cua form (o trong / sai dinh dang), '' neu hop le
 *   76  UserCreateForm()                form tao tai khoan; onTaoXong(user) bao man cha tai lai danh sach
 *   83    useEffect                     tai danh sach ban khi mo form
 *   93    canBan                        vai tro dang chon co phai chon ban khong
 *   96    doiVaiTro()                   doi vai tro: vai tro khong thuoc ban thi xoa ban da chon
 *  103    guiForm()                     kiem form, hop le thi gui POST /api/users, xong thi bao man cha
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  180  [Dang tao… / Tao tai khoan]     guiForm() -> kiemTraForm() -> goi POST /api/users
 *  183  [Huy]                           onHuy() -> dong form
 */

// Vai tro chon duoc (roles.code) va co phai thuoc ban khong (PV / BTV / Truong ban, comment bang users)
const VAI_TRO = [
  { ma: 'REPORTER', ten: 'Phóng viên', canBan: true },
  { ma: 'EDITOR', ten: 'Biên tập viên', canBan: true },
  { ma: 'HEAD_OF_DEPARTMENT', ten: 'Trưởng ban', canBan: true },
  { ma: 'EDITORIAL_SECRETARY', ten: 'Thư ký tòa soạn', canBan: false },
  { ma: 'ACCOUNTANT', ten: 'Kế toán', canBan: false },
  { ma: 'ADMIN', ten: 'Quản trị', canBan: false },
]

// Gia tri ban dau cua form
const FORM_TRONG = {
  username: '',
  email: '',
  password: '',
  fullName: '',
  roleCode: '',
  sectionId: '',
}

// Khop voi server: UserPasswordChangeRequest.MIN_PASSWORD_LENGTH va UserCreateRequest.USERNAME_PATTERN
const DO_DAI_MAT_KHAU_TOI_THIEU = 6
const MAU_TEN_DANG_NHAP = /^[a-zA-Z0-9._-]+$/

/**
 * Kiem o trong / sai dinh dang truoc khi gui. Tra ve cau loi DAU TIEN theo thu tu o tren form,
 * '' neu hop le. Qua dai thi maxLength cua tung o da chan. Server van kiem lai bang @Valid.
 */
function kiemTraForm(form, canBan) {
  if (!form.fullName.trim()) return 'Vui lòng nhập họ tên.'
  if (!form.username) return 'Vui lòng nhập tên đăng nhập.'
  if (!MAU_TEN_DANG_NHAP.test(form.username)) {
    return 'Tên đăng nhập chỉ gồm chữ không dấu, số, dấu chấm, gạch dưới, gạch ngang.'
  }
  if (!form.email.trim()) return 'Vui lòng nhập email.'
  if (form.password.length < DO_DAI_MAT_KHAU_TOI_THIEU) {
    return `Mật khẩu ban đầu phải có ít nhất ${DO_DAI_MAT_KHAU_TOI_THIEU} ký tự.`
  }
  if (!form.roleCode) return 'Vui lòng chọn vai trò.'
  if (canBan && form.sectionId === '') return 'Phóng viên, Biên tập viên và Trưởng ban phải thuộc một ban.'
  return ''
}

export default function UserCreateForm({ onTaoXong, onHuy }) {
  const [form, setForm] = useState(FORM_TRONG)
  const [dsBan, setDsBan] = useState([])
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // tai danh sach ban khi mo form
  useEffect(() => {
    let isMounted = true
    getSections()
      .then((data) => { if (isMounted) setDsBan(data) })
      .catch((err) => { if (isMounted) setError(err.message) })
    return () => { isMounted = false }
  }, [])

  // vai tro dang chon co phai chon ban khong
  const vaiTroDangChon = VAI_TRO.find((vaiTro) => vaiTro.ma === form.roleCode)
  const canBan = vaiTroDangChon ? vaiTroDangChon.canBan : false

  /** Doi vai tro: vai tro khong thuoc ban thi xoa ban da chon (server tu choi neu van gui ban). */
  function doiVaiTro(maVaiTro) {
    const vaiTro = VAI_TRO.find((item) => item.ma === maVaiTro)
    const giuBan = vaiTro && vaiTro.canBan
    setForm({ ...form, roleCode: maVaiTro, sectionId: giuBan ? form.sectionId : '' })
  }

  /** Kiem form truoc, hop le moi gui POST /api/users; thanh cong thi bao man cha tai lai danh sach. */
  function guiForm(event) {
    event.preventDefault()
    const loi = kiemTraForm(form, canBan)
    if (loi) {
      setError(loi)
      return
    }
    setSaving(true)
    setError('')
    const duLieu = {
      ...form,
      sectionId: form.sectionId === '' ? null : Number(form.sectionId),
    }
    createUser(duLieu)
      .then((user) => {
        setForm(FORM_TRONG)
        onTaoXong(user)
      })
      .catch((err) => setError(err.message))
      .finally(() => setSaving(false))
  }

  return (
    <form className="trang-qt__card trang-qt__tao-tk" onSubmit={guiForm} noValidate>
      <h2 className="trang-qt__group-title">Tạo tài khoản mới</h2>
      {error && <p className="thong-bao-loi" role="alert">{error}</p>}

      <div className="trang-qt__form">
        <div>
          <label className="nhan" htmlFor="tao-tk-fullName">Họ tên</label>
          <input id="tao-tk-fullName" className="o-nhap" value={form.fullName} maxLength={100}
            onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        </div>
        <div>
          <label className="nhan" htmlFor="tao-tk-username">Tên đăng nhập</label>
          <input id="tao-tk-username" className="o-nhap" value={form.username} maxLength={50}
            autoComplete="off" placeholder="vd lan.pt"
            onChange={(e) => setForm({ ...form, username: e.target.value })} />
          <p className="trang-qt__muted">Chữ không dấu, số, dấu chấm, gạch dưới, gạch ngang.</p>
        </div>
        <div>
          <label className="nhan" htmlFor="tao-tk-email">Email</label>
          <input id="tao-tk-email" type="email" className="o-nhap" value={form.email} maxLength={150}
            autoComplete="off"
            onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </div>
        <div>
          <label className="nhan" htmlFor="tao-tk-password">Mật khẩu ban đầu</label>
          <input id="tao-tk-password" type="password" className="o-nhap" value={form.password} maxLength={72}
            autoComplete="new-password"
            onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <p className="trang-qt__muted">Từ {DO_DAI_MAT_KHAU_TOI_THIEU} ký tự. Người dùng tự đổi sau ở trang hồ sơ.</p>
        </div>
        <div>
          <label className="nhan" htmlFor="tao-tk-role">Vai trò</label>
          <select id="tao-tk-role" className="o-nhap" value={form.roleCode}
            onChange={(e) => doiVaiTro(e.target.value)}>
            <option value="">— Chọn vai trò —</option>
            {VAI_TRO.map((vaiTro) => (
              <option key={vaiTro.ma} value={vaiTro.ma}>{vaiTro.ten}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="nhan" htmlFor="tao-tk-section">Ban</label>
          <select id="tao-tk-section" className="o-nhap" value={form.sectionId} disabled={!canBan}
            onChange={(e) => setForm({ ...form, sectionId: e.target.value })}>
            <option value="">{canBan ? '— Chọn ban —' : 'Không thuộc ban'}</option>
            {canBan && dsBan.map((ban) => (
              <option key={ban.id} value={ban.id}>{ban.name}</option>
            ))}
          </select>
          <p className="trang-qt__muted">Chỉ Phóng viên, Biên tập viên, Trưởng ban thuộc ban.</p>
        </div>
      </div>

      <div className="trang-qt__actions">
        <button type="submit" className="nut-chinh-fm" disabled={saving}>
          {saving ? 'Đang tạo…' : 'Tạo tài khoản'}
        </button>
        <button type="button" className="nut-nhe" disabled={saving} onClick={onHuy}>
          Huỷ
        </button>
      </div>
    </form>
  )
}
