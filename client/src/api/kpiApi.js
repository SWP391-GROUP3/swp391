/**
 * API client cho luồng KPI (BF-05).
 * Gọi qua goiApi(): tự gắn token đăng nhập, tự báo lỗi tiếng Việt (AGENTS.md mục 4).
 */
import { goiApi } from './goiApi.js'

/**
 * Lấy danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
 */
export async function layDinhMucChuan() {
  return goiApi('/api/kpi-targets/benchmarks')
}

/**
 * Cập nhật danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
 */
export async function capNhatDinhMucChuan(danhSach) {
  return goiApi('/api/kpi-targets/benchmarks', { method: 'PUT', body: danhSach })
}

/**
 * Lấy danh sách chỉ tiêu theo năm và tháng (UC-59).
 */
export async function layChiTieuTheoThang(nam, thang) {
  return goiApi(`/api/kpi-targets?year=${nam}&month=${thang}`)
}

/**
 * Giao chỉ tiêu tháng mới cho phóng viên (UC-59).
 * Người giao (set_by) server tự lấy từ token, không cần gửi kèm.
 */
export async function taoChiTieu(duLieu) {
  return goiApi('/api/kpi-targets', { method: 'POST', body: duLieu })
}

/**
 * Điều chỉnh chỉ tiêu đã giao (UC-59 nhánh A2).
 */
export async function capNhatChiTieu(id, duLieu) {
  return goiApi(`/api/kpi-targets/${id}`, { method: 'PUT', body: duLieu })
}

/**
 * Xoá chỉ tiêu KPI.
 */
export async function xoaChiTieu(id) {
  return goiApi(`/api/kpi-targets/${id}`, { method: 'DELETE' })
}
