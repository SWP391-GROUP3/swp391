import { goiApi } from './goiApi.js'
import { layToken } from '../auth/phienDangNhap.js'

/**
 * API client cho chuc nang nhap so lieu doc gia tu CSV va xem nhat ky (UC-63, BF-05).
 *
 * MUC LUC (so dong)
 *   12  taiLenCsvDocGia              - Gui file CSV len server de import
 *   20  layNhatKyImport              - Lay danh sach lich su cac lan import (co phan trang)
 *   27  taiFileCsvMau                - Tai file CSV mau cho so lieu doc gia
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
  const token = layToken()
  const headers = {}
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch('/api/reader-imports/template', {
    method: 'GET',
    headers,
  })

  if (!res.ok) {
    throw new Error('Không thể tải file CSV mẫu.')
  }

  const blob = await res.blob()
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'reader_data_template.csv'
  document.body.appendChild(a)
  a.click()
  window.URL.revokeObjectURL(url)
  document.body.removeChild(a)
}
