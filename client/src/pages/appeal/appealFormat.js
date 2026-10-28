/**
 * Ham dinh dang dung chung cho cac man BF-03: hien thoi gian va thoi gian con lai cua han khieu nai /
 * han cham.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   10  dinhDangThoiGian()              dinh dang thoi gian
 *   17  thoiGianConLai()                thoi gian con lai
 */

export function dinhDangThoiGian(giaTri) {
  if (!giaTri) return '—'
  const ngay = new Date(giaTri)
  if (Number.isNaN(ngay.getTime())) return '—'
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(ngay)
}

export function thoiGianConLai(hanChot) {
  const soPhut = Math.max(0, Math.ceil((new Date(hanChot).getTime() - Date.now()) / 60000))
  const soNgay = Math.floor(soPhut / 1440)
  const soGio = Math.floor((soPhut % 1440) / 60)
  return `${soNgay} ngày ${soGio} giờ ${soPhut % 60} phút`
}
