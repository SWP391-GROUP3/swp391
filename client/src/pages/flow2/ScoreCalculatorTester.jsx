import { useState } from 'react'
import Icon from '../../components/common/Icon.jsx'
import { BT_MAY_TINH, BT_DAU_TICH, BT_DAU_X } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

export default function ScoreCalculatorTester() {
  const [items, setItems] = useState([
    { id: 1, name: 'Tính thời sự & thông tin mới', weight: 30, score: 85, comment: 'Bài viết cập nhật số liệu mới nhất trong ngày' },
    { id: 2, name: 'Độ chính xác & nguồn tin kiểm chứng', weight: 40, score: 90, comment: 'Có đối chiếu đầy đủ báo cáo chính thức' },
    { id: 3, name: 'Văn phong & chuẩn mực đạo đức', weight: 30, score: 45, comment: '' } // cố tình để điểm < 50 và comment trống
  ])

  // Tính tổng điểm real-time
  const totalScore = items.reduce((acc, it) => {
    const s = Number(it.score) || 0
    const w = Number(it.weight) || 0
    return acc + (s * w / 100)
  }, 0).toFixed(2)

  // Kiểm tra BR-QA-08: Nếu điểm < 50 bắt buộc phải có comment
  const hasInvalidComment = items.some(it => Number(it.score) < 50 && (!it.comment || it.comment.trim() === ''))

  function handleScoreChange(idx, val) {
    const updated = [...items]
    updated[idx].score = Math.max(0, Math.min(100, Number(val)))
    setItems(updated)
  }

  function handleCommentChange(idx, val) {
    const updated = [...items]
    updated[idx].comment = val
    setItems(updated)
  }

  return (
    <div className="bf02__the">
      {/* Trình mô phỏng tính điểm có trọng số - BR-QA-08 */}
      <h3 className="bf02__tieu-de"><Icon>{BT_MAY_TINH}</Icon>Trình mô phỏng Tính điểm Chất lượng</h3>
      <p className="bf02__mo-ta">
        Giao diện thử nghiệm công thức tính tổng điểm có trọng số:
        <em> "Bất kỳ tiêu chí nào chấm dưới 50 điểm bắt buộc phải nhập nhận xét lý do"</em>.
      </p>

      <div className="bf02__cuon">
        <table className="bang-fm">
          <thead>
            <tr>
              <th>Tiêu chí</th>
              <th className="bf02__giua">Trọng số</th>
              <th className="bf02__giua">Điểm (0-100)</th>
              <th>Nhận xét / Góp ý</th>
            </tr>
          </thead>
          <tbody>
            {items.map((it, idx) => {
              const isUnder50 = Number(it.score) < 50
              const isMissingComment = isUnder50 && (!it.comment || it.comment.trim() === '')
              return (
                <tr key={it.id} className={isMissingComment ? 'bf02__dong--loi' : undefined}>
                  <td className="bf02__dam">{it.name}</td>
                  <td className="bf02__giua bf02__trong-so">{it.weight}%</td>
                  <td className="bf02__giua">
                    <input
                      type="number"
                      min={0}
                      max={100}
                      value={it.score}
                      onChange={(e) => handleScoreChange(idx, e.target.value)}
                      aria-label={'Điểm ' + it.name}
                      className={'bf02__o-nhap bf02__o-nhap--so bf02__o-nhap--diem' + (isUnder50 ? ' bf02__o-nhap--loi' : '')}
                    />
                  </td>
                  <td>
                    <input
                      type="text"
                      placeholder={isUnder50 ? 'Bắt buộc nhận xét khi điểm < 50...' : 'Nhận xét thêm (không bắt buộc)...'}
                      value={it.comment}
                      onChange={(e) => handleCommentChange(idx, e.target.value)}
                      aria-label={'Nhận xét ' + it.name}
                      className={'bf02__o-nhap' + (isMissingComment ? ' bf02__o-nhap--loi' : '')}
                    />
                    {isMissingComment && (
                      <span className="bf02__loi-nho">
                        * Tiêu chí dưới 50 điểm bắt buộc phải có nhận xét lý do!
                      </span>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* Khung tổng kết điểm */}
      <div className="bf02__tong-ket">
        <div>
          <span className="bf02__tong-nhan">Tổng điểm có trọng số:</span>
          <span className="bf02__tong-diem">{totalScore} / 100</span>
        </div>

        <div>
          {hasInvalidComment ? (
            <span className="nhan-tt nhan-tt--loi">
              <Icon>{BT_DAU_X}</Icon>
              Không đủ điều kiện nộp: Có tiêu chí dưới 50 điểm chưa ghi nhận xét!
            </span>
          ) : (
            <span className="nhan-tt nhan-tt--tot">
              <Icon>{BT_DAU_TICH}</Icon>
              Đầy đủ điều kiện nộp phiếu chấm
            </span>
          )}
        </div>
      </div>
    </div>
  )
}
