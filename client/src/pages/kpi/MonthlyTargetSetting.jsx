import { useState, useEffect } from 'react'
import { namHienTai, thangHienTai } from './kyKpiMacDinh.js'
import {
  layDinhMucChuan,
  layChiTieuTheoThang,
  taoChiTieu,
  capNhatChiTieu,
  xoaChiTieu,
} from '../../api/kpiApi.js'

/**
 * Màn hình Cấu hình Định mức Chuẩn (UC-58) & Giao Chỉ tiêu KPI Tháng (UC-59).
 *
 * MO TRANG NAY TAI: /kpi-targets
 *
 * MUC LUC (so dong - ten - tac dung)
 *   36  MonthlyTargetSetting()          xem tom tat dau file
 *   54    useEffect                     Tai dinh muc chuan khi mo trang
 *   61    useEffect                     Tai danh sach chi tieu theo thang
 *   65    taiDanhSachChiTieu()          tai danh sach chi tieu
 *   79    moModalThemMoi()              mo modal them moi
 *   89    moModalSua()                  mo modal sua
 *   99    apDungDinhMuc()               ap dung dinh muc
 *  104    xuLyLuuForm()                 xu ly luu form
 *  141    xuLyXoa()                     xu ly xoa
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  163  [+ Giao chi tieu moi]           moModalThemMoi() -> tren trang: dong/xoa dang sua id, dong/xoa form reporter id, form min published
 *  201  [Tai lai]                       taiDanhSachChiTieu() -> goi GET /api/kpi-targets?year={nam}&month={thang}
 *  247  [Sua]                           moModalSua() -> tren trang: dang sua id, form reporter id, form min published
 *  254  [Xoa]                           xuLyXoa() -> hoi xac nhan; goi DELETE /api/kpi-targets/{id}, GET /api/kpi-targets?year={nam}&month={thang}
 *  276  [✕]                             tren trang: dong/xoa hien modal
 *  282  form, nut [Dang luu... / Cap nhat / Giao chi t…]  xuLyLuuForm() -> goi PUT /api/kpi-targets/{id}, POST /api/kpi-targets, GET /api/kpi-targets?year={nam}&month={thang}
 *  292  [{dm.levelName} ( {dm.defaultMinPublishe…]  apDungDinhMuc() -> tren trang: form min published, form min avg score
 *  349  [Huy]                           tren trang: dong/xoa hien modal
 */
export default function MonthlyTargetSetting({ nguoiDung }) {
  const [nam, setNam] = useState(namHienTai())
  const [thang, setThang] = useState(thangHienTai())
  const [danhSach, setDanhSach] = useState([])
  const [dinhMucChuan, setDinhMucChuan] = useState([])
  const [dangTai, setDangTai] = useState(false)
  const [thongBao, setThongBao] = useState('')
  const [loi, setLoi] = useState('')

  // State cho Modal Form
  const [hienModal, setHienModal] = useState(false)
  const [dangSuaId, setDangSuaId] = useState(null)
  const [formReporterId, setFormReporterId] = useState('')
  const [formMinPublished, setFormMinPublished] = useState(6)
  const [formMinAvgScore, setFormMinAvgScore] = useState(65.0)
  const [dangLuu, setDangLuu] = useState(false)

  // Tải định mức chuẩn khi mở trang
  useEffect(() => {
    layDinhMucChuan()
      .then((data) => setDinhMucChuan(data || []))
      .catch((err) => console.error('Lỗi lấy định mức chuẩn:', err))
  }, [])

  // Tải danh sách chỉ tiêu theo tháng
  useEffect(() => {
    taiDanhSachChiTieu(nam, thang)
  }, [nam, thang])

  async function taiDanhSachChiTieu(y, m) {
    setDangTai(true)
    setLoi('')
    try {
      const data = await layChiTieuTheoThang(y, m)
      setDanhSach(data || [])
    } catch (err) {
      setLoi(err.message)
      setDanhSach([])
    } finally {
      setDangTai(false)
    }
  }

  function moModalThemMoi() {
    setDangSuaId(null)
    setFormReporterId('')
    setFormMinPublished(6)
    setFormMinAvgScore(65.0)
    setLoi('')
    setThongBao('')
    setHienModal(true)
  }

  function moModalSua(item) {
    setDangSuaId(item.id)
    setFormReporterId(item.reporterId)
    setFormMinPublished(item.minPublished)
    setFormMinAvgScore(item.minAvgScore)
    setLoi('')
    setThongBao('')
    setHienModal(true)
  }

  function apDungDinhMuc(dm) {
    setFormMinPublished(dm.defaultMinPublished)
    setFormMinAvgScore(dm.defaultMinAvgScore)
  }

  async function xuLyLuuForm(e) {
    e.preventDefault()
    setDangLuu(true)
    setLoi('')
    setThongBao('')

    try {
      if (dangSuaId) {
        // Cập nhật chỉ tiêu (A2)
        await capNhatChiTieu(dangSuaId, {
          minPublished: Number(formMinPublished),
          minAvgScore: Number(formMinAvgScore),
        })
        setThongBao('Cập nhật chỉ tiêu thành công!')
      } else {
        // Tạo chỉ tiêu mới
        await taoChiTieu(
          {
            reporterId: Number(formReporterId),
            targetYear: Number(nam),
            targetMonth: Number(thang),
            minPublished: Number(formMinPublished),
            minAvgScore: Number(formMinAvgScore),
          }
          // nguoi giao (set_by): server tu lay tu token dang nhap
        )
        setThongBao('Giao chỉ tiêu mới thành công!')
      }
      setHienModal(false)
      taiDanhSachChiTieu(nam, thang)
    } catch (err) {
      setLoi(err.message)
    } finally {
      setDangLuu(false)
    }
  }

  async function xuLyXoa(id) {
    if (!window.confirm('Bạn có chắc chắn muốn xoá chỉ tiêu này?')) return
    try {
      await xoaChiTieu(id)
      setThongBao('Xoá chỉ tiêu thành công!')
      taiDanhSachChiTieu(nam, thang)
    } catch (err) {
      setLoi(err.message)
    }
  }

  return (
    <div className="trang-kpi">
      {/* Tiêu đề & thanh công cụ */}
      <div className="kpi-header">
        <div>
          <h2 className="kpi-tieu-de">Giao Chỉ tiêu KPI Tháng</h2>
          <p className="kpi-phu-de">
            Thiết lập và quản lý hạn ngạch chỉ tiêu hàng tháng cho từng phóng viên trong ban.
          </p>
        </div>

        <button className="nut-chinh" style={{ width: 'auto' }} type="button" onClick={moModalThemMoi}>
          + Giao chỉ tiêu mới
        </button>
      </div>

      {/* Thông báo kết quả */}
      {thongBao && <div className="thong-bao-thanh-cong">{thongBao}</div>}
      {loi && <div className="thong-bao-loi">{loi}</div>}

      {/* Danh sách chỉ tiêu tháng (UC-59) */}
      <div className="the-khung">
        <div className="thanh-loc">
          <div className="nhom-loc">
            <label className="nhan">Năm:</label>
            <input
              type="number"
              className="o-nhap-ngan"
              value={nam}
              onChange={(e) => setNam(Number(e.target.value))}
            />
          </div>

          <div className="nhom-loc">
            <label className="nhan">Tháng:</label>
            <select
              className="o-nhap-ngan"
              value={thang}
              onChange={(e) => setThang(Number(e.target.value))}
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                <option key={m} value={m}>Tháng {m}</option>
              ))}
            </select>
          </div>

          <button
            className="nut-phu"
            type="button"
            onClick={() => taiDanhSachChiTieu(nam, thang)}
          >
            Tải lại
          </button>
        </div>

        {/* Bảng dữ liệu */}
        {dangTai ? (
          <p className="dang-tai">Đang tải danh sách chỉ tiêu...</p>
        ) : danhSach.length === 0 ? (
          <div className="trong-rong">
            Chưa có chỉ tiêu nào được thiết lập cho tháng {thang}/{nam}.
          </div>
        ) : (
          <div className="vung-bang">
            <table className="bang-kpi">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>ID Phóng viên</th>
                  <th>Kỳ đánh giá</th>
                  <th>Số bài tối thiểu</th>
                  <th>Điểm TB tối thiểu</th>
                  <th>Trưởng ban giao</th>
                  <th style={{ textAlign: 'center' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {danhSach.map((item) => (
                  <tr key={item.id}>
                    <td>#{item.id}</td>
                    <td>
                      <strong>PV #{item.reporterId}</strong>
                    </td>
                    <td>Tháng {item.targetMonth}/{item.targetYear}</td>
                    <td>
                      <span className="the-so">{item.minPublished} bài</span>
                    </td>
                    <td>
                      <span className="the-diem">{item.minAvgScore} đ</span>
                    </td>
                    <td>ID #{item.setBy}</td>
                    <td style={{ textAlign: 'center' }}>
                      <button
                        className="nut-thao-tac nut-sua"
                        type="button"
                        onClick={() => moModalSua(item)}
                      >
                        Sửa
                      </button>
                      <button
                        className="nut-thao-tac nut-xoa"
                        type="button"
                        onClick={() => xuLyXoa(item.id)}
                      >
                        Xoá
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* MODAL: Form Giao / Cập nhật chỉ tiêu */}
      {hienModal && (
        <div className="lop-phu-modal">
          <div className="hop-modal">
            <div className="modal-header">
              <h3>{dangSuaId ? 'Điều chỉnh chỉ tiêu KPI' : 'Giao chỉ tiêu KPI mới'}</h3>
              <button
                className="nut-dong"
                type="button"
                onClick={() => setHienModal(false)}
              >
                ✕
              </button>
            </div>

            <form onSubmit={xuLyLuuForm}>
              {/* Nút điền nhanh theo định mức chuẩn */}
              <div className="vung-ap-dung-nhanh">
                <span className="nhan-nho">Áp dụng nhanh định mức chuẩn:</span>
                <div className="nhom-nut-nhanh">
                  {dinhMucChuan.map((dm) => (
                    <button
                      key={dm.seniorityLevel}
                      type="button"
                      className="nut-nhanh"
                      onClick={() => apDungDinhMuc(dm)}
                    >
                      {dm.levelName} ({dm.defaultMinPublished} bài - {dm.defaultMinAvgScore}đ)
                    </button>
                  ))}
                </div>
              </div>

              {!dangSuaId && (
                <div>
                  <label className="nhan" htmlFor="reporterId">ID Phóng viên</label>
                  <input
                    id="reporterId"
                    className="o-nhap"
                    type="number"
                    min="1"
                    required
                    value={formReporterId}
                    onChange={(e) => setFormReporterId(e.target.value)}
                    placeholder="Nhập ID phóng viên (vd: 1, 2, 7...)"
                  />
                </div>
              )}

              <div>
                <label className="nhan" htmlFor="minPublished">Số bài xuất bản tối thiểu</label>
                <input
                  id="minPublished"
                  className="o-nhap"
                  type="number"
                  min="0"
                  required
                  value={formMinPublished}
                  onChange={(e) => setFormMinPublished(e.target.value)}
                />
              </div>

              <div>
                <label className="nhan" htmlFor="minAvgScore">Điểm chất lượng trung bình sàn (0 - 100)</label>
                <input
                  id="minAvgScore"
                  className="o-nhap"
                  type="number"
                  step="0.01"
                  min="0"
                  max="100"
                  required
                  value={formMinAvgScore}
                  onChange={(e) => setFormMinAvgScore(e.target.value)}
                />
              </div>

              <div className="modal-footer">
                <button
                  className="nut-phu"
                  type="button"
                  disabled={dangLuu}
                  onClick={() => setHienModal(false)}
                >
                  Huỷ
                </button>
                <button
                  className="nut-chinh"
                  style={{ width: 'auto' }}
                  type="submit"
                  disabled={dangLuu}
                >
                  {dangLuu ? 'Đang lưu...' : dangSuaId ? 'Cập nhật' : 'Giao chỉ tiêu'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
