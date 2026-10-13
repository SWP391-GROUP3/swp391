/**
 * API client cho module cau hinh luat khieu nai (UC-44).
 * Quy tac AGENTS.md: Chi goi qua /api/..., khong dung http://localhost:8080.
 */

export async function getActiveConfig() {
  const res = await fetch('/api/appeal-rule-configs/active')
  const json = await res.json()
  if (!res.ok || !json.success) {
    throw new Error(json.message || 'Không thể tải cấu hình khiếu nại hiện tại')
  }
  return json.data
}

export async function getAllConfigs() {
  const res = await fetch('/api/appeal-rule-configs')
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
      'X-User-Id': '8', // TODO(auth): Tam thoi truyen Admin ID theo de xuat AGENTS.md
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
