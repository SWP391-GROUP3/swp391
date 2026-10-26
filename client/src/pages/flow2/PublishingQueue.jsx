import { useState, useEffect } from 'react'
import { layDanhSachChoDuyetDang, duyetDangBaiViet } from '../../api/assessmentApi.js'
import { getArticleDetail } from '../../api/articleApi.js'
import { formatDate } from '../../utils/formatDate.js'
import './PublishingQueue.css'
import '../../styles/Flow2.css'

export default function PublishingQueue({ nguoiDung }) {
  const [danhSach, setDanhSach] = useState([])
  const [dangTai, setDangTai] = useState(false)
  const [thongBao, setThongBao] = useState('')
  const [loi, setLoi] = useState('')
  const [tuKhoa, setTuKhoa] = useState('')

  // Modal kiểm duyệt xuất bản
  const [baiKiemDuyet, setBaiKiemDuyet] = useState(null)
  const [chiTietBai, setChiTietBai] = useState(null)
  const [dangTaiChiTiet, setDangTaiChiTiet] = useState(false)
  const [dangXuLy, setDangXuLy] = useState(false)

  useEffect(() => {
    taiDanhSach()
  }, [])

  async function taiDanhSach() {
    setDangTai(true)
    setLoi('')
    try {
      const data = await layDanhSachChoDuyetDang(nguoiDung?.sectionId)
      setDanhSach(data || [])
    } catch (err) {
      setLoi(err.message || 'Không thể tải danh sách chờ duyệt xuất bản')
    } finally {
      setDangTai(false)
    }
  }

  async function moModalKiemDuyet(item) {
    setBaiKiemDuyet(item)
    setDangTaiChiTiet(true)
    setChiTietBai(null)
    try {
      const chiTiet = await getArticleDetail(item.articleId)
      setChiTietBai(chiTiet)
    } catch {
      // Nếu không lấy được chi tiết mở rộng thì vẫn hiển thị thông tin tóm tắt
    } finally {
      setDangTaiChiTiet(false)
    }
  }

  function dongModal() {
    if (dangXuLy) return
    setBaiKiemDuyet(null)
    setChiTietBai(null)
  }

  async function xuLyDuyetDang() {
    if (!baiKiemDuyet) return
    setDangXuLy(true)
    setLoi('')
    try {
      await duyetDangBaiViet(baiKiemDuyet.articleId)
      setThongBao(`Duyệt xuất bản thành công bài viết: "${baiKiemDuyet.title}". Bài đã chuyển sang trạng thái ĐÃ XUẤT BẢN và bàn giao sang tính nhuận bút.`)
      setBaiKiemDuyet(null)
      setChiTietBai(null)
      taiDanhSach()
    } catch (err) {
      setLoi(err.message || 'Duyệt xuất bản bài viết thất bại')
    } finally {
      setDangXuLy(false)
    }
  }

  // Lọc theo từ khóa tìm kiếm
  const danhSachLoc = danhSach.filter(item => {
    if (!tuKhoa.trim()) return true
    const kw = tuKhoa.toLowerCase()
    return (
      item.title?.toLowerCase().includes(kw) ||
      item.authorName?.toLowerCase().includes(kw) ||
      item.categoryName?.toLowerCase().includes(kw) ||
      item.reviewerName?.toLowerCase().includes(kw)
    )
  })

  // Thống kê nhanh
  const tongSoBai = danhSach.length
  const soBaiTrongHanKhieuNai = danhSach.filter(item => item.isAppealWindowOpen).length
  const diemTb = tongSoBai > 0
    ? (danhSach.reduce((sum, item) => sum + Number(item.totalScore || 0), 0) / tongSoBai).toFixed(2)
    : '0.00'

  return (
    <div className="duyet-dang">
      <div className="duyet-dang__the">
        <div className="duyet-dang__dau">
          <div>
            <h2 className="duyet-dang__tieu-de">Hàng chờ duyệt xuất bản bài viết</h2>
            <p className="duyet-dang__mo-ta">
              Xem xét các bài viết đã hoàn thiện đánh giá chất lượng và công bố điểm để chính thức phát hành lên báo trực tuyến.
            </p>
          </div>
          <button
            type="button"
            className="duyet-dang__nut-lam-moi"
            onClick={taiDanhSach}
            disabled={dangTai}
          >
            {dangTai ? 'Đang tải...' : 'Làm mới'}
          </button>
        </div>

        {thongBao && (
          <div className="duyet-dang__thong-bao duyet-dang__thong-bao--thanh-cong">
            {thongBao}
          </div>
        )}

        {loi && (
          <div className="duyet-dang__thong-bao duyet-dang__thong-bao--loi">
            {loi}
          </div>
        )}

        {/* Khối thống kê */}
        <div className="duyet-dang__thong-ke">
          <div className="duyet-dang__thong-ke-the">
            <span className="duyet-dang__thong-ke-nhan">Chờ duyệt xuất bản</span>
            <span className="duyet-dang__thong-ke-so">{tongSoBai}</span>
            <span className="duyet-dang__thong-ke-ghi-chu">Bài đã công bố điểm</span>
          </div>
          <div className="duyet-dang__thong-ke-the">
            <span className="duyet-dang__thong-ke-nhan">Trong hạn khiếu nại (7 ngày)</span>
            <span className="duyet-dang__thong-ke-so" style={{ color: '#7c3aed' }}>{soBaiTrongHanKhieuNai}</span>
            <span className="duyet-dang__thong-ke-ghi-chu">Có thể duyệt ngay hoặc chờ</span>
          </div>
          <div className="duyet-dang__thong-ke-the">
            <span className="duyet-dang__thong-ke-nhan">Điểm chất lượng trung bình</span>
            <span className="duyet-dang__thong-ke-so" style={{ color: '#0284c7' }}>{diemTb}</span>
            <span className="duyet-dang__thong-ke-ghi-chu">Thang điểm 100</span>
          </div>
        </div>

        {/* Thanh tìm kiếm */}
        <div className="duyet-dang__bo-loc">
          <input
            type="text"
            className="duyet-dang__o-tim-kiem"
            placeholder="Tìm theo tiêu đề, tác giả, thể loại, biên tập viên..."
            value={tuKhoa}
            onChange={(e) => setTuKhoa(e.target.value)}
          />
        </div>

        {/* Bảng danh sách bài */}
        {dangTai ? (
          <div className="duyet-dang__trong">Đang tải danh sách bài chờ duyệt xuất bản...</div>
        ) : danhSachLoc.length === 0 ? (
          <div className="duyet-dang__trong">
            {tuKhoa ? 'Không tìm thấy bài viết nào khớp với từ khóa tìm kiếm.' : 'Hiện không có bài viết nào chờ duyệt xuất bản.'}
          </div>
        ) : (
          <div className="duyet-dang__bang-bao">
            <table className="duyet-dang__bang">
              <thead>
                <tr>
                  <th style={{ width: '60px' }}>Mã</th>
                  <th>Tiêu đề bài viết</th>
                  <th>Tác giả & BTV</th>
                  <th>Thể loại</th>
                  <th>Điểm chất lượng</th>
                  <th>Ngày công bố</th>
                  <th>Cửa sổ khiếu nại</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {danhSachLoc.map((item) => (
                  <tr key={item.articleId}>
                    <td style={{ color: 'var(--fm-chu-nhat)' }}>#{item.articleId}</td>
                    <td>
                      <div className="duyet-dang__tieu-de-bai">{item.title}</div>
                    </td>
                    <td>
                      <div>{item.authorName}</div>
                      <div className="duyet-dang__tac-gia-sub">BTV: {item.reviewerName}</div>
                    </td>
                    <td>
                      <span className="duyet-dang__huy-hieu" style={{ background: 'var(--fm-nen)' }}>
                        {item.categoryName}
                      </span>
                    </td>
                    <td>
                      <span className="duyet-dang__huy-hieu duyet-dang__huy-hieu--diem">
                        {Number(item.totalScore).toFixed(2)}đ
                      </span>
                    </td>
                    <td>
                      {item.scoreApprovedAt ? formatDate(item.scoreApprovedAt) : '—'}
                    </td>
                    <td>
                      {item.isAppealWindowOpen ? (
                        <span className="duyet-dang__huy-hieu duyet-dang__huy-hieu--khieu-nai-mo">
                          Còn {item.daysRemaining} ngày
                        </span>
                      ) : (
                        <span className="duyet-dang__huy-hieu duyet-dang__huy-hieu--het-han">
                          Hết hạn khiếu nại
                        </span>
                      )}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        type="button"
                        className="duyet-dang__nut-kiem-duyet"
                        onClick={() => moModalKiemDuyet(item)}
                      >
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M21 3 3 10.5l7 2.5 2.5 7L21 3Z" />
                          <path d="m10 13 4-4" />
                        </svg>
                        Kiểm duyệt & Đăng
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Kiểm duyệt & Duyệt xuất bản (bám sát mockup-07) */}
      {baiKiemDuyet && (
        <div className="duyet-dang__modal-nen" onClick={dongModal}>
          <div className="duyet-dang__modal-hop" onClick={(e) => e.stopPropagation()}>
            <div className="duyet-dang__modal-dau">
              <h3>Kiểm duyệt xuất bản: {baiKiemDuyet.title}</h3>
              <button
                type="button"
                className="duyet-dang__nut-dong"
                onClick={dongModal}
                disabled={dangXuLy}
              >
                &times;
              </button>
            </div>

            <div className="duyet-dang__modal-than">
              {/* Mục chọn thời gian phát hành */}
              <div className="duyet-dang__muc-chon">
                <div className="duyet-dang__muc-chon-tieu-de">Thời gian phát hành bài viết</div>
                <label className="duyet-dang__radio-nhan">
                  <input type="radio" checked readOnly name="thoiGianPhatHanh" />
                  <strong>Xuất bản ngay lập tức (Thời gian thực)</strong>
                </label>
              </div>

              {/* Thông tin bài viết */}
              <div className="duyet-dang__thong-tin-meta">
                <span>
                  <strong>{baiKiemDuyet.categoryName}</strong> • Điểm: <strong>{Number(baiKiemDuyet.totalScore).toFixed(2)}đ</strong>
                </span>
                <span>
                  Phóng viên: <strong>{baiKiemDuyet.authorName}</strong> • BTV thẩm định: <strong>{baiKiemDuyet.reviewerName}</strong>
                </span>
              </div>

              <h4 className="duyet-dang__bai-tieu-de">{baiKiemDuyet.title}</h4>

              {/* Trích đoạn / Nội dung bài viết */}
              <div className="duyet-dang__bai-noi-dung">
                {dangTaiChiTiet ? (
                  <p style={{ color: 'var(--fm-chu-nhat)' }}>Đang tải nội dung chi tiết bài viết...</p>
                ) : chiTietBai?.content ? (
                  chiTietBai.content
                ) : (
                  <p style={{ color: 'var(--fm-chu-nhat)', fontStyle: 'italic' }}>
                    Bài viết đã hoàn thành biên tập và được đánh giá chất lượng đạt yêu cầu để duyệt xuất bản.
                  </p>
                )}
              </div>

              {/* Cảnh báo cửa sổ khiếu nại nếu còn trong 7 ngày */}
              {baiKiemDuyet.isAppealWindowOpen && (
                <div className="duyet-dang__canh-bao">
                  <strong>Lưu ý về thời hạn khiếu nại:</strong> Bài viết hiện còn trong cửa sổ 7 ngày khiếu nại điểm chất lượng của phóng viên (còn {baiKiemDuyet.daysRemaining} ngày). Trưởng ban có thể duyệt xuất bản ngay theo tiến độ tòa soạn hoặc chờ hết hạn khiếu nại.
                </div>
              )}

              {/* Thông báo quy trình bàn giao */}
              <div className="duyet-dang__thong-bao-ban-giao">
                Sau khi phê duyệt, bài viết sẽ chính thức được xuất bản lên hệ thống (PUBLISHED), ghi nhận ngày phát hành và tự động bàn giao cho Thư ký tòa soạn để chấm nhuận bút.
              </div>
            </div>

            <div className="duyet-dang__modal-duoi">
              <button
                type="button"
                className="duyet-dang__nut-phu"
                onClick={dongModal}
                disabled={dangXuLy}
              >
                Đóng
              </button>
              <button
                type="button"
                className="duyet-dang__nut-xac-nhan-dang"
                onClick={xuLyDuyetDang}
                disabled={dangXuLy}
              >
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10" />
                  <path d="m4.93 4.93 4.24 4.24" />
                  <path d="m14.83 9.17 4.24-4.24" />
                  <path d="m14.83 14.83 4.24 4.24" />
                  <path d="m9.17 14.83-4.24 4.24" />
                  <circle cx="12" cy="12" r="4" />
                </svg>
                {dangXuLy ? 'Đang duyệt xuất bản...' : 'Duyệt xuất bản'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
