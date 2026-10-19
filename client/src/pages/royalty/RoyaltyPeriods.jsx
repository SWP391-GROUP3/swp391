import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getRoyaltyPeriods } from '../../api/royaltyApi.js'
import { formatMoney } from '../../utils/formatMoney.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import { PERIOD_STATUS_COLORS, periodStatusLabel } from '../../utils/royaltyLabels.js'
import '../../styles/Royalty.css'

/**
 * UC-49 - "Ky nhuan but" (Thu ky toa soan va Ke toan dung chung 1 trang).
 * Moi thang 1 ky: trang thai (BR-RY-07), so bai da cham, tong tien cac dong trong ky.
 * Bam "Xem bang ke" -> trang chi tiet ky (UC-50/51, lam o Iter 2).
 *
 * MUC LUC (so dong)
 *   19  RoyaltyPeriods               - UC-49 - "Ky nhuan but" (Thu ky toa soan va Ke toan dung chung 1 trang).
 *   23    useEffect                  - tai du lieu khi mo trang
 */
export default function RoyaltyPeriods() {
  const [periods, setPeriods] = useState(null)   // null = dang tai
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    let isMounted = true
    getRoyaltyPeriods()
      .then((data) => { if (isMounted) setPeriods(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
    return () => { isMounted = false }
  }, [])

  return (
    <div>
      <div className="royalty__header">
        <h1 className="royalty__title">Kỳ nhuận bút</h1>
        <p className="royalty__subtitle">
          Mỗi tháng một kỳ: Thư ký chấm và chốt kỳ, Kế toán duyệt, khoá kỳ rồi chi trả. Kỳ mới nhất xếp trước.
        </p>
      </div>

      <div className="royalty__card">
        {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
        {!periods && !loadError && <p className="royalty__empty">Đang tải…</p>}
        {periods && periods.length === 0 && <p className="royalty__empty">Chưa có kỳ nhuận bút nào.</p>}

        {periods && periods.length > 0 && (
          <div className="royalty__scroll">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th>Kỳ</th>
                  <th>Trạng thái</th>
                  <th className="royalty__number">Số bài đã chấm</th>
                  <th className="royalty__number">Tổng tiền</th>
                  <th>Khoá lúc</th>
                  <th>Chi xong lúc</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {periods.map((period) => (
                  <tr key={period.id}>
                    <td className="royalty__strong royalty__nowrap">Tháng {period.periodMonth}/{period.periodYear}</td>
                    <td>
                      <span className="royalty__status">
                        <span className={`cham cham--${PERIOD_STATUS_COLORS[period.status]}`} aria-hidden="true" />
                        {periodStatusLabel(period.status)}
                      </span>
                      {/* Ke toan tra ky ve (UC-52) thi hien ly do de Thu ky sua */}
                      {period.returnNote && <p className="royalty__note">Bị trả lại: {period.returnNote}</p>}
                    </td>
                    <td className="royalty__number">{formatNumber(period.gradedArticleCount)}</td>
                    <td className="royalty__number royalty__strong">{formatMoney(period.totalAmount)}</td>
                    <td className="royalty__date">{formatDate(period.lockedAt)}</td>
                    <td className="royalty__date">{formatDate(period.paidAt)}</td>
                    <td className="royalty__actions">
                      <Link to={`/royalty-periods/${period.id}`} className="royalty__link">Xem bảng kê</Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
