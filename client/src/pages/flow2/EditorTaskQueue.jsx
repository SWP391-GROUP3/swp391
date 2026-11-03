import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { layBaiDuocGiaoCuaBtv, layPhieuChamMoiNhat } from '../../api/assessmentApi.js'
import { goiApi } from '../../api/goiApi.js'
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
  const [genres, setGenres] = useState([])
  const [filterType, setFilterType] = useState('ALL') // 'ALL' | 'UNSCORED' | 'SCORED'
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')

  // Mo trang: tai hang viec cua BTV
  // Tải danh mục thể loại từ database
  useEffect(() => {
    async function fetchGenres() {
      try {
        const data = await goiApi('/api/genres')
        if (Array.isArray(data)) setGenres(data)
      } catch (e) {
        console.error('Không tải được thể loại:', e)
      }
    }
    fetchGenres()
  }, [])

  useEffect(() => {
    loadEditorTasks()
  }, [editorId])

  // Bai duoc giao cho BTV (id lay tu tai khoan dang dang nhap)
  async function loadEditorTasks() {
    setLoading(true)
    setMessage('')
    try {
      const data = await layBaiDuocGiaoCuaBtv(editorId)
      const tasksList = data || []

      // Bổ sung thông tin chấm bài và tiêu đề (tương thích ngay cả khi backend chưa restart)
      const enriched = await Promise.all(
        tasksList.map(async (task) => {
          if (task.scoringStatus && task.articleTitle) {
            return task
          }
          try {
            const attempt = await layPhieuChamMoiNhat(task.articleId)
            if (attempt) {
              const isSubmitted = Boolean(attempt.submittedAt)
              return {
                ...task,
                articleTitle: task.articleTitle || attempt.articleTitle,
                categoryId: task.categoryId || attempt.categoryId,
                categoryName: task.categoryName || (attempt.categoryId ? `Thể loại #${attempt.categoryId}` : null),
                scoringStatus: isSubmitted ? 'DA_CHAM' : (attempt.totalScore ? 'DANG_CHAM' : 'CHUA_CHAM'),
                totalScore: task.totalScore != null ? task.totalScore : attempt.totalScore,
                decision: task.decision || attempt.decision
              }
            }
          } catch {
            // Chưa mở phiếu chấm nào
          }
          return {
            ...task,
            scoringStatus: task.scoringStatus || 'CHUA_CHAM'
          }
        })
      )

      setTasks(enriched)
    } catch (err) {
      setMessage('Không thể kết nối máy chủ: ' + err.message)
      setTasks([])
    } finally {
      setLoading(false)
    }
  }

  // Số lượng bài theo trạng thái chấm
  const unscoredCount = tasks.filter(t => t.scoringStatus !== 'DA_CHAM').length
  const scoredCount = tasks.filter(t => t.scoringStatus === 'DA_CHAM').length

  const filteredTasks = tasks.filter(task => {
    if (filterType === 'UNSCORED') return task.scoringStatus !== 'DA_CHAM'
    if (filterType === 'SCORED') return task.scoringStatus === 'DA_CHAM'
    return true
  })

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

      {/* Tabs lọc nhanh Đã chấm / Chưa chấm */}
      <div className="bf02__tabs">
        <button
          type="button"
          onClick={() => setFilterType('ALL')}
          className={`bf02__tab ${filterType === 'ALL' ? 'bf02__tab--chon' : ''}`}
        >
          Tất cả ({tasks.length})
        </button>
        <button
          type="button"
          onClick={() => setFilterType('UNSCORED')}
          className={`bf02__tab ${filterType === 'UNSCORED' ? 'bf02__tab--chon' : ''}`}
        >
          Chưa chấm ({unscoredCount})
        </button>
        <button
          type="button"
          onClick={() => setFilterType('SCORED')}
          className={`bf02__tab ${filterType === 'SCORED' ? 'bf02__tab--chon' : ''}`}
        >
          Đã chấm ({scoredCount})
        </button>
      </div>

      {message && <div className="thong-bao-loi">{message}</div>}

      {loading ? (
        <p className="bf02__nhat">Đang tải danh sách bài được giao...</p>
      ) : tasks.length === 0 ? (
        <div className="bf02__trong">
          Hiện tại bạn chưa được phân công bài viết nào.
        </div>
      ) : filteredTasks.length === 0 ? (
        <div className="bf02__trong">
          Không có bài viết nào phù hợp với bộ lọc &quot;{filterType === 'SCORED' ? 'Đã chấm' : 'Chưa chấm'}&quot;.
        </div>
      ) : (
        <div className="bf02__cuon">
          <table className="bang-fm">
            <thead>
              <tr>
                <th className="bf02__giua">STT</th>
                <th>Bài viết</th>
                <th>Thể loại</th>
                <th>Trạng thái chấm</th>
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
              {filteredTasks.map((task, idx) => {
                const isScored = task.scoringStatus === 'DA_CHAM'
                const isDraft = task.scoringStatus === 'DANG_CHAM'

                return (
                  <tr key={task.id}>
                    <td className="bf02__giua">{idx + 1}</td>
                    <td>
                      <div className="bf02__dam" style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                        <span>#{task.articleId}</span>
                        {task.articleTitle && (
                          <span style={{ fontWeight: 500, color: 'var(--fm-chu)' }}>{task.articleTitle}</span>
                        )}
                      </div>
                    </td>
                    <td>{genres.find(g => g.id === task.categoryId)?.name || task.categoryName || '—'}</td>
                    <td>
                      {isScored ? (
                        <div>
                          <span className="nhan-tt nhan-tt--tot">
                            <Icon>{BT_DAU_TICH}</Icon>Đã chấm
                          </span>
                          {task.totalScore != null && (
                            <strong style={{ marginLeft: 6, fontSize: 13, color: 'var(--fm-tot)' }}>
                              {Number(task.totalScore).toFixed(1)}/100
                            </strong>
                          )}
                          {task.decision && (
                            <div style={{ fontSize: 11, color: 'var(--fm-chu-nhat)', marginTop: 2 }}>
                              {task.decision === 'ACCEPT' ? 'Đã duyệt nộp' : task.decision === 'REVISION' ? 'Yêu cầu sửa' : 'Từ chối'}
                            </div>
                          )}
                        </div>
                      ) : isDraft ? (
                        <div>
                          <span className="nhan-tt nhan-tt--chu-y">Đang chấm (bản nháp)</span>
                          {task.totalScore != null && (
                            <span style={{ marginLeft: 6, fontSize: 12, color: 'var(--fm-chu-nhat)' }}>
                              ({Number(task.totalScore).toFixed(1)}đ)
                            </span>
                          )}
                        </div>
                      ) : (
                        <span className="nhan-tt">Chưa chấm</span>
                      )}
                    </td>
                    <td>{task.assignedAt ? new Date(task.assignedAt).toLocaleString('vi-VN') : '—'}</td>
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
                        className={isScored ? 'nut-nhe' : 'nut-chinh-fm'}
                      >
                        {isScored ? 'Xem lại phiếu' : isDraft ? 'Tiếp tục chấm' : 'Bắt đầu chấm bài'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
