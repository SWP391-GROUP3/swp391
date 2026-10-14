import { goiApi } from './goiApi.js'

/**
 * API client cho Luồng BF-02: Phân công, Đánh giá chất lượng & Duyệt đăng.
 */

// UC-14: Lấy danh sách các phiên bản bộ tiêu chí theo thể loại
export async function layDanhSachBoTieuChi(categoryId) {
  return await goiApi(`/api/criteria-sets/category/${categoryId}`)
}

// UC-14 / BR-QA-03: Lấy bộ tiêu chí ACTIVE của thể loại
export async function layBoTieuChiActive(categoryId) {
  return await goiApi(`/api/criteria-sets/category/${categoryId}/active`)
}

// UC-15, UC-16: Tạo phiên bản bộ tiêu chí mới (DRAFT)
export async function taoBoTieuChiDraft(payload) {
  return await goiApi('/api/criteria-sets', {
    method: 'POST',
    body: payload,
  })
}

// UC-17: Kích hoạt bộ tiêu chí (DRAFT -> ACTIVE)
export async function kichHoatBoTieuChi(versionId) {
  return await goiApi(`/api/criteria-sets/${versionId}/activate`, {
    method: 'PUT',
  })
}

// UC-18, UC-20: Trưởng ban phân công bài viết cho BTV
export async function phanCongBaiViet(payload) {
  return await goiApi('/api/assignments', {
    method: 'POST',
    body: payload,
  })
}

// Lấy thông tin phân công của bài viết
export async function layPhanCongBaiViet(articleId) {
  return await goiApi(`/api/assignments/article/${articleId}`)
}

// UC-21: Danh sách bài được giao của BTV
export async function layBaiDuocGiaoCuaBtv(editorId) {
  return await goiApi(`/api/assignments/editor/${editorId}`)
}

// UC-19: Khối lượng công việc của BTV
export async function layKhoiLuongCongViecBtv(editorId) {
  return await goiApi(`/api/assignments/editor/${editorId}/workload`)
}
