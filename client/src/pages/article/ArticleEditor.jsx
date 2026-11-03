import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  checkDuplication, createArticle, estimateRoyalty, getArticleDetail, getCoAuthorCandidates, getGenres, getOpenEvents,
  resubmitArticle, submitArticle, updateArticle,
} from '../../api/articleApi.js'
import { NATURE_LABELS, statusLabel } from '../../utils/articleLabels.js'
import { formatMoney } from '../../utils/formatMoney.js'
import Icon from '../../components/common/Icon.jsx'
import ArticleAttachments from './ArticleAttachments.jsx'
import ArticleDuplicationResult from './ArticleDuplicationResult.jsx'
import '../../styles/ArticleEditor.css'

/**
 * Work Editor (Iter 2): soan / sua ban nhap (UC-03), khai dong tac gia + ty le (UC-04),
 * nop bai (UC-05), sua va nop lai bai bi tra ve (UC-10).
 * /my-articles/new = bai moi, /my-articles/:id/edit = sua bai DRAFT / REVISION_REQUESTED.
 * Moi luat (5 tac gia, tong 100%, ai duoc sua) kiem o server; man nay chi hien loi server tra ve.
 *
 * CODE LIEN QUAN (Ctrl+P trong VS Code, dan duong dan de mo file):
 *   client/src/api/articleApi.js
 *   server/src/main/java/com/swp391/kpi/article/controller/ArticleController.java
 *   server/src/main/java/com/swp391/kpi/article/service/ArticleEditorService.java     tao / sua nhap, nop, nop lai (doSubmit), uoc tinh
 *   server/src/main/java/com/swp391/kpi/article/service/ArticleShareValidator.java    luat tac gia + ty le (BR-IN-02)
 *   server/src/main/java/com/swp391/kpi/article/service/DuplicationCheckService.java  kiem trung noi dung (UC-06)
 * DUONG DI KHI MO TRANG / BAM NUT (ham api -> URL -> Controller -> Service):
 *   Mo trang: getGenres(), getCoAuthorCandidates(), getOpenEvents(), getArticleDetail() (neu dang sua)
 *   [Luu nhap]: createArticle() / updateArticle() -> POST / PUT /api/articles -> ArticleEditorService.createDraft() / updateDraft()
 *   [Luu va kiem tra de nop]: luu nhu tren roi checkDuplication() -> POST /api/articles/{id}/duplication-check
 *       -> DuplicationCheckService.previewCheck() (chi tinh, khong luu)
 *   [Xac nhan nop]: submitArticle() / resubmitArticle() -> POST /api/articles/{id}/submit | /resubmit
 *       -> ArticleEditorService.submitArticle() / resubmitArticle() -> doSubmit()
 *   [Uoc tinh nhuan but]: estimateRoyalty() -> POST /api/articles/royalty-estimate -> ArticleEditorService.estimateRoyalty()
 *
 * MO TRANG NAY TAI: /my-articles/new, /my-articles/:id/edit
 *
 * MUC LUC (so dong - ten - tac dung)
 *   59  ArticleEditor()                 xem tom tat dau file
 *   87    useEffect                     Tai the loai, danh sach phong vien va (neu dang sua) noi dung bai
 *  133    changeField()                 Go vao 1 o cua form (tieu de, noi dung, the loai, tinh chat)
 *  140    changeShare()                 Doi ty le cua 1 tac gia
 *  146    addCoAuthor()                 Them dong tac gia da chon o o chon; ty le de 0 cho nguoi dung tu nhap
 *  154    removeCoAuthor()              Bo 1 dong tac gia (UC-04 A1: phai chia lai ty le truoc khi luu)
 *  159    buildPayload()                Du lieu gui server: dung ten field cua ArticleSaveRequest
 *  175    save()                        Luu nhap; alsoCheck = true thi luu xong kiem trung noi dung (UC-05 buoc 2) va hien ket qua, nguoi dung bam "X…
 *  199    confirmSubmit()               UC-05 buoc 3 / UC-10 buoc 3: xac nhan nop (kem giai trinh neu can), server kiem trung lai roi moi nop
 *  211    runEstimate()                 UC-09: uoc tinh nhuan but theo the loai, tinh chat, su kien dang chon (khong luu gi)
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  236  [< Chi tiet bai / Bai cua toi]  quay ve /my-articles/:id (dang sua) hoac /my-articles (bai moi), khong luu
 *  311  [Uoc tinh nhuan but]            runEstimate() -> POST /api/articles/royalty-estimate (UC-09), chi hien bang uoc tinh, khong luu
 *  352  [Bo] o dong dong tac gia        bo nguoi do khoi danh sach (chua goi server, bam Luu moi luu)
 *  370  [Them]                          them nguoi chon o o 'Them dong tac gia' vao danh sach, ty le 0%
 *  376  [Luu nhap]                      bai moi: POST /api/articles roi chuyen sang /my-articles/:id/edit; bai cu: PUT /api/articles/{id}
 *  379  [Luu va kiem tra de nop] / [nop lai]  luu nhu tren roi POST /api/articles/{id}/duplication-check (UC-05 buoc 2): hien ket qua kiem trung, CHUA nop
 *  398  [De sau]                        dong khung ket qua kiem trung, bai van la Nhap
 *  400  [Xac nhan nop] / [Xac nhan nop lai]  an khi BLOCKED; POST /submit (UC-05) hoac /resubmit (UC-10) kem giai trinh, server kiem trung lai roi moi nop -> mo /my-articles/:id
 */
export default function ArticleEditor({ nguoiDung }) {
  const { id } = useParams()            // khong co id = bai moi
  const navigate = useNavigate()

  const [form, setForm] = useState({ title: '', content: '', declaredCategoryId: '', declaredNature: '', eventId: '' })
  // Gom ca tac gia chinh (dong dau, khong xoa duoc). sharePercent giu dang chuoi cho o nhap so
  const [authors, setAuthors] = useState([
    { userId: nguoiDung.id, fullName: nguoiDung.hoTen, sharePercent: '100', main: true },
  ])
  const [genres, setGenres] = useState([])
  const [events, setEvents] = useState([])           // su kien dang mo (UC-03, BR-IN-03)
  const [estimate, setEstimate] = useState(null)     // ket qua uoc tinh nhuan but (UC-09)
  const [estimating, setEstimating] = useState(false)
  const [candidates, setCandidates] = useState([])
  const [status, setStatus] = useState('DRAFT')       // trang thai bai dang sua
  const [canEdit, setCanEdit] = useState(true)        // server cho sua khong (bai moi luon sua duoc)
  const [returnNote, setReturnNote] = useState('')    // nhan xet BTV khi tra bai (UC-10 buoc 1)
  const [pickedUserId, setPickedUserId] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  // UC-05 buoc 2-3: ket qua kiem trung truoc khi xac nhan nop, giai trinh (UC-08), ma bai vua luu
  const [check, setCheck] = useState(null)
  const [explanation, setExplanation] = useState('')
  const [savedId, setSavedId] = useState(null)

  // Tai the loai, danh sach phong vien va (neu dang sua) noi dung bai
  useEffect(() => {
    let isMounted = true

    const loadArticle = id ? getArticleDetail(id) : Promise.resolve(null)
    Promise.all([getGenres(), getCoAuthorCandidates(), loadArticle, getOpenEvents()])
      .then(([genreList, candidateList, article, eventList]) => {
        if (!isMounted) return
        setGenres(genreList)
        setEvents(eventList)
        setCandidates(candidateList)
        if (article) {
          if (!article.canEdit) {
            setError('Bài này không sửa được (chỉ tác giả chính sửa khi bài ở trạng thái Nháp hoặc Trả về – cần sửa).')
          }
          setCanEdit(article.canEdit)
          setForm({
            title: article.title,
            content: article.content || '',
            declaredCategoryId: String(article.declaredCategoryId),
            declaredNature: article.declaredNature,
            eventId: article.eventId ? String(article.eventId) : '',
          })
          // Tac gia chinh dung dau
          const rows = article.authors.map((author) => ({
            userId: author.userId,
            fullName: author.fullName || `Người dùng #${author.userId}`,
            sharePercent: String(Number(author.sharePercent)),
            main: author.mainAuthor,
          }))
          rows.sort((a, b) => Number(b.main) - Number(a.main))
          setAuthors(rows)
          setStatus(article.status)
          // Nhan xet chung cua lan BTV tra bai gan nhat
          const returnLogs = article.statusLogs.filter((log) => log.toStatus === 'REVISION_REQUESTED')
          if (article.status === 'REVISION_REQUESTED' && returnLogs.length > 0) {
            setReturnNote(returnLogs[returnLogs.length - 1].note || '')
          }
        }
      })
      .catch((err) => { if (isMounted) setError(err.message) })
      .finally(() => { if (isMounted) setLoading(false) })

    return () => { isMounted = false }
  }, [id])

  /** Go vao 1 o cua form (tieu de, noi dung, the loai, tinh chat). */
  function changeField(field, value) {
    setForm({ ...form, [field]: value })
    setEstimate(null)   // doi khai bao thi so uoc tinh cu khong con dung
    setCheck(null)      // doi noi dung / tinh chat thi phai kiem trung lai
  }

  /** Doi ty le cua 1 tac gia. */
  function changeShare(userId, value) {
    setAuthors(authors.map((author) => (author.userId === userId ? { ...author, sharePercent: value } : author)))
    setCheck(null)
  }

  /** Them dong tac gia da chon o o chon; ty le de 0 cho nguoi dung tu nhap. */
  function addCoAuthor() {
    const candidate = candidates.find((item) => String(item.id) === pickedUserId)
    if (!candidate) return
    setAuthors([...authors, { userId: candidate.id, fullName: candidate.fullName, sharePercent: '0', main: false }])
    setPickedUserId('')
  }

  /** Bo 1 dong tac gia (UC-04 A1: phai chia lai ty le truoc khi luu). */
  function removeCoAuthor(userId) {
    setAuthors(authors.filter((author) => author.userId !== userId))
  }

  /** Du lieu gui server: dung ten field cua ArticleSaveRequest. */
  function buildPayload() {
    return {
      title: form.title,
      content: form.content,
      declaredCategoryId: form.declaredCategoryId ? Number(form.declaredCategoryId) : null,
      declaredNature: form.declaredNature || null,
      // Luon gui lai su kien dang chon: bai da phan cong ma gui khac su kien da khoa thi server chan (B13)
      eventId: form.eventId ? Number(form.eventId) : null,
      authors: authors.map((author) => ({ userId: author.userId, sharePercent: Number(author.sharePercent) })),
    }
  }

  /**
   * Luu nhap; alsoCheck = true thi luu xong kiem trung noi dung (UC-05 buoc 2) va hien ket qua,
   * nguoi dung bam "Xác nhận nộp" moi nop that (confirmSubmit).
   */
  function save(alsoCheck) {
    setSaving(true)
    setError('')
    setSuccess('')
    const saveRequest = id ? updateArticle(id, buildPayload()) : createArticle(buildPayload())
    saveRequest
      .then((article) => {
        if (!alsoCheck) {
          setSuccess('Đã lưu bản nháp.')
          if (!id) navigate(`/my-articles/${article.id}/edit`, { replace: true })
          return null
        }
        // Bai moi chua co id tren URL -> giu ma bai vua tao de nop (khong chuyen trang de giu ket qua kiem)
        setSavedId(article.id)
        return checkDuplication(article.id).then((data) => {
          setCheck(data)
          setExplanation('')
        })
      })
      .catch((err) => setError(err.message))
      .finally(() => setSaving(false))
  }

  /** UC-05 buoc 3 / UC-10 buoc 3: xac nhan nop (kem giai trinh neu can), server kiem trung lai roi moi nop. */
  function confirmSubmit() {
    setSaving(true)
    setError('')
    const duLieu = { explanation: explanation.trim() || null }
    const submitRequest = isResubmit ? resubmitArticle(savedId, duLieu) : submitArticle(savedId, duLieu)
    submitRequest
      .then(() => navigate(`/my-articles/${savedId}`))
      .catch((err) => setError(err.message))
      .finally(() => setSaving(false))
  }

  /** UC-09: uoc tinh nhuan but theo the loai, tinh chat, su kien dang chon (khong luu gi). */
  function runEstimate() {
    setEstimating(true)
    setError('')
    estimateRoyalty({
      declaredCategoryId: form.declaredCategoryId ? Number(form.declaredCategoryId) : null,
      declaredNature: form.declaredNature || null,
      eventId: form.eventId ? Number(form.eventId) : null,
      articleId: id ? Number(id) : null,   // bai da tung phan cong thi server dung ty le da khoa (BR-IN-07)
    })
      .then((data) => setEstimate(data))
      .catch((err) => setError(err.message))
      .finally(() => setEstimating(false))
  }

  // Tong ty le dang nhap, hien ngay khi go (UC-04 buoc 3)
  const total = authors.reduce((sum, author) => sum + (Number(author.sharePercent) || 0), 0)
  const totalIsValid = Math.abs(total - 100) < 0.001
  const addedIds = authors.map((author) => author.userId)
  const freeCandidates = candidates.filter((item) => !addedIds.includes(item.id))
  const isResubmit = status === 'REVISION_REQUESTED'
  // Su kien cua bai khong con trong danh sach dang mo (vd da khoa tu lan nop dau) thi van hien de khong mat
  const eventInList = !form.eventId || events.some((event) => String(event.id) === form.eventId)

  return (
    <div className="article-editor">
      <Link to={id ? `/my-articles/${id}` : '/my-articles'} className="article-editor__back">
        <Icon><path d="M15 6l-6 6 6 6" /></Icon>
        <span>{id ? 'Chi tiết bài' : 'Bài của tôi'}</span>
      </Link>

      <div className="article-editor__header">
        <h1 className="article-editor__title">{id ? 'Sửa bài' : 'Soạn bài mới'}</h1>
        {id && <span className="article-editor__muted">Trạng thái: {statusLabel(status)}</span>}
      </div>

      {error && <p className="thong-bao-loi" role="alert">{error}</p>}
      {success && <p className="article-editor__success" role="status">{success}</p>}
      {loading && <p className="article-editor__muted">Đang tải…</p>}

      {/* UC-10 buoc 1: nhac lai nhan xet chung cua BTV */}
      {returnNote && (
        <div className="article-editor__return-note">
          <strong>Nhận xét của biên tập viên:</strong> {returnNote}
        </div>
      )}

      {!loading && (
        <div className="article-editor__grid">
          {/* Cot trai: noi dung + anh/video (dung lai class xep doc cua cot phai) */}
          <div className="article-editor__side">
            <section className="article-editor__section">
              <label className="nhan" htmlFor="article-title">Tiêu đề *</label>
              <input id="article-title" className="o-nhap" value={form.title} maxLength={300}
                onChange={(e) => changeField('title', e.target.value)} />

              <label className="nhan" htmlFor="article-content">Nội dung (bắt buộc khi nộp)</label>
              <textarea id="article-content" className="o-nhap article-editor__content" value={form.content}
                onChange={(e) => changeField('content', e.target.value)} />
            </section>

            {/* Iter 3: anh/video gan vao bai da luu (can ma bai), nen bai moi phai Luu nhap truoc */}
            {id ? (
              <ArticleAttachments articleId={id} canEdit={canEdit} authors={authors} />
            ) : (
              <section className="article-editor__section">
                <h2 className="article-editor__section-title">Ảnh / video</h2>
                <p className="article-editor__muted">Bấm "Lưu nháp" trước, sau đó thêm ảnh/video cho bài.</p>
              </section>
            )}
          </div>

          {/* Cot phai: khai bao + tac gia */}
          <div className="article-editor__side">
            <section className="article-editor__section">
              <h2 className="article-editor__section-title">Khai báo</h2>
              <label className="nhan" htmlFor="article-category">Thể loại *</label>
              <select id="article-category" className="o-nhap" value={form.declaredCategoryId}
                onChange={(e) => changeField('declaredCategoryId', e.target.value)}>
                <option value="">-- Chọn thể loại --</option>
                {genres.map((genre) => <option key={genre.id} value={genre.id}>{genre.name}</option>)}
              </select>

              {/* Bat buoc chon, khong chon san (nhom truong 24/10, B3) */}
              <label className="nhan" htmlFor="article-nature">Tính chất *</label>
              <select id="article-nature" className="o-nhap" value={form.declaredNature}
                onChange={(e) => changeField('declaredNature', e.target.value)}>
                <option value="">-- Chọn tính chất --</option>
                {Object.entries(NATURE_LABELS).map(([code, label]) => <option key={code} value={code}>{label}</option>)}
              </select>
              {/* UC-03, BR-IN-03: chon su kien dang mo; bai da phan cong (bi tra ve) giu su kien da khoa (B13) */}
              <label className="nhan" htmlFor="article-event">Sự kiện</label>
              <select id="article-event" className="o-nhap" value={form.eventId} disabled={isResubmit}
                onChange={(e) => changeField('eventId', e.target.value)}>
                <option value="">-- Không thuộc sự kiện --</option>
                {events.map((event) => <option key={event.id} value={event.id}>{event.name}</option>)}
                {!eventInList && <option value={form.eventId}>Sự kiện mã {form.eventId} (đã khóa)</option>}
              </select>
              <p className="article-editor__muted">Khai báo bị khóa khi nộp bài, chỉ đổi được khi sửa và nộp lại.</p>

              {/* UC-09: uoc tinh nhuan but o 3 muc diem, chi phan bai viet */}
              <button type="button" className="nut-nhe" disabled={estimating} onClick={runEstimate}>
                {estimating ? 'Đang tính…' : 'Ước tính nhuận bút'}
              </button>
              {estimate && (
                <table className="article-editor__estimate">
                  <thead>
                    <tr><th>Điểm</th><th>Bão hòa</th><th>Hệ số</th><th>Tiền bài viết</th></tr>
                  </thead>
                  <tbody>
                    {estimate.items.map((item) => (
                      <tr key={item.score}>
                        <td>{item.score}</td>
                        <td>{Number(item.saturationRate)}%</td>
                        <td>{Number(item.finalFactor)}</td>
                        <td>{formatMoney(item.amount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
              {estimate && (
                <p className="article-editor__muted">
                  Đơn giá hôm nay {formatMoney(estimate.unitPrice)} · chưa gồm ảnh/video · số thật tính khi chấm nhuận bút.
                </p>
              )}
            </section>

            <section className="article-editor__section">
              <h2 className="article-editor__section-title">Tác giả và tỷ lệ nhuận bút</h2>
              <ul className="article-editor__authors">
                {authors.map((author) => (
                  <li key={author.userId} className="article-editor__author">
                    <span className="article-editor__author-name">
                      {author.fullName}
                      <span className="article-editor__muted">{author.main ? ' · Tác giả chính' : ' · Đồng tác giả'}</span>
                    </span>
                    <input type="number" min="0" max="100" step="0.01" className="o-nhap-ngan article-editor__share"
                      aria-label={`Tỷ lệ của ${author.fullName}`} value={author.sharePercent}
                      onChange={(e) => changeShare(author.userId, e.target.value)} />
                    <span>%</span>
                    {!author.main && (
                      <button type="button" className="nut-nhe" onClick={() => removeCoAuthor(author.userId)}>Bỏ</button>
                    )}
                  </li>
                ))}
              </ul>

              <p className={`article-editor__total ${totalIsValid ? '' : 'article-editor__total--sai'}`}>
                Tổng: {Number(total.toFixed(2))}% {totalIsValid ? '' : '(phải bằng 100%)'}
              </p>

              <div className="article-editor__add">
                <select className="o-nhap-ngan" value={pickedUserId} aria-label="Chọn đồng tác giả"
                  onChange={(e) => setPickedUserId(e.target.value)}>
                  <option value="">-- Thêm đồng tác giả --</option>
                  {freeCandidates.map((item) => (
                    <option key={item.id} value={item.id}>{item.fullName}{item.sectionName ? ` (${item.sectionName})` : ''}</option>
                  ))}
                </select>
                <button type="button" className="nut-nhe" disabled={!pickedUserId} onClick={addCoAuthor}>Thêm</button>
              </div>
              <p className="article-editor__muted">Đồng tác giả chỉ được báo khi nộp bài và phải xác nhận trong 3 ngày.</p>
            </section>

            <div className="article-editor__actions">
              <button type="button" className="nut-nhe" disabled={saving} onClick={() => save(false)}>
                Lưu nháp
              </button>
              <button type="button" className="nut-chinh-fm" disabled={saving} onClick={() => save(true)}>
                {saving ? 'Đang lưu…' : (isResubmit ? 'Lưu và kiểm tra để nộp lại' : 'Lưu và kiểm tra để nộp')}
              </button>
            </div>

            {/* UC-05 buoc 2-3, UC-08: ket qua kiem trung + giai trinh + xac nhan nop */}
            {check && (
              <section className="article-editor__section">
                <h2 className="article-editor__section-title">Kiểm tra trùng nội dung</h2>
                <ArticleDuplicationResult check={check} />
                {check.result === 'NEED_EXPLAIN' && (
                  <>
                    <label className="nhan" htmlFor="article-explanation">Giải trình *</label>
                    <textarea id="article-explanation" className="o-nhap" rows={4} maxLength={1000}
                      value={explanation} onChange={(e) => setExplanation(e.target.value)}
                      placeholder="Vì sao bài có đoạn trùng với bài khác (trích dẫn, cùng nguồn tin…)" />
                  </>
                )}
                <div className="article-editor__actions">
                  <button type="button" className="nut-nhe" disabled={saving} onClick={() => setCheck(null)}>Để sau</button>
                  {check.result !== 'BLOCKED' && (
                    <button type="button" className="nut-chinh-fm" disabled={saving} onClick={confirmSubmit}>
                      {saving ? 'Đang nộp…' : (isResubmit ? 'Xác nhận nộp lại' : 'Xác nhận nộp')}
                    </button>
                  )}
                </div>
              </section>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
