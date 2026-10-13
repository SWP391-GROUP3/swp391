/**
 * API cua luong royalty (BF-04). Goi qua goiApi(): tu gan token, tu bao loi tieng Viet.
 */
import { goiApi } from './goiApi.js'

/**
 * Tat ca phien ban don gia nhuan but (royalty_rates), moi nhat truoc.
 * Moi dong: { id, baseSalary, amount, effectiveFrom: "2026-07-01", createdBy }.
 * Don gia AP DUNG cho 1 bai = dong co effectiveFrom lon nhat ma <= ngay dang (BR-RY-03).
 */
export async function layDanhSachDonGia() {
  return goiApi('/api/royalty-rates')
}
