import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { laySoSanhKhieuNai, layNguoiChamHopLe, phanCongLanHai, chotDiemKhieuNai } from '../../api/appealApi.js'
import '../../styles/QualityReportPage.css'
import './ReviewWorkflow.css'

/**
 * Man Truong ban so sanh diem goc va cac lan cham lai mu cua 1 khieu nai (UC-39), he thong de xuat ket
 * qua theo luat chenh lech; can thi giao cham lan 2 (S2), xong thi chot diem.
 *
 * MO TRANG NAY TAI: /appeals/:id/comparison
 *
 * MUC LUC (so dong - ten - tac dung)
 *   29  TEN_LUAT                        hang so
 *   31  AppealComparisonPage()          appeal comparison page
 *   36  ComparisonContent()             comparison content
 *   42    useEffect                     tai du lieu khi mo trang
 *   54    xuLy()                        xu ly
 *   63    giaoLanHai()                  giao lan hai
 *   74    xacNhan()                     xac nhan
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   80  link [← Chi tiet khieu nai]     mo trang /appeals/{id} (AppealDetailPage)
 *  102  form, nut [Phan cong S2]        giaoLanHai() -> hoi xac nhan; goi POST /api/appeals/{id}/assign-second
 *  110  [Xac nhan diem hieu luc]        xacNhan() -> hoi xac nhan; goi POST /api/appeals/{id}/finalize
 *  112  link [Xem nhat ky]              mo trang /audit-logs?appealId={id} (AuditLogPage)
 */

const TEN_LUAT = { USE_S1: 'Dùng điểm S1', AVERAGE_S0_S2: 'Trung bình S0 và S2', AVERAGE_S1_S2: 'Trung bình S1 và S2', USE_S2_TIE: 'Dùng S2 khi hai khoảng cách bằng nhau', KEEP_ORIGINAL: 'Giữ điểm gốc (lịch sử)', USE_RESCORE: 'Dùng điểm chấm lại (lịch sử)' }

export default function AppealComparisonPage() {
  const { id } = useParams()
  return <ComparisonContent key={id} id={id} />
}

function ComparisonContent({ id }) {
  const [lanTai, setLanTai] = useState(0)
  const [ketQua, setKetQua] = useState(null)
  const [loi, setLoi] = useState('')
  const [thongBao, setThongBao] = useState('')
  const [dangLuu, setDangLuu] = useState(false)
  useEffect(() => {
    let conHienThi = true
    Promise.all([laySoSanhKhieuNai(id), layNguoiChamHopLe(id)]).then(([soSanh, ungVien]) => {
      if (conHienThi) setKetQua({ soSanh, ungVien })
    }).catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [id, lanTai])
  const duLieu = ketQua?.soSanh
  const deXuat = duLieu?.proposal
  const daChot = duLieu?.status === 'RESOLVED'
  const phieuCuoi = duLieu?.second ?? duLieu?.first

  async function xuLy(thaoTac, thanhCong) {
    setDangLuu(true); setLoi(''); setThongBao('')
    try { await thaoTac(); setThongBao(thanhCong); setKetQua(null); setLanTai((cu) => cu + 1) }
    catch (error) {
      setLoi(error.message)
      if (error.status === 409) { setKetQua(null); setLanTai((cu) => cu + 1) }
    } finally { setDangLuu(false) }
  }

  function giaoLanHai(e) {
    e.preventDefault()
    const nhap = new FormData(e.currentTarget)
    const reviewerId = Number(nhap.get('reviewerId'))
    const reason = String(nhap.get('reason') || '').trim()
    const nguoi = ketQua.ungVien.find((item) => item.reviewerId === reviewerId)
    if (!nguoi || (nguoi.crossDepartment && !reason)) { setLoi('Chọn người hợp lệ và nhập lý do khi phân công khác ban.'); return }
    if (window.confirm('Phân công đợt chấm lại thứ hai và cuối cùng?'))
      xuLy(() => phanCongLanHai(id, reviewerId, reason), 'Đã phân công S2.')
  }

  function xacNhan() {
    if (window.confirm(`Xác nhận điểm hiệu lực ${deXuat.effectiveScore}/100? Sau khi chốt không thể sửa kết quả khiếu nại.`))
      xuLy(() => chotDiemKhieuNai(id, phieuCuoi.id), 'Đã xác nhận điểm hiệu lực và hoàn tất khiếu nại.')
  }

  return <section className="bao-cao-qc luong-qc">
    <Link to={`/appeals/${id}`}>← Chi tiết khiếu nại</Link>
    <header className="bao-cao-qc__dau"><h1>So sánh &amp; chốt điểm #{id}</h1><p>Điểm gốc và các lần chấm được giữ để đối chiếu. Chỉ Trưởng ban quản lý bài được xác nhận.</p></header>
    {loi && <p role="alert" className="thong-bao-loi">{loi}</p>}
    {thongBao && <p role="status">{thongBao}</p>}
    {!duLieu && !loi && <p>Đang tải so sánh…</p>}
    {duLieu && <>
      <h2>{duLieu.original.title}</h2>
      <p>Ngưỡng đã gắn với khiếu nại: {duLieu.threshold} điểm. Chênh lệch xét theo trị tuyệt đối.</p>
      <div className="bao-cao-qc__bang-boc"><table className="luong-qc__bang">
        <thead><tr><th>Tiêu chí / trọng số</th>{['Gốc S0', 'Chấm lại S1', 'Trọng tài S2'].map((ten) => <th key={ten}>{ten}</th>)}</tr></thead>
        <tbody>{duLieu.original.criteria.map((tieuChi) => <tr key={tieuChi.criterionId}>
          <th>{tieuChi.name}<br />{tieuChi.weight}%</th>
          {[duLieu.original, duLieu.first, duLieu.second].map((phieu, viTri) => {
            const dong = phieu?.criteria.find((item) => item.criterionId === tieuChi.criterionId)
            return <td key={viTri}>{dong?.score ?? '—'}{dong?.comment && <p>{dong.comment}</p>}</td>
          })}
        </tr>)}
          <tr><th>Tổng điểm</th>{[duLieu.original, duLieu.first, duLieu.second].map((phieu, viTri) => <td key={viTri}><strong>{phieu?.totalScore ?? '—'}</strong><br />{phieu ? phieu.submittedAt ? 'Đã nộp' : 'Đang chấm' : 'Chưa có phiếu'}</td>)}</tr>
        </tbody>
      </table></div>
      {daChot ? <div className="luong-qc__the"><h2>Đã giải quyết</h2><p>Điểm hiệu lực: <strong>{duLieu.confirmedScore}/100</strong> · {TEN_LUAT[duLieu.confirmedRule] || duLieu.confirmedRule}</p></div>
        : deXuat?.requiresSecondReview ? <section className="luong-qc__the"><h2>Cần chấm S2</h2><p>Chênh lệch S1 và S0 vượt ngưỡng, phải có người chấm độc lập thứ ba.</p>
          {ketQua.ungVien.length ? <form className="luong-qc__form" onSubmit={giaoLanHai}>
            <label>Người chấm S2<select name="reviewerId" required defaultValue="" disabled={dangLuu}><option value="" disabled>Chọn biên tập viên</option>
              {ketQua.ungVien.map((item) => <option key={item.reviewerId} value={item.reviewerId}>{item.reviewerName} · {item.sectionName}{item.crossDepartment ? ' · Khác ban' : ''}</option>)}</select></label>
            <label>Lý do phân công (bắt buộc khi khác ban)<textarea name="reason" maxLength={1000} rows={3} disabled={dangLuu} /></label>
            <button disabled={dangLuu} type="submit">Phân công S2</button>
          </form> : <p>Chưa có biên tập viên đủ điều kiện. Chưa thể chốt kết quả.</p>}
        </section> : deXuat ? <section className="luong-qc__the"><h2>Đề xuất: {deXuat.effectiveScore}/100</h2>
          <p>{TEN_LUAT[deXuat.rule]}. Kết quả có thể tăng hoặc giảm so với S0.</p>
          <button disabled={dangLuu} onClick={xacNhan}>Xác nhận điểm hiệu lực</button>
        </section> : <p>Chờ nộp đủ phiếu chấm được yêu cầu. Phân công hoặc giao lại tại màn chi tiết khiếu nại.</p>}
      <Link to={`/audit-logs?appealId=${id}`}>Xem nhật ký</Link>
    </>}
  </section>
}
