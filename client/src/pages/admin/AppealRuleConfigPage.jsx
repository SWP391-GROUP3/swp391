import { useState, useEffect } from 'react'
import { getActiveConfig, getAllConfigs, createConfig } from '../../api/appealRuleConfigApi.js'

export default function AppealRuleConfigPage() {
  const [activeConfig, setActiveConfig] = useState(null)
  const [historyList, setHistoryList] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [successMsg, setSuccessMsg] = useState('')

  // Modal tao moi
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [form, setForm] = useState({
    appealWindowDays: 7,
    maxAppealsPerArticle: 1,
    varianceThreshold: 5.0,
    effectiveFrom: '',
  })
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState('')

  useEffect(() => {
    taiDuLieu()
  }, [])

  async function taiDuLieu() {
    setLoading(true)
    setError('')
    try {
      const [active, list] = await Promise.all([
        getActiveConfig().catch(() => null),
        getAllConfigs().catch(() => []),
      ])
      setActiveConfig(active)
      setHistoryList(list)
    } catch (err) {
      setError(err.message || 'Lỗi khi tải dữ liệu cấu hình')
    } finally {
      setLoading(false)
    }
  }

  function handleOpenModal() {
    // Mac dinh ngay mai
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    const dateStr = tomorrow.toISOString().split('T')[0]

    setForm({
      appealWindowDays: 7,
      maxAppealsPerArticle: 1,
      varianceThreshold: 5.0,
      effectiveFrom: dateStr,
    })
    setFormError('')
    setIsModalOpen(true)
  }

  async function handleFormSubmit(e) {
    e.preventDefault()
    setFormError('')
    setSubmitting(true)

    try {
      await createConfig({
        appealWindowDays: Number(form.appealWindowDays),
        maxAppealsPerArticle: Number(form.maxAppealsPerArticle),
        varianceThreshold: Number(form.varianceThreshold),
        effectiveFrom: form.effectiveFrom,
      })
      setSuccessMsg('Tạo phiên bản cấu hình khiếu nại mới thành công!')
      setIsModalOpen(false)
      await taiDuLieu()
      setTimeout(() => setSuccessMsg(''), 4000)
    } catch (err) {
      setFormError(err.message || 'Lỗi khi lưu cấu hình')
    } finally {
      setSubmitting(false)
    }
  }

  const homNay = new Date().toISOString().split('T')[0]

  return (
    <div style={{ maxWidth: 1000, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
        <div>
          {/* UC-44: Cấu hình quy tắc khiếu nại */}
          <h2 style={{ margin: 0, fontSize: 20 }}>Cấu hình quy tắc khiếu nại &amp; Tham số chấm</h2>
          <p style={{ margin: '4px 0 0', color: 'var(--muc-nhat)', fontSize: 13.5 }}>
            Quản trị thời hạn khiếu nại, số lượt tối đa và ngưỡng chênh lệch điểm (T) theo phiên bản hiệu lực.
          </p>
        </div>
        <button
          type="button"
          className="nut-chinh"
          style={{ width: 'auto', padding: '8px 16px' }}
          onClick={handleOpenModal}
        >
          + Tạo phiên bản cấu hình mới
        </button>
      </div>

      {error && <div className="thong-bao-loi">{error}</div>}
      {successMsg && (
        <div style={{ padding: '9px 13px', background: '#e7f3ec', color: 'var(--chinh)', border: '1px solid #bbf7d0', borderRadius: 6, marginBottom: 16 }}>
          {successMsg}
        </div>
      )}

      {/* THE HIEN THI CAU HINH HIEN TAI */}
      <div style={{ background: 'var(--nen-the)', border: '1px solid var(--vien)', borderRadius: 8, padding: 20, marginBottom: 24 }}>
        <h3 style={{ margin: '0 0 14px', fontSize: 15, fontWeight: 600, color: 'var(--muc)' }}>
          Cấu hình đang áp dụng hiện tại
        </h3>
        {loading ? (
          <p style={{ color: 'var(--muc-nhat)', margin: 0 }}>Đang tải cấu hình...</p>
        ) : activeConfig ? (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16 }}>
            <div style={{ padding: 12, background: 'var(--nen)', borderRadius: 6 }}>
              <div style={{ fontSize: 12.5, color: 'var(--muc-nhat)' }}>Thời hạn khiếu nại</div>
              <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--chinh)', marginTop: 4 }}>
                {activeConfig.appealWindowDays} ngày
              </div>
              <div style={{ fontSize: 11.5, color: 'var(--muc-nhat)', marginTop: 2 }}>Từ lúc công bố điểm</div>
            </div>

            <div style={{ padding: 12, background: 'var(--nen)', borderRadius: 6 }}>
              <div style={{ fontSize: 12.5, color: 'var(--muc-nhat)' }}>Số lần khiếu nại tối đa</div>
              <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--chinh)', marginTop: 4 }}>
                {activeConfig.maxAppealsPerArticle} lần
              </div>
              <div style={{ fontSize: 11.5, color: 'var(--muc-nhat)', marginTop: 2 }}>Trên mỗi bài viết</div>
            </div>

            <div style={{ padding: 12, background: 'var(--nen)', borderRadius: 6 }}>
              <div style={{ fontSize: 12.5, color: 'var(--muc-nhat)' }}>Ngưỡng chênh lệch (T)</div>
              <div style={{ fontSize: 20, fontWeight: 700, color: 'var(--chinh)', marginTop: 4 }}>
                {activeConfig.varianceThreshold} điểm
              </div>
              <div style={{ fontSize: 11.5, color: 'var(--muc-nhat)', marginTop: 2 }}>Kích hoạt chấm lại lần 2 (S2)</div>
            </div>

            <div style={{ padding: 12, background: 'var(--nen)', borderRadius: 6 }}>
              <div style={{ fontSize: 12.5, color: 'var(--muc-nhat)' }}>Ngày bắt đầu hiệu lực</div>
              <div style={{ fontSize: 16, fontWeight: 600, marginTop: 6 }}>
                {activeConfig.effectiveFrom}
              </div>
              <div style={{ fontSize: 11.5, color: 'var(--muc-nhat)', marginTop: 2 }}>Phiên bản #{activeConfig.id}</div>
            </div>
          </div>
        ) : (
          <p style={{ color: 'var(--do)', margin: 0 }}>Chưa có cấu hình nào có hiệu lực cho ngày hôm nay.</p>
        )}
      </div>

      {/* BANG LICH SU PHIEN BAN */}
      <div style={{ background: 'var(--nen-the)', border: '1px solid var(--vien)', borderRadius: 8, padding: 20 }}>
        <h3 style={{ margin: '0 0 14px', fontSize: 15, fontWeight: 600, color: 'var(--muc)' }}>
          Lịch sử các phiên bản cấu hình
        </h3>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13.5 }}>
          <thead>
            <tr style={{ background: 'var(--nen)', textAlign: 'left', borderBottom: '1px solid var(--vien)' }}>
              <th style={{ padding: '10px 12px' }}>Mã PB</th>
              <th style={{ padding: '10px 12px' }}>Ngày hiệu lực</th>
              <th style={{ padding: '10px 12px' }}>Hạn khiếu nại</th>
              <th style={{ padding: '10px 12px' }}>Lượt tối đa</th>
              <th style={{ padding: '10px 12px' }}>Ngưỡng T</th>
              <th style={{ padding: '10px 12px' }}>Trạng thái</th>
            </tr>
          </thead>
          <tbody>
            {historyList.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ padding: 16, textAlign: 'center', color: 'var(--muc-nhat)' }}>
                  Chưa có phiên bản nào trong hệ thống
                </td>
              </tr>
            ) : (
              historyList.map((cfg) => {
                const isCurrentlyActive = activeConfig?.id === cfg.id
                const isFuture = cfg.effectiveFrom > homNay

                let badgeText = 'Đã qua'
                let badgeStyle = { background: '#f1f5f9', color: '#64748b' }
                if (isCurrentlyActive) {
                  badgeText = 'Đang áp dụng'
                  badgeStyle = { background: '#dcfce7', color: '#15803d' }
                } else if (isFuture) {
                  badgeText = 'Sắp áp dụng'
                  badgeStyle = { background: '#eff6ff', color: '#1d4ed8' }
                }

                return (
                  <tr key={cfg.id} style={{ borderBottom: '1px solid var(--vien)' }}>
                    <td style={{ padding: '10px 12px', fontWeight: 600 }}>#{cfg.id}</td>
                    <td style={{ padding: '10px 12px' }}>{cfg.effectiveFrom}</td>
                    <td style={{ padding: '10px 12px' }}>{cfg.appealWindowDays} ngày</td>
                    <td style={{ padding: '10px 12px' }}>{cfg.maxAppealsPerArticle} lần</td>
                    <td style={{ padding: '10px 12px' }}>{cfg.varianceThreshold} điểm</td>
                    <td style={{ padding: '10px 12px' }}>
                      <span style={{ ...badgeStyle, padding: '3px 8px', borderRadius: 999, fontSize: 11.5, fontWeight: 600 }}>
                        {badgeText}
                      </span>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>

      {/* MODAL TAO PHIEN BAN CAU HINH MOI */}
      {isModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
          <div style={{ background: '#fff', borderRadius: 8, padding: 24, width: '100%', maxWidth: 440, boxShadow: '0 4px 16px rgba(0,0,0,0.15)' }}>
            <h3 style={{ margin: '0 0 16px', fontSize: 17, fontWeight: 600 }}>Tạo phiên bản cấu hình mới</h3>
            {formError && <div className="thong-bao-loi">{formError}</div>}

            <form onSubmit={handleFormSubmit}>
              <label className="nhan">Thời hạn khiếu nại (ngày)</label>
              <input
                type="number"
                min="1"
                className="o-nhap"
                value={form.appealWindowDays}
                onChange={(e) => setForm({ ...form, appealWindowDays: e.target.value })}
                required
              />

              <label className="nhan">Số lần khiếu nại tối đa / bài</label>
              <input
                type="number"
                min="1"
                className="o-nhap"
                value={form.maxAppealsPerArticle}
                onChange={(e) => setForm({ ...form, maxAppealsPerArticle: e.target.value })}
                required
              />

              <label className="nhan">Ngưỡng chênh lệch điểm T (thang 0-100)</label>
              <input
                type="number"
                step="0.1"
                min="0"
                max="100"
                className="o-nhap"
                value={form.varianceThreshold}
                onChange={(e) => setForm({ ...form, varianceThreshold: e.target.value })}
                required
              />

              <label className="nhan">Ngày bắt đầu áp dụng hiệu lực</label>
              <input
                type="date"
                className="o-nhap"
                value={form.effectiveFrom}
                onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })}
                required
              />

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 16 }}>
                <button
                  type="button"
                  className="nut-thoat"
                  onClick={() => setIsModalOpen(false)}
                  disabled={submitting}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="nut-chinh"
                  style={{ width: 'auto', padding: '8px 18px' }}
                  disabled={submitting}
                >
                  {submitting ? 'Đang lưu...' : 'Lưu cấu hình'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
