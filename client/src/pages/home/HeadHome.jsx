import { useEffect, useState } from 'react'
import { layTrangChuTruongBan } from '../../api/headHomeApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import HomeFrame from '../../components/common/HomeFrame.jsx'
import StatusCounter from '../../components/common/StatusCounter.jsx'
import PendingList from '../../components/common/PendingList.jsx'
import SummaryCard from '../../components/common/SummaryCard.jsx'

/**
 * Gop viec cua Truong ban thanh 1 danh sach "Chờ xử lý".
 * Thu tu: khieu nai cho xac nhan (dang treo diem cua PV), diem cho duyet,
 * cuoi cung la bai cho phan cong.
 */
function gopDanhSachViec(duLieu) {
  const danhSach = []

  // UC-37: lech diem vuot nguong -> BTV thu 3 cham (S2), Truong ban xac nhan
  for (const kn of duLieu.appealsToConfirm) {
    danhSach.push({
      khoa: `khieu-nai-${kn.appealId}`,
      loai: 'Khiếu nại chờ xác nhận',
      mau: 'hong',
      tieuDe: kn.title,
      moTa: `Điểm gốc ${formatNumber(kn.s0)} · chấm lại ${formatNumber(kn.s1)} · trọng tài ${formatNumber(kn.s2)}`,
      hanChot: null,
      duongDan: `/appeals/${kn.appealId}`,
      nut: 'Xác nhận',
    })
  }

  // UC-29…31: BTV da cham, cho Truong ban duyet diem
  for (const lan of duLieu.pendingApprovals) {
    danhSach.push({
      khoa: `duyet-${lan.scoringAttemptId}`,
      loai: 'Chờ duyệt điểm',
      mau: 'cam',
      tieuDe: lan.title,
      moTa: `BTV ${lan.editorName} · ${formatNumber(lan.totalScore)}/100 · gửi ${formatDate(lan.submittedAt)}`,
      hanChot: null,
      duongDan: `/score-approvals/${lan.scoringAttemptId}`,
      nut: 'Duyệt',
    })
  }

  // UC-18, 20: bai da nop, chua giao BTV
  for (const bai of duLieu.unassignedArticles) {
    danhSach.push({
      khoa: `phan-cong-${bai.articleId}`,
      loai: 'Chờ phân công',
      mau: 'xanh-duong',
      tieuDe: bai.title,
      moTa: `${bai.authorName} · nộp ${formatDate(bai.submittedAt)}`,
      hanChot: null,
      duongDan: '/assignments',
      nut: 'Phân công',
    })
  }

  return danhSach
}

/** Cac o dem ben phai. Khac don vi (bai / khieu nai) nen khong hien tong. */
function taoCacNhom(dem) {
  return [
    { nhan: 'Chờ phân công',     mau: 'xanh-duong', so: dem.submitted,       duongDan: '/assignments' },
    { nhan: 'Đang chấm',         mau: 'tim',        so: dem.assigned,        duongDan: '/assignments' },
    { nhan: 'Chờ duyệt điểm',    mau: 'cam',        so: dem.pendingApproval, duongDan: '/score-approvals' },
    { nhan: 'Chờ duyệt đăng',    mau: 'xanh-la',    so: dem.scorePublished,  duongDan: '/publishing' },
    { nhan: 'Khiếu nại đang mở', mau: 'hong',       so: dem.openAppeals,     duongDan: '/appeals' },
  ]
}

/**
 * Trang chu cua Truong ban bien tap.
 * Cot trai: viec cho xu ly + tom tat KPI thang cua ban. Cot phai: viec cua ban theo buoc.
 */
export default function HeadHome({ nguoiDung }) {
  const [duLieu, setDuLieu] = useState(null)   // null = dang tai
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true

    layTrangChuTruongBan()
      .then((ketQua) => { if (conHienThi) setDuLieu(ketQua) })
      .catch(() => { if (conHienThi) setLoi('Không tải được dữ liệu trang chủ.') })

    return () => { conHienThi = false }
  }, [])

  const kpi = duLieu?.kpiThisMonth

  return (
    <HomeFrame
      nguoiDung={nguoiDung}
      cotPhai={
        <StatusCounter
          tieuDe="Việc của ban"
          cacNhom={duLieu ? taoCacNhom(duLieu.counts) : null}
          hienTong={false}
          loi={loi}
        />
      }
    >
      <PendingList danhSach={duLieu ? gopDanhSachViec(duLieu) : null} loi={loi} />

      <SummaryCard
        tieuDe={kpi ? `KPI của ban tháng ${kpi.month}/${kpi.year}` : 'KPI của ban'}
        nhan={kpi ? (kpi.finalized ? { chu: 'Đã chốt', kieu: 'tot' } : { chu: 'Chưa chốt', kieu: 'chu-y' }) : null}
        dangTai={!duLieu}
        loi={loi}
        cacDong={kpi ? [
          { nhan: 'Phóng viên dự kiến đạt', giaTri: `${kpi.expectedPass} / ${kpi.reporterCount}` },
          { nhan: 'Dự kiến chưa đạt', giaTri: kpi.expectedFail, canhBao: kpi.expectedFail > 0 },
          { nhan: 'Chưa giao chỉ tiêu', giaTri: kpi.noTarget, canhBao: kpi.noTarget > 0 },
        ] : []}
        lienKet={{ to: '/kpi-board', chu: 'Xem bảng KPI' }}
      />
    </HomeFrame>
  )
}
