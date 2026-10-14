import { layHeaderNguoiDung } from './appealSessionHeaders.js'

export async function layBaoCaoDoLech(boLoc = {}) {
  const query = new URLSearchParams()
  if (boLoc.period) query.set('period', `${boLoc.period}-01`)
  if (boLoc.editorId) query.set('editorId', boLoc.editorId)
  if (boLoc.sectionId) query.set('sectionId', boLoc.sectionId)

  const duongDan = query.size > 0
    ? `/api/reports/reviewer-deviations?${query.toString()}`
    : '/api/reports/reviewer-deviations'
  const response = await fetch(duongDan, { headers: layHeaderNguoiDung() })
  const body = await response.json()
  if (!response.ok || !body.success) {
    throw new Error(body.message || 'Không thể tải báo cáo độ lệch điểm.')
  }
  return body.data
}
