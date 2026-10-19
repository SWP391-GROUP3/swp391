import { useState, useEffect } from 'react'
import {
  layKhoiLuongCongViecBtv,
  layPhanCongBaiViet,
  phanCongBaiViet
} from '../../api/assessmentApi.js'
import Icon from '../../components/common/Icon.jsx'
import { BT_PHAN_CONG, BT_THONG_TIN, BT_BIEU_DO } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

/**
 * MUC LUC (so dong)
 *   21  AssignmentQueue
 *   45    useEffect                  - tai du lieu khi mo trang
 *   49    useEffect                  - tai du lieu khi mo trang
 *   53    loadWorkload()
 *   62    loadCurrentAssignment()
 *   72    handleAssign()
 */

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
    <div className="bf02__the">
      {/* UC-18, UC-20; BR-QA-04: Trưởng ban phân công bài viết */}
      <h3 className="bf02__tieu-de"><Icon>{BT_PHAN_CONG}</Icon>Hàng chờ phân công bài viết</h3>
      <p className="bf02__mo-ta">
        Trưởng ban chọn bài viết đã nộp và chỉ định một Biên tập viên chịu trách nhiệm chấm chất lượng (Mỗi bài chỉ có 1 BTV chính).
      </p>

      {/* Thông báo */}
      {message && <div className="thong-bao-thanh-cong">{message}</div>}
      {error && <div className="thong-bao-loi">{error}</div>}

      {/* Danh sách bài chờ giao */}
      <h4 className="bf02__tieu-de-nho">Danh sách bài viết đã gửi (SUBMITTED):</h4>
      <div className="bf02__cuon">
        <table className="bang-fm">
          <thead>
            <tr>
              <th>Mã bài</th>
              <th>Tiêu đề bài viết</th>
              <th>Thể loại</th>
              <th>Tác giả</th>
              <th>Thời gian gửi</th>
              <th className="bf02__giua">Chọn</th>
            </tr>
          </thead>
          <tbody>
            {articles.map(art => (
              <tr key={art.id} className={selectedArticleId === art.id ? 'bf02__dong--chon' : undefined}>
                <td className="bf02__dam">#{art.id}</td>
                <td>{art.title}</td>
                <td>{art.category}</td>
                <td>{art.author}</td>
                <td className="bf02__nhat">{art.submittedAt}</td>
                <td className="bf02__giua">
                  <button
                    type="button"
                    onClick={() => setSelectedArticleId(art.id)}
                    className={selectedArticleId === art.id ? 'nut-chinh-fm' : 'nut-nhe'}
                    aria-pressed={selectedArticleId === art.id}
                  >
                    {selectedArticleId === art.id ? 'Đang chọn' : 'Chọn'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Form giao việc cho bài được chọn */}
      <div className="bf02__khung-phu">
        <h4 className="bf02__tieu-de-nho">
          Giao việc cho Bài #{selectedArticleId}: <em>"{selectedArticle?.title}"</em>
        </h4>

        {currentAssignment && (
          <div className="bf02__thong-tin">
            <Icon>{BT_THONG_TIN}</Icon>
            <span>
              Bài này hiện đã được giao cho <strong>BTV #{currentAssignment.editorId}</strong> vào lúc {new Date(currentAssignment.assignedAt).toLocaleString('vi-VN')}. (Phân công lại sẽ cập nhật BTV mới).
            </span>
          </div>
        )}

        <form onSubmit={handleAssign} className="bf02__form-luoi">
          <div>
            <label className="nhan" htmlFor="phan-cong-btv">
              Chỉ định Biên tập viên (BTV):
            </label>
            <select
              id="phan-cong-btv"
              value={selectedEditorId}
              onChange={(e) => setSelectedEditorId(Number(e.target.value))}
              className="o-nhap-ngan bf02__o-nhap"
            >
              {editors.map(ed => (
                <option key={ed.id} value={ed.id}>{ed.name} ({ed.username})</option>
              ))}
            </select>

            {workload !== null && (
              /* UC-19: Gợi ý BTV và xem khối lượng bài đang phụ trách */
              <p className="bf02__goi-y">
                <Icon>{BT_BIEU_DO}</Icon>
                <span>Khối lượng công việc: BTV này đang phụ trách <strong>{workload}</strong> bài viết.</span>
              </p>
            )}
          </div>

          <div className="bf02__cot-nut">
            <label className="bf02__hop-chon">
              <input
                type="checkbox"
                checked={wasSuggested}
                onChange={(e) => setWasSuggested(e.target.checked)}
              />
              <span>Ghi nhận giao theo gợi ý của hệ thống (was_suggested)</span>
            </label>

            <button type="submit" disabled={loading} className="nut-chinh-fm">
              {loading ? 'Đang lưu...' : 'Xác nhận Phân công bài viết'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
