import { tenVaiTro } from '../auth/taiKhoanTam.js'

/**
 * Trang chinh sau khi dang nhap. Hien tai moi co khung, chua co noi dung.
 * Nguoi phu trach tung chuc nang chen phan cua minh vao <main> ben duoi.
 */
export default function Main({ nguoiDung, onDangXuat }) {
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
        {/* ---------------------------------------------------------------
            CHO DAT NOI DUNG. Hien tai de trong theo dung yeu cau.
            --------------------------------------------------------------- */}
      </main>
    </>
  )
}
