import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getMyCoAuthorInvites } from '../../api/articleApi.js'
import { formatDate, daysUntil } from '../../utils/formatDate.js'
import '../../styles/CoAuthorInvites.css'

/**
 * UC-12 (phan xem) - "Xac nhan dong tac gia": bai dang cho nguoi dung xac nhan
 * ty le dong tac gia, sap het han xep truoc. Bam tieu de de xem bai (UC-02).
 * Nut Dong y / Tu choi lam o buoc sau cua Iter 2.
 *
 * MUC LUC (so dong)
 *   17  CoAuthorInvites              - UC-12 (phan xem) - "Xac nhan dong tac gia": bai dang cho nguoi dung xac…
 *   21    useEffect                  - tai du lieu khi mo trang
 *   37    deadlineText()             - Han xac nhan 3 ngay (BR-IN-06): hien so ngay con lai cho de thay
 */
export default function CoAuthorInvites() {
  const [invites, setInvites] = useState(null)   // null = dang tai
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true

    getMyCoAuthorInvites()
      .then((data) => {
        if (isMounted) {
          setInvites(data)
          setError('')
        }
      })
      .catch((err) => { if (isMounted) setError(err.message) })

    return () => { isMounted = false }
  }, [])

  /** Han xac nhan 3 ngay (BR-IN-06): hien so ngay con lai cho de thay */
  function deadlineText(confirmDeadline) {
    if (!confirmDeadline) {
      return '—'
    }
    const daysLeft = daysUntil(confirmDeadline)
    if (daysLeft <= 0) {
      return `${formatDate(confirmDeadline)} (quá hạn)`
    }
    return `${formatDate(confirmDeadline)} (còn ${daysLeft} ngày)`
  }

  return (
    <div className="co-author-invites">
      <div className="co-author-invites__header">
        <h1 className="co-author-invites__title">Xác nhận đồng tác giả</h1>
        <p className="co-author-invites__subtitle">
          Bài đang chờ bạn đồng ý tỷ lệ đồng tác giả, sắp hết hạn xếp trước. Quá hạn 3 ngày bài sẽ về Nháp.
        </p>
      </div>

      <div className="co-author-invites__card">
        {error && <p className="thong-bao-loi" role="alert">{error}</p>}
        {!invites && !error && <p className="co-author-invites__empty">Đang tải…</p>}

        {invites && invites.length === 0 && (
          <p className="co-author-invites__empty">Không có bài nào đang chờ bạn xác nhận.</p>
        )}

        {invites && invites.length > 0 && (
          <div className="co-author-invites__scroll">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th>Tác phẩm</th>
                  <th>Tác giả chính</th>
                  <th>Tỷ lệ của bạn</th>
                  <th>Hạn xác nhận</th>
                </tr>
              </thead>
              <tbody>
                {invites.map((invite) => (
                  <tr key={invite.articleId}>
                    <td>
                      <Link to={`/my-articles/${invite.articleId}`} className="co-author-invites__article-title">
                        {invite.title}
                      </Link>
                    </td>
                    <td>{invite.mainAuthorName || `Người dùng #${invite.mainAuthorId}`}</td>
                    <td className="co-author-invites__nowrap">{Number(invite.sharePercent)}%</td>
                    <td className="co-author-invites__nowrap">{deadlineText(invite.confirmDeadline)}</td>
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
