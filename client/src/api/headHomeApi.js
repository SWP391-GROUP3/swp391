/**
 * API cho trang chu Truong ban (M-A6: da bo du lieu gia, ghep tu API that cua BF-02, BF-03, BF-05).
 * Hang cho BF-02 do server loc theo ban cua Truong ban dang dang nhap (users.section_id).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  layTrangChuTruongBan()          Tat ca so lieu trang chu cua Truong ban: - counts: so bai cho phan cong / cho duyet diem / cho duyet dang, so…
 */
import { layDanhSachChoPhanCong, layDanhSachChoDuyetDiem, layDanhSachChoDuyetDang } from './assessmentApi.js'
import { layHangChoKhieuNai } from './appealApi.js'
import { layBangKpiBan } from './kpiApi.js'

/**
 * Tat ca so lieu trang chu cua Truong ban:
 * - counts: so bai cho phan cong / cho duyet diem / cho duyet dang, so khieu nai chua xong
 * - newAppeals: khieu nai moi gui (SUBMITTED), cho Truong ban giao nguoi cham mu - UC-38
 * - pendingApprovals: lan cham cho duyet (PENDING_APPROVAL) - UC-29
 * - unassignedArticles: bai SUBMITTED chua phan cong - UC-18
 * - kpiThisMonth: tom tat bang KPI thang (UC-61); noTarget = so PV chua duoc giao chi tieu (UC-59)
 */
export async function layTrangChuTruongBan() {
  const homNay = new Date()
  const nam = homNay.getFullYear()
  const thang = homNay.getMonth() + 1

  const [choPhanCong, choDuyetDiem, choDuyetDang, khieuNaiMoi, khieuNaiDangCham, bangKpi] = await Promise.all([
    layDanhSachChoPhanCong(),
    layDanhSachChoDuyetDiem(),
    layDanhSachChoDuyetDang(),
    layHangChoKhieuNai({ status: 'SUBMITTED' }),
    layHangChoKhieuNai({ status: 'IN_REVIEW' }),
    layBangKpiBan(nam, thang),
  ])

  const dongKpi = bangKpi?.items || []
  let chuaGiao = 0
  for (const dong of dongKpi) {
    if (dong.minPublished == null) {
      chuaGiao++
    }
  }

  return {
    counts: {
      submitted: choPhanCong.length,
      pendingApproval: choDuyetDiem.length,
      scorePublished: choDuyetDang.length,
      openAppeals: khieuNaiMoi.totalElements + khieuNaiDangCham.totalElements,
    },
    newAppeals: khieuNaiMoi.content.map((kn) => ({
      appealId: kn.id,
      title: kn.articleTitle,
      appellantName: kn.appellantName,
      submittedAt: kn.submittedAt,
    })),
    pendingApprovals: choDuyetDiem.map((lan) => ({
      articleId: lan.articleId,
      title: lan.articleTitle,
      editorName: lan.reviewerName,
      totalScore: lan.totalScore,
      submittedAt: lan.submittedAt,
    })),
    unassignedArticles: choPhanCong.map((bai) => ({
      articleId: bai.id,
      title: bai.title,
      authorName: bai.author,
      submittedAt: bai.submittedAt,
    })),
    kpiThisMonth: {
      year: nam,
      month: thang,
      reporterCount: bangKpi?.totalReporters ?? dongKpi.length,
      expectedPass: bangKpi?.passCount ?? 0,
      expectedFail: bangKpi?.notPassCount ?? 0,
      noTarget: chuaGiao,
      finalized: Boolean(bangKpi?.isFinalized ?? bangKpi?.finalized),
    },
  }
}
