/**
 * Dinh dang so thuong (diem, so bai) kieu Viet Nam: 72.5 -> "72,5".
 * Toi da 2 chu so thap phan vi diem/he so luu DECIMAL(5,2).
 *
 * MUC LUC (so dong)
 *    9  DINH_DANG_SO                 - Dinh dang so thuong (diem, so bai) kieu Viet Nam: 72.5 -> "72,5".
 *   15  formatNumber
 */
const DINH_DANG_SO = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 })

/**
 * @param {number|string|null|undefined} value
 * @returns {string} vd "72,5", hoac "—" neu khong phai so hop le
 */
export function formatNumber(value) {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  const so = Number(value)
  if (Number.isNaN(so)) {
    return '—'
  }
  return DINH_DANG_SO.format(so)
}
