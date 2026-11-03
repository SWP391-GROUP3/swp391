import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  acceptCoAuthorship, getArticleDetail, getDuplicationCheck, rejectCoAuthorship, withdrawArticle,
} from '../../api/articleApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { STATUS_COLORS, statusLabel, natureLabel, confirmLabel } from '../../utils/articleLabels.js'
import Icon from '../../components/common/Icon.jsx'
import ArticleScoreCard from '../flow2/ArticleScoreCard.jsx'
import ArticleAttachments from './ArticleAttachments.jsx'
import ArticleDuplicationResult from './ArticleDuplicationResult.jsx'
import '../../styles/ArticleDetail.css'

/**
 * UC-02 - Chi tiet bai: noi dung, khai bao, tac gia + trang thai xac nhan,
 * lich su trang thai. Tac gia chinh rut duoc bai dang "Da nop" (UC-11, BR-IN-08):
 * server tra canWithdraw de biet co hien nut hay khong.
 *
 * Ai mo duoc: /my-articles/:id (Phong vien), /articles/:id (BTV duoc giao, Truong ban cung ban - chi xem, Iter 3).
 * Ai duoc xem do server kiem (ArticleViewService); nut thao tac chi hien khi server bat co (canEdit, canWithdraw...).
 *
 * CODE LIEN QUAN (Ctrl+P trong VS Code, dan duong dan de mo file):
 *   client/src/api/articleApi.js
 *   server/src/main/java/com/swp391/kpi/article/controller/ArticleController.java
 *   server/src/main/java/com/swp391/kpi/article/service/ArticleViewService.java          ai duoc xem bai (403)
 *   server/src/main/java/com/swp391/kpi/article/service/ArticleService.java              toDetailResponse(): tinh co canEdit, canWithdraw...; withdrawArticle()
 *   server/src/main/java/com/swp391/kpi/article/service/ArticleConfirmationService.java  dong y / tu choi dong tac gia
 *   server/src/main/java/com/swp391/kpi/article/service/DuplicationCheckService.java     ket qua kiem trung da luu
 * DUONG DI KHI MO TRANG / BAM NUT (ham api -> URL -> Controller -> Service):
 *   Mo trang: getArticleDetail() -> GET /api/articles/{id} -> ArticleController.getArticleDetail() -> ArticleViewService.getArticleDetail()
 *             getDuplicationCheck() -> GET /api/articles/{id}/duplication-check -> DuplicationCheckService.getLatestCheck()
 *   [Xac nhan rut bai]: withdrawArticle() -> POST /api/articles/{id}/withdraw -> ArticleService.withdrawArticle()
 *   [Dong y]: acceptCoAuthorship() -> POST /api/articles/{id}/co-author/accept -> ArticleConfirmationService.acceptCoAuthorship()
 *   [Gui tu choi]: rejectCoAuthorship() -> POST /api/articles/{id}/co-author/reject -> ArticleConfirmationService.rejectCoAuthorship()
 *
 * MO TRANG NAY TAI: /my-articles/:id, /articles/:id
 *
 * MUC LUC (so dong - ten - tac dung)
 *   57  ArticleDetail()                 xem tom tat dau file
 *   74    useEffect                     Tai chi tiet bai khi mo trang (hoac doi sang bai khac)
 *   91    handleConfirmWithdraw()       Bam "Xac nhan rut bai" trong hop hoi: goi API, thay du lieu bai bang ban moi
 *  107    runAction()                   Goi 1 API thao tac (nop bai, dong y, tu choi) roi thay du lieu bai bang ban moi server tra ve
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  131  [< Bai cua toi]                 quay ve trang /my-articles (MyArticles)
 *  168  [Sua bai] / [Sua va nop lai]    chi hien khi canEdit -> mo trang /my-articles/:id/edit (ArticleEditor, UC-03/UC-10)
 *  174  [Kiem tra va nop]               chi hien khi canSubmit -> mo trang /my-articles/:id/edit de kiem trung roi xac nhan nop (UC-05, UC-06)
 *  177  [Rut bai]                       chi hien khi canWithdraw -> mo hop hoi lai ngay ben duoi (UC-11)
 *  206  [Tu choi]                       chi hien khi canRespond -> mo o nhap ly do tu choi (UC-12 A1)
 *  210  [Dong y]                        dong tac gia dong y -> POST /api/articles/{id}/co-author/accept (UC-12)
 *  217  [Huy] (o ly do)                 dong o nhap ly do, khong gui gi
 *  221  [Gui tu choi]                   POST /api/articles/{id}/co-author/reject kem ly do -> bai ve Nhap
 *  237  [Huy] (hop rut bai)             dong hop hoi, khong rut
 *  240  [Xac nhan rut bai]              POST /api/articles/{id}/withdraw -> bai ve Nhap (BR-IN-08)
 *  249  khoi diem theo tieu chi         khong phai nut: component cua BF-02 (UC-34), chi hien khi bai SCORE_PUBLISHED / PUBLISHED
 */
export default function ArticleDetail({ nguoiDung }) {
  const { id } = useParams()
  const navigate = useNavigate()
  // PV quay ve "Bai cua toi"; BTV / Truong ban mo bai tu trang cua ho (hang cho, phieu cham) -> quay lai trang truoc
  const laPhongVien = nguoiDung?.vaiTro?.[0] === 'REPORTER'

  const [article, setArticle] = useState(null)   // null = dang tai
  const [error, setError] = useState('')
  const [confirmingWithdraw, setConfirmingWithdraw] = useState(false)   // dang hien hop xac nhan rut bai
  const [withdrawing, setWithdrawing] = useState(false)
  const [successMessage, setSuccessMessage] = useState('')            // bao thanh cong sau khi rut / nop / tra loi
  const [busy, setBusy] = useState(false)                             // dang goi API nop bai / tra loi xac nhan
  const [rejecting, setRejecting] = useState(false)                   // dang hien o nhap ly do tu choi
  const [rejectReason, setRejectReason] = useState('')
  const [duplication, setDuplication] = useState(null)   // ket qua kiem trung luc nop gan nhat (UC-06), null = chua nop

  // Tai chi tiet bai khi mo trang (hoac doi sang bai khac)
  useEffect(() => {
    let isMounted = true

    Promise.all([getArticleDetail(id), getDuplicationCheck(id)])
      .then(([data, check]) => {
        if (isMounted) {
          setArticle(data)
          setDuplication(check)
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

  /** Goi 1 API thao tac (nop bai, dong y, tu choi) roi thay du lieu bai bang ban moi server tra ve. */
  function runAction(request, message) {
    setBusy(true)
    setError('')
    setSuccessMessage('')
    request
      .then((data) => {
        setArticle(data)
        setSuccessMessage(message)
        setRejecting(false)
        setRejectReason('')
      })
      .catch((err) => setError(err.message))
      .finally(() => setBusy(false))
  }

  // Nhan xet chung cua lan BTV tra bai gan nhat (UC-10 buoc 1)
  const returnLogs = article ? article.statusLogs.filter((log) => log.toStatus === 'REVISION_REQUESTED') : []
  const returnNote = article && article.status === 'REVISION_REQUESTED' && returnLogs.length > 0
    ? returnLogs[returnLogs.length - 1].note
    : ''

  return (
    <div className="article-detail">
      {laPhongVien ? (
        <Link to="/my-articles" className="article-detail__back">
          <Icon><path d="M15 6l-6 6 6 6" /></Icon>
          <span>Bài của tôi</span>
        </Link>
      ) : (
        // Mo bai trong tab moi thi khong co trang truoc -> di theo link ve trang chu
        <Link
          to="/main"
          className="article-detail__back"
          onClick={(e) => {
            if (window.history.length > 1) {
              e.preventDefault()
              navigate(-1)
            }
          }}
        >
          <Icon><path d="M15 6l-6 6 6 6" /></Icon>
          <span>Quay lại</span>
        </Link>
      )}

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
            <div className="article-detail__actions">
              {/* Server tinh san canEdit / canSubmit / canResubmit (BR-IN-01, UC-05, UC-10) */}
              {article.canEdit && (
                <Link to={`/my-articles/${article.id}/edit`} className="nut-nhe">
                  {article.canResubmit ? 'Sửa và nộp lại' : 'Sửa bài'}
                </Link>
              )}
              {/* UC-05 buoc 2-3: nop phai kiem trung truoc -> mo man soan bai de kiem tra va xac nhan nop */}
              {article.canSubmit && (
                <Link to={`/my-articles/${article.id}/edit`} className="nut-chinh-fm">Kiểm tra và nộp</Link>
              )}
              {article.canWithdraw && !confirmingWithdraw && (
                <button type="button" className="nut-nhe article-detail__withdraw-button" onClick={() => setConfirmingWithdraw(true)}>
                  Rút bài
                </button>
              )}
            </div>
          </div>

          {successMessage && <p className="article-detail__success" role="status">{successMessage}</p>}

          {returnNote && (
            <div className="article-detail__return-note">
              <strong>Nhận xét của biên tập viên:</strong> {returnNote}
            </div>
          )}

          {/* UC-12: dong tac gia dong y / tu choi (bat buoc ly do) */}
          {article.canRespond && (
            <div className="article-detail__respond-box">
              <p>Bạn được mời làm đồng tác giả bài này. Xem nội dung và tỷ lệ của bạn rồi trả lời trước hạn.</p>
              {rejecting && (
                <>
                  <label className="nhan" htmlFor="reject-reason">Lý do từ chối *</label>
                  <textarea id="reject-reason" className="o-nhap" rows={3} maxLength={500} value={rejectReason}
                    onChange={(e) => setRejectReason(e.target.value)} />
                </>
              )}
              <div className="article-detail__confirm-actions">
                {!rejecting && (
                  <>
                    <button type="button" className="nut-nhe" disabled={busy} onClick={() => setRejecting(true)}>
                      Từ chối
                    </button>
                    <button type="button" className="nut-chinh-fm" disabled={busy}
                      onClick={() => runAction(acceptCoAuthorship(article.id), 'Bạn đã đồng ý làm đồng tác giả.')}>
                      Đồng ý
                    </button>
                  </>
                )}
                {rejecting && (
                  <>
                    <button type="button" className="nut-nhe" disabled={busy} onClick={() => setRejecting(false)}>
                      Huỷ
                    </button>
                    <button type="button" className="article-detail__confirm-button" disabled={busy || !rejectReason.trim()}
                      onClick={() => runAction(rejectCoAuthorship(article.id, rejectReason), 'Bạn đã từ chối, bài trở về Nháp.')}>
                      Gửi từ chối
                    </button>
                  </>
                )}
              </div>
            </div>
          )}

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

          {/* UC-34: Bang diem danh gia chat luong theo tieu chi khi da cong bo diem hoac xuat ban */}
          {(article.status === 'SCORE_PUBLISHED' || article.status === 'PUBLISHED') && (
            <ArticleScoreCard articleId={article.id} />
          )}

          <div className="article-detail__grid">
            {/* Cot trai: noi dung bai + anh/video (dung lai class xep doc cua cot phai) */}
            <div className="article-detail__side">
              <section className="article-detail__section">
                <h2 className="article-detail__section-title">Nội dung</h2>
                <div className="article-detail__content">{article.content || 'Chưa có nội dung.'}</div>
              </section>

              {/* Iter 3: anh/video kem ket qua kiem trung (UC-07), chi xem; sua o man soan bai */}
              <ArticleAttachments articleId={article.id} canEdit={false} />

              {/* UC-06 / UC-08: ket qua kiem trung noi dung luc nop gan nhat, kem giai trinh (B8) */}
              {duplication && (
                <section className="article-detail__section">
                  <h2 className="article-detail__section-title">Kiểm tra trùng nội dung</h2>
                  <ArticleDuplicationResult check={duplication} />
                </section>
              )}
            </div>

            {/* Cot phai: khai bao, tac gia, lich su */}
            <div className="article-detail__side">
              <section className="article-detail__section">
                <h2 className="article-detail__section-title">Khai báo</h2>
                <dl className="article-detail__declaration">
                  {/* Ten the loai server lay qua GenreService; the loai da tat thi hien ma */}
                  <dt>Thể loại khai</dt><dd>{article.declaredCategoryName || `Mã ${article.declaredCategoryId}`}</dd>
                  <dt>Thể loại áp dụng</dt><dd>{article.categoryName || `Mã ${article.categoryId}`}</dd>
                  <dt>Tính chất</dt><dd>{natureLabel(article.declaredNature)}</dd>
                  <dt>Sự kiện</dt><dd>{article.eventId ? `Mã ${article.eventId}` : 'Không'}</dd>
                  {/* BR-IN-03: ty le bao hoa khoa luc nop bai dau tien */}
                  {article.saturationRate != null && (<><dt>Tỷ lệ bão hòa</dt><dd>{Number(article.saturationRate)}%</dd></>)}
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
                        <span className="article-detail__name">{author.fullName || `Người dùng #${author.userId}`}</span>
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
                      <span className="article-detail__muted">
                        {/* actorId null = job he thong tu chay (vd qua han xac nhan) */}
                        {' · '}{formatDate(log.createdAt)}{' · '}{log.actorId == null ? 'Hệ thống' : (log.actorName || `Người dùng #${log.actorId}`)}
                      </span>
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
