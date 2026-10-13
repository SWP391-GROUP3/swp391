import { useState, useEffect } from 'react'

export default function CriteriaManagement({ nguoiDung }) {
  const [categoryId, setCategoryId] = useState(1)
  const [activeVersion, setActiveVersion] = useState(null)
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
    loadActiveVersion(categoryId)
  }, [categoryId])

  async function loadActiveVersion(catId) {
    setLoading(true)
    setError('')
    setMessage('')
    try {
      const res = await fetch(`/api/criteria-sets/active/${catId}`)
      if (res.ok) {
        const data = await res.json()
        setActiveVersion(data)
      } else if (res.status === 404) {
        setActiveVersion(null)
      } else {
        const txt = await res.text()
        setError(txt || 'Lỗi khi tải bộ tiêu chí')
      }
    } catch (err) {
      setError('Không thể kết nối đến máy chủ: ' + err.message)
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

    if (totalWeight !== 100) {
      setError(`Tổng trọng số phải bằng 100.00% (Hiện tại: ${totalWeight}%)`)
      return
    }

    try {
      const payload = {
        categoryId: Number(categoryId),
        criteria: criteriaList.map((c, idx) => ({
          name: c.name,
          description: c.description,
          weight: Number(c.weight),
          maxScore: Number(c.maxScore) || 100,
          sortOrder: idx + 1
        }))
      }

      const res = await fetch('/api/criteria-sets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      })

      if (res.ok) {
        const created = await res.json()
        setMessage(`Tạo thành công Phiên bản ${created.versionNo} (Trạng thái: ${created.status})! Mã ID: ${created.id}`)
        // Nếu chưa có bản ACTIVE nào, hỏi kích hoạt
        if (!activeVersion && created.id) {
          if (confirm('Thể loại này chưa có phiên bản áp dụng. Bạn có muốn Kích hoạt (ACTIVE) phiên bản này ngay không?')) {
            await handleActivate(created.id)
          }
        }
      } else {
        const txt = await res.text()
        setError(txt || 'Tạo phiên bản thất bại')
      }
    } catch (err) {
      setError('Lỗi khi gửi yêu cầu: ' + err.message)
    }
  }

  async function handleActivate(versionId) {
    setError('')
    setMessage('')
    try {
      const res = await fetch(`/api/criteria-sets/${versionId}/activate`, {
        method: 'PUT'
      })
      if (res.ok) {
        const data = await res.json()
        setMessage(`Đã kích hoạt thành công Phiên bản ${data.versionNo}!`)
        setActiveVersion(data)
      } else {
        const txt = await res.text()
        setError(txt || 'Kích hoạt thất bại')
      }
    } catch (err) {
      setError('Lỗi kích hoạt: ' + err.message)
    }
  }

  return (
    <div style={{ padding: '20px', background: '#fff', borderRadius: '8px', border: '1px solid #dfe3e8', marginBottom: '24px' }}>
      <h3 style={{ marginTop: 0, color: '#1b2430' }}>⚙️ Cấu hình Bộ tiêu chí chấm điểm (Admin - UC-14 đến UC-17)</h3>
      <p style={{ color: '#64748b', fontSize: '14px' }}>
        Thiết lập các tiêu chí chất lượng cho từng thể loại. Mỗi tiêu chí có trọng số riêng, tổng các trọng số bắt buộc bằng 100% (BR-QA-01).
      </p>

      {/* Chọn thể loại */}
      <div style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '12px' }}>
        <label style={{ fontWeight: '600', fontSize: '14px' }}>Thể loại bài viết:</label>
        <select
          value={categoryId}
          onChange={(e) => setCategoryId(Number(e.target.value))}
          style={{ padding: '8px 12px', borderRadius: '6px', border: '1px solid #dfe3e8', fontSize: '14px' }}
        >
          <option value={1}>1 - Phóng sự điều tra</option>
          <option value={2}>2 - Phỏng vấn & Đối thoại</option>
          <option value={3}>3 - Bình luận & Xã luận</option>
          <option value={4}>4 - Tin phản ánh & Thời sự</option>
        </select>
        <button
          type="button"
          onClick={() => loadActiveVersion(categoryId)}
          style={{ padding: '8px 14px', background: '#f4f6f8', border: '1px solid #dfe3e8', borderRadius: '6px', cursor: 'pointer' }}
        >
          Tải lại
        </button>
      </div>

      {/* Thông báo */}
      {message && <div style={{ padding: '10px 14px', background: '#ecfdf5', color: '#065f46', border: '1px solid #a7f3d0', borderRadius: '6px', marginBottom: '16px' }}>{message}</div>}
      {error && <div style={{ padding: '10px 14px', background: '#fef2f2', color: '#991b1b', border: '1px solid #fecaca', borderRadius: '6px', marginBottom: '16px' }}>{error}</div>}

      {/* Phiên bản ACTIVE hiện tại */}
      <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0', marginBottom: '24px' }}>
        <h4 style={{ margin: '0 0 10px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span>Phiên bản đang áp dụng (ACTIVE):</span>
          {activeVersion ? (
            <span style={{ background: '#10b981', color: '#fff', fontSize: '12px', padding: '2px 8px', borderRadius: '12px' }}>
              Version {activeVersion.versionNo}
            </span>
          ) : (
            <span style={{ background: '#f59e0b', color: '#fff', fontSize: '12px', padding: '2px 8px', borderRadius: '12px' }}>
              Chưa có phiên bản ACTIVE
            </span>
          )}
        </h4>

        {loading ? (
          <p style={{ color: '#64748b' }}>Đang tải...</p>
        ) : activeVersion ? (
          <div>
            <p style={{ margin: '4px 0', fontSize: '13px', color: '#64748b' }}>
              Mã bộ tiêu chí: <strong>#{activeVersion.id}</strong> | Ngày tạo: {new Date(activeVersion.createdAt).toLocaleString('vi-VN')}
            </p>
            <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px', fontSize: '14px' }}>
              <thead>
                <tr style={{ background: '#edf2f7', textAlign: 'left' }}>
                  <th style={{ padding: '8px', border: '1px solid #cbd5e1' }}>STT</th>
                  <th style={{ padding: '8px', border: '1px solid #cbd5e1' }}>Tên tiêu chí</th>
                  <th style={{ padding: '8px', border: '1px solid #cbd5e1' }}>Mô tả</th>
                  <th style={{ padding: '8px', border: '1px solid #cbd5e1', textAlign: 'center' }}>Trọng số</th>
                </tr>
              </thead>
              <tbody>
                {activeVersion.criteria?.map((item, idx) => (
                  <tr key={item.id || idx}>
                    <td style={{ padding: '8px', border: '1px solid #cbd5e1', textAlign: 'center' }}>{idx + 1}</td>
                    <td style={{ padding: '8px', border: '1px solid #cbd5e1', fontWeight: '500' }}>{item.name}</td>
                    <td style={{ padding: '8px', border: '1px solid #cbd5e1', color: '#475569' }}>{item.description}</td>
                    <td style={{ padding: '8px', border: '1px solid #cbd5e1', textAlign: 'center', fontWeight: '600', color: '#047857' }}>
                      {item.weight}%
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p style={{ color: '#64748b', fontSize: '14px', margin: 0 }}>
            Thể loại này hiện chưa có bộ tiêu chí ACTIVE nào. Vui lòng tạo phiên bản mới bên dưới và kích hoạt.
          </p>
        )}
      </div>

      {/* Form tạo phiên bản mới */}
      <form onSubmit={handleCreateNewVersion}>
        <h4 style={{ margin: '0 0 12px' }}>➕ Tạo Phiên bản Bộ tiêu chí mới (BR-QA-02)</h4>

        <div style={{ marginBottom: '14px', overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
            <thead>
              <tr style={{ background: '#f1f5f9', textAlign: 'left' }}>
                <th style={{ padding: '8px', border: '1px solid #cbd5e1', width: '50px' }}>STT</th>
                <th style={{ padding: '8px', border: '1px solid #cbd5e1' }}>Tên tiêu chí</th>
                <th style={{ padding: '8px', border: '1px solid #cbd5e1' }}>Mô tả / Hướng dẫn</th>
                <th style={{ padding: '8px', border: '1px solid #cbd5e1', width: '120px', textAlign: 'center' }}>Trọng số (%)</th>
                <th style={{ padding: '8px', border: '1px solid #cbd5e1', width: '60px', textAlign: 'center' }}>Xóa</th>
              </tr>
            </thead>
            <tbody>
              {criteriaList.map((crit, idx) => (
                <tr key={idx}>
                  <td style={{ padding: '8px', border: '1px solid #cbd5e1', textAlign: 'center' }}>{idx + 1}</td>
                  <td style={{ padding: '8px', border: '1px solid #cbd5e1' }}>
                    <input
                      type="text"
                      required
                      placeholder="Nhập tên tiêu chí..."
                      value={crit.name}
                      onChange={(e) => handleCriterionChange(idx, 'name', e.target.value)}
                      style={{ width: '100%', padding: '6px 8px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                    />
                  </td>
                  <td style={{ padding: '8px', border: '1px solid #cbd5e1' }}>
                    <input
                      type="text"
                      placeholder="Mô tả tiêu chuẩn đạt..."
                      value={crit.description}
                      onChange={(e) => handleCriterionChange(idx, 'description', e.target.value)}
                      style={{ width: '100%', padding: '6px 8px', borderRadius: '4px', border: '1px solid #cbd5e1', boxSizing: 'border-box' }}
                    />
                  </td>
                  <td style={{ padding: '8px', border: '1px solid #cbd5e1', textAlign: 'center' }}>
                    <input
                      type="number"
                      required
                      min={1}
                      max={100}
                      value={crit.weight}
                      onChange={(e) => handleCriterionChange(idx, 'weight', Number(e.target.value))}
                      style={{ width: '70px', padding: '6px 8px', textAlign: 'center', borderRadius: '4px', border: '1px solid #cbd5e1' }}
                    />
                  </td>
                  <td style={{ padding: '8px', border: '1px solid #cbd5e1', textAlign: 'center' }}>
                    <button
                      type="button"
                      onClick={() => handleRemoveCriterion(idx)}
                      style={{ background: '#fee2e2', color: '#b91c1c', border: 'none', borderRadius: '4px', padding: '4px 8px', cursor: 'pointer' }}
                    >
                      ✕
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <button
            type="button"
            onClick={handleAddCriterion}
            style={{ padding: '8px 14px', background: '#f8fafc', border: '1px dashed #64748b', borderRadius: '6px', cursor: 'pointer', fontSize: '13px' }}
          >
            + Thêm tiêu chí
          </button>

          <div style={{ fontSize: '15px', fontWeight: '600' }}>
            Tổng trọng số: {' '}
            <span style={{ color: totalWeight === 100 ? '#16a34a' : '#dc2626', fontSize: '16px' }}>
              {totalWeight}% {totalWeight === 100 ? '✓ (Hợp lệ)' : '✗ (Bắt buộc phải đúng 100%)'}
            </span>
          </div>
        </div>

        <button
          type="submit"
          disabled={totalWeight !== 100}
          style={{
            padding: '10px 20px',
            background: totalWeight === 100 ? '#14532d' : '#94a3b8',
            color: '#fff',
            border: 'none',
            borderRadius: '6px',
            fontWeight: '600',
            cursor: totalWeight === 100 ? 'pointer' : 'not-allowed'
          }}
        >
          Lưu Phiên bản Mới
        </button>
      </form>
    </div>
  )
}
