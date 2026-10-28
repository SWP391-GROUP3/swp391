import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { layChiTietDotCheo, dongDotChamCheo, goBaiChamCheo } from '../../api/crossReviewApi.js'
import '../../styles/QualityReportPage.css'
import './ReviewWorkflow.css'

/**
 * Man Truong ban xem 1 dot cham cheo (UC-40): danh sach bai va BTV duoc giao, tien do nop phieu; go
 * bai khoi dot hoac dong dot.
 *
 * MO TRANG NAY TAI: /cross-reviews/:id
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  CrossReviewBatchDetailPage()    cross review batch detail page
 *   30  BatchDetail()                   batch detail
 *   35    useEffect                     tai du lieu khi mo trang
 *   40    xuLy()                        xu ly
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   49  link [← Cac dot cham cheo]      mo trang /cross-reviews (CrossReviewBatchPage)
 *   59  [Go bai]                        xuLy() -> hoi xac nhan; goi DELETE /api/cross-review-batches/{id}/tasks/{taskId}
 *   63  [Dong dot]                      xuLy() -> hoi xac nhan; goi POST /api/cross-review-batches/{id}/close
 *   64  link [Nhat ky dot]              mo trang /audit-logs?entityType=cross_review_batches&entityId={id} (AuditLogPage)
 */

export default function CrossReviewBatchDetailPage() {
  const { id } = useParams()
  return <BatchDetail key={id} id={id} />
}
function BatchDetail({ id }) {
  const [duLieu, setDuLieu] = useState(null)
  const [loi, setLoi] = useState('')
  const [lanTai, setLanTai] = useState(0)
  const [dangLuu, setDangLuu] = useState(false)
  useEffect(() => {
    let conHienThi = true
    layChiTietDotCheo(id).then((data) => { if (conHienThi) setDuLieu(data) }).catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [id, lanTai])
  async function xuLy(thaoTac, cauHoi) {
    if (!window.confirm(cauHoi)) return
    setDangLuu(true); setLoi('')
    try { setDuLieu(await thaoTac()) }
    catch (error) { setLoi(error.message); if (error.status === 409) setLanTai((cu) => cu + 1) }
    finally { setDangLuu(false) }
  }
  const daXong = duLieu?.tasks.every((task) => task.status === 'DONE')
  return <section className="bao-cao-qc luong-qc">
    <Link to="/cross-reviews">← Các đợt chấm chéo</Link>
    <header className="bao-cao-qc__dau"><h1>{duLieu?.batchName || `Đợt chấm chéo #${id}`}</h1></header>
    {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
    {!duLieu && !loi && <p>Đang tải đợt…</p>}
    {duLieu && <>
      <p>Khoảng lấy mẫu: {duLieu.periodStart} → {duLieu.periodEnd} · {duLieu.status === 'OPEN' ? 'Đang mở' : 'Đã đóng'}</p>
      <div className="bao-cao-qc__bang-boc"><table className="luong-qc__bang"><thead><tr><th>Bài / phiếu</th><th>BTV</th><th>Trạng thái</th><th>Điểm phân tích</th><th>Độ lệch có dấu</th><th>Thao tác</th></tr></thead>
        <tbody>{duLieu.tasks.map((task) => <tr key={task.id}>
          <td>{task.title}<br />Task #{task.id} · Phiếu gốc #{task.originalScoringId}</td>
          <td>#{task.reviewerId}</td><td>{task.status === 'DONE' ? 'Đã nộp' : 'Chờ chấm'}</td><td>{task.score ?? '—'}</td><td>{task.deviation ?? '—'}</td>
          <td>{duLieu.status === 'OPEN' && task.status === 'PENDING' && <button disabled={dangLuu} onClick={() => xuLy(() => goBaiChamCheo(id, task.id), 'Gỡ bài chưa nộp khỏi đợt? Thao tác được lưu nhật ký.')}>Gỡ bài</button>}</td>
        </tr>)}</tbody></table></div>
      {!duLieu.tasks.length && <p>Đợt không còn bài chấm.</p>}
      {duLieu.status === 'OPEN' && <><p>{daXong ? 'Các bài đã hoàn tất, có thể đóng đợt.' : 'Chỉ đóng đợt khi mọi bài đã được nộp.'}</p>
        <button disabled={dangLuu || !daXong} onClick={() => xuLy(() => dongDotChamCheo(id), 'Đóng đợt chấm chéo?')}>Đóng đợt</button></>}
      <Link to={`/audit-logs?entityType=cross_review_batches&entityId=${id}`}>Nhật ký đợt</Link>
    </>}
  </section>
}
