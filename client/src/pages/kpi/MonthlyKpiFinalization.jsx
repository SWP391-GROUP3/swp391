import { useState, useEffect } from 'react'
import { layBangKpiBan, tongHopKpiThang, chotKpiThang } from '../../api/kpiApi.js'
import { formatNumber } from '../../utils/formatNumber.js'
import './MonthlyKpiFinalization.css'

/**
 * Man hinh Chot ky KPI thang (UC-66: Finalize Monthly KPI).
 * Danh cho Truong ban bien tap (Head of Editorial Department) khoa ky danh gia,
 * xet tu dong PASS / NOT_PASS theo BR-KPI-05, nhap nhan xet dinh tinh va luu
 * ban ghi snapshot bat bien vao kpi_snapshots (BR-KPI-06).
 *
 * MO TRANG NAY TAI: /kpi-finalization
 *
 * MUC LUC (so dong - ten - tac dung)
 *   25  MonthlyKpiFinalization       - Component chinh man hinh chot KPI thang
 *   35    useEffect                  - Tai du lieu khi doi nam/thang
 *   39    taiDuLieuKy()              - Tai du lieu KPI thang va trang thai chot
 *   54    xuLyChayTongHop()          - Kich hoat job tong hop tu dong truoc khi chot
 *   70    xuLyChotKyKpi()            - Khoa ky, chot snapshot bat bien va gui thong bao
 */
export default function MonthlyKpiFinalization({ nguoiDung }) {
  const [nam, setNam] = useState(2026)
  const [thang, setThang] = useState(9)
  const [duLieu, setDuLieu] = useState(null)
  const [ghiChuMap, setGhiChuMap] = useState({}) // reporterId -> notes
  const [dangTai, setDangTai] = useState(false)
  const [dangXuLy, setDangXuLy] = useState(false)
  const [thongBao, setThongBao] = useState('')
  const [loi, setLoi] = useState('')

  useEffect(() => {
    taiDuLieuKy(nam, thang)
  }, [nam, thang])

  async function taiDuLieuKy(y, m) {
    setDangTai(true)
    setLoi('')
    try {
      const data = await layBangKpiBan(y, m)
      setDuLieu(data)

      // Khoi tao ghi chu tu du lieu hien co
      const map = {}
      if (data?.items) {
        data.items.forEach((item) => {
          map[item.reporterId] = item.notes || ''
        })
      }
      setGhiChuMap(map)
    } catch (err) {
      setLoi(err.message || 'Không thể tải dữ liệu KPI tháng')
      setDuLieu(null)
    } finally {
      setDangTai(false)
    }
  }

  async function xuLyChayTongHop() {
    if (!window.confirm(`Bạn có muốn chạy tổng hợp lại số liệu KPI mới nhất cho Tháng ${thang}/${nam}?`)) {
      return
    }

    setDangXuLy(true)
    setLoi('')
    setThongBao('')
    try {
      await tongHopKpiThang(nam, thang)
      setThongBao(`Đã cập nhật tổng hợp số liệu mới nhất cho Tháng ${thang}/${nam}!`)
      await taiDuLieuKy(nam, thang)
    } catch (err) {
      setLoi(err.message || 'Lỗi khi tổng hợp số liệu')
    } finally {
      setDangXuLy(false)
    }
  }

  async function xuLyChotKyKpi() {
    const canhBao =
      `XÁC NHẬN CHỐT KỲ KPI THÁNG ${thang}/${nam}?\n\n` +
      `- Hệ thống sẽ khóa vĩnh viễn và lưu bản ghi bất biến (FINALIZED) theo luật BR-KPI-06.\n` +
      `- Sau khi chốt, kết quả PASS / NOT_PASS không thể thay đổi hoặc tính lại.\n` +
      `- Thông báo chính thức sẽ được gửi đến tất cả phóng viên trong ban.\n\n` +
      `Bạn có chắc chắn muốn tiếp tục?`

    if (!window.confirm(canhBao)) {
      return
    }

    setDangXuLy(true)
    setLoi('')
    setThongBao('')
    try {
      const res = await chotKpiThang({
        year: nam,
        month: thang,
        reporterNotes: ghiChuMap,
      })
      setThongBao(
        `Chốt thành công kỳ KPI Tháng ${thang}/${nam}! (${res.passCount} phóng viên Đạt, ${res.notPassCount} phóng viên Chưa đạt).`
      )
      await taiDuLieuKy(nam, thang)
    } catch (err) {
      setLoi(err.message || 'Không thể chốt kỳ KPI')
    } finally {
      setDangXuLy(false)
    }
  }

  function capNhatGhiChu(reporterId, value) {
    setGhiChuMap((prev) => ({
      ...prev,
      [reporterId]: value,
    }))
  }

  const items = duLieu?.items || []
  const daKhoa = duLieu?.isFinalized === true || items.some((i) => i.snapshotStatus === 'FINALIZED')
  const tyLeHoanThanh =
    duLieu?.totalReporters > 0
      ? Math.round((duLieu.passCount / duLieu.totalReporters) * 100)
      : 0

  return (
    <div className="chot-kpi">
      {/* Header & Controls */}
      <div className="chot-kpi__header">
        <div>
          <h1 className="chot-kpi__tieu-de">Chốt & Khóa KPI Tháng (Finalize Monthly KPI)</h1>
          <p className="chot-kpi__mo-ta">
            Khóa chu kỳ đánh giá, tự động xếp loại PASS / NOT_PASS và lưu bản ghi snapshot bất biến theo BR-KPI-05 &amp; BR-KPI-06 (UC-66).
          </p>
        </div>

        <div className="chot-kpi__bo-loc">
          <select
            className="chot-kpi__select"
            value={nam}
            onChange={(e) => setNam(Number(e.target.value))}
            disabled={dangXuLy}
          >
            <option value={2025}>Năm 2025</option>
            <option value={2026}>Năm 2026</option>
            <option value={2027}>Năm 2027</option>
          </select>

          <select
            className="chot-kpi__select"
            value={thang}
            onChange={(e) => setThang(Number(e.target.value))}
            disabled={dangXuLy}
          >
            {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
              <option key={m} value={m}>
                Tháng {m}
              </option>
            ))}
          </select>

          {!daKhoa && (
            <button
              className="chot-kpi__btn-tong-hop"
              onClick={xuLyChayTongHop}
              disabled={dangXuLy || dangTai}
            >
              {dangXuLy ? 'Đang xử lý...' : '⚡ Cập nhật số liệu'}
            </button>
          )}

          <button
            className="chot-kpi__btn-chot"
            onClick={xuLyChotKyKpi}
            disabled={daKhoa || dangXuLy || dangTai || items.length === 0}
            title={daKhoa ? 'Kỳ KPI này đã được chốt và khóa vĩnh viễn' : 'Khóa kỳ và chốt kết quả'}
          >
            {daKhoa ? '🔒 Đã chốt kỳ (FINALIZED)' : '🔒 Chốt kỳ KPI tháng'}
          </button>
        </div>
      </div>

      {/* Thông báo kết quả */}
      {thongBao && <div className="chot-kpi__thong-bao-thanh-cong">{thongBao}</div>}
      {loi && <div className="chot-kpi__thong-bao-loi">{loi}</div>}

      {/* Banner trạng thái kỳ */}
      {daKhoa ? (
        <div className="chot-kpi__banner-khoa chot-kpi__banner-khoa--finalized">
          <span style={{ fontSize: '20px' }}>✓</span>
          <div>
            <strong>Kỳ KPI Tháng {thang}/{nam} đã được khóa chính thức (FINALIZED).</strong>
            <div style={{ fontSize: '13px', marginTop: '2px' }}>
              Toàn bộ số liệu bài viết, điểm số và kết quả Đạt / Chưa đạt đã được lưu trữ vĩnh viễn và bất biến theo luật BR-KPI-06.
            </div>
          </div>
        </div>
      ) : (
        <div className="chot-kpi__banner-khoa">
          <span style={{ fontSize: '20px' }}>ℹ</span>
          <div>
            <strong>Kỳ KPI Tháng {thang}/{nam} đang ở trạng thái TẠM (DRAFT).</strong>
            <div style={{ fontSize: '13px', marginTop: '2px' }}>
              Trưởng ban có thể nhập nhận xét định tính cho từng phóng viên bên dưới trước khi bấm nút <strong>Chốt kỳ KPI tháng</strong>.
            </div>
          </div>
        </div>
      )}

      {/* Thẻ thống kê */}
      <div className="chot-kpi__thong-ke">
        <div className="chot-kpi__the">
          <span className="chot-kpi__the-nhan">Tổng số phóng viên</span>
          <span className="chot-kpi__the-so">{duLieu?.totalReporters ?? 0}</span>
        </div>
        <div className="chot-kpi__the">
          <span className="chot-kpi__the-nhan">Số người ĐẠT (PASS)</span>
          <span className="chot-kpi__the-so chot-kpi__the-so--dat">{duLieu?.passCount ?? 0}</span>
        </div>
        <div className="chot-kpi__the">
          <span className="chot-kpi__the-nhan">Số người CHƯA ĐẠT</span>
          <span className="chot-kpi__the-so chot-kpi__the-so--chua-dat">
            {duLieu?.notPassCount ?? 0}
          </span>
        </div>
        <div className="chot-kpi__the">
          <span className="chot-kpi__the-nhan">Tỷ lệ hoàn thành ban</span>
          <span className="chot-kpi__the-so">{tyLeHoanThanh}%</span>
        </div>
        <div className="chot-kpi__the">
          <span className="chot-kpi__the-nhan">Trạng thái chốt kỳ</span>
          <span className="chot-kpi__the-so" style={{ fontSize: '18px' }}>
            {daKhoa ? (
              <span className="chot-kpi__badge-snapshot chot-kpi__badge-snapshot--finalized">
                ĐÃ CHỐT (FINALIZED)
              </span>
            ) : (
              <span className="chot-kpi__badge-snapshot chot-kpi__badge-snapshot--draft">
                TẠM (DRAFT)
              </span>
            )}
          </span>
        </div>
      </div>

      {/* Bảng danh sách phóng viên và đánh giá */}
      <div className="chot-kpi__bang-khung">
        {dangTai ? (
          <div className="chot-kpi__trang-thai-rong">Đang tải dữ liệu kỳ KPI...</div>
        ) : items.length === 0 ? (
          <div className="chot-kpi__trang-thai-rong">Chưa có phóng viên nào trong kỳ này.</div>
        ) : (
          <table className="chot-kpi__table">
            <thead>
              <tr>
                <th>Phóng viên</th>
                <th style={{ textAlign: 'center' }}>Chỉ tiêu (Bài / Điểm)</th>
                <th style={{ textAlign: 'center' }}>Thực tế (Bài / Điểm TB)</th>
                <th style={{ textAlign: 'center' }}>Tiến độ bài</th>
                <th style={{ textAlign: 'center' }}>Đánh giá tự động</th>
                <th style={{ textAlign: 'center' }}>Bản ghi</th>
                <th>Nhận xét của Trưởng ban (Appraisal Notes)</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.reporterId}>
                  <td>
                    <div className="chot-kpi__phong-vien-ten">{item.reporterName}</div>
                    <div className="chot-kpi__phong-vien-email">{item.reporterEmail}</div>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <div>{item.minPublished} bài</div>
                    <div style={{ fontSize: '12px', color: 'var(--fm-chu-nhat, #707070)' }}>
                      Sàn: {item.minAvgScore} đ
                    </div>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <div style={{ fontWeight: 'bold' }}>{item.actualArticles} bài</div>
                    <div style={{ fontSize: '12px', color: 'var(--fm-chu-nhat, #707070)' }}>
                      TB: {item.actualAvgScore !== null ? `${item.actualAvgScore} đ` : '—'}
                    </div>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span style={{ fontWeight: '600' }}>{item.articleProgressPercent}%</span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span
                      className={`chot-kpi__badge-status ${
                        item.projectedStatus === 'PASS'
                          ? 'chot-kpi__badge-status--pass'
                          : 'chot-kpi__badge-status--not-pass'
                      }`}
                    >
                      {item.projectedStatus === 'PASS' ? 'ĐẠT (PASS)' : 'CHƯA ĐẠT (NOT_PASS)'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <span
                      className={`chot-kpi__badge-snapshot ${
                        item.snapshotStatus === 'FINALIZED'
                          ? 'chot-kpi__badge-snapshot--finalized'
                          : 'chot-kpi__badge-snapshot--draft'
                      }`}
                    >
                      {item.snapshotStatus || 'DRAFT'}
                    </span>
                  </td>
                  <td>
                    {daKhoa ? (
                      <span className="chot-kpi__ghi-chu-da-khoa">
                        {item.notes || 'Không có ghi chú.'}
                      </span>
                    ) : (
                      <input
                        type="text"
                        className="chot-kpi__input-ghi-chu"
                        placeholder="Nhập nhận xét đánh giá..."
                        value={ghiChuMap[item.reporterId] || ''}
                        onChange={(e) => capNhatGhiChu(item.reporterId, e.target.value)}
                        disabled={dangXuLy}
                      />
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
