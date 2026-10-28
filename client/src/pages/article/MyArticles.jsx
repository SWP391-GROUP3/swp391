import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getMyArticles } from '../../api/articleApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { STATUS_GROUPS, STATUS_COLORS, statusLabel, natureLabel } from '../../utils/articleLabels.js'
import Icon from '../../components/common/Icon.jsx'
import '../../styles/MyArticles.css'

/** So bai moi trang (AGENTS.md 3.7: ?page=0&size=10) */
const PAGE_SIZE = 10

/**
 * UC-01 - "Bai cua toi": bai ma Phong vien la tac gia chinh hoac dong tac gia.
 * Loc theo nhom trang thai bang URL (?status=A,B), nen trang chu link thang toi
 * dung nhom duoc. Bam tieu de de mo chi tiet bai (UC-02).
 *
 * MO TRANG NAY TAI: /my-articles
 *
 * MUC LUC (so dong - ten - tac dung)
 *   10  PAGE_SIZE                       So bai moi trang (AGENTS.md 3.7: ?page=0&size=10)
 *   33  MyArticles()                    xem tom tat dau file
 *   43    useEffect                     Tai lai danh sach moi khi doi bo loc hoac doi trang
 *   60    selectGroup()                 Bam 1 nhom loc: ghi vao URL va quay ve trang dau
 *   69    goToPage()                    Bam Trang truoc / Trang sau: doi so trang tren URL, giu bo loc
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   83  [Soan bai moi]                  mo trang /my-articles/new (ArticleEditor) de soan bai moi (UC-03)
 *   94  chip [Tat ca]                   bo loc, ghi ?status= tren URL -> goi lai GET /api/articles
 *  105  chip nhom trang thai            loc theo nhom (Nhap, Cho xac nhan...) -> ?status=A,B -> GET /api/articles
 *  140  [ten bai] trong bang            mo trang /my-articles/:id (ArticleDetail) xem chi tiet bai (UC-02)
 *  174  [Trang truoc] / [Trang sau]     doi ?page= tren URL -> tai trang khac cua danh sach
 */
export default function MyArticles() {
  const [searchParams, setSearchParams] = useSearchParams()
  const statusFilter = searchParams.get('status') || ''   // vd "ASSIGNED,PENDING_APPROVAL"
  const page = Number(searchParams.get('page') || 0)

  const [result, setResult] = useState(null)   // null = dang tai
  const [error, setError] = useState('')

  // Tai lai danh sach moi khi doi bo loc hoac doi trang.
  // Trong luc tai, bang cu van hien cho toi khi co du lieu moi.
  useEffect(() => {
    let isMounted = true

    const statuses = statusFilter ? statusFilter.split(',') : []
    getMyArticles(statuses, page, PAGE_SIZE)
      .then((data) => {
        if (isMounted) {
          setResult(data)
          setError('')
        }
      })
      .catch((err) => { if (isMounted) setError(err.message) })

    return () => { isMounted = false }
  }, [statusFilter, page])

  /** Bam 1 nhom loc: ghi vao URL va quay ve trang dau. Chuoi rong = "Tat ca". */
  function selectGroup(groupStatuses) {
    const next = new URLSearchParams()
    if (groupStatuses) {
      next.set('status', groupStatuses)
    }
    setSearchParams(next)
  }

  /** Bam Trang truoc / Trang sau: doi so trang tren URL, giu bo loc. */
  function goToPage(newPage) {
    const next = new URLSearchParams(searchParams)
    next.set('page', newPage)
    setSearchParams(next)
  }

  return (
    <div className="my-articles">
      <div className="my-articles__header">
        <div>
          <h1 className="my-articles__title">Bài của tôi</h1>
          <p className="my-articles__subtitle">Bài bạn là tác giả chính hoặc đồng tác giả, sửa gần nhất xếp trước.</p>
        </div>
        {/* UC-03: mo man soan bai (ArticleEditor) */}
        <Link to="/my-articles/new" className="nut-chinh-fm">
          <Icon><path d="M12 5v14M5 12h14" /></Icon>
          <span>Soạn bài mới</span>
        </Link>
      </div>

      {/* Bo loc theo nhom trang thai */}
      <div className="my-articles__filters" role="group" aria-label="Lọc theo trạng thái">
        <button
          type="button"
          className={`my-articles__chip ${statusFilter === '' ? 'my-articles__chip--selected' : ''}`}
          onClick={() => selectGroup('')}
        >
          Tất cả
        </button>
        {STATUS_GROUPS.map((group) => {
          const groupStatuses = group.statuses.join(',')
          return (
            <button
              key={group.label}
              type="button"
              className={`my-articles__chip ${statusFilter === groupStatuses ? 'my-articles__chip--selected' : ''}`}
              onClick={() => selectGroup(groupStatuses)}
            >
              <span className={`cham cham--${group.color}`} aria-hidden="true" />
              {group.label}
            </button>
          )
        })}
      </div>

      <div className="my-articles__card">
        {error && <p className="thong-bao-loi" role="alert">{error}</p>}
        {!result && !error && <p className="my-articles__empty">Đang tải…</p>}

        {result && result.items.length === 0 && (
          <p className="my-articles__empty">Không có bài nào ở bộ lọc này.</p>
        )}

        {result && result.items.length > 0 && (
          <div className="my-articles__scroll">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th>Tác phẩm</th>
                  <th>Thể loại</th>
                  <th>Tính chất</th>
                  <th>Vai trò của bạn</th>
                  <th>Trạng thái</th>
                  <th>Ngày nộp</th>
                  <th>Cập nhật</th>
                </tr>
              </thead>
              <tbody>
                {result.items.map((article) => (
                  <tr key={article.id}>
                    <td>
                      <Link to={`/my-articles/${article.id}`} className="my-articles__article-title">{article.title}</Link>
                    </td>
                    {/* Ten the loai server lay qua GenreService; the loai da tat thi hien ma */}
                    <td>{article.categoryName || `Mã ${article.categoryId}`}</td>
                    <td className="my-articles__nowrap">{natureLabel(article.declaredNature)}</td>
                    <td className="my-articles__nowrap">
                      {article.myRole === 'MAIN_AUTHOR' ? (
                        <span className="my-articles__role my-articles__role--main">Tác giả chính</span>
                      ) : (
                        <span className="my-articles__role">Đồng tác giả</span>
                      )}
                      {article.mySharePercent != null && (
                        <span className="my-articles__share"> · {Number(article.mySharePercent)}%</span>
                      )}
                    </td>
                    <td>
                      <span className="my-articles__status">
                        <span className={`cham cham--${STATUS_COLORS[article.status]}`} aria-hidden="true" />
                        {statusLabel(article.status)}
                      </span>
                    </td>
                    <td className="my-articles__date">{formatDate(article.submittedAt)}</td>
                    <td className="my-articles__date">{formatDate(article.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Phan trang: chi hien khi co tu 2 trang tro len */}
        {result && result.totalPages > 1 && (
          <div className="my-articles__pagination">
            <button type="button" className="nut-nhe" disabled={page === 0}
              onClick={() => goToPage(page - 1)}>
              Trang trước
            </button>
            <span>Trang {result.page + 1} / {result.totalPages} · {result.totalElements} bài</span>
            <button type="button" className="nut-nhe" disabled={page + 1 >= result.totalPages}
              onClick={() => goToPage(page + 1)}>
              Trang sau
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
