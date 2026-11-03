import AppLayout from './AppLayout.jsx'

/**
 * Menu cua Quan tri he thong (Admin). Chia 3 nhom co tieu de.
 * "Tài khoản", "Thể loại" chua co UC rieng trong SRS: lay theo mo ta Actor 1
 * (SRS_Overview 4.1: quan ly tai khoan, the loai) va bang ma luong admin so huu.
 * Duong dan (path) khai bao o routes/AppRoutes.jsx.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   25  MENU_ADMIN                      xem tom tat dau file
 *   64  AdminLayout()                   Khung trang cua Admin
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   29  menu [Tai khoan]                mo trang /users (UserList)
 *   31  menu [The loai]                 mo trang /genres (GenreList)
 *   33  menu [Tham so he thong]         mo trang /system-parameters (SystemParameters)
 *   40  menu [Bo tieu chi]              mo trang /criteria-sets (CriteriaManagement)
 *   42  menu [Cau hinh khieu nai]       mo trang /appeal-rules (AppealRuleConfigPage)
 *   44  menu [Ho so bien tap vien]      mo trang /reviewers (ReviewerDeviationPage)
 *   46  menu [Nhat ky he thong]         mo trang /audit-logs (AuditLogPage)
 *   53  menu [He so anh/video]          mo trang /media-factors (MediaFactors)
 *   55  menu [Dinh muc KPI chuan]       mo trang /kpi-benchmarks (KpiBenchmarksConfig)
 *   57  menu [Nhap so lieu doc gia]     mo trang /reader-imports (ReaderDataImport)
 */
const MENU_ADMIN = [
  {
    tieuDe: 'Hệ thống',
    cacMuc: [
      { path: '/users', nhan: 'Tài khoản',                               // users (Actor 1)
        icon: <><circle cx="9" cy="8" r="3" /><path d="M3 20a6 6 0 0 1 12 0" /><path d="M16 5a3 3 0 0 1 0 6M18 20a6 6 0 0 0-3-5.2" /></> },
      { path: '/genres', nhan: 'Thể loại',                               // genres (Actor 1)
        icon: <><path d="M4 6h16M4 12h16M4 18h10" /></> },
      { path: '/system-parameters', nhan: 'Tham số hệ thống',            // UC-13 + system_parameters
        icon: <><path d="M4 7h10M18 7h2M4 17h4M12 17h8" /><circle cx="16" cy="7" r="2" /><circle cx="10" cy="17" r="2" /></> },
    ],
  },
  {
    tieuDe: 'Chấm điểm & khiếu nại',
    cacMuc: [
      { path: '/criteria-sets', nhan: 'Bộ tiêu chí',                     // UC-14…17
        icon: <><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4h6v3H9Z" /><path d="M9 12h6M9 16h4" /></> },
      { path: '/appeal-rules', nhan: 'Cấu hình khiếu nại',               // UC-44
        icon: <><path d="M12 3v18M5 21h14" /><path d="M5 7h14" /><path d="M5 7l-3 7a3 3 0 0 0 6 0L5 7ZM19 7l-3 7a3 3 0 0 0 6 0l-3-7Z" /></> },
      { path: '/reviewers', nhan: 'Hồ sơ biên tập viên',                 // UC-42
        icon: <><circle cx="12" cy="8" r="3.5" /><path d="M5 20a7 7 0 0 1 14 0" /></> },
      { path: '/audit-logs', nhan: 'Nhật ký hệ thống',                   // UC-45
        icon: <><circle cx="12" cy="12" r="8" /><path d="M12 8v4l3 2" /></> },
    ],
  },
  {
    tieuDe: 'Nhuận bút & KPI',
    cacMuc: [
      { path: '/media-factors', nhan: 'Hệ số ảnh/video',                 // UC-56
        icon: <><rect x="3" y="5" width="18" height="14" rx="2" /><circle cx="9" cy="10" r="1.6" /><path d="m21 16-5-5-8 8" /></> },
      { path: '/kpi-benchmarks', nhan: 'Định mức KPI chuẩn',             // UC-58
        icon: <><circle cx="12" cy="12" r="8" /><circle cx="12" cy="12" r="4" /><circle cx="12" cy="12" r=".6" /></> },
      { path: '/reader-imports', nhan: 'Nhập số liệu độc giả',           // UC-63
        icon: <><path d="M12 4v11M7 10l5 5 5-5" /><path d="M4 18v2h16v-2" /></> },
    ],
  },
]

/** Khung trang cua Admin. */
export default function AdminLayout({ nguoiDung, onDangXuat, children }) {
  return (
    <AppLayout
      nguoiDung={nguoiDung}
      onDangXuat={onDangXuat}
      tenMenu="Chức năng quản trị"
      nhomMenu={MENU_ADMIN}
    >
      {children}
    </AppLayout>
  )
}
