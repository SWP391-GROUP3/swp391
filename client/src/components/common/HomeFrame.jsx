import './HomeFrame.css'

/** "Thứ Ba, 13 tháng 10, 2026" */
const DINH_DANG_NGAY = new Intl.DateTimeFormat('vi-VN', {
  weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
})

/** Ten goi (chu cuoi cua ho ten): "Nguyễn Văn An" -> "An". */
function tenGoi(hoTen = '') {
  const cacTu = hoTen.trim().split(' ')
  return cacTu[cacTu.length - 1]
}

/**
 * Bo cuc chung cua trang chu moi vai tro (theo trang chu Phong vien / Figma):
 * loi chao + ngay o tren, ben duoi chia 2 cot:
 * - cot trai (children): cac the trang, moi the boc bang
 *   <section className="trang-chu__khoi"> (vd PendingList, SummaryCard)
 * - cot phai (cotPhai): cot dem so luong, thuong la <StatusCounter />
 * goiCaTen: true khi ho ten khong phai ten nguoi (vd tai khoan "Quản trị viên"),
 * chao bang ca ten thay vi chu cuoi.
 */
export default function HomeFrame({ nguoiDung, cotPhai, goiCaTen = false, children }) {
  return (
    <div className="trang-chu">
      <div className="trang-chu__dau">
        <h1 className="trang-chu__tieu-de">
          Xin chào, {goiCaTen ? nguoiDung.hoTen : tenGoi(nguoiDung.hoTen)}
        </h1>
        <p className="trang-chu__ngay">{DINH_DANG_NGAY.format(new Date())}</p>
      </div>

      <div className="trang-chu__luoi">
        <div className="trang-chu__trai">{children}</div>
        {cotPhai}
      </div>
    </div>
  )
}
