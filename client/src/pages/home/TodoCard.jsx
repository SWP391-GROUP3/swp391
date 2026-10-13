import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { layViecCanXuLy } from '../../api/reporterHomeApi.js'
import { formatDate, daysUntil } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import './TodoCard.css'

/** Con <= so ngay nay thi to do han chot cho PV de y. */
const NGAY_GAP = 1

/**
 * Chu hien han chot. daysUntil lam tron len nen:
 * <= 0 -> da qua han; 1 -> con chua toi 24 tieng; n -> con n ngay.
 */
function chuHanChot(hanChot) {
  const soNgay = daysUntil(hanChot)
  if (soNgay <= 0) {
    return 'đã quá hạn'
  }
  if (soNgay === 1) {
    return 'còn dưới 1 ngày'
  }
  return `còn ${soNgay} ngày`
}

/**
 * Gop 3 loai viec server tra ve thanh 1 danh sach de ve chung 1 kieu.
 * Thu tu: loi moi dong tac gia truoc (han 3 ngay, gap nhat), roi bai bi tra
 * ve, cuoi cung la bai con han khieu nai.
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

/** Khoi "Can ban xu ly" o cot trai trang chu Phong vien. */
export default function TodoCard() {
  const [danhSach, setDanhSach] = useState(null)   // null = dang tai
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true

    layViecCanXuLy()
      .then((duLieu) => { if (conHienThi) setDanhSach(gopDanhSachViec(duLieu)) })
      .catch(() => { if (conHienThi) setLoi('Không tải được danh sách việc cần xử lý.') })

    return () => { conHienThi = false }
  }, [])

  return (
    <section className="trang-chu-pv__khoi" aria-labelledby="tieu-de-viec">
      <div className="trang-chu-pv__khoi-dau">
        <h2 id="tieu-de-viec" className="trang-chu-pv__khoi-tieu-de">Cần bạn xử lý</h2>
        {danhSach && danhSach.length > 0 && (
          <span className="viec-pv__huy-hieu">
            {danhSach.length}<span className="an-di"> việc</span>
          </span>
        )}
      </div>

      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {!danhSach && !loi && <p className="dang-tai">Đang tải…</p>}

      {danhSach && danhSach.length === 0 && (
        <p className="trong-rong">Bạn không có việc nào cần xử lý 🎉</p>
      )}

      {danhSach && danhSach.length > 0 && (
        <ul className="viec-pv__ds">
          {danhSach.map((viec) => (
            <li key={viec.khoa} className="viec-pv__muc">
              <div className="viec-pv__noi-dung">
                <p className="viec-pv__loai">
                  <span className={`cham cham--${viec.mau}`} aria-hidden="true" />
                  {viec.loai}
                  {viec.hanChot && (
                    <span className={daysUntil(viec.hanChot) <= NGAY_GAP ? 'viec-pv__han viec-pv__han--gap' : 'viec-pv__han'}>
                      · {chuHanChot(viec.hanChot)}
                    </span>
                  )}
                </p>
                <p className="viec-pv__tieu-de">{viec.tieuDe}</p>
                <p className="viec-pv__mo-ta">{viec.moTa}</p>
              </div>
              <Link to={viec.duongDan} className="nut-nhe">{viec.nut}</Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
