import { useId } from 'react'
import { Link } from 'react-router-dom'
import './SummaryCard.css'

/**
 * The tom tat o cot trai trang chu: tieu de + nhan trang thai + vai dong "nhan ... so".
 * Vd "Kỳ nhuận bút 10/2026 [Đang mở]", "KPI của ban tháng 10/2026 [Chưa chốt]".
 *
 * - nhan:     { chu, kieu } hoac null. kieu: 'tot' (xanh la) | 'chu-y' (cam) | 'thuong' (xam)
 * - cacDong:  [{ nhan, giaTri, canhBao }]; canhBao = true thi to do giaTri
 * - lienKet:  { to, chu } hoac null - link "Xem chi tiết →" o cuoi the
 * - dangTai, loi: trang thai tai du lieu
 */
export default function SummaryCard({ tieuDe, nhan, cacDong, lienKet, dangTai, loi }) {
  const idTieuDe = useId()

  return (
    <section className="trang-chu__khoi" aria-labelledby={idTieuDe}>
      <div className="trang-chu__khoi-dau">
        <h2 id={idTieuDe} className="trang-chu__khoi-tieu-de">{tieuDe}</h2>
        {nhan && <span className={`tom-tat__nhan tom-tat__nhan--${nhan.kieu}`}>{nhan.chu}</span>}
      </div>

      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {dangTai && !loi && <p className="dang-tai">Đang tải…</p>}

      {!dangTai && !loi && (
        <>
          <dl className="tom-tat__ds">
            {cacDong.map((dong) => (
              <div key={dong.nhan} className="tom-tat__dong">
                <dt>{dong.nhan}</dt>
                <dd className={dong.canhBao ? 'tom-tat__so tom-tat__so--canh-bao' : 'tom-tat__so'}>
                  {dong.giaTri}
                </dd>
              </div>
            ))}
          </dl>
          {lienKet && <Link to={lienKet.to} className="tom-tat__xem">{lienKet.chu} →</Link>}
        </>
      )}
    </section>
  )
}
