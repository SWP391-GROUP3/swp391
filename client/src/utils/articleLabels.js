/**
 * Chu hien thi cho cac ma cua bai viet (BF-01) - dung chung cho moi man
 * de cung 1 ma luon hien cung 1 chu (gop y nhom truong 13/10, muc B6, B7).
 * Ma (key) giu dung gia tri CHECK trong database/schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  STATUS_LABELS                   articles.status -> chu hien thi
 *   34  STATUS_COLORS                   articles.status -> mau cham nhan
 *   50  STATUS_GROUPS                   Gop 9 trang thai thanh 7 nhom cho de doc (giong trang chu Phong vien)
 *   61  NATURE_LABELS                   articles.declared_nature -> chu hien thi
 *   69  CONFIRM_LABELS                  article_authors.confirm_status -> chu hien thi
 *   77  statusLabel()                   Chu cua ma, ma la (chua co trong bang) thi hien nguyen ma
 *   82  natureLabel()                   Chu tieng Viet cua tinh chat bai (ORIGINAL -> Sang tac...)
 *   87  confirmLabel()                  Chu tieng Viet cua trang thai xac nhan dong tac gia (PENDING -> Cho xac nhan...)
 */

/** articles.status -> chu hien thi */
export const STATUS_LABELS = {
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

/**
 * articles.status -> mau cham nhan. Gia tri la duoi cua class chung
 * .cham--<mau> trong index.css (file dung chung) nen giu nguyen ten.
 */
export const STATUS_COLORS = {
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
export const STATUS_GROUPS = [
  { label: 'Nháp',             color: 'xam',        statuses: ['DRAFT'] },
  { label: 'Chờ xác nhận',     color: 'cam',        statuses: ['PENDING_CONFIRMATION'] },
  { label: 'Đã nộp',           color: 'xanh-duong', statuses: ['SUBMITTED'] },
  { label: 'Đang chấm',        color: 'tim',        statuses: ['ASSIGNED', 'PENDING_APPROVAL', 'SCORE_PUBLISHED'] },
  { label: 'Trả về – cần sửa', color: 'hong',       statuses: ['REVISION_REQUESTED'] },
  { label: 'Đã đăng',          color: 'xanh-la',    statuses: ['PUBLISHED'] },
  { label: 'Từ chối',          color: 'xam-dam',    statuses: ['REJECTED'] },
]

/** articles.declared_nature -> chu hien thi */
export const NATURE_LABELS = {
  ORIGINAL:    'Sáng tác',
  TRANSLATION: 'Dịch',
  COMPILATION: 'Tổng hợp',
  REPOST:      'Đăng lại',
}

/** article_authors.confirm_status -> chu hien thi */
export const CONFIRM_LABELS = {
  PENDING:  'Chờ xác nhận',
  ACCEPTED: 'Đã đồng ý',
  REJECTED: 'Đã từ chối',
  EXPIRED:  'Quá hạn',
}

/** Chu cua ma, ma la (chua co trong bang) thi hien nguyen ma. */
export function statusLabel(code) {
  return STATUS_LABELS[code] ?? code
}

// Chu tieng Viet cua tinh chat bai (ORIGINAL -> Sang tac...)
export function natureLabel(code) {
  return NATURE_LABELS[code] ?? code
}

// Chu tieng Viet cua trang thai xac nhan dong tac gia (PENDING -> Cho xac nhan...)
export function confirmLabel(code) {
  return CONFIRM_LABELS[code] ?? code
}
