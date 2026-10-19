/**
 * Ham dung chung cho cac file API dang dung DU LIEU GIA (chua co API that):
 * reporterHomeApi, editorHomeApi, secretaryHomeApi, headHomeApi.
 * Khi backend xong, file API do doi sang goiApi(...) va khong can file nay nua.
 *
 * MUC LUC (so dong)
 *   13  DO_TRE_MS                    - Gia lap do tre mang, de thay duoc trang thai "Dang tai...".
 *   20  giaLapGoiApi                 - Tra ve ban sao cua du lieu sau DO_TRE_MS, giong 1 lan goi API that.
 *   31  cachHomNay                   - Ngay gio cach hom nay `soNgay` ngay, dang "2026-10-15T17:00:00"
 */

/** Gia lap do tre mang, de thay duoc trang thai "Dang tai...". */
const DO_TRE_MS = 300

/**
 * Tra ve ban sao cua du lieu sau DO_TRE_MS, giong 1 lan goi API that.
 * Tra ban sao (structuredClone) de component co lo sua object cung khong
 * lam hong du lieu goc.
 */
export function giaLapGoiApi(duLieu) {
  return new Promise((resolve) => {
    setTimeout(() => resolve(structuredClone(duLieu)), DO_TRE_MS)
  })
}

/**
 * Ngay gio cach hom nay `soNgay` ngay, dang "2026-10-15T17:00:00"
 * (giong LocalDateTime cua Spring tra ve trong JSON).
 * Tinh theo hom nay de han chot gia luc nao cung con y nghia khi demo.
 */
export function cachHomNay(soNgay) {
  const ngay = new Date()
  ngay.setDate(ngay.getDate() + soNgay)
  const nam = ngay.getFullYear()
  const thang = String(ngay.getMonth() + 1).padStart(2, '0')
  const ngayTrongThang = String(ngay.getDate()).padStart(2, '0')
  return `${nam}-${thang}-${ngayTrongThang}T17:00:00`
}
