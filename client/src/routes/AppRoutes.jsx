import { Routes, Route, Navigate, Outlet } from 'react-router-dom'
import RequireRole from './RequireRole.jsx'
import Login from '../pages/Login.jsx'
import ComingSoon from '../pages/ComingSoon.jsx'
import ReporterLayout from '../layouts/ReporterLayout.jsx'
import EditorLayout from '../layouts/EditorLayout.jsx'
import SecretaryLayout from '../layouts/SecretaryLayout.jsx'
import HeadLayout from '../layouts/HeadLayout.jsx'
import AccountantLayout from '../layouts/AccountantLayout.jsx'
import AdminLayout from '../layouts/AdminLayout.jsx'
import ReporterHome from '../pages/home/ReporterHome.jsx'
import EditorHome from '../pages/home/EditorHome.jsx'
import SecretaryHome from '../pages/home/SecretaryHome.jsx'
import HeadHome from '../pages/home/HeadHome.jsx'
import AccountantHome from '../pages/home/AccountantHome.jsx'
import AdminHome from '../pages/home/AdminHome.jsx'
import MyArticles from '../pages/article/MyArticles.jsx'
import ArticleDetail from '../pages/article/ArticleDetail.jsx'
import MonthlyTargetSetting from '../pages/kpi/MonthlyTargetSetting.jsx'
import KpiBenchmarksConfig from '../pages/kpi/KpiBenchmarksConfig.jsx'
import DepartmentKpiDashboard from '../pages/kpi/DepartmentKpiDashboard.jsx'
import AppealRuleConfigPage from '../pages/admin/AppealRuleConfigPage.jsx'
import RoyaltyPeriods from '../pages/royalty/RoyaltyPeriods.jsx'
import RoyaltyRates from '../pages/royalty/RoyaltyRates.jsx'
import MediaFactors from '../pages/royalty/MediaFactors.jsx'

/*
 * Trang CHUA LAM cua tung vai tro: menu va nut tren trang chu tro toi day,
 * tam hien ComingSoon. Lam xong trang nao thi bo dong do khoi mang
 * va khai bao <Route> rieng tro toi trang that (xem phan "trang da lam" ben duoi).
 */
const TRANG_PV_CHUA_LAM = [
  { path: '/my-articles/new',   ten: 'Soạn bài mới' },           // UC-03
  { path: '/co-author-invites', ten: 'Xác nhận đồng tác giả' },  // UC-12
  { path: '/my-appeals',        ten: 'Khiếu nại điểm' },         // UC-35
  { path: '/my-appeals/new',    ten: 'Gửi khiếu nại' },          // UC-35
  { path: '/my-royalties',      ten: 'Nhuận bút' },              // UC-57
  { path: '/my-kpi',            ten: 'KPI cá nhân' },            // UC-60, UC-67
]

const TRANG_BTV_CHUA_LAM = [
  { path: '/assigned-articles',     ten: 'Bài được giao' },          // UC-21
  { path: '/assigned-articles/:id', ten: 'Chấm bài' },               // UC-22…28
  { path: '/re-reviews',            ten: 'Chấm lại & chấm chéo' },   // UC-36, UC-37
  { path: '/re-reviews/:id',        ten: 'Phiếu chấm lại' },         // UC-36, UC-37
  { path: '/my-scorings',           ten: 'Lịch sử chấm' },           // UC-21
]

const TRANG_TK_CHUA_LAM = [
  { path: '/royalty-grading',            ten: 'Bài chờ chấm nhuận bút' },  // UC-46
  { path: '/royalty-grading/:articleId', ten: 'Chấm nhuận bút' },          // UC-47, UC-48
  { path: '/royalty-periods/:id',        ten: 'Bảng kê kỳ nhuận bút' },    // UC-50
]

const TRANG_TB_CHUA_LAM = [
  { path: '/assignments',           ten: 'Phân công chấm' },          // UC-18, 19, 20
  { path: '/score-approvals',       ten: 'Duyệt điểm' },              // UC-29
  { path: '/score-approvals/:id',   ten: 'Chi tiết kết quả chấm' },   // UC-30, 31, 32
  { path: '/publishing',            ten: 'Duyệt đăng' },              // UC-33
  { path: '/appeals',               ten: 'Khiếu nại' },               // UC-38
  { path: '/appeals/:id',           ten: 'Chi tiết khiếu nại' },      // UC-39, 37, 43
  { path: '/cross-reviews',         ten: 'Đợt chấm chéo' },           // UC-40
  { path: '/reviewers',             ten: 'Độ lệch & hồ sơ BTV' },     // UC-41
  { path: '/reviewers/:id',         ten: 'Hồ sơ biên tập viên' },     // UC-42
  { path: '/audit-logs',            ten: 'Nhật ký chấm' },            // UC-45
  { path: '/kpi-board/:reporterId', ten: 'KPI phóng viên' },          // UC-62
  { path: '/kpi-reports',           ten: 'Báo cáo' },                 // UC-64, 65, 68
]

const TRANG_KT_CHUA_LAM = [
  { path: '/royalty-periods/:id', ten: 'Bảng kê kỳ nhuận bút' },    // UC-51, 52, 53
  { path: '/royalty-payments',    ten: 'Chi trả nhuận bút' },       // UC-54
]

const TRANG_ADMIN_CHUA_LAM = [
  { path: '/users',              ten: 'Tài khoản' },
  { path: '/genres',             ten: 'Thể loại' },
  { path: '/system-parameters',  ten: 'Tham số hệ thống' },        // UC-13
  { path: '/criteria-sets',      ten: 'Bộ tiêu chí' },             // UC-14, 15
  { path: '/criteria-sets/:id',  ten: 'Chi tiết bộ tiêu chí' },    // UC-16, 17
  { path: '/reviewers',          ten: 'Hồ sơ biên tập viên' },     // UC-42
  { path: '/reviewers/:id',      ten: 'Hồ sơ biên tập viên' },     // UC-42
  { path: '/audit-logs',         ten: 'Nhật ký hệ thống' },        // UC-45
  { path: '/reader-imports',     ten: 'Nhập số liệu độc giả' },    // UC-63
]

/**
 * Moi vai tro co khung trang (menu) rieng, khai bao o day:
 * Layout = khung + menu, TrangChu = trang hien o /main, trangChuaLam = danh sach o tren.
 */
const KHUNG_THEO_VAI_TRO = {
  REPORTER:            { Layout: ReporterLayout,   TrangChu: ReporterHome,   trangChuaLam: TRANG_PV_CHUA_LAM },
  EDITOR:              { Layout: EditorLayout,     TrangChu: EditorHome,     trangChuaLam: TRANG_BTV_CHUA_LAM },
  EDITORIAL_SECRETARY: { Layout: SecretaryLayout,  TrangChu: SecretaryHome,  trangChuaLam: TRANG_TK_CHUA_LAM },
  HEAD_OF_DEPARTMENT:  { Layout: HeadLayout,       TrangChu: HeadHome,       trangChuaLam: TRANG_TB_CHUA_LAM },
  ACCOUNTANT:          { Layout: AccountantLayout, TrangChu: AccountantHome, trangChuaLam: TRANG_KT_CHUA_LAM },
  ADMIN:               { Layout: AdminLayout,      TrangChu: AdminHome,      trangChuaLam: TRANG_ADMIN_CHUA_LAM },
}

/**
 * Khai bao toan bo route cua app (AGENTS.md muc 4: route moi them o day).
 *
 * Moi vai tro co 1 NHOM route rieng. Nguoi dang nhap la vai tro nao thi
 * chi nhom do ton tai, nen cung la "/main" nhung moi vai tro thay trang chu
 * cua minh. Bam vao duong dan cua vai tro khac -> khong co route -> ve trang chu.
 */
export default function AppRoutes({ nguoiDung, onDangNhap, onDangXuat }) {
  // Moi user chi co 1 vai tro (users.role_id)
  const vaiTro = nguoiDung?.vaiTro?.[0]
  const khung = KHUNG_THEO_VAI_TRO[vaiTro]
  const Layout = khung?.Layout
  const TrangChu = khung?.TrangChu

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

      {khung ? (
        // Route cha khong co path, chi ve KHUNG (sidebar + thanh tren).
        // <Outlet /> la cho trong de route con hien vao. Doi trang chi thay
        // phan Outlet, khung giu nguyen -> khong tai lai menu, so du vi...
        <Route
          element={
            <RequireRole nguoiDung={nguoiDung} vaiTro={vaiTro}>
              <Layout nguoiDung={nguoiDung} onDangXuat={onDangXuat}>
                <Outlet />
              </Layout>
            </RequireRole>
          }
        >
          {/* ---- Trang da lam ---- */}
          <Route path="/main" element={<TrangChu nguoiDung={nguoiDung} />} />
          {vaiTro === 'REPORTER' && (
            <>
              {/* BF-01 (Minh): UC-01 bai cua toi, UC-02 chi tiet bai (+ UC-11 rut bai) */}
              <Route path="/my-articles" element={<MyArticles nguoiDung={nguoiDung} />} />
              <Route path="/my-articles/:id" element={<ArticleDetail nguoiDung={nguoiDung} />} />
            </>
          )}
          {vaiTro === 'HEAD_OF_DEPARTMENT' && (
            <>
              {/* BF-05 (Khang) */}
              <Route path="/kpi-board" element={<DepartmentKpiDashboard nguoiDung={nguoiDung} />} />
              <Route path="/kpi-targets" element={<MonthlyTargetSetting nguoiDung={nguoiDung} />} />
              <Route path="/kpi-benchmarks" element={<KpiBenchmarksConfig />} />
            </>
          )}
          {vaiTro === 'EDITORIAL_SECRETARY' && (
            <>
              {/* BF-04 (Quang): UC-49 ky nhuan but */}
              <Route path="/royalty-periods" element={<RoyaltyPeriods />} />
            </>
          )}
          {vaiTro === 'ACCOUNTANT' && (
            <>
              {/* BF-04 (Quang): UC-49 ky nhuan but, UC-55 don gia */}
              <Route path="/royalty-periods" element={<RoyaltyPeriods />} />
              <Route path="/royalty-rates" element={<RoyaltyRates />} />
            </>
          )}
          {vaiTro === 'ADMIN' && (
            <>
              {/* BF-03 (Vu), BF-05 (Khang), BF-04 (Quang): UC-56 he so anh/video */}
              <Route path="/appeal-rules" element={<AppealRuleConfigPage />} />
              <Route path="/kpi-benchmarks" element={<KpiBenchmarksConfig />} />
              <Route path="/media-factors" element={<MediaFactors />} />
            </>
          )}

          {/* ---- Trang chua lam -> ComingSoon ---- */}
          {khung.trangChuaLam.map((trang) => (
            <Route
              key={trang.path}
              path={trang.path}
              element={<ComingSoon tenChucNang={trang.ten} />}
            />
          ))}
        </Route>
      ) : (
        // ---- Chua dang nhap (hoac vai tro la): van phai co route /main,
        // de RequireRole day ve /login. Thieu route nay thi "*" -> "/" -> "/main"
        // -> "*"... lap vo han. ----
        <Route
          path="/main"
          element={
            <RequireRole nguoiDung={nguoiDung}>
              <ComingSoon tenChucNang="Vai trò này chưa có giao diện" />
            </RequireRole>
          }
        />
      )}

      {/* Duong dan la (hoac trang cua vai tro khac) - nem ve trang chu */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
