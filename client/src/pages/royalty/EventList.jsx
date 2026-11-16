import { useEffect, useState } from 'react'
import { createEvent, deleteEvent, getEvents, setEventActive, updateEvent } from '../../api/royaltyApi.js'
import { formatDate } from '../../utils/formatDate.js'
import { formatNumber } from '../../utils/formatNumber.js'
import '../../styles/Royalty.css'

// Gioi han giong EventSaveRequest.java. Kiem o day de bao loi nhanh; server van kiem lai bang @Valid.
const NAME_MAX_LENGTH = 200
const MAX_WINDOW_DAYS = 365
const MAX_ARTICLES_PER_LEVEL = 1000
const SCORE_PATTERN = /^\d{1,3}(\.\d{1,2})?$/   // so tu 0, toi da 2 chu so thap phan (cot DECIMAL(5,2))

// Form trong: 3 so mac dinh giong DEFAULT cua bang events (cua so 7 ngay, 10 bai moi bac, mien giam tu 90 diem)
const EMPTY_FORM = { name: '', startDate: '', endDate: '', windowDays: '7', articlesPerLevel: '10', exemptScore: '90' }

/**
 * "Su kien bao hoa" (Thu ky toa soan, GBR-09).
 * Su kien lon (bao lu, bau cu...) co nhieu bai cung viet: bai nop sau bi giam he so nhuan but.
 * Tren: form tao su kien moi, hoac sua su kien dang chon. Duoi: tat ca su kien, moi nhat truoc.
 * Su kien DA BAT DAU chi sua duoc ten va ngay ket thuc (ty le bao hoa cua bai da khoa luc nop, BR-IN-03).
 *
 * MO TRANG NAY TAI: /royalty-events
 *
 * MUC LUC (so dong - ten - tac dung)
 *   44  EventList()                     xem tom tat dau file
 *   57    useEffect                     tai danh sach su kien khi mo trang
 *   66    changeField()                 doi gia tri 1 o trong form (tao object moi, khong sua object cu)
 *   70    startEdit()                   dua du lieu su kien len form de sua
 *   85    cancelEdit()                  bo sua, quay ve form tao moi
 *   91    handleSubmit()                kiem form -> goi tao hoac sua -> tai lai danh sach
 *  129    handleToggle()                bat / tat su kien
 *  151    handleDelete()                xoa su kien (da co bai gan vao thi server tu choi)
 *  333  EventBadge()                    Nhan tinh trang: da tat / sap dien ra / dang mo / da ket thuc
 *  347  validateForm()                  Kiem 6 o, tra ve cau loi dau tien (chuoi rong = hop le)
 *  380  todayIsoDate()                  Ngay hom nay dang "yyyy-MM-dd"
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  198  form, nut [Tao su kien / Luu thay doi]  handleSubmit() -> goi POST /api/events hoac PUT /api/events/{id}
 *  255  [Huy sua]                       cancelEdit() -> form quay ve tao moi
 *  308  [Sua]                           startEdit() -> dua su kien len form
 *  312  [Tat / Bat lai]                 handleToggle() -> goi POST /api/events/{id}/deactivate hoac /activate
 *  316  [Xoa]                           handleDelete() -> hoi xac nhan; goi DELETE /api/events/{id}
 */
export default function EventList() {
  const [events, setEvents] = useState(null)       // null = dang tai
  const [loadError, setLoadError] = useState('')

  const [form, setForm] = useState(EMPTY_FORM)
  const [editingEvent, setEditingEvent] = useState(null)   // su kien dang sua, null = dang tao moi
  const [isSaving, setIsSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [togglingId, setTogglingId] = useState(null)       // id su kien dang bat / tat
  const [deletingId, setDeletingId] = useState(null)       // id su kien dang xoa
  const [listError, setListError] = useState('')

  useEffect(() => {
    let isMounted = true
    getEvents()
      .then((data) => { if (isMounted) setEvents(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
    return () => { isMounted = false }
  }, [])

  // Doi gia tri 1 o trong form: tao object moi, khong sua object cu
  function changeField(field, value) {
    setForm({ ...form, [field]: value })
  }

  function startEdit(item) {
    setEditingEvent(item)
    setForm({
      name: item.name,
      startDate: item.startDate,
      endDate: item.endDate,
      windowDays: String(item.windowDays),
      articlesPerLevel: String(item.articlesPerLevel),
      exemptScore: String(item.exemptScore),
    })
    setFormError('')
    setSuccessMessage('')
    window.scrollTo({ top: 0, behavior: 'smooth' })   // form nam o dau trang
  }

  function cancelEdit() {
    setEditingEvent(null)
    setForm(EMPTY_FORM)
    setFormError('')
  }

  async function handleSubmit(submitEvent) {
    submitEvent.preventDefault()   // khong cho trinh duyet tai lai trang khi gui form
    setFormError('')
    setSuccessMessage('')

    // Kiem tra nhanh o trinh duyet; server van kiem lai (@Valid + luat trong EventService)
    const isCreating = editingEvent === null
    const error = validateForm(form, isCreating, todayIsoDate())
    if (error) {
      setFormError(error)
      return
    }

    const body = {
      name: form.name.trim(),
      startDate: form.startDate,
      endDate: form.endDate,
      windowDays: Number(form.windowDays),
      articlesPerLevel: Number(form.articlesPerLevel),
      exemptScore: Number(form.exemptScore),
    }
    setIsSaving(true)
    try {
      const saved = isCreating ? await createEvent(body) : await updateEvent(editingEvent.id, body)
      // Tai lai ca danh sach vi doi ngay bat dau co the lam doi thu tu cac dong
      setEvents(await getEvents())
      setEditingEvent(null)
      setForm(EMPTY_FORM)
      setSuccessMessage(isCreating
        ? `Đã tạo sự kiện “${saved.name}”. Phóng viên chọn được sự kiện này khi nộp bài trong khoảng ngày đã đặt.`
        : `Đã cập nhật sự kiện “${saved.name}”.`)
    } catch (err) {
      setFormError(err.message)
    } finally {
      setIsSaving(false)
    }
  }

  async function handleToggle(item) {
    if (item.active) {
      const confirmed = window.confirm(
        `Tắt sự kiện “${item.name}”? Phóng viên sẽ không chọn được sự kiện này nữa; bài đã nộp vẫn giữ tỷ lệ bão hoà cũ.`)
      if (!confirmed) {
        return
      }
    }
    setTogglingId(item.id)
    setListError('')
    setSuccessMessage('')
    try {
      const updated = await setEventActive(item.id, !item.active)
      // Thay dong vua doi bang du lieu server tra ve, cac dong khac giu nguyen
      setEvents(events.map((current) => (current.id === updated.id ? updated : current)))
    } catch (err) {
      setListError(err.message)
    } finally {
      setTogglingId(null)
    }
  }

  async function handleDelete(item) {
    const confirmed = window.confirm(`Xoá sự kiện “${item.name}”? Thao tác này không hoàn tác được.`)
    if (!confirmed) {
      return
    }
    setDeletingId(item.id)
    setListError('')
    setSuccessMessage('')
    try {
      await deleteEvent(item.id)
      // Bo dong vua xoa khoi danh sach (tao mang moi), cac dong khac giu nguyen
      setEvents(events.filter((current) => current.id !== item.id))
      if (editingEvent !== null && editingEvent.id === item.id) {
        cancelEdit()   // dang sua dung su kien vua xoa thi dua form ve tao moi
      }
      setSuccessMessage(`Đã xoá sự kiện “${item.name}”.`)
    } catch (err) {
      // Su kien da co bai gan vao: server tra 409 kem cau "hay tat su kien thay vi xoa"
      setListError(err.message)
    } finally {
      setDeletingId(null)
    }
  }

  const today = todayIsoDate()
  const isRowBusy = togglingId !== null || deletingId !== null   // dang bat/tat hoac xoa 1 dong thi khoa cac nut trong bang
  // Su kien da bat dau: 4 o cach tinh bao hoa bi khoa (giong luat trong EventService.updateEvent)
  const isLocked = editingEvent !== null && editingEvent.startDate <= today
  const articlesPerLevel = Number(form.articlesPerLevel)

  return (
    <div>
      <div className="royalty__header">
        <h1 className="royalty__title">Sự kiện bão hoà</h1>
        <p className="royalty__subtitle">
          Sự kiện lớn có nhiều bài cùng viết: bài nộp sau bị giảm hệ số nhuận bút, trừ bài đạt điểm miễn giảm.
          Tỷ lệ của mỗi bài được khoá lúc phóng viên nộp bài.
        </p>
      </div>

      <div className="royalty__card">
        <h2 className="royalty__card-title">
          {editingEvent ? `Sửa sự kiện: ${editingEvent.name}` : 'Tạo sự kiện mới'}
        </h2>
        {formError && <p className="thong-bao-loi" role="alert">{formError}</p>}
        {successMessage && <p className="thong-bao-thanh-cong" role="status">{successMessage}</p>}

        <form className="royalty__form" onSubmit={handleSubmit} noValidate>
          <label className="royalty__field">
            Tên sự kiện
            <input
              type="text" className="royalty__input"
              maxLength={NAME_MAX_LENGTH}
              value={form.name} disabled={isSaving}
              onChange={(inputEvent) => changeField('name', inputEvent.target.value)}
            />
          </label>
          <label className="royalty__field">
            Ngày bắt đầu
            <input
              type="date" className="royalty__input"
              value={form.startDate} disabled={isSaving || isLocked}
              onChange={(inputEvent) => changeField('startDate', inputEvent.target.value)}
            />
          </label>
          <label className="royalty__field">
            Ngày kết thúc
            <input
              type="date" className="royalty__input"
              value={form.endDate} min={form.startDate || undefined} disabled={isSaving}
              onChange={(inputEvent) => changeField('endDate', inputEvent.target.value)}
            />
          </label>
          <label className="royalty__field">
            Cửa sổ tính (ngày)
            <input
              type="number" min="1" max={MAX_WINDOW_DAYS} step="1" inputMode="numeric"
              className="royalty__input royalty__input--small"
              value={form.windowDays} disabled={isSaving || isLocked}
              onChange={(inputEvent) => changeField('windowDays', inputEvent.target.value)}
            />
          </label>
          <label className="royalty__field">
            Số bài mỗi bậc
            <input
              type="number" min="1" max={MAX_ARTICLES_PER_LEVEL} step="1" inputMode="numeric"
              className="royalty__input royalty__input--small"
              value={form.articlesPerLevel} disabled={isSaving || isLocked}
              onChange={(inputEvent) => changeField('articlesPerLevel', inputEvent.target.value)}
            />
          </label>
          <label className="royalty__field">
            Điểm miễn giảm
            <input
              type="number" min="0" max="100" step="0.01" inputMode="decimal"
              className="royalty__input royalty__input--small"
              value={form.exemptScore} disabled={isSaving || isLocked}
              onChange={(inputEvent) => changeField('exemptScore', inputEvent.target.value)}
            />
          </label>
          <button type="submit" className="nut-chinh-fm" disabled={isSaving}>
            {isSaving ? 'Đang lưu…' : (editingEvent ? 'Lưu thay đổi' : 'Tạo sự kiện')}
          </button>
          {editingEvent && (
            <button type="button" className="nut-nhe" disabled={isSaving} onClick={cancelEdit}>
              Huỷ sửa
            </button>
          )}
        </form>
        {isLocked && (
          <p className="royalty__hint">
            Sự kiện đã bắt đầu nên chỉ sửa được tên và ngày kết thúc. Muốn đổi cách tính bão hoà, hãy tắt sự kiện này và tạo sự kiện mới.
          </p>
        )}
        {Number.isInteger(articlesPerLevel) && articlesPerLevel >= 1 && (
          <p className="royalty__hint">
            Với {articlesPerLevel} bài mỗi bậc: bài thứ 1–{articlesPerLevel} nhận đủ hệ số,
            bài thứ {articlesPerLevel + 1}–{articlesPerLevel * 2} giảm bậc 2,
            từ bài thứ {articlesPerLevel * 2 + 1} giảm bậc 3 (tỷ lệ từng bậc nằm trong tham số hệ thống).
            Chỉ đếm các bài nộp trong {form.windowDays || '…'} ngày gần nhất.
          </p>
        )}
      </div>

      <div className="royalty__card">
        <h2 className="royalty__card-title">Danh sách sự kiện</h2>
        {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
        {listError && <p className="thong-bao-loi" role="alert">{listError}</p>}
        {!events && !loadError && <p className="royalty__empty">Đang tải…</p>}
        {events && events.length === 0 && <p className="royalty__empty">Chưa có sự kiện nào.</p>}

        {events && events.length > 0 && (
          <div className="royalty__scroll">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th>Sự kiện</th>
                  <th>Từ ngày</th>
                  <th>Đến ngày</th>
                  <th className="royalty__number">Cửa sổ (ngày)</th>
                  <th className="royalty__number">Số bài mỗi bậc</th>
                  <th className="royalty__number">Điểm miễn giảm</th>
                  <th>Tình trạng</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {events.map((item) => (
                  <tr key={item.id}>
                    <td className="royalty__strong royalty__title-cell">{item.name}</td>
                    <td className="royalty__date">{formatDate(item.startDate)}</td>
                    <td className="royalty__date">{formatDate(item.endDate)}</td>
                    <td className="royalty__number">{item.windowDays}</td>
                    <td className="royalty__number">{item.articlesPerLevel}</td>
                    <td className="royalty__number">{formatNumber(item.exemptScore)}</td>
                    <td><EventBadge item={item} today={today} /></td>
                    <td className="royalty__actions">
                      <button type="button" className="nut-nhe" disabled={isRowBusy}
                              onClick={() => startEdit(item)}>
                        Sửa
                      </button>
                      <button type="button" className="nut-nhe" disabled={isRowBusy}
                              onClick={() => handleToggle(item)}>
                        {togglingId === item.id ? 'Đang lưu…' : (item.active ? 'Tắt' : 'Bật lại')}
                      </button>
                      <button type="button" className="nut-nhe" disabled={isRowBusy}
                              onClick={() => handleDelete(item)}>
                        {deletingId === item.id ? 'Đang xoá…' : 'Xoá'}
                      </button>
                    </td>
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

/** Nhan tinh trang cua 1 su kien. Chuoi "yyyy-MM-dd" so sanh duoc truc tiep. */
function EventBadge({ item, today }) {
  if (!item.active) {
    return <span className="royalty__badge">Đã tắt</span>
  }
  if (item.startDate > today) {
    return <span className="royalty__badge royalty__badge--future">Sắp diễn ra</span>
  }
  if (item.endDate < today) {
    return <span className="royalty__badge">Đã kết thúc</span>
  }
  return <span className="royalty__badge royalty__badge--current">Đang mở</span>
}

/** Kiem 6 o cua form, tra ve cau loi dau tien gap; chuoi rong = hop le. */
function validateForm(form, isCreating, today) {
  const name = form.name.trim()
  if (name === '') {
    return 'Vui lòng nhập tên sự kiện.'
  }
  if (name.length > NAME_MAX_LENGTH) {
    return `Tên sự kiện tối đa ${NAME_MAX_LENGTH} ký tự.`
  }
  if (!form.startDate || !form.endDate) {
    return 'Vui lòng chọn ngày bắt đầu và ngày kết thúc.'
  }
  if (form.startDate > form.endDate) {
    return 'Ngày bắt đầu không được sau ngày kết thúc.'
  }
  if (isCreating && form.endDate < today) {
    return 'Ngày kết thúc không được ở quá khứ.'
  }

  const windowDays = Number(form.windowDays)
  if (!Number.isInteger(windowDays) || windowDays < 1 || windowDays > MAX_WINDOW_DAYS) {
    return `Cửa sổ tính phải là số nguyên từ 1 đến ${MAX_WINDOW_DAYS} ngày.`
  }
  const articlesPerLevel = Number(form.articlesPerLevel)
  if (!Number.isInteger(articlesPerLevel) || articlesPerLevel < 1 || articlesPerLevel > MAX_ARTICLES_PER_LEVEL) {
    return `Số bài mỗi bậc phải là số nguyên từ 1 đến ${MAX_ARTICLES_PER_LEVEL}.`
  }
  if (!SCORE_PATTERN.test(form.exemptScore) || Number(form.exemptScore) > 100) {
    return 'Điểm miễn giảm phải từ 0 đến 100, tối đa 2 chữ số thập phân (vd 90 hoặc 87.5).'
  }
  return ''
}

/** Ngay hom nay theo gio may dang "yyyy-MM-dd" (giong dinh dang server tra ve). */
function todayIsoDate() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
