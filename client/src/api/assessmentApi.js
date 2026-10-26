import { goiApi } from './goiApi.js'


/**
 * API client cho Luồng BF-02: Phân công, Đánh giá chất lượng & Duyệt đăng.
 *
 * MUC LUC (so dong)
 *   18  layDanhSachBoTieuChi         - UC-14: Lấy danh sách các phiên bản bộ tiêu chí theo thể loại
 *   23  layBoTieuChiActive           - UC-14 / BR-QA-03: Lấy bộ tiêu chí ACTIVE của thể loại
 *   28  taoBoTieuChiDraft            - UC-15, UC-16: Tạo phiên bản bộ tiêu chí mới (DRAFT)
 *   36  kichHoatBoTieuChi            - UC-17: Kích hoạt bộ tiêu chí (DRAFT -> ACTIVE)
 *   43  phanCongBaiViet              - UC-18, UC-20: Trưởng ban phân công bài viết cho BTV
 *   51  layPhanCongBaiViet           - Lấy thông tin phân công của bài viết
 *   56  layBaiDuocGiaoCuaBtv         - UC-21: Danh sách bài được giao của BTV
 *   61  layKhoiLuongCongViecBtv      - UC-19: Khối lượng công việc của BTV
 */

// UC-14: Lấy danh sách các phiên bản bộ tiêu chí theo thể loại
export async function layDanhSachBoTieuChi(categoryId) {
  return await goiApi(`/api/criteria-sets/category/${categoryId}`)
}

export function layBoTieuChiActive(categoryId) {
  return goiApi(`/api/criteria-sets/category/${categoryId}/active`)
}

export function taoBoTieuChiDraft(payload) {
  return goiApi('/api/criteria-sets', { method: 'POST', body: payload })
}

export function kichHoatBoTieuChi(versionId) {
  return goiApi(`/api/criteria-sets/${versionId}/activate`, { method: 'PUT' })
}

export function phanCongBaiViet(payload) {
  return goiApi('/api/assignments', { method: 'POST', body: payload })
}

export function layPhanCongBaiViet(articleId) {
  return goiApi(`/api/assignments/article/${articleId}`)
}

export function layBaiDuocGiaoCuaBtv(editorId) {
  return goiApi(`/api/assignments/editor/${editorId}`)
}

export function layKhoiLuongCongViecBtv(editorId) {
  return goiApi(`/api/assignments/editor/${editorId}/workload`)
}

export function moPhieuCham(articleId) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/open`, { method: 'POST' })
}

export function layPhieuChamMoiNhat(articleId) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/latest`)
}

export function doiTheLoaiBaiViet(articleId, newCategoryId) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/change-category`, {
    method: 'POST',
    body: { newCategoryId }
  })
}

export function luuDiemTieuChi(articleId, danhSachDiem) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/scores`, {
    method: 'PUT',
    body: { scores: danhSachDiem }
  })
}

export function nopPhieuCham(articleId, quyetDinh, ghiChu) {
  return goiApi(`/api/scoring-attempts/article/${articleId}/submit`, {
    method: 'POST',
    body: {
      decision: quyetDinh,
      decisionNote: ghiChu
    }
  })
}

// Truong ban: Lay hang cho duyet diem danh gia (UC-29)
export function layDanhSachChoDuyetDiem(sectionId) {
  const query = sectionId ? `?sectionId=${sectionId}` : ''
  return goiApi(`/api/score-approvals${query}`)
}

// Truong ban: Phe duyet diem danh gia (UC-32)
export function duyetDiemBaiViet(articleId) {
  return goiApi(`/api/score-approvals/article/${articleId}/approve`, {
    method: 'POST'
  })
}

// Truong ban: Yeu cau cham lai voi ly do bat buoc (UC-31)
export function yeuCauChamLai(articleId, reason) {
  return goiApi(`/api/score-approvals/article/${articleId}/return`, {
    method: 'POST',
    body: { reason }
  })
}

// Truong ban: Lay hang cho duyet xuat ban bai viet (UC-33)
export function layDanhSachChoDuyetDang(sectionId) {
  const query = sectionId ? `?sectionId=${sectionId}` : ''
  return goiApi(`/api/score-approvals/publishing${query}`)
}

// Truong ban: Phe duyet xuat ban bai viet (UC-33)
export function duyetDangBaiViet(articleId) {
  return goiApi(`/api/score-approvals/article/${articleId}/publish`, {
    method: 'POST'
  })
}
// API Client cho Luong BF-02 (Phan cong & Danh gia chat luong)
// export function layDanhSachBoTieuChi(categoryId) {
//   return goiApi(`/api/criteria-sets/category/${categoryId}`)
// }
  /**
   * API client cho Luồng BF-02: Phân công, Đánh giá chất lượng & Duyệt đăng.
   *
   * MUC LUC (so dong)
   *   18  layDanhSachBoTieuChi         - UC-14: Lấy danh sách các phiên bản bộ tiêu chí theo thể loại
   *   23  layBoTieuChiActive           - UC-14 / BR-QA-03: Lấy bộ tiêu chí ACTIVE của thể loại
   *   28  taoBoTieuChiDraft            - UC-15, UC-16: Tạo phiên bản bộ tiêu chí mới (DRAFT)
   *   36  kichHoatBoTieuChi            - UC-17: Kích hoạt bộ tiêu chí (DRAFT -> ACTIVE)
   *   43  phanCongBaiViet              - UC-18, UC-20: Trưởng ban phân công bài viết cho BTV
   *   51  layPhanCongBaiViet           - Lấy thông tin phân công của bài viết
   *   56  layBaiDuocGiaoCuaBtv         - UC-21: Danh sách bài được giao của BTV
   *   61  layKhoiLuongCongViecBtv      - UC-19: Khối lượng công việc của BTV
   */

  // UC-14: Lấy danh sách các phiên bản bộ tiêu chí theo thể loại
  export async function layDanhSachBoTieuChi(categoryId) {
    return await goiApi(`/api/criteria-sets/category/${categoryId}`)

  }

  export function layBoTieuChiActive(categoryId) {
    return goiApi(`/api/criteria-sets/category/${categoryId}/active`)
  }

  export function taoBoTieuChiDraft(payload) {
    return goiApi('/api/criteria-sets', { method: 'POST', body: payload })
  }

  export function kichHoatBoTieuChi(versionId) {
    return goiApi(`/api/criteria-sets/${versionId}/activate`, { method: 'PUT' })
  }

  export function phanCongBaiViet(payload) {
    return goiApi('/api/assignments', { method: 'POST', body: payload })
  }

  export function layPhanCongBaiViet(articleId) {
    return goiApi(`/api/assignments/article/${articleId}`)
  }

  export function layBaiDuocGiaoCuaBtv(editorId) {
    return goiApi(`/api/assignments/editor/${editorId}`)
  }

  export function layKhoiLuongCongViecBtv(editorId) {
    return goiApi(`/api/assignments/editor/${editorId}/workload`)
  }

  export function moPhieuCham(articleId) {
    return goiApi(`/api/scoring-attempts/article/${articleId}/open`, { method: 'POST' })
  }

  export function layPhieuChamMoiNhat(articleId) {
    return goiApi(`/api/scoring-attempts/article/${articleId}/latest`)
  }

  export function doiTheLoaiBaiViet(articleId, newCategoryId) {
    return goiApi(`/api/scoring-attempts/article/${articleId}/change-category`, {
      method: 'POST',
      body: { newCategoryId }
    })
  }

  export function luuDiemTieuChi(articleId, danhSachDiem) {
    return goiApi(`/api/scoring-attempts/article/${articleId}/scores`, {
      method: 'PUT',
      body: { scores: danhSachDiem }
    })
  }

  export function nopPhieuCham(articleId, quyetDinh, ghiChu) {
    return goiApi(`/api/scoring-attempts/article/${articleId}/submit`, {
      method: 'POST',
      body: {
        decision: quyetDinh,
        decisionNote: ghiChu
      }
    })
  }

  // Truong ban: Lay hang cho duyet diem danh gia (UC-29)
  export function layDanhSachChoDuyetDiem(sectionId) {
    const query = sectionId ? `?sectionId=${sectionId}` : ''
    return goiApi(`/api/score-approvals${query}`)
  }

  // Truong ban: Phe duyet diem danh gia (UC-32)
  export function duyetDiemBaiViet(articleId) {
    return goiApi(`/api/score-approvals/article/${articleId}/approve`, {
      method: 'POST'
    })
  }

  // Truong ban: Yeu cau cham lai voi ly do bat buoc (UC-31)
  export function yeuCauChamLai(articleId, reason) {
    return goiApi(`/api/score-approvals/article/${articleId}/return`, {
      method: 'POST',
      body: { reason }
    })
  }



