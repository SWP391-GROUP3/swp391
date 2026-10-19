import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { completeRoyaltyGrading, previewRoyaltyGrading } from '../../api/royaltyApi.js'
import { formatMoney } from '../../utils/formatMoney.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import { NATURE_LABELS, mediaKindLabel } from '../../utils/royaltyLabels.js'
import '../../styles/Royalty.css'

/**
 * UC-47, UC-48 - "Cham nhuan but" 1 bai (Thu ky toa soan).
 * He so va tien do SERVER tinh (BR-RY-01: Thu ky khong sua he so). Thu ky chi:
 *   1. xac nhan tinh chat bai -> goi lai xem truoc de thay so moi
 *   2. xem anh/video nao duoc tinh tien (UC-48)
 *   3. bam Hoan tat -> luu phieu, bai vao ky dang mo (BR-RY-05)
 *
 * MUC LUC (so dong)
 *   26  RoyaltyGrading               - UC-47, UC-48 - "Cham nhuan but" 1 bai (Thu ky toa soan).
 *   36    useEffect                  - Moi lan doi tinh chat thi tinh lai (xem truoc, chua luu)
 *   45    handleNatureChange()
 *   52    handleComplete()
 *  145  FormulaCard                  - Cac buoc cong thuc (BR-RY-01..03): server tinh, man hinh chi hien de Th…
 *  172  AuthorCard                   - Chia tien bai cho cac tac gia (GBR-10): dong tac gia lam tron xuong, ta…
 *  198  MediaCard                    - UC-48: tung anh/video. Anh khong tinh tien (nguon ngoai, tai su dung) h…
 */
export default function RoyaltyGrading() {
  const { articleId } = useParams()
  const [nature, setNature] = useState('')        // '' = dung tinh chat PV khai (server tu chon)
  const [grading, setGrading] = useState(null)    // phieu tinh san: xem truoc, hoac da luu (co gradingId)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState('')

  // Moi lan doi tinh chat thi tinh lai (xem truoc, chua luu)
  useEffect(() => {
    let isMounted = true
    previewRoyaltyGrading(articleId, nature)
      .then((data) => { if (isMounted) setGrading(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
      .finally(() => { if (isMounted) setIsLoading(false) })
    return () => { isMounted = false }
  }, [articleId, nature])

  function handleNatureChange(event) {
    setIsLoading(true)
    setLoadError('')
    setSaveError('')
    setNature(event.target.value)
  }

  async function handleComplete() {
    const confirmed = window.confirm(
      `Hoàn tất chấm nhuận bút bài này với tổng ${formatMoney(grading.totalAmount)}? Sau khi lưu không sửa được.`)
    if (!confirmed) {
      return
    }
    setIsSaving(true)
    setSaveError('')
    try {
      const saved = await completeRoyaltyGrading({ articleId: Number(articleId), nature: grading.nature })
      setGrading(saved)
    } catch (err) {
      setSaveError(err.message)
    } finally {
      setIsSaving(false)
    }
  }

  const isSaved = Boolean(grading && grading.gradingId)
  const hasOpenPeriod = Boolean(grading && grading.periodId)

  return (
    <div>
      <div className="royalty__header">
        <Link to="/royalty-grading" className="royalty__back">← Bài chờ chấm nhuận bút</Link>
        <h1 className="royalty__title">Chấm nhuận bút</h1>
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

            <label className="royalty__field royalty__total-field">
              Tính chất bài (Thư ký xác nhận)
              <select className="royalty__input royalty__input--full" value={grading.nature}
                      onChange={handleNatureChange} disabled={isSaved || isSaving || isLoading}>
                {Object.entries(NATURE_LABELS).map(([code, label]) => (
                  <option key={code} value={code}>{label}</option>
                ))}
              </select>
            </label>

            {isLoading && <p className="royalty__muted">Đang tính lại…</p>}
            {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
            {saveError && <p className="thong-bao-loi" role="alert">{saveError}</p>}

            {isSaved ? (
              <>
                <p className="thong-bao-thanh-cong" role="status">
                  Đã chấm nhuận bút. Bài vào kỳ tháng {grading.periodMonth}/{grading.periodYear}.
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
                  {isSaving ? 'Đang lưu…' : 'Hoàn tất chấm nhuận bút'}
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
