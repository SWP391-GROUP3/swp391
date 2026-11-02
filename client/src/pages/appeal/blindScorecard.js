/**
 * Ham thuan cho phieu cham mu (BF-03): tinh tong diem tam tren giao dien va kiem tra phieu da cham du
 * tieu chi chua.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   10  tinhTongTam()                   tinh tong tam
 *   21  kiemTraPhieuCham()              kiem tra phieu cham
 */

export function tinhTongTam(cacDong) { // [LUONG 3 - CHAM MU] 2c: tong TAM de nguoi cham xem truoc; con so duoc luu la con so SERVER tinh o buoc 7
  let tong = 0
  for (const dong of cacDong) {
    if (dong.score === '' || dong.score == null) return null
    const diem = Number(dong.score)
    if (!Number.isFinite(diem) || diem < 0 || diem > 100) return null
    tong += Math.round(diem * 100) * Math.round(Number(dong.weight) * 100)
  }
  return (Math.round(tong / 10000) / 100).toFixed(2)
}

export function kiemTraPhieuCham(cacDong) {
  for (const dong of cacDong) {
    if (dong.score === '' || dong.score == null) return 'Phải chấm đủ mọi tiêu chí trước khi nộp.'
    const diem = Number(dong.score)
    if (!Number.isFinite(diem) || diem < 0 || diem > 100) return 'Điểm phải từ 0 đến 100.'
    if (diem < 50 && !dong.comment?.trim()) return 'Phải nhập nhận xét cho tiêu chí có điểm dưới 50.'
  }
  return ''
}
