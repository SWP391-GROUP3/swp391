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


