import { useState } from 'react'
import CriteriaManagement from './CriteriaManagement.jsx'
import AssignmentQueue from './AssignmentQueue.jsx'
import EditorTaskQueue from './EditorTaskQueue.jsx'
import ScoreCalculatorTester from './ScoreCalculatorTester.jsx'
import Icon from '../../components/common/Icon.jsx'
import { BT_TIEU_CHI, BT_PHAN_CONG, BT_BAI_DUOC_GIAO, BT_MAY_TINH } from './bieuTuongBf02.jsx'
import '../../styles/Flow2.css'

/**
 * Dashboard tong hop cho Luong BF-02 (Phan cong, Danh gia chat luong & Duyet dang).
 * Co the nhung truc tiep vao Main.jsx ma khong lam vo bat ky giao dien chung nao.
 *
 * MUC LUC (so dong)
 *   17  Flow2Dashboard               - Dashboard tong hop cho Luong BF-02 (Phan cong, Danh gia chat luong & Du…
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
    { id: 'CRITERIA', label: 'Cấu hình Tiêu chí (Admin)', icon: BT_TIEU_CHI },
    { id: 'ASSIGNMENT', label: 'Phân công Bài viết (Trưởng ban)', icon: BT_PHAN_CONG },
    { id: 'EDITOR_TASKS', label: 'Hàng việc của tôi (Biên tập viên)', icon: BT_BAI_DUOC_GIAO },
    { id: 'CALCULATOR', label: 'Mô phỏng Tính điểm', icon: BT_MAY_TINH }
  ]

  return (
    <div className="bf02">
      {/* Header thong tin module */}
      <div className="bf02__dau">
        <div>
          <h2 className="bf02__ten-luong">
            Luồng BF-02: Phân công &amp; Đánh giá chất lượng
          </h2>
          <p className="bf02__phu-de">
            Phân hệ phụ trách: <strong>Nguyễn Tùng Dương</strong> | Iteration 1 (Giao diện chuẩn chức năng Backend)
          </p>
        </div>

        {nguoiDung && (
          <div className="bf02__nguoi-dung">
            Đang đăng nhập: <strong>{nguoiDung.hoTen}</strong>
          </div>
        )}
      </div>

      {/* Thanh chuyen Tab */}
      <div className="bf02__tabs">
        {tabs.map(t => (
          <button
            key={t.id}
            type="button"
            onClick={() => setActiveTab(t.id)}
            className={activeTab === t.id ? 'bf02__tab bf02__tab--chon' : 'bf02__tab'}
            aria-pressed={activeTab === t.id}
          >
            <Icon>{t.icon}</Icon>
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
