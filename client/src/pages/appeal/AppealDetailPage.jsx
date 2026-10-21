import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { layChiTietKhieuNai, layNguoiChamHopLe, phanCongKhieuNai, giaoLaiKhieuNai } from '../../api/appealApi.js'
import { formatDate } from '../../utils/formatDate.js'
import '../../styles/QualityReportPage.css'
import './AppealManagementPage.css'
import './AppealDetailPage.css'

export default function AppealDetailPage() {
  const { id } = useParams()
  const location = useLocation()
  const [lanTai, setLanTai] = useState(0)
  const khoa = `${id}/${lanTai}`
  const [ketQua, setKetQua] = useState({ khoa: null, duLieu: null, loi: '' })
  const [dangLuu, setDangLuu] = useState(false)
  const [thongBao, setThongBao] = useState({ id: null, loi: '', thanhCong: '' })
  const duLieu = ketQua.khoa === khoa ? ketQua.duLieu : null
  const loi = ketQua.khoa === khoa ? ketQua.loi : ''
  const khieuNai = duLieu?.khieuNai
  const nguoiCham = duLieu?.nguoiCham || []
  const phieu = khieuNai?.currentAttempt
  const coTheGiao = khieuNai && (khieuNai.status === 'SUBMITTED' || (khieuNai.status === 'IN_REVIEW' && phieu && !phieu.submittedAt))

  useEffect(() => {
    let conHienThi = true
    Promise.all([layChiTietKhieuNai(id), layNguoiChamHopLe(id)])
      .then(([khieuNai, nguoiCham]) => { if (conHienThi) setKetQua({ khoa, duLieu: { khieuNai, nguoiCham }, loi: '' }) })
      .catch((error) => { if (conHienThi) setKetQua({ khoa, duLieu: null, loi: error.message }) })
    return () => { conHienThi = false }
  }, [id, khoa])

  async function luuPhanCong(e) {
    e.preventDefault()
    const form = e.currentTarget
    const nhap = new FormData(form)
    const reviewerId = Number(nhap.get('reviewerId'))
    const reason = String(nhap.get('reason') || '').trim()
    const ungVien = nguoiCham.find((item) => item.reviewerId === reviewerId)
    const giaoLai = khieuNai.status === 'IN_REVIEW'
    if (!ungVien || ((giaoLai || ungVien.crossDepartment) && !reason)) {
      setThongBao({ id, loi: 'Chọn người hợp lệ và nhập lý do khi giao lại hoặc khác ban.', thanhCong: '' })
      return
    }
    if (!window.confirm(giaoLai ? 'Xác nhận giao lại? Nháp cũ được lưu trong nhật ký và xóa khỏi phiếu.' : 'Xác nhận phân công chấm mù?')) return
    setDangLuu(true)
    setThongBao({ id, loi: '', thanhCong: '' })
    try {
      if (giaoLai) await giaoLaiKhieuNai(id, { attemptId: phieu.id, expectedReviewerId: phieu.reviewerId, reviewerId, reason })
      else await phanCongKhieuNai(id, reviewerId, reason)
      setThongBao({ id, loi: '', thanhCong: giaoLai ? 'Giao lại thành công.' : 'Phân công thành công.' })
      form.reset()
      setLanTai((cu) => cu + 1)
    } catch (error) {
      setThongBao({ id, loi: error.message, thanhCong: '' })
      if (error.status === 409) setLanTai((cu) => cu + 1)
    } finally { setDangLuu(false) }
  }

  return <section className="bao-cao-qc chi-tiet-kn">
    <Link to={`/appeals${location.state?.danhSach ? `?${location.state.danhSach}` : ''}`}>← Quay lại danh sách</Link>
    <header className="bao-cao-qc__dau"><h1>Chi tiết khiếu nại #{id}</h1></header>
    {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
    {thongBao.id === id && thongBao.loi && <p className="thong-bao-loi" role="alert">{thongBao.loi}</p>}
    {thongBao.id === id && thongBao.thanhCong && <p role="status">{thongBao.thanhCong}</p>}
    {!duLieu && !loi && <p>Đang tải chi tiết…</p>}
    {khieuNai && <>
      <h2>{khieuNai.articleTitle}</h2>
      <p>Phóng viên: {khieuNai.appellantName} · Gửi: {formatDate(khieuNai.submittedAt)} · Trạng thái: {khieuNai.status}</p>
      <p>Hạn gửi của bài: {formatDate(khieuNai.appealDeadline)}. Hết hạn gửi không ngăn xử lý khiếu nại đã tiếp nhận.</p>
      <h2>Tiêu chí và lý do khiếu nại</h2>
      <ul className="chi-tiet-kn__ly-do">{khieuNai.criteria.map((item) => <li key={item.criterionId}>
        <strong>{item.criterionName || `Tiêu chí #${item.criterionId}`}</strong><p>{item.reason}</p>
      </li>)}</ul>
      <h2>Phân công chấm mù</h2>
      {phieu ? <p>Phiếu #{phieu.id} · BTV #{phieu.reviewerId} · {phieu.crossDepartment ? 'Khác ban' : 'Cùng ban'} · {phieu.submittedAt ? `Đã nộp ${formatDate(phieu.submittedAt)}` : 'Chưa nộp'}</p> : <p>Chưa phân công người chấm lại.</p>}
      {!coTheGiao && <p>Chỉ đọc: khiếu nại đã giải quyết hoặc phiếu đã nộp. So sánh/chốt điểm thuộc Iter3.</p>}
      {coTheGiao && (nguoiCham.length ? <form key={khoa} className="chi-tiet-kn__form" onSubmit={luuPhanCong}>
        <p>{nguoiCham[0].crossDepartment ? 'Không còn BTV hợp lệ cùng ban. Phân công khác ban bắt buộc có lý do.' : 'Ưu tiên các BTV hợp lệ cùng ban; người đã tham gia không được chọn lại.'}</p>
        <label>Biên tập viên mới<select name="reviewerId" required defaultValue="" disabled={dangLuu}>
          <option value="" disabled>Chọn người chấm</option>
          {nguoiCham.map((item) => <option key={item.reviewerId} value={item.reviewerId}>{item.reviewerName} (#{item.reviewerId}) · {item.sectionName}</option>)}
        </select></label>
        <label>Lý do {khieuNai.status === 'IN_REVIEW' || nguoiCham[0].crossDepartment ? '(bắt buộc)' : '(tùy chọn)'}
          <textarea name="reason" maxLength={1000} rows={3} disabled={dangLuu}
            required={khieuNai.status === 'IN_REVIEW' || nguoiCham[0].crossDepartment} /></label>
        <button disabled={dangLuu} type="submit">{dangLuu ? 'Đang lưu…' : khieuNai.status === 'SUBMITTED' ? 'Phân công' : 'Giao lại'}</button>
      </form> : <p>Không có biên tập viên đủ điều kiện. Chưa thay đổi phân công.</p>)}
      <Link to={`/audit-logs?appealId=${id}`}>Xem nhật ký khiếu nại</Link>
    </>}
  </section>
}
