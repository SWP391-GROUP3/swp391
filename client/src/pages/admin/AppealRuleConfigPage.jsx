import { useState, useEffect } from 'react'
import { getActiveConfig, getAllConfigs, createConfig } from '../../api/appealRuleConfigApi.js'
import '../../styles/AppealRuleConfig.css'

/**
 * Man Admin cau hinh luat khieu nai (UC-44): xem cau hinh dang ap dung, lich su cac lan doi va them
 * cau hinh moi (so ngay duoc khieu nai, nguong chenh diem...).
 *
 * MO TRANG NAY TAI: /appeal-rules
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  AppealRuleConfigPage()          appeal rule config page
 *   42    useEffect                     tai du lieu khi mo trang
 *   46    taiDuLieu()                   tai du lieu
 *   66    handleOpenModal()             xu ly open modal
 *   82    handleFormSubmit()            xu ly form submit
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  117  [+ Tao phien ban cau hinh moi]  handleOpenModal() -> tren trang: form, dong/xoa form error, mo kiem tra modal open
 *  224  form, nut [Dang luu... / Luu cau hinh]  handleFormSubmit() -> goi POST /api/appeal-rule-configs, GET /api/appeal-rule-configs/active, GET /api/appeal-rule-configs
 *  274  [Huy]                           tren trang: dong/xoa kiem tra modal open
 */

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
        getActiveConfig().catch((loi) => {
          if (loi.status === 404) return null
          throw loi
        }),
        getAllConfigs(),
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
    <div className="cau-hinh-kn">
      <div className="cau-hinh-kn__dau">
        <div>
          {/* UC-44: Cấu hình quy tắc khiếu nại */}
          <h2 className="cau-hinh-kn__tieu-de">Cấu hình quy tắc khiếu nại &amp; Tham số chấm</h2>
          <p className="cau-hinh-kn__phu-de">
            Quản trị thời hạn khiếu nại, số lượt tối đa và ngưỡng chênh lệch điểm (T) theo phiên bản hiệu lực.
          </p>
        </div>
        <button type="button" className="nut-chinh-fm" onClick={handleOpenModal}>
          + Tạo phiên bản cấu hình mới
        </button>
      </div>

      {error && <div className="thong-bao-loi">{error}</div>}
      {successMsg && <div className="thong-bao-thanh-cong">{successMsg}</div>}

      {/* THE HIEN THI CAU HINH HIEN TAI */}
      <div className="cau-hinh-kn__the">
        <h3 className="cau-hinh-kn__tieu-de-the">Cấu hình đang áp dụng hiện tại</h3>
        {loading ? (
          <p className="cau-hinh-kn__nhat">Đang tải cấu hình...</p>
        ) : activeConfig ? (
          <div className="cau-hinh-kn__luoi">
            <div className="cau-hinh-kn__o">
              <div className="cau-hinh-kn__o-nhan">Thời hạn khiếu nại</div>
              <div className="cau-hinh-kn__o-so">{activeConfig.appealWindowDays} ngày</div>
              <div className="cau-hinh-kn__o-ghi-chu">Từ lúc công bố điểm</div>
            </div>

            <div className="cau-hinh-kn__o">
              <div className="cau-hinh-kn__o-nhan">Số lần khiếu nại tối đa</div>
              <div className="cau-hinh-kn__o-so">{activeConfig.maxAppealsPerArticle} lần</div>
              <div className="cau-hinh-kn__o-ghi-chu">Trên mỗi bài viết</div>
            </div>

            <div className="cau-hinh-kn__o">
              <div className="cau-hinh-kn__o-nhan">Ngưỡng chênh lệch (T)</div>
              <div className="cau-hinh-kn__o-so">{activeConfig.varianceThreshold} điểm</div>
              <div className="cau-hinh-kn__o-ghi-chu">Kích hoạt chấm lại lần 2 (S2)</div>
            </div>

            <div className="cau-hinh-kn__o">
              <div className="cau-hinh-kn__o-nhan">Ngày bắt đầu hiệu lực</div>
              <div className="cau-hinh-kn__o-so cau-hinh-kn__o-so--ngay">{activeConfig.effectiveFrom}</div>
              <div className="cau-hinh-kn__o-ghi-chu">Phiên bản #{activeConfig.id}</div>
            </div>
          </div>
        ) : (
          <p className="cau-hinh-kn__loi">Chưa có cấu hình nào có hiệu lực cho ngày hôm nay.</p>
        )}
      </div>

      {/* BANG LICH SU PHIEN BAN */}
      <div className="cau-hinh-kn__the">
        <h3 className="cau-hinh-kn__tieu-de-the">Lịch sử các phiên bản cấu hình</h3>
        <div className="cau-hinh-kn__cuon">
          <table className="bang-fm">
            <thead>
              <tr>
                <th>Mã PB</th>
                <th>Ngày hiệu lực</th>
                <th>Hạn khiếu nại</th>
                <th>Lượt tối đa</th>
                <th>Ngưỡng T</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {historyList.length === 0 ? (
                <tr>
                  <td colSpan={6} className="cau-hinh-kn__trong">
                    Chưa có phiên bản nào trong hệ thống
                  </td>
                </tr>
              ) : (
                historyList.map((cfg) => {
                  const isCurrentlyActive = activeConfig?.id === cfg.id
                  const isFuture = cfg.effectiveFrom > homNay

                  let badgeText = 'Đã qua'
                  let badgeClass = 'nhan-tt'
                  if (isCurrentlyActive) {
                    badgeText = 'Đang áp dụng'
                    badgeClass = 'nhan-tt nhan-tt--tot'
                  } else if (isFuture) {
                    badgeText = 'Sắp áp dụng'
                    badgeClass = 'nhan-tt nhan-tt--thong-tin'
                  }

                  return (
                    <tr key={cfg.id}>
                      <td className="cau-hinh-kn__dam">#{cfg.id}</td>
                      <td>{cfg.effectiveFrom}</td>
                      <td>{cfg.appealWindowDays} ngày</td>
                      <td>{cfg.maxAppealsPerArticle} lần</td>
                      <td>{cfg.varianceThreshold} điểm</td>
                      <td>
                        <span className={badgeClass}>{badgeText}</span>
                      </td>
                    </tr>
                  )
                })
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* MODAL TAO PHIEN BAN CAU HINH MOI */}
      {isModalOpen && (
        <div className="lop-phu-modal">
          <div className="hop-modal" role="dialog" aria-modal="true" aria-labelledby="cau-hinh-kn-modal-tieu-de">
            <h3 id="cau-hinh-kn-modal-tieu-de" className="cau-hinh-kn__tieu-de-modal">Tạo phiên bản cấu hình mới</h3>
            {formError && <div className="thong-bao-loi">{formError}</div>}

            <form onSubmit={handleFormSubmit}>
              <label className="nhan" htmlFor="ckn-so-ngay">Thời hạn khiếu nại (ngày)</label>
              <input
                id="ckn-so-ngay"
                type="number"
                min="1"
                className="o-nhap"
                value={form.appealWindowDays}
                onChange={(e) => setForm({ ...form, appealWindowDays: e.target.value })}
                required
              />

              <label className="nhan" htmlFor="ckn-so-lan">Số lần khiếu nại tối đa / bài</label>
              <input
                id="ckn-so-lan"
                type="number"
                min="1"
                className="o-nhap"
                value={form.maxAppealsPerArticle}
                onChange={(e) => setForm({ ...form, maxAppealsPerArticle: e.target.value })}
                required
              />

              <label className="nhan" htmlFor="ckn-nguong">Ngưỡng chênh lệch điểm T (thang 0-100)</label>
              <input
                id="ckn-nguong"
                type="number"
                step="0.1"
                min="0"
                max="100"
                className="o-nhap"
                value={form.varianceThreshold}
                onChange={(e) => setForm({ ...form, varianceThreshold: e.target.value })}
                required
              />

              <label className="nhan" htmlFor="ckn-ngay">Ngày bắt đầu áp dụng hiệu lực</label>
              <input
                id="ckn-ngay"
                type="date"
                className="o-nhap"
                value={form.effectiveFrom}
                onChange={(e) => setForm({ ...form, effectiveFrom: e.target.value })}
                required
              />

              <div className="modal-footer">
                <button
                  type="button"
                  className="nut-thoat"
                  onClick={() => setIsModalOpen(false)}
                  disabled={submitting}
                >
                  Hủy
                </button>
                <button type="submit" className="nut-chinh-fm" disabled={submitting}>
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
