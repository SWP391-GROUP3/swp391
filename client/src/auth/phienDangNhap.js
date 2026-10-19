/**
 * Luu / doc / xoa phien dang nhap trong sessionStorage.
 * Phien = { token, nguoiDung } nhan duoc tu POST /api/auth/login.
 *
 * Vi sao can luu: React giu state trong bo nho, F5 mot cai la mat sach. Khong luu
 * thi cu tai lai trang la bi da ve man hinh dang nhap.
 *
 * Vi sao dung sessionStorage chu khong phai localStorage: sessionStorage tu xoa
 * khi dong tab. localStorage thi nam lai mai tren may - dung o phong may truong
 * thi nguoi sau mo ra van con token cua nguoi truoc.
 *
 * MUC LUC (so dong)
 *   21  KHOA_PHIEN
 *   24  KHOA_CU                      - Khoa cu (thoi dung tai khoan gia, chua co token) - xoa di cho sach.
 *   27  docPhien
 *   42  luuPhien
 *   47  xoaPhien                     - Dang xuat (UC-70): server khong luu phien nen chi can xoa token o day.
 *   52  layToken                     - Token de gan vao header Authorization; null neu chua dang nhap.
 */

const KHOA_PHIEN = 'phienDangNhap'

/** Khoa cu (thoi dung tai khoan gia, chua co token) - xoa di cho sach. */
const KHOA_CU = 'nguoiDung'

/** @returns {{ token: string, nguoiDung: object } | null} */
export function docPhien() {
  sessionStorage.removeItem(KHOA_CU)
  try {
    const raw = sessionStorage.getItem(KHOA_PHIEN)
    const phien = raw ? JSON.parse(raw) : null
    // Phien hong / thieu token -> coi nhu chua dang nhap
    if (!phien || !phien.token || !phien.nguoiDung) {
      return null
    }
    return phien
  } catch {
    return null
  }
}

export function luuPhien(phien) {
  sessionStorage.setItem(KHOA_PHIEN, JSON.stringify(phien))
}

/** Dang xuat (UC-70): server khong luu phien nen chi can xoa token o day. */
export function xoaPhien() {
  sessionStorage.removeItem(KHOA_PHIEN)
}

/** Token de gan vao header Authorization; null neu chua dang nhap. */
export function layToken() {
  const phien = docPhien()
  return phien ? phien.token : null
}
