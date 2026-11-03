/**
 * API cho trang chu Phong vien (M-A6: da bo du lieu gia, goi API that).
 * Moi ham tra dung dang du lieu ma cac khoi tren trang chu dang dung
 * (StatusCounter, TodoCard, KpiProgressCard), nen component khong phai sua.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  layThongKeTrangThai()           So bai cua phong vien theo tung trang thai: [{ status, count }]
 *   27  layViecCanXuLy()                Nhung viec phong vien can lam ngay: - revisionRequests: bai bi tra ve (REVISION_REQUESTED), kem ghi chu lan t…
 *   51  layKpiThangNay()                KPI thang hien tai cua phong vien dang dang nhap
 */
import { getMyStatusCounts, getMyRevisionRequests, getMyCoAuthorInvites } from './articleApi.js'
import { layBaiCoTheKhieuNai } from './appealApi.js'
import { layChiTietKpiPhongVien } from './kpiApi.js'
import { docPhien } from '../auth/phienDangNhap.js'

/** So bai cua phong vien theo tung trang thai: [{ status, count }]. */
export function layThongKeTrangThai() {
  return getMyStatusCounts()
}

/**
 * Nhung viec phong vien can lam ngay:
 * - revisionRequests: bai bi tra ve (REVISION_REQUESTED), kem ghi chu lan tra gan nhat
 * - coAuthorInvites: loi moi dong tac gia dang cho minh tra loi (BR-IN-06)
 * - appealableScores: bai da cong bo diem, con trong han khieu nai (BF-03)
 */
export async function layViecCanXuLy() {
  const [revisionRequests, coAuthorInvites, baiCoTheKhieuNai] = await Promise.all([
    getMyRevisionRequests(),
    getMyCoAuthorInvites(),
    layBaiCoTheKhieuNai(),
  ])
  return {
    revisionRequests: revisionRequests || [],
    coAuthorInvites: coAuthorInvites || [],
    appealableScores: (baiCoTheKhieuNai || []).map((bai) => ({
      articleId: bai.articleId,
      title: bai.articleTitle,
      totalScore: bai.totalScore,
      announcedAt: bai.announcedAt,
      appealDeadline: bai.appealDeadline,
    })),
  }
}

/**
 * KPI thang hien tai cua phong vien dang dang nhap.
 * actualPublished tinh theo ty le dong tac gia (GBR-13) nen co the le.
 * Truong ban chua giao chi tieu thang nay -> tra null (khoi KPI hien "chua giao chi tieu").
 */
export async function layKpiThangNay() {
  const homNay = new Date()
  const nam = homNay.getFullYear()
  const thang = homNay.getMonth() + 1
  const nguoiDung = docPhien()?.nguoiDung
  const kpi = await layChiTietKpiPhongVien(nguoiDung?.id, nam, thang)
  // API KPI tra chi tieu 0 / 0 khi thang nay chua co dong kpi_targets -> cung coi la chua giao
  if (!kpi || kpi.minPublished == null || (Number(kpi.minPublished) === 0 && Number(kpi.minAvgScore) === 0)) {
    return null
  }
  return {
    year: nam,
    month: thang,
    minPublished: kpi.minPublished,
    actualPublished: Number(kpi.exactActualArticles ?? kpi.actualArticles ?? 0),
    minAvgScore: Number(kpi.minAvgScore),
    actualAvgScore: kpi.actualAvgScore == null ? null : Number(kpi.actualAvgScore),
  }
}
