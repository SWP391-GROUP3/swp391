/**
 * ===========================================================================
 * DU LIEU GIA cho trang chu Ke toan - CHUA CO API THAT.
 * ===========================================================================
 * (Rieng don gia da co API that: royaltyApi.layDanhSachDonGia.)
 * Khi backend BF-04 xong chi can doi ruot ham thanh goiApi('/api/...'),
 * component goi ham KHONG phai sua. Trang thai dung chu trong CHECK cua schema.sql.
 */
import { giaLapGoiApi } from './duLieuGia.js'

/**
 * So lieu trang chu Ke toan:
 * - periodStatusCounts: so ky nhuan but NAM NAY theo royalty_periods.status
 * - periodsToReview: ky CLOSED (Thu ky da gui) cho Ke toan duyet bang ke - UC-51
 *   personCount / totalAmount: so nguoi va tong tien cac royalty_lines cua ky
 * - periodsToPay: ky LOCKED con nguoi chua chi (royalty_payments.status = UNPAID) - UC-54
 */
export async function layTrangChuKeToan() {
  const nam = new Date().getFullYear()
  return giaLapGoiApi({
    periodStatusCounts: [
      { status: 'OPEN',   count: 1 },
      { status: 'CLOSED', count: 1 },
      { status: 'LOCKED', count: 1 },
      { status: 'PAID',   count: 7 },
    ],
    periodsToReview: [
      { periodId: 9, periodYear: nam, periodMonth: 9, personCount: 34, totalAmount: 52300000 },
    ],
    periodsToPay: [
      { periodId: 8, periodYear: nam, periodMonth: 8, personCount: 12, paidCount: 9, unpaidAmount: 8400000 },
    ],
  })
}
