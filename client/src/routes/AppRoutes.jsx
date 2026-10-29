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
import AppealRuleConfigPage from '../pages/admin/AppealRuleConfigPage.jsx'
import RoyaltyPeriods from '../pages/royalty/RoyaltyPeriods.jsx'
import RoyaltyPeriodDetail from '../pages/royalty/RoyaltyPeriodDetail.jsx'
import RoyaltyPendingList from '../pages/royalty/RoyaltyPendingList.jsx'
import RoyaltyGrading from '../pages/royalty/RoyaltyGrading.jsx'
import RoyaltyRates from '../pages/royalty/RoyaltyRates.jsx'
import MediaFactors from '../pages/royalty/MediaFactors.jsx'
import ReaderDataImport from '../pages/admin/ReaderDataImport.jsx'
import CriteriaManagement from '../pages/flow2/CriteriaManagement.jsx'
import AssignmentQueue from '../pages/flow2/AssignmentQueue.jsx'
import EditorTaskQueue from '../pages/flow2/EditorTaskQueue.jsx'
import ScoringSheet from '../pages/flow2/ScoringSheet.jsx'
import ScoreApprovalQueue from '../pages/flow2/ScoreApprovalQueue.jsx'
import PublishingQueue from '../pages/flow2/PublishingQueue.jsx'
import Flow2Dashboard from '../pages/flow2/Flow2Dashboard.jsx'
import ScoreCalculatorTester from '../pages/flow2/ScoreCalculatorTester.jsx'
import UserProfile from '../pages/profile/UserProfile.jsx'

/*
 * Trang CHUA LAM cua tung vai tro: menu va nut tren trang chu tro toi day,
 * tam hien ComingSoon. Lam xong trang nao thi bo dong do khoi mang
 * va khai bao <Route> rieng tro toi trang that (xem phan "trang da lam" ben duoi).
 */
const TRANG_PV_CHUA_LAM = [
  { path: '/my-royalties',      ten: 'Nhuận bút' },              // UC-57
  { path: '/my-royalties', ten: 'Nhuận bút' },              // UC-57
]

const TRANG_BTV_CHUA_LAM = [
  { path: '/assigned-articles/:id', ten: 'Chấm bài' },               // UC-22…28
  { path: '/my-scorings',           ten: 'Lịch sử chấm' },           // UC-21
  { path: '/re-reviews', ten: 'Chấm lại & chấm chéo' },   // UC-36, UC-37
  { path: '/re-reviews/:id', ten: 'Phiếu chấm lại' },         // UC-36, UC-37
  { path: '/my-scorings', ten: 'Lịch sử chấm' },           // UC-21
]

// Thu ky: Iter 2 da lam het (UC-46, 47, 48, 50) -> mang rong
const TRANG_TK_CHUA_LAM = []

const TRANG_TB_CHUA_LAM = [
  { path: '/reviewers/:id',         ten: 'Hồ sơ biên tập viên' },     // UC-42
  { path: '/audit-logs',            ten: 'Nhật ký chấm' },            // UC-45
  { path: '/kpi-board',             ten: 'Bảng KPI của ban' },        // UC-61, 66
  { path: '/cross-reviews',         ten: 'Đợt chấm chéo' },           // UC-40
  { path: '/kpi-board/:reporterId', ten: 'KPI phóng viên' },          // UC-62
  { path: '/kpi-reports',           ten: 'Báo cáo' },                 // UC-64, 65, 68
]

const TRANG_KT_CHUA_LAM = [
  { path: '/royalty-payments', ten: 'Chi trả nhuận bút' },       // UC-54
]

const TRANG_ADMIN_CHUA_LAM = [
  { path: '/users', ten: 'Tài khoản' },
  { path: '/genres', ten: 'Thể loại' },
  { path: '/system-parameters', ten: 'Tham số hệ thống' },        // UC-13
  { path: '/criteria-sets', ten: 'Bộ tiêu chí' },             // UC-14, 15
  { path: '/criteria-sets/:id', ten: 'Chi tiết bộ tiêu chí' },    // UC-16, 17
]

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
 *   61  TRANG_PV_CHUA_LAM               Trang CHUA LAM cua tung vai tro: menu va nut tren trang chu tro toi day, tam hien ComingSoon
 *   66  TRANG_BTV_CHUA_LAM              hang so
 *   75  TRANG_TK_CHUA_LAM               Thu ky: Iter 2 da lam het (UC-46, 47, 48, 50) -> mang rong
 *   77  TRANG_TB_CHUA_LAM               hang so
 *   86  TRANG_KT_CHUA_LAM               hang so
 *   90  TRANG_ADMIN_CHUA_LAM            hang so
 *  102  KHUNG_THEO_VAI_TRO              Moi vai tro co khung trang (menu) rieng, khai bao o day: Layout = khung + menu, TrangChu = trang hien o /main…
 *  189  AppRoutes()                     xem tom tat dau file
 *
 * DUONG DAN -> TRANG (so dong - path - component)
 *  199  /                               chuyen tiep -> /main
 *  202  /login                          chuyen tiep -> /main hoac Login (tuy da dang nhap chua)
 *  225  /main                           TrangChu
 *  226  /calculator                     ScoreCalculatorTester
 *  227  /flow2                          Flow2Dashboard
 *  230  /profile                        UserProfile
 *  233  /my-appeals                     MyAppealsPage
 *  234  /my-appeals/new                 AppealCreatePage
 *  236  /my-articles                    MyArticles
 *  237  /my-articles/:id                ArticleDetail
 *  239  /my-articles/new                ArticleEditor
 *  240  /my-articles/:id/edit           ArticleEditor
 *  242  /co-author-invites              CoAuthorInvites
 *  244  /my-kpi                         PersonalKpiProgress
 *  250  /assigned-articles              EditorTaskQueue
 *  251  /re-reviews                     BlindReviewQueuePage
 *  252  /re-reviews/:id                 BlindReviewPage
 *  253  /cross-review-tasks/:id         BlindReviewPage
 *  254  /assigned-articles/:articleId   ScoringSheet
 *  255  /flow2-dashboard                Flow2Dashboard
 *  261  /assignments                    AssignmentQueue
 *  263  /score-approvals                ScoreApprovalQueue
 *  264  /score-approvals/:id            ScoreApprovalQueue
 *  265  /publishing                     PublishingQueue
 *  267  /appeals                        AppealManagementPage
 *  268  /appeals/:id                    AppealDetailPage
 *  269  /appeals/:id/comparison         AppealComparisonPage
 *  270  /cross-reviews                  CrossReviewBatchPage
 *  271  /cross-reviews/:id              CrossReviewBatchDetailPage
 *  273  /flow2-dashboard                Flow2Dashboard
 *  275  /kpi-board                      DepartmentKpiDashboard
 *  276  /kpi-board/:reporterId          ReporterKpiDetails
 *  277  /kpi-targets                    MonthlyTargetSetting
 *  278  /kpi-benchmarks                 KpiBenchmarksConfig
 *  284  /royalty-grading                RoyaltyPendingList
 *  285  /royalty-grading/:articleId     RoyaltyGrading
 *  286  /royalty-periods                RoyaltyPeriods
 *  287  /royalty-periods/:id            RoyaltyPeriodDetail
 *  294  /royalty-periods                RoyaltyPeriods
 *  295  /royalty-periods/:id            RoyaltyPeriodDetail
 *  296  /royalty-rates                  RoyaltyRates
 *  302  /criteria-sets                  CriteriaManagement
 *  303  /criteria-sets/:id              CriteriaManagement
 *  304  /flow2-dashboard                Flow2Dashboard
 *  306  /appeal-rules                   AppealRuleConfigPage
 *  307  /kpi-benchmarks                 KpiBenchmarksConfig
 *  308  /media-factors                  MediaFactors
 *  309  /reader-imports                 ReaderDataImport
 *  315  /reviewers                      ReviewerDeviationPage
 *  316  /reviewers/:id                  ReviewerProfilePage
 *  317  /reviewer-deviations            ReviewerDeviationPage
 *  318  /audit-logs                     AuditLogPage
 *  336  /main                           RequireRole
 *  346  *                               chuyen tiep -> /
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  199  tu chuyen trang (Navigate)      chuyen sang /main (trang chu theo vai tro)
 *  206  tu chuyen trang (Navigate)      chuyen sang /main (trang chu theo vai tro)
 *  346  tu chuyen trang (Navigate)      chuyen sang / (Navigate)
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
          <Route path="/calculator" element={<ScoreCalculatorTester />} />
          <Route path="/flow2" element={<Flow2Dashboard nguoiDung={nguoiDung} />} />

          {/* Ho so ca nhan: moi vai tro deu co, mo tu menu avatar o thanh tren */}
          <Route path="/profile" element={<UserProfile onCapNhatNguoiDung={onCapNhatNguoiDung} />} />
          {vaiTro === 'REPORTER' && (
            <>
              <Route path="/my-appeals" element={<MyAppealsPage />} />
              <Route path="/my-appeals/new" element={<AppealCreatePage />} />
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
            </>
          )}
          {vaiTro === 'EDITOR' && (
            <>
              {/* BF-02 (Duong): UC-21 Bài được giao & UC-22..28 Chấm điểm */}
              <Route path="/assigned-articles" element={<EditorTaskQueue nguoiDung={nguoiDung} />} />
              <Route path="/re-reviews" element={<BlindReviewQueuePage />} />
              <Route path="/re-reviews/:id" element={<BlindReviewPage />} />
              <Route path="/cross-review-tasks/:id" element={<BlindReviewPage cheo />} />
              <Route path="/assigned-articles/:articleId" element={<ScoringSheet nguoiDung={nguoiDung} />} />
              <Route path="/flow2-dashboard" element={<Flow2Dashboard nguoiDung={nguoiDung} />} />
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
              <Route path="/appeals/:id" element={<AppealDetailPage />} />
              <Route path="/appeals/:id/comparison" element={<AppealComparisonPage />} />
              <Route path="/cross-reviews" element={<CrossReviewBatchPage />} />
              <Route path="/cross-reviews/:id" element={<CrossReviewBatchDetailPage />} />

              <Route path="/flow2-dashboard" element={<Flow2Dashboard nguoiDung={nguoiDung} />} />
              {/* BF-05 (Khang) */}
              <Route path="/kpi-board" element={<DepartmentKpiDashboard nguoiDung={nguoiDung} />} />
              <Route path="/kpi-board/:reporterId" element={<ReporterKpiDetails nguoiDung={nguoiDung} />} />
              <Route path="/kpi-targets" element={<MonthlyTargetSetting nguoiDung={nguoiDung} />} />
              <Route path="/kpi-benchmarks" element={<KpiBenchmarksConfig />} />
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
            </>
          )}
          {vaiTro === 'ADMIN' && (
            <>
              {/* BF-02 (Duong): UC-14…17 Bộ tiêu chí chấm điểm */}
              <Route path="/criteria-sets" element={<CriteriaManagement nguoiDung={nguoiDung} />} />
              <Route path="/criteria-sets/:id" element={<CriteriaManagement nguoiDung={nguoiDung} />} />
              <Route path="/flow2-dashboard" element={<Flow2Dashboard nguoiDung={nguoiDung} />} />
              {/* BF-03 (Vu), BF-05 (Khang), BF-04 (Quang): UC-56 he so anh/video */}
              <Route path="/appeal-rules" element={<AppealRuleConfigPage />} />
              <Route path="/kpi-benchmarks" element={<KpiBenchmarksConfig />} />
              <Route path="/media-factors" element={<MediaFactors />} />
              <Route path="/reader-imports" element={<ReaderDataImport />} />
            </>
          )}

          {['HEAD_OF_DEPARTMENT', 'ADMIN'].includes(vaiTro) && (
            <>
              <Route path="/reviewers" element={<ReviewerDeviationPage />} />
              <Route path="/reviewers/:id" element={<ReviewerProfilePage />} />
              <Route path="/reviewer-deviations" element={<ReviewerDeviationPage />} />
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
