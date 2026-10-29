import { layToken, xoaPhien } from '../auth/phienDangNhap.js'

/**
 * Ham goi API dung chung cho MOI file trong src/api/ (thay cho fetch truc tiep).
 * Lam san 4 viec de cac luong khong phai tu viet lai:
 *  1. Gan header "Authorization: Bearer <token>" neu da dang nhap
 *  2. Doi body (object) sang JSON
 *  3. Loi -> nem Error co message tieng Viet tu server (ghep ca loi tung truong)
 *  4. Token het han / sai (401) -> xoa phien, ve trang dang nhap
 *
 * Dung:  goiApi('/api/royalty-rates')
 *        goiApi('/api/royalty-rates', { method: 'POST', body: { baseSalary: 2340000 } })
 * @returns phan "data" cua ApiResponse
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  goiApi()                        xem tom tat dau file [GET duongDan]
 *   57  docJson()                       Doc body JSON; body rong / khong phai JSON (vd server tat) thi tra {} de khong vo
 *   66  chuLoi()                        Uu tien loi tung truong (@Valid) -> message chung -> cau mac dinh
 */
export async function goiApi(duongDan, tuyChon = {}) {
  const headers = {}
  const isFormData = typeof FormData !== 'undefined' && tuyChon.body instanceof FormData
  if (tuyChon.body !== undefined && !isFormData) {
    headers['Content-Type'] = 'application/json'
  }
  const token = layToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(duongDan, {
    method: tuyChon.method || 'GET',
    headers,
    body: tuyChon.body !== undefined ? (isFormData ? tuyChon.body : JSON.stringify(tuyChon.body)) : undefined,
  })

  // Dang co token ma van 401 = token het han hoac bi sua -> bat dang nhap lai.
  // (Chua co token ma 401 thi la dang nhap sai, de Login tu hien loi.)
  if (res.status === 401 && token) {
    xoaPhien()
    window.location.href = '/login'
    const loi = new Error('Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.')
    loi.status = res.status
    throw loi
  }

  const json = await docJson(res)
  if (!res.ok || json.success === false) {
    const loi = new Error(chuLoi(json))
    loi.status = res.status
    throw loi
  }
  return json.data
}

/** Doc body JSON; body rong / khong phai JSON (vd server tat) thi tra {} de khong vo. */
async function docJson(res) {
  try {
    return await res.json()
  } catch {
    return {}
  }
}

/** Uu tien loi tung truong (@Valid) -> message chung -> cau mac dinh. */
function chuLoi(json) {
  if (json.errors) {
    return Object.values(json.errors).join(', ')
  }
  return json.message || 'Có lỗi xảy ra, vui lòng thử lại.'
}
