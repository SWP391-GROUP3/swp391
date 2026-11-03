import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { layLichSuChamCuaToi } from '../../api/assessmentApi.js'
import { STATUS_COLORS, statusLabel } from '../../utils/articleLabels.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import Icon from '../../components/common/Icon.jsx'
import { BT_BAI_DUOC_GIAO } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

/**
 * Man "Lich su cham" cua BTV (M-A7): moi bai da duoc giao cho BTV dang dang nhap kem lan cham moi nhat.
 * Loc theo trang thai bai bang ?status=A,B (link tu cac o dem tren trang chu BTV).
 *
 * MO TRANG NAY TAI: /my-scorings
 *
 * MUC LUC (so dong - ten - tac dung)
 *   27  QUYET_DINH                      Ten tieng Viet cua scoring_attempts.decision
 *   33  MyScorings()                    my scorings
 *   40    useEffect                     tai du lieu khi mo trang
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   61  link [Bo loc]                   mo trang /my-scorings (MyScorings)
 *  102  link [Mo phieu]                 mo trang /assigned-articles/{bai.articleId} (ScoringSheet)
 */
// Ten tieng Viet cua scoring_attempts.decision
const QUYET_DINH = {
  ACCEPT: 'Đạt',
  REVISION: 'Yêu cầu sửa',
  REJECT: 'Từ chối',
}

export default function MyScorings() {
  const [searchParams] = useSearchParams()
  const boLoc = searchParams.get('status')
  const cacTrangThai = boLoc ? boLoc.split(',') : []
  const [danhSach, setDanhSach] = useState(null)   // null = dang tai
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true
    layLichSuChamCuaToi()
      .then((data) => { if (conHienThi) setDanhSach(data || []) })
      .catch((err) => { if (conHienThi) setLoi(err.message) })
    return () => { conHienThi = false }
  }, [])

  const hienThi = (danhSach || []).filter(
    (bai) => cacTrangThai.length === 0 || cacTrangThai.includes(bai.articleStatus)
  )

  return (
    <div className="bf02__the">
      <h3 className="bf02__tieu-de"><Icon>{BT_BAI_DUOC_GIAO}</Icon>Lịch sử chấm</h3>
      <p className="bf02__mo-ta">
        Các bài Trưởng ban đã giao cho bạn và kết quả lần chấm mới nhất của từng bài.
      </p>

      {cacTrangThai.length > 0 && (
        <p className="bf02__nhat">
          Đang lọc: {cacTrangThai.map(statusLabel).join(', ')} · <Link to="/my-scorings">Bỏ lọc</Link>
        </p>
      )}

      {loi && <div className="thong-bao-loi" role="alert">{loi}</div>}
      {danhSach === null && !loi && <p className="bf02__nhat">Đang tải…</p>}
      {danhSach !== null && hienThi.length === 0 && (
        <div className="bf02__trong">Chưa có bài nào.</div>
      )}

      {hienThi.length > 0 && (
        <div className="bf02__cuon">
          <table className="bang-fm">
            <thead>
              <tr>
                <th>Mã bài</th>
                <th>Tiêu đề</th>
                <th>Thể loại</th>
                <th>Trạng thái bài</th>
                <th>Lần chấm</th>
                <th>Tổng điểm</th>
                <th>Quyết định</th>
                <th>Ngày nộp phiếu</th>
                <th className="bf02__giua">Phiếu chấm</th>
              </tr>
            </thead>
            <tbody>
              {hienThi.map((bai) => (
                <tr key={bai.articleId}>
                  <td className="bf02__dam">#{bai.articleId}</td>
                  <td>{bai.title}</td>
                  <td>{bai.categoryName}</td>
                  <td>
                    <span className={`cham cham--${STATUS_COLORS[bai.articleStatus]}`} aria-hidden="true" />{' '}
                    {statusLabel(bai.articleStatus)}
                  </td>
                  <td>{bai.attemptNo ? `Lần ${bai.attemptNo}${bai.attemptType === 'RESCORE' ? ' (chấm lại)' : ''}` : '—'}</td>
                  <td>{bai.totalScore == null ? '—' : formatNumber(bai.totalScore)}</td>
                  <td>{QUYET_DINH[bai.decision] || '—'}</td>
                  <td className="bf02__nhat">{bai.submittedAt ? formatDate(bai.submittedAt) : 'Chưa nộp'}</td>
                  <td className="bf02__giua">
                    <Link to={`/assigned-articles/${bai.articleId}`} className="nut-nhe">Mở phiếu</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
