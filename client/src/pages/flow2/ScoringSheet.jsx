import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  moPhieuCham,
  doiTheLoaiBaiViet,
  luuDiemTieuChi,
  nopPhieuCham
} from '../../api/assessmentApi.js'
import Icon from '../../components/common/Icon.jsx'
import { BT_BAI_DUOC_GIAO, BT_TIEU_CHI, BT_DAU_TICH, BT_DAU_X } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

/**
 * Phieu cham cua BTV (UC-22...28): doi the loai truoc khi cham, cham diem va nhan xet tung tieu chi,
 * tinh tong ngay tren trang, luu nhap, roi chon Chap nhan / Tra sua / Tu choi.
 *
 * MO TRANG NAY TAI: /assigned-articles/:articleId
 *
 * MUC LUC (so dong - ten - tac dung)
 *   43  DANH_SACH_THE_LOAI              hang so
 *   54  ScoringSheet()                  scoring sheet
 *   73    useEffect                     tai du lieu khi mo trang
 *   79    loadScoringSheet()            tai scoring sheet
 *  103    handleScoreChange()           Cap nhat diem cua 1 tieu chi
 *  110    handleCommentChange()         Cap nhat nhan xet cua 1 tieu chi
 *  117    handleChangeCategory()        UC-23: BTV doi the loai bai viet
 *  143    handleSaveDraft()             UC-24, UC-25: Luu nhap diem tieu chi
 *  175    handleOpenDecisionModal()     Mo modal de nop quyet dinh
 *  190    handleSubmitDecision()        UC-26, UC-27, UC-28: Nop phieu cham
 *  218    handleGoBack()                xu ly go back
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  228  [<- Quay lai danh sach bai duoc giao]  handleGoBack() -> chuyen sang /assigned-articles (EditorTaskQueue); bao len component cha (onBack)
 *  325  [Doi the loai]                  handleChangeCategory() UC-23: BTV doi the loai bai viet -> hoi xac nhan; goi POST /api/scoring-attempts/article/{articleId}/change-category
 *  408  [Luu nhap diem]                 handleSaveDraft() UC-24, UC-25: Luu nhap diem tieu chi -> goi PUT /api/scoring-attempts/article/{articleId}/scores
 *  417  [Yeu cau sua (RETURN)]          handleOpenDecisionModal() Mo modal de nop quyet dinh -> tren trang: dong/xoa error, selected decision, dong/xoa decision note
 *  427  [Tu choi bai (REJECT)]          handleOpenDecisionModal() Mo modal de nop quyet dinh -> tren trang: dong/xoa error, selected decision, dong/xoa decision note
 *  437  [{BT_DAU_TICH} Duyet dat & Nop (ACCEPT)]  handleOpenDecisionModal() Mo modal de nop quyet dinh -> tren trang: dong/xoa error, selected decision, dong/xoa decision note
 *  510  [Huy bo]                        tren trang: dong/xoa modal open
 *  518  [Dang nop... / Xac nhan nop]    handleSubmitDecision() UC-26, UC-27, UC-28: Nop phieu cham -> goi POST /api/scoring-attempts/article/{articleId}/submit
 */

const DANH_SACH_THE_LOAI = [
  { id: 1, name: '1 - Tin tức' },
  { id: 2, name: '2 - Tin sâu' },
  { id: 3, name: '3 - Bài phản ánh' },
  { id: 4, name: '4 - Phóng sự' },
  { id: 5, name: '5 - Phỏng vấn' },
  { id: 6, name: '6 - Bình luận' },
  { id: 7, name: '7 - Tường thuật' },
  { id: 8, name: '8 - Infographic' }
]

export default function ScoringSheet({ nguoiDung, articleIdProp, onBack }) {
  const params = useParams()
  const navigate = useNavigate()
  const articleId = Number(articleIdProp || params.articleId || params.id)

  const [attempt, setAttempt] = useState(null)
  const [details, setDetails] = useState([])
  const [newCategoryId, setNewCategoryId] = useState('')
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  // Modal xac nhan quyet dinh (ACCEPT, REVISION, REJECT)
  const [modalOpen, setModalOpen] = useState(false)
  const [selectedDecision, setSelectedDecision] = useState('')
  const [decisionNote, setDecisionNote] = useState('')

  useEffect(() => {
    let ignore = false

    async function loadScoringSheet() {
      setLoading(true)
      setError('')
      setSuccess('')
      try {
        const data = await moPhieuCham(articleId)
        if (!ignore) {
          setAttempt(data)
          setDetails(data.details || [])
          setNewCategoryId(data.categoryId || 1)
        }
      } catch (err) {
        if (!ignore) {
          setError('Lỗi tải phiếu chấm: ' + err.message)
        }
      } finally {
        if (!ignore) {
          setLoading(false)
        }
      }
    }

    if (articleId) {
      loadScoringSheet()
    }

    return () => {
      ignore = true
    }
  }, [articleId])

  // Tinh tong diem truc tiep tren UI (UC-25)
  const calculatedTotalScore = details.reduce((sum, item) => {
    const s = Number(item.score) || 0
    const w = Number(item.weight) || 0
    return sum + (s * w) / 100
  }, 0).toFixed(2)

  // Cap nhat diem cua 1 tieu chi
  function handleScoreChange(index, newScore) {
    const updated = [...details]
    updated[index].score = newScore
    setDetails(updated)
  }

  // Cap nhat nhan xet cua 1 tieu chi
  function handleCommentChange(index, newComment) {
    const updated = [...details]
    updated[index].comment = newComment
    setDetails(updated)
  }

  // UC-23: BTV doi the loai bai viet
  async function handleChangeCategory() {
    if (!newCategoryId || Number(newCategoryId) === attempt.categoryId) {
      alert('Vui lòng chọn một thể loại khác với thể loại hiện tại!')
      return
    }

    if (!window.confirm('Đổi thể loại sẽ nạp lại bộ tiêu chí của thể loại mới. Bạn có chắc chắn muốn đổi không?')) {
      return
    }

    setSaving(true)
    setError('')
    setSuccess('')
    try {
      const data = await doiTheLoaiBaiViet(articleId, Number(newCategoryId))
      setAttempt(data)
      setDetails(data.details || [])
      setSuccess('Đã đổi thể loại và nạp bộ tiêu chí mới thành công!')
    } catch (err) {
      setError('Lỗi đổi thể loại: ' + err.message)
    } finally {
      setSaving(false)
    }
  }

  // UC-24, UC-25: Luu nhap diem tieu chi
  async function handleSaveDraft() {
    setError('')
    setSuccess('')

    // Kiem tra hop le: neu diem < 50 bat buoc co nhan xet
    for (const d of details) {
      const s = Number(d.score)
      if (s < 50 && (!d.comment || !d.comment.trim())) {
        setError(`Tiêu chí "${d.criterionName}" đạt dưới 50 điểm (${s}) bắt buộc phải nhập nhận xét lý do!`)
        return
      }
    }

    setSaving(true)
    try {
      const payload = details.map(d => ({
        criterionId: d.criterionId,
        score: Number(d.score) || 0,
        comment: d.comment || null
      }))
      const data = await luuDiemTieuChi(articleId, payload)
      setAttempt(data)
      setDetails(data.details || [])
      setSuccess('Đã lưu điểm tiêu chí thành công! Tổng điểm hiện tại: ' + (data.totalScore || calculatedTotalScore))
    } catch (err) {
      setError('Lỗi lưu điểm: ' + err.message)
    } finally {
      setSaving(false)
    }
  }

  // Mo modal de nop quyet dinh
  function handleOpenDecisionModal(decision) {
    setError('')
    // Neu la ACCEPT thi phai luu diem truoc
    if (decision === 'ACCEPT') {
      if (!attempt.totalScore && Number(calculatedTotalScore) === 0) {
        alert('Vui lòng nhập điểm và lưu điểm trước khi chọn Đồng ý (ACCEPT)!')
        return
      }
    }
    setSelectedDecision(decision)
    setDecisionNote('')
    setModalOpen(true)
  }

  // UC-26, UC-27, UC-28: Nop phieu cham
  async function handleSubmitDecision() {
    if (selectedDecision === 'REVISION' && (!decisionNote || !decisionNote.trim())) {
      alert('Yêu cầu sửa đổi (REVISION) bắt buộc phải nhập nội dung nhận xét lý do!')
      return
    }
    if (selectedDecision === 'REJECT' && (!decisionNote || !decisionNote.trim())) {
      alert('Từ chối bài viết (REJECT) bắt buộc phải nhập lý do từ chối!')
      return
    }

    setSubmitting(true)
    setError('')
    setSuccess('')
    try {
      const data = await nopPhieuCham(articleId, selectedDecision, decisionNote)
      setAttempt(data)
      setDetails(data.details || [])
      setModalOpen(false)
      setSuccess(`Đã nộp phiếu chấm điểm thành công với quyết định: ${selectedDecision}! Phiếu chấm đã được khoá.`)
    } catch (err) {
      setError('Lỗi nộp phiếu: ' + err.message)
    } finally {
      setSubmitting(false)
    }
  }

  const isSubmitted = Boolean(attempt?.submittedAt)

  function handleGoBack() {
    if (onBack) onBack()
    else navigate('/assigned-articles')
  }

  return (
    <div className="bf02__the">
      {/* Thanh tieu de & Nut quay lai */}
      <div className="bf02__dau">
        <div>
          <button type="button" onClick={handleGoBack} className="nut-nhe" style={{ marginBottom: 8 }}>
            &larr; Quay lại danh sách bài được giao
          </button>
          <h3 className="bf02__tieu-de">
            <Icon>{BT_TIEU_CHI}</Icon>
            Phiếu chấm điểm bài viết #{articleId}
          </h3>
          <p className="bf02__phu-de">
            {attempt?.articleTitle || 'Đang tải thông tin bài viết...'}
          </p>
        </div>

        {attempt && (
          <div style={{ textAlign: 'right' }}>
            <div>
              Trạng thái:{' '}
              {isSubmitted ? (
                <span className="nhan-tt nhan-tt--tot">
                  <Icon>{BT_DAU_TICH}</Icon> ĐÃ NỘP ({attempt.decision})
                </span>
              ) : (
                <span className="nhan-tt nhan-tt--thong-tin">
                  ĐANG CHẤM (Lần #{attempt.attemptNo} - {attempt.attemptType})
                </span>
              )}
            </div>
            {attempt.submittedAt && (
              <small className="bf02__nhat">
                Nộp lúc: {new Date(attempt.submittedAt).toLocaleString('vi-VN')}
              </small>
            )}
          </div>
        )}
      </div>

      {error && <div className="thong-bao-loi" style={{ marginBottom: 16 }}>{error}</div>}
      {success && <div className="thong-bao-thanh-cong" style={{ marginBottom: 16 }}>{success}</div>}

      {loading ? (
        <p className="bf02__nhat">Đang tải phiếu chấm điểm...</p>
      ) : !attempt ? (
        <div className="bf02__trong">Không tìm thấy phiếu chấm của bài viết này.</div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
          
          {/* COT TRAI: NOI DUNG BAI VIET */}
          <div style={{ background: 'var(--fm-nen)', padding: 16, borderRadius: 8, border: '1px solid var(--fm-vien)' }}>
            <h4 style={{ margin: '0 0 12px 0', fontSize: 16 }}>Nội dung bài viết nộp</h4>
            <div style={{ marginBottom: 12, fontSize: 13, color: 'var(--fm-chu-nhat)' }}>
              <div><strong>Tiêu đề:</strong> {attempt.articleTitle}</div>
              <div><strong>Bản nộp:</strong> Version #{attempt.versionId}</div>
            </div>
            <div style={{
              maxHeight: '480px',
              overflowY: 'auto',
              padding: 12,
              background: 'var(--fm-the)',
              borderRadius: 6,
              border: '1px solid var(--fm-vien)',
              whiteSpace: 'pre-wrap',
              lineHeight: 1.6,
              fontSize: 14
            }}>
              {attempt.articleContent || 'Không có nội dung văn bản.'}
            </div>
          </div>

          {/* COT PHAI: FORM CHAM DIEM & DOI THE LOAI */}
          <div>
            {/* UC-23: Chuyen the loai bai viet */}
            <div style={{
              background: 'var(--fm-the)',
              padding: 14,
              borderRadius: 8,
              border: '1px solid var(--fm-vien)',
              marginBottom: 16
            }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
                <div>
                  <span className="bf02__nhan">Thể loại áp dụng:</span>{' '}
                  <strong>{DANH_SACH_THE_LOAI.find(c => c.id === attempt.categoryId)?.name || attempt.categoryId}</strong>
                </div>

                {!isSubmitted && (
                  <div style={{ display: 'flex', gap: 8 }}>
                    <select
                      value={newCategoryId}
                      onChange={(e) => setNewCategoryId(e.target.value)}
                      className="o-nhap-ngan"
                      style={{ fontSize: 13 }}
                    >
                      {DANH_SACH_THE_LOAI.map(c => (
                        <option key={c.id} value={c.id}>{c.name}</option>
                      ))}
                    </select>
                    <button
                      type="button"
                      onClick={handleChangeCategory}
                      disabled={saving || Number(newCategoryId) === attempt.categoryId}
                      className="nut-nhe"
                    >
                      Đổi thể loại
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* UC-24: Bang nhap diem tieu chi */}
            <div className="bf02__cuon" style={{ marginBottom: 16 }}>
              <table className="bang-fm">
                <thead>
                  <tr>
                    <th>Tiêu chí đánh giá</th>
                    <th style={{ width: 80, textAlign: 'center' }}>Trọng số</th>
                    <th style={{ width: 100, textAlign: 'center' }}>Điểm (0-100)</th>
                    <th>Nhận xét lý do</th>
                  </tr>
                </thead>
                <tbody>
                  {details.map((d, idx) => {
                    const isBelow50 = Number(d.score) < 50
                    return (
                      <tr key={d.id || idx}>
                        <td>
                          <strong>{d.criterionName}</strong>
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          <span className="nhan-tt nhan-tt--thong-tin">{d.weight}%</span>
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          <input
                            type="number"
                            min="0"
                            max="100"
                            step="0.5"
                            value={d.score === 0 ? 0 : d.score || ''}
                            onChange={(e) => handleScoreChange(idx, e.target.value)}
                            disabled={isSubmitted || saving}
                            className="o-nhap-ngan bf02__o-nhap--so"
                            style={{ width: '70px', textAlign: 'center' }}
                          />
                        </td>
                        <td>
                          <input
                            type="text"
                            placeholder={isBelow50 ? 'Bắt buộc nhận xét (< 50đ)' : 'Nhận xét (không bắt buộc)...'}
                            value={d.comment || ''}
                            onChange={(e) => handleCommentChange(idx, e.target.value)}
                            disabled={isSubmitted || saving}
                            className="o-nhap-ngan"
                            style={{
                              width: '100%',
                              borderColor: isBelow50 && (!d.comment || !d.comment.trim()) ? 'var(--fm-loi)' : undefined
                            }}
                          />
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {/* UC-25: Tong diem tinh toan */}
            <div className="bf02__tong-ket" style={{ marginBottom: 20 }}>
              <div>
                <span className="bf02__tong-nhan">Tổng điểm có trọng số:</span>
                <span className="bf02__nhat">(Scale 100 theo công thức chuẩn BR-SC-07)</span>
              </div>
              <div className="bf02__tong-diem">
                {attempt.totalScore ? attempt.totalScore.toFixed(2) : calculatedTotalScore} / 100.00
              </div>
            </div>

            {/* 4 NUT HANH DONG CUA BTV */}
            {!isSubmitted ? (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, justifyContent: 'flex-end' }}>
                <button
                  type="button"
                  onClick={handleSaveDraft}
                  disabled={saving || submitting}
                  className="nut-nhe"
                >
                  Lưu nháp điểm
                </button>

                <button
                  type="button"
                  onClick={() => handleOpenDecisionModal('REVISION')}
                  disabled={saving || submitting}
                  className="nut-nhe"
                  style={{ color: 'var(--fm-canh-bao)', borderColor: 'var(--fm-canh-bao)' }}
                >
                  Yêu cầu sửa (RETURN)
                </button>

                <button
                  type="button"
                  onClick={() => handleOpenDecisionModal('REJECT')}
                  disabled={saving || submitting}
                  className="nut-nhe"
                  style={{ color: 'var(--fm-loi)', borderColor: 'var(--fm-loi)' }}
                >
                  Từ chối bài (REJECT)
                </button>

                <button
                  type="button"
                  onClick={() => handleOpenDecisionModal('ACCEPT')}
                  disabled={saving || submitting}
                  className="nut-chinh-fm"
                >
                  <Icon>{BT_DAU_TICH}</Icon> Duyệt đạt &amp; Nộp (ACCEPT)
                </button>
              </div>
            ) : (
              <div className="bf02__tong-ket" style={{ background: 'var(--fm-the)' }}>
                <div>
                  <strong>Quyết định:</strong> {attempt.decision}
                  {attempt.decisionNote && (
                    <div style={{ marginTop: 4, fontSize: 13, color: 'var(--fm-chu-nhat)' }}>
                      Ghi chú: {attempt.decisionNote}
                    </div>
                  )}
                </div>
                <span className="nhan-tt nhan-tt--tot">Phiếu đã nộp (Khoá)</span>
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL XAC NHAN QUYET DINH */}
      {modalOpen && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 9999
        }}>
          <div style={{
            background: 'var(--fm-the)',
            padding: 24,
            borderRadius: 8,
            width: '460px',
            boxShadow: '0 4px 16px rgba(0,0,0,0.2)',
            border: '1px solid var(--fm-vien)'
          }}>
            <h4 style={{ margin: '0 0 12px 0', fontSize: 17 }}>
              Xác nhận quyết định: {selectedDecision}
            </h4>
            
            <p style={{ fontSize: 13, color: 'var(--fm-chu-nhat)', marginBottom: 12 }}>
              {selectedDecision === 'ACCEPT' && 'Bài viết sẽ được chuyển sang hàng chờ duyệt điểm của Trưởng ban (PENDING_APPROVAL).'}
              {selectedDecision === 'REVISION' && 'Bài viết sẽ được trả về cho Phóng viên sửa đổi (REVISION_REQUESTED). Bạn bắt buộc phải nhập nhận xét lý do bên dưới.'}
              {selectedDecision === 'REJECT' && 'Bài viết sẽ bị từ chối chính thức (REJECTED). Bạn bắt buộc phải nhập lý do từ chối bên dưới.'}
            </p>

            <div style={{ marginBottom: 16 }}>
              <label style={{ display: 'block', fontSize: 13, marginBottom: 6, fontWeight: 500 }}>
                {selectedDecision === 'ACCEPT' ? 'Ghi chú thêm (không bắt buộc):' : 'Lý do / Nội dung nhận xét (bắt buộc):'}
              </label>
              <textarea
                rows={4}
                value={decisionNote}
                onChange={(e) => setDecisionNote(e.target.value)}
                placeholder="Nhập nội dung tại đây..."
                className="o-nhap-ngan"
                style={{ width: '100%', resize: 'vertical' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                disabled={submitting}
                className="nut-nhe"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleSubmitDecision}
                disabled={submitting}
                className="nut-chinh-fm"
              >
                {submitting ? 'Đang nộp...' : 'Xác nhận nộp'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
