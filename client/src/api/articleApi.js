import { goiApi } from './goiApi.js'

/**
 * API cua luong Bai viet (BF-01). Chi file nay goi server cho BF-01 (AGENTS.md muc 4).
 * Moi ham goi qua goiApi dung chung: tu gan token dang nhap (server biet ai dang
 * thao tac tu token), tra ve phan "data" cua ApiResponse, loi thi nem Error kem
 * message tieng Viet server tra ve de man hinh hien thang.
 */

/**
 * UC-01: bai cua toi, sua gan nhat len dau.
 * cacTrangThai: mang ma trang thai, rong = tat ca. trang bat dau tu 0.
 */
export function layBaiCuaToi(cacTrangThai, trang, soDong) {
  const thamSo = new URLSearchParams({ page: trang, size: soDong })
  if (cacTrangThai.length > 0) {
    thamSo.set('status', cacTrangThai.join(','))
  }
  return goiApi(`/api/articles?${thamSo}`)
}

/** UC-02: chi tiet 1 bai (noi dung, tac gia, lich su trang thai). */
export function layChiTietBai(idBai) {
  return goiApi(`/api/articles/${idBai}`)
}

/** UC-11: rut bai da nop ve Nhap. Tra ve chi tiet bai sau khi rut. */
export function rutBai(idBai) {
  return goiApi(`/api/articles/${idBai}/withdraw`, { method: 'POST' })
}

/**
 * 3 ham cho trang chu Phong vien (gop y nhom truong muc C4).
 * Dang du lieu giong ham cung ten trong reporterHomeApi.js, rieng loi moi
 * dong tac gia tam tra mainAuthorId (chua co ten vi chua co UserService).
 */
export function layThongKeTrangThaiBai() {
  return goiApi('/api/articles/status-counts')
}

export function layBaiBiTraVe() {
  return goiApi('/api/articles/revision-requests')
}

export function layLoiMoiDongTacGia() {
  return goiApi('/api/articles/co-author-invites')
}
