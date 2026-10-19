import { useEffect, useState } from 'react'
import { layBaoCaoDoLech } from '../../api/reviewerDeviationApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import '../../styles/QualityReportPage.css'

/**
 * MUC LUC (so dong)
 *   15  ReviewerDeviationPage
 *   20    useEffect                  - tai du lieu khi mo trang
 *   28    locBaoCao()
 *   38    doiBoLoc()
 */

export default function ReviewerDeviationPage() {
  const [danhSach, setDanhSach] = useState(null)
  const [boLoc, setBoLoc] = useState({ period: '', editorId: '', sectionId: '' })
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true
    layBaoCaoDoLech()
      .then((data) => { if (conHienThi) setDanhSach(data) })
      .catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [])

  async function locBaoCao(event) {
    event.preventDefault()
    setLoi('')
    try {
      setDanhSach(await layBaoCaoDoLech(boLoc))
    } catch (error) {
      setLoi(error.message)
    }
  }

  function doiBoLoc(event) {
    const { name, value } = event.target
    setBoLoc((hienTai) => ({ ...hienTai, [name]: value }))
  }

  return (
    <section className="bao-cao-qc">
      <header className="bao-cao-qc__dau">
        <div>
          <h1>Báo cáo độ lệch điểm biên tập viên</h1>
          <p>Theo dõi độ ổn định chấm điểm theo tháng; dữ liệu chỉ dùng để phân tích.</p>
        </div>
      </header>

      <form className="bao-cao-qc__bo-loc" onSubmit={locBaoCao}>
        <label>
          Tháng
          <input type="month" name="period" value={boLoc.period} onChange={doiBoLoc} />
        </label>
        <label>
          Mã biên tập viên
          <input type="number" min="1" name="editorId" value={boLoc.editorId} onChange={doiBoLoc} />
        </label>
        <label>
          Ban
          <select name="sectionId" value={boLoc.sectionId} onChange={doiBoLoc}>
            <option value="">Tất cả</option>
            <option value="1">Ban Thời sự</option>
            <option value="2">Ban Kinh tế</option>
          </select>
        </label>
        <button className="nut-nhe" type="submit">Lọc báo cáo</button>
      </form>

      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {!danhSach && !loi && <p className="dang-tai">Đang tải báo cáo…</p>}
      {danhSach?.length === 0 && <p className="trong-rong">Chưa có dữ liệu độ lệch trong kỳ đã chọn.</p>}

      {danhSach?.length > 0 && (
        <div className="bao-cao-qc__bang-boc">
          <table className="bao-cao-qc__bang">
            <thead>
              <tr>
                <th>Tháng</th>
                <th>Biên tập viên</th>
                <th>Ban</th>
                <th>Độ lệch TB</th>
                <th>Khiếu nại</th>
                <th>Tỷ lệ đổi điểm</th>
                <th>Khối lượng</th>
                <th>Cập nhật</th>
              </tr>
            </thead>
            <tbody>
              {danhSach.map((item) => (
                <tr key={item.id}>
                  <td>{item.period.slice(0, 7)}</td>
                  <td>{item.editorName}</td>
                  <td>{item.sectionName}</td>
                  <td>{formatNumber(item.avgDeviation)}</td>
                  <td>{item.appealCount}</td>
                  <td>{formatNumber(item.overturnRate)}%</td>
                  <td>{item.workloadCount}</td>
                  <td>{formatDate(item.lastUpdated)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
