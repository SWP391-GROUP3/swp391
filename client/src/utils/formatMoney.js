/**
 * Dinh dang tien VND de hien thi: 2350000 -> "2.350.000 ₫".
 * Moi man hinh deu goi ham nay, khong tu format kieu khac (AGENTS.md muc 4).
 *
 * MUC LUC (so dong)
 *   13  DINH_DANG_VND                - Tao 1 lan dung lai mai, khong tao moi moi lan goi ham.
 *   19  KHONG_CO_SO                  - Chu hien khi chua co so tien (chua tai xong, hoac du lieu trong).
 *   26  formatMoney                  - trong JSON co the la so hoac chuoi, nen nhan ca 2.
 */

// Tao 1 lan dung lai mai, khong tao moi moi lan goi ham.
// VND khong co phan le nen Intl tu bo so thap phan.
const DINH_DANG_VND = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
})

/** Chu hien khi chua co so tien (chua tai xong, hoac du lieu trong). */
const KHONG_CO_SO = '—'

/**
 * @param {number|string|null|undefined} amount so tien. Server tra BigDecimal
 *        trong JSON co the la so hoac chuoi, nen nhan ca 2.
 * @returns {string} vd "2.350.000 ₫", hoac "—" neu khong phai so hop le
 */
export function formatMoney(amount) {
  if (amount === null || amount === undefined || amount === '') {
    return KHONG_CO_SO
  }

  const soTien = Number(amount)
  if (Number.isNaN(soTien)) {
    return KHONG_CO_SO
  }

  return DINH_DANG_VND.format(soTien)
}
