import { useEffect, useState } from 'react'
import { layViecCanXuLy } from '../../api/reporterHomeApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import PendingList from '../../components/common/PendingList.jsx'

/**
 * Gop 3 loai viec server tra ve thanh 1 danh sach de ve chung 1 kieu.
 * Thu tu: loi moi dong tac gia truoc (han 3 ngay, gap nhat), roi bai bi tra
 * ve, cuoi cung la bai con han khieu nai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  gopDanhSachViec()               xem tom tat dau file
 *   66  TodoCard()                      Khoi "Cho xu ly" o cot trai trang chu Phong vien: tai du lieu roi giao cho PendingList ve
 *   70    useEffect                     tai du lieu khi mo trang
 */
function gopDanhSachViec(duLieu) {
  const danhSach = []

  // UC-12: dong tac gia xac nhan / tu choi (BR-IN-06)
  for (const loiMoi of duLieu.coAuthorInvites) {
    danhSach.push({
      khoa: `dong-tac-gia-${loiMoi.articleId}`,
      loai: 'Mời làm đồng tác giả',
      mau: 'cam',
      tieuDe: loiMoi.title,
      moTa: `${loiMoi.mainAuthorName} mời bạn, tỷ lệ ${formatNumber(loiMoi.sharePercent)}%`,
      hanChot: loiMoi.confirmDeadline,
      duongDan: '/co-author-invites',
      nut: 'Xem & xác nhận',
    })
  }

  // Bai bi BTV tra ve, note la loi nhan xet trong article_status_logs
  for (const bai of duLieu.revisionRequests) {
    danhSach.push({
      khoa: `tra-ve-${bai.articleId}`,
      loai: 'Trả về cần sửa',
      mau: 'hong',
      tieuDe: bai.title,
      moTa: `“${bai.note}” · ${formatDate(bai.returnedAt)}`,
      hanChot: null,
      duongDan: `/my-articles/${bai.articleId}`,
      nut: 'Sửa bài',
    })
  }

  // UC-35: con trong cua so khieu nai 7 ngay (GBR-06)
  for (const bai of duLieu.appealableScores) {
    danhSach.push({
      khoa: `khieu-nai-${bai.articleId}`,
      loai: 'Đã có điểm',
      mau: 'xanh-duong',
      tieuDe: bai.title,
      moTa: `Điểm ${formatNumber(bai.totalScore)}/100 · khiếu nại được nếu chưa đồng ý`,
      hanChot: bai.appealDeadline,
      duongDan: `/my-appeals/new?articleId=${bai.articleId}`,
      nut: 'Khiếu nại',
    })
  }

  return danhSach
}

/** Khoi "Cho xu ly" o cot trai trang chu Phong vien: tai du lieu roi giao cho PendingList ve. */
export default function TodoCard() {
  const [danhSach, setDanhSach] = useState(null)   // null = dang tai
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true

    layViecCanXuLy()
      .then((duLieu) => { if (conHienThi) setDanhSach(gopDanhSachViec(duLieu)) })
      .catch(() => { if (conHienThi) setLoi('Không tải được danh sách việc chờ xử lý.') })

    return () => { conHienThi = false }
  }, [])

  // Phan ve dung chung voi trang chu cac vai tro khac
  return <PendingList danhSach={danhSach} loi={loi} />
}
