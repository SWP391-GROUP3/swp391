import { goiApi } from './goiApi.js'

/**
 * API cua luong admin (Minh). Chi file nay goi server cho cac man Admin: tham so nhap bai (UC-13),
 * the loai, tai khoan. Moi ham goi qua goiApi dung chung (tu gan token, loi -> Error tieng Viet).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  getIntakeParameters()          UC-13: 6 tham so nhap bai hien tai [GET /api/system-parameters/intake]
 *   27  updateIntakeParameters()       UC-13: Admin sua tham so nhap bai [PUT /api/system-parameters/intake]
 *   32  getGenres()                    The loai dang dung (ma, ten, he so chuan) [GET /api/genres]
 *   37  getUsers()                      Moi tai khoan (chi Admin) [GET /api/users]
 *   45  createUser()                    Admin tao tai khoan [POST /api/users]
 *   50  lockUser()                      Admin khoa tai khoan [PUT /api/users/{id}/lock]
 *   55  unlockUser()                    Admin mo khoa tai khoan [PUT /api/users/{id}/unlock]
 *   60  getSections()                   Cac ban dang hoat dong [GET /api/sections]
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

/**
 * Admin tao tai khoan. duLieu: { username, email, password, fullName, roleCode, sectionId }.
 * sectionId = null voi vai tro khong thuoc ban. Tra ve tai khoan vua tao.
 */
export function createUser(duLieu) {
  return goiApi('/api/users', { method: 'POST', body: duLieu })
}

// Admin khoa tai khoan (khong tu khoa minh duoc)
export function lockUser(id) {
  return goiApi(`/api/users/${id}/lock`, { method: 'PUT' })
}

// Admin mo khoa tai khoan
export function unlockUser(id) {
  return goiApi(`/api/users/${id}/unlock`, { method: 'PUT' })
}

// Cac ban dang hoat dong (o chon ban khi tao tai khoan)
export function getSections() {
  return goiApi('/api/sections')
}
