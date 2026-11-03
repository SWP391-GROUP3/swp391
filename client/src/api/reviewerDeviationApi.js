import { goiApi } from './goiApi.js'

/**
 * API bao cao do lech nguoi cham (UC-41). Goi qua goiApi().
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  layBaoCaoDoLech()               lay bao cao do lech [GET duongDan]
 *   25  tinhLaiBaoCaoDoLech()           Tinh lai va luu chi so (thang trong bo loc, de trong = moi thang) [POST /api/reports/reviewer-deviations/refresh{query}]
 *      tinhLaiBaoCaoDoLech()           tinh lai chi so [POST /api/reports/reviewer-deviations/refresh]
 */

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

// Tinh lai va luu chi so (thang trong bo loc, de trong = moi thang).
export async function tinhLaiBaoCaoDoLech(period) {
  const query = period ? `?period=${period}-01` : ''
  return goiApi(`/api/reports/reviewer-deviations/refresh${query}`, { method: 'POST' })
}
