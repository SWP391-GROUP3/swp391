import { useState } from 'react'
import { tenVaiTro } from '../auth/taiKhoanTam.js'
<<<<<<< HEAD
import AppealRuleConfigPage from './admin/AppealRuleConfigPage.jsx'
=======
import KpiBenchmarksConfig from './kpi/KpiBenchmarksConfig.jsx'
import MonthlyTargetSetting from './kpi/MonthlyTargetSetting.jsx'
>>>>>>> 6845fa6e3ae75e920768ba51801b4fcb7fa79d00

/**
 * Trang chinh sau khi dang nhap.
 */
export default function Main({ nguoiDung, onDangXuat }) {
<<<<<<< HEAD
  const [tabHienTai, setTabHienTai] = useState('appeal-rules')

  const laAdmin = nguoiDung?.vaiTro?.includes('ADMIN')
=======
  // Tab hien tai: 'target' (UC-59) hoac 'benchmark' (UC-58)
  const [tabHienTai, setTabHienTai] = useState('target')

  const laAdmin = nguoiDung?.vaiTro?.includes('ADMIN')
  const laTruongBan = nguoiDung?.vaiTro?.includes('HEAD_OF_DEPARTMENT')
>>>>>>> 6845fa6e3ae75e920768ba51801b4fcb7fa79d00

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

      <div style={{ background: '#fff', borderBottom: '1px solid var(--vien)', padding: '0 24px' }}>
        <nav style={{ display: 'flex', gap: 16 }}>
          <button
            type="button"
            onClick={() => setTabHienTai('appeal-rules')}
            style={{
              padding: '12px 4px',
              border: 0,
              borderBottom: tabHienTai === 'appeal-rules' ? '2px solid var(--chinh)' : '2px solid transparent',
              background: 'none',
              fontWeight: tabHienTai === 'appeal-rules' ? 600 : 400,
              color: tabHienTai === 'appeal-rules' ? 'var(--chinh)' : 'var(--muc-nhat)',
              cursor: 'pointer',
              fontSize: 14,
            }}
          >
            Cấu hình khiếu nại (UC-44)
          </button>
        </nav>
      </div>

      <main className="vung-noi-dung">
<<<<<<< HEAD
        {tabHienTai === 'appeal-rules' && <AppealRuleConfigPage />}
=======
        {/* ---------------------------------------------------------------
            CHO DAT NOI DUNG. Hien tai de trong theo dung yeu cau.
            --------------------------------------------------------------- */}

        {/* Truong hop 1: Tai khoan Trưởng ban -> Hien thi man hinh Giao chi tieu thang (UC-59) */}
        {laTruongBan && !laAdmin && (
          <MonthlyTargetSetting nguoiDung={nguoiDung} />
        )}

        {/* Truong hop 2: Tai khoan Admin -> Hien thi man hinh Cau hinh dinh muc chuan (UC-58) */}
        {laAdmin && !laTruongBan && (
          <KpiBenchmarksConfig />
        )}

        {/* Truong hop dac biet: Tai khoan co ca 2 quyen thi cho phep chuyen Tab */}
        {laTruongBan && laAdmin && (
          <>
            <nav className="thanh-dieu-huong-tab">
              <button
                type="button"
                className={`nut-tab ${tabHienTai === 'target' ? 'dang-chon' : ''}`}
                onClick={() => setTabHienTai('target')}
              >
              Giao chỉ tiêu tháng
              </button>
              <button
                type="button"
                className={`nut-tab ${tabHienTai === 'benchmark' ? 'dang-chon' : ''}`}
                onClick={() => setTabHienTai('benchmark')}
              >
              Cấu hình định mức chuẩn
              </button>
            </nav>
            {tabHienTai === 'target' && <MonthlyTargetSetting nguoiDung={nguoiDung} />}
            {tabHienTai === 'benchmark' && <KpiBenchmarksConfig />}
          </>
        )}
>>>>>>> 6845fa6e3ae75e920768ba51801b4fcb7fa79d00
      </main>
    </>
  )
}
