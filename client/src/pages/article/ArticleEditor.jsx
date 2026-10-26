import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  createArticle, getArticleDetail, getCoAuthorCandidates, getGenres,
  resubmitArticle, submitArticle, updateArticle,
} from '../../api/articleApi.js'
import { NATURE_LABELS, statusLabel } from '../../utils/articleLabels.js'
import Icon from '../../components/common/Icon.jsx'
import '../../styles/ArticleEditor.css'

/**
 * Work Editor (Iter 2): soan / sua ban nhap (UC-03), khai dong tac gia + ty le (UC-04),
 * nop bai (UC-05), sua va nop lai bai bi tra ve (UC-10).
 * /my-articles/new = bai moi, /my-articles/:id/edit = sua bai DRAFT / REVISION_REQUESTED.
 * Moi luat (5 tac gia, tong 100%, ai duoc sua) kiem o server; man nay chi hien loi server tra ve.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   34  ArticleEditor()                 Work Editor (Iter 2): soan / sua ban nhap (UC-03), khai dong tac gia + ty le (UC-04), nop bai (UC-05), sua va…
 *   54    useEffect                     Tai the loai, danh sach phong vien va (neu dang sua) noi dung bai
 *   97    changeField()                 Go vao 1 o cua form (tieu de, noi dung, the loai, tinh chat)
 *  102    changeShare()                 Doi ty le cua 1 tac gia
 *  107    addCoAuthor()                 Them dong tac gia da chon o o chon; ty le de 0 cho nguoi dung tu nhap
 *  115    removeCoAuthor()              Bo 1 dong tac gia (UC-04 A1: phai chia lai ty le truoc khi luu)
 *  120    buildPayload()                Du lieu gui server: dung ten field cua ArticleSaveRequest
 *  131    save()                        Luu nhap; alsoSubmit = true thi luu xong nop luon (UC-05) hoac nop lai (UC-10)
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  161  [< Chi tiet bai / Bai cua toi]  quay ve /my-articles/:id (dang sua) hoac /my-articles (bai moi), khong luu
 *  230  [Bo] o dong dong tac gia        bo nguoi do khoi danh sach (chua goi server, bam Luu moi luu)
 *  248  [Them]                          them nguoi chon o o 'Them dong tac gia' vao danh sach, ty le 0%
 *  254  [Luu nhap]                      bai moi: POST /api/articles roi chuyen sang /my-articles/:id/edit; bai cu: PUT /api/articles/{id}
 *  257  [Luu va nop bai] / [nop lai]    luu nhu tren roi POST /submit (UC-05) hoac /resubmit (UC-10), xong mo /my-articles/:id
 */
export default function ArticleEditor({ nguoiDung }) {
  const { id } = useParams()            // khong co id = bai moi
  const navigate = useNavigate()

  const [form, setForm] = useState({ title: '', content: '', declaredCategoryId: '', declaredNature: '' })
  // Gom ca tac gia chinh (dong dau, khong xoa duoc). sharePercent giu dang chuoi cho o nhap so
  const [authors, setAuthors] = useState([
    { userId: nguoiDung.id, fullName: nguoiDung.hoTen, sharePercent: '100', main: true },
  ])
  const [genres, setGenres] = useState([])
  const [candidates, setCandidates] = useState([])
  const [status, setStatus] = useState('DRAFT')       // trang thai bai dang sua
  const [returnNote, setReturnNote] = useState('')    // nhan xet BTV khi tra bai (UC-10 buoc 1)
  const [pickedUserId, setPickedUserId] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Tai the loai, danh sach phong vien va (neu dang sua) noi dung bai
  useEffect(() => {
    let isMounted = true

    const loadArticle = id ? getArticleDetail(id) : Promise.resolve(null)
    Promise.all([getGenres(), getCoAuthorCandidates(), loadArticle])
      .then(([genreList, candidateList, article]) => {
        if (!isMounted) return
        setGenres(genreList)
        setCandidates(candidateList)
        if (article) {
          if (!article.canEdit) {
            setError('Bài này không sửa được (chỉ tác giả chính sửa khi bài ở trạng thái Nháp hoặc Trả về – cần sửa).')
          }
          setForm({
            title: article.title,
            content: article.content || '',
            declaredCategoryId: String(article.declaredCategoryId),
            declaredNature: article.declaredNature,
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
  }

  /** Doi ty le cua 1 tac gia. */
  function changeShare(userId, value) {
    setAuthors(authors.map((author) => (author.userId === userId ? { ...author, sharePercent: value } : author)))
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
      authors: authors.map((author) => ({ userId: author.userId, sharePercent: Number(author.sharePercent) })),
    }
  }

  /** Luu nhap; alsoSubmit = true thi luu xong nop luon (UC-05) hoac nop lai (UC-10). */
  function save(alsoSubmit) {
    setSaving(true)
    setError('')
    setSuccess('')
    const saveRequest = id ? updateArticle(id, buildPayload()) : createArticle(buildPayload())
    saveRequest
      .then((article) => {
        if (!alsoSubmit) {
          setSuccess('Đã lưu bản nháp.')
          if (!id) navigate(`/my-articles/${article.id}/edit`, { replace: true })
          return null
        }
        const submitRequest = article.status === 'REVISION_REQUESTED'
          ? resubmitArticle(article.id)
          : submitArticle(article.id)
        return submitRequest.then(() => navigate(`/my-articles/${article.id}`))
      })
      .catch((err) => setError(err.message))
      .finally(() => setSaving(false))
  }

  // Tong ty le dang nhap, hien ngay khi go (UC-04 buoc 3)
  const total = authors.reduce((sum, author) => sum + (Number(author.sharePercent) || 0), 0)
  const totalIsValid = Math.abs(total - 100) < 0.001
  const addedIds = authors.map((author) => author.userId)
  const freeCandidates = candidates.filter((item) => !addedIds.includes(item.id))
  const isResubmit = status === 'REVISION_REQUESTED'

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
          {/* Cot trai: noi dung */}
          <section className="article-editor__section">
            <label className="nhan" htmlFor="article-title">Tiêu đề *</label>
            <input id="article-title" className="o-nhap" value={form.title} maxLength={300}
              onChange={(e) => changeField('title', e.target.value)} />

            <label className="nhan" htmlFor="article-content">Nội dung (bắt buộc khi nộp)</label>
            <textarea id="article-content" className="o-nhap article-editor__content" value={form.content}
              onChange={(e) => changeField('content', e.target.value)} />
          </section>

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
              <p className="article-editor__muted">Khai báo bị khóa khi nộp bài, chỉ đổi được khi sửa và nộp lại.</p>
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
                {saving ? 'Đang lưu…' : (isResubmit ? 'Lưu và nộp lại' : 'Lưu và nộp bài')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
