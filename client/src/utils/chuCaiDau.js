/** Chu cai dau cua TEN (tu cuoi) de lam avatar: "Nguyễn Văn An" -> "A". */
/**
 * MUC LUC (so dong)
 *    7  chuCaiDau                    - Chu cai dau cua TEN (tu cuoi) de lam avatar: "Nguyễn Văn An" -> "A".
 */

export function chuCaiDau(hoTen = '') {
  const cacTu = hoTen.trim().split(' ')
  return cacTu[cacTu.length - 1].charAt(0).toUpperCase()
}
