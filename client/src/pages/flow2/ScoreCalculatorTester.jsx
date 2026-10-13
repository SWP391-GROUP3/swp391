import { useState } from 'react'

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
    <div style={{ padding: '20px', background: '#fff', borderRadius: '8px', border: '1px solid #dfe3e8', marginBottom: '24px' }}>
      <h3 style={{ marginTop: 0, color: '#1b2430' }}>🧮 Trình mô phỏng Tính điểm Chất lượng (Score Calculator & BR-QA-08)</h3>
      <p style={{ color: '#64748b', fontSize: '14px' }}>
        Giao diện thử nghiệm công thức tính tổng điểm có trọng số và kiểm tra quy tắc nghiệp vụ <strong>BR-QA-08</strong>: 
        <em> "Bất kỳ tiêu chí nào chấm dưới 50 điểm bắt buộc phải nhập nhận xét lý do"</em>.
      </p>

      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px', marginBottom: '16px' }}>
        <thead>
          <tr style={{ background: '#f8fafc', textAlign: 'left' }}>
            <th style={{ padding: '8px', border: '1px solid #cbd5e1' }}>Tiêu chí</th>
            <th style={{ padding: '8px', border: '1px solid #cbd5e1', width: '100px', textAlign: 'center' }}>Trọng số</th>
            <th style={{ padding: '8px', border: '1px solid #cbd5e1', width: '120px', textAlign: 'center' }}>Điểm (0-100)</th>
            <th style={{ padding: '8px', border: '1px solid #cbd5e1' }}>Nhận xét / Góp ý</th>
          </tr>
        </thead>
        <tbody>
          {items.map((it, idx) => {
            const isUnder50 = Number(it.score) < 50
            const isMissingComment = isUnder50 && (!it.comment || it.comment.trim() === '')
            return (
              <tr key={it.id} style={{ background: isMissingComment ? '#fff1f2' : 'transparent' }}>
                <td style={{ padding: '8px', border: '1px solid #cbd5e1', fontWeight: '500' }}>{it.name}</td>
                <td style={{ padding: '8px', border: '1px solid #cbd5e1', textAlign: 'center', color: '#047857', fontWeight: '600' }}>
                  {it.weight}%
                </td>
                <td style={{ padding: '8px', border: '1px solid #cbd5e1', textAlign: 'center' }}>
                  <input
                    type="number"
                    min={0}
                    max={100}
                    value={it.score}
                    onChange={(e) => handleScoreChange(idx, e.target.value)}
                    style={{
                      width: '70px',
                      padding: '6px',
                      textAlign: 'center',
                      borderRadius: '4px',
                      border: isUnder50 ? '2px solid #e11d48' : '1px solid #cbd5e1',
                      fontWeight: 'bold',
                      color: isUnder50 ? '#e11d48' : '#1b2430'
                    }}
                  />
                </td>
                <td style={{ padding: '8px', border: '1px solid #cbd5e1' }}>
                  <input
                    type="text"
                    placeholder={isUnder50 ? '⚠️ Bắt buộc nhận xét khi điểm < 50...' : 'Nhận xét thêm (không bắt buộc)...'}
                    value={it.comment}
                    onChange={(e) => handleCommentChange(idx, e.target.value)}
                    style={{
                      width: '100%',
                      padding: '6px 8px',
                      borderRadius: '4px',
                      border: isMissingComment ? '1px solid #e11d48' : '1px solid #cbd5e1',
                      background: isMissingComment ? '#fff' : '#f8fafc',
                      boxSizing: 'border-box'
                    }}
                  />
                  {isMissingComment && (
                    <span style={{ color: '#e11d48', fontSize: '12px', display: 'block', marginTop: '2px' }}>
                      * Vi phạm BR-QA-08: Tiêu chí dưới 50 điểm bắt buộc phải có lý do!
                    </span>
                  )}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>

      {/* Khung tổng kết điểm */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px', background: '#f1f5f9', borderRadius: '6px' }}>
        <div>
          <span style={{ fontSize: '15px', fontWeight: '600', marginRight: '10px' }}>Tổng điểm có trọng số:</span>
          <span style={{ fontSize: '22px', fontWeight: 'bold', color: '#14532d' }}>{totalScore} / 100</span>
        </div>

        <div>
          {hasInvalidComment ? (
            <span style={{ padding: '6px 12px', background: '#ffe4e6', color: '#be123c', borderRadius: '4px', fontWeight: '600', fontSize: '13px' }}>
              ⛔ Không đủ điều kiện nộp: Có tiêu chí dưới 50 điểm chưa ghi nhận xét!
            </span>
          ) : (
            <span style={{ padding: '6px 12px', background: '#dcfce7', color: '#15803d', borderRadius: '4px', fontWeight: '600', fontSize: '13px' }}>
              ✓ Đầy đủ điều kiện nộp phiếu chấm
            </span>
          )}
        </div>
      </div>
    </div>
  )
}
