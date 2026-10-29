import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { completeRoyaltyGrading, previewRoyaltyGrading } from '../../api/royaltyApi.js'
import { formatMoney } from '../../utils/formatMoney.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import { mediaKindLabel, natureLabel } from '../../utils/royaltyLabels.js'
import '../../styles/Royalty.css'

const RECALC_REASON_MAX = 500   // = cot royalty_gradings.recalc_reason NVARCHAR(500)

/**
 * UC-47, UC-48 - "Cham nhuan but" 1 bai (Thu ky toa soan).
 * He so va tien do SERVER tinh (BR-RY-01: Thu ky khong sua he so); tinh chat bai lay theo
 * PV khai (BR-IN-03, Thu ky khong chon lai). Thu ky chi:
 *   1. xem anh/video nao duoc tinh tien (UC-48)
 *   2. bam Hoan tat -> luu phieu, bai vao ky dang mo (BR-RY-05)
 * Mo tu bang ke voi ?chamLai=1 la cham lai (nhom chot 27/10/2026): bat buoc ly do,
 * phieu moi vao cung ky voi phieu cu, chi duoc khi ky do con mo.
 *
 * MO TRANG NAY TAI: /royalty-grading/:articleId
 *
 * MUC LUC (so dong - ten - tac dung)
 *   10  RECALC_REASON_MAX               hang so
 *   38  RoyaltyGrading()                xem tom tat dau file
 *   50    useEffect                     Tinh truoc he so va tien khi mo trang (xem truoc, chua luu)
 *   59    handleComplete()              xu ly complete
 *  171  FormulaCard()                   Cac buoc cong thuc (BR-RY-01..03): server tinh, man hinh chi hien de Thu ky kiem tra
 *  198  AuthorCard()                    Chia tien bai cho cac tac gia (GBR-10): dong tac gia lam tron xuong, tac gia chinh nhan phan du
 *  224  MediaCard()                     UC-48: tung anh/video
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   97  link [{backLink.label}]         mo trang {backLink.to} (trang lay tu du lieu)
 *  148  link [Xem bang ke ky]           mo trang /royalty-periods/{grading.periodId} (RoyaltyPeriodDetail)
 *  149  link [Cham bai khac]            mo trang /royalty-grading (RoyaltyPendingList)
 *  154  [Dang luu… / Luu cham lai / Hoan tat cha…]  handleComplete() -> hoi xac nhan; goi POST /api/royalty-gradings
 */
export default function RoyaltyGrading() {
  const { articleId } = useParams()
  const [searchParams] = useSearchParams()
  const isRegrade = searchParams.get('chamLai') === '1'
  const [grading, setGrading] = useState(null)    // phieu tinh san: xem truoc, hoac da luu (co gradingId)
  const [recalcReason, setRecalcReason] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState('')

  // Tinh truoc he so va tien khi mo trang (xem truoc, chua luu)
  useEffect(() => {
    let isMounted = true
    previewRoyaltyGrading(articleId)
      .then((data) => { if (isMounted) setGrading(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
      .finally(() => { if (isMounted) setIsLoading(false) })
    return () => { isMounted = false }
  }, [articleId])

  async function handleComplete() {
    // Cham lai bat buoc ly do (CHECK ck_royalty_gradings_recalc); server cung kiem lai
    if (isRegrade && recalcReason.trim() === '') {
      setSaveError('Vui lòng nhập lý do chấm lại.')
      return
    }
    const action = isRegrade ? 'Chấm lại' : 'Hoàn tất chấm'
    const confirmed = window.confirm(
      `${action} nhuận bút bài này với tổng ${formatMoney(grading.totalAmount)}?`)
    if (!confirmed) {
      return
    }
    setIsSaving(true)
    setSaveError('')
    try {
      const body = { articleId: Number(articleId) }
      if (isRegrade) {
        body.recalcReason = recalcReason.trim()
      }
      const saved = await completeRoyaltyGrading(body)
      setGrading(saved)
    } catch (err) {
      setSaveError(err.message)
    } finally {
      setIsSaving(false)
    }
  }

  const isSaved = Boolean(grading && grading.gradingId)
  const hasOpenPeriod = Boolean(grading && grading.periodId)
  // Cham lai thi quay ve bang ke cua ky; lan dau thi ve danh sach bai cho cham
  const backLink = isRegrade && hasOpenPeriod
    ? { to: `/royalty-periods/${grading.periodId}`, label: '← Bảng kê kỳ nhuận bút' }
    : { to: '/royalty-grading', label: '← Bài chờ chấm nhuận bút' }

  return (
    <div>
      <div className="royalty__header">
        <Link to={backLink.to} className="royalty__back">{backLink.label}</Link>
        <h1 className="royalty__title">{isRegrade ? 'Chấm lại nhuận bút' : 'Chấm nhuận bút'}</h1>
        {grading && <p className="royalty__subtitle">{grading.title}</p>}
      </div>

      {!grading && isLoading && <div className="royalty__card"><p className="royalty__empty">Đang tính…</p></div>}
      {!grading && loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}

      {grading && (
        <div className="royalty__grid">
          <div>
            <FormulaCard grading={grading} />
            <AuthorCard authors={grading.authors} />
            <MediaCard mediaItems={grading.mediaItems} authors={grading.authors} />
          </div>

          <div className="royalty__card royalty__total">
            <p className="royalty__total-label">Tổng nhuận bút</p>
            <p className="royalty__total-amount">{formatMoney(grading.totalAmount)}</p>
            <div className="royalty__total-row"><span>Phần bài viết</span><span>{formatMoney(grading.articleAmount)}</span></div>
            <div className="royalty__total-row"><span>Ảnh / video</span><span>{formatMoney(grading.mediaAmount)}</span></div>
            <div className="royalty__total-row">
              <span>Vào kỳ</span>
              <span>{hasOpenPeriod ? `Tháng ${grading.periodMonth}/${grading.periodYear}` : 'Chưa có kỳ đang mở'}</span>
            </div>

            {/* BR-IN-03: tinh chat PV khai luc nop bai, Thu ky chi xem */}
            <div className="royalty__total-row">
              <span>Tính chất bài (phóng viên khai)</span><span>{natureLabel(grading.nature)}</span>
            </div>

            {isRegrade && !isSaved && (
              <label className="royalty__field royalty__total-field">
                Lý do chấm lại (bắt buộc)
                <textarea className="royalty__input royalty__input--full royalty__textarea" rows={3}
                          maxLength={RECALC_REASON_MAX} value={recalcReason} disabled={isSaving}
                          onChange={(event) => setRecalcReason(event.target.value)}
                          placeholder="Vd: Kế toán trả kỳ vì sai tỷ lệ đồng tác giả" />
              </label>
            )}

            {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
            {saveError && <p className="thong-bao-loi" role="alert">{saveError}</p>}

            {isSaved ? (
              <>
                <p className="thong-bao-thanh-cong" role="status">
                  {isRegrade ? 'Đã chấm lại nhuận bút (phiên bản mới).' : 'Đã chấm nhuận bút.'} Bài
                  thuộc kỳ tháng {grading.periodMonth}/{grading.periodYear}.
                </p>
                <div className="royalty__total-links">
                  <Link to={`/royalty-periods/${grading.periodId}`} className="royalty__link">Xem bảng kê kỳ</Link>
                  <Link to="/royalty-grading" className="royalty__link">Chấm bài khác</Link>
                </div>
              </>
            ) : (
              <>
                <button type="button" className="nut-chinh-fm royalty__input--full" onClick={handleComplete}
                        disabled={isSaving || isLoading || Boolean(loadError) || !hasOpenPeriod}>
                  {isSaving ? 'Đang lưu…' : (isRegrade ? 'Lưu chấm lại' : 'Hoàn tất chấm nhuận bút')}
                </button>
                {!hasOpenPeriod && (
                  <p className="royalty__muted">Chưa có kỳ nhuận bút đang mở nên chưa thể hoàn tất (UC-47 E1).</p>
                )}
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

/** Cac buoc cong thuc (BR-RY-01..03): server tinh, man hinh chi hien de Thu ky kiem tra. */
function FormulaCard({ grading }) {
  return (
    <div className="royalty__card">
      <h2 className="royalty__card-title">Cách tính phần bài viết</h2>
      <table className="bang-fm">
        <tbody>
          <tr><td>Ngày đăng</td><td className="royalty__number">{formatDate(grading.publishedAt)}</td></tr>
          <tr><td>Điểm chất lượng đã duyệt</td><td className="royalty__number">{formatNumber(grading.scoreUsed)}</td></tr>
          <tr><td>Hệ số chuẩn của thể loại</td><td className="royalty__number">{formatNumber(grading.baseFactor)}</td></tr>
          <tr><td>Hệ số = hệ số chuẩn × điểm / 100</td><td className="royalty__number">{formatNumber(grading.suggestedFactor)}</td></tr>
          <tr><td>Tỷ lệ bão hoà sự kiện</td><td className="royalty__number">{formatNumber(grading.saturationRate)}%</td></tr>
          <tr><td>Tỷ lệ theo tính chất bài</td><td className="royalty__number">{formatNumber(grading.natureRate)}%</td></tr>
          <tr className="royalty__row-total">
            <td>Hệ số cuối (tối thiểu 1)</td><td className="royalty__number">{formatNumber(grading.finalFactor)}</td>
          </tr>
          <tr><td>Đơn giá 1 hệ số tại ngày đăng</td><td className="royalty__number">{formatMoney(grading.unitPrice)}</td></tr>
          <tr className="royalty__row-total">
            <td>Tiền bài = hệ số cuối × đơn giá (làm tròn xuống 1.000đ)</td>
            <td className="royalty__number">{formatMoney(grading.articleAmount)}</td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}

/** Chia tien bai cho cac tac gia (GBR-10): dong tac gia lam tron xuong, tac gia chinh nhan phan du. */
function AuthorCard({ authors }) {
  return (
    <div className="royalty__card">
      <h2 className="royalty__card-title">Chia tiền bài cho tác giả</h2>
      <table className="bang-fm">
        <thead>
          <tr><th>Tác giả</th><th className="royalty__number">Tỷ lệ</th><th className="royalty__number">Nhận</th></tr>
        </thead>
        <tbody>
          {authors.map((author) => (
            <tr key={author.userId}>
              <td>
                {author.fullName}
                {author.mainAuthor && <span className="royalty__badge royalty__badge--current royalty__badge-gap">Tác giả chính</span>}
              </td>
              <td className="royalty__number">{formatNumber(author.sharePercent)}%</td>
              <td className="royalty__number royalty__strong">{formatMoney(author.amount)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/** UC-48: tung anh/video. Anh khong tinh tien (nguon ngoai, tai su dung) hien ly do. */
function MediaCard({ mediaItems, authors }) {
  // Ten tac gia anh: tac gia anh luon la 1 trong cac tac gia cua bai (BR-IN-05)
  const authorNames = {}
  for (const author of authors) {
    authorNames[author.userId] = author.fullName
  }

  return (
    <div className="royalty__card">
      <h2 className="royalty__card-title">Ảnh / video</h2>
      {mediaItems.length === 0 && <p className="royalty__empty">Bài không có ảnh hoặc video.</p>}
      {mediaItems.length > 0 && (
        <div className="royalty__scroll">
          <table className="bang-fm">
            <thead>
              <tr>
                <th>Ảnh / video</th>
                <th>Tác giả ảnh</th>
                <th className="royalty__number">Hệ số</th>
                <th className="royalty__number">Tiền</th>
              </tr>
            </thead>
            <tbody>
              {mediaItems.map((media) => (
                <tr key={media.attachmentId}>
                  <td>
                    <div>{media.caption || `Tệp #${media.attachmentId}`}</div>
                    <div className="royalty__muted">{mediaKindLabel(media.mediaKind)}</div>
                  </td>
                  <td className="royalty__nowrap">{authorNames[media.authorId] || `Người dùng #${media.authorId}`}</td>
                  <td className="royalty__number">{media.payable ? formatNumber(media.factor) : '—'}</td>
                  <td className="royalty__number">
                    {media.payable
                      ? <span className="royalty__strong">{formatMoney(media.amount)}</span>
                      : <span className="royalty__muted">Không tính: {media.noRoyaltyReason}</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
