import { useEffect, useState } from 'react'
import { getMyRoyalties } from '../../api/royaltyApi.js'
import { formatMoney } from '../../utils/formatMoney.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import { PERIOD_STATUS_COLORS, periodStatusLabel } from '../../utils/royaltyLabels.js'
import '../../styles/Royalty.css'

// royalty_lines.line_type -> chu hien thi (xem comment bang royalty_lines trong schema.sql)
const LINE_TYPE_LABELS = {
  ARTICLE:    'Tiền bài',
  MEDIA:      'Ảnh / video',
  ADJUSTMENT: 'Điều chỉnh',
}

/**
 * UC-57 - "Nhuan but cua toi" (Phong vien).
 * Moi ky 1 the: cac dong tien cua minh (tien bai theo ty le dong tac gia, anh/video, dieu chinh),
 * tong tien trong ky, va phieu chi khi ky da khoa (no ky truoc, thuc nhan, da chi chua).
 *
 * MO TRANG NAY TAI: /my-royalties
 *
 * MUC LUC (so dong - ten - tac dung)
 *   10  LINE_TYPE_LABELS                royalty_lines.line_type -> chu hien thi (xem comment bang royalty_lines trong schema.sql)
 *   30  MyRoyalties()                   xem tom tat dau file
 *   34    useEffect                     tai du lieu khi mo trang
 *   62  PeriodCard()                    1 ky: phieu chi (neu co) + bang dong tien
 *  104  PaymentSummary()                Tom tat phieu chi cua ky da khoa: no ky truoc tru vao, thuc nhan, da chi chua (GBR-12)
 */
export default function MyRoyalties() {
  const [periods, setPeriods] = useState(null)   // null = dang tai
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    let isMounted = true
    getMyRoyalties()
      .then((data) => { if (isMounted) setPeriods(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
    return () => { isMounted = false }
  }, [])

  return (
    <div>
      <div className="royalty__header">
        <h1 className="royalty__title">Nhuận bút của tôi</h1>
        <p className="royalty__subtitle">
          Tiền bài chia theo tỷ lệ đồng tác giả, làm tròn xuống 1.000đ. Kỳ đã khoá thì có phiếu chi, kế toán chi xong sẽ báo cho bạn.
        </p>
      </div>

      {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
      {!periods && !loadError && <div className="royalty__card"><p className="royalty__empty">Đang tải…</p></div>}
      {periods && periods.length === 0 && (
        <div className="royalty__card"><p className="royalty__empty">Bạn chưa có nhuận bút nào.</p></div>
      )}
      {periods && periods.map((period) => <PeriodCard key={period.periodId} period={period} />)}
    </div>
  )
}

/** 1 ky: phieu chi (neu co) + bang dong tien. */
function PeriodCard({ period }) {
  return (
    <div className="royalty__card">
      <h2 className="royalty__card-title">
        Tháng {period.periodMonth}/{period.periodYear}{' '}
        <span className="royalty__status">
          <span className={`cham cham--${PERIOD_STATUS_COLORS[period.periodStatus]}`} aria-hidden="true" />
          {periodStatusLabel(period.periodStatus)}
        </span>
      </h2>
      <p className="royalty__muted">Tổng trong kỳ: <span className="royalty__strong">{formatMoney(period.totalAmount)}</span></p>
      {period.payment && <PaymentSummary payment={period.payment} />}

      {period.lines.length > 0 && (
        <div className="royalty__scroll">
          <table className="bang-fm">
            <thead>
              <tr>
                <th>Bài</th>
                <th>Loại</th>
                <th className="royalty__number">Tỷ lệ của bạn</th>
                <th className="royalty__number">Số tiền</th>
              </tr>
            </thead>
            <tbody>
              {period.lines.map((line) => (
                <tr key={line.lineId}>
                  <td className="royalty__title-cell">{line.articleTitle || `Bài #${line.articleId}`}</td>
                  <td className="royalty__nowrap">{LINE_TYPE_LABELS[line.lineType] || line.lineType}</td>
                  <td className="royalty__number">{line.sharePercent ? `${formatNumber(line.sharePercent)}%` : '—'}</td>
                  <td className="royalty__number royalty__strong">{formatMoney(line.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

/** Tom tat phieu chi cua ky da khoa: no ky truoc tru vao, thuc nhan, da chi chua (GBR-12). */
function PaymentSummary({ payment }) {
  return (
    <div className="royalty__summary">
      <div className="royalty__summary-item">
        <span className="royalty__muted">Nợ kỳ trước</span>
        <span className="royalty__summary-value">{formatMoney(payment.debtIn)}</span>
      </div>
      <div className="royalty__summary-item">
        <span className="royalty__muted">Thực nhận</span>
        <span className="royalty__summary-value royalty__strong">{formatMoney(payment.paidAmount)}</span>
      </div>
      <div className="royalty__summary-item">
        <span className="royalty__muted">Nợ chuyển kỳ sau</span>
        <span className="royalty__summary-value">{formatMoney(payment.debtOut)}</span>
      </div>
      <div className="royalty__summary-item">
        <span className="royalty__muted">Phiếu chi</span>
        <span className="royalty__summary-value">
          {payment.status === 'PAID' ? `Đã chi ngày ${formatDate(payment.paidAt)}` : 'Chờ kế toán chi'}
        </span>
      </div>
    </div>
  )
}
