import { useEffect, useState } from 'react'
import { layNhatKy } from '../../api/auditLogApi.js'
import { dinhDangThoiGian } from './appealFormat.js'
import './QualityReportPage.css'

export default function AuditLogPage() {
  const [danhSach, setDanhSach] = useState(null)
  const [boLoc, setBoLoc] = useState({
    actorId: '',
    articleId: '',
    appealId: '',
    editorId: '',
    actionType: '',
    entityType: '',
    entityId: '',
    fromTime: '',
    toTime: '',
  })
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true
    layNhatKy()
      .then((data) => { if (conHienThi) setDanhSach(data) })
      .catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [])

  async function locNhatKy(event) {
    event.preventDefault()
    setLoi('')
    try {
      setDanhSach(await layNhatKy(boLoc))
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
        <h1>Nhật ký hệ thống và chấm điểm</h1>
        <p>Nhật ký chỉ đọc, sắp xếp từ sự kiện mới nhất.</p>
      </header>

      <form className="bao-cao-qc__bo-loc" onSubmit={locNhatKy}>
        <label>Bài viết<input type="number" min="1" name="articleId" value={boLoc.articleId} onChange={doiBoLoc} /></label>
        <label>Khiếu nại<input type="number" min="1" name="appealId" value={boLoc.appealId} onChange={doiBoLoc} /></label>
        <label>Biên tập viên<input type="number" min="1" name="editorId" value={boLoc.editorId} onChange={doiBoLoc} /></label>
        <label>
          Người thao tác
          <input type="number" min="1" name="actorId" value={boLoc.actorId} onChange={doiBoLoc} />
        </label>
        <label>
          Hành động
          <input name="actionType" placeholder="CREATE" value={boLoc.actionType} onChange={doiBoLoc} />
        </label>
        <label>
          Loại dữ liệu
          <input name="entityType" placeholder="appeals" value={boLoc.entityType} onChange={doiBoLoc} />
        </label>
        <label>
          Mã dữ liệu
          <input type="number" min="1" name="entityId" value={boLoc.entityId} onChange={doiBoLoc} />
        </label>
        <label>
          Từ ngày
          <input type="date" name="fromTime" value={boLoc.fromTime} onChange={doiBoLoc} />
        </label>
        <label>
          Đến ngày
          <input type="date" name="toTime" value={boLoc.toTime} onChange={doiBoLoc} />
        </label>
        <button className="nut-nhe" type="submit">Lọc nhật ký</button>
      </form>

      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {!danhSach && !loi && <p className="dang-tai">Đang tải nhật ký…</p>}
      {danhSach?.length === 0 && <p className="trong-rong">Không có nhật ký phù hợp.</p>}

      {danhSach?.length > 0 && (
        <div className="bao-cao-qc__bang-boc">
          <table className="bao-cao-qc__bang bao-cao-qc__bang--nhat-ky">
            <thead>
              <tr>
                <th>Thời gian</th>
                <th>Người thao tác</th>
                <th>Hành động</th>
                <th>Dữ liệu</th>
                <th>Giá trị cũ</th>
                <th>Giá trị mới</th>
              </tr>
            </thead>
            <tbody>
              {danhSach.map((item) => (
                <tr key={item.id}>
                  <td>{dinhDangThoiGian(item.createdAt)}</td>
                  <td>{item.actorDisplay}</td>
                  <td>{item.actionType}</td>
                  <td>{item.entityType} #{item.entityId}</td>
                  <td><details><summary>Xem giá trị cũ</summary><pre>{item.oldValue || '—'}</pre></details></td>
                  <td><details><summary>Xem giá trị mới</summary><pre>{item.newValue || '—'}</pre></details></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
