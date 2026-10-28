import { goiApi } from './goiApi.js'

/**
 * API nhat ky cham (UC-45): lay nhat ky thao tac cham / khieu nai theo bo loc. Goi qua goiApi().
 *
 * MUC LUC (so dong - ten - tac dung)
 *   10  layNhatKy()                     lay nhat ky [GET duongDan]
 */

export async function layNhatKy(boLoc = {}) {
  const query = new URLSearchParams()
  if (boLoc.actorId) query.set('actorId', boLoc.actorId)
  if (boLoc.actionType) query.set('actionType', boLoc.actionType)
  if (boLoc.entityType) query.set('entityType', boLoc.entityType)
  if (boLoc.entityId) query.set('entityId', boLoc.entityId)
  if (boLoc.articleId) query.set('articleId', boLoc.articleId)
  if (boLoc.appealId) query.set('appealId', boLoc.appealId)
  if (boLoc.editorId) query.set('editorId', boLoc.editorId)
  if (boLoc.fromTime) query.set('fromTime', `${boLoc.fromTime}T00:00:00`)
  if (boLoc.toTime) query.set('toTime', `${boLoc.toTime}T23:59:59.9999999`)

  const duongDan = query.size > 0
    ? `/api/audit-logs?${query.toString()}`
    : '/api/audit-logs'
  return goiApi(duongDan)
}
