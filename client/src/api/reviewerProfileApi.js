import { goiApi } from './goiApi.js'

/**
 * API ho so bien tap vien (UC-42): danh sach BTV, ho so chat luong cham, lich su cham. Goi qua
 * goiApi().
 *
 * MUC LUC (so dong - ten - tac dung)
 *   13  layDanhSachBienTapVien()        lay danh sach bien tap vien [GET /api/reviewers]
 *   17  layHoSoBienTapVien()            lay ho so bien tap vien [GET /api/reviewers/{editorId}{sizequery}` : ''}]
 *   23  layLichSuCham()                 lay lich su cham [GET /api/reviewers/{editorId}/scoring-history?{query}]
 */

export function layDanhSachBienTapVien() {
  return goiApi('/api/reviewers')
}

export function layHoSoBienTapVien(editorId, thang) {
  const query = new URLSearchParams()
  if (thang) query.set('period', `${thang}-01`)
  return goiApi(`/api/reviewers/${editorId}${query.size ? `?${query}` : ''}`)
}

export function layLichSuCham(editorId, thang, trang = 0) {
  const query = new URLSearchParams()
  if (thang) query.set('period', `${thang}-01`)
  query.set('page', String(trang))
  query.set('size', '10')
  return goiApi(`/api/reviewers/${editorId}/scoring-history?${query}`)
}
