import AppLayout from './AppLayout.jsx'

/**
 * Menu cua Ke toan (luong royalty BF-04).
 * Duyet bang ke / tra ky / khoa ky (UC-51, 52, 53) lam trong man chi tiet ky,
 * mo tu "Kỳ nhuận bút", khong nam tren menu.
 * Duong dan (path) khai bao o routes/AppRoutes.jsx.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  MENU_KE_TOAN                    xem tom tat dau file
 *   33  AccountantLayout()              Khung trang cua Ke toan
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   22  menu [Ky nhuan but]             mo trang /royalty-periods (RoyaltyPeriods)
 *   24  menu [Chi tra]                  mo trang /royalty-payments (ComingSoon - chua lam "Chi tra nhuan but")
 *   26  menu [Don gia nhuan but]        mo trang /royalty-rates (RoyaltyRates)
 */
const MENU_KE_TOAN = [
  {
    tieuDe: '',
    cacMuc: [
      { path: '/royalty-periods', nhan: 'Kỳ nhuận bút',                  // UC-49, 51, 52, 53
        icon: <><rect x="4" y="5" width="16" height="15" rx="2" /><path d="M4 10h16M9 3v4M15 3v4" /></> },
      { path: '/royalty-payments', nhan: 'Chi trả',                      // UC-54
        icon: <><rect x="3" y="6" width="18" height="13" rx="2" /><path d="M3 10h18" /><path d="M7 15h4" /></> },
      { path: '/royalty-rates', nhan: 'Đơn giá nhuận bút',               // UC-55
        icon: <><path d="M20 12 12 20l-8-8V4h8l8 8Z" /><circle cx="8.5" cy="8.5" r="1.2" /></> },
    ],
  },
]

/** Khung trang cua Ke toan. */
export default function AccountantLayout({ nguoiDung, onDangXuat, children }) {
  return (
    <AppLayout
      nguoiDung={nguoiDung}
      onDangXuat={onDangXuat}
      tenMenu="Chức năng kế toán"
      nhomMenu={MENU_KE_TOAN}
    >
      {children}
    </AppLayout>
  )
}
