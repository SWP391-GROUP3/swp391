import '../../styles/Icon.css'

/**
 * Khung icon SVG dung chung (net 1.5px giong Figma), khong cai thu vien icon.
 * Truyen cac hinh <path>/<rect>/<circle> vao children, vd:
 *   <Icon><circle cx="11" cy="11" r="6.5" /></Icon>
 * aria-hidden: icon chi de trang tri, trinh doc man hinh doc chu ben canh.
 */
export default function Icon({ children }) {
  return (
    <svg className="icon" viewBox="0 0 24 24" width="20" height="20" fill="none"
      stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"
      strokeLinejoin="round" aria-hidden="true">
      {children}
    </svg>
  )
}
