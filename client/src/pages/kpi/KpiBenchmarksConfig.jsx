import { useState, useEffect } from 'react'
import { layDinhMucChuan, capNhatDinhMucChuan } from '../../api/kpiApi.js'

/**
 * Màn hình 1: KPI Benchmarks Config Screen (UC-58).
 * Cho phép Trưởng ban / Quản trị viên cấu hình các mức chỉ tiêu chuẩn
 * theo từng ngạch phóng viên (Tập sự, Sơ cấp, Kỳ cựu).
 *
 * MO TRANG NAY TAI: /kpi-benchmarks
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  KpiBenchmarksConfig()           xem tom tat dau file
 *   29    useEffect                     tai du lieu khi mo trang
 *   33    taiDuLieu()                   tai du lieu
 *   46    thayDoiGiaTri()               thay doi gia tri
 *   55    xuLyLuu()                     xu ly luu
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  101  form, nut [Dang luu... / Luu cau hinh dinh muc]  xuLyLuu() -> goi PUT /api/kpi-targets/benchmarks
 *  167  [Tai lai mac dinh]              taiDuLieu() -> goi GET /api/kpi-targets/benchmarks
 */
export default function KpiBenchmarksConfig() {
  const [dinhMuc, setDinhMuc] = useState([])
  const [dangTai, setDangTai] = useState(false)
  const [dangLuu, setDangLuu] = useState(false)
  const [thongBao, setThongBao] = useState('')
  const [loi, setLoi] = useState('')

  useEffect(() => {
    taiDuLieu()
  }, [])

  async function taiDuLieu() {
    setDangTai(true)
    setLoi('')
    try {
      const data = await layDinhMucChuan()
      setDinhMuc(data || [])
    } catch (err) {
      setLoi(err.message)
    } finally {
      setDangTai(false)
    }
  }

  function thayDoiGiaTri(index, truong, giaTri) {
    const banSao = [...dinhMuc]
    banSao[index] = {
      ...banSao[index],
      [truong]: giaTri,
    }
    setDinhMuc(banSao)
  }

  async function xuLyLuu(e) {
    e.preventDefault()
    setDangLuu(true)
    setLoi('')
    setThongBao('')

    try {
      const formatted = dinhMuc.map((item) => ({
        seniorityLevel: item.seniorityLevel,
        levelName: item.levelName,
        defaultMinPublished: Number(item.defaultMinPublished),
        defaultMinAvgScore: Number(item.defaultMinAvgScore),
      }))

      await capNhatDinhMucChuan(formatted)
      setThongBao('Lưu cấu hình định mức chuẩn thành công!')
    } catch (err) {
      setLoi(err.message)
    } finally {
      setDangLuu(false)
    }
  }

  return (
    <div className="trang-kpi">
      <div className="kpi-header">
        <div>
          <h2 className="kpi-tieu-de">Cấu hình Định mức Chuẩn KPI</h2>
          <p className="kpi-phu-de">
            Thiết lập chỉ tiêu số bài tối thiểu và điểm sàn mặc định theo ngạch phóng viên.
          </p>
        </div>
      </div>

      {thongBao && <div className="thong-bao-thanh-cong">{thongBao}</div>}
      {loi && <div className="thong-bao-loi">{loi}</div>}

      <div className="the-khung">
        <h3 className="the-khung__tieu-de">Bảng định mức chuẩn theo ngạch phóng viên</h3>
        <p className="the-khung__mo-ta">
          Các giá trị cấu hình tại đây sẽ được hệ thống gợi ý và tự động điền nhanh khi Trưởng ban giao chỉ tiêu tháng.
        </p>

        {dangTai ? (
          <p className="dang-tai">Đang tải cấu hình định mức...</p>
        ) : (
          <form onSubmit={xuLyLuu}>
            <div className="vung-bang">
              <table className="bang-kpi">
                <thead>
                  <tr>
                    <th>Mã ngạch</th>
                    <th>Tên ngạch phóng viên</th>
                    <th>Số bài tối thiểu mặc định (bài)</th>
                    <th>Điểm sàn chất lượng mặc định (0-100)</th>
                  </tr>
                </thead>
                <tbody>
                  {dinhMuc.map((dm, idx) => (
                    <tr key={dm.seniorityLevel}>
                      <td>
                        <code>{dm.seniorityLevel}</code>
                      </td>
                      <td>
                        <strong>{dm.levelName}</strong>
                      </td>
                      <td>
                        <input
                          type="number"
                          min="0"
                          required
                          className="o-nhap-ngan"
                          style={{ width: '120px' }}
                          value={dm.defaultMinPublished}
                          onChange={(e) =>
                            thayDoiGiaTri(idx, 'defaultMinPublished', e.target.value)
                          }
                        />
                      </td>
                      <td>
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          max="100"
                          required
                          className="o-nhap-ngan"
                          style={{ width: '120px' }}
                          value={dm.defaultMinAvgScore}
                          onChange={(e) =>
                            thayDoiGiaTri(idx, 'defaultMinAvgScore', e.target.value)
                          }
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div style={{ marginTop: '20px', display: 'flex', gap: '10px' }}>
              <button
                type="submit"
                className="nut-chinh"
                style={{ width: 'auto' }}
                disabled={dangLuu}
              >
                {dangLuu ? 'Đang lưu...' : 'Lưu cấu hình định mức'}
              </button>
              <button
                type="button"
                className="nut-phu"
                onClick={taiDuLieu}
                disabled={dangLuu}
              >
                Tải lại mặc định
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
