import { useState, useEffect } from 'react'
import { useParams, useSearchParams, useNavigate } from 'react-router-dom'
import { layChiTietKpiPhongVien } from '../../api/kpiApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import '../../styles/ReporterKpiDetails.css'

/**
 * Man hinh Chi tiet KPI cua mot phong vien (UC-62: View Reporter KPI Details).
 * Danh cho Truong ban bien tap theo doi cac bai viet da xuat ban, ty le quy doi
 * dong tac gia (0.6 / 0.4), diem chat luong va luot xem tich luy.
 *
 * MUC LUC (so dong)
 *   21  ReporterKpiDetails           - Man hinh Chi tiet KPI cua mot phong vien (UC-62: View Reporter KPI Deta…
 *   33    useEffect                  - tai du lieu khi mo trang
 *   39    taiChiTietKpi()
 *   53    doiNam()
 *   57    doiThang()
 *   61    quayLaiBangChung()
 */
export default function ReporterKpiDetails({ nguoiDung }) {
  const { reporterId } = useParams()
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()

  const nam = Number(searchParams.get('year')) || 2026
  const thang = Number(searchParams.get('month')) || 9

  const [duLieu, setDuLieu] = useState(null)
  const [dangTai, setDangTai] = useState(false)
  const [loi, setLoi] = useState('')

  useEffect(() => {
    if (reporterId) {
      taiChiTietKpi(reporterId, nam, thang)
    }
  }, [reporterId, nam, thang])

  async function taiChiTietKpi(id, y, m) {
    setDangTai(true)
    setLoi('')
    try {
      const data = await layChiTietKpiPhongVien(id, y, m)
      setDuLieu(data)
    } catch (err) {
      setLoi(err.message)
      setDuLieu(null)
    } finally {
      setDangTai(false)
    }
  }

  function doiNam(namMoi) {
    setSearchParams({ year: namMoi, month: thang })
  }

  function doiThang(thangMoi) {
    setSearchParams({ year: nam, month: thangMoi })
  }

  function quayLaiBangChung() {
    navigate(`/kpi-board?year=${nam}&month=${thang}`)
  }

  const articles = duLieu?.articles || []

  return (
    <div className="chi-tiet-kpi-pv">
      {/* Dieu huong quay lai */}
      <div className="chi-tiet-kpi-pv__dieu-huong">
        <button
          type="button"
          className="chi-tiet-kpi-pv__btn-quay-lai"
          onClick={quayLaiBangChung}
        >
          ← Quay lại Bảng KPI Ban
        </button>
      </div>

      {/* Header & Bo loc */}
      <div className="chi-tiet-kpi-pv__header">
        <div>
          <h1 className="chi-tiet-kpi-pv__tieu-de">
            Chi tiết KPI: {duLieu?.reporterName || `Phóng viên #${reporterId}`}
          </h1>
          <p className="chi-tiet-kpi-pv__mo-ta">
            {duLieu?.reporterEmail} • Theo dõi bài viết xuất bản, tỷ lệ phân chia tác giả và số liệu độc giả (UC-62).
          </p>
        </div>

        <div className="chi-tiet-kpi-pv__bo-loc">
          <select
            className="chi-tiet-kpi-pv__select"
            value={nam}
            onChange={(e) => doiNam(Number(e.target.value))}
          >
            <option value={2025}>Năm 2025</option>
            <option value={2026}>Năm 2026</option>
            <option value={2027}>Năm 2027</option>
          </select>

          <select
            className="chi-tiet-kpi-pv__select"
            value={thang}
            onChange={(e) => doiThang(Number(e.target.value))}
          >
            {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
              <option key={m} value={m}>Tháng {m}</option>
            ))}
          </select>
        </div>
      </div>

      {loi && <div className="chi-tiet-kpi-pv__thong-bao-loi">{loi}</div>}

      {/* Thong ke tong quan */}
      <div className="chi-tiet-kpi-pv__thong-ke">
        <div className="chi-tiet-kpi-pv__the-thong-ke">
          <span className="chi-tiet-kpi-pv__the-nhan">Bài quy đổi / Chỉ tiêu</span>
          <span className="chi-tiet-kpi-pv__the-so">
            {formatNumber(duLieu?.exactActualArticles)} / {duLieu?.minPublished ?? 0}
          </span>
          <span className="chi-tiet-kpi-pv__the-phu">
            Tiến độ: {formatNumber(duLieu?.articleProgressPercent)}%
          </span>
        </div>

        <div className="chi-tiet-kpi-pv__the-thong-ke">
          <span className="chi-tiet-kpi-pv__the-nhan">Điểm TB / Điểm sàn</span>
          <span className="chi-tiet-kpi-pv__the-so">
            {duLieu?.actualAvgScore !== null ? formatNumber(duLieu?.actualAvgScore) : '—'}
            {' / '}
            {formatNumber(duLieu?.minAvgScore)}
          </span>
          <span className="chi-tiet-kpi-pv__the-phu">Thang điểm 0 - 100</span>
        </div>

        <div className="chi-tiet-kpi-pv__the-thong-ke">
          <span className="chi-tiet-kpi-pv__the-nhan">Tổng lượt xem độc giả</span>
          <span className="chi-tiet-kpi-pv__the-so chi-tiet-kpi-pv__the-so--views">
            {formatNumber(duLieu?.totalViews ?? 0)}
          </span>
          <span className="chi-tiet-kpi-pv__the-phu">Tích lũy từ reader data</span>
        </div>

        <div className="chi-tiet-kpi-pv__the-thong-ke">
          <span className="chi-tiet-kpi-pv__the-nhan">Đánh giá tháng ({duLieu?.snapshotStatus || 'DRAFT'})</span>
          <div>
            <span
              className={`chi-tiet-kpi-pv__badge-status ${
                duLieu?.projectedStatus === 'PASS'
                  ? 'chi-tiet-kpi-pv__badge-status--pass'
                  : 'chi-tiet-kpi-pv__badge-status--not-pass'
              }`}
            >
              {duLieu?.projectedStatus || 'NOT_PASS'}
            </span>
          </div>
          <span className="chi-tiet-kpi-pv__the-phu">Quy tắc nhị phân BR-KPI-05</span>
        </div>
      </div>

      {/* Ghi chu nhan xet cua Truong ban (neu co) */}
      {duLieu?.notes && (
        <div className="chi-tiet-kpi-pv__ghi-chu-box">
          <div className="chi-tiet-kpi-pv__ghi-chu-tieu-de">Nhận xét đánh giá của Trưởng ban:</div>
          <div className="chi-tiet-kpi-pv__ghi-chu-noi-dung">{duLieu.notes}</div>
        </div>
      )}

      {/* Bang danh sach bai viet da xuat ban */}
      <div className="chi-tiet-kpi-pv__khung-bang">
        <div className="chi-tiet-kpi-pv__bang-tieu-de-khung">
          <span className="chi-tiet-kpi-pv__bang-tieu-de">
            Danh sách tác phẩm xuất bản trong kỳ ({articles.length} bài)
          </span>
        </div>

        {dangTai ? (
          <div className="chi-tiet-kpi-pv__trang-thai-rong">Đang tải chi tiết bài viết...</div>
        ) : articles.length === 0 ? (
          <div className="chi-tiet-kpi-pv__trang-thai-rong">
            Không có tác phẩm nào xuất bản trong Tháng {thang}/{nam}.
          </div>
        ) : (
          <table className="chi-tiet-kpi-pv__table">
            <thead>
              <tr>
                <th style={{ width: '45%' }}>Tác phẩm</th>
                <th style={{ textAlign: 'center' }}>Ngày xuất bản</th>
                <th style={{ textAlign: 'center' }}>Tính chất</th>
                <th style={{ textAlign: 'center' }}>Tỷ lệ tác giả</th>
                <th style={{ textAlign: 'center' }}>Điểm chất lượng</th>
                <th style={{ textAlign: 'right' }}>Lượt xem</th>
              </tr>
            </thead>
            <tbody>
              {articles.map((art) => (
                <tr key={art.articleId}>
                  <td>
                    <div className="chi-tiet-kpi-pv__bai-tieu-de">{art.title}</div>
                    <div className="chi-tiet-kpi-pv__bai-sub">Mã bài: #{art.articleId}</div>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    {formatDate(art.publishedAt)}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span className="chi-tiet-kpi-pv__badge-nature">{art.nature}</span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <div className="chi-tiet-kpi-pv__ty-le-dong-tac-gia">
                      <span className="chi-tiet-kpi-pv__ty-le-chinh">
                        {formatNumber(art.sharePercent)}%
                      </span>
                      <span className="chi-tiet-kpi-pv__ty-le-quy-doi">
                        ({formatNumber(art.convertedArticles)} bài)
                      </span>
                    </div>
                  </td>
                  <td style={{ textAlign: 'center', fontWeight: '600' }}>
                    {art.effectiveScore !== null ? formatNumber(art.effectiveScore) : '—'}
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 'bold' }}>
                    {formatNumber(art.pageViews)}
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
