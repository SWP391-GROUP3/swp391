import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { layDotChamCheo, layBaiChamCheoHopLe, taoDotChamCheo } from '../../api/crossReviewApi.js'
import '../../styles/QualityReportPage.css'
import './ReviewWorkflow.css'

export default function CrossReviewBatchPage() {
  const dieuHuong = useNavigate()
  const [cacDot, setCacDot] = useState(null)
  const [cacBai, setCacBai] = useState(null)
  const [daChon, setDaChon] = useState({})
  const [tenDot, setTenDot] = useState('')
  const [tuNgay, setTuNgay] = useState(() => { const ngay = new Date(); ngay.setDate(ngay.getDate() - 7); return ngay.toLocaleDateString('en-CA') })
  const [denNgay, setDenNgay] = useState(() => new Date().toLocaleDateString('en-CA'))
  const [loi, setLoi] = useState('')
  const [dangLuu, setDangLuu] = useState(false)
  useEffect(() => {
    let conHienThi = true
    layDotChamCheo().then((data) => { if (conHienThi) setCacDot(data) }).catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [])

  async function taiBai(e) {
    e.preventDefault(); setDangLuu(true); setLoi(''); setCacBai(null); setDaChon({})
    try { setCacBai(await layBaiChamCheoHopLe(tuNgay, denNgay)) }
    catch (error) { setLoi(error.message) }
    finally { setDangLuu(false) }
  }
  function doiLuaChon(id, ten, giaTri) {
    setDaChon((cu) => ({ ...cu, [id]: { ...cu[id], [ten]: giaTri } }))
  }
  async function taoDot(e) {
    e.preventDefault()
    const tasks = Object.entries(daChon).filter(([, luaChon]) => luaChon.reviewerId)
      .map(([id, luaChon]) => ({ originalScoringId: Number(id), reviewerId: Number(luaChon.reviewerId), reason: luaChon.reason?.trim() || '' }))
    if (!tasks.length) { setLoi('Chọn ít nhất một bài và người chấm.'); return }
    if (!window.confirm(`Tạo đợt chấm chéo gồm ${tasks.length} bài?`)) return
    setDangLuu(true); setLoi('')
    try {
      const dot = await taoDotChamCheo({ batchName: tenDot.trim(), periodStart: tuNgay, periodEnd: denNgay, tasks })
      dieuHuong(`/cross-reviews/${dot.id}`)
    } catch (error) { setLoi(error.message) }
    finally { setDangLuu(false) }
  }
  function doiNgay(hamDoi, giaTri) { hamDoi(giaTri); setCacBai(null); setDaChon({}) }

  return <section className="bao-cao-qc luong-qc">
    <header className="bao-cao-qc__dau"><h1>Đợt chấm chéo</h1><p>Đánh giá độ lệch người chấm. Kết quả chấm chéo không thay đổi điểm chính thức của bài.</p></header>
    {loi && <p role="alert" className="thong-bao-loi">{loi}</p>}
    <section className="luong-qc__the"><h2>Tạo đợt mới</h2>
      <form className="bao-cao-qc__bo-loc" onSubmit={taiBai}>
        <label>Từ ngày<input type="date" value={tuNgay} required disabled={dangLuu} onChange={(e) => doiNgay(setTuNgay, e.target.value)} /></label>
        <label>Đến ngày<input type="date" value={denNgay} min={tuNgay} required disabled={dangLuu} onChange={(e) => doiNgay(setDenNgay, e.target.value)} /></label>
        <button type="submit" disabled={dangLuu}>Tìm bài đã chấm</button>
      </form>
      {cacBai && <form className="luong-qc__form" onSubmit={taoDot}>
        <label>Tên đợt (duy nhất)<input required maxLength={100} value={tenDot} disabled={dangLuu} onChange={(e) => setTenDot(e.target.value)} /></label>
        {!cacBai.length && <p>Không có phiếu chấm hợp lệ trong khoảng ngày này.</p>}
        {cacBai.map((bai) => <div className="luong-qc__tieu-chi" key={bai.originalScoringId}>
          <strong>{bai.title} · Phiếu #{bai.originalScoringId}</strong>
          {bai.reviewers.length ? <>
            <label>Người chấm (để trống nếu bỏ qua bài)<select disabled={dangLuu} value={daChon[bai.originalScoringId]?.reviewerId || ''}
              onChange={(e) => doiLuaChon(bai.originalScoringId, 'reviewerId', e.target.value)}>
              <option value="">Không chọn bài này</option>{bai.reviewers.map((nguoi) => <option key={nguoi.reviewerId} value={nguoi.reviewerId}>{nguoi.reviewerName} · {nguoi.sectionName}{nguoi.crossDepartment ? ' · Khác ban' : ''}</option>)}
            </select></label>
            <label>Lý do khi phân công khác ban<input maxLength={1000} disabled={dangLuu} value={daChon[bai.originalScoringId]?.reason || ''}
              onChange={(e) => doiLuaChon(bai.originalScoringId, 'reason', e.target.value)} /></label>
          </> : <p>Chưa có người chấm hợp lệ.</p>}
        </div>)}
        <button disabled={dangLuu || !cacBai.length} type="submit">{dangLuu ? 'Đang xử lý…' : 'Tạo đợt chấm chéo'}</button>
      </form>}
    </section>
    <section className="luong-qc__the"><h2>Các đợt trong ban</h2>
      {!cacDot && <p>Đang tải danh sách…</p>}
      {cacDot?.length === 0 && <p>Chưa có đợt chấm chéo.</p>}
      <ul>{cacDot?.map((dot) => <li key={dot.id}><Link to={`/cross-reviews/${dot.id}`}>{dot.batchName}</Link> · {dot.status === 'OPEN' ? 'Đang mở' : 'Đã đóng'} · {dot.tasks.filter((task) => task.status === 'DONE').length}/{dot.tasks.length} bài đã chấm</li>)}</ul>
    </section>
  </section>
}
