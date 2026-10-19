import { goiApi } from './goiApi.js'

/**
 * MUC LUC (so dong)
 *   11  layDieuKienKhieuNai
 *   15  guiKhieuNai
 *   19  layKhieuNaiCuaToi
 *   23  layBaiCoTheKhieuNai
 */

export async function layDieuKienKhieuNai(articleId) {
  return goiApi(`/api/articles/${articleId}/appeal-eligibility`)
}

export async function guiKhieuNai(articleId, criteria) {
  return goiApi('/api/appeals', { method: 'POST', body: { articleId, criteria } })
}

export async function layKhieuNaiCuaToi() {
  return goiApi('/api/appeals/mine')
}

export async function layBaiCoTheKhieuNai() {
  return goiApi('/api/appeals/eligible-articles')
}
