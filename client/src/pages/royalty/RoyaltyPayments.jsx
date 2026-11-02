import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getRoyaltyPayments, getRoyaltyPeriods, recordRoyaltyPayment } from '../../api/royaltyApi.js'
import { formatMoney } from '../../utils/formatMoney.js'
import { formatDate } from '../../utils/formatDate.js'
import { PERIOD_STATUS_COLORS, periodStatusLabel } from '../../utils/royaltyLabels.js'
import '../../styles/Royalty.css'

// Chi ky da khoa (LOCKED) hoac da chi xong (PAID) moi co phieu chi (UC-53, UC-54)
const PAYABLE_STATUSES = ['LOCKED', 'PAID']

/**
 * UC-54 - "Chi tra nhuan but" (Ke toan).
 * Chon 1 ky da khoa -> thay phieu chi cua tung nguoi (tong tien, no ky truoc, tien thuc chi, no chuyen ky sau).
 * Bam "Ghi nhan da chi" tung phieu; phieu cuoi cung chi xong thi ky tu sang PAID (BR-RY-07).
 *
 * MO TRANG NAY TAI: /royalty-payments (co the kem ?periodId=)
 *
 * MUC LUC (so dong - ten - tac dung)
 *    9  PAYABLE_STATUSES                hang so
 *   30  RoyaltyPayments()               xem tom tat dau file
 *   39    useEffect                     tai danh sach ky da khoa khi mo trang, chon san ky trong ?periodId
 *   58    useEffect                     doi ky -> tai phieu chi cua ky do
 *   73    handlePay()                   ghi nhan da chi 1 phieu, tai lai phieu va trang thai ky
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  116  o chon [Ky]                     doi ?periodId -> tai phieu chi cua ky moi
 *  136  link [Xem bang ke]              mo trang /royalty-periods/{periodId} (RoyaltyPeriodDetail)
 *  175  [Ghi nhan da chi]               handlePay() -> hoi xac nhan; goi POST /api/royalty-payments/{id}/pay
 */
export default function RoyaltyPayments() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [periods, setPeriods] = useState(null)       // null = dang tai
  const [payments, setPayments] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [payError, setPayError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [payingId, setPayingId] = useState(null)     // id phieu dang ghi nhan, de khoa nut

  useEffect(() => {
    let isMounted = true
    getRoyaltyPeriods()
      .then((data) => {
        if (!isMounted) return
        const payable = data.filter((period) => PAYABLE_STATUSES.includes(period.status))
        setPeriods(payable)
        // Chua chon ky nao thi chon san ky da khoa moi nhat (danh sach da xep moi nhat truoc)
        if (!searchParams.get('periodId') && payable.length > 0) {
          setSearchParams({ periodId: String(payable[0].id) }, { replace: true })
        }
      })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
    return () => { isMounted = false }
  }, [])   // eslint-disable-line react-hooks/exhaustive-deps -- chi tai 1 lan khi mo trang

  const periodId = searchParams.get('periodId')

  useEffect(() => {
    if (!periodId) return
    let isMounted = true
    setPayments(null)
    getRoyaltyPayments(periodId)
      .then((data) => { if (isMounted) setPayments(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
    return () => { isMounted = false }
  }, [periodId])

  const selectedPeriod = periods?.find((period) => String(period.id) === periodId)

  async function handlePay(payment) {
    const confirmed = window.confirm(
      `Xác nhận đã chi ${formatMoney(payment.paidAmount)} cho ${payment.fullName || `người dùng #${payment.userId}`}?`)
    if (!confirmed) {
      return
    }
    setPayingId(payment.id)
    setPayError('')
    setSuccessMessage('')
    try {
      await recordRoyaltyPayment(payment.id)
      // Tai lai phieu chi va danh sach ky: phieu cuoi chi xong thi ky doi sang PAID
      const [updatedPayments, allPeriods] = await Promise.all([getRoyaltyPayments(periodId), getRoyaltyPeriods()])
      setPayments(updatedPayments)
      setPeriods(allPeriods.filter((period) => PAYABLE_STATUSES.includes(period.status)))
      const periodNowPaid = allPeriods.some((period) => String(period.id) === periodId && period.status === 'PAID')
      setSuccessMessage(periodNowPaid
        ? 'Đã chi xong toàn bộ phiếu. Kỳ đã chuyển sang "Đã chi trả" và phóng viên đã được báo.'
        : `Đã ghi nhận chi cho ${payment.fullName || `người dùng #${payment.userId}`}.`)
    } catch (err) {
      setPayError(err.message)
    } finally {
      setPayingId(null)
    }
  }

  const unpaidCount = payments ? payments.filter((payment) => payment.status === 'UNPAID').length : 0

  return (
    <div>
      <div className="royalty__header">
        <h1 className="royalty__title">Chi trả nhuận bút</h1>
        <p className="royalty__subtitle">
          Chọn kỳ đã khoá, chi cho từng người rồi bấm ghi nhận. Chi xong mọi phiếu thì kỳ tự chuyển sang “Đã chi trả”.
        </p>
      </div>

      {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
      {!periods && !loadError && <div className="royalty__card"><p className="royalty__empty">Đang tải…</p></div>}
      {periods && periods.length === 0 && (
        <div className="royalty__card">
          <p className="royalty__empty">Chưa có kỳ nào được khoá. Khoá kỳ ở trang “Kỳ nhuận bút” để tạo phiếu chi.</p>
        </div>
      )}

      {periods && periods.length > 0 && (
        <div className="royalty__card royalty__filters">
          <label className="royalty__field">
            Kỳ
            <select className="royalty__input" value={periodId || ''}
                    onChange={(event) => { setSuccessMessage(''); setPayError(''); setSearchParams({ periodId: event.target.value }) }}>
              {periods.map((period) => (
                <option key={period.id} value={period.id}>
                  Tháng {period.periodMonth}/{period.periodYear} - {periodStatusLabel(period.status)}
                </option>
              ))}
            </select>
          </label>
          {selectedPeriod && (
            <>
              <span className="royalty__status">
                <span className={`cham cham--${PERIOD_STATUS_COLORS[selectedPeriod.status]}`} aria-hidden="true" />
                {periodStatusLabel(selectedPeriod.status)}
              </span>
              <span className="royalty__muted">Tổng tiền kỳ: {formatMoney(selectedPeriod.totalAmount)}</span>
              <span className="royalty__muted">Còn {unpaidCount} phiếu chưa chi</span>
              <Link to={`/royalty-periods/${selectedPeriod.id}`} className="royalty__link">Xem bảng kê</Link>
            </>
          )}
        </div>
      )}

      {payError && <p className="thong-bao-loi" role="alert">{payError}</p>}
      {successMessage && <p className="thong-bao-thanh-cong" role="status">{successMessage}</p>}

      {periodId && periods && periods.length > 0 && (
        <div className="royalty__card">
          <h2 className="royalty__card-title">Phiếu chi từng người</h2>
          {!payments && <p className="royalty__empty">Đang tải…</p>}
          {payments && payments.length === 0 && <p className="royalty__empty">Kỳ này không có phiếu chi nào.</p>}
          {payments && payments.length > 0 && (
            <div className="royalty__scroll">
              <table className="bang-fm">
                <thead>
                  <tr>
                    <th>Người nhận</th>
                    <th className="royalty__number">Tổng trong kỳ</th>
                    <th className="royalty__number">Nợ kỳ trước</th>
                    <th className="royalty__number">Thực chi</th>
                    <th className="royalty__number">Nợ chuyển kỳ sau</th>
                    <th>Trạng thái</th>
                    <th>Chi lúc</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {payments.map((payment) => (
                    <tr key={payment.id}>
                      <td>{payment.fullName || `Người dùng #${payment.userId}`}</td>
                      <td className="royalty__number">{formatMoney(payment.grossAmount)}</td>
                      <td className="royalty__number">{formatMoney(payment.debtIn)}</td>
                      <td className="royalty__number royalty__strong">{formatMoney(payment.paidAmount)}</td>
                      <td className="royalty__number">{formatMoney(payment.debtOut)}</td>
                      <td className="royalty__nowrap">{payment.status === 'PAID' ? 'Đã chi' : 'Chưa chi'}</td>
                      <td className="royalty__date">{formatDate(payment.paidAt)}</td>
                      <td className="royalty__actions">
                        {payment.status === 'UNPAID' && (
                          <button type="button" className="nut-chinh-fm" disabled={payingId !== null}
                                  onClick={() => handlePay(payment)}>
                            {payingId === payment.id ? 'Đang ghi nhận…' : 'Ghi nhận đã chi'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
