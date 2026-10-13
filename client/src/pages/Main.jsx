import { useState } from 'react'
import KpiBenchmarksConfig from './kpi/KpiBenchmarksConfig.jsx'
import MonthlyTargetSetting from './kpi/MonthlyTargetSetting.jsx'
import AppealRuleConfigPage from './admin/AppealRuleConfigPage.jsx'

/**
 * Trang chinh sau khi dang nhap (cac vai tro chua co layout rieng).
 * Moi vai tro chi thay tab cua minh. Chi co 1 tab thi khong hien thanh tab.
 */
export default function Main({ nguoiDung, onDangXuat }) {
  const laAdmin = nguoiDung?.vaiTro?.includes('ADMIN')
  const laTruongBan = nguoiDung?.vaiTro?.includes('HEAD_OF_DEPARTMENT')

  // Danh sach tab theo vai tro
  const cacTab = []
  if (laTruongBan) {
    cacTab.push({ ma: 'target', nhan: 'Giao chỉ tiêu tháng' })              // UC-59
  }
  if (laAdmin) {
    cacTab.push({ ma: 'benchmark', nhan: 'Cấu hình định mức chuẩn' })       // UC-58
    cacTab.push({ ma: 'appeal-rules', nhan: 'Cấu hình khiếu nại' })         // UC-44 (Actor: Admin)
  }

  // Mac dinh mo tab dau tien cua vai tro
  const [tabHienTai, setTabHienTai] = useState(cacTab.length > 0 ? cacTab[0].ma : null)

  return (
    <>
      <header className="thanh-tren">
        <span className="thanh-tren__ten">Hệ thống KPI &amp; Nhuận bút</span>

        <div className="thanh-tren__nguoi-dung">
          <span className="ho-ten">{nguoiDung.hoTen}</span>
          <span className="vai-tro">{nguoiDung.tenVaiTro}</span>
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

        {tabHienTai === 'target' && <MonthlyTargetSetting nguoiDung={nguoiDung} />}
        {tabHienTai === 'benchmark' && <KpiBenchmarksConfig />}
        {tabHienTai === 'appeal-rules' && <AppealRuleConfigPage />}

        {cacTab.length === 0 && (
          <p className="trong-rong">Chưa có chức năng nào cho vai trò này.</p>
        )}
      </main>
    </>
  )
}
