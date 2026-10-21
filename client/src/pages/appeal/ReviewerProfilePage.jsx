import { useEffect, useState } from 'react'
import { Link, useLocation, useParams, useSearchParams } from 'react-router-dom'
import { layHoSoBienTapVien, layLichSuCham } from '../../api/reviewerProfileApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import '../../styles/QualityReportPage.css'
import './ReviewerProfilePage.css'

export default function ReviewerProfilePage() {
  const { id } = useParams()
  const location = useLocation()
  const [thamSo, doiThamSo] = useSearchParams()
  const homNay = new Date()
  const thang = thamSo.get('period') || `${homNay.getFullYear()}-${String(homNay.getMonth() + 1).padStart(2, '0')}`
  const soTrang = Number(thamSo.get('page') || 0)
  const trang = Number.isInteger(soTrang) && soTrang >= 0 ? soTrang : 0
  const khoaYeuCau = `${id}/${thang}/${trang}`
  const [ketQua, setKetQua] = useState({ khoa: null, duLieu: null, loi: '' })
  const duLieu = ketQua.khoa === khoaYeuCau ? ketQua.duLieu : null
  const loi = ketQua.khoa === khoaYeuCau ? ketQua.loi : ''

  useEffect(() => {
    let conHienThi = true
    Promise.all([layHoSoBienTapVien(id, thang), layLichSuCham(id, thang, trang)])
      .then(([hoSo, lichSu]) => { if (conHienThi) setKetQua({ khoa: khoaYeuCau, duLieu: { hoSo, lichSu }, loi: '' }) })
      .catch((error) => { if (conHienThi) setKetQua({ khoa: khoaYeuCau, duLieu: null, loi: error.message }) })
    return () => { conHienThi = false }
  }, [id, thang, trang, khoaYeuCau])

  const hoSo = duLieu?.hoSo
  const lichSu = duLieu?.lichSu
  const chiSo = hoSo?.deviation

  return (
    <section className="bao-cao-qc ho-so-btv">
      <Link to="/reviewers" state={{ boLoc: location.state?.boLoc }}>← Quay lại báo cáo độ lệch</Link>
      <header className="bao-cao-qc__dau">
        <h1>Hồ sơ &amp; lịch sử chấm biên tập viên</h1>
        <p>Thông tin chỉ đọc, dùng để tham khảo phân công; không tự xếp hạng hoặc xử phạt.</p>
      </header>
      <div className="bao-cao-qc__bo-loc">
        <label>Tháng <input type="month" aria-label="Tháng hồ sơ" value={thang}
          onChange={(e) => doiThamSo({ period: e.target.value, page: '0' }, { state: location.state })} /></label>
      </div>
      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {!duLieu && !loi && <p>Đang tải hồ sơ…</p>}
      {hoSo && <>
        <dl className="ho-so-btv__thong-tin">
          <div><dt>Biên tập viên</dt><dd>{hoSo.editorName} (#{hoSo.editorId})</dd></div>
          <div><dt>Ban</dt><dd>{hoSo.sectionName || 'Chưa có ban'}</dd></div>
          <div><dt>Tài khoản</dt><dd>{hoSo.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã khóa'}</dd></div>
          <div><dt>Bài chấm gốc đang xử lý</dt><dd>{hoSo.activeArticleCount}</dd></div>
          <div><dt>Phiếu chấm chéo đang chờ</dt><dd>{hoSo.pendingCrossReviewCount}</dd></div>
          <div><dt>Phiếu khiếu nại đang chờ</dt><dd>{hoSo.pendingAppealReviewCount}</dd></div>
        </dl>
        <h2>Chỉ số chất lượng trong tháng</h2>
        {chiSo ? <dl className="ho-so-btv__thong-tin">
          <div><dt>Độ lệch tuyệt đối TB</dt><dd>{formatNumber(chiSo.avgDeviation)} điểm</dd></div>
          <div><dt>Khiếu nại đã giải quyết</dt><dd>{chiSo.appealCount}</dd></div>
          <div><dt>Tỷ lệ đổi điểm</dt><dd>{formatNumber(chiSo.overturnRate)}%</dd></div>
          <div><dt>Phiếu chấm gốc đã nộp</dt><dd>{chiSo.workloadCount}</dd></div>
        </dl> : <p>Chưa có dữ liệu so sánh trong tháng này. Không suy ra độ lệch bằng 0.</p>}
        <h2>Lịch sử chấm đã nộp</h2>
        {lichSu.content.length === 0 ? <p>Chưa có lần chấm trong trang/kỳ đã chọn.</p> :
          <div className="bao-cao-qc__bang-boc"><table className="bao-cao-qc__bang">
            <thead><tr><th>Phiếu</th><th>Bài viết</th><th>Loại chấm</th><th>Điểm</th><th>Ngày nộp</th></tr></thead>
            <tbody>{lichSu.content.map((lan) => <tr key={lan.attemptId}>
              <td>#{lan.attemptId}</td><td>{lan.articleTitle}</td>
              <td>{({ INITIAL: 'Lần đầu', RESCORE: 'Chấm lại', APPEAL: 'Khiếu nại' })[lan.attemptType] || lan.attemptType}</td>
              <td>{formatNumber(lan.totalScore)}</td><td>{formatDate(lan.submittedAt)}</td>
            </tr>)}</tbody>
          </table></div>}
        <nav className="ho-so-btv__phan-trang" aria-label="Phân trang lịch sử chấm">
          <button type="button" disabled={trang === 0} onClick={() => doiThamSo({ period: thang, page: String(trang - 1) }, { state: location.state })}>Trang trước</button>
          <span>Trang {trang + 1} / {Math.max(1, lichSu.totalPages)} · {lichSu.totalElements} lần chấm</span>
          <button type="button" disabled={trang + 1 >= lichSu.totalPages}
            onClick={() => doiThamSo({ period: thang, page: String(trang + 1) }, { state: location.state })}>Trang sau</button>
        </nav>
        <h2>Lịch sử chấm chéo đã hoàn thành</h2>
        {hoSo.crossReviews.length === 0 ? <p>Chưa có phiếu chấm chéo hoàn thành trong kỳ.</p> :
          <div className="bao-cao-qc__bang-boc"><table className="bao-cao-qc__bang">
            <thead><tr><th>Phiếu</th><th>Mã bài</th><th>Điểm chấm chéo</th><th>Chênh lệch có dấu</th><th>Ngày nộp</th></tr></thead>
            <tbody>{hoSo.crossReviews.map((phieu) => <tr key={phieu.id}>
              <td>#{phieu.id}</td><td>#{phieu.articleId}</td><td>{formatNumber(phieu.score)}</td>
              <td>{formatNumber(phieu.deviation)}</td><td>{formatDate(phieu.submittedAt)}</td>
            </tr>)}</tbody>
          </table></div>}
      </>}
    </section>
  )
}
