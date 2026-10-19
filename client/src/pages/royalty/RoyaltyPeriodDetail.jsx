import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { closeRoyaltyPeriod, getRoyaltyPeriodDetail } from '../../api/royaltyApi.js'
import { formatMoney } from '../../utils/formatMoney.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import { PERIOD_STATUS_COLORS, natureLabel, periodStatusLabel } from '../../utils/royaltyLabels.js'
import '../../styles/Royalty.css'

/**
 * UC-50 - "Bang ke ky nhuan but" (Thu ky va Ke toan dung chung 1 trang).
 * Tren: tong tien tung nguoi (Ke toan can de lam phieu chi). Duoi: cac bai da cham trong ky.
 * Chi Thu ky thay nut "Chot ky", va chi khi ky dang OPEN (BR-RY-07).
 * Ky bi Ke toan tra ve (UC-52) thi hien ly do de Thu ky sua (UC-50 A1).
 */
export default function RoyaltyPeriodDetail({ nguoiDung }) {
  const { id } = useParams()
  const [period, setPeriod] = useState(null)      // null = dang tai
  const [loadError, setLoadError] = useState('')
  const [isClosing, setIsClosing] = useState(false)
  const [closeError, setCloseError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  useEffect(() => {
    let isMounted = true
    getRoyaltyPeriodDetail(id)
      .then((data) => { if (isMounted) setPeriod(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
    return () => { isMounted = false }
  }, [id])

  async function handleClose() {
    const confirmed = window.confirm(
      `Chốt kỳ tháng ${period.periodMonth}/${period.periodYear} và gửi Kế toán? Sau khi chốt, bài chấm mới sẽ vào kỳ tháng sau.`)
    if (!confirmed) {
      return
    }
    setIsClosing(true)
    setCloseError('')
    try {
      await closeRoyaltyPeriod(id)
      // Tai lai bang ke de lay trang thai moi (CLOSED)
      const updated = await getRoyaltyPeriodDetail(id)
      setPeriod(updated)
      setSuccessMessage('Đã chốt kỳ và báo Kế toán. Hệ thống đã mở kỳ tháng sau để chấm tiếp.')
    } catch (err) {
      setCloseError(err.message)
    } finally {
      setIsClosing(false)
    }
  }

  const isSecretary = Boolean(nguoiDung?.vaiTro?.includes('EDITORIAL_SECRETARY'))
  const canClose = isSecretary && period && period.status === 'OPEN'

  return (
    <div>
      <div className="royalty__header">
        <Link to="/royalty-periods" className="royalty__back">← Kỳ nhuận bút</Link>
        <h1 className="royalty__title">
          Bảng kê kỳ nhuận bút{period && ` tháng ${period.periodMonth}/${period.periodYear}`}
        </h1>
        {period && (
          <p className="royalty__subtitle">
            <span className="royalty__status">
              <span className={`cham cham--${PERIOD_STATUS_COLORS[period.status]}`} aria-hidden="true" />
              {periodStatusLabel(period.status)}
            </span>
          </p>
        )}
      </div>

      {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
      {!period && !loadError && <div className="royalty__card"><p className="royalty__empty">Đang tải…</p></div>}

      {period && (
        <>
          {/* UC-50 A1: Ke toan tra ky ve thi Thu ky thay ly do */}
          {period.returnNote && (
            <p className="thong-bao-loi" role="alert">Kế toán đã trả kỳ về: {period.returnNote}</p>
          )}

          <div className="royalty__card royalty__summary">
            <SummaryItem label="Tổng tiền kỳ" value={formatMoney(period.totalAmount)} strong />
            <SummaryItem label="Số bài đã chấm" value={formatNumber(period.gradedArticleCount)} />
            <SummaryItem label="Số người nhận" value={formatNumber(period.people.length)} />
          </div>

          {canClose && (
            <div className="royalty__card">
              <h2 className="royalty__card-title">Chốt kỳ</h2>
              <p className="royalty__hint">
                Kiểm tra bảng kê rồi chốt để gửi Kế toán. Mọi bài đăng đến hết tháng {period.periodMonth}/{period.periodYear} phải chấm xong trước khi chốt.
              </p>
              {closeError && <p className="thong-bao-loi" role="alert">{closeError}</p>}
              <div className="royalty__form">
                <button type="button" className="nut-chinh-fm" onClick={handleClose} disabled={isClosing}>
                  {isClosing ? 'Đang chốt…' : 'Chốt kỳ và gửi Kế toán'}
                </button>
              </div>
            </div>
          )}
          {successMessage && <p className="thong-bao-thanh-cong" role="status">{successMessage}</p>}

          <PeopleCard people={period.people} />
          <ArticlesCard articles={period.articles} />
        </>
      )}
    </div>
  )
}

function SummaryItem({ label, value, strong }) {
  return (
    <div className="royalty__summary-item">
      <span className="royalty__muted">{label}</span>
      <span className={strong ? 'royalty__summary-value royalty__strong' : 'royalty__summary-value'}>{value}</span>
    </div>
  )
}

/** Tong tien tung nguoi trong ky (UC-50 "total per person"). */
function PeopleCard({ people }) {
  return (
    <div className="royalty__card">
      <h2 className="royalty__card-title">Tổng tiền từng người</h2>
      {people.length === 0 && <p className="royalty__empty">Kỳ chưa có dòng tiền nào.</p>}
      {people.length > 0 && (
        <div className="royalty__scroll">
          <table className="bang-fm">
            <thead>
              <tr>
                <th>Người nhận</th>
                <th className="royalty__number">Tiền bài</th>
                <th className="royalty__number">Ảnh / video</th>
                <th className="royalty__number">Điều chỉnh</th>
                <th className="royalty__number">Tổng</th>
              </tr>
            </thead>
            <tbody>
              {people.map((person) => (
                <tr key={person.userId}>
                  <td>{person.fullName || `Người dùng #${person.userId}`}</td>
                  <td className="royalty__number">{formatMoney(person.articleAmount)}</td>
                  <td className="royalty__number">{formatMoney(person.mediaAmount)}</td>
                  <td className="royalty__number">{formatMoney(person.adjustmentAmount)}</td>
                  <td className="royalty__number royalty__strong">{formatMoney(person.totalAmount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

/** Cac bai da cham trong ky: he so, tien bai, tien anh. */
function ArticlesCard({ articles }) {
  return (
    <div className="royalty__card">
      <h2 className="royalty__card-title">Bài đã chấm trong kỳ</h2>
      {articles.length === 0 && <p className="royalty__empty">Kỳ chưa có bài nào được chấm.</p>}
      {articles.length > 0 && (
        <div className="royalty__scroll">
          <table className="bang-fm">
            <thead>
              <tr>
                <th>Bài</th>
                <th>Tính chất</th>
                <th className="royalty__number">Điểm</th>
                <th className="royalty__number">Hệ số cuối</th>
                <th className="royalty__number">Tiền bài</th>
                <th className="royalty__number">Ảnh / video</th>
                <th className="royalty__number">Tổng</th>
                <th>Ngày chấm</th>
              </tr>
            </thead>
            <tbody>
              {articles.map((article) => (
                <tr key={article.gradingId}>
                  <td className="royalty__title-cell">{article.title || `Bài #${article.articleId}`}</td>
                  <td className="royalty__nowrap">{natureLabel(article.nature)}</td>
                  <td className="royalty__number">{formatNumber(article.scoreUsed)}</td>
                  <td className="royalty__number">{formatNumber(article.finalFactor)}</td>
                  <td className="royalty__number">{formatMoney(article.articleAmount)}</td>
                  <td className="royalty__number">{formatMoney(article.mediaAmount)}</td>
                  <td className="royalty__number royalty__strong">{formatMoney(article.totalAmount)}</td>
                  <td className="royalty__date">{formatDate(article.gradedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
