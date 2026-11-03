import { useState, useEffect } from 'react'
import { danhSachNam, namHienTai, thangHienTai } from './kyKpiMacDinh.js'
import { Link } from 'react-router-dom'
import { layBangKpiBan, tongHopKpiThang } from '../../api/kpiApi.js'
import '../../styles/DepartmentKpiDashboard.css'

/**
 * Man hinh Bang theo doi KPI toan ban bien tap (UC-61: View Department KPI Board).
 * Hien thi tien do thuc te, chi tieu, xet PASS/NOT_PASS theo BR-KPI-05 va cho phep chay job tong hop.
 *
 * MO TRANG NAY TAI: /kpi-board
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  DepartmentKpiDashboard()        xem tom tat dau file
 *   33    useEffect                     tai du lieu khi mo trang
 *   37    taiDuLieuDashboard()          tai du lieu dashboard
 *   51    xuLyChayTongHop()             xu ly chay tong hop
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  108  [Dang tong hop... / ⚡ Chay tong hop tu d…]  xuLyChayTongHop() -> hoi xac nhan; goi POST /api/kpi-snapshots/aggregate?year={nam}&month={thang}, GET /api/kpi-snapshots/dashboard?year={nam}&month={thang}
 *  115  link [🔒 Chot KPI thang →]      mo trang /kpi-finalization (MonthlyKpiFinalization)
 *  227  link [Chi tiet →]               mo trang /kpi-board/{item.reporterId}?year={nam}&month={thang} (ReporterKpiDetails)
 */
export default function DepartmentKpiDashboard({ nguoiDung }) {
  const [nam, setNam] = useState(namHienTai())
  const [thang, setThang] = useState(thangHienTai())
  const [duLieu, setDuLieu] = useState(null)
  const [dangTai, setDangTai] = useState(false)
  const [dangTongHop, setDangTongHop] = useState(false)
  const [thongBao, setThongBao] = useState('')
  const [loi, setLoi] = useState('')

  useEffect(() => {
    taiDuLieuDashboard(nam, thang)
  }, [nam, thang])

  async function taiDuLieuDashboard(y, m) {
    setDangTai(true)
    setLoi('')
    try {
      const data = await layBangKpiBan(y, m)
      setDuLieu(data)
    } catch (err) {
      setLoi(err.message)
      setDuLieu(null)
    } finally {
      setDangTai(false)
    }
  }

  async function xuLyChayTongHop() {
    if (!window.confirm(`Bạn có chắc chắn muốn chạy tổng hợp tự động số liệu KPI cho Tháng ${thang}/${nam}?`)) {
      return
    }

    setDangTongHop(true)
    setLoi('')
    setThongBao('')
    try {
      await tongHopKpiThang(nam, thang)
      setThongBao(`Đã chạy tổng hợp tự động thành công cho Tháng ${thang}/${nam}!`)
      await taiDuLieuDashboard(nam, thang)
    } catch (err) {
      setLoi(err.message)
    } finally {
      setDangTongHop(false)
    }
  }

  const items = duLieu?.items || []
  const tyLeHoanThanhBan = duLieu?.totalReporters > 0
    ? Math.round((duLieu.passCount / duLieu.totalReporters) * 100)
    : 0

  return (
    <div className="bang-kpi-ban">
      {/* Header & Controls */}
      <div className="bang-kpi-ban__header">
        <div>
          <h1 className="bang-kpi-ban__tieu-de">Bảng theo dõi KPI Ban Biên Tập</h1>
          <p className="bang-kpi-ban__mo-ta">
            Theo dõi tiến độ thực hiện bài viết, điểm chất lượng và kết quả đánh giá KPI của phóng viên (UC-61).
          </p>
        </div>

        <div className="bang-kpi-ban__bo-loc">
          <select
            className="bang-kpi-ban__select"
            value={nam}
            onChange={(e) => setNam(Number(e.target.value))}
          >
            {danhSachNam().map((n) => <option key={n} value={n}>Năm {n}</option>)}
            <option value={2027}>Năm 2027</option>
          </select>

          <select
            className="bang-kpi-ban__select"
            value={thang}
            onChange={(e) => setThang(Number(e.target.value))}
          >
            {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
              <option key={m} value={m}>Tháng {m}</option>
            ))}
          </select>

          <button
            className="bang-kpi-ban__btn-tong-hop"
            onClick={xuLyChayTongHop}
            disabled={dangTongHop || dangTai}
          >
            {dangTongHop ? 'Đang tổng hợp...' : '⚡ Chạy tổng hợp tự động'}
          </button>

          <Link
            to="/kpi-finalization"
            className="bang-kpi-ban__btn-tong-hop"
            style={{ textDecoration: 'none', backgroundColor: '#e2f0d9', borderColor: '#b9e2b4', color: '#1f6f1a' }}
          >
            🔒 Chốt KPI tháng →
          </Link>
        </div>
      </div>

      {/* Thông báo */}
      {thongBao && <div className="bang-kpi-ban__thong-bao-thanh-cong">{thongBao}</div>}
      {loi && <div className="bang-kpi-ban__thong-bao-loi">{loi}</div>}

      {/* Thẻ thống kê */}
      <div className="bang-kpi-ban__thong-ke">
        <div className="bang-kpi-ban__the-thong-ke">
          <span className="bang-kpi-ban__the-nhan">Tổng số phóng viên</span>
          <span className="bang-kpi-ban__the-so">{duLieu?.totalReporters ?? 0}</span>
        </div>
        <div className="bang-kpi-ban__the-thong-ke">
          <span className="bang-kpi-ban__the-nhan">Dự kiến Đạt (PASS)</span>
          <span className="bang-kpi-ban__the-so bang-kpi-ban__the-so--dat">{duLieu?.passCount ?? 0}</span>
        </div>
        <div className="bang-kpi-ban__the-thong-ke">
          <span className="bang-kpi-ban__the-nhan">Chưa đạt (NOT_PASS)</span>
          <span className="bang-kpi-ban__the-so bang-kpi-ban__the-so--chua-dat">{duLieu?.notPassCount ?? 0}</span>
        </div>
        <div className="bang-kpi-ban__the-thong-ke">
          <span className="bang-kpi-ban__the-nhan">Tỷ lệ hoàn thành ban</span>
          <span className="bang-kpi-ban__the-so bang-kpi-ban__the-so--ty-le">{tyLeHoanThanhBan}%</span>
        </div>
      </div>

      {/* Bảng danh sách phóng viên */}
      <div className="bang-kpi-ban__bang-khung">
        {dangTai ? (
          <div className="bang-kpi-ban__trang-thai-rong">Đang tải dữ liệu KPI...</div>
        ) : items.length === 0 ? (
          <div className="bang-kpi-ban__trang-thai-rong">Chưa có phóng viên nào trong kỳ này.</div>
        ) : (
          <table className="bang-kpi-ban__table">
            <thead>
              <tr>
                <th>Phóng viên</th>
                <th style={{ textAlign: 'center' }}>Chỉ tiêu bài</th>
                <th style={{ textAlign: 'center' }}>Điểm sàn</th>
                <th style={{ textAlign: 'center' }}>Bài thực tế</th>
                <th style={{ textAlign: 'center' }}>Điểm TB</th>
                <th>Tiến độ bài</th>
                <th style={{ textAlign: 'center' }}>Trạng thái dự kiến</th>
                <th style={{ textAlign: 'center' }}>Bản ghi</th>
                <th style={{ textAlign: 'center' }}>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.reporterId}>
                  <td>
                    <div className="bang-kpi-ban__phong-vien-ten">{item.reporterName}</div>
                    <div className="bang-kpi-ban__phong-vien-email">{item.reporterEmail}</div>
                  </td>
                  <td style={{ textAlign: 'center' }}>{item.minPublished}</td>
                  <td style={{ textAlign: 'center' }}>{item.minAvgScore}</td>
                  <td style={{ textAlign: 'center', fontWeight: 'bold' }}>
                    {item.actualArticles}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    {item.actualAvgScore !== null ? item.actualAvgScore : '—'}
                  </td>
                  <td>
                    <div className="bang-kpi-ban__tien-do-khung">
                      <div className="bang-kpi-ban__thanh-tien-do">
                        <div
                          className={`bang-kpi-ban__thanh-tien-do-ruot ${
                            Number(item.articleProgressPercent) >= 100
                              ? 'bang-kpi-ban__thanh-tien-do-ruot--day'
                              : ''
                          }`}
                          style={{
                            width: `${Math.min(Number(item.articleProgressPercent) || 0, 100)}%`,
                          }}
                        />
                      </div>
                      <span className="bang-kpi-ban__tien-do-so">
                        {item.articleProgressPercent}%
                      </span>
                    </div>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span
                      className={`bang-kpi-ban__badge-status ${
                        item.projectedStatus === 'PASS'
                          ? 'bang-kpi-ban__badge-status--pass'
                          : 'bang-kpi-ban__badge-status--not-pass'
                      }`}
                    >
                      {item.projectedStatus}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span
                      className={`bang-kpi-ban__badge-snapshot ${
                        item.snapshotStatus === 'FINALIZED'
                          ? 'bang-kpi-ban__badge-snapshot--finalized'
                          : 'bang-kpi-ban__badge-snapshot--draft'
                      }`}
                    >
                      {item.snapshotStatus || 'DRAFT'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <Link
                      to={`/kpi-board/${item.reporterId}?year=${nam}&month=${thang}`}
                      className="bang-kpi-ban__link-chi-tiet"
                    >
                      Chi tiết →
                    </Link>
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
