import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { layBaiCuaToi } from '../../api/articleApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { NHOM_TRANG_THAI, MAU_TRANG_THAI, tenTrangThai, tenTinhChat } from '../../utils/trangThaiBai.js'
import Icon from '../../components/common/Icon.jsx'
import './MyArticles.css'

/** So bai moi trang (AGENTS.md 3.7: ?page=0&size=10) */
const SO_DONG_MOI_TRANG = 10

/**
 * UC-01 - "Bai cua toi": bai ma Phong vien la tac gia chinh hoac dong tac gia.
 * Loc theo nhom trang thai bang URL (?status=A,B), nen trang chu link thang toi
 * dung nhom duoc. Bam tieu de de mo chi tiet bai (UC-02).
 */
export default function MyArticles() {
  const [thamSoUrl, setThamSoUrl] = useSearchParams()
  const chuoiTrangThai = thamSoUrl.get('status') || ''   // vd "ASSIGNED,PENDING_APPROVAL"
  const trang = Number(thamSoUrl.get('page') || 0)

  const [ketQua, setKetQua] = useState(null)   // null = dang tai
  const [loi, setLoi] = useState('')

  // Tai lai danh sach moi khi doi bo loc hoac doi trang.
  // Trong luc tai, bang cu van hien cho toi khi co du lieu moi.
  useEffect(() => {
    let conHienThi = true

    const cacTrangThai = chuoiTrangThai ? chuoiTrangThai.split(',') : []
    layBaiCuaToi(cacTrangThai, trang, SO_DONG_MOI_TRANG)
      .then((duLieu) => {
        if (conHienThi) {
          setKetQua(duLieu)
          setLoi('')
        }
      })
      .catch((err) => { if (conHienThi) setLoi(err.message) })

    return () => { conHienThi = false }
  }, [chuoiTrangThai, trang])

  /** Bam 1 nhom loc: ghi vao URL va quay ve trang dau. Chuoi rong = "Tat ca". */
  function chonNhom(chuoi) {
    const moi = new URLSearchParams()
    if (chuoi) {
      moi.set('status', chuoi)
    }
    setThamSoUrl(moi)
  }

  function chuyenTrang(trangMoi) {
    const moi = new URLSearchParams(thamSoUrl)
    moi.set('page', trangMoi)
    setThamSoUrl(moi)
  }

  return (
    <div className="ds-bai">
      <div className="ds-bai__dau">
        <div>
          <h1 className="ds-bai__tieu-de">Bài của tôi</h1>
          <p className="ds-bai__mo-ta">Bài bạn là tác giả chính hoặc đồng tác giả, sửa gần nhất xếp trước.</p>
        </div>
        {/* UC-03 lam o Iter 2, route /my-articles/new dang la trang "sap co" */}
        <Link to="/my-articles/new" className="ds-bai__nut-chinh">
          <Icon><path d="M12 5v14M5 12h14" /></Icon>
          <span>Soạn bài mới</span>
        </Link>
      </div>

      {/* Bo loc theo nhom trang thai */}
      <div className="ds-bai__loc" role="group" aria-label="Lọc theo trạng thái">
        <button
          type="button"
          className={`ds-bai__chip ${chuoiTrangThai === '' ? 'ds-bai__chip--chon' : ''}`}
          onClick={() => chonNhom('')}
        >
          Tất cả
        </button>
        {NHOM_TRANG_THAI.map((nhom) => {
          const chuoiNhom = nhom.cacTrangThai.join(',')
          return (
            <button
              key={nhom.nhan}
              type="button"
              className={`ds-bai__chip ${chuoiTrangThai === chuoiNhom ? 'ds-bai__chip--chon' : ''}`}
              onClick={() => chonNhom(chuoiNhom)}
            >
              <span className={`cham cham--${nhom.mau}`} aria-hidden="true" />
              {nhom.nhan}
            </button>
          )
        })}
      </div>

      <div className="ds-bai__the">
        {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
        {!ketQua && !loi && <p className="ds-bai__trong">Đang tải…</p>}

        {ketQua && ketQua.items.length === 0 && (
          <p className="ds-bai__trong">Không có bài nào ở bộ lọc này.</p>
        )}

        {ketQua && ketQua.items.length > 0 && (
          <div className="ds-bai__cuon">
            <table className="ds-bai__bang">
              <thead>
                <tr>
                  <th>Tác phẩm</th>
                  <th>Thể loại</th>
                  <th>Tính chất</th>
                  <th>Vai trò của bạn</th>
                  <th>Trạng thái</th>
                  <th>Ngày nộp</th>
                  <th>Cập nhật</th>
                </tr>
              </thead>
              <tbody>
                {ketQua.items.map((bai) => (
                  <tr key={bai.id}>
                    <td>
                      <Link to={`/my-articles/${bai.id}`} className="ds-bai__ten-bai">{bai.title}</Link>
                    </td>
                    {/* TODO(admin): hien ten the loai khi server tra ve (can entity Genre) */}
                    <td>Mã {bai.categoryId}</td>
                    <td className="ds-bai__khong-xuong-dong">{tenTinhChat(bai.declaredNature)}</td>
                    <td className="ds-bai__khong-xuong-dong">
                      {bai.myRole === 'MAIN_AUTHOR' ? (
                        <span className="ds-bai__vai-tro ds-bai__vai-tro--chinh">Tác giả chính</span>
                      ) : (
                        <span className="ds-bai__vai-tro">Đồng tác giả</span>
                      )}
                      {bai.mySharePercent != null && (
                        <span className="ds-bai__ty-le"> · {Number(bai.mySharePercent)}%</span>
                      )}
                    </td>
                    <td>
                      <span className="ds-bai__trang-thai">
                        <span className={`cham cham--${MAU_TRANG_THAI[bai.status]}`} aria-hidden="true" />
                        {tenTrangThai(bai.status)}
                      </span>
                    </td>
                    <td className="ds-bai__ngay">{formatDate(bai.submittedAt)}</td>
                    <td className="ds-bai__ngay">{formatDate(bai.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Phan trang: chi hien khi co tu 2 trang tro len */}
        {ketQua && ketQua.totalPages > 1 && (
          <div className="ds-bai__phan-trang">
            <button type="button" className="nut-nhe" disabled={trang === 0}
              onClick={() => chuyenTrang(trang - 1)}>
              Trang trước
            </button>
            <span>Trang {ketQua.page + 1} / {ketQua.totalPages} · {ketQua.totalElements} bài</span>
            <button type="button" className="nut-nhe" disabled={trang + 1 >= ketQua.totalPages}
              onClick={() => chuyenTrang(trang + 1)}>
              Trang sau
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
