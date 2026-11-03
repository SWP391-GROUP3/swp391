import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { layBaiDuocGiaoCuaBtv } from '../../api/assessmentApi.js'
import Icon from '../../components/common/Icon.jsx'
import { BT_BAI_DUOC_GIAO, BT_DAU_TICH } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

/**
 * Man BTV xem bai duoc giao cham (UC-21, UC-22): danh sach bai, trang thai, ngay giao; bam de mo phieu
 * cham.
 *
 * MO TRANG NAY TAI: /assigned-articles
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  EditorTaskQueue()               editor task queue
 *   33    useEffect                     Mo trang: tai hang viec cua BTV
 *   38    loadEditorTasks()             Bai duoc giao cho BTV (id lay tu tai khoan dang dang nhap)
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *   61  [Lam moi]                       loadEditorTasks() Bai duoc giao cho BTV (id lay tu tai khoan dang dang nhap) -> goi GET /api/assignments/editor/{editorId}
 *  102  [Bat dau Cham bai]              xu ly tren trang
 */

export default function EditorTaskQueue({ nguoiDung }) {
  const navigate = useNavigate()
  // M-A3: BTV chi xem hang viec cua chinh minh (id lay tu tai khoan dang dang nhap)
  const editorId = nguoiDung.id
  const [tasks, setTasks] = useState([])
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  // Mo trang: tai hang viec cua BTV
  useEffect(() => {
    loadEditorTasks()
  }, [editorId])

  // Bai duoc giao cho BTV (id lay tu tai khoan dang dang nhap)
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
        Danh sách các bài viết Trưởng ban đã phân công cho bạn. BTV chọn bài để tiến hành đọc bài và chấm điểm chất lượng.
      </p>

      <div className="bf02__hang">
        <button type="button" onClick={loadEditorTasks} className="nut-nhe">
          Làm mới
        </button>
      </div>

      {message && <div className="thong-bao-loi">{message}</div>}

      {loading ? (
        <p className="bf02__nhat">Đang tải danh sách bài được giao...</p>
      ) : tasks.length === 0 ? (
        <div className="bf02__trong">
          Hiện tại bạn chưa được phân công bài viết nào.
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
                      onClick={() => navigate('/assigned-articles/' + task.articleId)}
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
