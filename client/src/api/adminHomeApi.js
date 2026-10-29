/**
 * ===========================================================================
 * DU LIEU GIA cho trang chu Admin - CHUA CO API THAT.
 * ===========================================================================
 * (Rieng quy tac khieu nai da co API that: appealRuleConfigApi.getActiveConfig.)
 * Khi backend luong admin xong chi can doi ruot ham thanh goiApi('/api/...'),
 * component goi ham KHONG phai sua. Trang thai dung chu trong CHECK cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  layTrangChuAdmin()              So lieu trang chu Admin: - counts: tai khoan theo users.status, the l… [du lieu gia (chua co API)]
 */
import { giaLapGoiApi, cachHomNay } from './duLieuGia.js'

/**
 * So lieu trang chu Admin:
 * - counts: tai khoan theo users.status, the loai dang dung (genres.is_active),
 *   so bo tieu chi DRAFT (criteria_set_versions)
 * - genresWithoutCriteria: the loai dang dung nhung chua co bo tieu chi ACTIVE
 *   -> BTV khong cham duoc bai the loai nay (UC-17, UC-22)
 * - draftCriteriaSets: phien ban bo tieu chi DRAFT chua kich hoat - UC-17
 * - readerImport: lan nhap CSV gan nhat (csv_import_logs) - UC-63 nhap hang ngay.
 *   importedToday = false -> nhac Admin nhap so lieu hom nay.
 */
export async function layTrangChuAdmin() {
  return giaLapGoiApi({
    counts: {
      activeUsers: 9,
      lockedUsers: 0,
      activeGenres: 8,
      draftCriteriaSets: 1,
    },
    genresWithoutCriteria: [
      { genreId: 7, name: 'Bình luận' },
    ],
    draftCriteriaSets: [
      { versionId: 12, genreId: 3, genreName: 'Phóng sự', versionNo: 2, createdAt: cachHomNay(-1) },
    ],
    readerImport: {
      lastFileName: 'doc-gia-2026-10-12.csv',
      lastImportedAt: cachHomNay(-2),
      totalRecords: 420,
      successRecords: 418,
      importedToday: false,
    },
  })
}
