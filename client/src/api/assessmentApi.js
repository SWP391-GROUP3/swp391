import { goiApi } from './goiApi.js'


/**
 * API client cho Luồng BF-02: Phân công, Đánh giá chất lượng & Duyệt đăng.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   29  layDanhSachBoTieuChi()          UC-14: Lay danh sach cac phien ban bo tieu chi theo the loai [GET /api/criteria-sets/category/{categoryId}]
 *   33  layBoTieuChiActive()            lay bo tieu chi active [GET /api/criteria-sets/category/{categoryId}/active]
 *   37  taoBoTieuChiDraft()             tao bo tieu chi draft [POST /api/criteria-sets]
 *   41  kichHoatBoTieuChi()             kich hoat bo tieu chi [PUT /api/criteria-sets/{versionId}/activate]
 *   45  phanCongBaiViet()               phan cong bai viet [POST /api/assignments]
 *   49  layPhanCongBaiViet()            lay phan cong bai viet [GET /api/assignments/article/{articleId}]
 *   53  layBaiDuocGiaoCuaBtv()          lay bai duoc giao cua btv [GET /api/assignments/editor/{editorId}]
 *   57  layKhoiLuongCongViecBtv()       lay khoi luong cong viec btv [GET /api/assignments/editor/{editorId}/workload]
 *   61  moPhieuCham()                   mo phieu cham [POST /api/scoring-attempts/article/{articleId}/open]
 *   65  layPhieuChamMoiNhat()           lay phieu cham moi nhat [GET /api/scoring-attempts/article/{articleId}/latest]
 *   69  doiTheLoaiBaiViet()             doi the loai bai viet [POST /api/scoring-attempts/article/{articleId}/change-category]
 *   76  luuDiemTieuChi()                luu diem tieu chi [PUT /api/scoring-attempts/article/{articleId}/scores]
 *   83  nopPhieuCham()                  nop phieu cham [POST /api/scoring-attempts/article/{articleId}/submit]
 *   94  layDanhSachChoDuyetDiem()       Truong ban: Lay hang cho duyet diem danh gia (UC-29) [GET /api/score-approvals{query}]
 *  100  duyetDiemBaiViet()              Truong ban: Phe duyet diem danh gia (UC-32) [POST /api/score-approvals/article/{articleId}/approve]
 *  107  yeuCauChamLai()                 Truong ban: Yeu cau cham lai voi ly do bat buoc (UC-31) [POST /api/score-approvals/article/{articleId}/return]
 *  115  layDanhSachChoDuyetDang()       Truong ban: Lay hang cho duyet xuat ban bai viet (UC-33) [GET /api/score-approvals/publishing{query}]
 *  121  duyetDangBaiViet()              Truong ban: Phe duyet xuat ban bai viet (UC-33) [POST /api/score-approvals/article/{articleId}/publish]
 */

// UC-14: Lấy danh sách các phiên bản bộ tiêu chí theo thể loại
export async function layDanhSachBoTieuChi(categoryId) {
  return await goiApi(`/api/criteria-sets/category/${categoryId}`)
}

export function layBoTieuChiActive(categoryId) {
  return goiApi(`/api/criteria-sets/category/${categoryId}/active`)
}

export function taoBoTieuChiDraft(payload) {
  return goiApi('/api/criteria-sets', { method: 'POST', body: payload })
}

export function kichHoatBoTieuChi(versionId) {
  return goiApi(`/api/criteria-sets/${versionId}/activate`, { method: 'PUT' })
}

export function phanCongBaiViet(payload) {
  return goiApi('/api/assignments', { method: 'POST', body: payload })
}

export function layPhanCongBaiViet(articleId) {
  return goiApi(`/api/assignments/article/${articleId}`)
}

export function layBaiDuocGiaoCuaBtv(editorId) {
  return goiApi(`/api/assignments/editor/${editorId}`)
}

export function layKhoiLuongCongViecBtv(editorId) {
  return goiApi(`/api/assignments/editor/${editorId}/workload`)
}

export function moPhieuCham(articleId) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/open`, { method: 'POST' })
}

export function layPhieuChamMoiNhat(articleId) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/latest`)
}

export function doiTheLoaiBaiViet(articleId, newCategoryId) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/change-category`, {
    method: 'POST',
    body: { newCategoryId }
  })
}

export function luuDiemTieuChi(articleId, danhSachDiem) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/scores`, {
    method: 'PUT',
    body: { scores: danhSachDiem }
  })
}

export function nopPhieuCham(articleId, quyetDinh, ghiChu) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/submit`, {
    method: 'POST',
    body: {
      decision: quyetDinh,
      decisionNote: ghiChu
    }
  })
}

// Truong ban: Lay hang cho duyet diem danh gia (UC-29)
export function layDanhSachChoDuyetDiem(sectionId) {
  const query = sectionId ? `?sectionId=${sectionId}` : ''
  return goiApi(`/api/score-approvals${query}`)
}

// Truong ban: Phe duyet diem danh gia (UC-32)
export function duyetDiemBaiViet(articleId) {
  return goiApi(`/api/score-approvals/article/${articleId}/approve`, {
    method: 'POST'
  })
}

// Truong ban: Yeu cau cham lai voi ly do bat buoc (UC-31)
export function yeuCauChamLai(articleId, reason) {
  return goiApi(`/api/score-approvals/article/${articleId}/return`, {
    method: 'POST',
    body: { reason }
  })
}

// Truong ban: Lay hang cho duyet xuat ban bai viet (UC-33)
export function layDanhSachChoDuyetDang(sectionId) {
  const query = sectionId ? `?sectionId=${sectionId}` : ''
  return goiApi(`/api/score-approvals/publishing${query}`)
}

// Truong ban: Phe duyet xuat ban bai viet (UC-33)
export function duyetDangBaiViet(articleId) {
  return goiApi(`/api/score-approvals/article/${articleId}/publish`, {
    method: 'POST'
  })
}
