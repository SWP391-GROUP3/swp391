import { useState, useEffect } from 'react'
import {
  layDanhSachChoDuyetDang,
  layDanhSachDaXuatBan,
  duyetDangBaiViet,
  huyDuyetDangBaiViet
} from '../../api/assessmentApi.js'
import { getArticleDetail } from '../../api/articleApi.js'
import { formatDate } from '../../utils/formatDate.js'
import './PublishingQueue.css'
import '../../styles/Flow2.css'

/**
 * Man Truong ban duyet dang bai (UC-33): danh sach bai da cong bo diem cho dang, tim kiem, xem chi
 * tiet, duyet dang (PUBLISHED) va huy duyet xuat ban (Alternative/Exception).
 *
 * MO TRANG NAY TAI: /publishing
 */

export default function PublishingQueue({ nguoiDung }) {
  const [tabHienTai, setTabHienTai] = useState('CHO_DANG') // 'CHO_DANG' | 'DA_DANG'
  const [danhSachChoDang, setDanhSachChoDang] = useState([])
  const [danhSachDaDang, setDanhSachDaDang] = useState([])
  const [dangTai, setDangTai] = useState(false)
  const [thongBao, setThongBao] = useState('')
  const [loi, setLoi] = useState('')
  const [tuKhoa, setTuKhoa] = useState('')

  // Modal kiểm duyệt xuất bản
  const [baiKiemDuyet, setBaiKiemDuyet] = useState(null)
  const [chiTietBai, setChiTietBai] = useState(null)
  const [dangTaiChiTiet, setDangTaiChiTiet] = useState(false)
  const [loiChiTiet, setLoiChiTiet] = useState('')
  const [dangXuLy, setDangXuLy] = useState(false)

  // Modal hủy duyệt xuất bản
  const [baiHuyDuyet, setBaiHuyDuyet] = useState(null)
  const [lyDoHuy, setLyDoHuy] = useState('')
  const [dangXuLyHuy, setDangXuLyHuy] = useState(false)

  useEffect(() => {
    taiDuLieu()
  }, [])

  // Tải cả danh sách chờ xuất bản và đã xuất bản (dùng Promise.allSettled để không bị chặn lẫn nhau)
  async function taiDuLieu() {
    setDangTai(true)
    setLoi('')
    try {
      const [choDangRes, daDangRes] = await Promise.allSettled([
        layDanhSachChoDuyetDang(),
        layDanhSachDaXuatBan()
      ])

      if (choDangRes.status === 'fulfilled') {
        setDanhSachChoDang(choDangRes.value || [])
      } else {
        setLoi(choDangRes.reason?.message || 'Không thể tải danh sách chờ duyệt xuất bản')
      }

      if (daDangRes.status === 'fulfilled') {
        setDanhSachDaDang(daDangRes.value || [])
      } else if (choDangRes.status === 'fulfilled') {
        // Nếu chỉ lỗi danh sách đã xuất bản (vd backend cũ chưa nạp endpoint)
        setLoi(daDangRes.reason?.message || 'Không thể tải danh sách đã xuất bản')
      }
    } catch (err) {
      setLoi(err.message || 'Không thể tải danh sách xuất bản bài viết')
    } finally {
      setDangTai(false)
    }
  }

  // Mo hop kiem duyet: tai noi dung bai de doc truoc khi dang
  async function moModalKiemDuyet(item) {
    setBaiKiemDuyet(item)
    setDangTaiChiTiet(true)
    setChiTietBai(null)
    setLoiChiTiet('')
    try {
      const chiTiet = await getArticleDetail(item.articleId)
      setChiTietBai(chiTiet)
    } catch (err) {
      setLoiChiTiet(err.message)
    } finally {
      setDangTaiChiTiet(false)
    }
  }

  // Dong hop kiem duyet (UC-33 A1: hoãn duyệt xuất bản)
  function dongModal() {
    if (dangXuLy) return
    setBaiKiemDuyet(null)
    setChiTietBai(null)
  }

  // UC-33: duyet dang -> bai PUBLISHED, ghi ngay dang (BR-SC-11)
  async function xuLyDuyetDang() {
    if (!baiKiemDuyet) return
    setDangXuLy(true)
    setLoi('')
    try {
      await duyetDangBaiViet(baiKiemDuyet.articleId)
      setThongBao(`Duyệt xuất bản thành công bài viết: "${baiKiemDuyet.title}". Bài đã chuyển sang trạng thái ĐÃ XUẤT BẢN và bàn giao sang tính nhuận bút.`)
      setBaiKiemDuyet(null)
      setChiTietBai(null)
      taiDuLieu()
    } catch (err) {
      setLoi(err.message || 'Duyệt xuất bản bài viết thất bại')
    } finally {
      setDangXuLy(false)
    }
  }

  // Mở modal hủy duyệt xuất bản
  function moModalHuyDuyet(item) {
    setBaiHuyDuyet(item)
    setLyDoHuy('')
    setLoi('')
  }

  // Đóng modal hủy duyệt xuất bản
  function dongModalHuyDuyet() {
    if (dangXuLyHuy) return
    setBaiHuyDuyet(null)
    setLyDoHuy('')
  }

  // UC-33 Alternative/Exception: Hủy duyệt xuất bản -> bai tro ve SCORE_PUBLISHED
  async function xuLyHuyDuyet() {
    if (!baiHuyDuyet) return
    setDangXuLyHuy(true)
    setLoi('')
    try {
      await huyDuyetDangBaiViet(baiHuyDuyet.articleId, lyDoHuy)
      setThongBao(`Đã hủy duyệt xuất bản bài viết: "${baiHuyDuyet.title}". Bài viết đã quay về trạng thái Chờ duyệt xuất bản (SCORE_PUBLISHED).`)
      setBaiHuyDuyet(null)
      setLyDoHuy('')
      taiDuLieu()
    } catch (err) {
      setLoi(err.message || 'Hủy duyệt xuất bản bài viết thất bại')
    } finally {
      setDangXuLyHuy(false)
    }
  }

  // Lấy danh sách hiển thị theo tab
  const danhSachHienTai = tabHienTai === 'CHO_DANG' ? danhSachChoDang : danhSachDaDang

  // Lọc theo từ khóa tìm kiếm
  const danhSachLoc = danhSachHienTai.filter(item => {
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
  const tongSoBaiChoDang = danhSachChoDang.length
  const tongSoBaiDaDang = danhSachDaDang.length
  const soBaiTrongHanKhieuNai = danhSachChoDang.filter(item => item.isAppealWindowOpen).length
  const diemTb = danhSachChoDang.length > 0
    ? (danhSachChoDang.reduce((sum, item) => sum + Number(item.totalScore || 0), 0) / danhSachChoDang.length).toFixed(2)
    : '0.00'

  return (
    <div className="duyet-dang">
      <div className="duyet-dang__the">
        <div className="duyet-dang__dau">
          <div>
            <h2 className="duyet-dang__tieu-de">Quản lý duyệt xuất bản bài viết</h2>
            <p className="duyet-dang__mo-ta">
              Xem xét các bài viết đã hoàn thiện đánh giá chất lượng để chính thức phát hành lên báo trực tuyến hoặc hủy duyệt xuất bản.
            </p>
          </div>
          <button
            type="button"
            className="duyet-dang__nut-lam-moi"
            onClick={taiDuLieu}
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
            <span className="duyet-dang__thong-ke-so">{tongSoBaiChoDang}</span>
            <span className="duyet-dang__thong-ke-ghi-chu">Bài đã công bố điểm</span>
          </div>
          <div className="duyet-dang__thong-ke-the">
            <span className="duyet-dang__thong-ke-nhan">Đã xuất bản trực tuyến</span>
            <span className="duyet-dang__thong-ke-so" style={{ color: '#16a34a' }}>{tongSoBaiDaDang}</span>
            <span className="duyet-dang__thong-ke-ghi-chu">Có thể hủy duyệt nếu cần</span>
          </div>
          <div className="duyet-dang__thong-ke-the">
            <span className="duyet-dang__thong-ke-nhan">Trong hạn khiếu nại (7 ngày)</span>
            <span className="duyet-dang__thong-ke-so" style={{ color: '#7c3aed' }}>{soBaiTrongHanKhieuNai}</span>
            <span className="duyet-dang__thong-ke-ghi-chu">Có thể duyệt ngay hoặc chờ</span>
          </div>
          <div className="duyet-dang__thong-ke-the">
            <span className="duyet-dang__thong-ke-nhan">Điểm chất lượng trung bình</span>
            <span className="duyet-dang__thong-ke-so" style={{ color: '#0284c7' }}>{diemTb}</span>
            <span className="duyet-dang__thong-ke-ghi-chu">Hàng chờ duyệt</span>
          </div>
        </div>

        {/* Tabs chuyển đổi Chờ duyệt xuất bản / Đã xuất bản */}
        <div className="duyet-dang__tabs">
          <button
            type="button"
            className={`duyet-dang__tab-nut ${tabHienTai === 'CHO_DANG' ? 'duyet-dang__tab-nut--kich-hoat' : ''}`}
            onClick={() => setTabHienTai('CHO_DANG')}
          >
            Chờ duyệt xuất bản ({tongSoBaiChoDang})
          </button>
          <button
            type="button"
            className={`duyet-dang__tab-nut ${tabHienTai === 'DA_DANG' ? 'duyet-dang__tab-nut--kich-hoat' : ''}`}
            onClick={() => setTabHienTai('DA_DANG')}
          >
            Đã xuất bản ({tongSoBaiDaDang})
          </button>
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
          <div className="duyet-dang__trong">Đang tải danh sách bài viết...</div>
        ) : danhSachLoc.length === 0 ? (
          <div className="duyet-dang__trong">
            {tuKhoa
              ? 'Không tìm thấy bài viết nào khớp với từ khóa tìm kiếm.'
              : tabHienTai === 'CHO_DANG'
              ? 'Hiện không có bài viết nào chờ duyệt xuất bản.'
              : 'Chưa có bài viết nào đã xuất bản trong ban của bạn.'}
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
                  <th>{tabHienTai === 'CHO_DANG' ? 'Ngày công bố' : 'Ngày xuất bản'}</th>
                  {tabHienTai === 'CHO_DANG' && <th>Cửa sổ khiếu nại</th>}
                  {tabHienTai === 'DA_DANG' && <th>Trạng thái</th>}
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
                    {tabHienTai === 'CHO_DANG' && (
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
                    )}
                    {tabHienTai === 'DA_DANG' && (
                      <td>
                        <span className="duyet-dang__huy-hieu" style={{ background: '#dcfce7', color: '#15803d' }}>
                          Đã xuất bản
                        </span>
                      </td>
                    )}
                    <td style={{ textAlign: 'right' }}>
                      {tabHienTai === 'CHO_DANG' ? (
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
                      ) : (
                        <button
                          type="button"
                          className="duyet-dang__nut-huy-dang"
                          onClick={() => moModalHuyDuyet(item)}
                        >
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                            <circle cx="12" cy="12" r="10" />
                            <line x1="15" y1="9" x2="9" y2="15" />
                            <line x1="9" y1="9" x2="15" y2="15" />
                          </svg>
                          Hủy duyệt đăng
                        </button>
                      )}
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
                ) : loiChiTiet ? (
                  <p className="thong-bao-loi" role="alert">Không tải được nội dung bài: {loiChiTiet}</p>
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
                Đóng (Hoãn duyệt)
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

      {/* Modal xác nhận Hủy duyệt xuất bản */}
      {baiHuyDuyet && (
        <div className="duyet-dang__modal-nen" onClick={dongModalHuyDuyet}>
          <div className="duyet-dang__modal-hop" style={{ maxWidth: '550px' }} onClick={(e) => e.stopPropagation()}>
            <div className="duyet-dang__modal-dau">
              <h3>Xác nhận hủy duyệt xuất bản</h3>
              <button
                type="button"
                className="duyet-dang__nut-dong"
                onClick={dongModalHuyDuyet}
                disabled={dangXuLyHuy}
              >
                &times;
              </button>
            </div>

            <div className="duyet-dang__modal-than">
              <p style={{ margin: '0 0 12px 0', fontSize: '14px', color: 'var(--fm-chu)' }}>
                Bạn có chắc chắn muốn hủy duyệt xuất bản bài viết sau không?
              </p>
              <div style={{ padding: '12px', background: 'var(--fm-nen)', borderRadius: '6px', marginBottom: '16px' }}>
                <strong style={{ color: 'var(--fm-chu)', fontSize: '14px' }}>{baiHuyDuyet.title}</strong>
                <div style={{ fontSize: '12px', color: 'var(--fm-chu-nhat)', marginTop: '4px' }}>
                  Tác giả: {baiHuyDuyet.authorName} • Thể loại: {baiHuyDuyet.categoryName} • Điểm: {Number(baiHuyDuyet.totalScore).toFixed(2)}đ
                </div>
              </div>

              <div className="duyet-dang__canh-bao" style={{ background: '#fef2f2', borderColor: '#fca5a5', color: '#991b1b' }}>
                <strong>Lưu ý:</strong> Bài viết sẽ được thu hồi khỏi trạng thái ĐÃ XUẤT BẢN và quay trở về danh sách <strong>Chờ duyệt xuất bản (SCORE_PUBLISHED)</strong>. Ngày phát hành sẽ được gỡ bỏ cho đến khi có quyết định duyệt lại.
              </div>

              <label style={{ display: 'block', marginTop: '14px', fontSize: '13px', fontWeight: '500', color: 'var(--fm-chu)' }}>
                Lý do hủy duyệt xuất bản (tùy chọn):
                <textarea
                  className="duyet-dang__o-nhap-ly-do"
                  rows="3"
                  placeholder="Nhập lý do thu hồi / hủy duyệt xuất bản bài viết này..."
                  value={lyDoHuy}
                  onChange={(e) => setLyDoHuy(e.target.value)}
                  disabled={dangXuLyHuy}
                />
              </label>
            </div>

            <div className="duyet-dang__modal-duoi">
              <button
                type="button"
                className="duyet-dang__nut-phu"
                onClick={dongModalHuyDuyet}
                disabled={dangXuLyHuy}
              >
                Quay lại
              </button>
              <button
                type="button"
                className="duyet-dang__nut-xac-nhan-huy"
                onClick={xuLyHuyDuyet}
                disabled={dangXuLyHuy}
              >
                {dangXuLyHuy ? 'Đang xử lý hủy...' : 'Xác nhận hủy duyệt'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
