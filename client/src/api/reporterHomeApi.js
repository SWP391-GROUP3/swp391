/**
 * ===========================================================================
 * DU LIEU GIA cho trang chu Phong vien - CHUA CO API THAT.
 * ===========================================================================
 * Moi ham tra ve Promise giong het fetch that va tra dung phan "data" cua
 * ApiResponse. Khi backend xong, chi can thay ruot tung ham bang fetch
 * (xem mau trong kpiApi.js), cac component goi ham KHONG phai sua.
 *
 * Ten field dat theo DTO tieng Anh ma backend se tra (camelCase), gia tri
 * trang thai dung dung chu trong CHECK cua database/schema.sql.
 */
import { layBaiCoTheKhieuNai } from './appealApi.js'
import { giaLapGoiApi, cachHomNay } from './duLieuGia.js'

/**
 * So du vi cua phong vien dang dang nhap.
 * TODO(BF-04): chua co bang luu so du vi -> can PR "CO DOI DB" + sua SRS.
 */
export async function layViTien() {
  return giaLapGoiApi({ balance: 2350000 })
}

/**
 * So bai cua phong vien theo tung trang thai (articles.status).
 * API that se dem bang GROUP BY status; trang thai khong co bai thi count = 0.
 */
export async function layThongKeTrangThai() {
  return giaLapGoiApi([
    { status: 'DRAFT',                count: 3 },
    { status: 'PENDING_CONFIRMATION', count: 1 },
    { status: 'SUBMITTED',            count: 2 },
    { status: 'ASSIGNED',             count: 1 },
    { status: 'REVISION_REQUESTED',   count: 1 },
    { status: 'REJECTED',             count: 0 },
    { status: 'PENDING_APPROVAL',     count: 1 },
    { status: 'SCORE_PUBLISHED',      count: 1 },
    { status: 'PUBLISHED',            count: 5 },
  ])
}

/**
 * Nhung viec phong vien can lam ngay:
 * - revisionRequests: bai bi tra ve (REVISION_REQUESTED), note lay tu
 *   article_status_logs.note
 * - coAuthorInvites: loi moi dong tac gia dang PENDING (article_authors),
 *   phai tra loi truoc confirmDeadline (BR-IN-06, 3 ngay)
 * - appealableScores: bai da cong bo diem, con trong han khieu nai
 *   (score_approvals.announced_at + 7 ngay, GBR-06)
 */
export async function layViecCanXuLy() {
  const baiCoTheKhieuNai = await layBaiCoTheKhieuNai()
  return giaLapGoiApi({
    revisionRequests: [
      {
        articleId: 12,
        title: 'Giá rau xanh tăng mạnh sau đợt mưa lớn',
        note: 'Bổ sung ý kiến của tiểu thương và số liệu của Sở Công Thương.',
        returnedAt: cachHomNay(-1),
      },
    ],
    coAuthorInvites: [
      {
        articleId: 15,
        title: 'Hành trình đưa nông sản Việt sang Nhật Bản',
        mainAuthorName: 'Trần Thị Bình',
        sharePercent: 40,
        confirmDeadline: cachHomNay(2),
      },
    ],
    appealableScores: baiCoTheKhieuNai.map((bai) => ({
      articleId: bai.articleId,
      title: bai.articleTitle,
      totalScore: bai.totalScore,
      announcedAt: bai.announcedAt,
      appealDeadline: bai.appealDeadline,
    })),
  })
}

/**
 * KPI thang hien tai: chi tieu (kpi_targets) va tien do thuc te.
 * actualPublished tinh theo ty le dong tac gia (GBR-13) nen co the le.
 * actualAvgScore = null khi chua co bai nao co diem.
 */
export async function layKpiThangNay() {
  const homNay = new Date()
  return giaLapGoiApi({
    year: homNay.getFullYear(),
    month: homNay.getMonth() + 1,
    minPublished: 6,
    actualPublished: 4,
    minAvgScore: 80,
    actualAvgScore: 78,
  })
}
