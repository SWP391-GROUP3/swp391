import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getPendingGradings } from '../../api/royaltyApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { natureLabel } from '../../utils/royaltyLabels.js'
import '../../styles/Royalty.css'

/**
 * UC-46 - "Cham nhuan but" (Thu ky toa soan): bai da dang chua cham nhuan but.
 * Bai dang som nhat (gan han chi nhat) len dau. Loc theo ban, the loai, sap toi han.
 * Trang chu Thu ky mo /royalty-grading?sapToiHan=1 de chi xem bai sap toi han.
 * Bam "Cham" -> man cham nhuan but (UC-47, UC-48).
 *
 * MO TRANG NAY TAI: /royalty-grading
 *
 * MUC LUC (so dong - ten - tac dung)
 *   25  RoyaltyPendingList()            xem tom tat dau file
 *   33    useEffect                     tai du lieu khi mo trang
 *  134  DeadlineBadge()                 Han chi + so ngay con lai
 *  144  uniqueValues()                  Cac gia tri khac nhau cua 1 truong (vd ten ban), xep theo ABC, de lam o chon loc
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  120  link [Cham]                     mo trang /royalty-grading/{article.articleId} (RoyaltyGrading)
 */
export default function RoyaltyPendingList() {
  const [searchParams] = useSearchParams()
  const [articles, setArticles] = useState(null)   // null = dang tai
  const [loadError, setLoadError] = useState('')
  const [sectionFilter, setSectionFilter] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [onlyNearDeadline, setOnlyNearDeadline] = useState(searchParams.get('sapToiHan') === '1')

  useEffect(() => {
    let isMounted = true
    getPendingGradings()
      .then((data) => { if (isMounted) setArticles(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
    return () => { isMounted = false }
  }, [])

  // Loc o trinh duyet: danh sach it (vai chuc bai/thang) nen khong can goi lai server
  const allArticles = articles || []
  const sectionNames = uniqueValues(allArticles, 'sectionName')
  const categoryNames = uniqueValues(allArticles, 'categoryName')
  const shownArticles = allArticles.filter((article) =>
    (!sectionFilter || article.sectionName === sectionFilter)
    && (!categoryFilter || article.categoryName === categoryFilter)
    && (!onlyNearDeadline || article.nearDeadline))
  const nearDeadlineCount = allArticles.filter((article) => article.nearDeadline).length

  return (
    <div>
      <div className="royalty__header">
        <h1 className="royalty__title">Bài chờ chấm nhuận bút</h1>
        <p className="royalty__subtitle">
          Bài đã đăng nhưng chưa chấm nhuận bút. Bài đăng sớm nhất (gần hạn chi nhất) xếp trước.
        </p>
      </div>

      <div className="royalty__card">
        <div className="royalty__filters">
          <label className="royalty__field">
            Ban
            <select className="royalty__input" value={sectionFilter}
                    onChange={(event) => setSectionFilter(event.target.value)}>
              <option value="">Tất cả ban</option>
              {sectionNames.map((name) => <option key={name} value={name}>{name}</option>)}
            </select>
          </label>
          <label className="royalty__field">
            Thể loại
            <select className="royalty__input" value={categoryFilter}
                    onChange={(event) => setCategoryFilter(event.target.value)}>
              <option value="">Tất cả thể loại</option>
              {categoryNames.map((name) => <option key={name} value={name}>{name}</option>)}
            </select>
          </label>
          <label className="royalty__checkbox">
            <input type="checkbox" checked={onlyNearDeadline}
                   onChange={(event) => setOnlyNearDeadline(event.target.checked)} />
            Chỉ bài sắp tới hạn chi ({nearDeadlineCount})
          </label>
        </div>

        {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
        {!articles && !loadError && <p className="royalty__empty">Đang tải…</p>}
        {articles && shownArticles.length === 0 && (
          <p className="royalty__empty">
            {allArticles.length === 0 ? 'Không còn bài nào chờ chấm nhuận bút.' : 'Không có bài nào khớp bộ lọc.'}
          </p>
        )}

        {shownArticles.length > 0 && (
          <div className="royalty__scroll">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th>Bài</th>
                  <th>Ban</th>
                  <th>Thể loại</th>
                  <th>Tính chất PV khai</th>
                  <th>Ngày đăng</th>
                  <th>Hạn chi</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {shownArticles.map((article) => (
                  <tr key={article.articleId}>
                    <td className="royalty__title-cell">
                      <div className="royalty__strong">{article.title}</div>
                      <div className="royalty__muted">{article.authorName}</div>
                    </td>
                    <td className="royalty__nowrap">{article.sectionName}</td>
                    <td className="royalty__nowrap">{article.categoryName}</td>
                    <td className="royalty__nowrap">{natureLabel(article.declaredNature)}</td>
                    <td className="royalty__date">{formatDate(article.publishedAt)}</td>
                    <td><DeadlineBadge article={article} /></td>
                    <td className="royalty__actions">
                      <Link to={`/royalty-grading/${article.articleId}`} className="nut-chinh-fm">Chấm</Link>
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

/** Han chi + so ngay con lai. Qua han: do; sap toi han (nearDeadline): cam; con lai: xam. */
function DeadlineBadge({ article }) {
  const deadline = formatDate(article.paymentDeadline)
  if (article.daysLeft < 0) {
    return <span className="royalty__deadline royalty__deadline--late">{deadline} · quá hạn {-article.daysLeft} ngày</span>
  }
  const className = article.nearDeadline ? 'royalty__deadline royalty__deadline--soon' : 'royalty__deadline'
  return <span className={className}>{deadline} · còn {article.daysLeft} ngày</span>
}

/** Cac gia tri khac nhau cua 1 truong (vd ten ban), xep theo ABC, de lam o chon loc. */
function uniqueValues(items, fieldName) {
  const values = new Set(items.map((item) => item[fieldName]))
  return [...values].sort((a, b) => a.localeCompare(b, 'vi'))
}
