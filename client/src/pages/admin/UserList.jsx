import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getUsers, lockUser, unlockUser } from '../../api/adminApi.js'
import { docPhien } from '../../auth/phienDangNhap.js'
import UserCreateForm from './UserCreateForm.jsx'
import './AdminPages.css'

/**
 * Danh sach tai khoan (Admin, M-A7): ho ten, vai tro, ban, trang thai.
 * Admin tao tai khoan moi (form UserCreateForm), khoa / mo khoa tai khoan. Khong xoa tai khoan
 * (schema.sql: chi khoa bang status = LOCKED). Admin khong tu khoa minh duoc.
 *
 * ?status=ACTIVE / LOCKED (link tu trang chu Admin) -> chi hien tai khoan o trang thai do.
 *
 * DUONG DI KHI MO TRANG / BAM NUT (ham api -> URL -> Controller -> Service):
 *   Mo trang: getUsers() -> GET /api/users -> UserReadService.getUsersForAdmin()
 *   [Khoa]: lockUser() -> PUT /api/users/{id}/lock -> UserAdminService.lockUser()
 *   [Mo khoa]: unlockUser() -> PUT /api/users/{id}/unlock -> UserAdminService.unlockUser()
 *
 * MO TRANG NAY TAI: /users
 *
 * MUC LUC (so dong - ten - tac dung)
 *   37  TEN_VAI_TRO                     Ten vai tro theo roles.code
 *   46  UserList()                      user list
 *   58    taiDanhSach()                 tai lai danh sach tai khoan
 *   65    useEffect                     tai du lieu khi mo trang
 *   70    taoXong()                     form tao xong: dong form, xoa loi cu, bao thanh cong, tai lai danh sach
 *   78    doiTrangThai()                bam Khoa / Mo khoa: hoi xac nhan, goi API, tai lai danh sach
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  106  [+ Tao tai khoan]               mo form UserCreateForm
 *  120  link [Bo loc]                   mo trang /users (UserList)
 *  146  [Khoa / Mo khoa]                doiTrangThai() -> goi PUT /api/users/{id}/lock hoac /unlock
 */

// Ten vai tro theo roles.code
const TEN_VAI_TRO = {
  REPORTER: 'Phóng viên',
  EDITOR: 'Biên tập viên',
  EDITORIAL_SECRETARY: 'Thư ký tòa soạn',
  HEAD_OF_DEPARTMENT: 'Trưởng ban',
  ACCOUNTANT: 'Kế toán',
  ADMIN: 'Quản trị',
}

export default function UserList() {
  const [users, setUsers] = useState(null)   // null = dang tai
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [moForm, setMoForm] = useState(false)
  const [dangXuLyId, setDangXuLyId] = useState(null)   // id tai khoan dang khoa / mo khoa
  const [searchParams] = useSearchParams()
  const boLocTrangThai = searchParams.get('status')
  const phien = docPhien()
  const toiId = phien ? phien.nguoiDung.id : null      // Admin dang dang nhap (khong tu khoa minh)

  /** Tai lai danh sach tai khoan. */
  function taiDanhSach() {
    return getUsers()
      .then((data) => setUsers(data))
      .catch((err) => setError(err.message))
  }

  // tai du lieu khi mo trang
  useEffect(() => {
    taiDanhSach()
  }, [])

  /** Form tao xong: dong form, bao thanh cong, tai lai danh sach. */
  function taoXong(user) {
    setMoForm(false)
    setError('')   // xoa loi cu (vd lan khoa that bai truoc do) de khong hien cung luc voi cau thanh cong
    setSuccess(`Đã tạo tài khoản ${user.username}.`)
    taiDanhSach()
  }

  /** Bam Khoa / Mo khoa: hoi xac nhan, goi API, tai lai danh sach. */
  function doiTrangThai(user) {
    const dangHoatDong = user.status === 'ACTIVE'
    const cauHoi = dangHoatDong
      ? `Khoá tài khoản của ${user.fullName}? Người này sẽ không đăng nhập được nữa.`
      : `Mở khoá tài khoản của ${user.fullName}?`
    if (!window.confirm(cauHoi)) return

    setDangXuLyId(user.id)
    setError('')
    setSuccess('')
    const goi = dangHoatDong ? lockUser(user.id) : unlockUser(user.id)
    goi
      .then(() => {
        setSuccess(dangHoatDong ? `Đã khoá tài khoản ${user.fullName}.` : `Đã mở khoá tài khoản ${user.fullName}.`)
        return taiDanhSach()
      })
      .catch((err) => setError(err.message))
      .finally(() => setDangXuLyId(null))
  }

  return (
    <div>
      <div className="trang-qt__header trang-qt__header--co-nut">
        <div>
          <h1 className="trang-qt__title">Tài khoản</h1>
          <p className="trang-qt__subtitle">Mọi tài khoản trong hệ thống, mỗi người một vai trò.</p>
        </div>
        {!moForm && (
          <button type="button" className="nut-chinh-fm" onClick={() => { setMoForm(true); setSuccess('') }}>
            + Tạo tài khoản
          </button>
        )}
      </div>

      {moForm && <UserCreateForm onTaoXong={taoXong} onHuy={() => setMoForm(false)} />}

      <div className="trang-qt__card">
        {error && <p className="thong-bao-loi" role="alert">{error}</p>}
        {success && <p className="thong-bao-thanh-cong" role="status">{success}</p>}
        {!users && !error && <p className="trang-qt__muted">Đang tải…</p>}
        {boLocTrangThai && (
          <p className="trang-qt__muted">
            Đang lọc: {boLocTrangThai === 'ACTIVE' ? 'Đang hoạt động' : 'Đã khóa'} · <Link to="/users">Bỏ lọc</Link>
          </p>
        )}
        {users && (
          <div className="trang-qt__scroll">
            <table className="bang-fm">
              <thead>
                <tr><th>#</th><th>Họ tên</th><th>Vai trò</th><th>Ban</th><th>Trạng thái</th><th>Thao tác</th></tr>
              </thead>
              <tbody>
                {users.filter((user) => !boLocTrangThai || (boLocTrangThai === 'ACTIVE') === (user.status === 'ACTIVE')).map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>{user.fullName}</td>
                    <td>{TEN_VAI_TRO[user.role] || user.role}</td>
                    <td>{user.sectionName || '—'}</td>
                    <td>
                      <span className={`nhan-tt ${user.status === 'ACTIVE' ? 'nhan-tt--tot' : 'nhan-tt--chu-y'}`}>
                        {user.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã khóa'}
                      </span>
                    </td>
                    <td>
                      {user.id === toiId ? (
                        <span className="trang-qt__muted">Tài khoản của bạn</span>
                      ) : (
                        <button type="button" className="nut-nhe" disabled={dangXuLyId === user.id}
                          onClick={() => doiTrangThai(user)}>
                          {user.status === 'ACTIVE' ? 'Khoá' : 'Mở khoá'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
