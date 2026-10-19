import { goiApi } from './goiApi.js'

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
