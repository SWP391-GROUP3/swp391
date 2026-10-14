import AppLayout from './AppLayout.jsx'

/**
 * Menu cua Bien tap vien (luong assessment BF-02 + appeal BF-03).
 * Man "Chấm bài" (UC-22…28) mo tu danh sach "Bài được giao", khong nam tren menu.
 * Duong dan (path) khai bao o routes/AppRoutes.jsx.
 */
const MENU_BIEN_TAP_VIEN = [
  {
    tieuDe: '',
    cacMuc: [
      { path: '/assigned-articles', nhan: 'Bài được giao',               // UC-21
        icon: <><path d="M4 13h4l2 3h4l2-3h4" /><path d="M4 13 6.5 5h11L20 13v6H4v-6Z" /></> },
      { path: '/re-reviews', nhan: 'Chấm lại & chấm chéo',               // UC-36, UC-37 + phieu cham cheo (cross_review_tasks)
        icon: <><path d="M3 3l18 18" /><path d="M10.6 6.1A9 9 0 0 1 12 6c5 0 9 6 9 6a16 16 0 0 1-2.6 3.3M6.6 7.6C4.4 9.2 3 12 3 12s4 6 9 6a8.8 8.8 0 0 0 3.4-.7" /></> },
      { path: '/my-scorings', nhan: 'Lịch sử chấm',                      // UC-21 (bai da cham xong)
        icon: <><circle cx="12" cy="12" r="8" /><path d="M12 8v4l3 2" /></> },
    ],
  },
]

/** Khung trang cua Bien tap vien. */
export default function EditorLayout({ nguoiDung, onDangXuat, children }) {
  return (
    <AppLayout
      nguoiDung={nguoiDung}
      onDangXuat={onDangXuat}
      tenMenu="Chức năng biên tập viên"
      nhomMenu={MENU_BIEN_TAP_VIEN}
    >
      {children}
    </AppLayout>
  )
}
