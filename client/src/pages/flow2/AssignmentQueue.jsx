import { useState, useEffect } from 'react'
import {
  layKhoiLuongCongViecBtv,
  layPhanCongBaiViet,
  phanCongBaiViet,
  layDanhSachChoPhanCong,
  layDanhSachBaiDaGiaoBan,
  doiBienTapVien,
  thuHoiPhanCong
} from '../../api/assessmentApi.js'
import { layDanhSachBienTapVien } from '../../api/reviewerProfileApi.js'
import Icon from '../../components/common/Icon.jsx'
import { BT_PHAN_CONG, BT_THONG_TIN, BT_BIEU_DO, BT_DAU_TICH, BT_DAU_X } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

/**
 * Man Truong ban phan cong bai (UC-18, UC-19, UC-20): danh sach bai SUBMITTED cho phan cong, goi y BTV
 * it viec / da cham cung the loai, chon BTV de giao bai.
 *
 * MO TRANG NAY TAI: /assignments
 *
 * MUC LUC (so dong - ten - tac dung)
 *   37  AssignmentQueue()               assignment queue
 *   55    useEffect                     Mo trang: tai hang cho phan cong va danh sach BTV cung ban
 *   61    useEffect                     Doi BTV dang chon: tai so bai BTV do dang giu (UC-19 goi y BTV it viec)
 *   68    useEffect                     Doi bai dang chon: xem bai da co phan cong chua
 *   77    loadPendingArticles()         UC-18: bai SUBMITTED cua ban Truong ban dang dang nhap (server lay ban tu token)
 *  100    loadEditors()                 UC-19: BTV cung ban, chi giu BTV dang ACTIVE; chon san BTV dau tien
 *  114    loadWorkload()                So bai BTV dang giu, hien canh o chon BTV
 *  124    loadCurrentAssignment()       Phan cong hien tai cua bai; chua co thi server tra 404 -> bo qua
 *  135    handleAssign()                UC-20: bam Xac nhan phan cong -> POST /api/assignments, xong tai lai hang cho
 *  169    dinhDangNgay()                Ngay gio kieu Viet Nam de hien trong bang
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  198  [Dang tai... / Lam moi danh sach]  loadPendingArticles() UC-18: bai SUBMITTED cua ban Truong ban dang dang nhap (server lay ban tu token) -> goi GET /api/assignments/pending
 *  249  [Dang chon / Chon]              tren trang: selected article id
 *  277  form, nut [Dang luu... / Xac nhan Phan cong ba…]  handleAssign() UC-20: bam Xac nhan phan cong -> POST /api/assignments, xong tai lai hang cho -> goi POST /api/assignments, GET /api/assignments/pending, GET /a…
 */

export default function AssignmentQueue() {
  // Tab hiện tại: Chờ phân công (SUBMITTED) hoặc Đang giao BTV chấm (ASSIGNED)
  const [tabHienTai, setTabHienTai] = useState('CHO_PHAN_CONG')

  // Danh sách bài viết chờ phân công (SUBMITTED) tải động từ Database theo ban của Trưởng ban (UC-18)
  const [articles, setArticles] = useState([])
  const [loadingArticles, setLoadingArticles] = useState(false)

  // Danh sách bài viết đã phân công (ASSIGNED)
  const [assignedArticles, setAssignedArticles] = useState([])
  const [loadingAssigned, setLoadingAssigned] = useState(false)

  // M-A3: BTV dang hoat dong cua ban Truong ban, lay tu GET /api/reviewers (UC-20 E2: BTV bi khoa khong chon duoc)
  const [editors, setEditors] = useState([])

  const [selectedArticleId, setSelectedArticleId] = useState(null)
  const [selectedEditorId, setSelectedEditorId] = useState(null)
  const [wasSuggested, setWasSuggested] = useState(false)
  const [workload, setWorkload] = useState(null)
  const [currentAssignment, setCurrentAssignment] = useState(null)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // Modal đổi BTV (Re-assign)
  const [baiDoiBtv, setBaiDoiBtv] = useState(null)
  const [btvMoiId, setBtvMoiId] = useState('')
  const [dangXuLyDoi, setDangXuLyDoi] = useState(false)

  // Modal thu hồi phân công (Revoke)
  const [baiThuHoi, setBaiThuHoi] = useState(null)
  const [lyDoThuHoi, setLyDoThuHoi] = useState('')
  const [dangXuLyThuHoi, setDangXuLyThuHoi] = useState(false)

  // Mo trang: tai hang cho phan cong, danh sach da phan cong va danh sach BTV cung ban
  useEffect(() => {
    loadPendingArticles()
    loadAssignedArticles()
    loadEditors()
  }, [])

  // Doi BTV dang chon: tai so bai BTV do dang giu (UC-19 goi y BTV it viec)
  useEffect(() => {
    if (selectedEditorId) {
      loadWorkload(selectedEditorId)
    }
  }, [selectedEditorId])

  // Doi bai dang chon: xem bai da co phan cong chua
  useEffect(() => {
    if (selectedArticleId) {
      loadCurrentAssignment(selectedArticleId)
    } else {
      setCurrentAssignment(null)
    }
  }, [selectedArticleId])

  // UC-18: bai SUBMITTED cua ban Truong ban dang dang nhap (server lay ban tu token)
  async function loadPendingArticles() {
    setLoadingArticles(true)
    setError('')
    try {
      const data = await layDanhSachChoPhanCong()
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

  // Tải danh sách bài viết đã phân công (ASSIGNED) trong ban
  async function loadAssignedArticles() {
    setLoadingAssigned(true)
    try {
      const data = await layDanhSachBaiDaGiaoBan()
      setAssignedArticles(data || [])
    } catch (err) {
      setError('Lỗi tải danh sách bài đã phân công: ' + err.message)
      setAssignedArticles([])
    } finally {
      setLoadingAssigned(false)
    }
  }

  // Mở modal đổi BTV (Re-assign)
  function moModalDoiBtv(item) {
    setBaiDoiBtv(item)
    const btvKhac = editors.find(ed => ed.editorId !== item.editorId)
    setBtvMoiId(btvKhac ? btvKhac.editorId : '')
    setError('')
    setMessage('')
  }

  // Xác nhận đổi BTV
  async function xacNhanDoiBtv() {
    if (!btvMoiId) {
      setError('Vui lòng chọn Biên tập viên mới!')
      return
    }
    if (Number(btvMoiId) === Number(baiDoiBtv.editorId)) {
      setError('Biên tập viên mới phải khác Biên tập viên đang phụ trách!')
      return
    }

    setDangXuLyDoi(true)
    setError('')
    try {
      await doiBienTapVien(baiDoiBtv.articleId, Number(btvMoiId))
      setMessage(`Đã chuyển quyền chấm bài #${baiDoiBtv.articleId} sang BTV mới thành công!`)
      setBaiDoiBtv(null)
      await Promise.all([loadPendingArticles(), loadAssignedArticles()])
      if (selectedEditorId) loadWorkload(selectedEditorId)
    } catch (err) {
      setError('Lỗi khi đổi BTV: ' + err.message)
    } finally {
      setDangXuLyDoi(false)
    }
  }

  // Mở modal thu hồi phân công (Revoke)
  function moModalThuHoi(item) {
    setBaiThuHoi(item)
    setLyDoThuHoi('')
    setError('')
    setMessage('')
  }

  // Xác nhận thu hồi phân công
  async function xacNhanThuHoi() {
    if (!lyDoThuHoi.trim()) {
      setError('Vui lòng nhập lý do thu hồi phân công!')
      return
    }

    setDangXuLyThuHoi(true)
    setError('')
    try {
      await thuHoiPhanCong(baiThuHoi.articleId, lyDoThuHoi)
      setMessage(`Đã thu hồi phân công bài #${baiThuHoi.articleId}. Bài viết đã trở về trạng thái Chờ phân công (SUBMITTED)!`)
      setBaiThuHoi(null)
      await Promise.all([loadPendingArticles(), loadAssignedArticles()])
      if (selectedEditorId) loadWorkload(selectedEditorId)
    } catch (err) {
      setError('Lỗi khi thu hồi phân công: ' + err.message)
    } finally {
      setDangXuLyThuHoi(false)
    }
  }

  // UC-19: BTV cung ban, chi giu BTV dang ACTIVE; chon san BTV dau tien
  async function loadEditors() {
    try {
      const data = await layDanhSachBienTapVien()
      const dangHoatDong = (data || []).filter(ed => ed.status === 'ACTIVE')
      setEditors(dangHoatDong)
      if (dangHoatDong.length > 0) {
        setSelectedEditorId(dangHoatDong[0].editorId)
      }
    } catch (err) {
      setError('Lỗi tải danh sách Biên tập viên: ' + err.message)
    }
  }

  // So bai BTV dang giu, hien canh o chon BTV
  async function loadWorkload(editorId) {
    try {
      const data = await layKhoiLuongCongViecBtv(editorId)
      setWorkload(data.activeArticleCount)
    } catch {
      // bỏ qua nếu lỗi
    }
  }

  // Phan cong hien tai cua bai; chua co thi server tra 404 -> bo qua
  async function loadCurrentAssignment(articleId) {
    setCurrentAssignment(null)
    try {
      const data = await layPhanCongBaiViet(articleId)
      setCurrentAssignment(data)
    } catch {
      // chưa có phân công
    }
  }

  // UC-20: bam Xac nhan phan cong -> POST /api/assignments, xong tai lai hang cho
  async function handleAssign(e) {
    e.preventDefault()
    if (!selectedArticleId) {
      setError('Vui lòng chọn một bài viết để phân công!')
      return
    }
    if (!selectedEditorId) {
      setError('Ban chưa có Biên tập viên đang hoạt động để giao bài.')
      return
    }

    setMessage('')
    setError('')
    setLoading(true)

    try {
      const payload = {
        articleId: Number(selectedArticleId),
        editorId: Number(selectedEditorId),
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

  // Ngay gio kieu Viet Nam de hien trong bang
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
      {/* UC-18, UC-20; BR-SC-04: Trưởng ban phân công bài viết */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
        <div>
          <h3 className="bf02__tieu-de"><Icon>{BT_PHAN_CONG}</Icon>Hàng chờ phân công bài viết</h3>
          <p className="bf02__mo-ta">
            Trưởng ban chọn bài viết đã nộp và chỉ định một Biên tập viên chịu trách nhiệm chấm chất lượng, hoặc điều chỉnh phân công (Đổi BTV, Thu hồi).
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            loadPendingArticles()
            loadAssignedArticles()
          }}
          disabled={loadingArticles || loadingAssigned}
          className="nut-nhe"
        >
          {loadingArticles || loadingAssigned ? 'Đang tải...' : 'Làm mới danh sách'}
        </button>
      </div>

      {/* Thông báo */}
      {message && <div className="thong-bao-thanh-cong">{message}</div>}
      {error && <div className="thong-bao-loi">{error}</div>}

      {/* Tab chuyển đổi giữa Chờ phân công và Đang giao BTV */}
      <div style={{ display: 'flex', gap: 12, marginBottom: 20, borderBottom: '1px solid var(--fm-vien)' }}>
        <button
          type="button"
          onClick={() => setTabHienTai('CHO_PHAN_CONG')}
          className="nut-nhe"
          style={{
            border: 'none',
            borderBottom: tabHienTai === 'CHO_PHAN_CONG' ? '2px solid var(--fm-chinh)' : '2px solid transparent',
            borderRadius: 0,
            padding: '8px 16px',
            fontWeight: tabHienTai === 'CHO_PHAN_CONG' ? 600 : 400,
            color: tabHienTai === 'CHO_PHAN_CONG' ? 'var(--fm-chinh)' : 'var(--fm-chu-nhat)'
          }}
        >
          Chờ phân công (SUBMITTED) ({articles.length})
        </button>
        <button
          type="button"
          onClick={() => setTabHienTai('DA_PHAN_CONG')}
          className="nut-nhe"
          style={{
            border: 'none',
            borderBottom: tabHienTai === 'DA_PHAN_CONG' ? '2px solid var(--fm-chinh)' : '2px solid transparent',
            borderRadius: 0,
            padding: '8px 16px',
            fontWeight: tabHienTai === 'DA_PHAN_CONG' ? 600 : 400,
            color: tabHienTai === 'DA_PHAN_CONG' ? 'var(--fm-chinh)' : 'var(--fm-chu-nhat)'
          }}
        >
          Đang giao BTV chấm (ASSIGNED) ({assignedArticles.length})
        </button>
      </div>

      {tabHienTai === 'CHO_PHAN_CONG' && (
        <>
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
                  Bài này hiện đã được giao cho <strong>BTV #{currentAssignment.editorId}</strong> vào lúc {new Date(currentAssignment.assignedAt).toLocaleString('vi-VN')}.
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
                  value={selectedEditorId ?? ''}
                  onChange={(e) => setSelectedEditorId(Number(e.target.value))}
                  className="o-nhap-ngan bf02__o-nhap"
                >
                  {editors.map(ed => (
                    <option key={ed.editorId} value={ed.editorId}>{ed.editorName} – {ed.sectionName}</option>
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
        </>
      )}

      {tabHienTai === 'DA_PHAN_CONG' && (
        <>
          <h4 className="bf02__tieu-de-nho">Danh sách bài viết đang giao BTV chấm (ASSIGNED):</h4>
          <div className="bf02__cuon">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th>Mã bài</th>
                  <th>Tiêu đề bài viết</th>
                  <th>Tác giả</th>
                  <th>Thể loại</th>
                  <th>BTV phụ trách</th>
                  <th>Tiến độ chấm</th>
                  <th>Thời gian giao</th>
                  <th className="bf02__giua">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {loadingAssigned && (
                  <tr>
                    <td colSpan="8" style={{ textAlign: 'center', padding: 24, color: 'var(--fm-chu-nhat)' }}>
                      Đang tải danh sách bài đã phân công...
                    </td>
                  </tr>
                )}
                {!loadingAssigned && assignedArticles.length === 0 && (
                  <tr>
                    <td colSpan="8" style={{ textAlign: 'center', padding: 24, color: 'var(--fm-chu-nhat)' }}>
                      Hiện chưa có bài viết nào đang trong trạng thái Phân công (ASSIGNED).
                    </td>
                  </tr>
                )}
                {!loadingAssigned && assignedArticles.map(item => (
                  <tr key={item.articleId}>
                    <td className="bf02__dam">#{item.articleId}</td>
                    <td>{item.articleTitle}</td>
                    <td>{item.authorName || '—'}</td>
                    <td>{item.genreName || '—'}</td>
                    <td className="bf02__dam">{item.editorName || `BTV #${item.editorId}`}</td>
                    <td>
                      {item.scoringStatus === 'IN_PROGRESS' ? (
                        <span className="nhan-tt nhan-tt--chu-y">Đang chấm</span>
                      ) : (
                        <span className="nhan-tt">Chưa chấm</span>
                      )}
                    </td>
                    <td className="bf02__nhat">{dinhDangNgay(item.assignedAt)}</td>
                    <td className="bf02__giua">
                      <div style={{ display: 'inline-flex', gap: 6 }}>
                        <button
                          type="button"
                          className="nut-nhe"
                          onClick={() => moModalDoiBtv(item)}
                          title="Đổi sang BTV khác"
                        >
                          Đổi BTV
                        </button>
                        <button
                          type="button"
                          className="nut-thao-tac nut-xoa"
                          onClick={() => moModalThuHoi(item)}
                          title="Thu hồi phân công về trạng thái SUBMITTED"
                        >
                          Thu hồi
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* Modal Đổi BTV khác (Re-assign) */}
      {baiDoiBtv && (
        <div className="bf02__modal-nen" onClick={() => setBaiDoiBtv(null)}>
          <div className="bf02__modal-hop" style={{ maxWidth: 500 }} onClick={(e) => e.stopPropagation()}>
            <div className="bf02__modal-dau">
              <h3>Đổi Biên tập viên phụ trách</h3>
              <button type="button" className="nut-nhe" onClick={() => setBaiDoiBtv(null)}>✕</button>
            </div>
            <div className="bf02__modal-than">
              <p style={{ margin: 0, fontSize: 13 }}>
                Bài viết: <strong>#{baiDoiBtv.articleId} - {baiDoiBtv.articleTitle}</strong>
              </p>
              <p style={{ margin: '4px 0 12px', fontSize: 13, color: 'var(--fm-chu-nhat)' }}>
                BTV hiện tại: <strong>{baiDoiBtv.editorName || `BTV #${baiDoiBtv.editorId}`}</strong>
              </p>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 13, fontWeight: 500 }}>
                  Chọn Biên tập viên mới:
                </label>
                <select
                  value={btvMoiId}
                  onChange={(e) => setBtvMoiId(e.target.value)}
                  className="o-nhap-ngan bf02__o-nhap"
                  style={{ width: '100%' }}
                >
                  <option value="">-- Chọn BTV tiếp nhận --</option>
                  {editors
                    .filter(ed => ed.editorId !== baiDoiBtv.editorId)
                    .map(ed => (
                      <option key={ed.editorId} value={ed.editorId}>
                        {ed.editorName} ({ed.sectionName})
                      </option>
                    ))}
                </select>
              </div>
            </div>
            <div className="bf02__modal-chan">
              <button type="button" className="nut-nhe" onClick={() => setBaiDoiBtv(null)} disabled={dangXuLyDoi}>
                Hủy
              </button>
              <button type="button" className="nut-chinh-fm" onClick={xacNhanDoiBtv} disabled={dangXuLyDoi || !btvMoiId}>
                {dangXuLyDoi ? 'Đang lưu...' : 'Xác nhận Đổi BTV'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Thu hồi phân công (Revoke) */}
      {baiThuHoi && (
        <div className="bf02__modal-nen" onClick={() => setBaiThuHoi(null)}>
          <div className="bf02__modal-hop" style={{ maxWidth: 500 }} onClick={(e) => e.stopPropagation()}>
            <div className="bf02__modal-dau">
              <h3>Thu hồi phân công bài viết</h3>
              <button type="button" className="nut-nhe" onClick={() => setBaiThuHoi(null)}>✕</button>
            </div>
            <div className="bf02__modal-than">
              <p style={{ margin: 0, fontSize: 13 }}>
                Bài viết: <strong>#{baiThuHoi.articleId} - {baiThuHoi.articleTitle}</strong>
              </p>
              <p style={{ margin: '4px 0 12px', fontSize: 13, color: 'var(--fm-loi)' }}>
                Sau khi thu hồi, bài viết sẽ quay lại trạng thái <strong>Chờ phân công (SUBMITTED)</strong> để Trưởng ban phân công lại sau.
              </p>
              <div>
                <label style={{ display: 'block', marginBottom: 6, fontSize: 13, fontWeight: 500 }}>
                  Lý do thu hồi phân công <span style={{ color: 'var(--fm-loi)' }}>*</span>:
                </label>
                <textarea
                  className="duyet-diem__textarea"
                  rows={3}
                  placeholder="Nhập lý do thu hồi (ví dụ: BTV bận đột xuất, cần phân công lại...)"
                  value={lyDoThuHoi}
                  onChange={(e) => setLyDoThuHoi(e.target.value)}
                />
              </div>
            </div>
            <div className="bf02__modal-chan">
              <button type="button" className="nut-nhe" onClick={() => setBaiThuHoi(null)} disabled={dangXuLyThuHoi}>
                Hủy
              </button>
              <button
                type="button"
                className="nut-chinh-fm"
                style={{ background: 'var(--fm-loi)', borderColor: 'var(--fm-loi)' }}
                onClick={xacNhanThuHoi}
                disabled={dangXuLyThuHoi}
              >
                {dangXuLyThuHoi ? 'Đang xử lý...' : 'Xác nhận Thu hồi'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
