import { useState } from 'react'
import CriteriaManagement from './CriteriaManagement.jsx'
import AssignmentQueue from './AssignmentQueue.jsx'
import EditorTaskQueue from './EditorTaskQueue.jsx'
import ScoreCalculatorTester from './ScoreCalculatorTester.jsx'

/**
 * Dashboard tong hop cho Luong BF-02 (Phan cong, Danh gia chat luong & Duyet dang).
 * Co the nhung truc tiep vao Main.jsx ma khong lam vo bat ky giao dien chung nao.
 */
export default function Flow2Dashboard({ nguoiDung }) {
  // Tu dong chon Tab phu hop voi vai tro dang nhap
  const vaiTro = nguoiDung?.vaiTro || []
  const defaultTab = vaiTro.includes('ADMIN')
    ? 'CRITERIA'
    : vaiTro.includes('HEAD_OF_DEPARTMENT')
    ? 'ASSIGNMENT'
    : vaiTro.includes('EDITOR')
    ? 'EDITOR_TASKS'
    : 'CRITERIA'

  const [activeTab, setActiveTab] = useState(defaultTab)

  const tabs = [
    { id: 'CRITERIA', label: '⚙️ Cấu hình Tiêu chí (Admin)' },
    { id: 'ASSIGNMENT', label: '📋 Phân công Bài viết (Trưởng ban)' },
    { id: 'EDITOR_TASKS', label: '✍️ Hàng việc của tôi (Biên tập viên)' },
    { id: 'CALCULATOR', label: '🧮 Mô phỏng Tính điểm & BR-QA-08' }
  ]

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '16px' }}>
      {/* Header thong tin module */}
      <div style={{ marginBottom: '20px', paddingBottom: '14px', borderBottom: '2px solid #e2e8f0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0, color: '#14532d', fontSize: '22px' }}>
            Luồng BF-02: Phân công &amp; Đánh giá chất lượng
          </h2>
          <span style={{ color: '#64748b', fontSize: '13.5px' }}>
            Phân hệ phụ trách: <strong>Nguyễn Tùng Dương</strong> | Iteration 1 (Giao diện chuẩn chức năng Backend)
          </span>
        </div>

        {nguoiDung && (
          <div style={{ textAlign: 'right', fontSize: '13px', color: '#475569' }}>
            Đang đăng nhập: <strong>{nguoiDung.hoTen}</strong>
          </div>
        )}
      </div>

      {/* Thanh chuyen Tab */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', borderBottom: '1px solid #cbd5e1', paddingBottom: '8px' }}>
        {tabs.map(t => (
          <button
            key={t.id}
            type="button"
            onClick={() => setActiveTab(t.id)}
            style={{
              padding: '9px 16px',
              fontSize: '14px',
              fontWeight: activeTab === t.id ? '600' : '400',
              color: activeTab === t.id ? '#14532d' : '#475569',
              background: activeTab === t.id ? '#dcfce7' : '#f8fafc',
              border: activeTab === t.id ? '1px solid #86efac' : '1px solid #e2e8f0',
              borderRadius: '6px',
              cursor: 'pointer',
              transition: 'all 0.15s ease'
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* Noi dung Tab duoc chon */}
      {activeTab === 'CRITERIA' && <CriteriaManagement nguoiDung={nguoiDung} />}
      {activeTab === 'ASSIGNMENT' && <AssignmentQueue nguoiDung={nguoiDung} />}
      {activeTab === 'EDITOR_TASKS' && <EditorTaskQueue nguoiDung={nguoiDung} />}
      {activeTab === 'CALCULATOR' && <ScoreCalculatorTester />}
    </div>
  )
}
