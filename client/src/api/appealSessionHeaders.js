// TODO(auth): thay X-User-Id bang token khi luong dang nhap duoc tich hop.
const MA_NGUOI_DUNG_TAM = {
  'an.nv': 1, 'binh.tt': 2, 'cuong.lh': 3, 'dung.pt': 4,
  'duc.vm': 5, 'chi.hl': 6, 'hung.dq': 7, admin: 8,
}

export function layHeaderNguoiDung() {
  let nguoiDung
  try {
    nguoiDung = JSON.parse(sessionStorage.getItem('nguoiDung') || 'null')
  } catch {
    throw new Error('Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.')
  }
  const maNguoiDung = nguoiDung?.id ?? MA_NGUOI_DUNG_TAM[nguoiDung?.username]
  if (!maNguoiDung) throw new Error('Vui lòng đăng nhập để tiếp tục.')
  return { 'X-User-Id': String(maNguoiDung) }
}
