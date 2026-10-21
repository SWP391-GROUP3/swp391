import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { layHangChoKhieuNai } from '../../api/appealApi.js'
import { formatDate } from '../../utils/formatDate.js'
import '../../styles/QualityReportPage.css'
import './AppealManagementPage.css'

const tenTrangThai = { SUBMITTED: 'Đã gửi', IN_REVIEW: 'Đang chấm lại', RESOLVED: 'Đã giải quyết' }

export default function AppealManagementPage() {
  const [thamSo, doiThamSo] = useSearchParams()
  const khoa = thamSo.toString()
  const [ketQua, setKetQua] = useState({ khoa: null, duLieu: null, loi: '' })
  const duLieu = ketQua.khoa === khoa ? ketQua.duLieu : null
  const loi = ketQua.khoa === khoa ? ketQua.loi : ''

  useEffect(() => {
    let conHienThi = true
    layHangChoKhieuNai(Object.fromEntries(new URLSearchParams(khoa)))
      .then((data) => { if (conHienThi) setKetQua({ khoa, duLieu: data, loi: '' }) })
      .catch((error) => { if (conHienThi) setKetQua({ khoa, duLieu: null, loi: error.message }) })
    return () => { conHienThi = false }
  }, [khoa])

  function locDanhSach(e) {
    e.preventDefault()
    const moi = new URLSearchParams()
    for (const [ten, giaTri] of new FormData(e.currentTarget)) if (giaTri) moi.set(ten, giaTri)
    moi.set('page', '0')
    doiThamSo(moi)
  }

  function doiTrang(trang) {
    const moi = new URLSearchParams(thamSo)
    moi.set('page', trang)
    doiThamSo(moi)
  }

  return <section className="bao-cao-qc quan-ly-kn">
    <header className="bao-cao-qc__dau">
      <h1>Quản lý khiếu nại</h1>
      <p>Phân công và giao lại phiếu chấm mù trong ban. Không thay đổi điểm gốc hoặc trạng thái xuất bản.</p>
    </header>
    <form key={khoa} className="bao-cao-qc__bo-loc" onSubmit={locDanhSach}>
      <label>Trạng thái<select name="status" defaultValue={thamSo.get('status') || ''}>
        <option value="">Tất cả</option>
        {Object.entries(tenTrangThai).map(([ma, ten]) => <option key={ma} value={ma}>{ten}</option>)}
      </select></label>
      <label>Mã bài<input name="articleId" type="number" min="1" defaultValue={thamSo.get('articleId') || ''} /></label>
      <label>Mã BTV đang được giao<input name="reviewerId" type="number" min="1" defaultValue={thamSo.get('reviewerId') || ''} /></label>
      <label>Từ ngày<input name="fromDate" type="date" defaultValue={thamSo.get('fromDate') || ''} /></label>
      <label>Đến ngày<input name="toDate" type="date" defaultValue={thamSo.get('toDate') || ''} /></label>
      <button type="submit">Lọc danh sách</button>
      <button type="button" onClick={() => doiThamSo({})}>Xóa bộ lọc</button>
    </form>
    {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
    {!duLieu && !loi && <p>Đang tải khiếu nại…</p>}
    {duLieu && <>
      <div className="bao-cao-qc__bang-boc"><table className="bao-cao-qc__bang">
        <thead><tr><th>Khiếu nại / bài viết</th><th>Phóng viên</th><th>Gửi lúc</th><th>Cửa sổ gửi</th><th>Người đang chấm</th><th>Trạng thái</th></tr></thead>
        <tbody>{duLieu.content.map((dong) => <tr key={dong.id}>
          <td><Link to={`/appeals/${dong.id}`} state={{ danhSach: khoa }}>#{dong.id} · {dong.articleTitle}</Link></td>
          <td>{dong.appellantName}</td><td>{formatDate(dong.submittedAt)}</td>
          <td>{dong.deadlineExpired ? 'Đã hết hạn gửi' : 'Còn hạn gửi'}<br />{formatDate(dong.appealDeadline)}</td>
          <td>{dong.currentAttempt ? `BTV #${dong.currentAttempt.reviewerId}` : 'Chưa phân công'}</td>
          <td>{tenTrangThai[dong.status] || dong.status}</td>
        </tr>)}</tbody>
      </table></div>
      {!duLieu.content.length && <p>Không có khiếu nại phù hợp.</p>}
      <div className="quan-ly-kn__phan-trang">
        <button disabled={duLieu.number === 0} onClick={() => doiTrang(duLieu.number - 1)}>Trang trước</button>
        <span>Trang {duLieu.number + 1}/{Math.max(duLieu.totalPages, 1)} · {duLieu.totalElements} khiếu nại</span>
        <button disabled={duLieu.last} onClick={() => doiTrang(duLieu.number + 1)}>Trang sau</button>
      </div>
      <p>Hạn trên là hạn phóng viên gửi khiếu nại, không phải hạn Trưởng ban xử lý.</p>
    </>}
  </section>
}
