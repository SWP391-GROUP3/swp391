import { useEffect, useState } from 'react'
import { layThongKeTrangThai } from '../../api/reporterHomeApi.js'
import HomeFrame from '../../components/common/HomeFrame.jsx'
import StatusCounter from '../../components/common/StatusCounter.jsx'
import TodoCard from './TodoCard.jsx'
import KpiProgressCard from './KpiProgressCard.jsx'

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

/**
 * Cong so bai cua tung nhom tu danh sach { status, count } server tra ve,
 * doi thanh dang StatusCounter can: { nhan, mau, so, duongDan }.
 * Bam vao 1 nhom mo "Bai cua toi" loc san theo nhom do (UC-01).
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
    ketQua.push({
      nhan: nhom.nhan,
      mau: nhom.mau,
      so: soBai,
      duongDan: `/my-articles?status=${nhom.cacTrangThai.join(',')}`,
    })
  }
  return ketQua
}

/**
 * Trang chu cua Phong vien.
 * Cot trai: viec cho xu ly + KPI thang. Cot phai: so bai theo trang thai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   14  NHOM_TRANG_THAI                 Gop 9 trang thai cua articles.status (schema.sql) thanh 7 nhom de PV de doc
 *   29  tinhSoBaiTheoNhom()             Cong so bai cua tung nhom tu danh sach { status, count } server tra ve, doi thanh dang StatusCounter can: { n…
 *   58  ReporterHome()                  xem tom tat dau file
 *   62    useEffect                     tai du lieu khi mo trang
 */
export default function ReporterHome({ nguoiDung }) {
  const [cacNhom, setCacNhom] = useState(null)   // null = dang tai
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true

    layThongKeTrangThai()
      .then((duLieu) => { if (conHienThi) setCacNhom(tinhSoBaiTheoNhom(duLieu)) })
      .catch(() => { if (conHienThi) setLoi('Không tải được thống kê bài viết.') })

    return () => { conHienThi = false }
  }, [])

  return (
    <HomeFrame
      nguoiDung={nguoiDung}
      cotPhai={<StatusCounter tieuDe="Trạng thái bài" cacNhom={cacNhom} loi={loi} />}
    >
      <TodoCard />
      <KpiProgressCard />
    </HomeFrame>
  )
}
