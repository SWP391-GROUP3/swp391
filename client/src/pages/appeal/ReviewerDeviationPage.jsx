import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { layBaoCaoDoLech } from '../../api/reviewerDeviationApi.js'
import { layDanhSachBienTapVien } from '../../api/reviewerProfileApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import '../../styles/QualityReportPage.css'

export default function ReviewerDeviationPage() {
  const location = useLocation()
  const [danhSach, setDanhSach] = useState(null)
  const [boLoc, setBoLoc] = useState(location.state?.boLoc ?? { period: '', editorId: '', sectionId: '' })
  const [loi, setLoi] = useState('')
  const [bienTapVien, setBienTapVien] = useState(null)
  const [loiDanhBa, setLoiDanhBa] = useState('')

  useEffect(() => {
    let conHienThi = true
    layDanhSachBienTapVien()
      .then((data) => { if (conHienThi) setBienTapVien(data) })
      .catch((error) => { if (conHienThi) setLoiDanhBa(error.message) })
    return () => { conHienThi = false }
  }, [])

  useEffect(() => {
    let conHienThi = true
    layBaoCaoDoLech(location.state?.boLoc ?? {})
      .then((data) => { if (conHienThi) setDanhSach(data) })
      .catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [location.state])

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
                  <td><Link to={`/reviewers/${item.editorId}?period=${item.period.slice(0, 7)}`} state={{ boLoc }}>{item.editorName}</Link></td>
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
      <h2>Hồ sơ biên tập viên</h2>
      <p>Có thể xem hồ sơ ngay cả khi chưa có dữ liệu so sánh điểm.</p>
      {loiDanhBa && <p role="alert" className="thong-bao-loi">{loiDanhBa}</p>}
      {!bienTapVien && !loiDanhBa && <p>Đang tải danh sách biên tập viên…</p>}
      {bienTapVien?.length === 0 && <p>Chưa có biên tập viên trong phạm vi được xem.</p>}
      {bienTapVien?.length > 0 && <div className="bao-cao-qc__bang-boc"><table className="bao-cao-qc__bang">
        <thead><tr><th>Biên tập viên</th><th>Ban</th><th>Tài khoản</th></tr></thead>
        <tbody>{bienTapVien.map((btv) => <tr key={btv.editorId}>
          <td><Link to={`/reviewers/${btv.editorId}${boLoc.period ? `?period=${boLoc.period}` : ''}`} state={{ boLoc }}>{btv.editorName}</Link></td>
          <td>{btv.sectionName || 'Chưa có ban'}</td>
          <td>{btv.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã khóa'}</td>
        </tr>)}</tbody>
      </table></div>}
    </section>
  )
}
