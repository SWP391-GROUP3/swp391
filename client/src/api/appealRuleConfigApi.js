/**
 * API client cho module cau hinh luat khieu nai (UC-44).
 * Quy tac AGENTS.md: Chi goi qua /api/..., khong dung http://localhost:8080.
 */

import { layHeaderNguoiDung } from './appealSessionHeaders.js'

export async function getActiveConfig() {
  const res = await fetch('/api/appeal-rule-configs/active', { headers: layHeaderNguoiDung() })
  const json = await res.json()
  if (!res.ok || !json.success) {
    const loi = new Error(json.message || 'Không thể tải cấu hình khiếu nại hiện tại')
    loi.status = res.status
    throw loi
  }
  return json.data
}

export async function getAllConfigs() {
  const res = await fetch('/api/appeal-rule-configs', { headers: layHeaderNguoiDung() })
  const json = await res.json()
  if (!res.ok || !json.success) {
    throw new Error(json.message || 'Không thể tải danh sách lịch sử cấu hình')
  }
  return json.data
}

export async function createConfig(data) {
  const res = await fetch('/api/appeal-rule-configs', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...layHeaderNguoiDung(),
    },
    body: JSON.stringify(data),
  })
  const json = await res.json()
  if (!res.ok || !json.success) {
    // Neu co loi chi tiet tung truong
    if (json.errors) {
      const errorMsg = Object.values(json.errors).join(', ')
      throw new Error(errorMsg || json.message)
    }
    throw new Error(json.message || 'Tạo cấu hình mới thất bại')
  }
  return json.data
}
