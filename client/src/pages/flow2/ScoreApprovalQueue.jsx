import { useState, useEffect } from 'react'
import { useParams } from 'react-router-dom'
import {
  layDanhSachChoDuyetDiem,
  layPhieuChamMoiNhat,
  duyetDiemBaiViet,
  yeuCauChamLai
} from '../../api/assessmentApi.js'
import './ScoreApprovalQueue.css'
import '../../styles/Flow2.css'

/**
 * Man Truong ban duyet diem (UC-29...32): hang cho duyet, xem chi tiet phieu cham, duyet diem hoac yeu
 * cau BTV cham lai kem ly do.
 *
 * MO TRANG NAY TAI: /score-approvals, /score-approvals/:id
 *
 * MUC LUC (so dong - ten - tac dung)
 *   46  ScoreApprovalQueue()            score approval queue
 *   63    useEffect                     tai du lieu khi mo trang
 *   67    useEffect                     tai du lieu khi mo trang
 *   74    taiDanhSach()                 UC-29: bai PENDING_APPROVAL cua ban minh
 *   88    xemChiTiet()                  UC-30: mo phieu cham moi nhat cua bai (diem tung tieu chi, nhan xet)
 *  102    xuLyDuyetDiem()               UC-31: duyet diem -> bai SCORE_PUBLISHED, bat dau han khieu nai
 *  122    moModalChamLai()              UC-32: mo hop nhap ly do yeu cau cham lai
 *  129    xacNhanChamLai()              UC-32: gui yeu cau cham lai (bat buoc ly do) -> bai ve dung BTV cu
 *  151    dinhDangNgay()                Ngay gio kieu Viet Nam de hien trong bang
 *  165    renderNhanDeXuat()            Nhan mau cho quyet dinh cua BTV (ACCEPT / REVISION / REJECT)
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  191  [Dang tai... / Lam moi danh sach]  taiDanhSach() UC-29: bai PENDING_APPROVAL cua ban minh -> goi GET /api/score-approvals
 *  259  [Xem chi tiet]                  xemChiTiet() UC-30: mo phieu cham moi nhat cua bai (diem tung tieu chi, nhan xet) -> goi GET /api/scoring-attempts/article/{articleId}/latest
 *  268  [Duyet diem]                    xuLyDuyetDiem() UC-31: duyet diem -> bai SCORE_PUBLISHED, bat dau han khieu nai -> hoi xac nhan; goi POST /api/score-approvals/article/{articleId}/approve, GET…
 *  277  [Cham lai]                      moModalChamLai() UC-32: mo hop nhap ly do yeu cau cham lai -> tren trang: bai viet cham lai, dong/xoa ly do cham lai, dong/xoa loi
 *  294  [vung nen ngoai hop thoai]      tren trang: dong/xoa phieu xem chi tiet
 *  302  [✕]                             tren trang: dong/xoa phieu xem chi tiet
 *  373  [Dong]                          tren trang: dong/xoa phieu xem chi tiet
 *  381  [Yeu cau cham lai]              tren trang: bai viet cham lai, dong/xoa ly do cham lai, dong/xoa loi
 *  393  [Phe duyet diem nay]            xuLyDuyetDiem() UC-31: duyet diem -> bai SCORE_PUBLISHED, bat dau han khieu nai -> hoi xac nhan; goi POST /api/score-approvals/article/{articleId}/approve, GET…
 *  405  [vung nen ngoai hop thoai]      tren trang: dong/xoa bai viet cham lai
 *  413  [✕]                             tren trang: dong/xoa bai viet cham lai
 *  439  [Huy bo]                        tren trang: dong/xoa bai viet cham lai
 *  448  [Dang gui... / Xac nhan yeu cau cham lai]  xacNhanChamLai() UC-32: gui yeu cau cham lai (bat buoc ly do) -> bai ve dung BTV cu -> goi POST /api/score-approvals/article/{articleId}/return, GET /api/score…
 */

export default function ScoreApprovalQueue() {
  // /score-approvals/:id (link tu trang chu Truong ban): id = ma bai, mo san phieu cham cua bai do
  const { id } = useParams()
  const [danhSach, setDanhSach] = useState([])
  const [dangTai, setDangTai] = useState(false)
  const [thongBao, setThongBao] = useState('')
  const [loi, setLoi] = useState('')

  // Modal xem chi tiết phiếu chấm (UC-30)
  const [phieuXemChiTiet, setPhieuXemChiTiet] = useState(null)
  const [dangTaiChiTiet, setDangTaiChiTiet] = useState(false)

  // Modal yêu cầu chấm lại (UC-32)
  const [baiVietChamLai, setBaiVietChamLai] = useState(null)
  const [lyDoChamLai, setLyDoChamLai] = useState('')
  const [dangXuLy, setDangXuLy] = useState(false)

  useEffect(() => {
    taiDanhSach()
  }, [])

  useEffect(() => {
    if (id) {
      xemChiTiet(Number(id))
    }
  }, [id])

  // UC-29: bai PENDING_APPROVAL cua ban minh
  async function taiDanhSach() {
    setDangTai(true)
    setLoi('')
    try {
      const data = await layDanhSachChoDuyetDiem()
      setDanhSach(data || [])
    } catch (err) {
      setLoi(err.message || 'Không thể tải danh sách chờ duyệt điểm')
    } finally {
      setDangTai(false)
    }
  }

  // UC-30: mo phieu cham moi nhat cua bai (diem tung tieu chi, nhan xet)
  async function xemChiTiet(articleId) {
    setDangTaiChiTiet(true)
    setLoi('')
    try {
      const data = await layPhieuChamMoiNhat(articleId)
      setPhieuXemChiTiet(data)
    } catch (err) {
      setLoi(err.message || 'Không thể xem chi tiết phiếu chấm')
    } finally {
      setDangTaiChiTiet(false)
    }
  }

  // UC-31: duyet diem -> bai SCORE_PUBLISHED, bat dau han khieu nai
  async function xuLyDuyetDiem(articleId) {
    if (!window.confirm('Bạn có chắc chắn muốn phê duyệt điểm đánh giá cho bài viết này? Điểm sẽ được chính thức công bố.')) {
      return
    }

    setDangXuLy(true)
    setLoi('')
    try {
      await duyetDiemBaiViet(articleId)
      setThongBao('Phê duyệt điểm thành công! Điểm đã được công bố (SCORE_PUBLISHED).')
      setPhieuXemChiTiet(null)
      await taiDanhSach()
    } catch (err) {
      setLoi(err.message || 'Phê duyệt điểm thất bại')
    } finally {
      setDangXuLy(false)
    }
  }

  // UC-32: mo hop nhap ly do yeu cau cham lai
  function moModalChamLai(item) {
    setBaiVietChamLai(item)
    setLyDoChamLai('')
    setLoi('')
  }

  // UC-32: gui yeu cau cham lai (bat buoc ly do) -> bai ve dung BTV cu
  async function xacNhanChamLai() {
    if (!lyDoChamLai.trim()) {
      setLoi('Vui lòng nhập lý do yêu cầu BTV chấm lại.')
      return
    }

    setDangXuLy(true)
    setLoi('')
    try {
      await yeuCauChamLai(baiVietChamLai.articleId, lyDoChamLai)
      setThongBao('Đã gửi yêu cầu chấm lại cho Biên tập viên. Bài viết đã trở về trạng thái Phân công (ASSIGNED).')
      setBaiVietChamLai(null)
      setPhieuXemChiTiet(null)
      await taiDanhSach()
    } catch (err) {
      setLoi(err.message || 'Yêu cầu chấm lại thất bại')
    } finally {
      setDangXuLy(false)
    }
  }

  // Ngay gio kieu Viet Nam de hien trong bang
  function dinhDangNgay(dateStr) {
    if (!dateStr) return '—'
    try {
      const d = new Date(dateStr)
      return d.toLocaleString('vi-VN', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      })
    } catch {
      return dateStr
    }
  }

  // Nhan mau cho quyet dinh cua BTV (ACCEPT / REVISION / REJECT)
  function renderNhanDeXuat(decision) {
    switch (decision) {
      case 'ACCEPT':
        return <span className="duyet-diem__nhan-de-xuat duyet-diem__nhan-de-xuat--accept">Đề xuất: Duyệt đăng</span>
      case 'REVISION':
        return <span className="duyet-diem__nhan-de-xuat duyet-diem__nhan-de-xuat--revision">Đề xuất: Cần sửa</span>
      case 'REJECT':
        return <span className="duyet-diem__nhan-de-xuat duyet-diem__nhan-de-xuat--reject">Đề xuất: Từ chối</span>
      default:
        return <span>—</span>
    }
  }

  return (
    <div className="duyet-diem">
      <div className="duyet-diem__the">
        <div className="duyet-diem__dau">
          <div>
            <h2 className="duyet-diem__tieu-de">Hàng chờ duyệt điểm đánh giá</h2>
            <p className="duyet-diem__mo-ta">
              Danh sách các bài viết đã được Biên tập viên chấm điểm và đang chờ Trưởng ban phê duyệt
            </p>
          </div>
          <button
            type="button"
            className="duyet-diem__nut-lam-moi"
            onClick={taiDanhSach}
            disabled={dangTai}
          >
            {dangTai ? 'Đang tải...' : 'Làm mới danh sách'}
          </button>
        </div>

        {thongBao && (
          <div className="thong-bao-thanh-cong" style={{ marginBottom: 16 }}>
            {thongBao}
          </div>
        )}

        {loi && (
          <div className="thong-bao-loi" style={{ marginBottom: 16 }}>
            {loi}
          </div>
        )}

        {dangTai ? (
          <div className="bf02__trong">Đang tải danh sách chờ duyệt điểm...</div>
        ) : danhSach.length === 0 ? (
          <div className="bf02__trong">
            Hiện không có bài viết nào đang ở trạng thái Chờ duyệt điểm (PENDING_APPROVAL).
          </div>
        ) : (
          <div className="bang-fm-cuon">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th style={{ width: 60 }}>Mã bài</th>
                  <th>Tiêu đề bài viết</th>
                  <th style={{ width: 120 }}>Lần chấm</th>
                  <th style={{ width: 160 }}>Biên tập viên</th>
                  <th style={{ width: 100, textAlign: 'right' }}>Tổng điểm</th>
                  <th style={{ width: 150, textAlign: 'center' }}>Đề xuất BTV</th>
                  <th style={{ width: 150 }}>Thời gian nộp</th>
                  <th style={{ width: 220, textAlign: 'center' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {danhSach.map((item) => (
                  <tr key={item.scoringAttemptId}>
                    <td style={{ fontWeight: 600 }}>#{item.articleId}</td>
                    <td>
                      <span style={{ fontWeight: 500 }}>{item.articleTitle}</span>
                    </td>
                    <td>
                      Lần {item.attemptNo}
                      {item.attemptType === 'RESCORE' && (
                        <span className="duyet-diem__nhan-cham-lai">Chấm lại</span>
                      )}
                    </td>
                    <td>{item.reviewerName}</td>
                    <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--fm-thong-tin)' }}>
                      {item.totalScore != null ? Number(item.totalScore).toFixed(2) : '—'} / 100
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      {renderNhanDeXuat(item.decision)}
                    </td>
                    <td style={{ fontSize: 12, color: 'var(--fm-chu-nhat)' }}>
                      {dinhDangNgay(item.submittedAt)}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <div style={{ display: 'inline-flex', gap: 6 }}>
                        <button
                          type="button"
                          className="nut-nhe"
                          onClick={() => xemChiTiet(item.articleId)}
                          disabled={dangTaiChiTiet}
                          title="Xem chi tiết các tiêu chí và nhận xét"
                        >
                          Xem chi tiết
                        </button>
                        <button
                          type="button"
                          className="nut-chinh-fm"
                          onClick={() => xuLyDuyetDiem(item.articleId)}
                          disabled={dangXuLy}
                        >
                          Duyệt điểm
                        </button>
                        <button
                          type="button"
                          className="nut-nhe"
                          style={{ borderColor: 'var(--fm-loi)', color: 'var(--fm-loi)' }}
                          onClick={() => moModalChamLai(item)}
                          disabled={dangXuLy}
                        >
                          Chấm lại
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Chi tiết Phiếu Chấm (UC-30) */}
      {phieuXemChiTiet && (
        <div className="duyet-diem__modal-nen" onClick={() => setPhieuXemChiTiet(null)}>
          <div className="duyet-diem__modal-hop" onClick={(e) => e.stopPropagation()}>
            <div className="duyet-diem__modal-dau">
              <h3>Chi tiết phiếu chấm điểm: {phieuXemChiTiet.articleTitle}</h3>
              <button
                type="button"
                className="nut-nhe"
                style={{ padding: '4px 8px' }}
                onClick={() => setPhieuXemChiTiet(null)}
              >
                ✕
              </button>
            </div>
            <div className="duyet-diem__modal-than">
              <div className="duyet-diem__phan-noi-dung">
                <h4 className="duyet-diem__tieu-de-muc">Nội dung bài viết nộp</h4>
                <div className="duyet-diem__noi-dung-bai">
                  {phieuXemChiTiet.articleContent || 'Không có nội dung'}
                </div>
              </div>

              <div>
                <h4 className="duyet-diem__tieu-de-muc">Bảng điểm theo từng tiêu chí</h4>
                <table className="bang-fm">
                  <thead>
                    <tr>
                      <th>Tiêu chí</th>
                      <th style={{ width: 90, textAlign: 'right' }}>Trọng số</th>
                      <th style={{ width: 100, textAlign: 'right' }}>Điểm (0-100)</th>
                      <th>Nhận xét của BTV</th>
                    </tr>
                  </thead>
                  <tbody>
                    {phieuXemChiTiet.details?.map((cd) => (
                      <tr key={cd.id}>
                        <td>
                          <strong>{cd.criterionName}</strong>
                        </td>
                        <td style={{ textAlign: 'right' }}>{cd.weight}%</td>
                        <td style={{
                          textAlign: 'right',
                          fontWeight: 600,
                          color: Number(cd.score) < 50 ? 'var(--fm-loi)' : 'inherit'
                        }}>
                          {cd.score != null ? Number(cd.score).toFixed(1) : '—'}
                        </td>
                        <td>
                          {cd.comment || <span style={{ color: 'var(--fm-chu-nhat)', fontStyle: 'italic' }}>Không có</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="duyet-diem__tong-ket">
                <div>
                  <div style={{ fontSize: 13, color: 'var(--fm-chu-nhat)' }}>Quyết định đề xuất:</div>
                  <div style={{ marginTop: 4 }}>
                    {renderNhanDeXuat(phieuXemChiTiet.decision)}
                  </div>
                  {phieuXemChiTiet.decisionNote && (
                    <div style={{ marginTop: 6, fontSize: 13 }}>
                      <strong>Ghi chú:</strong> {phieuXemChiTiet.decisionNote}
                    </div>
                  )}
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: 13, color: 'var(--fm-chu-nhat)' }}>Tổng điểm quy đổi:</div>
                  <div style={{ fontSize: 24, fontWeight: 700, color: 'var(--fm-thong-tin)' }}>
                    {phieuXemChiTiet.totalScore != null ? Number(phieuXemChiTiet.totalScore).toFixed(2) : '—'} / 100
                  </div>
                </div>
              </div>
            </div>
            <div className="duyet-diem__modal-chan">
              <button
                type="button"
                className="nut-nhe"
                onClick={() => setPhieuXemChiTiet(null)}
              >
                Đóng
              </button>
              <button
                type="button"
                className="nut-nhe"
                style={{ borderColor: 'var(--fm-loi)', color: 'var(--fm-loi)' }}
                onClick={() => {
                  moModalChamLai({
                    articleId: phieuXemChiTiet.articleId,
                    articleTitle: phieuXemChiTiet.articleTitle
                  })
                }}
              >
                Yêu cầu chấm lại
              </button>
              <button
                type="button"
                className="nut-chinh-fm"
                onClick={() => xuLyDuyetDiem(phieuXemChiTiet.articleId)}
                disabled={dangXuLy}
              >
                Phê duyệt điểm này
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Yêu Cầu Chấm Lại (UC-32) */}
      {baiVietChamLai && (
        <div className="duyet-diem__modal-nen" onClick={() => setBaiVietChamLai(null)}>
          <div className="duyet-diem__modal-hop" style={{ maxWidth: 500 }} onClick={(e) => e.stopPropagation()}>
            <div className="duyet-diem__modal-dau">
              <h3>Yêu cầu BTV chấm lại bài viết</h3>
              <button
                type="button"
                className="nut-nhe"
                style={{ padding: '4px 8px' }}
                onClick={() => setBaiVietChamLai(null)}
              >
                ✕
              </button>
            </div>
            <div className="duyet-diem__modal-than">
              <p style={{ margin: 0, fontSize: 13, color: 'var(--fm-chu)' }}>
                Bài viết: <strong>{baiVietChamLai.articleTitle}</strong>
              </p>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 13, fontWeight: 500 }}>
                  Lý do yêu cầu chấm lại <span style={{ color: 'var(--fm-loi)' }}>*</span>
                </label>
                <textarea
                  className="duyet-diem__textarea"
                  rows={4}
                  placeholder="Nhập lý do cụ thể gửi tới Biên tập viên (ví dụ: Chấm điểm tiêu chí hình ảnh chưa thỏa đáng, cần xem xét lại...)"
                  value={lyDoChamLai}
                  onChange={(e) => setLyDoChamLai(e.target.value)}
                />
              </div>
            </div>
            <div className="duyet-diem__modal-chan">
              <button
                type="button"
                className="nut-nhe"
                onClick={() => setBaiVietChamLai(null)}
                disabled={dangXuLy}
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                className="nut-chinh-fm"
                style={{ background: 'var(--fm-loi)', borderColor: 'var(--fm-loi)' }}
                onClick={xacNhanChamLai}
                disabled={dangXuLy}
              >
                {dangXuLy ? 'Đang gửi...' : 'Xác nhận yêu cầu chấm lại'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
