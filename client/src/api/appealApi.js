import { goiApi } from './goiApi.js'

/**
 * API cua luong appeal (BF-03) cho giao dien: dieu kien va gui khieu nai (Phong vien), hang cho / chi
 * tiet / phan cong cham mu (Truong ban), phieu cham mu (BTV), so sanh va chot diem. Goi qua goiApi().
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  layDieuKienKhieuNai()           lay dieu kien khieu nai [GET /api/articles/{articleId}/appeal-eligibility]
 *   30  guiKhieuNai()                   gui khieu nai [POST /api/appeals]
 *   34  layKhieuNaiCuaToi()             lay khieu nai cua toi [GET /api/appeals/mine]
 *   38  layBaiCoTheKhieuNai()           lay bai co the khieu nai [GET /api/appeals/eligible-articles]
 *   42  layHangChoKhieuNai()            lay hang cho khieu nai [GET /api/appeals?{thamSo}]
 *   52  layChiTietKhieuNai()            lay chi tiet khieu nai [GET /api/appeals/{id}]
 *   56  layNguoiChamHopLe()             lay nguoi cham hop le [GET /api/appeals/{id}/reviewer-candidates]
 *   60  phanCongKhieuNai()              phan cong khieu nai [POST /api/appeals/{id}/assign]
 *   64  giaoLaiKhieuNai()               giao lai khieu nai [POST /api/appeals/{id}/reassign]
 *   68  layPhieuChamMu()                lay phieu cham mu [GET /api/appeal-reviews]
 *   69  layChiTietChamMu()              lay chi tiet cham mu [GET /api/appeal-reviews/{id}]
 *   70  luuNhapChamMu()                 luu nhap cham mu [PUT /api/appeal-reviews/{id}]
 *   71  nopPhieuChamMu()                nop phieu cham mu [POST /api/appeal-reviews/{id}/submit]
 *   72  laySoSanhKhieuNai()             lay so sanh khieu nai [GET /api/appeals/{id}/comparison]
 *   73  phanCongLanHai()                phan cong lan hai [POST /api/appeals/{id}/assign-second]
 *   74  chotDiemKhieuNai()              chot diem khieu nai [POST /api/appeals/{id}/finalize]
 */

export async function layDieuKienKhieuNai(articleId) {
  return goiApi(`/api/articles/${articleId}/appeal-eligibility`)
}

export async function guiKhieuNai(articleId, criteria) { // [LUONG 1 - GUI KHIEU NAI] buoc 3/6 POST /api/appeals qua goiApi
  return goiApi('/api/appeals', { method: 'POST', body: { articleId, criteria } })
}

export async function layKhieuNaiCuaToi() {
  return goiApi('/api/appeals/mine')
}

export async function layBaiCoTheKhieuNai() {
  return goiApi('/api/appeals/eligible-articles')
}

export function layHangChoKhieuNai(boLoc = {}) {
  const thamSo = new URLSearchParams()
  for (const ten of ['status', 'articleId', 'reviewerId', 'fromDate', 'toDate']) {
    if (boLoc[ten]) thamSo.set(ten, boLoc[ten])
  }
  thamSo.set('page', boLoc.page ?? 0)
  thamSo.set('size', 10)
  return goiApi(`/api/appeals?${thamSo}`)
}

export function layChiTietKhieuNai(id) {
  return goiApi(`/api/appeals/${id}`)
}

export function layNguoiChamHopLe(id) {
  return goiApi(`/api/appeals/${id}/reviewer-candidates`)
}

export function phanCongKhieuNai(id, reviewerId, reason) { // [LUONG 2 - PHAN CONG] buoc 3/6 POST /api/appeals/{id}/assign
  return goiApi(`/api/appeals/${id}/assign`, { method: 'POST', body: { reviewerId, reason } })
}

export function giaoLaiKhieuNai(id, duLieu) {
  return goiApi(`/api/appeals/${id}/reassign`, { method: 'POST', body: duLieu })
}

export function layPhieuChamMu() { return goiApi('/api/appeal-reviews') }
export function layChiTietChamMu(id) { return goiApi(`/api/appeal-reviews/${id}`) }
export function luuNhapChamMu(id, criteria) { return goiApi(`/api/appeal-reviews/${id}`, { method: 'PUT', body: { criteria } }) } // [LUONG 3 - CHAM MU] buoc 3/7 PUT /api/appeal-reviews/{id} (luu nhap)
export function nopPhieuChamMu(id, criteria) { return goiApi(`/api/appeal-reviews/${id}/submit`, { method: 'POST', body: { criteria } }) } // [LUONG 3 - CHAM MU] buoc 3/7 POST /api/appeal-reviews/{id}/submit (nop phieu)
export function laySoSanhKhieuNai(id) { return goiApi(`/api/appeals/${id}/comparison`) } // [LUONG 4 - SO SANH & CHOT] buoc 4/7 GET /api/appeals/{id}/comparison
export function phanCongLanHai(id, reviewerId, reason) { return goiApi(`/api/appeals/${id}/assign-second`, { method: 'POST', body: { reviewerId, reason } }) } // [LUONG 4 - SO SANH & CHOT] nhanh S2: POST /api/appeals/{id}/assign-second
export function chotDiemKhieuNai(id, expectedAttemptId) { return goiApi(`/api/appeals/${id}/finalize`, { method: 'POST', body: { expectedAttemptId } }) } // [LUONG 4 - SO SANH & CHOT] buoc 4/7 POST /api/appeals/{id}/finalize
