/**
 * API client cho luồng KPI (BF-05).
 * Sử dụng fetch tiêu chuẩn và URL tương đối /api/... qua proxy của Vite (AGENTS.md mục 4).
 */

/**
 * Lấy danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
 */
export async function layDinhMucChuan() {
  const res = await fetch('/api/kpi-targets/benchmarks')
  const json = await res.json()
  if (!res.ok) {
    throw new Error(json.message || 'Không thể lấy định mức chuẩn')
  }
  return json.data
}

/**
 * Cập nhật danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
 */
export async function capNhatDinhMucChuan(danhSach) {
  const res = await fetch('/api/kpi-targets/benchmarks', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(danhSach),
  })
  const json = await res.json()
  if (!res.ok) {
    throw new Error(json.message || 'Không thể cập nhật định mức chuẩn')
  }
  return json.data
}

/**
 * Lấy danh sách chỉ tiêu theo năm và tháng (UC-59).
 */
export async function layChiTieuTheoThang(nam, thang) {
  const res = await fetch(`/api/kpi-targets?year=${nam}&month=${thang}`)
  const json = await res.json()
  if (!res.ok) {
    throw new Error(json.message || 'Không thể lấy danh sách chỉ tiêu')
  }
  return json.data
}

/**
 * Giao chỉ tiêu tháng mới cho phóng viên (UC-59).
 */
export async function taoChiTieu(duLieu, userId = 5) {
  const res = await fetch('/api/kpi-targets', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': String(userId),
    },
    body: JSON.stringify(duLieu),
  })
  const json = await res.json()
  if (!res.ok) {
    const errorMsg = json.errors
      ? Object.values(json.errors).join(', ')
      : json.message || 'Lỗi khi giao chỉ tiêu'
    throw new Error(errorMsg)
  }
  return json.data
}

/**
 * Điều chỉnh chỉ tiêu đã giao (UC-59 nhánh A2).
 */
export async function capNhatChiTieu(id, duLieu) {
  const res = await fetch(`/api/kpi-targets/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(duLieu),
  })
  const json = await res.json()
  if (!res.ok) {
    const errorMsg = json.errors
      ? Object.values(json.errors).join(', ')
      : json.message || 'Lỗi khi cập nhật chỉ tiêu'
    throw new Error(errorMsg)
  }
  return json.data
}

/**
 * Xoá chỉ tiêu KPI.
 */
export async function xoaChiTieu(id) {
  const res = await fetch(`/api/kpi-targets/${id}`, {
    method: 'DELETE',
  })
  const json = await res.json()
  if (!res.ok) {
    throw new Error(json.message || 'Lỗi khi xoá chỉ tiêu')
  }
  return json.data
}
