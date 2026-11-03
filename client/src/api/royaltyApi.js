/**
 * API cua luong royalty (BF-04). Goi qua goiApi(): tu gan token, tu bao loi tieng Viet.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   29  getRoyaltyRates()               UC-55: tat ca phien ban don gia nhuan but (royalty_rates), moi nhat t… [GET /api/royalty-rates]
 *   37  createRoyaltyRate()             UC-55: them don gia moi [POST /api/royalty-rates]
 *   42  getMediaFactors()               UC-56: he so cua tung loai anh/video [GET /api/media-factors]
 *   47  updateMediaFactor()             UC-56: Admin sua he so 1 loai anh/video [PUT /api/media-factors/{id}]
 *   56  getRoyaltyPeriods()             UC-49: cac ky nhuan but, ky moi nhat truoc [GET /api/royalty-periods]
 *   66  getPendingGradings()            UC-46: bai da dang chua cham nhuan but, bai dang som nhat (gan han ch… [GET /api/royalty-gradings/pending]
 *   75  previewRoyaltyGrading()         UC-47, 48: tinh truoc he so va tien cua 1 bai, CHUA luu [GET /api/royalty-gradings/preview?articleId={encodeURIComponentarticleId}]
 *   85  completeRoyaltyGrading()        UC-47 buoc 5: Thu ky bam Hoan tat -> luu phieu, anh/video, dong tien … [POST /api/royalty-gradings]
 *   95  getRoyaltyPeriodDetail()        UC-50: bang ke 1 ky. { id, periodYear, periodMonth, status, returnNot… [GET /api/royalty-periods/{periodId}]
 *  100  closeRoyaltyPeriod()            UC-50: Thu ky chot ky OPEN -> CLOSED [POST /api/royalty-periods/{periodId}/close]
 *  105  returnRoyaltyPeriod()           UC-52: Ke toan tra ky CLOSED ve OPEN cho Thu ky sua, bat buoc ly do (… [POST /api/royalty-periods/{periodId}/return]
 *  110  lockRoyaltyPeriod()             UC-53: Ke toan khoa ky CLOSED -> LOCKED, server tao 1 phieu chi cho m… [POST /api/royalty-periods/{periodId}/lock]
 *  119  getRoyaltyPayments()            UC-54: phieu chi cua tung nguoi trong 1 ky (ky da khoa moi co phieu) [GET /api/royalty-payments?periodId={encodeURIComponentperiodId}]
 *  124  recordRoyaltyPayment()          UC-54: Ke toan ghi nhan da chi 1 phieu [POST /api/royalty-payments/{paymentId}/pay]
 *  135  getMyRoyalties()                UC-57: nhuan but cua nguoi dang dang nhap, moi ky 1 dong, ky moi nhat… [GET /api/royalty-payments/me]
 *  144  getUnpaidRoyaltyTotal()         Chip vi o khung Phong vien: tong nhuan but CHUA NHAN
 */
import { goiApi } from './goiApi.js'

/**
 * UC-55: tat ca phien ban don gia nhuan but (royalty_rates), moi nhat truoc.
 * Moi dong: { id, baseSalary, amount, effectiveFrom: "2026-07-01", createdBy }.
 * Don gia AP DUNG cho 1 bai = dong co effectiveFrom lon nhat ma <= ngay dang (BR-RY-03).
 */
export async function getRoyaltyRates() {
  return goiApi('/api/royalty-rates')
}

/**
 * UC-55: them don gia moi. Ke toan chi nhap luong co so, server tu tinh don gia.
 * @param {{ baseSalary: number, effectiveFrom: string }} rate effectiveFrom dang "2027-01-01"
 */
export async function createRoyaltyRate(rate) {
  return goiApi('/api/royalty-rates', { method: 'POST', body: rate })
}

/** UC-56: he so cua tung loai anh/video. Moi dong: { id, mediaKind: "PHOTO", factor: 1.00 }. */
export async function getMediaFactors() {
  return goiApi('/api/media-factors')
}

/** UC-56: Admin sua he so 1 loai anh/video. */
export async function updateMediaFactor(id, factor) {
  return goiApi(`/api/media-factors/${id}`, { method: 'PUT', body: { factor } })
}

/**
 * UC-49: cac ky nhuan but, ky moi nhat truoc.
 * Moi dong: { id, periodYear, periodMonth, status, lockedAt, returnNote, paidAt,
 *             totalAmount, gradedArticleCount }.
 */
export async function getRoyaltyPeriods() {
  return goiApi('/api/royalty-periods')
}

/**
 * UC-46: bai da dang chua cham nhuan but, bai dang som nhat (gan han chi) truoc.
 * Moi dong: { articleId, title, authorName, sectionId, sectionName, categoryId, categoryName,
 *             declaredNature, publishedAt, paymentDeadline: "2026-10-22", daysLeft, nearDeadline }.
 * daysLeft am = da qua han chi.
 */
export async function getPendingGradings() {
  return goiApi('/api/royalty-gradings/pending')
}

/**
 * UC-47, 48: tinh truoc he so va tien cua 1 bai, CHUA luu.
 * Tinh chat bai server lay theo PV khai (BR-IN-03), Thu ky khong chon lai.
 * @param {number|string} articleId
 */
export async function previewRoyaltyGrading(articleId) {
  return goiApi(`/api/royalty-gradings/preview?articleId=${encodeURIComponent(articleId)}`)
}

/**
 * UC-47 buoc 5: Thu ky bam Hoan tat -> luu phieu, anh/video, dong tien vao ky dang mo.
 * Bai da cham thi la cham lai: bat buoc recalcReason, chi duoc khi ky cua phieu cu con mo.
 * Tra ve phieu da luu (cung dang voi previewRoyaltyGrading, co them gradingId).
 * @param {{ articleId: number, recalcReason?: string }} grading
 */
export async function completeRoyaltyGrading(grading) {
  return goiApi('/api/royalty-gradings', { method: 'POST', body: grading })
}

/**
 * UC-50: bang ke 1 ky. { id, periodYear, periodMonth, status, returnNote, totalAmount,
 *   gradedArticleCount, articles: [{ gradingId, articleId, title, scoreUsed, nature,
 *   finalFactor, articleAmount, mediaAmount, totalAmount, gradedAt }],
 *   people: [{ userId, fullName, articleAmount, mediaAmount, adjustmentAmount, totalAmount }] }.
 */
export async function getRoyaltyPeriodDetail(periodId) {
  return goiApi(`/api/royalty-periods/${periodId}`)
}

/** UC-50: Thu ky chot ky OPEN -> CLOSED. Tra ve ky da chot (cung dang voi getRoyaltyPeriods). */
export async function closeRoyaltyPeriod(periodId) {
  return goiApi(`/api/royalty-periods/${periodId}/close`, { method: 'POST' })
}

/** UC-52: Ke toan tra ky CLOSED ve OPEN cho Thu ky sua, bat buoc ly do (toi da 1000 ky tu). */
export async function returnRoyaltyPeriod(periodId, note) {
  return goiApi(`/api/royalty-periods/${periodId}/return`, { method: 'POST', body: { note } })
}

/** UC-53: Ke toan khoa ky CLOSED -> LOCKED, server tao 1 phieu chi cho moi nguoi. */
export async function lockRoyaltyPeriod(periodId) {
  return goiApi(`/api/royalty-periods/${periodId}/lock`, { method: 'POST' })
}

/**
 * UC-54: phieu chi cua tung nguoi trong 1 ky (ky da khoa moi co phieu).
 * Moi dong: { id, periodId, userId, fullName, grossAmount, debtIn, paidAmount, debtOut,
 *             status: "UNPAID" | "PAID", paidBy, paidAt }.
 */
export async function getRoyaltyPayments(periodId) {
  return goiApi(`/api/royalty-payments?periodId=${encodeURIComponent(periodId)}`)
}

/** UC-54: Ke toan ghi nhan da chi 1 phieu. Het phieu chua chi thi ky tu sang PAID. */
export async function recordRoyaltyPayment(paymentId) {
  return goiApi(`/api/royalty-payments/${paymentId}/pay`, { method: 'POST' })
}

/**
 * UC-57: nhuan but cua nguoi dang dang nhap, moi ky 1 dong, ky moi nhat truoc.
 * Moi dong: { periodId, periodYear, periodMonth, periodStatus, totalAmount,
 *   payment: (null khi ky chua khoa) { paidAmount, debtIn, debtOut, status, paidAt },
 *   lines: [{ lineId, articleId, articleTitle, lineType: "ARTICLE" | "MEDIA" | "ADJUSTMENT",
 *             sharePercent, amount }] }.
 */
export async function getMyRoyalties() {
  return goiApi('/api/royalty-payments/me')
}

/**
 * Chip vi o khung Phong vien: tong nhuan but CHUA NHAN.
 * Ky da khoa thi lay so tien thuc chi cua phieu UNPAID; ky chua khoa thi lay tong dong tien tam tinh.
 * Khong co bang "so du vi" (schema.sql), nen tinh tu du lieu UC-57.
 */
export async function getUnpaidRoyaltyTotal() {
  const periods = await getMyRoyalties()
  let total = 0
  for (const period of periods) {
    if (period.payment) {
      if (period.payment.status === 'UNPAID') {
        total += Number(period.payment.paidAmount)
      }
    } else {
      total += Number(period.totalAmount)
    }
  }
  return total
}
