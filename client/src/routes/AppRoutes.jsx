import { Routes, Route, Navigate, Outlet } from 'react-router-dom'
import RequireRole from './RequireRole.jsx'
import Login from '../pages/Login.jsx'
import Main from '../pages/Main.jsx'
import ComingSoon from '../pages/ComingSoon.jsx'
import ReporterLayout from '../layouts/ReporterLayout.jsx'
import ReporterHome from '../pages/home/ReporterHome.jsx'
import MyArticles from '../pages/article/MyArticles.jsx'
import ArticleDetail from '../pages/article/ArticleDetail.jsx'

/**
 * Trang cua Phong vien chua lam: menu (ReporterLayout) va nut trong
 * "Can ban xu ly" tro toi day. Lam xong trang nao thi bo dong do khoi mang
 * va khai bao <Route> rieng tro toi trang that (trong nhom Phong vien ben duoi).
 */
const TRANG_PV_CHUA_LAM = [
  { path: '/my-articles/new',   ten: 'Soạn bài mới' },           // UC-03
  { path: '/co-author-invites', ten: 'Xác nhận đồng tác giả' },  // UC-12
  { path: '/my-appeals',        ten: 'Khiếu nại điểm' },         // UC-35
  { path: '/my-appeals/new',    ten: 'Gửi khiếu nại' },          // UC-35
  { path: '/my-royalties',      ten: 'Nhuận bút' },              // UC-57
  { path: '/my-kpi',            ten: 'KPI cá nhân' },            // UC-60, UC-67
]

/**
 * Khai bao toan bo route cua app (AGENTS.md muc 4: route moi them o day).
 *
 * Moi vai tro co 1 NHOM route rieng. Nguoi dang nhap la vai tro nao thi
 * chi nhom do ton tai, nen cung la "/main" nhung Phong vien thay trang chu
 * cua Phong vien, vai tro khac thay Main cu.
 */
export default function AppRoutes({ nguoiDung, onDangNhap, onDangXuat }) {
  const laPhongVien = nguoiDung?.vaiTro?.includes('REPORTER')

  return (
    <Routes>
      {/* Vao "/" thi day sang /main; chua dang nhap thi /main lai day tiep ve /login */}
      <Route path="/" element={<Navigate to="/main" replace />} />

      <Route
        path="/login"
        element={
          // Da dang nhap roi thi khong cho xem lai form nua
          nguoiDung
            ? <Navigate to="/main" replace />
            : <Login onDangNhap={onDangNhap} />
        }
      />

      {laPhongVien ? (
        // ---- Nhom Phong vien ----
        // Route cha khong co path, chi ve KHUNG (sidebar + thanh tren).
        // <Outlet /> la cho trong de route con (trang chu, bai cua toi...)
        // hien vao. Doi trang chi thay phan Outlet, khung giu nguyen
        // -> khong tai lai so du vi moi lan bam menu.
        <Route
          element={
            <RequireRole nguoiDung={nguoiDung} vaiTro="REPORTER">
              <ReporterLayout nguoiDung={nguoiDung} onDangXuat={onDangXuat}>
                <Outlet />
              </ReporterLayout>
            </RequireRole>
          }
        >
          <Route path="/main" element={<ReporterHome nguoiDung={nguoiDung} />} />
          {/* BF-01: UC-01 bai cua toi, UC-02 chi tiet bai (+ UC-11 rut bai) */}
          <Route path="/my-articles" element={<MyArticles nguoiDung={nguoiDung} />} />
          <Route path="/my-articles/:id" element={<ArticleDetail nguoiDung={nguoiDung} />} />
          {TRANG_PV_CHUA_LAM.map((trang) => (
            <Route
              key={trang.path}
              path={trang.path}
              element={<ComingSoon tenChucNang={trang.ten} />}
            />
          ))}
        </Route>
      ) : (
        // ---- Vai tro khac: van dung Main cu cho toi khi co layout rieng ----
        <Route
          path="/main"
          element={
            <RequireRole nguoiDung={nguoiDung}>
              <Main nguoiDung={nguoiDung} onDangXuat={onDangXuat} />
            </RequireRole>
          }
        />
      )}

      {/* Duong dan la (hoac trang cua vai tro khac) - nem ve trang chu */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
