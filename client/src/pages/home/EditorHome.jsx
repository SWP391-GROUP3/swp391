import { useEffect, useState } from 'react'
import { layTrangChuBienTapVien } from '../../api/editorHomeApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import HomeFrame from '../../components/common/HomeFrame.jsx'
import StatusCounter from '../../components/common/StatusCounter.jsx'
import PendingList from '../../components/common/PendingList.jsx'
import SummaryCard from '../../components/common/SummaryCard.jsx'

/**
 * Gop trang thai cac bai DUOC GIAO CHO BTV (articles.status) thanh 5 nhom.
 * "Đã công bố điểm" gom ca bai da dang (PUBLISHED) vi voi BTV viec da xong.
 */
const NHOM_TRANG_THAI = [
  { nhan: 'Chờ chấm',             mau: 'tim',        cacTrangThai: ['ASSIGNED'],                     duongDan: '/assigned-articles' },
  { nhan: 'Chờ Trưởng ban duyệt', mau: 'cam',        cacTrangThai: ['PENDING_APPROVAL'],             duongDan: '/my-scorings?status=PENDING_APPROVAL' },
  { nhan: 'Đã trả PV sửa',        mau: 'hong',       cacTrangThai: ['REVISION_REQUESTED'],           duongDan: '/my-scorings?status=REVISION_REQUESTED' },
  { nhan: 'Đã công bố điểm',      mau: 'xanh-la',    cacTrangThai: ['SCORE_PUBLISHED', 'PUBLISHED'], duongDan: '/my-scorings?status=SCORE_PUBLISHED,PUBLISHED' },
  { nhan: 'Từ chối',              mau: 'xam-dam',    cacTrangThai: ['REJECTED'],                     duongDan: '/my-scorings?status=REJECTED' },
]

/** Doi { status, count } server tra ve thanh cac nhom cho StatusCounter. */
function tinhSoBaiTheoNhom(statusCounts) {
  const ketQua = []
  for (const nhom of NHOM_TRANG_THAI) {
    let soBai = 0
    for (const dong of statusCounts) {
      if (nhom.cacTrangThai.includes(dong.status)) {
        soBai += dong.count
      }
    }
    ketQua.push({ nhan: nhom.nhan, mau: nhom.mau, so: soBai, duongDan: nhom.duongDan })
  }
  return ketQua
}

/**
 * Gop cac loai viec thanh 1 danh sach "Chờ xử lý".
 * Thu tu: bi Truong ban tra cham lai (gap nhat), cham lai do khieu nai,
 * bai moi giao, cuoi cung la cham cheo (khong anh huong diem chinh thuc).
 */
function gopDanhSachViec(duLieu) {
  const danhSach = []

  // UC-32: Truong ban khong duyet diem, tra lai kem ly do
  for (const bai of duLieu.rescoreRequests) {
    danhSach.push({
      khoa: `cham-lai-${bai.articleId}`,
      loai: 'Trưởng ban trả chấm lại',
      mau: 'hong',
      tieuDe: bai.title,
      moTa: `“${bai.reason}” · ${formatDate(bai.returnedAt)}`,
      hanChot: null,
      duongDan: `/assigned-articles/${bai.articleId}`,
      nut: 'Chấm lại',
    })
  }

  // UC-36, 37: cham mu - khong duoc xem diem va nhan xet cu
  for (const phieu of duLieu.reReviewTasks) {
    danhSach.push({
      khoa: `khieu-nai-${phieu.taskId}`,
      loai: 'Chấm lại do khiếu nại',
      mau: 'cam',
      tieuDe: phieu.title,
      moTa: `Chấm mù, không xem điểm cũ · giao ${formatDate(phieu.assignedAt)}`,
      hanChot: null,
      duongDan: `/re-reviews/${phieu.taskId}`,
      nut: 'Mở phiếu',
    })
  }

  // UC-21, 22: bai moi giao chua cham
  for (const bai of duLieu.assignedArticles) {
    danhSach.push({
      khoa: `giao-${bai.articleId}`,
      loai: 'Chờ chấm',
      mau: 'tim',
      tieuDe: bai.title,
      moTa: `Thể loại ${bai.categoryName} · giao ${formatDate(bai.assignedAt)}`,
      hanChot: null,
      duongDan: `/assigned-articles/${bai.articleId}`,
      nut: 'Chấm bài',
    })
  }

  // UC-40: cham cheo chi de do do lech, khong doi diem cua bai
  for (const phieu of duLieu.crossReviewTasks) {
    danhSach.push({
      khoa: `cham-cheo-${phieu.taskId}`,
      loai: 'Chấm chéo',
      mau: 'xanh-duong',
      tieuDe: phieu.title,
      moTa: 'Không ảnh hưởng điểm chính thức',
      hanChot: null,
      duongDan: `/cross-review-tasks/${phieu.taskId}`,
      nut: 'Mở phiếu',
    })
  }

  return danhSach
}

/**
 * Trang chu cua Bien tap vien.
 * Cot trai: viec cho xu ly + thong ke thang. Cot phai: bai duoc giao theo trang thai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   14  NHOM_TRANG_THAI                 Gop trang thai cac bai DUOC GIAO CHO BTV (articles.status) thanh 5 nhom
 *   23  tinhSoBaiTheoNhom()             Doi { status, count } server tra ve thanh cac nhom cho StatusCounter
 *   42  gopDanhSachViec()               Gop cac loai viec thanh 1 danh sach "Cho xu ly"
 *  115  EditorHome()                    xem tom tat dau file
 *  119    useEffect                     tai du lieu khi mo trang
 */
export default function EditorHome({ nguoiDung }) {
  const [duLieu, setDuLieu] = useState(null)   // null = dang tai
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true

    layTrangChuBienTapVien()
      .then((ketQua) => { if (conHienThi) setDuLieu(ketQua) })
      .catch(() => { if (conHienThi) setLoi('Không tải được dữ liệu trang chủ.') })

    return () => { conHienThi = false }
  }, [])

  const thang = duLieu?.thisMonth

  return (
    <HomeFrame
      nguoiDung={nguoiDung}
      cotPhai={
        <StatusCounter
          tieuDe="Bài được giao"
          cacNhom={duLieu ? tinhSoBaiTheoNhom(duLieu.statusCounts) : null}
          loi={loi}
        />
      }
    >
      <PendingList danhSach={duLieu ? gopDanhSachViec(duLieu) : null} loi={loi} />

      <SummaryCard
        tieuDe="Tháng này"
        nhan={null}
        dangTai={!duLieu}
        loi={loi}
        cacDong={thang ? [
          { nhan: 'Bài đã chấm xong', giaTri: thang.scoredCount },
          { nhan: 'Điểm trung bình đã cho', giaTri: thang.avgScore == null ? '—' : formatNumber(thang.avgScore) },
        ] : []}
        lienKet={{ to: '/my-scorings', chu: 'Xem lịch sử chấm' }}
      />
    </HomeFrame>
  )
}
