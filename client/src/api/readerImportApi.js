import { goiApi } from './goiApi.js'

/**
 * API client cho chuc nang nhap so lieu doc gia tu CSV va xem nhat ky (UC-63, BF-05).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  taiLenCsvDocGia()               Gui file CSV len server de import so lieu doc gia (UC-63) [POST /api/reader-imports]
 *   28  layNhatKyImport()               Lay danh sach lich su cac lan import co phan trang (UC-63) [GET /api/reader-imports/logs?page={page}&size={size}]
 *   35  taiFileCsvMau()                 Tai file CSV mau ve may nguoi dung [GET /api/reader-imports/template]
 */

/**
 * Gui file CSV len server de import so lieu doc gia (UC-63).
 */
export async function taiLenCsvDocGia(file) {
  const formData = new FormData()
  formData.append('file', file)
  return goiApi('/api/reader-imports', {
    method: 'POST',
    body: formData,
  })
}

/**
 * Lay danh sach lich su cac lan import co phan trang (UC-63).
 */
export async function layNhatKyImport(page = 0, size = 10) {
  return goiApi(`/api/reader-imports/logs?page=${page}&size=${size}`)
}

/**
 * Tai file CSV mau ve may nguoi dung.
 */
export async function taiFileCsvMau() {
  // Qua goiApi de tu gan token va tu xu ly het han phien (AGENTS.md muc 4)
  const blob = await goiApi('/api/reader-imports/template', { taiFile: true })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'reader_data_template.csv'
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}
