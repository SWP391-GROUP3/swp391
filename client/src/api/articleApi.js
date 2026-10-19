import { goiApi } from './goiApi.js'

/**
 * API cua luong Bai viet (BF-01). Chi file nay goi server cho BF-01 (AGENTS.md muc 4).
 * Moi ham goi qua goiApi dung chung (ten giu nguyen vi la file dung chung): tu gan
 * token dang nhap (server biet ai dang thao tac tu token), tra ve phan "data" cua
 * ApiResponse, loi thi nem Error kem message tieng Viet server tra ve.
 *
 * MUC LUC (so dong)
 *   22  getMyArticles                - UC-01: bai cua toi, sua gan nhat len dau.
 *   31  getArticleDetail             - UC-02: chi tiet 1 bai (noi dung, tac gia, lich su trang thai).
 *   36  withdrawArticle              - UC-11: rut bai da nop ve Nhap. Tra ve chi tiet bai sau khi rut.
 *   45  getMyStatusCounts            - 3 ham cho trang chu Phong vien (gop y nhom truong muc C4), dang du lieu…
 *   50  getMyRevisionRequests        - Trang chu PV: bai bi tra ve can sua, kem ghi chu lan tra gan nhat
 *   55  getMyCoAuthorInvites         - Trang chu PV + trang UC-12: loi moi dong tac gia dang cho minh tra loi
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
