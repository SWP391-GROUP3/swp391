import { useState, useEffect } from 'react'
import { layBaoCaoSuKienXuHuong } from '../../api/kpiApi.js'
import { formatNumber } from '../../utils/formatNumber.js'
import { formatDate } from '../../utils/formatDate.js'
import './TrendingEventsReport.css'

/**
 * Man hinh Bao cao su kien xu huong & phat hien tang dot bien (UC-65: View Trending Events Report).
 * Danh cho Truong ban bien tap (Head of Editorial Department) theo doi luu luong doc gia
 * va phat hien cac chu de, su kien nong co luot xem trong 7 ngay gan nhat vuot gap doi (> 2x)
 * muc trung binh tuan cua 4 tuan co so truoc do.
 *
 * MO TRANG NAY TAI: /kpi-trending
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  TrendingEventsReport()          xem tom tat dau file
 *   31    useEffect                     tai du lieu khi mo trang
 *   35    taiDuLieuBaoCao()             tai du lieu bao cao
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  114  [Dang tai... / Lam moi]         taiDuLieuBaoCao() -> goi GET url
 */
export default function TrendingEventsReport({ nguoiDung }) {
  const [ngayMoc, setNgayMoc] = useState('')
  const [tuKhoa, setTuKhoa] = useState('')
  const [boLocTrangThai, setBoLocTrangThai] = useState('ALL') // ALL, SURGE, NORMAL
  const [duLieu, setDuLieu] = useState(null)
  const [dangTai, setDangTai] = useState(false)
  const [loi, setLoi] = useState('')

  useEffect(() => {
    taiDuLieuBaoCao(ngayMoc)
  }, [ngayMoc])

  async function taiDuLieuBaoCao(date) {
    setDangTai(true)
    setLoi('')
    try {
      const data = await layBaoCaoSuKienXuHuong(date || null)
      setDuLieu(data)
      if (!ngayMoc && data?.anchorDate) {
        setNgayMoc(data.anchorDate)
      }
    } catch (err) {
      setLoi(err.message || 'Không thể tải báo cáo sự kiện xu hướng')
      setDuLieu(null)
    } finally {
      setDangTai(false)
    }
  }

  const items = duLieu?.items || []
  const danhSachLoc = items.filter((item) => {
    // Loc theo tu khoa
    if (tuKhoa.trim()) {
      const kw = tuKhoa.toLowerCase().trim()
      const ten = item.eventName ? item.eventName.toLowerCase() : ''
      if (!ten.includes(kw)) {
        return false
      }
    }
    // Loc theo trang thai dot bien
    if (boLocTrangThai === 'SURGE') {
      return item.surgeDetected === true
    }
    if (boLocTrangThai === 'NORMAL') {
      return item.surgeDetected === false
    }
    return true
  })

  return (
    <div className="bc-xu-huong">
      {/* Header & Controls */}
      <div className="bc-xu-huong__header">
        <div>
          <h1 className="bc-xu-huong__tieu-de">Báo cáo sự kiện xu hướng & Phát hiện đột biến</h1>
          <p className="bc-xu-huong__mo-ta">
            Theo dõi xu hướng quan tâm của độc giả và phát hiện sự kiện bùng nổ lưu lượng theo quy tắc: 7 ngày gần nhất &gt; 2x mức trung bình 4 tuần cơ sở (UC-65).
          </p>
        </div>

        <div className="bc-xu-huong__bo-loc">
          <label style={{ fontSize: '13px', color: 'var(--fm-chu-nhat, #707070)' }}>
            Ngày đánh giá:
          </label>
          <input
            type="date"
            className="bc-xu-huong__input-ngay"
            value={ngayMoc}
            onChange={(e) => setNgayMoc(e.target.value)}
          />

          <input
            type="text"
            className="bc-xu-huong__input-tim-kiem"
            placeholder="Tìm theo tên sự kiện..."
            value={tuKhoa}
            onChange={(e) => setTuKhoa(e.target.value)}
          />

          <select
            className="bc-xu-huong__input-ngay"
            value={boLocTrangThai}
            onChange={(e) => setBoLocTrangThai(e.target.value)}
          >
            <option value="ALL">Tất cả sự kiện</option>
            <option value="SURGE">Chỉ sự kiện tăng đột biến</option>
            <option value="NORMAL">Sự kiện bình thường</option>
          </select>

          <button
            className="bc-xu-huong__btn-lam-moi"
            onClick={() => taiDuLieuBaoCao(ngayMoc)}
            disabled={dangTai}
          >
            {dangTai ? 'Đang tải...' : 'Làm mới'}
          </button>
        </div>
      </div>

      {/* Thông báo lỗi nếu có */}
      {loi && <div className="bc-xu-huong__thong-bao-loi">{loi}</div>}

      {/* Banner cảnh báo nếu có sự kiện tăng đột biến */}
      {duLieu?.surgeCount > 0 && (
        <div className="bc-xu-huong__canh-bao">
          <span style={{ fontSize: '18px' }}>⚡</span>
          <span>
            <strong>Phát hiện {duLieu.surgeCount} sự kiện tăng đột biến (Surge)!</strong> Lưu lượng độc giả trong 7 ngày gần nhất đã vượt hơn 200% so với mức trung bình 4 tuần cơ sở. Ban biên tập nên tập trung ưu tiên tài nguyên bài viết cho các chủ đề này.
          </span>
        </div>
      )}

      {/* Thẻ thống kê tổng quan */}
      <div className="bc-xu-huong__thong-ke">
        <div className="bc-xu-huong__the">
          <span className="bc-xu-huong__the-nhan">Tổng số sự kiện</span>
          <span className="bc-xu-huong__the-so">{duLieu?.totalEvents ?? 0}</span>
          <span className="bc-xu-huong__the-phu">Sự kiện có trong hệ thống</span>
        </div>
        <div className="bc-xu-huong__the">
          <span className="bc-xu-huong__the-nhan">Sự kiện tăng đột biến</span>
          <span className="bc-xu-huong__the-so bc-xu-huong__the-so--dot-bien">
            {duLieu?.surgeCount ?? 0}
          </span>
          <span className="bc-xu-huong__the-phu">Vượt ngưỡng &gt; 2x baseline</span>
        </div>
        <div className="bc-xu-huong__the">
          <span className="bc-xu-huong__the-nhan">Cửa sổ 7 ngày gần nhất</span>
          <span className="bc-xu-huong__the-so" style={{ fontSize: '16px', fontWeight: '600' }}>
            {duLieu?.recentWindowStart ? formatDate(duLieu.recentWindowStart) : '—'} đến{' '}
            {duLieu?.recentWindowEnd ? formatDate(duLieu.recentWindowEnd) : '—'}
          </span>
          <span className="bc-xu-huong__the-phu">Chu kỳ đánh giá trực tiếp</span>
        </div>
        <div className="bc-xu-huong__the">
          <span className="bc-xu-huong__the-nhan">Cửa sổ 4 tuần cơ sở</span>
          <span className="bc-xu-huong__the-so" style={{ fontSize: '16px', fontWeight: '600' }}>
            {duLieu?.baselineWindowStart ? formatDate(duLieu.baselineWindowStart) : '—'} đến{' '}
            {duLieu?.baselineWindowEnd ? formatDate(duLieu.baselineWindowEnd) : '—'}
          </span>
          <span className="bc-xu-huong__the-phu">Trung bình tuần cơ sở</span>
        </div>
      </div>

      {/* Bảng dữ liệu sự kiện */}
      <div className="bc-xu-huong__bang-khung">
        {dangTai ? (
          <div className="bc-xu-huong__trang-thai-rong">Đang tải dữ liệu phân tích sự kiện...</div>
        ) : danhSachLoc.length === 0 ? (
          <div className="bc-xu-huong__trang-thai-rong">Không có sự kiện nào phù hợp với bộ lọc.</div>
        ) : (
          <table className="bc-xu-huong__table">
            <thead>
              <tr>
                <th>Tên sự kiện</th>
                <th>Thời gian diễn ra</th>
                <th style={{ textAlign: 'center' }}>Số bài liên kết</th>
                <th style={{ textAlign: 'right' }}>Lượt xem 7 ngày</th>
                <th style={{ textAlign: 'right' }}>TB tuần cơ sở</th>
                <th style={{ textAlign: 'center' }}>Hệ số tăng trưởng</th>
                <th style={{ textAlign: 'center' }}>Trạng thái phân tích</th>
              </tr>
            </thead>
            <tbody>
              {danhSachLoc.map((item) => (
                <tr key={item.eventId}>
                  <td>
                    <div className="bc-xu-huong__ten-su-kien">{item.eventName}</div>
                    <div className="bc-xu-huong__thoi-gian">Mã sự kiện: #{item.eventId}</div>
                  </td>
                  <td>
                    <div style={{ fontSize: '13px' }}>
                      {formatDate(item.startDate)} - {formatDate(item.endDate)}
                    </div>
                    <span
                      style={{
                        fontSize: '11px',
                        color: item.isActive ? 'var(--fm-tot, #1f6f1a)' : 'var(--fm-chu-nhat, #707070)',
                      }}
                    >
                      {item.isActive ? '● Đang diễn ra' : '○ Đã đóng'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center', fontWeight: '600' }}>
                    {formatNumber(item.articleCount)}
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 'bold' }}>
                    {formatNumber(item.recent7DaysViews)}
                  </td>
                  <td style={{ textAlign: 'right' }}>
                    {item.baselineWeeklyViews !== null ? formatNumber(item.baselineWeeklyViews) : '0'}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span
                      className={
                        item.surgeDetected
                          ? 'bc-xu-huong__ty-le-dot-bien'
                          : 'bc-xu-huong__ty-le-binh-thuong'
                      }
                    >
                      {item.surgeRatio !== null ? `${Number(item.surgeRatio).toFixed(2)}x` : '—'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span
                      className={`bc-xu-huong__badge ${
                        item.statusLabel === 'TĂNG ĐỘT BIẾN'
                          ? 'bc-xu-huong__badge--dot-bien'
                          : item.statusLabel === 'BÌNH THƯỜNG'
                          ? 'bc-xu-huong__badge--binh-thuong'
                          : 'bc-xu-huong__badge--chua-du'
                      }`}
                    >
                      {item.statusLabel}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
