import { useId } from 'react'
import { Link } from 'react-router-dom'
import { daysUntil } from '../../utils/formatDate.js'
import '../../styles/PendingList.css'

/** Con <= so ngay nay thi to do han chot cho nguoi dung de y. */
const NGAY_GAP = 1

/**
 * Chu hien han chot. daysUntil lam tron len nen:
 * <= 0 -> da qua han; 1 -> con chua toi 24 tieng; n -> con n ngay.
 */
function chuHanChot(hanChot) {
  const soNgay = daysUntil(hanChot)
  if (soNgay <= 0) {
    return 'đã quá hạn'
  }
  if (soNgay === 1) {
    return 'còn dưới 1 ngày'
  }
  return `còn ${soNgay} ngày`
}

/**
 * Khoi "Chờ xử lý" tren trang chu cua moi vai tro.
 * Component nay chi VE; trang chu cua tung vai tro tu goi API va doi du lieu
 * thanh danh sach viec, moi viec co dang:
 *   { khoa, loai, mau, tieuDe, moTa, hanChot, duongDan, nut }
 *   - mau: ten mau cham (class .cham--<mau> trong index.css)
 *   - hanChot: chuoi ngay gio hoac null (khong co han)
 *
 * - danhSach: null = dang tai
 * - loi: chu bao loi khi tai that bai
 */
export default function PendingList({ danhSach, loi }) {
  const idTieuDe = useId()

  return (
    <section className="trang-chu__khoi" aria-labelledby={idTieuDe}>
      <div className="trang-chu__khoi-dau">
        <h2 id={idTieuDe} className="trang-chu__khoi-tieu-de">Chờ xử lý</h2>
        {danhSach && danhSach.length > 0 && (
          <span className="ds-cho__huy-hieu">
            {danhSach.length}<span className="an-di"> việc</span>
          </span>
        )}
      </div>

      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {!danhSach && !loi && <p className="dang-tai">Đang tải…</p>}

      {danhSach && danhSach.length === 0 && (
        <p className="trong-rong">Không có việc nào đang chờ xử lý 🎉</p>
      )}

      {danhSach && danhSach.length > 0 && (
        <ul className="ds-cho__ds">
          {danhSach.map((viec) => (
            <li key={viec.khoa} className="ds-cho__muc">
              <div className="ds-cho__noi-dung">
                <p className="ds-cho__loai">
                  <span className={`cham cham--${viec.mau}`} aria-hidden="true" />
                  {viec.loai}
                  {viec.hanChot && (
                    <span className={daysUntil(viec.hanChot) <= NGAY_GAP ? 'ds-cho__han ds-cho__han--gap' : 'ds-cho__han'}>
                      · {chuHanChot(viec.hanChot)}
                    </span>
                  )}
                </p>
                <p className="ds-cho__tieu-de">{viec.tieuDe}</p>
                <p className="ds-cho__mo-ta">{viec.moTa}</p>
              </div>
              <Link to={viec.duongDan} className="nut-nhe">{viec.nut}</Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
