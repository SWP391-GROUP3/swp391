import { useState, useEffect } from 'react'

export default function EditorTaskQueue({ nguoiDung, onSelectTaskToScore }) {
  // Lấy ID BTV: Nếu đang đăng nhập thì lấy ID (ví dụ cuong.lh = 3), hoặc mặc định là 3
  const [editorId, setEditorId] = useState(nguoiDung?.id || 3)
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  useEffect(() => {
    loadEditorTasks()
  }, [editorId])

  async function loadEditorTasks() {
    setLoading(true)
    setMessage('')
    try {
      const res = await fetch(`/api/assignments/editor/${editorId}`)
      if (res.ok) {
        const data = await res.json()
        setTasks(data)
      } else {
        setTasks([])
      }
    } catch (err) {
      setMessage('Không thể kết nối máy chủ: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ padding: '20px', background: '#fff', borderRadius: '8px', border: '1px solid #dfe3e8', marginBottom: '24px' }}>
      {/* UC-21: Hàng việc của Biên tập viên */}
      <h3 style={{ marginTop: 0, color: '#1b2430' }}>✍️ Hàng việc của Biên tập viên</h3>
      <p style={{ color: '#64748b', fontSize: '14px' }}>
        Danh sách tất cả các bài viết được Trưởng ban phân công cho BTV này. BTV chọn bài để tiến hành đọc bài và chấm điểm chất lượng.
      </p>

      {/* Bộ lọc xem theo BTV */}
      <div style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '12px' }}>
        <label style={{ fontWeight: '600', fontSize: '14px' }}>Mã BTV cần xem việc:</label>
        <input
          type="number"
          value={editorId}
          onChange={(e) => setEditorId(Number(e.target.value))}
          style={{ width: '80px', padding: '6px 10px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
        />
        <button
          type="button"
          onClick={loadEditorTasks}
          style={{ padding: '6px 14px', background: '#f4f6f8', border: '1px solid #cbd5e1', borderRadius: '6px', cursor: 'pointer' }}
        >
          Làm mới
        </button>
      </div>

      {message && <div style={{ color: '#b91c1c', marginBottom: '12px' }}>{message}</div>}

      {loading ? (
        <p style={{ color: '#64748b' }}>Đang tải danh sách bài được giao...</p>
      ) : tasks.length === 0 ? (
        <div style={{ padding: '24px', textAlign: 'center', background: '#f8fafc', borderRadius: '6px', color: '#64748b' }}>
          Hiện tại BTV #{editorId} chưa được phân công bài viết nào. (Hãy qua tab "Phân công bài viết" để giao việc thử).
        </div>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
          <thead>
            <tr style={{ background: '#f8fafc', textAlign: 'left' }}>
              <th style={{ padding: '8px', border: '1px solid #e2e8f0', width: '60px' }}>STT</th>
              <th style={{ padding: '8px', border: '1px solid #e2e8f0', width: '100px' }}>Mã bài</th>
              <th style={{ padding: '8px', border: '1px solid #e2e8f0' }}>Thời điểm giao</th>
              <th style={{ padding: '8px', border: '1px solid #e2e8f0' }}>Gợi ý bởi HT?</th>
              <th style={{ padding: '8px', border: '1px solid #e2e8f0', width: '140px', textAlign: 'center' }}>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {tasks.map((task, idx) => (
              <tr key={task.id}>
                <td style={{ padding: '8px', border: '1px solid #e2e8f0', textAlign: 'center' }}>{idx + 1}</td>
                <td style={{ padding: '8px', border: '1px solid #e2e8f0', fontWeight: 'bold' }}>#{task.articleId}</td>
                <td style={{ padding: '8px', border: '1px solid #e2e8f0' }}>{new Date(task.assignedAt).toLocaleString('vi-VN')}</td>
                <td style={{ padding: '8px', border: '1px solid #e2e8f0' }}>
                  {task.wasSuggested ? (
                    <span style={{ color: '#047857', fontWeight: '500' }}>✓ Đúng (Thuật toán)</span>
                  ) : (
                    <span style={{ color: '#64748b' }}>Trưởng ban chọn tay</span>
                  )}
                </td>
                <td style={{ padding: '8px', border: '1px solid #e2e8f0', textAlign: 'center' }}>
                  <button
                    type="button"
                    onClick={() => {
                      if (onSelectTaskToScore) onSelectTaskToScore(task)
                      else alert(`Mở bài #${task.articleId} để chấm điểm (Sẵn sàng tích hợp sang Iteration 2)!`)
                    }}
                    style={{
                      padding: '5px 12px',
                      background: '#14532d',
                      color: '#fff',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '13px'
                    }}
                  >
                    Bắt đầu Chấm bài
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
