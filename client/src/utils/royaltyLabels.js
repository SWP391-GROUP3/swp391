/**
 * Chu hien thi cho cac ma cua luong nhuan but (BF-04), dung chung cho moi man
 * de cung 1 ma luon hien cung 1 chu. Ma (key) giu dung gia tri trong database/schema.sql.
 */

/** royalty_periods.status -> chu hien thi (BR-RY-07: OPEN -> CLOSED -> LOCKED -> PAID) */
export const PERIOD_STATUS_LABELS = {
  OPEN:   'Đang mở',
  CLOSED: 'Đã chốt, chờ kế toán duyệt',
  LOCKED: 'Đã khoá, chờ chi trả',
  PAID:   'Đã chi trả',
}

/**
 * royalty_periods.status -> mau cham nhan, giong trang chu Ke toan.
 * Gia tri la duoi cua class chung .cham--<mau> trong index.css nen giu nguyen ten.
 */
export const PERIOD_STATUS_COLORS = {
  OPEN:   'xanh-duong',
  CLOSED: 'cam',
  LOCKED: 'tim',
  PAID:   'xanh-la',
}

/** media_factors.media_kind -> chu hien thi (xem comment bang media_factors trong schema.sql) */
export const MEDIA_KIND_LABELS = {
  PHOTO:   'Ảnh tác nghiệp',
  GRAPHIC: 'Đồ hoạ / infographic',
  VIDEO:   'Video',
}

/** Ma la (chua co trong bang tren) thi hien nguyen ma, khong de trong. */
export function periodStatusLabel(status) {
  return PERIOD_STATUS_LABELS[status] || status
}

export function mediaKindLabel(mediaKind) {
  return MEDIA_KIND_LABELS[mediaKind] || mediaKind
}
