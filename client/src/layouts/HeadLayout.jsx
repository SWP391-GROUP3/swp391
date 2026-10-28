import AppLayout from './AppLayout.jsx'

/**
 * Menu cua Truong ban bien tap. Nhieu viec nhat nen chia 3 nhom co tieu de.
 * Duong dan (path) khai bao o routes/AppRoutes.jsx.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  MENU_TRUONG_BAN                 xem tom tat dau file
 *   65  HeadLayout()                    Khung trang cua Truong ban bien tap
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   28  menu [Phan cong cham]           mo trang /assignments (AssignmentQueue)
 *   30  menu [Duyet diem]               mo trang /score-approvals (ScoreApprovalQueue)
 *   32  menu [Duyet dang]               mo trang /publishing (PublishingQueue)
 *   39  menu [Khieu nai]                mo trang /appeals (AppealManagementPage)
 *   41  menu [Dot cham cheo]            mo trang /cross-reviews (CrossReviewBatchPage)
 *   43  menu [Do lech & ho so BTV]      mo trang /reviewers (ReviewerDeviationPage)
 *   45  menu [Nhat ky cham]             mo trang /audit-logs (AuditLogPage)
 *   52  menu [Bang KPI cua ban]         mo trang /kpi-board (DepartmentKpiDashboard)
 *   54  menu [Giao chi tieu thang]      mo trang /kpi-targets (MonthlyTargetSetting)
 *   56  menu [Dinh muc chuan]           mo trang /kpi-benchmarks (KpiBenchmarksConfig)
 *   58  menu [Bao cao]                  mo trang /kpi-reports (ComingSoon - chua lam "Bao cao")
 */
const MENU_TRUONG_BAN = [
  {
    tieuDe: 'Duyệt bài',                                                 // BF-02
    cacMuc: [
      { path: '/assignments', nhan: 'Phân công chấm',                    // UC-18, 19, 20
        icon: <><circle cx="9" cy="8" r="3" /><path d="M3 20a6 6 0 0 1 12 0" /><path d="M19 8v6M16 11h6" /></> },
      { path: '/score-approvals', nhan: 'Duyệt điểm',                    // UC-29…32
        icon: <><rect x="5" y="4" width="14" height="17" rx="2" /><path d="M9 4h6v3H9Z" /><path d="m9 14 2 2 4-4" /></> },
      { path: '/publishing', nhan: 'Duyệt đăng',                         // UC-33
        icon: <><path d="M21 3 3 10.5l7 2.5 2.5 7L21 3Z" /><path d="m10 13 4-4" /></> },
    ],
  },
  {
    tieuDe: 'Chất lượng chấm',                                           // BF-03
    cacMuc: [
      { path: '/appeals', nhan: 'Khiếu nại',                             // UC-38, 39, 37 (xac nhan S2), 43 (giao lai)
        icon: <><path d="M12 3v18M5 21h14" /><path d="M5 7h14" /><path d="M5 7l-3 7a3 3 0 0 0 6 0L5 7ZM19 7l-3 7a3 3 0 0 0 6 0l-3-7Z" /></> },
      { path: '/cross-reviews', nhan: 'Đợt chấm chéo',                   // UC-40
        icon: <><path d="M4 8h14l-3-3M20 16H6l3 3" /></> },
      { path: '/reviewers', nhan: 'Độ lệch & hồ sơ BTV',                 // UC-41, 42
        icon: <><circle cx="9" cy="8" r="3" /><path d="M3 20a6 6 0 0 1 12 0" /><path d="M16 5a3 3 0 0 1 0 6M18 20a6 6 0 0 0-3-5.2" /></> },
      { path: '/audit-logs', nhan: 'Nhật ký chấm',                       // UC-45
        icon: <><circle cx="12" cy="12" r="8" /><path d="M12 8v4l3 2" /></> },
    ],
  },
  {
    tieuDe: 'KPI & báo cáo',                                             // BF-05
    cacMuc: [
      { path: '/kpi-board', nhan: 'Bảng KPI của ban',                    // UC-61, 62, 66 (chot thang)
        icon: <><path d="M4 20V10M10 20V4M16 20v-7M22 20H2" /></> },
      { path: '/kpi-targets', nhan: 'Giao chỉ tiêu tháng',               // UC-59
        icon: <><circle cx="12" cy="12" r="8" /><circle cx="12" cy="12" r="4" /><circle cx="12" cy="12" r=".6" /></> },
      { path: '/kpi-benchmarks', nhan: 'Định mức chuẩn',                 // UC-58
        icon: <><path d="M4 7h10M18 7h2M4 17h4M12 17h8" /><circle cx="16" cy="7" r="2" /><circle cx="10" cy="17" r="2" /></> },
      { path: '/kpi-reports', nhan: 'Báo cáo',                           // UC-64, 65, 68
        icon: <><path d="M6 3h9l4 4v14H6V3Z" /><path d="M15 3v4h4" /><path d="M9 17v-3M12 17v-6M15 17v-4" /></> },
    ],
  },
]

/** Khung trang cua Truong ban bien tap. */
export default function HeadLayout({ nguoiDung, onDangXuat, children }) {
  return (
    <AppLayout
      nguoiDung={nguoiDung}
      onDangXuat={onDangXuat}
      tenMenu="Chức năng trưởng ban"
      nhomMenu={MENU_TRUONG_BAN}
    >
      {children}
    </AppLayout>
  )
}
