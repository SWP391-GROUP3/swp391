import { Link } from 'react-router-dom'
import '../styles/ComingSoon.css'

/**
 * Trang tam cho chuc nang chua lam, de bam menu khong bi day ve trang chu.
 * Lam xong chuc nang nao thi doi route cua no trong routes/AppRoutes.jsx sang trang that.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   14  ComingSoon()                    xem tom tat dau file
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   19  link [Ve trang chu]             mo trang /main (trang chu theo vai tro)
 */
export default function ComingSoon({ tenChucNang }) {
  return (
    <div className="dang-xay-dung">
      <h1 className="dang-xay-dung__tieu-de">{tenChucNang}</h1>
      <p className="dang-xay-dung__mo-ta">Chức năng này đang được xây dựng.</p>
      <Link to="/main" className="nut-nhe">Về trang chủ</Link>
    </div>
  )
}
