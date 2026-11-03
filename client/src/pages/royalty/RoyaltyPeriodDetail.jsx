import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  closeRoyaltyPeriod, getRoyaltyPeriodDetail, lockRoyaltyPeriod, returnRoyaltyPeriod,
} from '../../api/royaltyApi.js'
import { formatMoney } from '../../utils/formatMoney.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import { PERIOD_STATUS_COLORS, natureLabel, periodStatusLabel } from '../../utils/royaltyLabels.js'
import '../../styles/Royalty.css'

const RETURN_NOTE_MAX = 1000   // = cot royalty_periods.return_note NVARCHAR(1000)

/**
 * UC-50, 51, 52, 53 - "Bang ke ky nhuan but" (Thu ky va Ke toan dung chung 1 trang).
 * Tren: tong tien tung nguoi (Ke toan can de lam phieu chi). Duoi: cac bai da cham trong ky.
 * Thu ky: ky OPEN thi chot ky (UC-50) va cham lai bai trong ky (nhom chot 27/10/2026).
 * Ke toan: ky CLOSED thi tra ky kem ly do (UC-52) hoac khoa ky de tao phieu chi (UC-53) (BR-RY-07).
 * Ky bi Ke toan tra ve thi hien ly do de Thu ky sua (UC-50 A1).
 *
 * MO TRANG NAY TAI: /royalty-periods/:id
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  RETURN_NOTE_MAX                 hang so
 *   44  RoyaltyPeriodDetail()           xem tom tat dau file
 *   52    useEffect                     tai du lieu khi mo trang
 *   60    handleClose()                 xu ly close
 *   82    handleAccountantDone()        Ke toan tra / khoa ky xong: tai lai bang ke, hien thong bao
 *  164  AccountantReviewCard()          UC-51..53: Ke toan kiem tra ky CLOSED roi tra lai (kem ly do) hoac khoa ky
 *  169    handleReturn()                xu ly return
 *  186    handleLock()                  xu ly lock
 *  231  SummaryItem()                   summary item
 *  241  PeopleCard()                    Tong tien tung nguoi trong ky (UC-50 "total per person")
 *  277  ArticlesCard()                  Cac bai da cham trong ky: he so, tien bai, tien anh
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   98  link [← Ky nhuan but]           mo trang /royalty-periods (RoyaltyPeriods)
 *  136  [Dang chot… / Chot ky va gui Ke toan]  handleClose() -> hoi xac nhan; goi POST /api/royalty-periods/{periodId}/close, GET /api/royalty-periods/{periodId}
 *  148  link [Xem phieu chi / Ghi nhan chi tra]  mo trang /royalty-payments?periodId={period.id} (RoyaltyPayments)
 *  219  [Tra ky ve Thu ky]              handleReturn() -> goi POST /api/royalty-periods/{periodId}/return; bao len component cha (onDone)
 *  222  [Dang xu ly… / Khoa ky va tao phieu chi]  handleLock() -> hoi xac nhan; goi POST /api/royalty-periods/{periodId}/lock; bao len component cha (onDone)
 *  311  link [Cham lai]                 mo trang /royalty-grading/{article.articleId}?chamLai=1 (RoyaltyGrading)
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

  // Ke toan tra / khoa ky xong: tai lai bang ke, hien thong bao
  async function handleAccountantDone(message) {
    const updated = await getRoyaltyPeriodDetail(id)
    setPeriod(updated)
    setSuccessMessage(message)
  }

  const isSecretary = Boolean(nguoiDung?.vaiTro?.includes('EDITORIAL_SECRETARY'))
  const isAccountant = Boolean(nguoiDung?.vaiTro?.includes('ACCOUNTANT'))
  const isOpen = Boolean(period && period.status === 'OPEN')
  const canClose = isSecretary && isOpen
  const canRegrade = isSecretary && isOpen                                    // cham lai chi khi ky con mo
  const canReview = isAccountant && period && period.status === 'CLOSED'    // UC-52, 53 chi voi ky da chot

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
          {/* UC-50 A1: Ke toan tra ky ve thi Thu ky thay ly do (chi can khi ky dang mo lai de sua) */}
          {isOpen && period.returnNote && (
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
          {canReview && <AccountantReviewCard period={period} onDone={handleAccountantDone} />}
          {/* UC-54: ky da khoa thi Ke toan sang trang chi tra de ghi nhan chi tung phieu */}
          {isAccountant && ['LOCKED', 'PAID'].includes(period.status) && (
            <div className="royalty__card">
              <h2 className="royalty__card-title">Chi trả</h2>
              <p className="royalty__hint">Kỳ đã khoá, phiếu chi đã được tạo cho từng người.</p>
              <Link to={`/royalty-payments?periodId=${period.id}`} className="nut-chinh-fm">
                {period.status === 'PAID' ? 'Xem phiếu chi' : 'Ghi nhận chi trả'}
              </Link>
            </div>
          )}
          {successMessage && <p className="thong-bao-thanh-cong" role="status">{successMessage}</p>}

          <PeopleCard people={period.people} />
          <ArticlesCard articles={period.articles} canRegrade={canRegrade} />
        </>
      )}
    </div>
  )
}

/** UC-51..53: Ke toan kiem tra ky CLOSED roi tra lai (kem ly do) hoac khoa ky. */
function AccountantReviewCard({ period, onDone }) {
  const [note, setNote] = useState('')
  const [isWorking, setIsWorking] = useState(false)
  const [actionError, setActionError] = useState('')

  async function handleReturn() {
    // UC-52: ly do bat buoc (CHECK ck_royalty_periods_returned); server cung kiem lai
    if (note.trim() === '') {
      setActionError('Vui lòng nhập lý do trả lại kỳ.')
      return
    }
    setIsWorking(true)
    setActionError('')
    try {
      await returnRoyaltyPeriod(period.id, note.trim())
      await onDone(`Đã trả kỳ ${period.periodMonth}/${period.periodYear} về cho Thư ký sửa.`)
    } catch (err) {
      setActionError(err.message)
      setIsWorking(false)
    }
  }

  async function handleLock() {
    const confirmed = window.confirm(
      `Khoá kỳ tháng ${period.periodMonth}/${period.periodYear}? Sau khi khoá, số liệu của kỳ không sửa được nữa và hệ thống tạo phiếu chi cho từng người.`)
    if (!confirmed) {
      return
    }
    setIsWorking(true)
    setActionError('')
    try {
      await lockRoyaltyPeriod(period.id)
      await onDone(`Đã khoá kỳ ${period.periodMonth}/${period.periodYear} và tạo phiếu chi cho từng người.`)
    } catch (err) {
      setActionError(err.message)
      setIsWorking(false)
    }
  }

  return (
    <div className="royalty__card">
      <h2 className="royalty__card-title">Kiểm tra kỳ</h2>
      <p className="royalty__hint">
        Đối chiếu tiền từng người bên dưới. Sai thì trả kỳ về cho Thư ký kèm lý do; đúng thì khoá kỳ để tạo phiếu chi.
      </p>
      {actionError && <p className="thong-bao-loi" role="alert">{actionError}</p>}
      <div className="royalty__form royalty__review">
        <label className="royalty__field royalty__review-note">
          Lý do trả lại (bắt buộc khi trả kỳ)
          <textarea className="royalty__input royalty__input--full royalty__textarea" rows={3}
                    maxLength={RETURN_NOTE_MAX} value={note} disabled={isWorking}
                    onChange={(event) => setNote(event.target.value)}
                    placeholder="Vd: Bài 5 sai tỷ lệ đồng tác giả" />
        </label>
        <div className="royalty__actions">
          <button type="button" className="nut-phu" onClick={handleReturn} disabled={isWorking}>
            Trả kỳ về Thư ký
          </button>
          <button type="button" className="nut-chinh-fm" onClick={handleLock} disabled={isWorking}>
            {isWorking ? 'Đang xử lý…' : 'Khoá kỳ và tạo phiếu chi'}
          </button>
        </div>
      </div>
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

/** Cac bai da cham trong ky: he so, tien bai, tien anh. Ky OPEN thi Thu ky cham lai duoc tung bai. */
function ArticlesCard({ articles, canRegrade }) {
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
                {canRegrade && <th>Thao tác</th>}
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
                  {canRegrade && (
                    <td className="royalty__nowrap">
                      <Link to={`/royalty-grading/${article.articleId}?chamLai=1`} className="royalty__link">
                        Chấm lại
                      </Link>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
