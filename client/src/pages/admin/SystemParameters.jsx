import { useEffect, useState } from 'react'
import { getIntakeParameters, updateIntakeParameters } from '../../api/adminApi.js'
import './AdminPages.css'

/**
 * UC-13 Configure Intake Parameters: Admin xem / sua 6 tham so nhap bai cua BF-01.
 * Gioi han tung o va luat "nguong canh bao < nguong chan" kiem o server; man nay chi hien loi server tra ve.
 * Sua chi ap dung cho bai nop SAU khi luu (ket qua da luu cua bai cu khong tinh lai).
 *
 * CODE LIEN QUAN (Ctrl+P trong VS Code, dan duong dan de mo file):
 *   client/src/api/adminApi.js
 *   server/src/main/java/com/swp391/kpi/admin/controller/SystemParameterController.java  (Quang)
 *   server/src/main/java/com/swp391/kpi/admin/service/SystemParameterService.java        updateIntakeParameters(): kiem gioi han, ghi audit_logs (Quang)
 * DUONG DI KHI MO TRANG / BAM NUT (ham api -> URL -> Controller -> Service):
 *   Mo trang: getIntakeParameters() -> GET /api/system-parameters/intake
 *   [Luu]: updateIntakeParameters() -> PUT /api/system-parameters/intake -> SystemParameterService.updateIntakeParameters()
 *
 * MO TRANG NAY TAI: /system-parameters
 *
 * MUC LUC (so dong - ten - tac dung)
 *   33  NHOM_THAM_SO                    Moi o: ten field cua API, nhan, goi y gioi han (khop @Min/@Max cua SystemParameterIntakeUpdateRequest)
 *   57  SystemParameters()              system parameters
 *   65    useEffect                     Tai 6 tham so khi mo trang
 *   78    toForm()                      Doi ket qua API sang gia tri chuoi cho o nhap
 *   89    save()                        Bam "Luu": gui du 6 truong, server kiem gioi han va ghi audit_logs cho o nao doi
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  140  [Dang luu… / Luu]               save() Bam "Luu": gui du 6 truong, server kiem gioi han va ghi audit_logs cho o nao doi -> goi PUT /api/system-parameters/intake
 *  143  [Hoan tac]                      tren trang: form
 */

// Moi o: ten field cua API, nhan, goi y gioi han (khop @Min/@Max cua SystemParameterIntakeUpdateRequest)
const NHOM_THAM_SO = [
  {
    tieuDe: 'Tác giả và xác nhận',
    cacO: [
      { field: 'maxAuthorsPerArticle', nhan: 'Số tác giả tối đa / bài', goiY: '1 – 5 (BR-IN-02, GBR-01)' },
      { field: 'coAuthorConfirmDays', nhan: 'Số ngày đồng tác giả xác nhận', goiY: 'từ 1 ngày (BR-IN-06)' },
    ],
  },
  {
    tieuDe: 'Kiểm trùng nội dung (BR-IN-04)',
    cacO: [
      { field: 'dupWarnPercent', nhan: 'Ngưỡng phải giải trình (%)', goiY: '1 – 100, nhỏ hơn ngưỡng chặn' },
      { field: 'dupBlockPercent', nhan: 'Ngưỡng chặn bài Sáng tác (%)', goiY: '1 – 100' },
      { field: 'shingleSize', nhan: 'Số âm tiết mỗi cụm so trùng', goiY: '3 – 10' },
    ],
  },
  {
    tieuDe: 'Kiểm trùng ảnh (BR-IN-05)',
    cacO: [
      { field: 'dhashMaxBits', nhan: 'Lệch dHash tối đa coi là giống (bit)', goiY: '0 – 64' },
    ],
  },
]

export default function SystemParameters() {
  const [form, setForm] = useState(null)       // null = dang tai; gia tri giu dang chuoi cho o nhap so
  const [goc, setGoc] = useState(null)         // gia tri dang luu tren server (de nut "Hoàn tác")
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [saving, setSaving] = useState(false)

  // Tai 6 tham so khi mo trang
  useEffect(() => {
    let isMounted = true
    getIntakeParameters()
      .then((data) => {
        if (!isMounted) return
        setForm(toForm(data))
        setGoc(toForm(data))
      })
      .catch((err) => { if (isMounted) setError(err.message) })
    return () => { isMounted = false }
  }, [])

  /** Doi ket qua API sang gia tri chuoi cho o nhap. */
  function toForm(data) {
    const result = {}
    for (const nhom of NHOM_THAM_SO) {
      for (const o of nhom.cacO) {
        result[o.field] = data[o.field] == null ? '' : String(data[o.field])
      }
    }
    return result
  }

  /** Bam "Lưu": gui du 6 truong, server kiem gioi han va ghi audit_logs cho o nao doi. */
  function save() {
    setSaving(true)
    setError('')
    setSuccess('')
    const duLieu = {}
    for (const field of Object.keys(form)) {
      duLieu[field] = form[field] === '' ? null : Number(form[field])
    }
    updateIntakeParameters(duLieu)
      .then((data) => {
        setForm(toForm(data))
        setGoc(toForm(data))
        setSuccess('Đã lưu tham số. Áp dụng cho bài nộp từ bây giờ.')
      })
      .catch((err) => setError(err.message))
      .finally(() => setSaving(false))
  }

  return (
    <div>
      <div className="trang-qt__header">
        <h1 className="trang-qt__title">Tham số nhập bài</h1>
        <p className="trang-qt__subtitle">
          Các tham số luồng tiếp nhận bài (BF-01). Sửa xong chỉ áp dụng cho bài nộp sau, kết quả cũ giữ nguyên.
        </p>
      </div>

      <div className="trang-qt__card">
        {error && <p className="thong-bao-loi" role="alert">{error}</p>}
        {success && <p className="thong-bao-thanh-cong" role="status">{success}</p>}
        {!form && !error && <p className="trang-qt__muted">Đang tải…</p>}

        {form && (
          <>
            <div className="trang-qt__form">
              {NHOM_THAM_SO.map((nhom) => (
                <div key={nhom.tieuDe} className="trang-qt__group">
                  <h2 className="trang-qt__group-title">{nhom.tieuDe}</h2>
                  {nhom.cacO.map((o) => (
                    <div key={o.field} className="trang-qt__field">
                      <label className="nhan" htmlFor={`tham-so-${o.field}`}>{o.nhan}</label>
                      <input id={`tham-so-${o.field}`} type="number" className="o-nhap" value={form[o.field]}
                        onChange={(e) => setForm({ ...form, [o.field]: e.target.value })} />
                      <p className="trang-qt__muted">{o.goiY}</p>
                    </div>
                  ))}
                </div>
              ))}
            </div>

            <div className="trang-qt__actions">
              <button type="button" className="nut-chinh-fm" disabled={saving} onClick={save}>
                {saving ? 'Đang lưu…' : 'Lưu'}
              </button>
              <button type="button" className="nut-nhe" disabled={saving} onClick={() => setForm(goc)}>
                Hoàn tác
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}
