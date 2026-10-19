import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getArticleDetail, withdrawArticle } from '../../api/articleApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { STATUS_COLORS, statusLabel, natureLabel, confirmLabel } from '../../utils/articleLabels.js'
import Icon from '../../components/common/Icon.jsx'
import '../../styles/ArticleDetail.css'

/**
 * UC-02 - Chi tiet bai: noi dung, khai bao, tac gia + trang thai xac nhan,
 * lich su trang thai. Tac gia chinh rut duoc bai dang "Da nop" (UC-11, BR-IN-08):
 * server tra canWithdraw de biet co hien nut hay khong.
 */
export default function ArticleDetail() {
  const { id } = useParams()

  const [article, setArticle] = useState(null)   // null = dang tai
  const [error, setError] = useState('')
  const [confirmingWithdraw, setConfirmingWithdraw] = useState(false)   // dang hien hop xac nhan rut bai
  const [withdrawing, setWithdrawing] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')            // bao thanh cong sau khi rut

  // Tai chi tiet bai khi mo trang (hoac doi sang bai khac)
  useEffect(() => {
    let isMounted = true

    getArticleDetail(id)
      .then((data) => {
        if (isMounted) {
          setArticle(data)
          setError('')
        }
      })
      .catch((err) => { if (isMounted) setError(err.message) })

    return () => { isMounted = false }
  }, [id])

  /** Bam "Xac nhan rut bai" trong hop hoi: goi API, thay du lieu bai bang ban moi. */
  function handleConfirmWithdraw() {
    setWithdrawing(true)
    setError('')
    withdrawArticle(id)
      .then((data) => {
        setArticle(data)
        setSuccessMessage('Đã rút bài về trạng thái Nháp.')
      })
      .catch((err) => setError(err.message))
      .finally(() => {
        setWithdrawing(false)
        setConfirmingWithdraw(false)
      })
  }

  return (
    <div className="article-detail">
      <Link to="/my-articles" className="article-detail__back">
        <Icon><path d="M15 6l-6 6 6 6" /></Icon>
        <span>Bài của tôi</span>
      </Link>

      {error && <p className="thong-bao-loi" role="alert">{error}</p>}
      {!article && !error && <p className="article-detail__loading">Đang tải…</p>}

      {article && (
        <>
          <div className="article-detail__header">
            <div>
              <h1 className="article-detail__title">{article.title}</h1>
              <span className="article-detail__status">
                <span className={`cham cham--${STATUS_COLORS[article.status]}`} aria-hidden="true" />
                {statusLabel(article.status)}
              </span>
            </div>
            {article.canWithdraw && !confirmingWithdraw && (
              <button type="button" className="nut-nhe article-detail__withdraw-button" onClick={() => setConfirmingWithdraw(true)}>
                Rút bài
              </button>
            )}
          </div>

          {successMessage && <p className="article-detail__success" role="status">{successMessage}</p>}

          {/* Hop hoi lai truoc khi rut (BR-IN-08) */}
          {confirmingWithdraw && (
            <div className="article-detail__confirm-box" role="alertdialog" aria-labelledby="confirm-withdraw-text">
              <p id="confirm-withdraw-text">
                Rút bài về <strong>Nháp</strong>? Bài sẽ rời hàng chờ phân công; bạn sửa xong thì nộp lại.
              </p>
              <div className="article-detail__confirm-actions">
                <button type="button" className="nut-nhe" onClick={() => setConfirmingWithdraw(false)} disabled={withdrawing}>
                  Huỷ
                </button>
                <button type="button" className="article-detail__confirm-button" onClick={handleConfirmWithdraw} disabled={withdrawing}>
                  {withdrawing ? 'Đang rút…' : 'Xác nhận rút bài'}
                </button>
              </div>
            </div>
          )}

          <div className="article-detail__grid">
            {/* Cot trai: noi dung bai */}
            <section className="article-detail__section">
              <h2 className="article-detail__section-title">Nội dung</h2>
              <div className="article-detail__content">{article.content || 'Chưa có nội dung.'}</div>
            </section>

            {/* Cot phai: khai bao, tac gia, lich su */}
            <div className="article-detail__side">
              <section className="article-detail__section">
                <h2 className="article-detail__section-title">Khai báo</h2>
                <dl className="article-detail__declaration">
                  {/* TODO(admin): hien ten the loai, ten ban khi server tra ve */}
                  <dt>Thể loại khai</dt><dd>Mã {article.declaredCategoryId}</dd>
                  <dt>Thể loại áp dụng</dt><dd>Mã {article.categoryId}</dd>
                  <dt>Tính chất</dt><dd>{natureLabel(article.declaredNature)}</dd>
                  <dt>Sự kiện</dt><dd>{article.eventId ? `Mã ${article.eventId}` : 'Không'}</dd>
                  <dt>Ngày tạo</dt><dd>{formatDate(article.createdAt)}</dd>
                  <dt>Nộp gần nhất</dt><dd>{formatDate(article.submittedAt)}</dd>
                  {article.withdrawnAt && (<><dt>Rút gần nhất</dt><dd>{formatDate(article.withdrawnAt)}</dd></>)}
                </dl>
              </section>

              <section className="article-detail__section">
                <h2 className="article-detail__section-title">Tác giả</h2>
                <ul className="article-detail__list">
                  {article.authors.map((author) => (
                    <li key={author.userId} className="article-detail__author">
                      <div>
                        {/* TODO(admin): hien ho ten khi co entity User */}
                        <span className="article-detail__name">Người dùng #{author.userId}</span>
                        <span className="article-detail__muted">
                          {author.mainAuthor ? 'Tác giả chính' : 'Đồng tác giả'} · {Number(author.sharePercent)}%
                        </span>
                      </div>
                      <span className="article-detail__muted">
                        {confirmLabel(author.confirmStatus)}
                        {author.confirmStatus === 'PENDING' && author.confirmDeadline
                          && ` (hạn ${formatDate(author.confirmDeadline)})`}
                      </span>
                    </li>
                  ))}
                </ul>
              </section>

              <section className="article-detail__section">
                <h2 className="article-detail__section-title">Lịch sử trạng thái</h2>
                <ol className="article-detail__list article-detail__history">
                  {article.statusLogs.map((log, index) => (
                    <li key={index}>
                      <span className="article-detail__status">
                        <span className={`cham cham--${STATUS_COLORS[log.toStatus]}`} aria-hidden="true" />
                        {statusLabel(log.toStatus)}
                      </span>
                      <span className="article-detail__muted"> · {formatDate(log.createdAt)}</span>
                      {log.note && <p className="article-detail__note">{log.note}</p>}
                    </li>
                  ))}
                </ol>
              </section>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
