import { goiApi } from './goiApi.js'

/**
 * API cua luong Bai viet (BF-01). Chi file nay goi server cho BF-01 (AGENTS.md muc 4).
 * Moi ham goi qua goiApi dung chung (ten giu nguyen vi la file dung chung): tu gan
 * token dang nhap (server biet ai dang thao tac tu token), tra ve phan "data" cua
 * ApiResponse, loi thi nem Error kem message tieng Viet server tra ve.
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
 * 3 ham cho trang chu Phong vien (gop y nhom truong muc C4).
 * Dang du lieu giong ham tuong ung trong reporterHomeApi.js, rieng loi moi
 * dong tac gia tam tra mainAuthorId (chua co ten vi chua co UserService).
 */
export function getMyStatusCounts() {
  return goiApi('/api/articles/status-counts')
}

export function getMyRevisionRequests() {
  return goiApi('/api/articles/revision-requests')
}

export function getMyCoAuthorInvites() {
  return goiApi('/api/articles/co-author-invites')
}
