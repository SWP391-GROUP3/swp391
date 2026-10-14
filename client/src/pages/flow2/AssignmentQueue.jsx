import { useState, useEffect } from 'react'
import {
  layKhoiLuongCongViecBtv,
  layPhanCongBaiViet,
  phanCongBaiViet
} from '../../api/assessmentApi.js'

export default function AssignmentQueue({ nguoiDung }) {
  // Danh sách bài viết chờ phân công (SUBMITTED) khớp 100% với Database
  const [articles] = useState([
    { id: 4, title: 'Điều tra đường dây buôn lậu khoáng sản qua biên giới', author: 'Nguyễn Văn An', category: 'Phóng sự điều tra', categoryId: 6, submittedAt: '13/10/2026 08:30' },
    { id: 3, title: 'Giá xăng giảm lần thứ ba liên tiếp', author: 'Nguyễn Văn An', category: 'Tin sâu / Thời sự', categoryId: 2, submittedAt: '27/09/2026 08:00' },
    { id: 5, title: 'Bình luận: Cơ hội và thách thức của AI trong tác nghiệp phóng viên', author: 'Trần Thị Bình', category: 'Bình luận', categoryId: 5, submittedAt: '13/10/2026 10:00' }
  ])

  // Danh sách BTV để chọn giao việc (theo data.sql: cuong.lh id=3, giang.nt id=9)
  const editors = [
    { id: 3, name: 'Lê Hoàng Cường (Editor Thời sự)', username: 'cuong.lh' },
    { id: 9, name: 'Ngô Thu Giang (Editor Kinh tế)', username: 'giang.nt' },
    { id: 4, name: 'Phạm Thu Dung (Thư ký toà soạn)', username: 'dung.pt' }
  ]

  const [selectedArticleId, setSelectedArticleId] = useState(4)
  const [selectedEditorId, setSelectedEditorId] = useState(3)
  const [wasSuggested, setWasSuggested] = useState(false)
  const [workload, setWorkload] = useState(null)
  const [currentAssignment, setCurrentAssignment] = useState(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadWorkload(selectedEditorId)
  }, [selectedEditorId])

  useEffect(() => {
    loadCurrentAssignment(selectedArticleId)
  }, [selectedArticleId])

  async function loadWorkload(editorId) {
    try {
      const data = await layKhoiLuongCongViecBtv(editorId)
      setWorkload(data.activeArticleCount)
    } catch {
      // bỏ qua nếu lỗi
    }
  }

  async function loadCurrentAssignment(articleId) {
    setCurrentAssignment(null)
    try {
      const data = await layPhanCongBaiViet(articleId)
      setCurrentAssignment(data)
    } catch {
      // chưa có phân công
    }
  }

  async function handleAssign(e) {
    e.preventDefault()
    setMessage('')
    setError('')
    setLoading(true)

    try {
      const payload = {
        articleId: Number(selectedArticleId),
        editorId: Number(selectedEditorId),
        assignedBy: nguoiDung?.id || 5, // ID của Trưởng ban Vũ Minh Đức
        wasSuggested: wasSuggested
      }

      const data = await phanCongBaiViet(payload)
      setMessage(`Phân công thành công Bài #${data.articleId} cho BTV (ID: ${data.editorId})!`)
      setCurrentAssignment(data)
      loadWorkload(selectedEditorId)
    } catch (err) {
      setError('Giao bài thất bại: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  const selectedArticle = articles.find(a => a.id === Number(selectedArticleId))

  return (
    <div style={{ padding: '20px', background: '#fff', borderRadius: '8px', border: '1px solid #dfe3e8', marginBottom: '24px' }}>
      {/* UC-18, UC-20; BR-QA-04: Trưởng ban phân công bài viết */}
      <h3 style={{ marginTop: 0, color: '#1b2430' }}>📋 Hàng chờ phân công bài viết</h3>
      <p style={{ color: '#64748b', fontSize: '14px' }}>
        Trưởng ban chọn bài viết đã nộp và chỉ định một Biên tập viên chịu trách nhiệm chấm chất lượng (Mỗi bài chỉ có 1 BTV chính).
      </p>

      {/* Thông báo */}
      {message && <div style={{ padding: '10px 14px', background: '#ecfdf5', color: '#065f46', border: '1px solid #a7f3d0', borderRadius: '6px', marginBottom: '16px' }}>{message}</div>}
      {error && <div style={{ padding: '10px 14px', background: '#fef2f2', color: '#991b1b', border: '1px solid #fecaca', borderRadius: '6px', marginBottom: '16px' }}>{error}</div>}

      {/* Danh sách bài chờ giao */}
      <h4 style={{ margin: '0 0 8px' }}>Danh sách bài viết đã gửi (SUBMITTED):</h4>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px', marginBottom: '20px' }}>
        <thead>
          <tr style={{ background: '#f8fafc', textAlign: 'left' }}>
            <th style={{ padding: '8px', border: '1px solid #e2e8f0', width: '50px' }}>Mã bài</th>
            <th style={{ padding: '8px', border: '1px solid #e2e8f0' }}>Tiêu đề bài viết</th>
            <th style={{ padding: '8px', border: '1px solid #e2e8f0' }}>Thể loại</th>
            <th style={{ padding: '8px', border: '1px solid #e2e8f0' }}>Tác giả</th>
            <th style={{ padding: '8px', border: '1px solid #e2e8f0' }}>Thời gian gửi</th>
            <th style={{ padding: '8px', border: '1px solid #e2e8f0', textAlign: 'center' }}>Chọn</th>
          </tr>
        </thead>
        <tbody>
          {articles.map(art => (
            <tr key={art.id} style={{ background: selectedArticleId === art.id ? '#f0fdf4' : 'transparent' }}>
              <td style={{ padding: '8px', border: '1px solid #e2e8f0', fontWeight: 'bold' }}>#{art.id}</td>
              <td style={{ padding: '8px', border: '1px solid #e2e8f0' }}>{art.title}</td>
              <td style={{ padding: '8px', border: '1px solid #e2e8f0' }}>{art.category}</td>
              <td style={{ padding: '8px', border: '1px solid #e2e8f0' }}>{art.author}</td>
              <td style={{ padding: '8px', border: '1px solid #e2e8f0', color: '#64748b' }}>{art.submittedAt}</td>
              <td style={{ padding: '8px', border: '1px solid #e2e8f0', textAlign: 'center' }}>
                <button
                  type="button"
                  onClick={() => setSelectedArticleId(art.id)}
                  style={{
                    padding: '4px 10px',
                    background: selectedArticleId === art.id ? '#14532d' : '#f1f5f9',
                    color: selectedArticleId === art.id ? '#fff' : '#1b2430',
                    border: '1px solid #cbd5e1',
                    borderRadius: '4px',
                    cursor: 'pointer'
                  }}
                >
                  {selectedArticleId === art.id ? 'Đang chọn' : 'Chọn'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Form giao việc cho bài được chọn */}
      <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
        <h4 style={{ margin: '0 0 12px' }}>
          Giao việc cho Bài #{selectedArticleId}: <em>"{selectedArticle?.title}"</em>
        </h4>

        {currentAssignment && (
          <div style={{ padding: '8px 12px', background: '#eff6ff', color: '#1e40af', borderRadius: '6px', marginBottom: '14px', fontSize: '13.5px' }}>
            ℹ️ Bài này hiện đã được giao cho <strong>BTV #{currentAssignment.editorId}</strong> vào lúc {new Date(currentAssignment.assignedAt).toLocaleString('vi-VN')}. (Phân công lại sẽ cập nhật BTV mới).
          </div>
        )}

        <form onSubmit={handleAssign} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontWeight: '600', marginBottom: '6px', fontSize: '13px' }}>
              Chỉ định Biên tập viên (BTV):
            </label>
            <select
              value={selectedEditorId}
              onChange={(e) => setSelectedEditorId(Number(e.target.value))}
              style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1', fontSize: '14px' }}
            >
              {editors.map(ed => (
                <option key={ed.id} value={ed.id}>{ed.name} ({ed.username})</option>
              ))}
            </select>

            {workload !== null && (
              /* UC-19: Gợi ý BTV và xem khối lượng bài đang phụ trách */
              <p style={{ margin: '6px 0 0', fontSize: '13px', color: '#047857' }}>
                📊 Khối lượng công việc: BTV này đang phụ trách <strong>{workload}</strong> bài viết.
              </p>
            )}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '14px', marginBottom: '12px' }}>
              <input
                type="checkbox"
                checked={wasSuggested}
                onChange={(e) => setWasSuggested(e.target.checked)}
              />
              <span>Ghi nhận giao theo gợi ý của hệ thống (was_suggested)</span>
            </label>

            <button
              type="submit"
              disabled={loading}
              style={{
                padding: '10px 18px',
                background: '#14532d',
                color: '#fff',
                border: 'none',
                borderRadius: '6px',
                fontWeight: '600',
                cursor: 'pointer'
              }}
            >
              {loading ? 'Đang lưu...' : 'Xác nhận Phân công bài viết'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
