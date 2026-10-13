import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { layChiTietBai, rutBai } from '../../api/articleApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { MAU_TRANG_THAI, tenTrangThai, tenTinhChat, tenXacNhan } from '../../utils/trangThaiBai.js'
import Icon from '../../components/common/Icon.jsx'
import './ArticleDetail.css'

/**
 * UC-02 - Chi tiet bai: noi dung, khai bao, tac gia + trang thai xac nhan,
 * lich su trang thai. Tac gia chinh rut duoc bai dang "Da nop" (UC-11, BR-IN-08):
 * server tra canWithdraw de biet co hien nut hay khong.
 */
export default function ArticleDetail() {
  const { id } = useParams()

  const [bai, setBai] = useState(null)        // null = dang tai
  const [loi, setLoi] = useState('')
  const [hoiRutBai, setHoiRutBai] = useState(false)   // dang hien hop xac nhan rut bai
  const [dangRut, setDangRut] = useState(false)
  const [thongBao, setThongBao] = useState('')        // bao thanh cong sau khi rut

  // Tai chi tiet bai khi mo trang (hoac doi sang bai khac)
  useEffect(() => {
    let conHienThi = true

    layChiTietBai(id)
      .then((duLieu) => {
        if (conHienThi) {
          setBai(duLieu)
          setLoi('')
        }
      })
      .catch((err) => { if (conHienThi) setLoi(err.message) })

    return () => { conHienThi = false }
  }, [id])

  /** Bam "Xac nhan rut bai" trong hop hoi: goi API, thay du lieu bai bang ban moi. */
  function xacNhanRutBai() {
    setDangRut(true)
    setLoi('')
    rutBai(id)
      .then((duLieu) => {
        setBai(duLieu)
        setThongBao('Đã rút bài về trạng thái Nháp.')
      })
      .catch((err) => setLoi(err.message))
      .finally(() => {
        setDangRut(false)
        setHoiRutBai(false)
      })
  }

  return (
    <div className="ct-bai">
      <Link to="/my-articles" className="ct-bai__quay-lai">
        <Icon><path d="M15 6l-6 6 6 6" /></Icon>
        <span>Bài của tôi</span>
      </Link>

      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {!bai && !loi && <p className="ct-bai__dang-tai">Đang tải…</p>}

      {bai && (
        <>
          <div className="ct-bai__dau">
            <div>
              <h1 className="ct-bai__tieu-de">{bai.title}</h1>
              <span className="ct-bai__trang-thai">
                <span className={`cham cham--${MAU_TRANG_THAI[bai.status]}`} aria-hidden="true" />
                {tenTrangThai(bai.status)}
              </span>
            </div>
            {bai.canWithdraw && !hoiRutBai && (
              <button type="button" className="nut-nhe ct-bai__nut-rut" onClick={() => setHoiRutBai(true)}>
                Rút bài
              </button>
            )}
          </div>

          {thongBao && <p className="ct-bai__thanh-cong" role="status">{thongBao}</p>}

          {/* Hop hoi lai truoc khi rut (BR-IN-08) */}
          {hoiRutBai && (
            <div className="ct-bai__hop-hoi" role="alertdialog" aria-labelledby="hoi-rut-bai">
              <p id="hoi-rut-bai">
                Rút bài về <strong>Nháp</strong>? Bài sẽ rời hàng chờ phân công; bạn sửa xong thì nộp lại.
              </p>
              <div className="ct-bai__hop-hoi-nut">
                <button type="button" className="nut-nhe" onClick={() => setHoiRutBai(false)} disabled={dangRut}>
                  Huỷ
                </button>
                <button type="button" className="ct-bai__nut-xac-nhan" onClick={xacNhanRutBai} disabled={dangRut}>
                  {dangRut ? 'Đang rút…' : 'Xác nhận rút bài'}
                </button>
              </div>
            </div>
          )}

          <div className="ct-bai__luoi">
            {/* Cot trai: noi dung bai */}
            <section className="ct-bai__khoi">
              <h2 className="ct-bai__khoi-tieu-de">Nội dung</h2>
              <div className="ct-bai__noi-dung">{bai.content || 'Chưa có nội dung.'}</div>
            </section>

            {/* Cot phai: khai bao, tac gia, lich su */}
            <div className="ct-bai__phai">
              <section className="ct-bai__khoi">
                <h2 className="ct-bai__khoi-tieu-de">Khai báo</h2>
                <dl className="ct-bai__khai-bao">
                  {/* TODO(admin): hien ten the loai, ten ban khi server tra ve */}
                  <dt>Thể loại khai</dt><dd>Mã {bai.declaredCategoryId}</dd>
                  <dt>Thể loại áp dụng</dt><dd>Mã {bai.categoryId}</dd>
                  <dt>Tính chất</dt><dd>{tenTinhChat(bai.declaredNature)}</dd>
                  <dt>Sự kiện</dt><dd>{bai.eventId ? `Mã ${bai.eventId}` : 'Không'}</dd>
                  <dt>Ngày tạo</dt><dd>{formatDate(bai.createdAt)}</dd>
                  <dt>Nộp gần nhất</dt><dd>{formatDate(bai.submittedAt)}</dd>
                  {bai.withdrawnAt && (<><dt>Rút gần nhất</dt><dd>{formatDate(bai.withdrawnAt)}</dd></>)}
                </dl>
              </section>

              <section className="ct-bai__khoi">
                <h2 className="ct-bai__khoi-tieu-de">Tác giả</h2>
                <ul className="ct-bai__ds">
                  {bai.authors.map((tacGia) => (
                    <li key={tacGia.userId} className="ct-bai__tac-gia">
                      <div>
                        {/* TODO(admin): hien ho ten khi co entity User */}
                        <span className="ct-bai__ten">Người dùng #{tacGia.userId}</span>
                        <span className="ct-bai__phu">
                          {tacGia.mainAuthor ? 'Tác giả chính' : 'Đồng tác giả'} · {Number(tacGia.sharePercent)}%
                        </span>
                      </div>
                      <span className="ct-bai__phu">
                        {tenXacNhan(tacGia.confirmStatus)}
                        {tacGia.confirmStatus === 'PENDING' && tacGia.confirmDeadline
                          && ` (hạn ${formatDate(tacGia.confirmDeadline)})`}
                      </span>
                    </li>
                  ))}
                </ul>
              </section>

              <section className="ct-bai__khoi">
                <h2 className="ct-bai__khoi-tieu-de">Lịch sử trạng thái</h2>
                <ol className="ct-bai__ds ct-bai__lich-su">
                  {bai.statusLogs.map((dong, viTri) => (
                    <li key={viTri}>
                      <span className="ct-bai__trang-thai">
                        <span className={`cham cham--${MAU_TRANG_THAI[dong.toStatus]}`} aria-hidden="true" />
                        {tenTrangThai(dong.toStatus)}
                      </span>
                      <span className="ct-bai__phu"> · {formatDate(dong.createdAt)}</span>
                      {dong.note && <p className="ct-bai__ghi-chu">{dong.note}</p>}
                    </li>
                  ))}
                </ol>
              </section>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
