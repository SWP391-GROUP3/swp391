import { useEffect, useState } from 'react'
import { NavLink, Link } from 'react-router-dom'
import { tenVaiTro } from '../auth/taiKhoanTam.js'
import { layViTien } from '../api/reporterHomeApi.js'
import { formatMoney } from '../utils/formatMoney.js'
import Icon from '../components/common/Icon.jsx'
import './ReporterLayout.css'

/**
 * Menu cua Phong vien. Moi muc ghi UC tuong ung trong SRS.
 * Duong dan (path) khai bao o routes/AppRoutes.jsx.
 */
const MENU_PHONG_VIEN = [
  { path: '/main', nhan: 'Trang chủ',
    icon: <><rect x="4" y="4" width="6" height="6" rx="1" /><rect x="14" y="4" width="6" height="6" rx="1" /><rect x="4" y="14" width="6" height="6" rx="1" /><rect x="14" y="14" width="6" height="6" rx="1" /></> },
  { path: '/my-articles', nhan: 'Bài của tôi',                       // UC-01
    icon: <><rect x="4" y="3" width="16" height="18" rx="2" /><path d="M8 8h8M8 12h8M8 16h5" /></> },
  { path: '/my-articles/new', nhan: 'Soạn bài mới',                  // UC-03
    icon: <><path d="M4 20h4l10.5-10.5a2.1 2.1 0 0 0-3-3L5 17v3Z" /><path d="M14 7l3 3" /></> },
  { path: '/co-author-invites', nhan: 'Xác nhận đồng tác giả',       // UC-12
    icon: <><circle cx="9" cy="8" r="3" /><path d="M3 20a6 6 0 0 1 12 0" /><path d="M16 11l2 2 4-4" /></> },
  { path: '/my-appeals', nhan: 'Khiếu nại điểm',                     // UC-35
    icon: <><path d="M12 3v18M5 21h14" /><path d="M5 7h14" /><path d="M5 7l-3 7a3 3 0 0 0 6 0L5 7ZM19 7l-3 7a3 3 0 0 0 6 0l-3-7Z" /></> },
  { path: '/my-royalties', nhan: 'Nhuận bút',                        // UC-57
    icon: <><rect x="3" y="6" width="18" height="13" rx="2" /><path d="M3 10h18" /><circle cx="16" cy="14.5" r="1.2" /></> },
  { path: '/my-kpi', nhan: 'KPI cá nhân',                            // UC-60, UC-67
    icon: <><path d="M4 20V10M10 20V4M16 20v-7M22 20H2" /></> },
]

/** Chu cai dau cua ten (vd "Nguyễn Văn An" -> "A") de lam avatar. */
function chuCaiDau(hoTen = '') {
  const cacTu = hoTen.trim().split(' ')
  return cacTu[cacTu.length - 1].charAt(0).toUpperCase()
}

/**
 * Khung trang cua Phong vien: sidebar trai + thanh tren + vung noi dung.
 * Trang con (trang chu, bai cua toi...) truyen vao qua `children`.
 */
export default function ReporterLayout({ nguoiDung, onDangXuat, children }) {
  // null = chua tai xong -> formatMoney hien "—"
  const [soDu, setSoDu] = useState(null)
  const [loiVi, setLoiVi] = useState('')

  // Tai so du vi 1 lan khi khung trang hien ra
  useEffect(() => {
    let conHienThi = true   // tranh setState sau khi component da bi go

    layViTien()
      .then((duLieu) => { if (conHienThi) setSoDu(duLieu.balance) })
      .catch(() => { if (conHienThi) setLoiVi('Không tải được số dư ví') })

    return () => { conHienThi = false }
  }, [])

  return (
    <div className="khung-pv">
      {/* Link an, chi hien khi bam Tab: nguoi dung ban phim nhay thang vao
          noi dung, khong phai Tab qua ~11 muc menu moi lan doi trang */}
      <a href="#noi-dung-chinh" className="khung-pv__bo-qua">Bỏ qua tới nội dung chính</a>

      <aside className="khung-pv__sidebar">
        <div className="khung-pv__logo">
          <Icon><path d="M12 3 4 7.5v9L12 21l8-4.5v-9L12 3Z" /><path d="M4 7.5 12 12l8-4.5M12 12v9" /></Icon>
          <span>KPI &amp; Nhuận bút</span>
        </div>

        <nav className="khung-pv__menu" aria-label="Chức năng phóng viên">
          {MENU_PHONG_VIEN.map((muc) => (
            // NavLink tu them class "active" va aria-current="page" cho muc dang mo.
            // end: chi sang khi URL khop dung, de /my-articles khong sang
            // cung luc voi /my-articles/new.
            // title: hien tooltip khi sidebar thu nho chi con icon.
            <NavLink key={muc.path} to={muc.path} end className="khung-pv__muc" title={muc.nhan}>
              <Icon>{muc.icon}</Icon>
              <span>{muc.nhan}</span>
            </NavLink>
          ))}
        </nav>

        <button type="button" className="khung-pv__muc khung-pv__thoat" onClick={onDangXuat}>
          <Icon><path d="M10 4H5v16h5" /><path d="M14 8l4 4-4 4M18 12H9" /></Icon>
          <span>Đăng xuất</span>
        </button>
      </aside>

      <div className="khung-pv__phai">
        <header className="khung-pv__thanh-tren">
          {/* TODO: o tim kiem chua hoat dong, cho API tim bai cua BF-01 */}
          <label className="khung-pv__tim">
            <Icon><circle cx="11" cy="11" r="6.5" /><path d="m20 20-4.2-4.2" /></Icon>
            <span className="an-di">Tìm bài viết</span>
            <input type="search" name="q" placeholder="Tìm bài viết…" autoComplete="off" />
          </label>

          <div className="khung-pv__nguoi-dung">
            {/* TODO: chua co trang thong bao, cho NotificationService dung chung */}
            <button type="button" className="khung-pv__nut-icon" aria-label="Thông báo">
              <Icon><path d="M6 10a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" /><path d="M10 20a2 2 0 0 0 4 0" /></Icon>
            </button>

            {/* Chip vi: bam vao mo trang Nhuan but (UC-57) */}
            <Link to="/my-royalties" className="khung-pv__vi" title={loiVi || 'Số dư ví'}>
              <Icon><rect x="3" y="6" width="18" height="13" rx="2" /><path d="M16 12.5h5" /></Icon>
              {/* Loi cung dua vao chu an, de trinh doc man hinh doc duoc (title thi khong) */}
              <span className="an-di">{loiVi ? `${loiVi}.` : 'Số dư ví:'}</span>
              <span className="khung-pv__vi-so">{formatMoney(soDu)}</span>
            </Link>

            <div className="khung-pv__ten">
              <span className="khung-pv__ho-ten">{nguoiDung.hoTen}</span>
              <span className="khung-pv__vai-tro">{tenVaiTro(nguoiDung.vaiTro)}</span>
            </div>
            <span className="khung-pv__avatar" aria-hidden="true">{chuCaiDau(nguoiDung.hoTen)}</span>
          </div>
        </header>

        {/* tabIndex -1: cho phep link "Bo qua" dua focus vao day */}
        <main id="noi-dung-chinh" tabIndex={-1} className="khung-pv__noi-dung">{children}</main>
      </div>
    </div>
  )
}
