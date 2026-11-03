import { goiApi } from './goiApi.js'


/**
 * API client cho Luồng BF-02: Phân công, Đánh giá chất lượng & Duyệt đăng.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  layDanhSachBoTieuChi()          UC-14: Lay danh sach cac phien ban bo tieu chi theo the loai [GET /api/criteria-sets/category/{categoryId}]
 *   35  layBoTieuChiActive()            lay bo tieu chi active [GET /api/criteria-sets/category/{categoryId}/active]
 *   39  taoBoTieuChiDraft()             tao bo tieu chi draft [POST /api/criteria-sets]
 *   43  kichHoatBoTieuChi()             kich hoat bo tieu chi [PUT /api/criteria-sets/{versionId}/activate]
 *   48  layDanhSachChoPhanCong()        Ban lay tu tai khoan Truong ban dang dang nhap (server doc tu token) [GET /api/assignments/pending]
 *   52  phanCongBaiViet()               phan cong bai viet [POST /api/assignments]
 *   56  layPhanCongBaiViet()            lay phan cong bai viet [GET /api/assignments/article/{articleId}]
 *   60  layBaiDuocGiaoCuaBtv()          lay bai duoc giao cua btv [GET /api/assignments/editor/{editorId}]
 *   64  layKhoiLuongCongViecBtv()       lay khoi luong cong viec btv [GET /api/assignments/editor/{editorId}/workload]
 *   69  layLichSuChamCuaToi()           BTV: lich su cham cua toi - bai duoc giao + lan cham moi nhat (M-A7) [GET /api/scoring-attempts/mine]
 *   73  moPhieuCham()                   mo phieu cham [POST /api/scoring-attempts/article/{articleId}/open]
 *   77  layPhieuChamMoiNhat()           lay phieu cham moi nhat [GET /api/scoring-attempts/article/{articleId}/latest]
 *   81  doiTheLoaiBaiViet()             doi the loai bai viet [POST /api/scoring-attempts/article/{articleId}/change-category]
 *   88  luuDiemTieuChi()                luu diem tieu chi [PUT /api/scoring-attempts/article/{articleId}/scores]
 *   95  nopPhieuCham()                  nop phieu cham [POST /api/scoring-attempts/article/{articleId}/submit]
 *  106  layDanhSachChoDuyetDiem()       Truong ban: Lay hang cho duyet diem danh gia (UC-29) [GET /api/score-approvals]
 *  111  duyetDiemBaiViet()              Truong ban: Phe duyet diem danh gia (UC-31) [POST /api/score-approvals/article/{articleId}/approve]
 *  118  yeuCauChamLai()                 Truong ban: Yeu cau cham lai voi ly do bat buoc (UC-32) [POST /api/score-approvals/article/{articleId}/return]
 *  126  layDanhSachChoDuyetDang()       Truong ban: Lay hang cho duyet xuat ban bai viet (UC-33) [GET /api/score-approvals/publishing]
 *  131  duyetDangBaiViet()              Truong ban: Phe duyet xuat ban bai viet (UC-33) [POST /api/score-approvals/article/{articleId}/publish]
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

// Ban lay tu tai khoan Truong ban dang dang nhap (server doc tu token)
export function layDanhSachChoPhanCong() {
  return goiApi('/api/assignments/pending')
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

// BTV: lich su cham cua toi - bai duoc giao + lan cham moi nhat (M-A7)
export function layLichSuChamCuaToi() {
  return goiApi('/api/scoring-attempts/mine')
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
export function layDanhSachChoDuyetDiem() {
  return goiApi('/api/score-approvals')
}

// Truong ban: Phe duyet diem danh gia (UC-31)
export function duyetDiemBaiViet(articleId) {
  return goiApi(`/api/score-approvals/article/${articleId}/approve`, {
    method: 'POST'
  })
}

// Truong ban: Yeu cau cham lai voi ly do bat buoc (UC-32)
export function yeuCauChamLai(articleId, reason) {
  return goiApi(`/api/score-approvals/article/${articleId}/return`, {
    method: 'POST',
    body: { reason }
  })
}

// Truong ban: Lay hang cho duyet xuat ban bai viet (UC-33)
export function layDanhSachChoDuyetDang() {
  return goiApi('/api/score-approvals/publishing')
}

// Truong ban: Phe duyet xuat ban bai viet (UC-33)
export function duyetDangBaiViet(articleId) {
  return goiApi(`/api/score-approvals/article/${articleId}/publish`, {
    method: 'POST'
  })
}
