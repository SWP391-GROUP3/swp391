import { useState, useEffect } from 'react'
import {
  layBoTieuChiActive,
  layDanhSachBoTieuChi,
  taoBoTieuChiDraft,
  kichHoatBoTieuChi
} from '../../api/assessmentApi.js'
import Icon from '../../components/common/Icon.jsx'
import { BT_TIEU_CHI, BT_DANH_SACH, BT_THEM, BT_THONG_TIN, BT_DAU_TICH, BT_DAU_X } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

/**
 * Man quan ly bo tieu chi cham (UC-14...17): xem phien ban bo tieu chi theo the loai, tao phien ban
 * moi voi danh sach tieu chi va trong so.
 *
 * MO TRANG NAY TAI: /criteria-sets, /criteria-sets/:id
 *
 * MUC LUC (so dong - ten - tac dung)
 *   37  CriteriaManagement()            criteria management
 *   58    useEffect                     tai du lieu khi mo trang
 *   62    loadCategoryData()            tai category data
 *   89    handleAddCriterion()          xu ly add criterion
 *   96    handleRemoveCriterion()       xu ly remove criterion
 *  104    handleCriterionChange()       xu ly criterion change
 *  110    handleCreateNewVersion()      xu ly create new version
 *  147    handleActivate()              xu ly activate
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  182  [Tai lai]                       xu ly tren trang
 *  203  [{BT_DAU_TICH} Kich hoat (ACTIVE) ngay]  handleActivate() -> goi PUT /api/criteria-sets/{versionId}/activate, GET /api/criteria-sets/category/{categoryId}/active, GET /api/criteria-sets/category/{cate…
 *  300  [{BT_DAU_TICH} Kich hoat (ACTIVE)]  handleActivate() -> goi PUT /api/criteria-sets/{versionId}/activate, GET /api/criteria-sets/category/{categoryId}/active, GET /api/criteria-sets/category/{cate…
 *  319  form, nut [Luu Phien ban Moi]   handleCreateNewVersion() -> goi POST /api/criteria-sets, GET /api/criteria-sets/category/{categoryId}/active, GET /api/criteria-sets/category/{categoryId}
 *  373  [{BT_DAU_X}]                    handleRemoveCriterion() -> tren trang: criteria list
 *  387  [{BT_THEM} Them tieu chi]       handleAddCriterion() -> tren trang: criteria list
 */

export default function CriteriaManagement({ nguoiDung }) {
  const [categoryId, setCategoryId] = useState(1)
  const [activeVersion, setActiveVersion] = useState(null)
  const [allVersions, setAllVersions] = useState([])
  const [previewVersion, setPreviewVersion] = useState(null)
  const [newlyCreatedVersion, setNewlyCreatedVersion] = useState(null)
  const [autoActivateNew, setAutoActivateNew] = useState(false)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  // Form soạn phiên bản tiêu chí mới
  const [criteriaList, setCriteriaList] = useState([
    { name: 'Tính thời sự & thông tin mới', description: 'Độ nhanh nhạy và tính độc quyền của tin tức', weight: 30, maxScore: 100, sortOrder: 1 },
    { name: 'Độ chính xác & nguồn tin kiểm chứng', description: 'Có đối chiếu đa nguồn, không đưa tin sai sự thật', weight: 40, maxScore: 100, sortOrder: 2 },
    { name: 'Văn phong & chuẩn mực đạo đức', description: 'Hành văn trong sáng, tuân thủ đạo đức báo chí', weight: 30, maxScore: 100, sortOrder: 3 }
  ])

  // Tính tổng trọng số
  const totalWeight = criteriaList.reduce((sum, c) => sum + (Number(c.weight) || 0), 0)

  useEffect(() => {
    loadCategoryData(categoryId)
  }, [categoryId])

  async function loadCategoryData(catId) {
    setLoading(true)
    setError('')
    setMessage('')
    setNewlyCreatedVersion(null)
    setPreviewVersion(null)
    try {
      // 1. Tải bản ACTIVE
      try {
        const activeData = await layBoTieuChiActive(catId)
        setActiveVersion(activeData)
      } catch {
        setActiveVersion(null)
      }

      // 2. Tải toàn bộ danh sách phiên bản của thể loại này
      try {
        const versionsData = await layDanhSachBoTieuChi(catId)
        setAllVersions(Array.isArray(versionsData) ? versionsData : [])
      } catch {
        setAllVersions([])
      }
    } finally {
      setLoading(false)
    }
  }

  function handleAddCriterion() {
    setCriteriaList([
      ...criteriaList,
      { name: '', description: '', weight: 0, maxScore: 100, sortOrder: criteriaList.length + 1 }
    ])
  }

  function handleRemoveCriterion(index) {
    if (criteriaList.length <= 1) {
      alert('Bộ tiêu chí phải có ít nhất 1 tiêu chí!')
      return
    }
    setCriteriaList(criteriaList.filter((_, i) => i !== index))
  }

  function handleCriterionChange(index, field, value) {
    const updated = [...criteriaList]
    updated[index][field] = value
    setCriteriaList(updated)
  }

  async function handleCreateNewVersion(e) {
    e.preventDefault()
    setError('')
    setMessage('')
    setNewlyCreatedVersion(null)

    if (totalWeight !== 100) {
      setError(`Tổng trọng số phải bằng 100.00% (Hiện tại: ${totalWeight}%)`)
      return
    }

    try {
      const payload = {
        categoryId: Number(categoryId),
        autoActivate: Boolean(autoActivateNew),
        criteria: criteriaList.map((c, idx) => ({
          name: c.name,
          description: c.description,
          weight: Number(c.weight),
          maxScore: Number(c.maxScore) || 100,
          sortOrder: idx + 1
        }))
      }

      const created = await taoBoTieuChiDraft(payload)
      if (autoActivateNew || created.status === 'ACTIVE') {
        setMessage(`Tạo và Kích hoạt thành công Phiên bản ${created.versionNo} (ACTIVE)!`)
      } else {
        setMessage(`Tạo thành công Phiên bản ${created.versionNo} (Trạng thái: ${created.status})! Mã ID: ${created.id}`)
        setNewlyCreatedVersion(created)
      }
      await loadCategoryData(categoryId)
    } catch (err) {
      setError('Lỗi khi gửi yêu cầu: ' + err.message)
    }
  }

  async function handleActivate(versionId) {
    setError('')
    setMessage('')
    try {
      const data = await kichHoatBoTieuChi(versionId)
      setMessage(`Đã kích hoạt thành công Phiên bản ${data.versionNo} (ACTIVE)!`)
      setNewlyCreatedVersion(null)
      await loadCategoryData(categoryId)
    } catch (err) {
      setError('Lỗi kích hoạt: ' + err.message)
    }
  }

  return (
    <div className="bf02__the">
      {/* UC-14 đến UC-17; BR-QA-01: Admin cấu hình bộ tiêu chí chấm điểm */}
      <h3 className="bf02__tieu-de"><Icon>{BT_TIEU_CHI}</Icon>Cấu hình Bộ tiêu chí chấm điểm</h3>
      <p className="bf02__mo-ta">
        Thiết lập các tiêu chí chất lượng cho từng thể loại. Mỗi tiêu chí có trọng số riêng, tổng các trọng số bắt buộc bằng 100%.
      </p>

      {/* Chọn thể loại */}
      <div className="bf02__hang">
        <label className="bf02__nhan" htmlFor="tieu-chi-the-loai">Thể loại bài viết:</label>
        <select
          id="tieu-chi-the-loai"
          value={categoryId}
          onChange={(e) => setCategoryId(Number(e.target.value))}
          className="o-nhap-ngan"
        >
          <option value={1}>1 - Phóng sự điều tra</option>
          <option value={2}>2 - Phỏng vấn & Đối thoại</option>
          <option value={3}>3 - Bình luận & Xã luận</option>
          <option value={4}>4 - Tin phản ánh & Thời sự</option>
        </select>
        <button type="button" onClick={() => loadActiveVersion(categoryId)} className="nut-nhe">
          Tải lại
        </button>
      </div>

      {/* Thông báo */}
      {message && <div className="thong-bao-thanh-cong">{message}</div>}
      {error && <div className="thong-bao-loi">{error}</div>}

      {/* Hộp hành động nhanh khi vừa tạo xong bản DRAFT */}
      {newlyCreatedVersion && newlyCreatedVersion.status === 'DRAFT' && (
        <div className="bf02__thong-tin bf02__thong-tin--nut">
          <div>
            <div className="bf02__thong-tin-chinh">
              <Icon>{BT_THONG_TIN}</Icon>
              Bạn vừa tạo Phiên bản {newlyCreatedVersion.versionNo} (ID: #{newlyCreatedVersion.id}) ở trạng thái Bản nháp (DRAFT).
            </div>
            <div>
              Bấm nút bên cạnh để kích hoạt phiên bản này thành phiên bản áp dụng chính thức (ACTIVE) ngay lập tức:
            </div>
          </div>
          <button type="button" onClick={() => handleActivate(newlyCreatedVersion.id)} className="nut-chinh-fm">
            <Icon>{BT_DAU_TICH}</Icon>
            Kích hoạt (ACTIVE) ngay
          </button>
        </div>
      )}

      {/* Phiên bản ACTIVE hiện tại */}
      <div className="bf02__khung-phu">
        <h4 className="bf02__tieu-de-nho">
          <span>Phiên bản đang áp dụng (ACTIVE):</span>
          {activeVersion ? (
            <span className="nhan-tt nhan-tt--tot">Version {activeVersion.versionNo}</span>
          ) : (
            <span className="nhan-tt nhan-tt--chu-y">Chưa có phiên bản ACTIVE</span>
          )}
        </h4>

        {loading ? (
          <p className="bf02__nhat">Đang tải...</p>
        ) : activeVersion ? (
          <div>
            <p className="bf02__mo-ta">
              Mã bộ tiêu chí: <strong>#{activeVersion.id}</strong> | Ngày tạo: {new Date(activeVersion.createdAt).toLocaleString('vi-VN')}
            </p>
            <div className="bf02__cuon">
              <table className="bang-fm">
                <thead>
                  <tr>
                    <th className="bf02__giua">STT</th>
                    <th>Tên tiêu chí</th>
                    <th>Mô tả</th>
                    <th className="bf02__giua">Trọng số</th>
                  </tr>
                </thead>
                <tbody>
                  {activeVersion.criteria?.map((item, idx) => (
                    <tr key={item.id || idx}>
                      <td className="bf02__giua">{idx + 1}</td>
                      <td className="bf02__dam">{item.name}</td>
                      <td className="bf02__nhat">{item.description}</td>
                      <td className="bf02__giua bf02__trong-so">{item.weight}%</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <p className="bf02__nhat">
            Thể loại này hiện chưa có bộ tiêu chí ACTIVE nào. Vui lòng tạo phiên bản mới bên dưới và kích hoạt.
          </p>
        )}
      </div>

      {/* Danh sách các phiên bản đã tạo của Thể loại này - UC-14 */}
      <div className="bf02__khung-vien">
        <h4 className="bf02__tieu-de-nho"><Icon>{BT_DANH_SACH}</Icon>Tất cả các phiên bản của Thể loại này:</h4>
        {allVersions.length === 0 ? (
          <p className="bf02__nhat">Chưa có phiên bản nào được lưu trong hệ thống.</p>
        ) : (
          <div className="bf02__cuon">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th>Phiên bản</th>
                  <th>Mã ID</th>
                  <th>Trạng thái</th>
                  <th className="bf02__giua">Số tiêu chí</th>
                  <th>Thời gian tạo</th>
                  <th className="bf02__giua">Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {allVersions.map((v) => (
                  <tr key={v.id} className={v.status === 'ACTIVE' ? 'bf02__dong--active' : undefined}>
                    <td className="bf02__dam">Phiên bản {v.versionNo}</td>
                    <td>#{v.id}</td>
                    <td>
                      {v.status === 'ACTIVE' && (
                        <span className="nhan-tt nhan-tt--tot">ACTIVE (Đang dùng)</span>
                      )}
                      {v.status === 'DRAFT' && (
                        <span className="nhan-tt nhan-tt--chu-y">DRAFT (Bản nháp)</span>
                      )}
                      {v.status === 'RETIRED' && (
                        <span className="nhan-tt">RETIRED (Lưu trữ)</span>
                      )}
                    </td>
                    <td className="bf02__giua">
                      {v.criteria ? v.criteria.length : '—'}
                    </td>
                    <td className="bf02__nhat">
                      {v.createdAt ? new Date(v.createdAt).toLocaleString('vi-VN') : '—'}
                    </td>
                    <td className="bf02__giua">
                      {v.status === 'DRAFT' ? (
                        <button type="button" onClick={() => handleActivate(v.id)} className="nut-chinh-fm">
                          <Icon>{BT_DAU_TICH}</Icon>
                          Kích hoạt (ACTIVE)
                        </button>
                      ) : (
                        <span className="bf02__nhat">
                          {v.status === 'ACTIVE' ? 'Đang áp dụng' : 'Đã thay thế'}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Form tạo phiên bản mới - UC-15, BR-QA-02 */}
      <form onSubmit={handleCreateNewVersion}>
        <h4 className="bf02__tieu-de-nho"><Icon>{BT_THEM}</Icon>Tạo Phiên bản Bộ tiêu chí mới</h4>

        <div className="bf02__cuon">
          <table className="bang-fm">
            <thead>
              <tr>
                <th className="bf02__giua">STT</th>
                <th>Tên tiêu chí</th>
                <th>Mô tả / Hướng dẫn</th>
                <th className="bf02__giua">Trọng số (%)</th>
                <th className="bf02__giua">Xóa</th>
              </tr>
            </thead>
            <tbody>
              {criteriaList.map((crit, idx) => (
                <tr key={idx}>
                  <td className="bf02__giua">{idx + 1}</td>
                  <td>
                    <input
                      type="text"
                      required
                      placeholder="Nhập tên tiêu chí..."
                      value={crit.name}
                      onChange={(e) => handleCriterionChange(idx, 'name', e.target.value)}
                      aria-label={'Tên tiêu chí ' + (idx + 1)}
                      className="bf02__o-nhap"
                    />
                  </td>
                  <td>
                    <input
                      type="text"
                      placeholder="Mô tả tiêu chuẩn đạt..."
                      value={crit.description}
                      onChange={(e) => handleCriterionChange(idx, 'description', e.target.value)}
                      aria-label={'Mô tả tiêu chí ' + (idx + 1)}
                      className="bf02__o-nhap"
                    />
                  </td>
                  <td className="bf02__giua">
                    <input
                      type="number"
                      required
                      min={1}
                      max={100}
                      value={crit.weight}
                      onChange={(e) => handleCriterionChange(idx, 'weight', Number(e.target.value))}
                      aria-label={'Trọng số tiêu chí ' + (idx + 1)}
                      className="bf02__o-nhap bf02__o-nhap--so"
                    />
                  </td>
                  <td className="bf02__giua">
                    <button
                      type="button"
                      onClick={() => handleRemoveCriterion(idx)}
                      className="nut-thao-tac nut-xoa"
                      aria-label={'Xóa tiêu chí ' + (idx + 1)}
                    >
                      <Icon>{BT_DAU_X}</Icon>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="bf02__hang bf02__hang--deu">
          <button type="button" onClick={handleAddCriterion} className="nut-nhe">
            <Icon>{BT_THEM}</Icon>
            Thêm tiêu chí
          </button>

          <div className="bf02__tong">
            Tổng trọng số: {' '}
            <span className={totalWeight === 100 ? 'bf02__tong--dat' : 'bf02__tong--chua-dat'}>
              {totalWeight}%
              <Icon>{totalWeight === 100 ? BT_DAU_TICH : BT_DAU_X}</Icon>
              {totalWeight === 100 ? '(Hợp lệ)' : '(Bắt buộc phải đúng 100%)'}
            </span>
          </div>
        </div>

        <div className="bf02__hang">
          <label className="bf02__hop-chon">
            <input
              type="checkbox"
              checked={autoActivateNew}
              onChange={(e) => setAutoActivateNew(e.target.checked)}
            />
            <span><strong>Kích hoạt áp dụng (ACTIVE) ngay lập tức</strong> sau khi lưu phiên bản này</span>
          </label>
        </div>

        <button type="submit" disabled={totalWeight !== 100} className="nut-chinh-fm">
          Lưu Phiên bản Mới
        </button>
      </form>
    </div>
  )
}
