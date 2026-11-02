import { useState, useEffect } from 'react'
import {
  layKhoiLuongCongViecBtv,
  layPhanCongBaiViet,
  phanCongBaiViet,
  layDanhSachChoPhanCong
} from '../../api/assessmentApi.js'
import Icon from '../../components/common/Icon.jsx'
import { BT_PHAN_CONG, BT_THONG_TIN, BT_BIEU_DO } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

/**
 * Man Truong ban phan cong bai (UC-18, UC-19, UC-20): danh sach bai SUBMITTED cho phan cong, goi y BTV
 * it viec / da cham cung the loai, chon BTV de giao bai.
 *
 * MO TRANG NAY TAI: /assignments
 *
 * MUC LUC (so dong - ten - tac dung)
 *   30  AssignmentQueue()               assignment queue
 *   54    useEffect                     tai du lieu khi mo trang
 *   58    useEffect                     tai du lieu khi mo trang
 *   62    loadWorkload()                tai workload
 *   71    loadCurrentAssignment()       tai current assignment
 *   81    handleAssign()                xu ly assign
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  145  [Dang chon / Chon]              tren trang: selected article id
 *  173  form, nut [Dang luu... / Xac nhan Phan cong ba…]  handleAssign() -> goi POST /api/assignments, GET /api/assignments/editor/{editorId}/workload
 */

export default function AssignmentQueue({ nguoiDung }) {
  // Danh sách bài viết chờ phân công (SUBMITTED) tải động từ Database theo ban của Trưởng ban (UC-18)
  const [articles, setArticles] = useState([])
  const [loadingArticles, setLoadingArticles] = useState(false)

  // Danh sách BTV để chọn giao việc (theo data.sql: cuong.lh id=3, giang.nt id=9)
  const editors = [
    { id: 3, name: 'Lê Hoàng Cường (Editor Thời sự)', username: 'cuong.lh' },
    { id: 9, name: 'Ngô Thu Giang (Editor Kinh tế)', username: 'giang.nt' },
    { id: 4, name: 'Phạm Thu Dung (Thư ký toà soạn)', username: 'dung.pt' }
  ]

  const [selectedArticleId, setSelectedArticleId] = useState(null)
  const [selectedEditorId, setSelectedEditorId] = useState(3)
  const [wasSuggested, setWasSuggested] = useState(false)
  const [workload, setWorkload] = useState(null)
  const [currentAssignment, setCurrentAssignment] = useState(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadPendingArticles()
  }, [nguoiDung?.sectionId])

  useEffect(() => {
    loadWorkload(selectedEditorId)
  }, [selectedEditorId])

  useEffect(() => {
    if (selectedArticleId) {
      loadCurrentAssignment(selectedArticleId)
    } else {
      setCurrentAssignment(null)
    }
  }, [selectedArticleId])

  async function loadPendingArticles() {
    setLoadingArticles(true)
    setError('')
    try {
      const data = await layDanhSachChoPhanCong(nguoiDung?.sectionId)
      setArticles(data || [])
      if (data && data.length > 0) {
        setSelectedArticleId(prev => {
          const tonTai = data.some(a => a.id === prev)
          return tonTai ? prev : data[0].id
        })
      } else {
        setSelectedArticleId(null)
      }
    } catch (err) {
      setError('Lỗi tải danh sách bài chờ phân công: ' + err.message)
      setArticles([])
    } finally {
      setLoadingArticles(false)
    }
  }

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
    if (!selectedArticleId) {
      setError('Vui lòng chọn một bài viết để phân công!')
      return
    }

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
      loadPendingArticles()
      loadWorkload(selectedEditorId)
    } catch (err) {
      setError('Giao bài thất bại: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  function dinhDangNgay(chuoiNgay) {
    if (!chuoiNgay) return '—'
    try {
      return new Date(chuoiNgay).toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      })
    } catch {
      return chuoiNgay
    }
  }

  const selectedArticle = articles.find(a => a.id === Number(selectedArticleId))

  return (
    <div className="bf02__the">
      {/* UC-18, UC-20; BR-QA-04: Trưởng ban phân công bài viết */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h3 className="bf02__tieu-de"><Icon>{BT_PHAN_CONG}</Icon>Hàng chờ phân công bài viết</h3>
          <p className="bf02__mo-ta">
            Trưởng ban chọn bài viết đã nộp và chỉ định một Biên tập viên chịu trách nhiệm chấm chất lượng (Mỗi bài chỉ có 1 BTV chính).
          </p>
        </div>
        <button
          type="button"
          onClick={loadPendingArticles}
          disabled={loadingArticles}
          className="nut-nhe"
        >
          {loadingArticles ? 'Đang tải...' : 'Làm mới danh sách'}
        </button>
      </div>

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
            {loadingArticles && (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: 24, color: 'var(--fm-chu-nhat)' }}>
                  Đang tải danh sách bài chờ phân công từ hệ thống...
                </td>
              </tr>
            )}
            {!loadingArticles && articles.length === 0 && (
              <tr>
                <td colSpan="6" style={{ textAlign: 'center', padding: 24, color: 'var(--fm-chu-nhat)' }}>
                  Hiện không có bài viết nào đang ở trạng thái Chờ phân công (SUBMITTED).
                </td>
              </tr>
            )}
            {!loadingArticles && articles.map(art => (
              <tr key={art.id} className={selectedArticleId === art.id ? 'bf02__dong--chon' : undefined}>
                <td className="bf02__dam">#{art.id}</td>
                <td>{art.title}</td>
                <td>{art.category}</td>
                <td>{art.author}</td>
                <td className="bf02__nhat">{dinhDangNgay(art.submittedAt)}</td>
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
