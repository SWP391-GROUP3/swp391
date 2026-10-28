import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { layKhieuNaiCuaToi } from '../../api/appealApi.js'
import { dinhDangThoiGian } from './appealFormat.js'
import '../../styles/AppealPage.css'

/**
 * Man Phong vien xem cac khieu nai da gui va trang thai xu ly (UC-35); nut tao khieu nai moi.
 *
 * MO TRANG NAY TAI: /my-appeals
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  TEN_TRANG_THAI                  hang so
 *   27  MyAppealsPage()                 my appeals page
 *   32    useEffect                     tai du lieu khi mo trang
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   47  link [Gui khieu nai]            mo trang /my-appeals/new (AppealCreatePage)
 */

const TEN_TRANG_THAI = {
  SUBMITTED: 'Đã gửi',
  IN_REVIEW: 'Đang xử lý',
  RESOLVED: 'Đã giải quyết',
}

export default function MyAppealsPage() {
  const location = useLocation()
  const [danhSach, setDanhSach] = useState(null)
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true
    layKhieuNaiCuaToi()
      .then((data) => { if (conHienThi) setDanhSach(data) })
      .catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [])

  return (
    <div className="khieu-nai-pv">
      <div className="khieu-nai-pv__dau">
        <div>
          <h1 className="khieu-nai-pv__tieu-de">Khiếu nại điểm của tôi</h1>
          <p className="khieu-nai-pv__phu-de">Theo dõi trạng thái các khiếu nại đã gửi.</p>
        </div>
        <Link to="/my-appeals/new" className="nut-nhe">Gửi khiếu nại</Link>
      </div>

      {location.state?.thongBao && <p className="thong-bao-thanh-cong">{location.state.thongBao}</p>}
      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {!danhSach && !loi && <p className="dang-tai">Đang tải danh sách…</p>}
      {danhSach?.length === 0 && <p className="trong-rong">Bạn chưa gửi khiếu nại nào.</p>}

      {danhSach?.map((appeal) => (
        <article key={appeal.id} className="khieu-nai-pv__the khieu-nai-pv__muc">
          <div>
            <h2>{appeal.articleTitle}</h2>
            <p>Gửi lúc {dinhDangThoiGian(appeal.submittedAt)} · {appeal.criteria.length} tiêu chí</p>
            {appeal.status === 'RESOLVED' && <p>Điểm hiệu lực đã xác nhận: <strong>{appeal.effectiveScore ?? 'Đang cập nhật'}/100</strong> · {dinhDangThoiGian(appeal.resolvedAt)}</p>}
            <details>
              <summary>Tiêu chí đã khiếu nại</summary>
              {appeal.criteria.map((item) => (
                <p key={item.criterionId}><strong>{item.criterionName}</strong>: {item.reason}</p>
              ))}
            </details>
          </div>
          <span className={`khieu-nai-pv__trang-thai khieu-nai-pv__trang-thai--${appeal.status.toLowerCase()}`}>
            {TEN_TRANG_THAI[appeal.status] || appeal.status}
          </span>
        </article>
      ))}
    </div>
  )
}
