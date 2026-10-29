import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { layPhieuChamMoiNhat } from '../../api/assessmentApi.js'
import { formatDate } from '../../utils/formatDate.js'
import './ArticleScoreCard.css'

/**
 * Component hiển thị Bảng điểm đánh giá chất lượng theo từng tiêu chí (UC-34).
 * Dành cho Phóng viên (tác giả chính & đồng tác giả) xem điểm khi bài đã SCORE_PUBLISHED hoặc PUBLISHED.
 * Kết nối luồng BF-02 sang BF-03 (cửa sổ 7 ngày khiếu nại).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  ArticleScoreCard()              xem tom tat dau file
 *   24    useEffect                     tai du lieu khi mo trang
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  120  link [Gui khieu nai diem nay]   mo trang /my-appeals/new?articleId={articleId} (AppealCreatePage)
 */
export default function ArticleScoreCard({ articleId }) {
  const [phieuCham, setPhieuCham] = useState(null)
  const [dangTai, setDangTai] = useState(false)
  const [loi, setLoi] = useState('')

  useEffect(() => {
    if (!articleId) return

    let isMounted = true
    setDangTai(true)
    setLoi('')

    layPhieuChamMoiNhat(articleId)
      .then((data) => {
        if (isMounted) {
          setPhieuCham(data)
        }
      })
      .catch((err) => {
        if (isMounted) {
          setLoi(err.message || 'Không thể tải bảng điểm đánh giá')
        }
      })
      .finally(() => {
        if (isMounted) setDangTai(false)
      })

    return () => { isMounted = false }
  }, [articleId])

  if (dangTai) {
    return (
      <div className="bang-diem-pv">
        <div className="bang-diem-pv__the">
          <div className="bang-diem-pv__tai">Đang tải bảng điểm đánh giá chất lượng...</div>
        </div>
      </div>
    )
  }

  if (loi || !phieuCham) {
    return null
  }

  const chiTiet = phieuCham.details || []
  const tongDiem = Number(phieuCham.totalScore || 0).toFixed(2)

  return (
    <div className="bang-diem-pv">
      <div className="bang-diem-pv__the">
        <div className="bang-diem-pv__dau">
          <div>
            <h3 className="bang-diem-pv__tieu-de">Kết quả đánh giá chất lượng</h3>
            <p className="bang-diem-pv__mo-ta">
              Biên tập viên thẩm định: <strong>{phieuCham.reviewerName || 'Biên tập viên'}</strong>
              {phieuCham.announcedAt && (
                <span> · Công bố ngày {formatDate(phieuCham.announcedAt)}</span>
              )}
            </p>
          </div>

          <div className="bang-diem-pv__khoi-diem">
            <div>
              <span className="bang-diem-pv__diem-so">{tongDiem}</span>
              <span className="bang-diem-pv__thang-diem"> / 100 điểm</span>
            </div>
          </div>
        </div>

        {/* Khối Cửa sổ khiếu nại 7 ngày (BR-SC-10 / BR-QC-01) */}
        {phieuCham.announcedAt && (
          <div
            className={`bang-diem-pv__banner-khieu-nai ${
              phieuCham.isAppealWindowOpen
                ? 'bang-diem-pv__banner-khieu-nai--mo'
                : 'bang-diem-pv__banner-khieu-nai--het-han'
            }`}
          >
            <div className="bang-diem-pv__thong-tin-khieu-nai">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <path d="M12 6v6l4 2" />
              </svg>
              <div>
                {phieuCham.isAppealWindowOpen ? (
                  <>
                    <strong>Thời hạn khiếu nại: </strong>
                    Bạn có quyền gửi khiếu nại chất lượng chấm điểm trong vòng 7 ngày kể từ khi công bố điểm (còn{' '}
                    <strong>{phieuCham.daysRemaining} ngày</strong>).
                  </>
                ) : (
                  <>
                    <strong>Thời hạn khiếu nại đã kết thúc: </strong>
                    Bài viết đã quá thời hạn khiếu nại 7 ngày kể từ thời điểm công bố điểm ({formatDate(phieuCham.announcedAt)}).
                  </>
                )}
              </div>
            </div>

            {phieuCham.isAppealWindowOpen && (
              <Link
                to={`/my-appeals/new?articleId=${articleId}`}
                className="bang-diem-pv__nut-khieu-nai"
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M12 3v18M5 21h14M5 7h14M5 7l-3 7a3 3 0 0 0 6 0l-3-7Z" />
                </svg>
                Gửi khiếu nại điểm này
              </Link>
            )}
          </div>
        )}

        {/* Bảng điểm chi tiết theo tiêu chí */}
        <div className="bang-diem-pv__bang-bao">
          <table className="bang-diem-pv__bang">
            <thead>
              <tr>
                <th style={{ width: '40px' }}>STT</th>
                <th>Tiêu chí đánh giá</th>
                <th style={{ width: '100px', textAlign: 'center' }}>Trọng số</th>
                <th style={{ width: '120px', textAlign: 'center' }}>Điểm chấm</th>
                <th style={{ width: '130px', textAlign: 'center' }}>Quy đổi</th>
                <th>Nhận xét của Biên tập viên</th>
              </tr>
            </thead>
            <tbody>
              {chiTiet.map((item, index) => {
                const diem = Number(item.score || 0)
                const trongSo = Number(item.weight || 0)
                const quyDoi = ((diem * trongSo) / 100).toFixed(2)
                const duoi50 = diem < 50

                return (
                  <tr key={item.id || index}>
                    <td style={{ color: 'var(--fm-chu-nhat)', textAlign: 'center' }}>{index + 1}</td>
                    <td>
                      <div className="bang-diem-pv__ten-tieu-chi">{item.criterionName}</div>
                    </td>
                    <td style={{ textAlign: 'center' }}>{trongSo}%</td>
                    <td style={{ textAlign: 'center' }}>
                      <strong style={{ color: duoi50 ? '#dc2626' : 'inherit' }}>
                        {diem.toFixed(2)}
                      </strong>
                    </td>
                    <td style={{ textAlign: 'center', color: '#0284c7', fontWeight: 600 }}>
                      {quyDoi}
                    </td>
                    <td>
                      {duoi50 && (
                        <span className="bang-diem-pv__canh-bao-diem-thap">Dưới 50đ</span>
                      )}
                      <span className="bang-diem-pv__nhan-xet-tieu-chi">
                        {item.comment || <span style={{ color: 'var(--fm-chu-nhat)', fontStyle: 'italic' }}>Không có nhận xét</span>}
                      </span>
                    </td>
                  </tr>
                )
              })}
              <tr className="bang-diem-pv__dong-tong-ket">
                <td colSpan={2}>Tổng kết chất lượng bài viết</td>
                <td style={{ textAlign: 'center' }}>100%</td>
                <td style={{ textAlign: 'center' }}>—</td>
                <td style={{ textAlign: 'center', color: '#0284c7', fontSize: '15px' }}>
                  {tongDiem}
                </td>
                <td>
                  Quyết định: <strong>{phieuCham.decision || 'Chấp thuận'}</strong>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Ghi chú chung của BTV */}
        {phieuCham.decisionNote && (
          <div className="bang-diem-pv__ghi-chu-btv">
            <strong>Ghi chú của Biên tập viên: </strong>
            {phieuCham.decisionNote}
          </div>
        )}
      </div>
    </div>
  )
}
