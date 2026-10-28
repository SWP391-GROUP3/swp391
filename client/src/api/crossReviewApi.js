import { goiApi } from './goiApi.js'

/**
 * API dot cham cheo (UC-40): Truong ban tao / dong dot, go bai khoi dot; BTV xem va nop phieu cham
 * cheo. Goi qua goiApi().
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  layDotChamCheo()                lay dot cham cheo [GET /api/cross-review-batches]
 *   20  layChiTietDotCheo()             lay chi tiet dot cheo [GET /api/cross-review-batches/{id}]
 *   21  layBaiChamCheoHopLe()           lay bai cham cheo hop le [GET /api/cross-review-batches/candidates?{newURLSearchParamsfromDatetoDate})}]
 *   24  taoDotChamCheo()                tao dot cham cheo [POST /api/cross-review-batches]
 *   25  dongDotChamCheo()               dong dot cham cheo [POST /api/cross-review-batches/{id}/close]
 *   26  goBaiChamCheo()                 chuyen bai cham cheo [DELETE /api/cross-review-batches/{id}/tasks/{taskId}]
 *   27  layPhieuChamCheo()              lay phieu cham cheo [GET /api/cross-review-tasks]
 *   28  layChiTietChamCheo()            lay chi tiet cham cheo [GET /api/cross-review-tasks/{id}]
 *   29  nopPhieuChamCheo()              nop phieu cham cheo [POST /api/cross-review-tasks/{id}/submit]
 */

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
