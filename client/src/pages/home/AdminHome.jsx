import { useEffect, useState } from 'react'
import { layTrangChuAdmin } from '../../api/adminHomeApi.js'
import { getActiveConfig } from '../../api/appealRuleConfigApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import HomeFrame from '../../components/common/HomeFrame.jsx'
import StatusCounter from '../../components/common/StatusCounter.jsx'
import PendingList from '../../components/common/PendingList.jsx'
import SummaryCard from '../../components/common/SummaryCard.jsx'

/**
 * Gop viec cua Admin thanh 1 danh sach "Chờ xử lý". Thu tu: the loai chua co
 * bo tieu chi (dang chan BTV cham bai), bo tieu chi nhap, roi nhac nhap CSV.
 */
function gopDanhSachViec(duLieu) {
  const danhSach = []

  // UC-15, 17: the loai chua co bo tieu chi ACTIVE -> BTV mo phieu cham (UC-22) se khong co tieu chi
  for (const theLoai of duLieu.genresWithoutCriteria) {
    danhSach.push({
      khoa: `the-loai-${theLoai.genreId}`,
      loai: 'Thể loại chưa có bộ tiêu chí',
      mau: 'hong',
      tieuDe: `Thể loại ${theLoai.name}`,
      moTa: 'Biên tập viên chưa chấm được bài thuộc thể loại này',
      hanChot: null,
      duongDan: `/criteria-sets?genreId=${theLoai.genreId}`,
      nut: 'Tạo bộ tiêu chí',
    })
  }

  // UC-17: ban DRAFT chua kich hoat thi chua duoc dung de cham
  for (const boTieuChi of duLieu.draftCriteriaSets) {
    danhSach.push({
      khoa: `bo-tieu-chi-${boTieuChi.versionId}`,
      loai: 'Bộ tiêu chí chưa kích hoạt',
      mau: 'cam',
      tieuDe: `${boTieuChi.genreName} · phiên bản ${boTieuChi.versionNo}`,
      moTa: `Tạo ${formatDate(boTieuChi.createdAt)} · cần kích hoạt mới dùng để chấm`,
      hanChot: null,
      duongDan: `/criteria-sets/${boTieuChi.versionId}`,
      nut: 'Xem & kích hoạt',
    })
  }

  // UC-63: so lieu doc gia nhap hang ngay, dung cho KPI va bao cao (BF-05)
  const nhap = duLieu.readerImport
  if (!nhap.importedToday) {
    danhSach.push({
      khoa: 'nhap-csv',
      loai: 'Số liệu độc giả',
      mau: 'xanh-duong',
      tieuDe: 'Chưa nhập số liệu độc giả hôm nay',
      moTa: `Lần gần nhất ${formatDate(nhap.lastImportedAt)} · ${nhap.successRecords}/${nhap.totalRecords} dòng hợp lệ`,
      hanChot: null,
      duongDan: '/reader-imports',
      nut: 'Nhập CSV',
    })
  }

  return danhSach
}

/** Cac o dem ben phai. Khac don vi (tai khoan / the loai / bo tieu chi) nen khong hien tong. */
function taoCacNhom(dem) {
  return [
    { nhan: 'Tài khoản hoạt động', mau: 'xanh-la',    so: dem.activeUsers,       duongDan: '/users?status=ACTIVE' },
    { nhan: 'Tài khoản bị khoá',   mau: 'xam-dam',    so: dem.lockedUsers,       duongDan: '/users?status=LOCKED' },
    { nhan: 'Thể loại đang dùng',  mau: 'xanh-duong', so: dem.activeGenres,      duongDan: '/genres' },
    { nhan: 'Bộ tiêu chí nháp',    mau: 'cam',        so: dem.draftCriteriaSets, duongDan: '/criteria-sets?status=DRAFT' },
  ]
}

/**
 * Trang chu cua Admin.
 * Cot trai: viec cho xu ly + quy tac khieu nai dang ap dung (API THAT, BF-03).
 * Cot phai: so lieu he thong.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   15  gopDanhSachViec()               Gop viec cua Admin thanh 1 danh sach "Cho xu ly"
 *   65  taoCacNhom()                    Cac o dem ben phai. Khac don vi (tai khoan / the loai / bo tieu chi) nen khong hien tong.
 *   85  AdminHome()                     xem tom tat dau file
 *   91    useEffect                     tai du lieu khi mo trang
 */
export default function AdminHome({ nguoiDung }) {
  const [duLieu, setDuLieu] = useState(null)         // null = dang tai
  const [loi, setLoi] = useState('')
  const [quyTac, setQuyTac] = useState(null)         // null = dang tai
  const [loiQuyTac, setLoiQuyTac] = useState('')

  useEffect(() => {
    let conHienThi = true

    layTrangChuAdmin()
      .then((ketQua) => { if (conHienThi) setDuLieu(ketQua) })
      .catch(() => { if (conHienThi) setLoi('Không tải được dữ liệu trang chủ.') })

    // Quy tac khieu nai: API that (GET /api/appeal-rule-configs/active, UC-44)
    getActiveConfig()
      .then((ketQua) => { if (conHienThi) setQuyTac(ketQua) })
      .catch((err) => { if (conHienThi) setLoiQuyTac(err.message) })

    return () => { conHienThi = false }
  }, [])

  return (
    <HomeFrame
      nguoiDung={nguoiDung}
      goiCaTen
      cotPhai={
        <StatusCounter
          tieuDe="Hệ thống"
          cacNhom={duLieu ? taoCacNhom(duLieu.counts) : null}
          hienTong={false}
          loi={loi}
        />
      }
    >
      <PendingList danhSach={duLieu ? gopDanhSachViec(duLieu) : null} loi={loi} />

      <SummaryCard
        tieuDe="Quy tắc khiếu nại"
        nhan={quyTac ? { chu: 'Đang áp dụng', kieu: 'tot' } : null}
        dangTai={!quyTac}
        loi={loiQuyTac}
        cacDong={quyTac ? [
          { nhan: 'Thời hạn khiếu nại', giaTri: `${quyTac.appealWindowDays} ngày` },
          { nhan: 'Số lần tối đa mỗi bài', giaTri: quyTac.maxAppealsPerArticle },
          { nhan: 'Ngưỡng lệch điểm', giaTri: formatNumber(quyTac.varianceThreshold) },
          { nhan: 'Áp dụng từ', giaTri: formatDate(quyTac.effectiveFrom) },
        ] : []}
        lienKet={{ to: '/appeal-rules', chu: 'Cấu hình khiếu nại' }}
      />
    </HomeFrame>
  )
}
