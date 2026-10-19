/**
 * API client cho luồng KPI (BF-05).
 * Gọi qua goiApi(): tự gắn token đăng nhập, tự báo lỗi tiếng Việt (AGENTS.md mục 4).
 *
 * MUC LUC (so dong)
 *   21  layDinhMucChuan              - Lấy danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
 *   28  capNhatDinhMucChuan          - Cập nhật danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
 *   35  layChiTieuTheoThang          - Lấy danh sách chỉ tiêu theo năm và tháng (UC-59).
 *   43  taoChiTieu                   - Giao chỉ tiêu tháng mới cho phóng viên (UC-59).
 *   50  capNhatChiTieu               - Điều chỉnh chỉ tiêu đã giao (UC-59 nhánh A2).
 *   57  xoaChiTieu                   - Xoá chỉ tiêu KPI.
 *   64  layBangKpiBan                - Lấy dữ liệu bảng KPI toàn ban theo năm và tháng (UC-61).
 *   71  tongHopKpiThang              - Kích hoạt job tổng hợp số liệu KPI tự động cho kỳ (Automated Aggregatio…
 *   78  layChiTietKpiPhongVien       - Lấy chi tiết KPI của một phóng viên theo chu kỳ tháng (UC-62, UC-60).
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

/**
 * Lấy dữ liệu bảng KPI toàn ban theo năm và tháng (UC-61).
 */
export async function layBangKpiBan(nam, thang) {
  return goiApi(`/api/kpi-snapshots/dashboard?year=${nam}&month=${thang}`)
}

/**
 * Kích hoạt job tổng hợp số liệu KPI tự động cho kỳ (Automated Aggregation Job).
 */
export async function tongHopKpiThang(nam, thang) {
  return goiApi(`/api/kpi-snapshots/aggregate?year=${nam}&month=${thang}`, { method: 'POST' })
}

/**
 * Lấy chi tiết KPI của một phóng viên theo chu kỳ tháng (UC-62, UC-60).
 */
export async function layChiTietKpiPhongVien(reporterId, nam, thang) {
  return goiApi(`/api/kpi-snapshots/reporters/${reporterId}/details?year=${nam}&month=${thang}`)
}

