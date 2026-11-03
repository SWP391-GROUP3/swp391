import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getUsers } from '../../api/adminApi.js'
import './AdminPages.css'

/**
 * Danh sach tai khoan (Admin, M-A7): ho ten, vai tro, ban, trang thai. Chi xem; them / khoa tai khoan lam sau.
 *
 * ?status=ACTIVE / LOCKED (link tu trang chu Admin) -> chi hien tai khoan o trang thai do.
 *
 * MO TRANG NAY TAI: /users
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  TEN_VAI_TRO                     Ten vai tro theo roles.code
 *   32  UserList()                      user list
 *   38    useEffect                     tai du lieu khi mo trang
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   58  link [Bo loc]                   mo trang /users (UserList)
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
  const [searchParams] = useSearchParams()
  const boLocTrangThai = searchParams.get('status')

  useEffect(() => {
    let isMounted = true
    getUsers()
      .then((data) => { if (isMounted) setUsers(data) })
      .catch((err) => { if (isMounted) setError(err.message) })
    return () => { isMounted = false }
  }, [])

  return (
    <div>
      <div className="trang-qt__header">
        <h1 className="trang-qt__title">Tài khoản</h1>
        <p className="trang-qt__subtitle">Mọi tài khoản trong hệ thống, mỗi người một vai trò.</p>
      </div>

      <div className="trang-qt__card">
        {error && <p className="thong-bao-loi" role="alert">{error}</p>}
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
                <tr><th>#</th><th>Họ tên</th><th>Vai trò</th><th>Ban</th><th>Trạng thái</th></tr>
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
