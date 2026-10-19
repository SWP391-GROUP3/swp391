/**
 * MUC LUC (so dong)
 *    7  dinhDangThoiGian
 *   14  thoiGianConLai
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
