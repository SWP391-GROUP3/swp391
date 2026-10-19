import { useEffect, useState } from 'react'
import { createRoyaltyRate, getRoyaltyRates } from '../../api/royaltyApi.js'
import { formatMoney } from '../../utils/formatMoney.js'
import { formatDate } from '../../utils/formatDate.js'
import '../../styles/Royalty.css'

/**
 * UC-55 - "Don gia nhuan but" (Ke toan).
 * Tren: form them don gia moi (chi nhap luong co so + ngay hieu luc, server tu tinh don gia).
 * Duoi: lich su don gia, moi nhat truoc. Don gia cu KHONG sua, chi them ban moi (BR-RY-03).
 */
export default function RoyaltyRates() {
  const [rates, setRates] = useState(null)        // null = dang tai
  const [loadError, setLoadError] = useState('')

  const [baseSalary, setBaseSalary] = useState('')
  const [effectiveFrom, setEffectiveFrom] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  useEffect(() => {
    let isMounted = true
    getRoyaltyRates()
      .then((data) => { if (isMounted) setRates(data) })
      .catch((err) => { if (isMounted) setLoadError(err.message) })
    return () => { isMounted = false }
  }, [])

  async function handleSubmit(event) {
    event.preventDefault()   // khong cho trinh duyet tai lai trang khi gui form
    setFormError('')
    setSuccessMessage('')

    // Kiem tra nhanh o trinh duyet; server van kiem lai (@Valid + luat ngay hieu luc)
    const salary = Number(baseSalary)
    if (!Number.isInteger(salary) || salary <= 0) {
      setFormError('Lương cơ sở phải là số nguyên dương (đồng).')
      return
    }
    if (!effectiveFrom) {
      setFormError('Vui lòng chọn ngày hiệu lực.')
      return
    }

    setIsSaving(true)
    try {
      const created = await createRoyaltyRate({ baseSalary: salary, effectiveFrom })
      // Don gia moi luon co ngay hieu luc lon nhat -> dat len dau danh sach (khong sua mang cu)
      setRates([created, ...rates])
      setBaseSalary('')
      setEffectiveFrom('')
      setSuccessMessage(`Đã thêm đơn giá ${formatMoney(created.amount)} cho 1 hệ số, `
        + `áp dụng từ ${formatDate(created.effectiveFrom)}.`)
    } catch (err) {
      setFormError(err.message)
    } finally {
      setIsSaving(false)
    }
  }

  const today = todayIsoDate()
  const currentRateId = findCurrentRateId(rates || [], today)
  // Ngay hieu luc phai SAU ngay cua don gia moi nhat -> ngay nho nhat chon duoc = ngay do + 1
  const minDate = rates && rates.length > 0 ? nextIsoDate(rates[0].effectiveFrom) : undefined

  return (
    <div>
      <div className="royalty__header">
        <h1 className="royalty__title">Đơn giá nhuận bút</h1>
        <p className="royalty__subtitle">
          Mỗi bài dùng đơn giá có ngày hiệu lực gần nhất, không sau ngày đăng bài. Đơn giá cũ không sửa, chỉ thêm bản mới.
        </p>
      </div>

      <div className="royalty__card">
        <h2 className="royalty__card-title">Thêm đơn giá mới</h2>
        {formError && <p className="thong-bao-loi" role="alert">{formError}</p>}
        {successMessage && <p className="thong-bao-thanh-cong" role="status">{successMessage}</p>}

        <form className="royalty__form" onSubmit={handleSubmit}>
          <label className="royalty__field">
            Lương cơ sở (đồng)
            <input
              type="number" min="1" step="1" inputMode="numeric"
              className="royalty__input"
              value={baseSalary}
              onChange={(event) => setBaseSalary(event.target.value)}
              placeholder="vd 2500000"
              required
            />
          </label>
          <label className="royalty__field">
            Ngày hiệu lực
            <input
              type="date"
              className="royalty__input"
              value={effectiveFrom}
              min={minDate}
              onChange={(event) => setEffectiveFrom(event.target.value)}
              required
            />
          </label>
          <button type="submit" className="nut-chinh-fm" disabled={isSaving}>
            {isSaving ? 'Đang lưu…' : 'Thêm đơn giá'}
          </button>
        </form>
        <p className="royalty__hint">
          {baseSalary && Number(baseSalary) > 0 && <>Lương cơ sở: {formatMoney(baseSalary)}. </>}
          Hệ thống tự tính đơn giá 1 hệ số theo tỷ lệ % lương cơ sở trong tham số hệ thống.
        </p>
      </div>

      <div className="royalty__card">
        <h2 className="royalty__card-title">Lịch sử đơn giá</h2>
        {loadError && <p className="thong-bao-loi" role="alert">{loadError}</p>}
        {!rates && !loadError && <p className="royalty__empty">Đang tải…</p>}
        {rates && rates.length === 0 && <p className="royalty__empty">Chưa có đơn giá nào.</p>}

        {rates && rates.length > 0 && (
          <div className="royalty__scroll">
            <table className="bang-fm">
              <thead>
                <tr>
                  <th>Áp dụng từ</th>
                  <th className="royalty__number">Lương cơ sở</th>
                  <th className="royalty__number">Đơn giá / 1 hệ số</th>
                  <th>Tình trạng</th>
                </tr>
              </thead>
              <tbody>
                {rates.map((rate) => (
                  <tr key={rate.id}>
                    <td className="royalty__date">{formatDate(rate.effectiveFrom)}</td>
                    <td className="royalty__number">{formatMoney(rate.baseSalary)}</td>
                    <td className="royalty__number royalty__strong">{formatMoney(rate.amount)}</td>
                    <td><RateBadge rate={rate} currentRateId={currentRateId} today={today} /></td>
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

/** Nhan tinh trang cua 1 don gia: sap ap dung / dang ap dung / da thay the. */
function RateBadge({ rate, currentRateId, today }) {
  if (rate.effectiveFrom > today) {
    return <span className="royalty__badge royalty__badge--future">Sắp áp dụng</span>
  }
  if (rate.id === currentRateId) {
    return <span className="royalty__badge royalty__badge--current">Đang áp dụng</span>
  }
  return <span className="royalty__badge">Đã thay thế</span>
}

/**
 * Don gia dang ap dung hom nay = dong dau tien (danh sach moi nhat truoc)
 * co ngay hieu luc <= hom nay. Chuoi "yyyy-MM-dd" so sanh duoc truc tiep.
 */
function findCurrentRateId(rates, today) {
  for (const rate of rates) {
    if (rate.effectiveFrom <= today) {
      return rate.id
    }
  }
  return null
}

/** Ngay hom nay theo gio may dang "yyyy-MM-dd" (giong dinh dang server tra ve). */
function todayIsoDate() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

/** "2026-01-31" -> "2026-02-01". Tinh theo gio UTC de khong bi lech mui gio. */
function nextIsoDate(isoDate) {
  const date = new Date(`${isoDate}T00:00:00Z`)
  date.setUTCDate(date.getUTCDate() + 1)
  return date.toISOString().slice(0, 10)
}
