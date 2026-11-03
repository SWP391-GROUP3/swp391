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
import AppealCreatePage from '../pages/appeal/AppealCreatePage.jsx'
import MyAppealsPage from '../pages/appeal/MyAppealsPage.jsx'
import ReviewerDeviationPage from '../pages/appeal/ReviewerDeviationPage.jsx'
import ReviewerProfilePage from '../pages/appeal/ReviewerProfilePage.jsx'
import AppealManagementPage from '../pages/appeal/AppealManagementPage.jsx'
import AppealDetailPage from '../pages/appeal/AppealDetailPage.jsx'
import BlindReviewQueuePage from '../pages/appeal/BlindReviewQueuePage.jsx'
import BlindReviewPage from '../pages/appeal/BlindReviewPage.jsx'
import AppealComparisonPage from '../pages/appeal/AppealComparisonPage.jsx'
import CrossReviewBatchPage from '../pages/appeal/CrossReviewBatchPage.jsx'
import CrossReviewBatchDetailPage from '../pages/appeal/CrossReviewBatchDetailPage.jsx'
import AuditLogPage from '../pages/appeal/AuditLogPage.jsx'
import ReporterHome from '../pages/home/ReporterHome.jsx'
import EditorHome from '../pages/home/EditorHome.jsx'
import SecretaryHome from '../pages/home/SecretaryHome.jsx'
import HeadHome from '../pages/home/HeadHome.jsx'
import AccountantHome from '../pages/home/AccountantHome.jsx'
import AdminHome from '../pages/home/AdminHome.jsx'
import MyArticles from '../pages/article/MyArticles.jsx'
import ArticleDetail from '../pages/article/ArticleDetail.jsx'
import CoAuthorInvites from '../pages/article/CoAuthorInvites.jsx'
import ArticleEditor from '../pages/article/ArticleEditor.jsx'
import MonthlyTargetSetting from '../pages/kpi/MonthlyTargetSetting.jsx'
import KpiBenchmarksConfig from '../pages/kpi/KpiBenchmarksConfig.jsx'
import DepartmentKpiDashboard from '../pages/kpi/DepartmentKpiDashboard.jsx'
import ReporterKpiDetails from '../pages/kpi/ReporterKpiDetails.jsx'
import PersonalKpiProgress from '../pages/kpi/PersonalKpiProgress.jsx'
import GenrePopularityReport from '../pages/kpi/GenrePopularityReport.jsx'
import TrendingEventsReport from '../pages/kpi/TrendingEventsReport.jsx'
import MonthlyKpiFinalization from '../pages/kpi/MonthlyKpiFinalization.jsx'
import AppealRuleConfigPage from '../pages/admin/AppealRuleConfigPage.jsx'
import RoyaltyPeriods from '../pages/royalty/RoyaltyPeriods.jsx'
import RoyaltyPeriodDetail from '../pages/royalty/RoyaltyPeriodDetail.jsx'
import RoyaltyPendingList from '../pages/royalty/RoyaltyPendingList.jsx'
import RoyaltyGrading from '../pages/royalty/RoyaltyGrading.jsx'
import RoyaltyRates from '../pages/royalty/RoyaltyRates.jsx'
import MediaFactors from '../pages/royalty/MediaFactors.jsx'
import RoyaltyPayments from '../pages/royalty/RoyaltyPayments.jsx'
import MyRoyalties from '../pages/royalty/MyRoyalties.jsx'
import ReaderDataImport from '../pages/admin/ReaderDataImport.jsx'
import SystemParameters from '../pages/admin/SystemParameters.jsx'
import GenreList from '../pages/admin/GenreList.jsx'
import UserList from '../pages/admin/UserList.jsx'
import CriteriaManagement from '../pages/flow2/CriteriaManagement.jsx'
import AssignmentQueue from '../pages/flow2/AssignmentQueue.jsx'
import EditorTaskQueue from '../pages/flow2/EditorTaskQueue.jsx'
import MyScorings from '../pages/flow2/MyScorings.jsx'
import ScoringSheet from '../pages/flow2/ScoringSheet.jsx'
import ScoreApprovalQueue from '../pages/flow2/ScoreApprovalQueue.jsx'
import PublishingQueue from '../pages/flow2/PublishingQueue.jsx'
import UserProfile from '../pages/profile/UserProfile.jsx'

/*
 * Trang CHUA LAM cua tung vai tro: menu va nut tren trang chu tro toi day,
 * tam hien ComingSoon. Lam xong trang nao thi bo dong do khoi mang
 * va khai bao <Route> rieng tro toi trang that (xem phan "trang da lam" ben duoi).
 */
// Phong vien: UC-57 Nhuan but da lam (Quang, 02/11/2026) -> mang rong
const TRANG_PV_CHUA_LAM = []

// BTV: M-A7 da co trang Lich su cham -> mang rong
const TRANG_BTV_CHUA_LAM = []

// Thu ky: Iter 2 da lam het (UC-46, 47, 48, 50) -> mang rong
const TRANG_TK_CHUA_LAM = []

// Truong ban: moi trang trong menu da co trang that (M-D1, 03/11) -> mang rong
const TRANG_TB_CHUA_LAM = []

// Ke toan: UC-54 Chi tra nhuan but da lam (Quang, 02/11/2026) -> mang rong
const TRANG_KT_CHUA_LAM = []

// Admin: tai khoan, the loai, tham so (UC-13) da lam (Minh, 03/11) -> mang rong
const TRANG_ADMIN_CHUA_LAM = []

/**
 * Moi vai tro co khung trang (menu) rieng, khai bao o day:
 * Layout = khung + menu, TrangChu = trang hien o /main, trangChuaLam = danh sach o tren.
 */
const KHUNG_THEO_VAI_TRO = {
  REPORTER: { Layout: ReporterLayout, TrangChu: ReporterHome, trangChuaLam: TRANG_PV_CHUA_LAM },
  EDITOR: { Layout: EditorLayout, TrangChu: EditorHome, trangChuaLam: TRANG_BTV_CHUA_LAM },
  EDITORIAL_SECRETARY: { Layout: SecretaryLayout, TrangChu: SecretaryHome, trangChuaLam: TRANG_TK_CHUA_LAM },
  HEAD_OF_DEPARTMENT: { Layout: HeadLayout, TrangChu: HeadHome, trangChuaLam: TRANG_TB_CHUA_LAM },
  ACCOUNTANT: { Layout: AccountantLayout, TrangChu: AccountantHome, trangChuaLam: TRANG_KT_CHUA_LAM },
  ADMIN: { Layout: AdminLayout, TrangChu: AdminHome, trangChuaLam: TRANG_ADMIN_CHUA_LAM },
}

/**
 * Khai bao toan bo route cua app (AGENTS.md muc 4: route moi them o day).
 *
 * Moi vai tro co 1 NHOM route rieng. Nguoi dang nhap la vai tro nao thi
 * chi nhom do ton tai, nen cung la "/main" nhung moi vai tro thay trang chu
 * cua minh. Bam vao duong dan cua vai tro khac -> khong co route -> ve trang chu.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   69  TRANG_PV_CHUA_LAM               Phong vien: UC-57 Nhuan but da lam (Quang, 02/11/2026) -> mang rong
 *   72  TRANG_BTV_CHUA_LAM              BTV: M-A7 da co trang Lich su cham -> mang rong
 *   75  TRANG_TK_CHUA_LAM               Thu ky: Iter 2 da lam het (UC-46, 47, 48, 50) -> mang rong
 *   78  TRANG_TB_CHUA_LAM               Truong ban: moi trang trong menu da co trang that (M-D1, 03/11) -> mang rong
 *   81  TRANG_KT_CHUA_LAM               Ke toan: UC-54 Chi tra nhuan but da lam (Quang, 02/11/2026) -> mang rong
 *   84  TRANG_ADMIN_CHUA_LAM            Admin: tai khoan, the loai, tham so (UC-13) da lam (Minh, 03/11) -> mang rong
 *   90  KHUNG_THEO_VAI_TRO              Moi vai tro co khung trang (menu) rieng, khai bao o day: Layout = khung + menu, TrangChu = trang hien o /main…
 *  181  AppRoutes()                     xem tom tat dau file
 *
 * DUONG DAN -> TRANG (so dong - path - component)
 *  191  /                               chuyen tiep -> /main
 *  194  /login                          chuyen tiep -> /main hoac Login (tuy da dang nhap chua)
 *  217  /main                           TrangChu
 *  220  /profile                        UserProfile
 *  223  /my-appeals                     MyAppealsPage
 *  224  /my-appeals/new                 AppealCreatePage
 *  226  /my-articles                    MyArticles
 *  227  /my-articles/:id                ArticleDetail
 *  229  /my-articles/new                ArticleEditor
 *  230  /my-articles/:id/edit           ArticleEditor
 *  232  /co-author-invites              CoAuthorInvites
 *  234  /my-kpi                         PersonalKpiProgress
 *  236  /my-royalties                   MyRoyalties
 *  242  /assigned-articles              EditorTaskQueue
 *  243  /my-scorings                    MyScorings
 *  244  /re-reviews                     BlindReviewQueuePage
 *  245  /re-reviews/:id                 BlindReviewPage
 *  246  /cross-review-tasks/:id         BlindReviewPage
 *  247  /assigned-articles/:articleId   ScoringSheet
 *  249  /articles/:id                   ArticleDetail
 *  255  /assignments                    AssignmentQueue
 *  257  /score-approvals                ScoreApprovalQueue
 *  258  /score-approvals/:id            ScoreApprovalQueue
 *  259  /publishing                     PublishingQueue
 *  261  /appeals                        AppealManagementPage
 *  262  /appeals/:id                    AppealDetailPage
 *  263  /appeals/:id/comparison         AppealComparisonPage
 *  264  /cross-reviews                  CrossReviewBatchPage
 *  265  /cross-reviews/:id              CrossReviewBatchDetailPage
 *  268  /articles/:id                   ArticleDetail
 *  270  /kpi-board                      DepartmentKpiDashboard
 *  271  /kpi-board/:reporterId          ReporterKpiDetails
 *  272  /kpi-targets                    MonthlyTargetSetting
 *  273  /kpi-benchmarks                 KpiBenchmarksConfig
 *  274  /kpi-reports                    GenrePopularityReport
 *  275  /kpi-trending                   TrendingEventsReport
 *  276  /kpi-finalization               MonthlyKpiFinalization
 *  282  /royalty-grading                RoyaltyPendingList
 *  283  /royalty-grading/:articleId     RoyaltyGrading
 *  284  /royalty-periods                RoyaltyPeriods
 *  285  /royalty-periods/:id            RoyaltyPeriodDetail
 *  292  /royalty-periods                RoyaltyPeriods
 *  293  /royalty-periods/:id            RoyaltyPeriodDetail
 *  294  /royalty-rates                  RoyaltyRates
 *  296  /royalty-payments               RoyaltyPayments
 *  302  /criteria-sets                  CriteriaManagement
 *  304  /appeal-rules                   AppealRuleConfigPage
 *  305  /kpi-benchmarks                 KpiBenchmarksConfig
 *  306  /media-factors                  MediaFactors
 *  307  /reader-imports                 ReaderDataImport
 *  309  /system-parameters              SystemParameters
 *  310  /genres                         GenreList
 *  311  /users                          UserList
 *  317  /reviewers                      ReviewerDeviationPage
 *  318  /reviewers/:id                  ReviewerProfilePage
 *  319  /audit-logs                     AuditLogPage
 *  337  /main                           RequireRole
 *  347  *                               chuyen tiep -> /
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  191  tu chuyen trang (Navigate)      chuyen sang /main (trang chu theo vai tro)
 *  198  tu chuyen trang (Navigate)      chuyen sang /main (trang chu theo vai tro)
 *  347  tu chuyen trang (Navigate)      chuyen sang / (Navigate)
 */
export default function AppRoutes({ nguoiDung, onDangNhap, onDangXuat, onCapNhatNguoiDung }) {
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

          {/* Ho so ca nhan: moi vai tro deu co, mo tu menu avatar o thanh tren */}
          <Route path="/profile" element={<UserProfile onCapNhatNguoiDung={onCapNhatNguoiDung} />} />
          {vaiTro === 'REPORTER' && (
            <>
              <Route path="/my-appeals" element={<MyAppealsPage />} />
              <Route path="/my-appeals/new" element={<AppealCreatePage />} /> {/* [LUONG 1 - GUI KHIEU NAI] buoc 1/6 route: /my-appeals/new -> AppealCreatePage */}
              {/* BF-01 (Minh): UC-01 bai cua toi, UC-02 chi tiet bai (+ UC-11 rut bai) */}
              <Route path="/my-articles" element={<MyArticles nguoiDung={nguoiDung} />} />
              <Route path="/my-articles/:id" element={<ArticleDetail nguoiDung={nguoiDung} />} />
              {/* BF-01 (Minh) Iter 2: UC-03, 04, 05 soan / sua / nop bai, UC-10 sua va nop lai */}
              <Route path="/my-articles/new" element={<ArticleEditor nguoiDung={nguoiDung} />} />
              <Route path="/my-articles/:id/edit" element={<ArticleEditor nguoiDung={nguoiDung} />} />
              {/* BF-01 (Minh): UC-12 danh sach loi moi dong tac gia */}
              <Route path="/co-author-invites" element={<CoAuthorInvites />} />
              {/* BF-05 (Khang): UC-60 tien do KPI ca nhan, UC-67 xem ket qua chinh thuc */}
              <Route path="/my-kpi" element={<PersonalKpiProgress nguoiDung={nguoiDung} />} />
              {/* BF-04 (Quang): UC-57 nhuan but cua toi */}
              <Route path="/my-royalties" element={<MyRoyalties />} />
            </>
          )}
          {vaiTro === 'EDITOR' && (
            <>
              {/* BF-02 (Duong): UC-21 Bài được giao & UC-22..28 Chấm điểm */}
              <Route path="/assigned-articles" element={<EditorTaskQueue nguoiDung={nguoiDung} />} />
              <Route path="/my-scorings" element={<MyScorings />} />
              <Route path="/re-reviews" element={<BlindReviewQueuePage />} />
              <Route path="/re-reviews/:id" element={<BlindReviewPage />} /> {/* [LUONG 3 - CHAM MU] buoc 1/7 route: /re-reviews/:id -> BlindReviewPage */}
              <Route path="/cross-review-tasks/:id" element={<BlindReviewPage cheo />} />
              <Route path="/assigned-articles/:articleId" element={<ScoringSheet nguoiDung={nguoiDung} />} />
              {/* BF-01 (Minh) Iter 3: UC-02 BTV duoc giao xem chi tiet bai (chi xem) */}
              <Route path="/articles/:id" element={<ArticleDetail nguoiDung={nguoiDung} />} />
            </>
          )}
          {vaiTro === 'HEAD_OF_DEPARTMENT' && (
            <>
              {/* BF-02 (Duong): UC-18, 19, 20 Phân công chấm, UC-29..32 Duyệt điểm & UC-33 Duyệt đăng */}
              <Route path="/assignments" element={<AssignmentQueue nguoiDung={nguoiDung} />} />

              <Route path="/score-approvals" element={<ScoreApprovalQueue nguoiDung={nguoiDung} />} />
              <Route path="/score-approvals/:id" element={<ScoreApprovalQueue nguoiDung={nguoiDung} />} />
              <Route path="/publishing" element={<PublishingQueue nguoiDung={nguoiDung} />} />

              <Route path="/appeals" element={<AppealManagementPage />} />
              <Route path="/appeals/:id" element={<AppealDetailPage />} /> {/* [LUONG 2 - PHAN CONG] buoc 1/6 route: /appeals/:id -> AppealDetailPage */}
              <Route path="/appeals/:id/comparison" element={<AppealComparisonPage />} /> {/* [LUONG 4 - SO SANH & CHOT] buoc 1/7 route: /appeals/:id/comparison -> AppealComparisonPage */}
              <Route path="/cross-reviews" element={<CrossReviewBatchPage />} />
              <Route path="/cross-reviews/:id" element={<CrossReviewBatchDetailPage />} />

              {/* BF-01 (Minh) Iter 3: UC-02 Truong ban xem chi tiet bai cung ban (chi xem) */}
              <Route path="/articles/:id" element={<ArticleDetail nguoiDung={nguoiDung} />} />
              {/* BF-05 (Khang) */}
              <Route path="/kpi-board" element={<DepartmentKpiDashboard nguoiDung={nguoiDung} />} />
              <Route path="/kpi-board/:reporterId" element={<ReporterKpiDetails nguoiDung={nguoiDung} />} />
              <Route path="/kpi-targets" element={<MonthlyTargetSetting nguoiDung={nguoiDung} />} />
              <Route path="/kpi-benchmarks" element={<KpiBenchmarksConfig />} />
              <Route path="/kpi-reports" element={<GenrePopularityReport nguoiDung={nguoiDung} />} />
              <Route path="/kpi-trending" element={<TrendingEventsReport nguoiDung={nguoiDung} />} />
              <Route path="/kpi-finalization" element={<MonthlyKpiFinalization nguoiDung={nguoiDung} />} />
            </>
          )}
          {vaiTro === 'EDITORIAL_SECRETARY' && (
            <>
              {/* BF-04 (Quang): UC-46 bai cho cham, UC-47/48 cham nhuan but, UC-49 ky, UC-50 bang ke + chot ky */}
              <Route path="/royalty-grading" element={<RoyaltyPendingList />} />
              <Route path="/royalty-grading/:articleId" element={<RoyaltyGrading />} />
              <Route path="/royalty-periods" element={<RoyaltyPeriods />} />
              <Route path="/royalty-periods/:id" element={<RoyaltyPeriodDetail nguoiDung={nguoiDung} />} />
            </>
          )}
          {vaiTro === 'ACCOUNTANT' && (
            <>
              {/* BF-04 (Quang): UC-49 ky nhuan but, UC-55 don gia. Bang ke ky: Ke toan chi xem
                  (khoa ky / tra ky UC-52, 53 lam o Iter 3) */}
              <Route path="/royalty-periods" element={<RoyaltyPeriods />} />
              <Route path="/royalty-periods/:id" element={<RoyaltyPeriodDetail nguoiDung={nguoiDung} />} />
              <Route path="/royalty-rates" element={<RoyaltyRates />} />
              {/* BF-04 (Quang): UC-54 ghi nhan chi tra phieu chi */}
              <Route path="/royalty-payments" element={<RoyaltyPayments />} />
            </>
          )}
          {vaiTro === 'ADMIN' && (
            <>
              {/* BF-02 (Duong): UC-14…17 Bộ tiêu chí chấm điểm */}
              <Route path="/criteria-sets" element={<CriteriaManagement nguoiDung={nguoiDung} />} />
              {/* BF-03 (Vu), BF-05 (Khang), BF-04 (Quang): UC-56 he so anh/video */}
              <Route path="/appeal-rules" element={<AppealRuleConfigPage />} />
              <Route path="/kpi-benchmarks" element={<KpiBenchmarksConfig />} />
              <Route path="/media-factors" element={<MediaFactors />} />
              <Route path="/reader-imports" element={<ReaderDataImport />} />
              {/* admin (Minh): UC-13 tham so nhap bai, danh sach the loai, danh sach tai khoan (M-A7) */}
              <Route path="/system-parameters" element={<SystemParameters />} />
              <Route path="/genres" element={<GenreList />} />
              <Route path="/users" element={<UserList />} />
            </>
          )}

          {['HEAD_OF_DEPARTMENT', 'ADMIN'].includes(vaiTro) && (
            <>
              <Route path="/reviewers" element={<ReviewerDeviationPage />} />
              <Route path="/reviewers/:id" element={<ReviewerProfilePage />} />
              <Route path="/audit-logs" element={<AuditLogPage />} />
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
