import { useId } from 'react'
import { Link } from 'react-router-dom'
import '../../styles/StatusCounter.css'

/**
 * Cot dem so luong ben phai trang chu (kieu cot "To do" cua Figma):
 * moi dong 1 cham mau + nhan + so, bam vao mo trang danh sach da loc san.
 *
 * - tieuDe:   vd "Trạng thái bài"
 * - cacNhom:  [{ nhan, mau, so, duongDan }], null = dang tai
 * - donVi:    chu sau tong, vd "bài"
 * - hienTong: false khi cac nhom chong len nhau / khac don vi (cong lai vo nghia)
 * - loi:      chu bao loi khi tai that bai
 *
 * MUC LUC (so dong)
 *   18  StatusCounter                - Cot dem so luong ben phai trang chu (kieu cot "To do" cua Figma):
 */
export default function StatusCounter({ tieuDe, cacNhom, donVi = 'bài', hienTong = true, loi }) {
  const idTieuDe = useId()

  let tong = 0
  for (const nhom of cacNhom ?? []) {
    tong += nhom.so
  }

  return (
    <section className="dem-tt" aria-labelledby={idTieuDe}>
      <div className="dem-tt__dau">
        <h2 id={idTieuDe} className="dem-tt__tieu-de">{tieuDe}</h2>
        {cacNhom && hienTong && <span className="dem-tt__tong">{tong} {donVi}</span>}
      </div>

      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {!cacNhom && !loi && <p className="dang-tai">Đang tải…</p>}

      {cacNhom && (
        <ul className="dem-tt__ds">
          {cacNhom.map((nhom) => (
            <li key={nhom.nhan}>
              <Link className="dem-tt__the" to={nhom.duongDan}>
                <span className={`cham cham--${nhom.mau}`} aria-hidden="true" />
                <span className="dem-tt__nhan">{nhom.nhan}</span>
                <span className="dem-tt__so">{nhom.so}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
