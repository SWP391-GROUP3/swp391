import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { layViTien } from '../api/reporterHomeApi.js'
import { formatMoney } from '../utils/formatMoney.js'
import Icon from '../components/common/Icon.jsx'
import AppLayout from './AppLayout.jsx'
import '../styles/ReporterLayout.css'

/**
 * Menu cua Phong vien. Moi muc ghi UC tuong ung trong SRS.
 * Duong dan (path) khai bao o routes/AppRoutes.jsx.
 */
const MENU_PHONG_VIEN = [
  {
    tieuDe: '',
    cacMuc: [
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
    ],
  },
]

/** Chip vi tien canh ten, bam vao mo trang Nhuan but (UC-57). Chi Phong vien co. */
function ChipViTien() {
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
    <Link to="/my-royalties" className="khung-pv__vi" title={loiVi || 'Số dư ví'}>
      <Icon><rect x="3" y="6" width="18" height="13" rx="2" /><path d="M16 12.5h5" /></Icon>
      {/* Loi cung dua vao chu an, de trinh doc man hinh doc duoc (title thi khong) */}
      <span className="an-di">{loiVi ? `${loiVi}.` : 'Số dư ví:'}</span>
      <span className="khung-pv__vi-so">{formatMoney(soDu)}</span>
    </Link>
  )
}

/** Khung trang cua Phong vien = khung chung + menu Phong vien + chip vi. */
export default function ReporterLayout({ nguoiDung, onDangXuat, children }) {
  return (
    <AppLayout
      nguoiDung={nguoiDung}
      onDangXuat={onDangXuat}
      tenMenu="Chức năng phóng viên"
      nhomMenu={MENU_PHONG_VIEN}
      phanThem={<ChipViTien />}
    >
      {children}
    </AppLayout>
  )
}
