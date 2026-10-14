import { layHeaderNguoiDung } from './appealSessionHeaders.js'

async function docJson(response) {
  const json = await response.json()
  if (!response.ok || !json.success) {
    if (json.errors) {
      throw new Error(Object.values(json.errors).join(', '))
    }
    throw new Error(json.message || 'Không thể xử lý yêu cầu khiếu nại')
  }
  return json.data
}

export async function layDieuKienKhieuNai(articleId) {
  const response = await fetch(`/api/articles/${articleId}/appeal-eligibility`, {
    headers: layHeaderNguoiDung(),
  })
  return docJson(response)
}

export async function guiKhieuNai(articleId, criteria) {
  const response = await fetch('/api/appeals', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...layHeaderNguoiDung(),
    },
    body: JSON.stringify({ articleId, criteria }),
  })
  return docJson(response)
}

export async function layKhieuNaiCuaToi() {
  const response = await fetch('/api/appeals/mine', {
    headers: layHeaderNguoiDung(),
  })
  return docJson(response)
}

export async function layBaiCoTheKhieuNai() {
  const response = await fetch('/api/appeals/eligible-articles', { headers: layHeaderNguoiDung() })
  return docJson(response)
}
