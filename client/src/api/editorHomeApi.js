/**
 * ===========================================================================
 * DU LIEU GIA cho trang chu Bien tap vien - CHUA CO API THAT.
 * ===========================================================================
 * Ham tra ve Promise giong goiApi va tra dung phan "data" cua ApiResponse.
 * Khi backend xong (luong assessment BF-02 + appeal BF-03) chi can doi ruot
 * ham thanh goiApi('/api/...'), component goi ham KHONG phai sua.
 * Ten field theo DTO tieng Anh (camelCase), trang thai dung chu trong CHECK
 * cua database/schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   25  layTrangChuBienTapVien()        Tat ca so lieu trang chu cua BTV dang dang nhap: - statusCounts: so b… [du lieu gia (chua co API)]
 */
import { giaLapGoiApi, cachHomNay } from './duLieuGia.js'

/**
 * Tat ca so lieu trang chu cua BTV dang dang nhap:
 * - statusCounts: so bai DUOC GIAO CHO TOI (assignments.editor_id) theo articles.status
 * - rescoreRequests: Truong ban tra cham lai (score_approvals.decision = RETURNED, reason bat buoc) - UC-32
 * - reReviewTasks: phieu cham lai do khieu nai, cham mu (scoring_attempts.attempt_type = APPEAL) - UC-36, 37
 * - assignedArticles: bai da giao nhung chua cham (articles.status = ASSIGNED), cu nhat truoc - UC-21
 * - crossReviewTasks: phieu cham cheo PENDING (cross_review_tasks.reviewer_id = toi) - UC-40
 * - thisMonth: thong ke thang nay (cac lan cham da gui, so bai bi khieu nai) - UC-42
 */
export async function layTrangChuBienTapVien() {
  return giaLapGoiApi({
    statusCounts: [
      { status: 'ASSIGNED',           count: 3 },
      { status: 'PENDING_APPROVAL',   count: 2 },
      { status: 'REVISION_REQUESTED', count: 1 },
      { status: 'SCORE_PUBLISHED',    count: 1 },
      { status: 'PUBLISHED',          count: 6 },
      { status: 'REJECTED',           count: 1 },
    ],
    rescoreRequests: [
      {
        articleId: 21,
        title: 'Ùn tắc giao thông giờ cao điểm tại cửa ngõ phía Tây',
        reason: 'Tiêu chí "Nguồn tin" chấm 85 là cao, bài chỉ có 1 nguồn chính thức.',
        returnedAt: cachHomNay(-1),
      },
    ],
    reReviewTasks: [
      {
        appealId: 4,
        articleId: 9,
        title: 'Phỏng vấn chuyên gia về chuyển đổi số y tế',
        assignedAt: cachHomNay(-2),
      },
    ],
    assignedArticles: [
      { articleId: 25, title: 'Hà Nội mở thêm 3 tuyến buýt điện', categoryName: 'Tin tức', assignedAt: cachHomNay(-3) },
      { articleId: 27, title: 'Người dân vùng lũ dựng lại nhà sau bão', categoryName: 'Phóng sự', assignedAt: cachHomNay(-1) },
    ],
    crossReviewTasks: [
      { taskId: 11, articleId: 18, title: 'Chợ đêm mới hút khách cuối tuần', batchName: 'Đợt chấm chéo tháng 9/2026' },
    ],
    thisMonth: {
      scoredCount: 14,
      avgScore: 76.4,
      appealedCount: 1,
    },
  })
}
