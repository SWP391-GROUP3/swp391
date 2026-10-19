/**
 * Ngay gio server tra ve dang LocalDateTime cua Spring: "2026-10-15T17:00:00"
 * (khong co mui gio). new Date() hieu chuoi nay la gio cua may nguoi dung.
 *
 * MUC LUC (so dong)
 *   12  DINH_DANG_NGAY
 *   16  MS_MOT_NGAY
 *   19  formatDate                   - "2026-10-12T17:00:00" -> "12/10/2026"; chuoi rong / sai -> "—".
 *   34  daysUntil                    - So ngay con lai tu bay gio toi han chot, lam tron len.
 */

const DINH_DANG_NGAY = new Intl.DateTimeFormat('vi-VN', {
  day: '2-digit', month: '2-digit', year: 'numeric',
})

const MS_MOT_NGAY = 24 * 60 * 60 * 1000

/** "2026-10-12T17:00:00" -> "12/10/2026"; chuoi rong / sai -> "—". */
export function formatDate(isoString) {
  if (!isoString) {
    return '—'
  }
  const ngay = new Date(isoString)
  if (Number.isNaN(ngay.getTime())) {
    return '—'
  }
  return DINH_DANG_NGAY.format(ngay)
}

/**
 * So ngay con lai tu bay gio toi han chot, lam tron len.
 * Con 30 tieng -> 2; con 3 tieng -> 1; da qua han -> 0 hoac am.
 */
export function daysUntil(isoString) {
  const conLaiMs = new Date(isoString).getTime() - Date.now()
  return Math.ceil(conLaiMs / MS_MOT_NGAY)
}
