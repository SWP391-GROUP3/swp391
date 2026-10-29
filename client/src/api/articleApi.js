import { goiApi } from './goiApi.js'

/**
 * API cua luong Bai viet (BF-01). Chi file nay goi server cho BF-01 (AGENTS.md muc 4).
 * Moi ham goi qua goiApi dung chung (ten giu nguyen vi la file dung chung): tu gan
 * token dang nhap (server biet ai dang thao tac tu token), tra ve phan "data" cua
 * ApiResponse, loi thi nem Error kem message tieng Viet server tra ve.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   30  getMyArticles()                 UC-01: bai cua toi, sua gan nhat len dau [GET /api/articles?{params}]
 *   39  getArticleDetail()              UC-02: chi tiet 1 bai (noi dung, tac gia, lich su trang thai) [GET /api/articles/{articleId}]
 *   44  withdrawArticle()               UC-11: rut bai da nop ve Nhap [POST /api/articles/{articleId}/withdraw]
 *   53  getMyStatusCounts()             3 ham cho trang chu Phong vien (gop y nhom truong muc C4), dang du li… [GET /api/articles/status-counts]
 *   58  getMyRevisionRequests()         Trang chu PV: bai bi tra ve can sua, kem ghi chu lan tra gan nhat [GET /api/articles/revision-requests]
 *   63  getMyCoAuthorInvites()          Trang chu PV + trang UC-12: loi moi dong tac gia dang cho minh tra loi [GET /api/articles/co-author-invites]
 *   70  getGenres()                     UC-03: danh sach the loai dang dung (API dung chung cua admin - GET /… [GET /api/genres]
 *   75  getCoAuthorCandidates()         UC-04: phong vien co the chon lam dong tac gia [GET /api/articles/co-author-candidates]
 *   83  createArticle()                 UC-03 + UC-04: tao ban nhap [POST /api/articles]
 *   88  updateArticle()                 UC-03 + UC-04: sua ban nhap (cung dang du lieu voi createArticle) [PUT /api/articles/{articleId}]
 *   93  submitArticle()                 UC-05: nop bai Nhap [POST /api/articles/{articleId}/submit]
 *   98  resubmitArticle()               UC-10: nop lai bai bi tra ve [POST /api/articles/{articleId}/resubmit]
 *  103  acceptCoAuthorship()            UC-12: dong tac gia dong y [POST /api/articles/{articleId}/co-author/accept]
 *  108  rejectCoAuthorship()            UC-12 A1: dong tac gia tu choi, bat buoc ly do [POST /api/articles/{articleId}/co-author/reject]
 */

/**
 * UC-01: bai cua toi, sua gan nhat len dau.
 * statuses: mang ma trang thai, rong = tat ca. page bat dau tu 0.
 */
export function getMyArticles(statuses, page, size) {
  const params = new URLSearchParams({ page, size })
  if (statuses.length > 0) {
    params.set('status', statuses.join(','))
  }
  return goiApi(`/api/articles?${params}`)
}

/** UC-02: chi tiet 1 bai (noi dung, tac gia, lich su trang thai). */
export function getArticleDetail(articleId) {
  return goiApi(`/api/articles/${articleId}`)
}

/** UC-11: rut bai da nop ve Nhap. Tra ve chi tiet bai sau khi rut. */
export function withdrawArticle(articleId) {
  return goiApi(`/api/articles/${articleId}/withdraw`, { method: 'POST' })
}

/**
 * 3 ham cho trang chu Phong vien (gop y nhom truong muc C4), dang du lieu giong
 * ham tuong ung trong reporterHomeApi.js. getMyCoAuthorInvites con dung cho
 * trang "Xac nhan dong tac gia" (UC-12): moi dong co mainAuthorName.
 */
export function getMyStatusCounts() {
  return goiApi('/api/articles/status-counts')
}

// Trang chu PV: bai bi tra ve can sua, kem ghi chu lan tra gan nhat
export function getMyRevisionRequests() {
  return goiApi('/api/articles/revision-requests')
}

// Trang chu PV + trang UC-12: loi moi dong tac gia dang cho minh tra loi
export function getMyCoAuthorInvites() {
  return goiApi('/api/articles/co-author-invites')
}

// ---------------------------------------------------------------- Iter 2

// UC-03: danh sach the loai dang dung (API dung chung cua admin - GET /api/genres)
export function getGenres() {
  return goiApi('/api/genres')
}

// UC-04: phong vien co the chon lam dong tac gia
export function getCoAuthorCandidates() {
  return goiApi('/api/articles/co-author-candidates')
}

/**
 * UC-03 + UC-04: tao ban nhap. duLieu = { title, content, declaredCategoryId, declaredNature,
 * authors: [{ userId, sharePercent }] } (gom ca tac gia chinh). Tra ve chi tiet bai.
 */
export function createArticle(duLieu) {
  return goiApi('/api/articles', { method: 'POST', body: duLieu })
}

// UC-03 + UC-04: sua ban nhap (cung dang du lieu voi createArticle)
export function updateArticle(articleId, duLieu) {
  return goiApi(`/api/articles/${articleId}`, { method: 'PUT', body: duLieu })
}

// UC-05: nop bai Nhap
export function submitArticle(articleId) {
  return goiApi(`/api/articles/${articleId}/submit`, { method: 'POST' })
}

// UC-10: nop lai bai bi tra ve
export function resubmitArticle(articleId) {
  return goiApi(`/api/articles/${articleId}/resubmit`, { method: 'POST' })
}

// UC-12: dong tac gia dong y
export function acceptCoAuthorship(articleId) {
  return goiApi(`/api/articles/${articleId}/co-author/accept`, { method: 'POST' })
}

// UC-12 A1: dong tac gia tu choi, bat buoc ly do
export function rejectCoAuthorship(articleId, reason) {
  return goiApi(`/api/articles/${articleId}/co-author/reject`, { method: 'POST', body: { reason } })
}
