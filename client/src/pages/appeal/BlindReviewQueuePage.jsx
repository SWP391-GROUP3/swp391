import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { layPhieuChamMu } from '../../api/appealApi.js'
import { layPhieuChamCheo } from '../../api/crossReviewApi.js'
import '../../styles/QualityReportPage.css'
import './ReviewWorkflow.css'

/**
 * Man BTV xem cac phieu cham lai mu va cham cheo duoc giao (UC-36, UC-40), chia theo trang thai; bam
 * vao phieu de mo trang cham.
 *
 * MO TRANG NAY TAI: /re-reviews
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  BlindReviewQueuePage()          blind review queue page
 *   25    useEffect                     tai du lieu khi mo trang
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   39  link [# {phieu.id} · {phieu.title}]  mo trang {duongDan}{phieu.id} (khong co route)
 */

export default function BlindReviewQueuePage() {
  const [duLieu, setDuLieu] = useState(null)
  const [loi, setLoi] = useState('')
  useEffect(() => {
    let conHienThi = true
    Promise.all([layPhieuChamMu(), layPhieuChamCheo()])
      .then(([chamMu, chamCheo]) => { if (conHienThi) setDuLieu({ chamMu, chamCheo }) })
      .catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [])
  return <section className="bao-cao-qc luong-qc">
    <header className="bao-cao-qc__dau"><h1>Chấm lại &amp; chấm chéo</h1><p>Các phiếu được giao cho bạn. Phiếu đã nộp chỉ được xem lại.</p></header>
    {loi && <p role="alert" className="thong-bao-loi">{loi}</p>}
    {!duLieu && !loi && <p>Đang tải phiếu…</p>}
    {duLieu && [['chamMu', 'Chấm lại mù', '/re-reviews/'], ['chamCheo', 'Chấm chéo phân tích', '/cross-review-tasks/']].map(([ten, nhan, duongDan]) =>
      <section key={ten} className="luong-qc__the"><h2>{nhan}</h2>
        {!duLieu[ten].length && <p>Chưa có phiếu được giao.</p>}
        <ul>{duLieu[ten].map((phieu) => <li key={phieu.id}><Link to={`${duongDan}${phieu.id}`}>#{phieu.id} · {phieu.title}</Link> · {phieu.submittedAt ? 'Đã nộp' : 'Chờ chấm'}</li>)}</ul>
      </section>)}
  </section>
}
