import { useState, useEffect } from 'react'
import { danhSachNam, namHienTai, thangHienTai } from './kyKpiMacDinh.js'
import { layChiTietKpiPhongVien } from '../../api/kpiApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import '../../styles/PersonalKpiProgress.css'

/**
 * Man hinh KPI Ca nhan & Tien do (UC-60: View Personal KPI & Progress, UC-67: View Final KPI Result).
 * Danh cho Phong vien tu theo doi chi tieu duoc giao, tien do quy doi dong tac gia (0.6 / 0.4),
 * so lieu tuong tac doc gia va ket qua danh gia PASS / NOT_PASS.
 *
 * MO TRANG NAY TAI: /my-kpi
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  PersonalKpiProgress()           xem tom tat dau file
 *   26    useEffect                     tai du lieu khi mo trang
 *   32    taiKpiCaNhan()                tai kpi ca nhan
 */
export default function PersonalKpiProgress({ nguoiDung }) {
  const [nam, setNam] = useState(namHienTai())
  const [thang, setThang] = useState(thangHienTai())
  const [duLieu, setDuLieu] = useState(null)
  const [dangTai, setDangTai] = useState(false)
  const [loi, setLoi] = useState('')

  useEffect(() => {
    if (nguoiDung?.id) {
      taiKpiCaNhan(nguoiDung.id, nam, thang)
    }
  }, [nguoiDung?.id, nam, thang])

  async function taiKpiCaNhan(id, y, m) {
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

  const articles = duLieu?.articles || []
  const laChinhThuc = duLieu?.snapshotStatus === 'FINALIZED'

  return (
    <div className="kpi-ca-nhan">
      {/* Header & Bo loc */}
      <div className="kpi-ca-nhan__header">
        <div>
          <h1 className="kpi-ca-nhan__tieu-de">KPI Cá Nhân & Tiến Độ</h1>
          <p className="kpi-ca-nhan__mo-ta">
            Theo dõi chỉ tiêu tháng, số bài quy đổi theo tỷ lệ đồng tác giả, tương tác độc giả và kết quả đánh giá (UC-60, UC-67).
          </p>
        </div>

        <div className="kpi-ca-nhan__bo-loc">
          <select
            className="kpi-ca-nhan__select"
            value={nam}
            onChange={(e) => setNam(Number(e.target.value))}
          >
            {danhSachNam().map((n) => <option key={n} value={n}>Năm {n}</option>)}
            <option value={2027}>Năm 2027</option>
          </select>

          <select
            className="kpi-ca-nhan__select"
            value={thang}
            onChange={(e) => setThang(Number(e.target.value))}
          >
            {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
              <option key={m} value={m}>Tháng {m}</option>
            ))}
          </select>
        </div>
      </div>

      {loi && <div className="kpi-ca-nhan__thong-bao-loi">{loi}</div>}

      {/* Thong ke tong quan */}
      <div className="kpi-ca-nhan__thong-ke">
        <div className="kpi-ca-nhan__the-thong-ke">
          <span className="kpi-ca-nhan__the-nhan">Bài quy đổi / Chỉ tiêu</span>
          <span className="kpi-ca-nhan__the-so">
            {formatNumber(duLieu?.exactActualArticles)} / {duLieu?.minPublished ?? 0}
          </span>
          <span className="kpi-ca-nhan__the-phu">
            Tiến độ: {formatNumber(duLieu?.articleProgressPercent)}%
          </span>
        </div>

        <div className="kpi-ca-nhan__the-thong-ke">
          <span className="kpi-ca-nhan__the-nhan">Điểm TB / Điểm sàn</span>
          <span className="kpi-ca-nhan__the-so">
            {duLieu?.actualAvgScore !== null ? formatNumber(duLieu?.actualAvgScore) : '—'}
            {' / '}
            {formatNumber(duLieu?.minAvgScore)}
          </span>
          <span className="kpi-ca-nhan__the-phu">Thang điểm 0 - 100</span>
        </div>

        <div className="kpi-ca-nhan__the-thong-ke">
          <span className="kpi-ca-nhan__the-nhan">Lượt xem độc giả</span>
          <span className="kpi-ca-nhan__the-so kpi-ca-nhan__the-so--views">
            {formatNumber(duLieu?.totalViews ?? 0)}
          </span>
          <span className="kpi-ca-nhan__the-phu">Tích lũy từ reader metrics</span>
        </div>

        <div className="kpi-ca-nhan__the-thong-ke">
          <span className="kpi-ca-nhan__the-nhan">
            {laChinhThuc ? 'Kết quả chính thức' : 'Dự kiến hiện tại'}
          </span>
          <div>
            <span
              className={`kpi-ca-nhan__badge-status ${
                duLieu?.projectedStatus === 'PASS'
                  ? 'kpi-ca-nhan__badge-status--pass'
                  : 'kpi-ca-nhan__badge-status--not-pass'
              }`}
            >
              {duLieu?.projectedStatus || 'NOT_PASS'}
            </span>
          </div>
          <span className="kpi-ca-nhan__the-phu">
            {laChinhThuc ? 'Đã được Trưởng ban chốt kỳ' : 'Đang trong chu kỳ tạm tính'}
          </span>
        </div>
      </div>

      {/* Nhan xet danh gia cua Truong ban (neu co, UC-67) */}
      {duLieu?.notes && (
        <div className="kpi-ca-nhan__ghi-chu-box">
          <div className="kpi-ca-nhan__ghi-chu-tieu-de">Nhận xét của Trưởng ban biên tập:</div>
          <div className="kpi-ca-nhan__ghi-chu-noi-dung">{duLieu.notes}</div>
        </div>
      )}

      {/* Bang danh sach bai viet da xuat ban */}
      <div className="kpi-ca-nhan__khung-bang">
        <div className="kpi-ca-nhan__bang-tieu-de-khung">
          <span className="kpi-ca-nhan__bang-tieu-de">
            Danh sách bài viết đã xuất bản trong kỳ ({articles.length} bài)
          </span>
        </div>

        {dangTai ? (
          <div className="kpi-ca-nhan__trang-thai-rong">Đang tải dữ liệu KPI cá nhân...</div>
        ) : articles.length === 0 ? (
          <div className="kpi-ca-nhan__trang-thai-rong">
            Bạn chưa có tác phẩm nào xuất bản trong Tháng {thang}/{nam}.
          </div>
        ) : (
          <table className="kpi-ca-nhan__table">
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
                    <div className="kpi-ca-nhan__bai-tieu-de">{art.title}</div>
                    <div className="kpi-ca-nhan__bai-sub">Mã bài: #{art.articleId}</div>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    {formatDate(art.publishedAt)}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span className="kpi-ca-nhan__badge-nature">{art.nature}</span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <div className="kpi-ca-nhan__ty-le-dong-tac-gia">
                      <span className="kpi-ca-nhan__ty-le-chinh">
                        {formatNumber(art.sharePercent)}%
                      </span>
                      <span className="kpi-ca-nhan__ty-le-quy-doi">
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
