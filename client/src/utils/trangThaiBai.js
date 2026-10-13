/**
 * Chu hien thi cho cac ma cua bai viet (BF-01) - dung chung cho moi man
 * de cung 1 ma luon hien cung 1 chu (gop y nhom truong 13/10, muc B6, B7).
 * Ma (key) giu dung gia tri CHECK trong database/schema.sql.
 */

/** articles.status -> chu hien thi */
export const TEN_TRANG_THAI = {
  DRAFT:                'Nháp',
  PENDING_CONFIRMATION: 'Chờ xác nhận đồng tác giả',
  SUBMITTED:            'Đã nộp',
  ASSIGNED:             'Đã phân công',
  REVISION_REQUESTED:   'Trả về – cần sửa',
  REJECTED:             'Từ chối',
  PENDING_APPROVAL:     'Chờ duyệt điểm',
  SCORE_PUBLISHED:      'Đã công bố điểm',
  PUBLISHED:            'Đã đăng',
}

/** articles.status -> mau cham nhan (class .cham--<mau> trong index.css) */
export const MAU_TRANG_THAI = {
  DRAFT:                'xam',
  PENDING_CONFIRMATION: 'cam',
  SUBMITTED:            'xanh-duong',
  ASSIGNED:             'tim',
  PENDING_APPROVAL:     'tim',
  SCORE_PUBLISHED:      'tim',
  REVISION_REQUESTED:   'hong',
  PUBLISHED:            'xanh-la',
  REJECTED:             'xam-dam',
}

/**
 * Gop 9 trang thai thanh 7 nhom cho de doc (giong trang chu Phong vien).
 * "Dang cham" gom 3 buoc ben BF-02: BTV cham, cho duyet diem, da cong bo diem.
 */
export const NHOM_TRANG_THAI = [
  { nhan: 'Nháp',             mau: 'xam',        cacTrangThai: ['DRAFT'] },
  { nhan: 'Chờ xác nhận',     mau: 'cam',        cacTrangThai: ['PENDING_CONFIRMATION'] },
  { nhan: 'Đã nộp',           mau: 'xanh-duong', cacTrangThai: ['SUBMITTED'] },
  { nhan: 'Đang chấm',        mau: 'tim',        cacTrangThai: ['ASSIGNED', 'PENDING_APPROVAL', 'SCORE_PUBLISHED'] },
  { nhan: 'Trả về – cần sửa', mau: 'hong',       cacTrangThai: ['REVISION_REQUESTED'] },
  { nhan: 'Đã đăng',          mau: 'xanh-la',    cacTrangThai: ['PUBLISHED'] },
  { nhan: 'Từ chối',          mau: 'xam-dam',    cacTrangThai: ['REJECTED'] },
]

/** articles.declared_nature -> chu hien thi */
export const TEN_TINH_CHAT = {
  ORIGINAL:    'Sáng tác',
  TRANSLATION: 'Dịch',
  COMPILATION: 'Tổng hợp',
  REPOST:      'Đăng lại',
}

/** article_authors.confirm_status -> chu hien thi */
export const TEN_XAC_NHAN = {
  PENDING:  'Chờ xác nhận',
  ACCEPTED: 'Đã đồng ý',
  REJECTED: 'Đã từ chối',
  EXPIRED:  'Quá hạn',
}

/** Chu cua ma, ma la (chua co trong bang) thi hien nguyen ma. */
export function tenTrangThai(ma) {
  return TEN_TRANG_THAI[ma] ?? ma
}

export function tenTinhChat(ma) {
  return TEN_TINH_CHAT[ma] ?? ma
}

export function tenXacNhan(ma) {
  return TEN_XAC_NHAN[ma] ?? ma
}
