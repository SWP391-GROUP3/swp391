/**
 * ===========================================================================
 * DU LIEU GIA cho trang chu Thu ky toa soan - CHUA CO API THAT.
 * ===========================================================================
 * Ham tra ve Promise giong goiApi va tra dung phan "data" cua ApiResponse.
 * Khi backend BF-04 xong chi can doi ruot ham thanh goiApi('/api/...'),
 * component goi ham KHONG phai sua.
 * Ten field theo DTO tieng Anh (camelCase), trang thai dung chu trong CHECK
 * cua database/schema.sql.
 *
 * MUC LUC (so dong)
 *   27  layTrangChuThuKy             - Tat ca so lieu trang chu cua Thu ky:
 */
import { giaLapGoiApi, cachHomNay } from './duLieuGia.js'

/**
 * Tat ca so lieu trang chu cua Thu ky:
 * - counts.pendingGrading: bai PUBLISHED chua co royalty_gradings - UC-46
 * - counts.nearDeadline:   trong so do, bai sap toi han chi nhuan but
 * - pendingArticles: vai bai cho cham co han gan nhat (paymentDeadline tang dan) - UC-46
 *   TODO(BF-04): han chi tinh tu ngay dang (UC-33) nhung data.sql chua co tham so
 *   so ngay -> can them vao system_parameters (PR [DB]).
 * - openPeriod: ky dang OPEN (royalty_periods). returnNote khac null = Ke toan
 *   da tra ky ve (UC-52), Thu ky phai sua roi gui lai (UC-50).
 *   gradedCount / totalAmount: so bai va tong tien cac royalty_gradings cua ky.
 */
export async function layTrangChuThuKy() {
  const homNay = new Date()
  return giaLapGoiApi({
    counts: {
      pendingGrading: 7,
      nearDeadline: 2,
    },
    pendingArticles: [
      { articleId: 31, title: 'Giá vàng lập đỉnh mới trong tuần', sectionName: 'Kinh tế', publishedAt: cachHomNay(-12), paymentDeadline: cachHomNay(1) },
      { articleId: 33, title: 'Khánh thành cầu vượt nút giao Mai Dịch', sectionName: 'Thời sự', publishedAt: cachHomNay(-10), paymentDeadline: cachHomNay(3) },
      { articleId: 36, title: 'Doanh nghiệp nhỏ xoay xở mùa cao điểm', sectionName: 'Kinh tế', publishedAt: cachHomNay(-4), paymentDeadline: cachHomNay(9) },
    ],
    openPeriod: {
      periodId: 10,
      periodYear: homNay.getFullYear(),
      periodMonth: homNay.getMonth() + 1,
      status: 'OPEN',
      returnNote: null,
      gradedCount: 23,
      totalAmount: 41500000,
    },
  })
}
