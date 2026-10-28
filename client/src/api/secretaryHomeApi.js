/**
 * So lieu trang chu Thu ky toa soan (BF-04), ghep tu 2 API THAT co san cua luong royalty
 * (khong can API rieng cho trang chu):
 *   - getPendingGradings(): bai da dang chua cham nhuan but (UC-46)
 *   - getRoyaltyPeriods():  cac ky nhuan but (UC-49), lay ky dang OPEN
 * Ten field tra ve giu nhu ban du lieu gia truoc day, nen SecretaryHome.jsx dung tiep duoc.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  SO_BAI_TREN_TRANG_CHU           So bai cho cham hien o danh sach "Cho xu ly" (han chi gan nhat truoc)
 *   27  layTrangChuThuKy()              Tat ca so lieu trang chu cua Thu ky: - counts.pendingGrading: so bai cho cham (UC-46) - counts.nearDeadline: …
 *   61  timKyDangMo()                   Ky dang OPEN, null neu khong co
 */
import { getPendingGradings, getRoyaltyPeriods } from './royaltyApi.js'

// So bai cho cham hien o danh sach "Chờ xử lý" (han chi gan nhat truoc).
// Xem het o man "Chấm nhuận bút".
const SO_BAI_TREN_TRANG_CHU = 5

/**
 * Tat ca so lieu trang chu cua Thu ky:
 * - counts.pendingGrading: so bai cho cham (UC-46)
 * - counts.nearDeadline:   trong so do, bai sap toi han hoac da qua han chi
 * - pendingArticles: vai bai cho cham, server da xep bai dang som nhat (gan han nhat) len dau
 * - openPeriod: ky dang OPEN; null neu chua co ky nao mo.
 *   returnNote khac null = Ke toan da tra ky ve (UC-52), Thu ky sua roi chot lai (UC-50).
 */
export async function layTrangChuThuKy() {
  const baiChoCham = await getPendingGradings()
  const cacKy = await getRoyaltyPeriods()

  let soBaiSapToiHan = 0
  for (const bai of baiChoCham) {
    if (bai.nearDeadline) {
      soBaiSapToiHan++
    }
  }

  const ky = timKyDangMo(cacKy)
  return {
    counts: {
      pendingGrading: baiChoCham.length,
      nearDeadline: soBaiSapToiHan,
    },
    pendingArticles: baiChoCham.slice(0, SO_BAI_TREN_TRANG_CHU),
    openPeriod: ky ? {
      periodId: ky.id,
      periodYear: ky.periodYear,
      periodMonth: ky.periodMonth,
      status: ky.status,
      returnNote: ky.returnNote,
      gradedCount: ky.gradedArticleCount,
      totalAmount: ky.totalAmount,
    } : null,
  }
}

/**
 * Ky dang OPEN, null neu khong co. API tra ky MOI NHAT truoc, nen duyet het danh sach
 * va giu ky OPEN gap sau cung = ky cu nhat. Giong server chon ky cho bai cham xong (BR-RY-05).
 */
function timKyDangMo(cacKy) {
  let kyDangMo = null
  for (const ky of cacKy) {
    if (ky.status === 'OPEN') {
      kyDangMo = ky
    }
  }
  return kyDangMo
}
