import { useState, useEffect } from 'react'
import { danhSachNam, namHienTai, thangHienTai } from './kyKpiMacDinh.js'
import { layBaoCaoTheLoai } from '../../api/kpiApi.js'
import { formatMoney } from '../../utils/formatMoney.js'
import { formatNumber } from '../../utils/formatNumber.js'
import './GenrePopularityReport.css'

/**
 * Man hinh Bao cao muc do quan tam the loai & chi phi nhuan but (UC-64: View Genre Popularity Report).
 * Danh cho Truong ban bien tap (Head of Editorial Department) theo doi cac chi so:
 * Mean/Median views, Bayesian rating, Royalty cost / 1.000 views, diem chat luong trung binh.
 *
 * MO TRANG NAY TAI: /kpi-reports
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  GenrePopularityReport()         xem tom tat dau file
 *   31    useEffect                     tai du lieu khi mo trang
 *   35    taiDuLieuBaoCao()             tai du lieu bao cao
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   96  [Dang tai... / Lam moi]         taiDuLieuBaoCao() -> goi GET url
 */
export default function GenrePopularityReport({ nguoiDung }) {
  const [nam, setNam] = useState(namHienTai())
  const [thang, setThang] = useState(thangHienTai())
  const [tuKhoa, setTuKhoa] = useState('')
  const [duLieu, setDuLieu] = useState(null)
  const [dangTai, setDangTai] = useState(false)
  const [loi, setLoi] = useState('')

  useEffect(() => {
    taiDuLieuBaoCao(nam, thang)
  }, [nam, thang])

  async function taiDuLieuBaoCao(y, m) {
    setDangTai(true)
    setLoi('')
    try {
      const data = await layBaoCaoTheLoai(y, m > 0 ? m : null)
      setDuLieu(data)
    } catch (err) {
      setLoi(err.message || 'Không thể tải dữ liệu báo cáo thể loại')
      setDuLieu(null)
    } finally {
      setDangTai(false)
    }
  }

  const items = duLieu?.items || []
  const danhSachLoc = items.filter((item) => {
    if (!tuKhoa.trim()) return true
    const kw = tuKhoa.toLowerCase().trim()
    return (
      (item.genreName && item.genreName.toLowerCase().includes(kw)) ||
      (item.genreCode && item.genreCode.toLowerCase().includes(kw)) ||
      (item.ndGroup && item.ndGroup.toLowerCase().includes(kw))
    )
  })

  return (
    <div className="bc-the-loai">
      {/* Header & Controls */}
      <div className="bc-the-loai__header">
        <div>
          <h1 className="bc-the-loai__tieu-de">Báo cáo mức độ quan tâm thể loại & chi phí nhuận bút</h1>
          <p className="bc-the-loai__mo-ta">
            Phân tích thị hiếu độc giả, điểm đánh giá Bayesian và hiệu quả chi phí nhuận bút theo thể loại (UC-64, BR-KPI-04).
          </p>
        </div>

        <div className="bc-the-loai__bo-loc">
          <select
            className="bc-the-loai__select"
            value={nam}
            onChange={(e) => setNam(Number(e.target.value))}
          >
            {danhSachNam().map((n) => <option key={n} value={n}>Năm {n}</option>)}
            <option value={2027}>Năm 2027</option>
          </select>

          <select
            className="bc-the-loai__select"
            value={thang}
            onChange={(e) => setThang(Number(e.target.value))}
          >
            <option value={0}>Cả năm</option>
            {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
              <option key={m} value={m}>
                Tháng {m}
              </option>
            ))}
          </select>

          <button
            className="bc-the-loai__btn-tai-lai"
            onClick={() => taiDuLieuBaoCao(nam, thang)}
            disabled={dangTai}
          >
            {dangTai ? 'Đang tải...' : 'Làm mới'}
          </button>
        </div>
      </div>

      {/* Thông báo lỗi nếu có */}
      {loi && <div className="bc-the-loai__loi">{loi}</div>}

      {/* Summary KPI Cards */}
      <div className="bc-the-loai__cards">
        <div className="bc-the-loai__card">
          <div className="bc-the-loai__card-nhan">Tổng thể loại / Bài xuất bản</div>
          <div className="bc-the-loai__card-gia-tri">
            {duLieu?.totalGenres ?? 0} <span style={{ fontSize: '15px', fontWeight: 500 }}>thể loại</span>
          </div>
          <div className="bc-the-loai__card-phu">
            Tổng {duLieu?.totalArticles ?? 0} bài đã xuất bản
          </div>
        </div>

        <div className="bc-the-loai__card">
          <div className="bc-the-loai__card-nhan">Tổng lượt xem độc giả</div>
          <div className="bc-the-loai__card-gia-tri bc-the-loai__card-gia-tri--chinh">
            {formatNumber(duLieu?.totalViews ?? 0)}
          </div>
          <div className="bc-the-loai__card-phu">
            Trung bình: {formatNumber(duLieu?.globalMeanViews ?? 0)} lượt / bài
          </div>
        </div>

        <div className="bc-the-loai__card">
          <div className="bc-the-loai__card-nhan">Điểm đánh giá Bayesian chung</div>
          <div className="bc-the-loai__card-gia-tri">
            {(duLieu?.globalRatingCount ?? 0) > 0
              ? <>
                  {formatNumber(duLieu?.globalBayesianRating)} / 5.0
                </>
              : '0 / 5.0'}
          </div>
          <div className="bc-the-loai__card-phu">Hệ số chuẩn m = 10 đánh giá</div>
        </div>

        <div className="bc-the-loai__card">
          <div className="bc-the-loai__card-nhan">Tổng chi phí nhuận bút</div>
          <div className="bc-the-loai__card-gia-tri">
            {formatMoney(duLieu?.totalRoyaltyPaid ?? 0)}
          </div>
          <div className="bc-the-loai__card-phu">Chi trả cho các bài đã duyệt</div>
        </div>
      </div>

      {/* Thanh công cụ tìm kiếm và ghi chú */}
      <div className="bc-the-loai__thanh-cong-cu">
        <input
          type="text"
          className="bc-the-loai__tim-kiem"
          placeholder="Tìm theo tên thể loại, mã hoặc nhóm..."
          value={tuKhoa}
          onChange={(e) => setTuKhoa(e.target.value)}
        />
        <div className="bc-the-loai__ghi-chu-luat">
          BR-KPI-04: Chi phí / 1.000 views = (Tổng nhuận bút / Tổng lượt xem) × 1.000
        </div>
      </div>

      {/* Bảng phân tích chi tiết */}
      <div className="bc-the-loai__bang-khung">
        {dangTai ? (
          <div className="bc-the-loai__dang-tai">Đang tổng hợp dữ liệu thể loại...</div>
        ) : danhSachLoc.length === 0 ? (
          <div className="bc-the-loai__trong">Không tìm thấy thể loại nào phù hợp.</div>
        ) : (
          <table className="bc-the-loai__bang">
            <thead>
              <tr>
                <th>Thể loại</th>
                <th>Hệ số chuẩn</th>
                <th>Số bài xuất bản</th>
                <th>Lượt xem TB (Mean)</th>
                <th>Lượt xem trung vị (Median)</th>
                <th>Bayesian Rating</th>
                <th>Điểm chất lượng TB</th>
                <th>Tổng nhuận bút</th>
                <th>Chi phí / 1.000 views</th>
                <th>Cảnh báo biên tập</th>
              </tr>
            </thead>
            <tbody>
              {danhSachLoc.map((item) => (
                <tr key={item.genreId}>
                  <td>
                    <div className="bc-the-loai__ten-the-loai">{item.genreName}</div>
                    <div className="bc-the-loai__ma-the-loai">
                      {item.genreCode} • {item.ndGroup}
                    </div>
                  </td>
                  <td>
                    <span className="bc-the-loai__so-chinh">{formatNumber(item.baseFactor)}</span>
                  </td>
                  <td>
                    <div className="bc-the-loai__so-chinh">{item.articleCount} bài</div>
                  </td>
                  <td>
                    <div className="bc-the-loai__so-chinh">{formatNumber(item.meanViews)}</div>
                    <div className="bc-the-loai__so-phu">Tổng: {formatNumber(item.totalViews)}</div>
                  </td>
                  <td>
                    <span className="bc-the-loai__so-chinh">{formatNumber(item.medianViews)}</span>
                  </td>
                  <td>
                    <div className="bc-the-loai__so-chinh">
                      {formatNumber(item.bayesianRating)} <span style={{ fontSize: '12px', color: '#667085' }}>/ 5.0</span>
                    </div>
                    <div className="bc-the-loai__so-phu">({item.ratingCount} lượt đánh giá)</div>
                  </td>
                  <td>
                    {item.averageQualityScore ? (
                      <span className="bc-the-loai__badge bc-the-loai__badge--tot">
                        {formatNumber(item.averageQualityScore)} / 100
                      </span>
                    ) : (
                      <span className="bc-the-loai__khong-co-so">—</span>
                    )}
                  </td>
                  <td>
                    <span className="bc-the-loai__so-chinh">{formatMoney(item.totalRoyaltyPaid)}</span>
                  </td>
                  <td>
                    {item.costPer1000Views !== null && item.costPer1000Views !== undefined ? (
                      <span className="bc-the-loai__so-chinh" style={{ color: 'var(--fm-chinh)' }}>
                        {formatMoney(item.costPer1000Views)}
                      </span>
                    ) : (
                      <span className="bc-the-loai__badge bc-the-loai__badge--phu" title="Chưa có dữ liệu lượt xem (UC-64 E1)">
                        N/A
                      </span>
                    )}
                  </td>
                  <td>
                    {item.insufficientData ? (
                      <span
                        className="bc-the-loai__badge bc-the-loai__badge--canh-bao"
                        title="Dưới 3 bài trong kỳ: Nguy cơ thiếu hụt nội dung biên tập"
                      >
                        Ít bài ({item.articleCount} &lt; 3)
                      </span>
                    ) : (
                      <span className="bc-the-loai__badge bc-the-loai__badge--tot">Đầy đủ</span>
                    )}
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
