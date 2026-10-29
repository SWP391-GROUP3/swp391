import { goiApi } from './goiApi.js'

/**
 * Ho so cua nguoi dang dang nhap. "/me" = chinh minh: server lay id tu token,
 * nen khong can (va khong duoc) gui id len.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   14  layHoSo()                       lay ho so [GET /api/users/me]
 *   19  capNhatHoSo()                   cap nhat ho so [PUT /api/users/me]
 *   27  doiMatKhau()                    doi mat khau [PUT /api/users/me/password]
 */

/** GET /api/users/me -> { id, username, email, fullName, roleName, sectionId, status, createdAt } */
export function layHoSo() {
  return goiApi('/api/users/me')
}

/** PUT /api/users/me - chi sua ho ten + email. Tra ve ho so moi. */
export function capNhatHoSo(fullName, email) {
  return goiApi('/api/users/me', {
    method: 'PUT',
    body: { fullName, email },
  })
}

/** PUT /api/users/me/password - phai nhap dung mat khau hien tai. */
export function doiMatKhau(currentPassword, newPassword, confirmPassword) {
  return goiApi('/api/users/me/password', {
    method: 'PUT',
    body: { currentPassword, newPassword, confirmPassword },
  })
}
