import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { layThongKeTrangThai } from '../../api/reporterHomeApi.js'
import TodoCard from './TodoCard.jsx'
import KpiProgressCard from './KpiProgressCard.jsx'
import './ReporterHome.css'

/**
 * Gop 9 trang thai cua articles.status (schema.sql) thanh 7 nhom de PV de doc.
 * "Dang cham" gom ca 3 buoc ben BF-02: BTV dang cham, cho Truong ban duyet
 * diem, da cong bo diem cho duyet dang.
 * mau: ten mau cua cham nhan (class .cham--<mau> trong index.css).
 */
const NHOM_TRANG_THAI = [
  { nhan: 'Nháp',          mau: 'xam',        cacTrangThai: ['DRAFT'] },
  { nhan: 'Chờ xác nhận',  mau: 'cam',        cacTrangThai: ['PENDING_CONFIRMATION'] },
  { nhan: 'Đã nộp',        mau: 'xanh-duong', cacTrangThai: ['SUBMITTED'] },
  { nhan: 'Đang chấm',     mau: 'tim',        cacTrangThai: ['ASSIGNED', 'PENDING_APPROVAL', 'SCORE_PUBLISHED'] },
  { nhan: 'Trả về sửa',    mau: 'hong',       cacTrangThai: ['REVISION_REQUESTED'] },
  { nhan: 'Đã đăng',       mau: 'xanh-la',    cacTrangThai: ['PUBLISHED'] },
  { nhan: 'Từ chối',       mau: 'xam-dam',    cacTrangThai: ['REJECTED'] },
]

/** "Thứ Ba, 13 tháng 10, 2026" */
const DINH_DANG_NGAY = new Intl.DateTimeFormat('vi-VN', {
  weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
})

/**
 * Cong so bai cua tung nhom tu danh sach { status, count } server tra ve.
 * Tra ve mang moi, khong sua NHOM_TRANG_THAI.
 */
function tinhSoBaiTheoNhom(thongKe) {
  const ketQua = []
  for (const nhom of NHOM_TRANG_THAI) {
    let soBai = 0
    for (const dong of thongKe) {
      if (nhom.cacTrangThai.includes(dong.status)) {
        soBai += dong.count
      }
    }
    ketQua.push({ ...nhom, soBai })
  }
  return ketQua
}

/** Ten goi (chu cuoi cua ho ten): "Nguyễn Văn An" -> "An". */
function tenGoi(hoTen = '') {
  const cacTu = hoTen.trim().split(' ')
  return cacTu[cacTu.length - 1]
}

/**
 * Trang chu cua Phong vien.
 * Cot trai: viec can xu ly + KPI thang. Cot phai: so bai theo trang thai.
 */
export default function ReporterHome({ nguoiDung }) {
  const [thongKe, setThongKe] = useState(null)   // null = dang tai
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true

    layThongKeTrangThai()
      .then((duLieu) => { if (conHienThi) setThongKe(duLieu) })
      .catch(() => { if (conHienThi) setLoi('Không tải được thống kê bài viết.') })

    return () => { conHienThi = false }
  }, [])

  const cacNhom = thongKe ? tinhSoBaiTheoNhom(thongKe) : []
  let tongSoBai = 0
  for (const nhom of cacNhom) {
    tongSoBai += nhom.soBai
  }

  return (
    <div className="trang-chu-pv">
      <div className="trang-chu-pv__dau">
        <h1 className="trang-chu-pv__tieu-de">Xin chào, {tenGoi(nguoiDung.hoTen)}</h1>
        <p className="trang-chu-pv__ngay">{DINH_DANG_NGAY.format(new Date())}</p>
      </div>

      <div className="trang-chu-pv__luoi">
        <div className="trang-chu-pv__trai">
          <TodoCard />
          <KpiProgressCard />
        </div>

        <section className="trang-thai-pv" aria-labelledby="tieu-de-trang-thai">
          <div className="trang-thai-pv__dau">
            <h2 id="tieu-de-trang-thai" className="trang-thai-pv__tieu-de">Trạng thái bài</h2>
            {thongKe && <span className="trang-thai-pv__tong">{tongSoBai} bài</span>}
          </div>

          {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
          {!thongKe && !loi && <p className="dang-tai">Đang tải…</p>}

          {thongKe && (
            <ul className="trang-thai-pv__ds">
              {cacNhom.map((nhom) => (
                <li key={nhom.nhan}>
                  {/* Bam vao mo "Bai cua toi" loc san theo nhom (UC-01) */}
                  <Link
                    className="trang-thai-pv__the"
                    to={`/my-articles?status=${nhom.cacTrangThai.join(',')}`}
                  >
                    <span className={`cham cham--${nhom.mau}`} aria-hidden="true" />
                    <span className="trang-thai-pv__nhan">{nhom.nhan}</span>
                    <span className="trang-thai-pv__so">{nhom.soBai}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
