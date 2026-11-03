import { goiApi } from './goiApi.js'

/**
 * API cua luong Bai viet (BF-01). Chi file nay goi server cho BF-01 (AGENTS.md muc 4).
 * Moi ham goi qua goiApi dung chung (ten giu nguyen vi la file dung chung): tu gan
 * token dang nhap (server biet ai dang thao tac tu token), tra ve phan "data" cua
 * ApiResponse, loi thi nem Error kem message tieng Viet server tra ve.
 *
 * CODE LIEN QUAN: moi ham o day goi 1 ham cung ten (hoac gan cung ten) trong
 *   server/src/main/java/com/swp391/kpi/article/controller/ArticleController.java
 *   (vd withdrawArticle -> ArticleController.withdrawArticle, createArticle -> createDraft, checkDuplication -> previewDuplicationCheck).
 *   getGenres, getOpenEvents, getMediaFactors goi API cua luong khac (admin, royalty).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   44  getMyArticles()                 UC-01: bai cua toi, sua gan nhat len dau [GET /api/articles?{params}]
 *   53  getArticleDetail()              UC-02: chi tiet 1 bai (noi dung, tac gia, lich su trang thai) [GET /api/articles/{articleId}]
 *   58  withdrawArticle()               UC-11: rut bai da nop ve Nhap [POST /api/articles/{articleId}/withdraw]
 *   67  getMyStatusCounts()             3 ham cho trang chu Phong vien (gop y nhom truong muc C4), dang du li… [GET /api/articles/status-counts]
 *   72  getMyRevisionRequests()         Trang chu PV: bai bi tra ve can sua, kem ghi chu lan tra gan nhat [GET /api/articles/revision-requests]
 *   77  getMyCoAuthorInvites()          Trang chu PV + trang UC-12: loi moi dong tac gia dang cho minh tra loi [GET /api/articles/co-author-invites]
 *   84  getGenres()                     UC-03: danh sach the loai dang dung (API dung chung cua admin - GET /… [GET /api/genres]
 *   89  getCoAuthorCandidates()         UC-04: phong vien co the chon lam dong tac gia [GET /api/articles/co-author-candidates]
 *   97  createArticle()                 UC-03 + UC-04: tao ban nhap [POST /api/articles]
 *  102  updateArticle()                 UC-03 + UC-04: sua ban nhap (cung dang du lieu voi createArticle) [PUT /api/articles/{articleId}]
 *  107  submitArticle()                 UC-05: nop bai Nhap. duLieu = { explanation } khi noi dung trung can … [POST /api/articles/{articleId}/submit]
 *  112  resubmitArticle()               UC-10: nop lai bai bi tra ve (duLieu giong submitArticle) [POST /api/articles/{articleId}/resubmit]
 *  117  acceptCoAuthorship()            UC-12: dong tac gia dong y [POST /api/articles/{articleId}/co-author/accept]
 *  122  rejectCoAuthorship()            UC-12 A1: dong tac gia tu choi, bat buoc ly do [POST /api/articles/{articleId}/co-author/reject]
 *  129  getOpenEvents()                 UC-03: su kien dang mo de chon (API cua BF-04 - GET /api/events/open) [GET /api/events/open]
 *  137  estimateRoyalty()               UC-09: uoc tinh nhuan but phan bai viet o 3 muc diem, khong luu gi [POST /api/articles/royalty-estimate]
 *  142  getMediaFactors()               UC-03: loai anh/video va he so (API dung chung cua admin - GET /api/m… [GET /api/media-factors]
 *  147  getAttachments()                UC-03 / UC-07: anh/video cua bai kem ket qua kiem trung [GET /api/articles/{articleId}/attachments]
 *  155  addAttachment()                 UC-03 / UC-04 buoc 4: them 1 anh hoac 1 video [POST /api/articles/{articleId}/attachments]
 *  160  deleteAttachment()              Xoa 1 anh/video khi bai con sua duoc [DELETE /api/articles/{articleId}/attachments/{attachmentId}]
 *  165  getAttachmentFile()             Q7: file anh dang Blob (kem token) de hien bang URL.createObjectURL [GET /api/articles/{articleId}/attachments/{attachmentId}/file]
 *  170  checkDuplication()              UC-05 buoc 2 (B5): kiem trung noi dung truoc khi xac nhan nop, server… [POST /api/articles/{articleId}/duplication-check]
 *  175  getDuplicationCheck()           UC-06 / UC-08: ket qua kiem trung luc nop gan nhat (kem giai trinh); … [GET /api/articles/{articleId}/duplication-check]
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

// UC-05: nop bai Nhap. duLieu = { explanation } khi noi dung trung can giai trinh (UC-08), co the bo trong
export function submitArticle(articleId, duLieu = {}) {
  return goiApi(`/api/articles/${articleId}/submit`, { method: 'POST', body: duLieu })
}

// UC-10: nop lai bai bi tra ve (duLieu giong submitArticle)
export function resubmitArticle(articleId, duLieu = {}) {
  return goiApi(`/api/articles/${articleId}/resubmit`, { method: 'POST', body: duLieu })
}

// UC-12: dong tac gia dong y
export function acceptCoAuthorship(articleId) {
  return goiApi(`/api/articles/${articleId}/co-author/accept`, { method: 'POST' })
}

// UC-12 A1: dong tac gia tu choi, bat buoc ly do
export function rejectCoAuthorship(articleId, reason) {
  return goiApi(`/api/articles/${articleId}/co-author/reject`, { method: 'POST', body: { reason } })
}

// ---------------------------------------------------------------- Iter 3

// UC-03: su kien dang mo de chon (API cua BF-04 - GET /api/events/open)
export function getOpenEvents() {
  return goiApi('/api/events/open')
}

/**
 * UC-09: uoc tinh nhuan but phan bai viet o 3 muc diem, khong luu gi.
 * duLieu = { declaredCategoryId, declaredNature, eventId, articleId } (eventId, articleId co the null).
 */
export function estimateRoyalty(duLieu) {
  return goiApi('/api/articles/royalty-estimate', { method: 'POST', body: duLieu })
}

// UC-03: loai anh/video va he so (API dung chung cua admin - GET /api/media-factors)
export function getMediaFactors() {
  return goiApi('/api/media-factors')
}

// UC-03 / UC-07: anh/video cua bai kem ket qua kiem trung
export function getAttachments(articleId) {
  return goiApi(`/api/articles/${articleId}/attachments`)
}

/**
 * UC-03 / UC-04 buoc 4: them 1 anh hoac 1 video. formData gom: file (anh) hoac videoUrl (link),
 * mediaKind, authorId, externalSource, caption. Gui FormData thi goiApi tu de trinh duyet dat Content-Type.
 */
export function addAttachment(articleId, formData) {
  return goiApi(`/api/articles/${articleId}/attachments`, { method: 'POST', body: formData })
}

// Xoa 1 anh/video khi bai con sua duoc
export function deleteAttachment(articleId, attachmentId) {
  return goiApi(`/api/articles/${articleId}/attachments/${attachmentId}`, { method: 'DELETE' })
}

// Q7: file anh dang Blob (kem token) de hien bang URL.createObjectURL
export function getAttachmentFile(articleId, attachmentId) {
  return goiApi(`/api/articles/${articleId}/attachments/${attachmentId}/file`, { taiFile: true })
}

// UC-05 buoc 2 (B5): kiem trung noi dung truoc khi xac nhan nop, server chi tinh, khong luu
export function checkDuplication(articleId) {
  return goiApi(`/api/articles/${articleId}/duplication-check`, { method: 'POST' })
}

// UC-06 / UC-08: ket qua kiem trung luc nop gan nhat (kem giai trinh); chua nop lan nao -> null
export function getDuplicationCheck(articleId) {
  return goiApi(`/api/articles/${articleId}/duplication-check`)
}
