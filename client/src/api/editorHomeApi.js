/**
 * API cho trang chu Bien tap vien (M-A6: da bo du lieu gia, ghep tu API that cua BF-02 va BF-03).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  layTrangChuBienTapVien()        Tat ca so lieu trang chu cua BTV dang dang nhap: - statusCounts: so bai DUOC GIAO CHO TOI theo articles.statu…
 */
import { layLichSuChamCuaToi } from './assessmentApi.js'
import { layPhieuChamMu } from './appealApi.js'
import { layPhieuChamCheo } from './crossReviewApi.js'

/**
 * Tat ca so lieu trang chu cua BTV dang dang nhap:
 * - statusCounts: so bai DUOC GIAO CHO TOI theo articles.status
 * - rescoreRequests: Truong ban tra cham lai, phieu moi chua nop - UC-32
 * - reReviewTasks: phieu cham mu do khieu nai - UC-36, 37
 * - assignedArticles: bai da giao, chua mo hoac chua nop phieu lan dau - UC-21
 * - crossReviewTasks: phieu cham cheo dang cho - UC-40
 * - thisMonth: so bai da nop phieu trong thang nay va diem trung binh da cho
 */
export async function layTrangChuBienTapVien() {
  const [lichSu, phieuChamMu, phieuChamCheo] = await Promise.all([
    layLichSuChamCuaToi(),
    layPhieuChamMu(),
    layPhieuChamCheo(),
  ])

  const demTheoTrangThai = {}
  const rescoreRequests = []
  const assignedArticles = []
  const homNay = new Date()
  let soBaiThangNay = 0
  let tongDiemThangNay = 0

  for (const bai of lichSu) {
    demTheoTrangThai[bai.articleStatus] = (demTheoTrangThai[bai.articleStatus] || 0) + 1

    // Bai dang o BTV (ASSIGNED) va phieu moi nhat chua nop
    if (bai.articleStatus === 'ASSIGNED' && !bai.submittedAt) {
      if (bai.returnReason) {
        rescoreRequests.push({ articleId: bai.articleId, title: bai.title, reason: bai.returnReason, returnedAt: bai.returnedAt })
      } else {
        assignedArticles.push({ articleId: bai.articleId, title: bai.title, categoryName: bai.categoryName, assignedAt: bai.assignedAt })
      }
    }

    // Thong ke thang: phieu da nop trong thang hien tai
    if (bai.submittedAt) {
      const ngayNop = new Date(bai.submittedAt)
      if (ngayNop.getFullYear() === homNay.getFullYear() && ngayNop.getMonth() === homNay.getMonth()) {
        soBaiThangNay++
        tongDiemThangNay += Number(bai.totalScore) || 0
      }
    }
  }

  const statusCounts = []
  for (const status of Object.keys(demTheoTrangThai)) {
    statusCounts.push({ status, count: demTheoTrangThai[status] })
  }

  return {
    statusCounts,
    rescoreRequests,
    reReviewTasks: phieuChamMu.map((phieu) => ({ taskId: phieu.id, title: phieu.title, assignedAt: phieu.submittedAt })),
    assignedArticles,
    crossReviewTasks: phieuChamCheo.map((phieu) => ({ taskId: phieu.id, title: phieu.title })),
    thisMonth: {
      scoredCount: soBaiThangNay,
      avgScore: soBaiThangNay > 0 ? tongDiemThangNay / soBaiThangNay : null,
    },
  }
}
