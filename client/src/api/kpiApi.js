/**
 * API client cho luồng KPI (BF-05).
 * Gọi qua goiApi(): tự gắn token đăng nhập, tự báo lỗi tiếng Việt (AGENTS.md mục 4).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  layDinhMucChuan()               Lay danh sach dinh muc chuan theo ngach phong vien (UC-58) [GET /api/kpi-targets/benchmarks]
 *   28  capNhatDinhMucChuan()           Cap nhat danh sach dinh muc chuan theo ngach phong vien (UC-58) [PUT /api/kpi-targets/benchmarks]
 *   35  layChiTieuTheoThang()           Lay danh sach chi tieu theo nam va thang (UC-59) [GET /api/kpi-targets?year={nam}&month={thang}]
 *   43  taoChiTieu()                    Giao chi tieu thang moi cho phong vien (UC-59) [POST /api/kpi-targets]
 *   50  capNhatChiTieu()                Dieu chinh chi tieu da giao (UC-59 nhanh A2) [PUT /api/kpi-targets/{id}]
 *   57  xoaChiTieu()                    Xoa chi tieu KPI. [DELETE /api/kpi-targets/{id}]
 *   64  layBangKpiBan()                 Lay du lieu bang KPI toan ban theo nam va thang (UC-61) [GET /api/kpi-snapshots/dashboard?year={nam}&month={thang}]
 *   71  tongHopKpiThang()               Kich hoat job tong hop so lieu KPI tu dong cho ky (Automated Aggregat… [POST /api/kpi-snapshots/aggregate?year={nam}&month={thang}]
 *   78  layChiTietKpiPhongVien()        Lay chi tiet KPI cua mot phong vien theo chu ky thang (UC-62, UC-60) [GET /api/kpi-snapshots/reporters/{reporterId}/details?year={nam}&month={thang}]
 *   85  layBaoCaoTheLoai()              Lay bao cao muc do quan tam the loai & chi phi nhuan but (UC-64) [GET /api/kpi-reports/genres?year={nam}&month={thang}]
 *   97  layBaoCaoSuKienXuHuong()        Lay bao cao su kien xu huong va phat hien tang dot bien (UC-65) [GET /api/kpi-reports/trending-events?anchorDate={ngay}]
 *  108  chotKpiThang()                  Chot ky KPI thang va luu snapshot bat bien (UC-66) [POST /api/kpi-snapshots/finalize]
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

/**
 * Lấy báo cáo mức độ quan tâm thể loại & chi phí nhuận bút (UC-64).
 */
export async function layBaoCaoTheLoai(nam, thang) {
  let url = '/api/kpi-reports/genres'
  const params = []
  if (nam) params.push(`year=${nam}`)
  if (thang) params.push(`month=${thang}`)
  if (params.length > 0) {
    url += `?${params.join('&')}`
  }
  return goiApi(url)
}

/**
 * Lấy báo cáo sự kiện xu hướng và phát hiện tăng đột biến (UC-65).
 */
export async function layBaoCaoSuKienXuHuong(anchorDate) {
  let url = '/api/kpi-reports/trending-events'
  if (anchorDate) {
    url += `?anchorDate=${anchorDate}`
  }
  return goiApi(url)
}

/**
 * Chốt kỳ KPI tháng và lưu snapshot bất biến (UC-66).
 * body: { year, month, reporterNotes }
 */
export async function chotKpiThang(duLieu) {
  return goiApi('/api/kpi-snapshots/finalize', { method: 'POST', body: duLieu })
}



