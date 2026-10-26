/**
 * ===========================================================================
 * DU LIEU GIA cho trang chu Truong ban - CHUA CO API THAT.
 * ===========================================================================
 * Ham tra ve Promise giong goiApi va tra dung phan "data" cua ApiResponse.
 * Khi backend xong (BF-02, BF-03, BF-05) chi can doi ruot ham thanh
 * goiApi('/api/...'), component goi ham KHONG phai sua.
 * Moi so lieu chi tinh trong BAN cua Truong ban (users.section_id).
 *
 * MUC LUC (so dong)
 *   25  layTrangChuTruongBan         - Tat ca so lieu trang chu cua Truong ban:
 */
import { giaLapGoiApi, cachHomNay } from './duLieuGia.js'

/**
 * Tat ca so lieu trang chu cua Truong ban:
 * - counts: so bai cua ban theo articles.status + so khieu nai chua xong
 *   (appeals.status SUBMITTED / IN_REVIEW)
 * - appealsToConfirm: khieu nai da co diem trong tai S2, cho Truong ban xac nhan - UC-37
 * - pendingApprovals: lan cham cho duyet (PENDING_APPROVAL), cu nhat truoc - UC-29
 * - unassignedArticles: bai SUBMITTED chua phan cong, cu nhat truoc - UC-18
 * - kpiThisMonth: tom tat KPI thang cua ban (kpi_targets + kpi_snapshots) - UC-61, 66
 *   noTarget = so PV chua duoc giao chi tieu thang nay (UC-59)
 */
export async function layTrangChuTruongBan() {
  const homNay = new Date()
  return giaLapGoiApi({
    counts: {
      submitted: 4,
      assigned: 5,
      pendingApproval: 3,
      scorePublished: 2,
      openAppeals: 2,
    },
    appealsToConfirm: [
      { appealId: 4, articleId: 9, title: 'Phỏng vấn chuyên gia về chuyển đổi số y tế', s0: 72.5, s1: 84, s2: 80 },
    ],
    pendingApprovals: [
      { scoringAttemptId: 40, articleId: 22, title: 'Mùa lúa chín trên ruộng bậc thang', editorName: 'Lê Hoàng Cường', totalScore: 81.5, submittedAt: cachHomNay(-2) },
      { scoringAttemptId: 42, articleId: 24, title: 'Học sinh vùng cao đến trường bằng xe đạp', editorName: 'Lê Hoàng Cường', totalScore: 68, submittedAt: cachHomNay(-1) },
    ],
    unassignedArticles: [
      { articleId: 28, title: 'Cảnh báo lừa đảo qua tin nhắn ngân hàng', authorName: 'Nguyễn Văn An', submittedAt: cachHomNay(-2) },
      { articleId: 30, title: 'Lễ hội đèn lồng phố cổ', authorName: 'Trần Thị Bình', submittedAt: cachHomNay(-1) },
    ],
    kpiThisMonth: {
      year: homNay.getFullYear(),
      month: homNay.getMonth() + 1,
      reporterCount: 8,
      expectedPass: 5,
      expectedFail: 2,
      noTarget: 1,
      finalized: false,
    },
  })
}
