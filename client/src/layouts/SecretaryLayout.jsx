import AppLayout from './AppLayout.jsx'

/**
 * Menu cua Thu ky toa soan (luong royalty BF-04).
 * Man "Chấm nhuận bút" (UC-47, UC-48) mo tu danh sach bai cho cham, khong nam tren menu.
 * Duong dan (path) khai bao o routes/AppRoutes.jsx.
 *
 * MUC LUC (so dong)
 *   12  MENU_THU_KY                  - Menu cua Thu ky toa soan (luong royalty BF-04).
 *   25  SecretaryLayout              - Khung trang cua Thu ky toa soan.
 */
const MENU_THU_KY = [
  {
    tieuDe: '',
    cacMuc: [
      { path: '/royalty-grading', nhan: 'Chấm nhuận bút',                // UC-46 (ds bai cho cham)
        icon: <><circle cx="12" cy="12" r="8" /><path d="M14.5 9.5c-.5-1-1.5-1.5-2.5-1.5-1.4 0-2.5.8-2.5 2s1.1 1.6 2.5 2 2.5.8 2.5 2-1.1 2-2.5 2c-1 0-2-.5-2.5-1.5M12 6.5V8M12 16v1.5" /></> },
      { path: '/royalty-periods', nhan: 'Kỳ nhuận bút',                  // UC-49, UC-50 (ky bi tra lai - UC-52 - cung xem o day)
        icon: <><rect x="4" y="5" width="16" height="15" rx="2" /><path d="M4 10h16M9 3v4M15 3v4" /></> },
    ],
  },
]

/** Khung trang cua Thu ky toa soan. */
export default function SecretaryLayout({ nguoiDung, onDangXuat, children }) {
  return (
    <AppLayout
      nguoiDung={nguoiDung}
      onDangXuat={onDangXuat}
      tenMenu="Chức năng thư ký toà soạn"
      nhomMenu={MENU_THU_KY}
    >
      {children}
    </AppLayout>
  )
}
