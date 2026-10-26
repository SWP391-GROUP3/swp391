import { goiApi } from './goiApi.js'

/**
 * MUC LUC (so dong)
 *   11  layDieuKienKhieuNai
 *   15  guiKhieuNai
 *   19  layKhieuNaiCuaToi
 *   23  layBaiCoTheKhieuNai
 */

export async function layDieuKienKhieuNai(articleId) {
  return goiApi(`/api/articles/${articleId}/appeal-eligibility`)
}

export async function guiKhieuNai(articleId, criteria) {
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

export function phanCongKhieuNai(id, reviewerId, reason) {
  return goiApi(`/api/appeals/${id}/assign`, { method: 'POST', body: { reviewerId, reason } })
}

export function giaoLaiKhieuNai(id, duLieu) {
  return goiApi(`/api/appeals/${id}/reassign`, { method: 'POST', body: duLieu })
}

export function layPhieuChamMu() { return goiApi('/api/appeal-reviews') }
export function layChiTietChamMu(id) { return goiApi(`/api/appeal-reviews/${id}`) }
export function luuNhapChamMu(id, criteria) { return goiApi(`/api/appeal-reviews/${id}`, { method: 'PUT', body: { criteria } }) }
export function nopPhieuChamMu(id, criteria) { return goiApi(`/api/appeal-reviews/${id}/submit`, { method: 'POST', body: { criteria } }) }
export function laySoSanhKhieuNai(id) { return goiApi(`/api/appeals/${id}/comparison`) }
export function phanCongLanHai(id, reviewerId, reason) { return goiApi(`/api/appeals/${id}/assign-second`, { method: 'POST', body: { reviewerId, reason } }) }
export function chotDiemKhieuNai(id, expectedAttemptId) { return goiApi(`/api/appeals/${id}/finalize`, { method: 'POST', body: { expectedAttemptId } }) }
