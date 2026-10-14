import { goiApi } from './goiApi.js'

export async function layBaoCaoDoLech(boLoc = {}) {
  const query = new URLSearchParams()
  if (boLoc.period) query.set('period', `${boLoc.period}-01`)
  if (boLoc.editorId) query.set('editorId', boLoc.editorId)
  if (boLoc.sectionId) query.set('sectionId', boLoc.sectionId)

  const duongDan = query.size > 0
    ? `/api/reports/reviewer-deviations?${query.toString()}`
    : '/api/reports/reviewer-deviations'
  return goiApi(duongDan)
}
