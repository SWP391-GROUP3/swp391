import { goiApi } from './goiApi.js'

export function layDotChamCheo() { return goiApi('/api/cross-review-batches') }
export function layChiTietDotCheo(id) { return goiApi(`/api/cross-review-batches/${id}`) }
export function layBaiChamCheoHopLe(fromDate, toDate) {
  return goiApi(`/api/cross-review-batches/candidates?${new URLSearchParams({ fromDate, toDate })}`)
}
export function taoDotChamCheo(duLieu) { return goiApi('/api/cross-review-batches', { method: 'POST', body: duLieu }) }
export function dongDotChamCheo(id) { return goiApi(`/api/cross-review-batches/${id}/close`, { method: 'POST' }) }
export function goBaiChamCheo(id, taskId) { return goiApi(`/api/cross-review-batches/${id}/tasks/${taskId}`, { method: 'DELETE' }) }
export function layPhieuChamCheo() { return goiApi('/api/cross-review-tasks') }
export function layChiTietChamCheo(id) { return goiApi(`/api/cross-review-tasks/${id}`) }
export function nopPhieuChamCheo(id, criteria) { return goiApi(`/api/cross-review-tasks/${id}/submit`, { method: 'POST', body: { criteria } }) }
