import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { layBaiDuocGiaoCuaBtv } from '../../api/assessmentApi.js'
import Icon from '../../components/common/Icon.jsx'
import { BT_BAI_DUOC_GIAO, BT_DAU_TICH } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

export default function EditorTaskQueue({ nguoiDung, onSelectTaskToScore }) {
  const navigate = useNavigate()
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
      const data = await layBaiDuocGiaoCuaBtv(editorId)
      setTasks(data || [])
    } catch (err) {
      setMessage('Không thể kết nối máy chủ: ' + err.message)
      setTasks([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="bf02__the">
      {/* UC-21: Hàng việc của Biên tập viên */}
      <h3 className="bf02__tieu-de"><Icon>{BT_BAI_DUOC_GIAO}</Icon>Hàng việc của Biên tập viên</h3>
      <p className="bf02__mo-ta">
        Danh sách tất cả các bài viết được Trưởng ban phân công cho BTV này. BTV chọn bài để tiến hành đọc bài và chấm điểm chất lượng.
      </p>

      {/* Bộ lọc xem theo BTV */}
      <div className="bf02__hang">
        <label className="bf02__nhan" htmlFor="btv-ma">Mã BTV cần xem việc:</label>
        <input
          id="btv-ma"
          type="number"
          value={editorId}
          onChange={(e) => setEditorId(Number(e.target.value))}
          className="o-nhap-ngan bf02__o-nhap--so"
        />
        <button type="button" onClick={loadEditorTasks} className="nut-nhe">
          Làm mới
        </button>
      </div>

      {message && <div className="thong-bao-loi">{message}</div>}

      {loading ? (
        <p className="bf02__nhat">Đang tải danh sách bài được giao...</p>
      ) : tasks.length === 0 ? (
        <div className="bf02__trong">
          Hiện tại BTV #{editorId} chưa được phân công bài viết nào. (Hãy qua tab "Phân công bài viết" để giao việc thử).
        </div>
      ) : (
        <div className="bf02__cuon">
          <table className="bang-fm">
            <thead>
              <tr>
                <th className="bf02__giua">STT</th>
                <th>Mã bài</th>
                <th>Thời điểm giao</th>
                <th>Gợi ý bởi HT?</th>
                <th className="bf02__giua">Hành động</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((task, idx) => (
                <tr key={task.id}>
                  <td className="bf02__giua">{idx + 1}</td>
                  <td className="bf02__dam">#{task.articleId}</td>
                  <td>{new Date(task.assignedAt).toLocaleString('vi-VN')}</td>
                  <td>
                    {task.wasSuggested ? (
                      <span className="nhan-tt nhan-tt--tot"><Icon>{BT_DAU_TICH}</Icon>Đúng (Thuật toán)</span>
                    ) : (
                      <span className="bf02__nhat">Trưởng ban chọn tay</span>
                    )}
                  </td>
                  <td className="bf02__giua">
                    <button
                      type="button"
                      onClick={() => {
                        if (onSelectTaskToScore) onSelectTaskToScore(task)
                        else navigate('/assigned-articles/' + task.articleId)
                      }}
                      className="nut-chinh-fm"
                    >
                      Bắt đầu Chấm bài
                    </button>
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
