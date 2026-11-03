/**
 * So lieu trang chu Ke toan (BF-04), ghep tu cac API THAT cua luong royalty
 * (khong can API rieng cho trang chu):
 *   - getRoyaltyPeriods():      cac ky nhuan but (UC-49)
 *   - getRoyaltyPeriodDetail(): so nguoi nhan cua ky cho duyet (UC-51)
 *   - getRoyaltyPayments():     phieu chi cua ky da khoa (UC-54)
 * Ten field tra ve giu nhu ban du lieu gia truoc day, nen AccountantHome.jsx dung tiep duoc.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  layTrangChuKeToan()             So lieu trang chu Ke toan: - periodStatusCounts: so ky nhuan but NAM NAY theo royalty_periods.status - period…
 *   46  demKyTheoTrangThai()            So ky NAM NAY theo royalty_periods.status, dang [{ status, count }]
 *   61  layKyChoDuyet()                 Ky CLOSED (Thu ky da gui) kem so nguoi nhan tien - UC-51
 *   73  layKyChoChi()                   Ky LOCKED kem so nguoi da chi / chua chi va tien con phai chi - UC-54
 */
import { getRoyaltyPayments, getRoyaltyPeriodDetail, getRoyaltyPeriods } from './royaltyApi.js'

/**
 * So lieu trang chu Ke toan:
 * - periodStatusCounts: so ky nhuan but NAM NAY theo royalty_periods.status
 * - periodsToReview: ky CLOSED cho Ke toan duyet bang ke (UC-51), kem personCount / totalAmount
 * - periodsToPay: ky LOCKED con nguoi chua chi (royalty_payments.status = UNPAID) - UC-54
 */
export async function layTrangChuKeToan() {
  const cacKy = await getRoyaltyPeriods()
  const kyChoDuyet = []
  const kyDaKhoa = []
  for (const ky of cacKy) {
    if (ky.status === 'CLOSED') kyChoDuyet.push(ky)
    if (ky.status === 'LOCKED') kyDaKhoa.push(ky)
  }

  // So ky it (12 ky/nam) nen goi chi tiet tung ky van nhanh
  const [periodsToReview, periodsToPay] = await Promise.all([
    Promise.all(kyChoDuyet.map(layKyChoDuyet)),
    Promise.all(kyDaKhoa.map(layKyChoChi)),
  ])

  return {
    periodStatusCounts: demKyTheoTrangThai(cacKy),
    periodsToReview,
    periodsToPay,
  }
}

/** So ky NAM NAY theo royalty_periods.status, dang [{ status, count }]. */
function demKyTheoTrangThai(cacKy) {
  const namNay = new Date().getFullYear()
  const soKy = {}
  for (const ky of cacKy) {
    if (ky.periodYear !== namNay) continue
    soKy[ky.status] = (soKy[ky.status] || 0) + 1
  }
  const ketQua = []
  for (const status of Object.keys(soKy)) {
    ketQua.push({ status, count: soKy[status] })
  }
  return ketQua
}

/** Ky CLOSED (Thu ky da gui) kem so nguoi nhan tien - UC-51. */
async function layKyChoDuyet(ky) {
  const chiTiet = await getRoyaltyPeriodDetail(ky.id)
  return {
    periodId: ky.id,
    periodYear: ky.periodYear,
    periodMonth: ky.periodMonth,
    personCount: chiTiet.people.length,
    totalAmount: ky.totalAmount,
  }
}

/** Ky LOCKED kem so nguoi da chi / chua chi va tien con phai chi - UC-54. */
async function layKyChoChi(ky) {
  const cacPhieu = await getRoyaltyPayments(ky.id)
  let soDaChi = 0
  let tienChuaChi = 0
  for (const phieu of cacPhieu) {
    if (phieu.status === 'PAID') {
      soDaChi++
    } else {
      tienChuaChi += Number(phieu.paidAmount)
    }
  }
  return {
    periodId: ky.id,
    periodYear: ky.periodYear,
    periodMonth: ky.periodMonth,
    personCount: cacPhieu.length,
    paidCount: soDaChi,
    unpaidAmount: tienChuaChi,
  }
}
