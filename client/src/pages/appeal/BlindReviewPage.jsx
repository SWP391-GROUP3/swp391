import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { layChiTietChamMu, luuNhapChamMu, nopPhieuChamMu } from '../../api/appealApi.js'
import { layChiTietChamCheo, nopPhieuChamCheo } from '../../api/crossReviewApi.js'
import { tinhTongTam, kiemTraPhieuCham } from './blindScorecard.js'
import '../../styles/QualityReportPage.css'
import './ReviewWorkflow.css'

export default function BlindReviewPage({ cheo = false }) {
  const { id } = useParams()
  return <BlindReviewForm key={`${cheo}/${id}`} id={id} cheo={cheo} />
}

function BlindReviewForm({ id, cheo }) {
  const [phieu, setPhieu] = useState(null)
  const [cacDong, setCacDong] = useState([])
  const [loi, setLoi] = useState('')
  const [thongBao, setThongBao] = useState('')
  const [dangLuu, setDangLuu] = useState(false)
  const daNop = Boolean(phieu?.submittedAt)

  useEffect(() => {
    let conHienThi = true
    const tai = cheo ? layChiTietChamCheo : layChiTietChamMu
    tai(id).then((data) => {
      if (!conHienThi) return
      setPhieu(data)
      setCacDong(data.criteria.map((dong) => ({ ...dong, score: dong.score ?? '', comment: dong.comment ?? '' })))
    }).catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [id, cheo])

  function doiDong(criterionId, ten, giaTri) {
    setCacDong((cu) => cu.map((dong) => dong.criterionId === criterionId ? { ...dong, [ten]: giaTri } : dong))
  }

  async function luuPhieu(nop) {
    if (nop) {
      const loiNhap = kiemTraPhieuCham(cacDong)
      if (loiNhap) { setLoi(loiNhap); return }
      if (!window.confirm('Nộp phiếu chấm? Sau khi nộp bạn không thể sửa điểm.')) return
    }
    const criteria = cacDong.map((dong) => ({ criterionId: dong.criterionId, score: dong.score === '' ? null : Number(dong.score), comment: dong.comment }))
    setDangLuu(true); setLoi(''); setThongBao('')
    try {
      const gui = cheo ? nopPhieuChamCheo : nop ? nopPhieuChamMu : luuNhapChamMu
      const data = await gui(id, criteria)
      setPhieu(data)
      if (!cheo) setCacDong(data.criteria.map((dong) => ({ ...dong, score: dong.score ?? '', comment: dong.comment ?? '' })))
      setThongBao(nop ? 'Đã nộp và khóa phiếu chấm.' : 'Đã lưu nháp.')
    } catch (error) {
      setLoi(error.message)
      if (error.status === 409 || error.status === 403) {
        try {
          const data = await (cheo ? layChiTietChamCheo : layChiTietChamMu)(id)
          setPhieu(data)
          setCacDong(data.criteria.map((dong) => ({ ...dong, score: dong.score ?? '', comment: dong.comment ?? '' })))
        } catch { setPhieu(null); setCacDong([]) }
      }
    } finally { setDangLuu(false) }
  }

  return <section className="bao-cao-qc luong-qc">
    <Link to="/re-reviews">← Danh sách phiếu</Link>
    <header className="bao-cao-qc__dau"><h1>{cheo ? 'Phiếu chấm chéo' : 'Phiếu chấm lại mù'} #{id}</h1>
      <p>{cheo ? 'Điểm chỉ phục vụ phân tích chất lượng chấm.' : 'Chấm độc lập theo phiên bản bài và bộ tiêu chí được giao.'}</p></header>
    {loi && <p role="alert" className="thong-bao-loi">{loi}</p>}
    {thongBao && <p role="status">{thongBao}</p>}
    {!phieu && !loi && <p>Đang tải phiếu…</p>}
    {phieu && <>
      <p>Phiên bản bài #{phieu.versionId} · Thể loại #{phieu.categoryId} · Bộ tiêu chí #{phieu.criteriaSetVersionId}</p>
      <div className="luong-qc__hai-cot">
        <article className="luong-qc__the"><h2>{phieu.title}</h2><div className="luong-qc__noi-dung">{phieu.content}</div></article>
        <form className="luong-qc__form" onSubmit={(e) => { e.preventDefault(); luuPhieu(true) }}>
          {daNop && <p role="status">Phiếu đã nộp, chỉ đọc. Tổng điểm: {phieu.totalScore}/100.</p>}
          {(!cheo || !daNop) && cacDong.map((dong) => <fieldset key={dong.criterionId} className="luong-qc__tieu-chi" disabled={dangLuu || daNop}>
            <legend>{dong.name} · {dong.weight}%</legend>
            <label>Điểm (0–100)<input aria-label={`Điểm ${dong.name}`} type="number" min="0" max="100" step="0.01" value={dong.score}
              onChange={(e) => doiDong(dong.criterionId, 'score', e.target.value)} /></label>
            <label>Nhận xét {dong.score !== '' && Number(dong.score) < 50 ? '(bắt buộc dưới 50 điểm)' : ''}
              <textarea rows={3} maxLength={1000} value={dong.comment} onChange={(e) => doiDong(dong.criterionId, 'comment', e.target.value)} /></label>
          </fieldset>)}
          {!daNop && <>
            <p>Tổng điểm tạm tính: <strong>{tinhTongTam(cacDong) ?? 'Chưa đủ điểm'}</strong>. Server kiểm tra lại khi lưu/nộp.</p>
            <div className="luong-qc__thao-tac">
              {!cheo && <button type="button" disabled={dangLuu} onClick={() => luuPhieu(false)}>Lưu nháp</button>}
              <button type="submit" disabled={dangLuu}>{dangLuu ? 'Đang lưu…' : 'Nộp phiếu chấm'}</button>
            </div>
          </>}
        </form>
      </div>
    </>}
  </section>
}
