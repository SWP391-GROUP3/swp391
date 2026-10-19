import { useEffect, useRef, useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import Icon from '../components/common/Icon.jsx'
import { chuCaiDau } from '../utils/chuCaiDau.js'
import '../styles/AppLayout.css'

/** Muc "Trang chủ" - vai tro nao cung co, AppLayout tu them o dau menu. */
const MUC_TRANG_CHU = {
  path: '/main',
  nhan: 'Trang chủ',
  icon: <><rect x="4" y="4" width="6" height="6" rx="1" /><rect x="14" y="4" width="6" height="6" rx="1" /><rect x="4" y="14" width="6" height="6" rx="1" /><rect x="14" y="14" width="6" height="6" rx="1" /></>,
}

/** 1 muc menu. NavLink tu them class "active" va aria-current="page" cho trang dang mo. */
function MucMenu({ muc }) {
  return (
    // end: chi sang khi URL khop dung, de /my-articles khong sang cung luc voi /my-articles/new.
    // title: hien tooltip khi sidebar thu nho chi con icon.
    <NavLink to={muc.path} end className="khung-app__muc" title={muc.nhan}>
      <Icon>{muc.icon}</Icon>
      <span>{muc.nhan}</span>
    </NavLink>
  )
}

/**
 * Ten + avatar o goc phai thanh tren. Bam vao -> menu tha: "Hồ sơ của tôi", "Đăng xuất".
 * Bam ra ngoai menu hoac nhan Esc thi dong.
 */
function MenuTaiKhoan({ nguoiDung, onDangXuat }) {
  const [moMenu, setMoMenu] = useState(false)
  const vungRef = useRef(null)   // ca nut + menu, de biet cu bam co nam ngoai khong

  useEffect(() => {
    if (!moMenu) {
      return
    }
    function bamRaNgoai(event) {
      if (vungRef.current && !vungRef.current.contains(event.target)) {
        setMoMenu(false)
      }
    }
    function nhanPhim(event) {
      if (event.key === 'Escape') {
        setMoMenu(false)
      }
    }
    document.addEventListener('mousedown', bamRaNgoai)
    document.addEventListener('keydown', nhanPhim)
    // Dong menu / roi trang thi go 2 ham nghe, khong de chay mai
    return () => {
      document.removeEventListener('mousedown', bamRaNgoai)
      document.removeEventListener('keydown', nhanPhim)
    }
  }, [moMenu])

  return (
    <div className="khung-app__tai-khoan" ref={vungRef}>
      <button
        type="button"
        className="khung-app__nut-tai-khoan"
        aria-expanded={moMenu}
        aria-controls="menu-tai-khoan"
        onClick={() => setMoMenu(!moMenu)}
      >
        <span className="khung-app__ten">
          <span className="khung-app__ho-ten">{nguoiDung.hoTen}</span>
          <span className="khung-app__vai-tro">{nguoiDung.tenVaiTro}</span>
        </span>
        <span className="khung-app__avatar" aria-hidden="true">{chuCaiDau(nguoiDung.hoTen)}</span>
        <span className="an-di">Mở menu tài khoản</span>
      </button>

      {moMenu && (
        <div id="menu-tai-khoan" className="khung-app__menu-tk">
          <div className="khung-app__menu-tk-dau">
            <span className="khung-app__ho-ten">{nguoiDung.hoTen}</span>
            <span className="khung-app__vai-tro">{nguoiDung.email}</span>
          </div>
          <Link to="/profile" className="khung-app__menu-tk-muc" onClick={() => setMoMenu(false)}>
            <Icon><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></Icon>
            Hồ sơ của tôi
          </Link>
          <button type="button" className="khung-app__menu-tk-muc" onClick={onDangXuat}>
            <Icon><path d="M10 4H5v16h5" /><path d="M14 8l4 4-4 4M18 12H9" /></Icon>
            Đăng xuất
          </button>
        </div>
      )}
    </div>
  )
}

/**
 * Khung trang dung chung cho moi vai tro: sidebar trai + thanh tren + vung noi dung.
 * Moi vai tro co 1 file layout rieng (ReporterLayout, EditorLayout...) chi khai bao
 * menu cua minh roi goi AppLayout. Trang con truyen vao qua `children`.
 *
 * - tenMenu:  ten cua menu cho trinh doc man hinh, vd "Chức năng phóng viên"
 * - nhomMenu: [{ tieuDe, cacMuc: [{ path, nhan, icon }] }]. tieuDe rong = nhom khong co tieu de.
 *             Muc "Trang chủ" luon tu them o dau, khong can khai bao.
 * - phanThem: hien them tren thanh tren, truoc ten nguoi dung (vd chip vi cua Phong vien)
 *
 * MUC LUC (so dong)
 *    8  MUC_TRANG_CHU                - Muc "Trang chủ" - vai tro nao cung co, AppLayout tu them o dau menu.
 *   15  MucMenu                      - 1 muc menu. NavLink tu them class "active" va aria-current="page" cho t…
 *   30  MenuTaiKhoan                 - Ten + avatar o goc phai thanh tren. Bam vao -> menu tha: "Hồ sơ của tôi…
 *   34    useEffect                  - tai du lieu khi mo trang
 *  111  AppLayout                    - Khung trang dung chung cho moi vai tro: sidebar trai + thanh tren + vun…
 */
export default function AppLayout({ nguoiDung, onDangXuat, tenMenu, nhomMenu, phanThem, children }) {
  return (
    <div className="khung-app">
      {/* Link an, chi hien khi bam Tab: nguoi dung ban phim nhay thang vao
          noi dung, khong phai Tab qua ca menu moi lan doi trang */}
      <a href="#noi-dung-chinh" className="khung-app__bo-qua">Bỏ qua tới nội dung chính</a>

      <aside className="khung-app__sidebar">
        <div className="khung-app__logo">
          <Icon><path d="M12 3 4 7.5v9L12 21l8-4.5v-9L12 3Z" /><path d="M4 7.5 12 12l8-4.5M12 12v9" /></Icon>
          <span>KPI &amp; Nhuận bút</span>
        </div>

        <nav className="khung-app__menu" aria-label={tenMenu}>
          <MucMenu muc={MUC_TRANG_CHU} />

          {nhomMenu.map((nhom, i) => (
            <div key={nhom.tieuDe || i} className="khung-app__nhom">
              {nhom.tieuDe && <span className="khung-app__nhom-tieu-de">{nhom.tieuDe}</span>}
              {nhom.cacMuc.map((muc) => <MucMenu key={muc.path} muc={muc} />)}
            </div>
          ))}
        </nav>

        <button type="button" className="khung-app__muc khung-app__thoat" onClick={onDangXuat}>
          <Icon><path d="M10 4H5v16h5" /><path d="M14 8l4 4-4 4M18 12H9" /></Icon>
          <span>Đăng xuất</span>
        </button>
      </aside>

      <div className="khung-app__phai">
        <header className="khung-app__thanh-tren">
          {/* TODO: o tim kiem chua hoat dong, cho API tim bai cua BF-01 */}
          <label className="khung-app__tim">
            <Icon><circle cx="11" cy="11" r="6.5" /><path d="m20 20-4.2-4.2" /></Icon>
            <span className="an-di">Tìm bài viết</span>
            <input type="search" name="q" placeholder="Tìm bài viết…" autoComplete="off" />
          </label>

          <div className="khung-app__nguoi-dung">
            {/* TODO: chua co trang thong bao, cho NotificationService dung chung */}
            <button type="button" className="khung-app__nut-icon" aria-label="Thông báo">
              <Icon><path d="M6 10a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" /><path d="M10 20a2 2 0 0 0 4 0" /></Icon>
            </button>

            {phanThem}

            <MenuTaiKhoan nguoiDung={nguoiDung} onDangXuat={onDangXuat} />
          </div>
        </header>

        {/* tabIndex -1: cho phep link "Bo qua" dua focus vao day */}
        <main id="noi-dung-chinh" tabIndex={-1} className="khung-app__noi-dung">{children}</main>
      </div>
    </div>
  )
}
