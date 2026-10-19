import { goiApi } from './goiApi.js'

// API Client cho Luong BF-02 (Phan cong & Danh gia chat luong)
export function layDanhSachBoTieuChi(categoryId) {
  return goiApi(`/api/criteria-sets/category/${categoryId}`)
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



