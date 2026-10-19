import { Navigate } from 'react-router-dom'

/**
 * Chan route o phia giao dien:
 * - chua dang nhap            -> day ve /login
 * - co truyen vaiTro ma nguoi dung khong co vai tro do -> day ve /main
 * - hop le                    -> hien noi dung ben trong (children)
 *
 * Chi la chan o giao dien cho dung luong. Backend van PHAI tu kiem tra quyen,
 * vi ai cung goi thang API duoc ma khong qua man hinh.
 * replace: khong luu vao lich su, bam Back khong quay lai trang bi chan.
 *
 * MUC LUC (so dong)
 *   16  RequireRole                  - Chan route o phia giao dien:
 */
export default function RequireRole({ nguoiDung, vaiTro, children }) {
  if (!nguoiDung) {
    return <Navigate to="/login" replace />
  }
  if (vaiTro && !nguoiDung.vaiTro?.includes(vaiTro)) {
    return <Navigate to="/main" replace />
  }
  return children
}
