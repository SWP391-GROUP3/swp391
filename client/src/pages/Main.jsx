import { useState } from 'react'
import { tenVaiTro } from '../auth/taiKhoanTam.js'
import AppealRuleConfigPage from './admin/AppealRuleConfigPage.jsx'

/**
 * Trang chinh sau khi dang nhap.
 */
export default function Main({ nguoiDung, onDangXuat }) {
  const [tabHienTai, setTabHienTai] = useState('appeal-rules')

  const laAdmin = nguoiDung?.vaiTro?.includes('ADMIN')

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
        {tabHienTai === 'appeal-rules' && <AppealRuleConfigPage />}
      </main>
    </>
  )
}
