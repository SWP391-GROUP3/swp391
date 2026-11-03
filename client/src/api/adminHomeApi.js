/**
 * API cho trang chu Admin (M-A6: da bo du lieu gia, ghep tu API that).
 * Rieng quy tac khieu nai do trang chu goi thang appealRuleConfigApi.getActiveConfig.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  layTrangChuAdmin()              So lieu trang chu Admin: - counts: tai khoan theo users.status, the loai dang dung, so bo tieu chi DRAFT - ge…
 */
import { getUsers, getGenres } from './adminApi.js'
import { layDanhSachBoTieuChi } from './assessmentApi.js'
import { layNhatKyImport } from './readerImportApi.js'

/**
 * So lieu trang chu Admin:
 * - counts: tai khoan theo users.status, the loai dang dung, so bo tieu chi DRAFT
 * - genresWithoutCriteria: the loai dang dung nhung chua co bo tieu chi ACTIVE
 *   -> BTV khong cham duoc bai the loai nay (UC-17, UC-22)
 * - draftCriteriaSets: phien ban bo tieu chi DRAFT chua kich hoat - UC-17
 * - readerImport: lan nhap CSV gan nhat (csv_import_logs) - UC-63; chua nhap lan nao -> null.
 *   importedToday = false -> nhac Admin nhap so lieu hom nay.
 */
export async function layTrangChuAdmin() {
  const [taiKhoan, theLoai, nhatKyNhap] = await Promise.all([getUsers(), getGenres(), layNhatKyImport(0, 1)])

  // Moi the loai 1 lan goi danh sach phien ban bo tieu chi (it the loai nen goi song song)
  const phienBanTheoTheLoai = await Promise.all(theLoai.map((tl) => layDanhSachBoTieuChi(tl.id)))

  const genresWithoutCriteria = []
  const draftCriteriaSets = []
  theLoai.forEach((tl, viTri) => {
    const cacPhienBan = phienBanTheoTheLoai[viTri] || []
    if (!cacPhienBan.some((pb) => pb.status === 'ACTIVE')) {
      genresWithoutCriteria.push({ genreId: tl.id, name: tl.name })
    }
    for (const pb of cacPhienBan) {
      if (pb.status === 'DRAFT') {
        draftCriteriaSets.push({ versionId: pb.id, genreId: tl.id, genreName: tl.name, versionNo: pb.versionNo, createdAt: pb.createdAt })
      }
    }
  })

  let readerImport = null
  const lanGanNhat = nhatKyNhap?.content?.[0]
  if (lanGanNhat) {
    const ngayNhap = new Date(lanGanNhat.importedAt)
    const homNay = new Date()
    readerImport = {
      lastFileName: lanGanNhat.fileName,
      lastImportedAt: lanGanNhat.importedAt,
      totalRecords: lanGanNhat.totalRecords,
      successRecords: lanGanNhat.successRecords,
      importedToday: ngayNhap.toDateString() === homNay.toDateString(),
    }
  }

  return {
    counts: {
      activeUsers: taiKhoan.filter((tk) => tk.status === 'ACTIVE').length,
      lockedUsers: taiKhoan.filter((tk) => tk.status !== 'ACTIVE').length,
      activeGenres: theLoai.length,
      draftCriteriaSets: draftCriteriaSets.length,
    },
    genresWithoutCriteria,
    draftCriteriaSets,
    readerImport,
  }
}
