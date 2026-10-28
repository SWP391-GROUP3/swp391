/** Chu cai dau cua TEN (tu cuoi) de lam avatar: "Nguyễn Văn An" -> "A". */

/**
 * Lay chu cai dau cua TEN (tu cuoi) de lam avatar: "Nguyen Van An" -> "A".
 *
 * MUC LUC (so dong - ten - tac dung)
 *   10  chuCaiDau()                     chu cai dau
 */

export function chuCaiDau(hoTen = '') {
  const cacTu = hoTen.trim().split(' ')
  return cacTu[cacTu.length - 1].charAt(0).toUpperCase()
}
