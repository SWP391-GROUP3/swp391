/**
 * API cua luong royalty (BF-04). Goi qua goiApi(): tu gan token, tu bao loi tieng Viet.
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
