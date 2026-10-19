import { useEffect, useState } from 'react'
import { layTrangChuKeToan } from '../../api/accountantHomeApi.js'
import { getRoyaltyRates } from '../../api/royaltyApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatMoney } from '../../utils/formatMoney.js'
import HomeFrame from '../../components/common/HomeFrame.jsx'
import StatusCounter from '../../components/common/StatusCounter.jsx'
import PendingList from '../../components/common/PendingList.jsx'
import SummaryCard from '../../components/common/SummaryCard.jsx'

/** Nhom theo royalty_periods.status (BR-RY-07: OPEN -> CLOSED -> LOCKED -> PAID). */
const NHOM_TRANG_THAI_KY = [
  { nhan: 'Thư ký đang chấm', mau: 'xanh-duong', status: 'OPEN' },
  { nhan: 'Chờ duyệt',        mau: 'cam',        status: 'CLOSED' },
  { nhan: 'Chờ chi trả',      mau: 'tim',        status: 'LOCKED' },
  { nhan: 'Đã chi xong',      mau: 'xanh-la',    status: 'PAID' },
]

/** Doi { status, count } thanh cac nhom cho StatusCounter; status khong co dong nao = 0 ky. */
function tinhSoKyTheoNhom(periodStatusCounts) {
  const ketQua = []
  for (const nhom of NHOM_TRANG_THAI_KY) {
    let soKy = 0
    for (const dong of periodStatusCounts) {
      if (dong.status === nhom.status) {
        soKy = dong.count
      }
    }
    ketQua.push({ nhan: nhom.nhan, mau: nhom.mau, so: soKy, duongDan: `/royalty-periods?status=${nhom.status}` })
  }
  return ketQua
}

/** Gop viec cua Ke toan: ky cho duyet bang ke truoc, roi ky da khoa con nguoi chua chi. */
function gopDanhSachViec(duLieu) {
  const danhSach = []

  // UC-51: Thu ky da gui ky (CLOSED) -> Ke toan xem bang ke, khoa (UC-53) hoac tra ve (UC-52)
  for (const ky of duLieu.periodsToReview) {
    danhSach.push({
      khoa: `duyet-ky-${ky.periodId}`,
      loai: 'Kỳ chờ duyệt',
      mau: 'cam',
      tieuDe: `Kỳ nhuận bút ${ky.periodMonth}/${ky.periodYear}`,
      moTa: `Thư ký đã gửi · ${ky.personCount} người · ${formatMoney(ky.totalAmount)}`,
      hanChot: null,
      duongDan: `/royalty-periods/${ky.periodId}`,
      nut: 'Duyệt bảng kê',
    })
  }

  // UC-54: ky da khoa (LOCKED), ghi nhan chi cho tung nguoi; chi het thi ky thanh PAID
  for (const ky of duLieu.periodsToPay) {
    danhSach.push({
      khoa: `chi-ky-${ky.periodId}`,
      loai: 'Chờ chi trả',
      mau: 'tim',
      tieuDe: `Kỳ nhuận bút ${ky.periodMonth}/${ky.periodYear}`,
      moTa: `Đã chi ${ky.paidCount}/${ky.personCount} người · còn ${formatMoney(ky.unpaidAmount)}`,
      hanChot: null,
      duongDan: `/royalty-payments?periodId=${ky.periodId}`,
      nut: 'Ghi nhận chi',
    })
  }

  return danhSach
}

/** Ngay hom nay dang "2026-10-14" theo gio may (de so voi effectiveFrom cua don gia). */
function homNayDangChuoi() {
  const ngay = new Date()
  const thang = String(ngay.getMonth() + 1).padStart(2, '0')
  const ngayTrongThang = String(ngay.getDate()).padStart(2, '0')
  return `${ngay.getFullYear()}-${thang}-${ngayTrongThang}`
}

/**
 * Tu danh sach don gia (moi nhat truoc), tim:
 * - dangApDung: dong co effectiveFrom lon nhat ma <= hom nay (BR-RY-03)
 * - sapApDung:  dong co hieu luc trong tuong lai gan nhat (Ke toan da nhap truoc), co the null
 * Chuoi "YYYY-MM-DD" so sanh truc tiep duoc vi cung do dai, nam-thang-ngay.
 */
function timDonGia(danhSach) {
  const homNay = homNayDangChuoi()
  let dangApDung = null
  let sapApDung = null
  for (const donGia of danhSach) {
    if (donGia.effectiveFrom <= homNay) {
      dangApDung = donGia
      break
    }
    sapApDung = donGia   // moi nhat truoc -> dong tuong lai gap cuoi cung la dong gan nhat
  }
  return { dangApDung, sapApDung }
}

/** Cac dong cua the "Đơn giá nhuận bút". */
function taoDongDonGia({ dangApDung, sapApDung }) {
  const cacDong = []
  if (dangApDung) {
    cacDong.push({ nhan: 'Lương cơ sở', giaTri: formatMoney(dangApDung.baseSalary) })
    cacDong.push({ nhan: 'Đơn giá 1 hệ số', giaTri: formatMoney(dangApDung.amount) })
    cacDong.push({ nhan: 'Áp dụng từ', giaTri: formatDate(dangApDung.effectiveFrom) })
  } else {
    cacDong.push({ nhan: 'Đơn giá đang áp dụng', giaTri: 'Chưa có', canhBao: true })
  }
  if (sapApDung) {
    cacDong.push({ nhan: `Đơn giá mới từ ${formatDate(sapApDung.effectiveFrom)}`, giaTri: formatMoney(sapApDung.amount) })
  }
  return cacDong
}

/**
 * Trang chu cua Ke toan (BF-04).
 * Cot trai: viec cho xu ly + don gia dang ap dung (API THAT). Cot phai: so ky theo trang thai.
 *
 * MUC LUC (so dong)
 *   12  NHOM_TRANG_THAI_KY           - Nhom theo royalty_periods.status (BR-RY-07: OPEN -> CLOSED -> LOCKED ->…
 *   20  tinhSoKyTheoNhom             - Doi { status, count } thanh cac nhom cho StatusCounter; status khong co…
 *   35  gopDanhSachViec              - Gop viec cua Ke toan: ky cho duyet bang ke truoc, roi ky da khoa con ng…
 *   70  homNayDangChuoi              - Ngay hom nay dang "2026-10-14" theo gio may (de so voi effectiveFrom cu…
 *   83  timDonGia                    - Tu danh sach don gia (moi nhat truoc), tim:
 *   98  taoDongDonGia                - Cac dong cua the "Đơn giá nhuận bút".
 *  127  AccountantHome               - Trang chu cua Ke toan (BF-04).
 *  133    useEffect                  - tai du lieu khi mo trang
 */
export default function AccountantHome({ nguoiDung }) {
  const [duLieu, setDuLieu] = useState(null)       // null = dang tai
  const [loi, setLoi] = useState('')
  const [donGia, setDonGia] = useState(null)       // { dangApDung, sapApDung }, null = dang tai
  const [loiDonGia, setLoiDonGia] = useState('')

  useEffect(() => {
    let conHienThi = true

    layTrangChuKeToan()
      .then((ketQua) => { if (conHienThi) setDuLieu(ketQua) })
      .catch(() => { if (conHienThi) setLoi('Không tải được dữ liệu trang chủ.') })

    // Don gia: API that cua BF-04 (GET /api/royalty-rates)
    getRoyaltyRates()
      .then((danhSach) => { if (conHienThi) setDonGia(timDonGia(danhSach)) })
      .catch((err) => { if (conHienThi) setLoiDonGia(err.message) })

    return () => { conHienThi = false }
  }, [])

  return (
    <HomeFrame
      nguoiDung={nguoiDung}
      cotPhai={
        <StatusCounter
          tieuDe={`Kỳ nhuận bút năm ${new Date().getFullYear()}`}
          cacNhom={duLieu ? tinhSoKyTheoNhom(duLieu.periodStatusCounts) : null}
          donVi="kỳ"
          loi={loi}
        />
      }
    >
      <PendingList danhSach={duLieu ? gopDanhSachViec(duLieu) : null} loi={loi} />

      <SummaryCard
        tieuDe="Đơn giá nhuận bút"
        nhan={donGia ? (donGia.dangApDung ? { chu: 'Đang áp dụng', kieu: 'tot' } : { chu: 'Chưa có', kieu: 'chu-y' }) : null}
        dangTai={!donGia}
        loi={loiDonGia}
        cacDong={donGia ? taoDongDonGia(donGia) : []}
        lienKet={{ to: '/royalty-rates', chu: 'Quản lý đơn giá' }}
      />
    </HomeFrame>
  )
}
