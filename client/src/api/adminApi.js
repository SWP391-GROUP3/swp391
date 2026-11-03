import { goiApi } from './goiApi.js'

/**
 * API cua luong admin (Minh). Chi file nay goi server cho cac man Admin: tham so nhap bai (UC-13),
 * the loai, tai khoan. Moi ham goi qua goiApi dung chung (tu gan token, loi -> Error tieng Viet).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   15  getIntakeParameters()           UC-13: 6 tham so nhap bai hien tai [GET /api/system-parameters/intake]
 *   23  updateIntakeParameters()        UC-13: Admin sua tham so nhap bai [PUT /api/system-parameters/intake]
 *   28  getGenres()                     The loai dang dung (ma, ten, he so chuan) [GET /api/genres]
 *   33  getUsers()                      Moi tai khoan (chi Admin) [GET /api/users]
 */

// UC-13: 6 tham so nhap bai hien tai
export function getIntakeParameters() {
  return goiApi('/api/system-parameters/intake')
}

/**
 * UC-13: Admin sua tham so nhap bai. duLieu gom du 6 truong: maxAuthorsPerArticle, coAuthorConfirmDays,
 * dupWarnPercent, dupBlockPercent, dhashMaxBits, shingleSize. Tra ve 6 tham so sau khi luu.
 */
export function updateIntakeParameters(duLieu) {
  return goiApi('/api/system-parameters/intake', { method: 'PUT', body: duLieu })
}

// The loai dang dung (ma, ten, he so chuan)
export function getGenres() {
  return goiApi('/api/genres')
}

// Moi tai khoan (chi Admin)
export function getUsers() {
  return goiApi('/api/users')
}
