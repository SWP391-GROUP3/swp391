import { useEffect, useState } from 'react'
import { layTrangChuThuKy } from '../../api/secretaryHomeApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatMoney } from '../../utils/formatMoney.js'
import HomeFrame from '../../components/common/HomeFrame.jsx'
import StatusCounter from '../../components/common/StatusCounter.jsx'
import PendingList from '../../components/common/PendingList.jsx'
import SummaryCard from '../../components/common/SummaryCard.jsx'

/**
 * Nhan hien thi cho royalty_periods.status (BR-RY-07: OPEN -> CLOSED -> LOCKED -> PAID).
 * kieu: mau nhan cua SummaryCard.
 */
const NHAN_TRANG_THAI_KY = {
  OPEN:   { chu: 'Đang mở',         kieu: 'thuong' },
  CLOSED: { chu: 'Đã gửi Kế toán',  kieu: 'chu-y' },
  LOCKED: { chu: 'Đã khoá',         kieu: 'tot' },
  PAID:   { chu: 'Đã chi',          kieu: 'tot' },
}

/**
 * Gop viec cua Thu ky thanh 1 danh sach "Chờ xử lý":
 * ky bi Ke toan tra ve (neu co) dung dau, sau do la bai cho cham theo han chi.
 */
function gopDanhSachViec(duLieu) {
  const danhSach = []
  const ky = duLieu.openPeriod

  // UC-52: Ke toan tra ky ve kem ghi chu -> Thu ky sua roi gui lai (UC-50)
  if (ky.returnNote) {
    danhSach.push({
      khoa: `tra-ky-${ky.periodId}`,
      loai: 'Kế toán trả kỳ',
      mau: 'hong',
      tieuDe: `Kỳ nhuận bút ${ky.periodMonth}/${ky.periodYear}`,
      moTa: `“${ky.returnNote}”`,
      hanChot: null,
      duongDan: `/royalty-periods/${ky.periodId}`,
      nut: 'Xem & gửi lại',
    })
  }

  // UC-46, 47: bai da dang chua cham nhuan but, han chi gan nhat len truoc
  for (const bai of duLieu.pendingArticles) {
    danhSach.push({
      khoa: `cham-nb-${bai.articleId}`,
      loai: 'Chờ chấm nhuận bút',
      mau: 'xanh-duong',
      tieuDe: bai.title,
      moTa: `Ban ${bai.sectionName} · đăng ${formatDate(bai.publishedAt)}`,
      hanChot: bai.paymentDeadline,
      duongDan: `/royalty-grading/${bai.articleId}`,
      nut: 'Chấm nhuận bút',
    })
  }

  return danhSach
}

/** Cac o dem ben phai. 2 o dau chong nhau (sap toi han nam trong cho cham) nen khong hien tong. */
function taoCacNhom(duLieu) {
  return [
    { nhan: 'Chờ chấm nhuận bút', mau: 'xanh-duong', so: duLieu.counts.pendingGrading, duongDan: '/royalty-grading' },
    { nhan: 'Sắp tới hạn chi',    mau: 'hong',       so: duLieu.counts.nearDeadline,   duongDan: '/royalty-grading?sapToiHan=1' },
    { nhan: 'Đã chấm trong kỳ',   mau: 'xanh-la',    so: duLieu.openPeriod.gradedCount, duongDan: `/royalty-periods/${duLieu.openPeriod.periodId}` },
  ]
}

/**
 * Trang chu cua Thu ky toa soan (BF-04).
 * Cot trai: viec cho xu ly + tom tat ky nhuan but dang mo. Cot phai: so bai theo nhom.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   14  NHAN_TRANG_THAI_KY              Nhan hien thi cho royalty_periods.status (BR-RY-07: OPEN -> CLOSED -> LOCKED -> PAID)
 *   25  gopDanhSachViec()               Gop viec cua Thu ky thanh 1 danh sach "Cho xu ly": ky bi Ke toan tra ve (neu co) dung dau, sau do la bai cho …
 *   61  taoCacNhom()                    Cac o dem ben phai. 2 o dau chong nhau (sap toi han nam trong cho cham) nen khong hien tong.
 *   80  SecretaryHome()                 xem tom tat dau file
 *   84    useEffect                     tai du lieu khi mo trang
 */
export default function SecretaryHome({ nguoiDung }) {
  const [duLieu, setDuLieu] = useState(null)   // null = dang tai
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true

    layTrangChuThuKy()
      .then((ketQua) => { if (conHienThi) setDuLieu(ketQua) })
      .catch(() => { if (conHienThi) setLoi('Không tải được dữ liệu trang chủ.') })

    return () => { conHienThi = false }
  }, [])

  const ky = duLieu?.openPeriod

  return (
    <HomeFrame
      nguoiDung={nguoiDung}
      cotPhai={
        <StatusCounter
          tieuDe="Nhuận bút"
          cacNhom={duLieu ? taoCacNhom(duLieu) : null}
          hienTong={false}
          loi={loi}
        />
      }
    >
      <PendingList danhSach={duLieu ? gopDanhSachViec(duLieu) : null} loi={loi} />

      <SummaryCard
        tieuDe={ky ? `Kỳ nhuận bút ${ky.periodMonth}/${ky.periodYear}` : 'Kỳ nhuận bút'}
        nhan={ky ? NHAN_TRANG_THAI_KY[ky.status] : null}
        dangTai={!duLieu}
        loi={loi}
        cacDong={ky ? [
          { nhan: 'Bài đã chấm', giaTri: ky.gradedCount },
          { nhan: 'Bài chưa chấm', giaTri: duLieu.counts.pendingGrading, canhBao: duLieu.counts.pendingGrading > 0 },
          { nhan: 'Tổng tiền tạm tính', giaTri: formatMoney(ky.totalAmount) },
        ] : []}
        lienKet={ky ? { to: `/royalty-periods/${ky.periodId}`, chu: 'Xem bảng kê' } : null}
      />
    </HomeFrame>
  )
}
