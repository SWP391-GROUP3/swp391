import { goiApi } from './goiApi.js'

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
