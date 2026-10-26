import { goiApi } from './goiApi.js'

/**
 * Dang nhap (UC-69): POST /api/auth/login.
 * login = email hoac ten dang nhap.
 *
 * Doi du lieu server (ten field tieng Anh cua DTO LoginResponse) sang dang ma
 * cac man hinh dang dung: nguoiDung.hoTen, nguoiDung.vaiTro (mang ma vai tro)...
 * @returns {{ token: string, nguoiDung: object }}
 *
 * MUC LUC (so dong)
 *   14  dangNhap                     - Dang nhap (UC-69): POST /api/auth/login.
 */
export async function dangNhap(login, matKhau) {
  const data = await goiApi('/api/auth/login', {
    method: 'POST',
    body: { login, password: matKhau },
  })
  return {
    token: data.token,
    nguoiDung: {
      id: data.id,
      username: data.username,
      email: data.email,
      hoTen: data.fullName,
      vaiTro: [data.roleCode],      // mang: RequireRole, AppRoutes dang dung .includes()
      tenVaiTro: data.roleName,     // "Phóng viên", "Kế toán"... de hien len man hinh
      sectionId: data.sectionId,
    },
  }
}
