import { useEffect, useState } from 'react'
import { getMediaFactors, updateMediaFactor } from '../../api/royaltyApi.js'
import { formatNumber } from '../../utils/formatNumber.js'
import { mediaKindLabel } from '../../utils/royaltyLabels.js'
import '../../styles/Royalty.css'

/** He so: so duong, toi da 3 chu so phan nguyen va 2 chu so thap phan (cot DECIMAL(5,2)). */
const FACTOR_PATTERN = /^\d{1,3}(\.\d{1,2})?$/

/**
 * UC-56 - "He so anh/video" (Admin).
 * Moi loai anh/video co 1 he so co dinh (BR-RY-04). Thu ky cham anh se dung he so nay.
 * Bam "Sua" tren 1 dong -> o nhap hien ngay tai dong do, "Luu" de goi API.
 */
export default function MediaFactors() {
  const [factors, setFactors] = useState(null)   // null = dang tai
  const [loadError, setLoadError] = useState('')

  const [editingId, setEditingId] = useState(null)   // id dong dang sua, null = khong sua dong nao
  const [editValue, setEditValue] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  useEffect(() => {
    let isMounted = true
    getMediaFactors()
      .then((data) => { if (isMounted) setFactors(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
    return () => { isMounted = false }
  }, [])

  function startEdit(mediaFactor) {
    setEditingId(mediaFactor.id)
    setEditValue(String(mediaFactor.factor))
    setSaveError('')
    setSuccessMessage('')
  }

  function cancelEdit() {
    setEditingId(null)
    setSaveError('')
  }

  async function saveEdit(mediaFactor) {
    // Kiem tra nhanh o trinh duyet; server van kiem lai bang @Valid
    if (!FACTOR_PATTERN.test(editValue) || Number(editValue) <= 0) {
      setSaveError('Hệ số phải lớn hơn 0, tối đa 2 chữ số thập phân (vd 1.50).')
      return
    }

    setIsSaving(true)
    try {
      const updated = await updateMediaFactor(mediaFactor.id, Number(editValue))
      // Thay dong vua sua bang du lieu server tra ve, cac dong khac giu nguyen
      setFactors(factors.map((item) => (item.id === updated.id ? updated : item)))
      setEditingId(null)
      setSaveError('')
      setSuccessMessage(`Đã cập nhật hệ số ${mediaKindLabel(updated.mediaKind)} thành ${formatNumber(updated.factor)}.`)
    } catch (err) {
      setSaveError(err.message)
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div>
      <div className="royalty__header">
        <h1 className="royalty__title">Hệ số ảnh/video</h1>
        <p className="royalty__subtitle">
          Mỗi loại ảnh/video có một hệ số cố định, dùng khi Thư ký chấm nhuận bút ảnh/video. Ảnh dùng lại hoặc lấy từ nguồn ngoài không được tính.
        </p>
      </div>

      <div className="royalty__card">
        {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
        {saveError && <p className="thong-bao-loi" role="alert">{saveError}</p>}
        {successMessage && <p className="thong-bao-thanh-cong" role="status">{successMessage}</p>}
        {!factors && !loadError && <p className="royalty__empty">Đang tải…</p>}
        {factors && factors.length === 0 && <p className="royalty__empty">Chưa có loại ảnh/video nào.</p>}

        {factors && factors.length > 0 && (
          <div className="royalty__scroll">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th>Loại</th>
                  <th>Mã</th>
                  <th className="royalty__number">Hệ số</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {factors.map((mediaFactor) => (
                  <tr key={mediaFactor.id}>
                    <td className="royalty__strong">{mediaKindLabel(mediaFactor.mediaKind)}</td>
                    <td className="royalty__date">{mediaFactor.mediaKind}</td>
                    {editingId === mediaFactor.id ? (
                      <>
                        <td className="royalty__number">
                          <input
                            type="number" min="0.01" step="0.01"
                            className="royalty__input royalty__input--small"
                            value={editValue}
                            onChange={(event) => setEditValue(event.target.value)}
                            aria-label={`Hệ số mới cho ${mediaKindLabel(mediaFactor.mediaKind)}`}
                            autoFocus
                          />
                        </td>
                        <td className="royalty__actions">
                          <button type="button" className="nut-chinh-fm" disabled={isSaving}
                            onClick={() => saveEdit(mediaFactor)}>
                            {isSaving ? 'Đang lưu…' : 'Lưu'}
                          </button>
                          <button type="button" className="nut-nhe" disabled={isSaving} onClick={cancelEdit}>
                            Huỷ
                          </button>
                        </td>
                      </>
                    ) : (
                      <>
                        <td className="royalty__number royalty__strong">{formatNumber(mediaFactor.factor)}</td>
                        <td className="royalty__actions">
                          <button type="button" className="nut-nhe" onClick={() => startEdit(mediaFactor)}>
                            Sửa
                          </button>
                        </td>
                      </>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
