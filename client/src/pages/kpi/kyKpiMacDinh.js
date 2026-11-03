/**
 * Ky KPI mac dinh khi mo cac trang KPI (BF-05): thang hien tai theo gio may,
 * thay cho so cung 9/2026 truoc day. Chi cac trang trong pages/kpi dung.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   11  NAM_BAT_DAU                     xem tom tat dau file
 *   14  namHienTai()                    Nam hien tai theo gio may
 *   19  thangHienTai()                  Thang hien tai (1-12): getMonth() dem tu 0 nen phai cong 1
 *   24  danhSachNam()                   Cac nam cho o chon nam: tu NAM_BAT_DAU toi nam sau (de dat chi tieu truoc)
 */
const NAM_BAT_DAU = 2025

/** Nam hien tai theo gio may */
export function namHienTai() {
  return new Date().getFullYear()
}

/** Thang hien tai (1-12): getMonth() dem tu 0 nen phai cong 1 */
export function thangHienTai() {
  return new Date().getMonth() + 1
}

/** Cac nam cho o chon nam: tu NAM_BAT_DAU toi nam sau (de dat chi tieu truoc) */
export function danhSachNam() {
  const ketQua = []
  for (let nam = NAM_BAT_DAU; nam <= namHienTai() + 1; nam++) {
    ketQua.push(nam)
  }
  return ketQua
}
