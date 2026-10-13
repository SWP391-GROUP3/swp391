import { useState } from 'react'
import { tenVaiTro } from '../auth/taiKhoanTam.js'
import KpiBenchmarksConfig from './kpi/KpiBenchmarksConfig.jsx'
import MonthlyTargetSetting from './kpi/MonthlyTargetSetting.jsx'
import AppealRuleConfigPage from './admin/AppealRuleConfigPage.jsx'
import CriteriaManagement from './flow2/CriteriaManagement.jsx'
import AssignmentQueue from './flow2/AssignmentQueue.jsx'
import EditorTaskQueue from './flow2/EditorTaskQueue.jsx'

/**
 * Trang chinh sau khi dang nhap (cac vai tro chua co layout rieng).
 * Moi vai tro chi thay tab cua minh. Chi co 1 tab thi khong hien thanh tab.
 */
export default function Main({ nguoiDung, onDangXuat }) {
  const laAdmin = nguoiDung?.vaiTro?.includes('ADMIN')
  const laTruongBan = nguoiDung?.vaiTro?.includes('HEAD_OF_DEPARTMENT')
  const laBTV = nguoiDung?.vaiTro?.includes('EDITOR')

  // Danh sach tab theo vai tro
  const cacTab = []
  if (laTruongBan) {
    cacTab.push({ ma: 'target', nhan: 'Giao chỉ tiêu tháng' })              // UC-59 (Khang - BF-05)
    cacTab.push({ ma: 'assignment', nhan: 'Phân công bài viết' })           // UC-18, UC-20 (Dương - BF-02)
  }
  if (laAdmin) {
    cacTab.push({ ma: 'benchmark', nhan: 'Cấu hình định mức chuẩn' })       // UC-58 (Khang - BF-05)
    cacTab.push({ ma: 'appeal-rules', nhan: 'Cấu hình khiếu nại' })         // UC-44 (Vũ - BF-03)
    cacTab.push({ ma: 'criteria', nhan: 'Cấu hình tiêu chí chấm' })         // UC-14..17 (Dương - BF-02)
  }
  if (laBTV) {
    cacTab.push({ ma: 'editor-tasks', nhan: 'Hàng việc chấm bài' })          // UC-21 (Dương - BF-02)
  }

  // Mac dinh mo tab dau tien cua vai tro
  const [tabHienTai, setTabHienTai] = useState(cacTab.length > 0 ? cacTab[0].ma : null)

  return (
    <>
      <header className="thanh-tren">
        <span className="thanh-tren__ten">Hệ thống KPI &amp; Nhuận bút</span>

        <div className="thanh-tren__nguoi-dung">
          <span className="ho-ten">{nguoiDung.hoTen}</span>
          <span className="vai-tro">{tenVaiTro(nguoiDung.vaiTro)}</span>
          <button className="nut-thoat" type="button" onClick={onDangXuat}>
            Đăng xuất
          </button>
        </div>
      </header>

      <main className="vung-noi-dung">
        {cacTab.length > 1 && (
          <nav className="thanh-dieu-huong-tab">
            {cacTab.map((tab) => (
              <button
                key={tab.ma}
                type="button"
                className={`nut-tab ${tabHienTai === tab.ma ? 'dang-chon' : ''}`}
                onClick={() => setTabHienTai(tab.ma)}
              >
                {tab.nhan}
              </button>
            ))}
          </nav>
        )}

        {/* Tab của BF-05 (Khang) */}
        {tabHienTai === 'target' && <MonthlyTargetSetting nguoiDung={nguoiDung} />}
        {tabHienTai === 'benchmark' && <KpiBenchmarksConfig />}

        {/* Tab của BF-03 (Vũ) */}
        {tabHienTai === 'appeal-rules' && <AppealRuleConfigPage />}

        {/* Tab của BF-02 (Dương) */}
        {tabHienTai === 'assignment' && <AssignmentQueue nguoiDung={nguoiDung} />}
        {tabHienTai === 'criteria' && <CriteriaManagement nguoiDung={nguoiDung} />}
        {tabHienTai === 'editor-tasks' && <EditorTaskQueue nguoiDung={nguoiDung} />}

        {cacTab.length === 0 && (
          <p className="trong-rong">Chưa có chức năng nào cho vai trò này.</p>
        )}
      </main>
    </>
  )
}
