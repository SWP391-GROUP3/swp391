import { useState, useEffect, useRef } from 'react'
import { taiLenCsvDocGia, layNhatKyImport, taiFileCsvMau } from '../../api/readerImportApi.js'
import Icon from '../../components/common/Icon.jsx'
import './ReaderDataImport.css'

/**
 * Màn hình Nhập số liệu tương tác độc giả (UC-63, BF-05 Admin).
 * Cho phép Quản trị viên tải lên file CSV số liệu đọc hàng ngày (ghi đè nếu trùng),
 * xem thống kê xử lý và theo dõi nhật ký lịch sử nhập file (audit log).
 *
 * MUC LUC (so dong)
 *   20  ReaderDataImport             - Component man hinh chinh
 *   36    taiLichSuImport()          - Lay danh sach nhat ky import theo trang
 *   52    xuLyChonFile()             - Xu ly khi nguoi dung chon file CSV
 *   64    xuLyKopThaFile()           - Ho tro keo tha file
 *   82    xuLyTaiLen()               - Gui file len backend thuc thi import
 *  110    dinhDangNgayGio()          - Format thoi gian hien thi
 */
export default function ReaderDataImport() {
  const [tabHienTai, setTabHienTai] = useState('upload') // 'upload' | 'history'

  // Tab 1: Upload state
  const [fileChon, setFileChon] = useState(null)
  const [dangTaiLen, setDangTaiLen] = useState(false)
  const [loiUpload, setLoiUpload] = useState('')
  const [ketQua, setKetQua] = useState(null)
  const [laDragOver, setLaDragOver] = useState(false)
  const fileInputRef = useRef(null)

  // Tab 2: History state
  const [lichSu, setLichSu] = useState([])
  const [trang, setTrang] = useState(0)
  const [tongTrang, setTongTrang] = useState(0)
  const [tongBanGhi, setTongBanGhi] = useState(0)
  const [dangTaiLichSu, setDangTaiLichSu] = useState(false)
  const [loiLichSu, setLoiLichSu] = useState('')

  useEffect(() => {
    if (tabHienTai === 'history') {
      taiLichSuImport(trang)
    }
  }, [tabHienTai, trang])

  async function taiLichSuImport(pageIndex = 0) {
    setDangTaiLichSu(true)
    setLoiLichSu('')
    try {
      const res = await layNhatKyImport(pageIndex, 10)
      if (res) {
        setLichSu(res.content || [])
        setTongTrang(res.totalPages || 0)
        setTongBanGhi(res.totalElements || 0)
      }
    } catch (err) {
      setLoiLichSu(err.message || 'Không thể tải lịch sử import.')
    } finally {
      setDangTaiLichSu(false)
    }
  }

  function xuLyChonFile(e) {
    const file = e.target.files?.[0]
    if (file) {
      if (!file.name.toLowerCase().endsWith('.csv')) {
        setLoiUpload('Vui lòng chọn file có định dạng .csv.')
        setFileChon(null)
        return
      }
      setFileChon(file)
      setLoiUpload('')
      setKetQua(null)
    }
  }

  function xuLyKeoTha(e) {
    e.preventDefault()
    setLaDragOver(false)
    const file = e.dataTransfer.files?.[0]
    if (file) {
      if (!file.name.toLowerCase().endsWith('.csv')) {
        setLoiUpload('Vui lòng chọn file có định dạng .csv.')
        setFileChon(null)
        return
      }
      setFileChon(file)
      setLoiUpload('')
      setKetQua(null)
    }
  }

  async function xuLyTaiLen(e) {
    e.preventDefault()
    if (!fileChon) {
      setLoiUpload('Vui lòng chọn một file CSV trước khi thực hiện.')
      return
    }

    setDangTaiLen(true)
    setLoiUpload('')
    setKetQua(null)

    try {
      const res = await taiLenCsvDocGia(fileChon)
      setKetQua(res)
      // Reset input sau khi tai len thanh cong
      setFileChon(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
    } catch (err) {
      setLoiUpload(err.message || 'Đã có lỗi xảy ra khi nhập file CSV.')
    } finally {
      setDangTaiLen(false)
    }
  }

  function dinhDangNgayGio(isoString) {
    if (!isoString) return '—'
    try {
      const d = new Date(isoString)
      if (isNaN(d.getTime())) return '—'
      return d.toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      })
    } catch {
      return isoString
    }
  }

  return (
    <div className="nhap-so-lieu">
      {/* Header */}
      <div className="nhap-so-lieu__header">
        <h1 className="nhap-so-lieu__tieu-de">Nhập số liệu độc giả (Reader Data Import)</h1>
        <p className="nhap-so-lieu__mo-ta">
          Chức năng thuộc phân hệ Admin &amp; KPI (BF-05 UC-63): Nhập dữ liệu tương tác độc giả hàng ngày từ file CSV,
          tự động ghi đè bản ghi trùng lặp và lưu vết lịch sử import.
        </p>
      </div>

      {/* Tabs */}
      <div className="nhap-so-lieu__tabs">
        <button
          type="button"
          className={`nhap-so-lieu__tab-btn ${tabHienTai === 'upload' ? 'nhap-so-lieu__tab-btn--active' : ''}`}
          onClick={() => setTabHienTai('upload')}
        >
          <Icon>
            <path d="M12 4v11M7 10l5 5 5-5" />
            <path d="M4 18v2h16v-2" />
          </Icon>
          Tải lên số liệu
        </button>
        <button
          type="button"
          className={`nhap-so-lieu__tab-btn ${tabHienTai === 'history' ? 'nhap-so-lieu__tab-btn--active' : ''}`}
          onClick={() => setTabHienTai('history')}
        >
          <Icon>
            <circle cx="12" cy="12" r="8" />
            <path d="M12 8v4l3 2" />
          </Icon>
          Nhật ký nhập file (Audit History)
        </button>
      </div>

      {/* Noi dung Tab 1: Upload */}
      {tabHienTai === 'upload' && (
        <div className="nhap-so-lieu__noi-dung-upload">
          {/* Banner quy tac nghiep vu */}
          <div className="nhap-so-lieu__banner-info">
            <strong>Quy tắc nghiệp vụ CSV (BR-KPI-03, BR-KPI-04):</strong>
            <ul>
              <li>
                File CSV yêu cầu 5 cột bắt buộc: <code>article_id, record_date, page_views, rating_count, avg_rating</code>.
              </li>
              <li>
                Nếu trùng cặp <code>(article_id, record_date)</code>, hệ thống sẽ tự động cập nhật ghi đè bản ghi cũ.
              </li>
              <li>
                Các dòng có <code>article_id</code> không tồn tại trên hệ thống sẽ được bỏ qua và ghi nhận cảnh báo chi tiết.
              </li>
              <li>
                Số liệu độc giả chỉ dùng cho phân tích, xếp hạng chuyên sâu và không ảnh hưởng tới kết quả PASS/NOT_PASS của phóng viên.
              </li>
            </ul>
          </div>

          {/* Thanh hanh dong */}
          <div className="nhap-so-lieu__action-bar">
            <button
              type="button"
              className="nhap-so-lieu__btn-mau"
              onClick={taiFileCsvMau}
              title="Tải file CSV mẫu để kiểm tra đúng cấu trúc cột"
            >
              <Icon>
                <path d="M12 3v13M7 11l5 5 5-5" />
                <path d="M5 20h14" />
              </Icon>
              Tải file mẫu (.csv)
            </button>

            {fileChon && (
              <button
                type="button"
                className="nhap-so-lieu__btn-submit"
                onClick={xuLyTaiLen}
                disabled={dangTaiLen}
              >
                <Icon>
                  <path d="M12 4v11M7 10l5 5 5-5" />
                  <path d="M4 18v2h16v-2" />
                </Icon>
                {dangTaiLen ? 'Đang xử lý import...' : 'Tiến hành Nhập dữ liệu'}
              </button>
            )}
          </div>

          {/* Vung chon/keo tha file */}
          <div
            className={`nhap-so-lieu__upload-box ${laDragOver ? 'nhap-so-lieu__upload-box--dragover' : ''}`}
            onDragOver={(e) => {
              e.preventDefault()
              setLaDragOver(true)
            }}
            onDragLeave={() => setLaDragOver(false)}
            onDrop={xuLyKeoTha}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv"
              className="nhap-so-lieu__upload-input"
              onChange={xuLyChonFile}
            />
            <div className="nhap-so-lieu__upload-icon">
              <Icon>
                <path d="M12 4v11M7 10l5 5 5-5" />
                <path d="M4 18v2h16v-2" />
              </Icon>
            </div>
            <div className="nhap-so-lieu__upload-text">
              Kéo thả file CSV vào đây hoặc bấm để chọn từ máy tính
            </div>
            <div className="nhap-so-lieu__upload-subtext">
              Định dạng hỗ trợ: .csv (mã hóa UTF-8)
            </div>

            {fileChon && (
              <div className="nhap-so-lieu__file-da-chon" onClick={(e) => e.stopPropagation()}>
                <Icon>
                  <rect x="5" y="4" width="14" height="17" rx="2" />
                  <path d="M9 12h6M9 16h4" />
                </Icon>
                <span>{fileChon.name} ({(fileChon.size / 1024).toFixed(1)} KB)</span>
              </div>
            )}
          </div>

          {/* Thong bao loi neu co */}
          {loiUpload && (
            <div className="nhap-so-lieu__alert nhap-so-lieu__alert--loi">
              <Icon>
                <circle cx="12" cy="12" r="9" />
                <path d="M12 8v5M12 16h.01" />
              </Icon>
              <span>{loiUpload}</span>
            </div>
          )}

          {/* Ket qua import */}
          {ketQua && (
            <div className="nhap-so-lieu__ket-qua">
              <div className="nhap-so-lieu__alert nhap-so-lieu__alert--thanh-cong">
                <Icon>
                  <circle cx="12" cy="12" r="9" />
                  <path d="m9 12 2 2 4-4" />
                </Icon>
                <span>
                  Đã hoàn thành nhập file <strong>{ketQua.fileName}</strong> (Mã phiên: #{ketQua.logId})
                </span>
              </div>

              <h2 className="nhap-so-lieu__ket-qua-tieu-de">Thống kê kết quả import</h2>
              <div className="nhap-so-lieu__grid-thong-ke">
                <div className="nhap-so-lieu__the-stat">
                  <div className="nhap-so-lieu__the-stat-so nhap-so-lieu__the-stat-so--tong">
                    {ketQua.totalRecords}
                  </div>
                  <div className="nhap-so-lieu__the-stat-nhan">Tổng số dòng dữ liệu</div>
                </div>

                <div className="nhap-so-lieu__the-stat">
                  <div className="nhap-so-lieu__the-stat-so nhap-so-lieu__the-stat-so--thanh-cong">
                    {ketQua.successRecords}
                  </div>
                  <div className="nhap-so-lieu__the-stat-nhan">Dòng thành công</div>
                </div>

                <div className="nhap-so-lieu__the-stat">
                  <div className="nhap-so-lieu__the-stat-so nhap-so-lieu__the-stat-so--them-moi">
                    {ketQua.insertedRecords}
                  </div>
                  <div className="nhap-so-lieu__the-stat-nhan">Thêm mới</div>
                </div>

                <div className="nhap-so-lieu__the-stat">
                  <div className="nhap-so-lieu__the-stat-so nhap-so-lieu__the-stat-so--ghi-de">
                    {ketQua.overwrittenRecords}
                  </div>
                  <div className="nhap-so-lieu__the-stat-nhan">Ghi đè (trùng lặp)</div>
                </div>

                <div className="nhap-so-lieu__the-stat">
                  <div className="nhap-so-lieu__the-stat-so nhap-so-lieu__the-stat-so--bo-qua">
                    {ketQua.skippedRecords}
                  </div>
                  <div className="nhap-so-lieu__the-stat-nhan">Bỏ qua (không hợp lệ)</div>
                </div>
              </div>

              {ketQua.warnings && ketQua.warnings.length > 0 && (
                <div className="nhap-so-lieu__canh-bao-box">
                  <div className="nhap-so-lieu__canh-bao-tieu-de">
                    Danh sách cảnh báo / bản ghi bị bỏ qua ({ketQua.warnings.length}):
                  </div>
                  <ul className="nhap-so-lieu__canh-bao-list">
                    {ketQua.warnings.map((canhBao, idx) => (
                      <li key={idx}>{canhBao}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Noi dung Tab 2: Audit History */}
      {tabHienTai === 'history' && (
        <div className="nhap-so-lieu__bang-card">
          <div className="nhap-so-lieu__bang-header">
            <h2 className="nhap-so-lieu__bang-header-tieu-de">
              Nhật ký các lần import CSV ({tongBanGhi} lần)
            </h2>
            <button
              type="button"
              className="nhap-so-lieu__btn-lam-moi"
              onClick={() => taiLichSuImport(trang)}
              disabled={dangTaiLichSu}
            >
              <Icon>
                <path d="M20 11A8.1 8.1 0 0 0 4.5 9M4 5v4h4" />
                <path d="M4 13a8.1 8.1 0 0 0 15.5 2m.5 4v-4h-4" />
              </Icon>
              {dangTaiLichSu ? 'Đang tải...' : 'Làm mới'}
            </button>
          </div>

          {loiLichSu && (
            <div className="nhap-so-lieu__alert nhap-so-lieu__alert--loi" style={{ margin: '16px' }}>
              <Icon>
                <circle cx="12" cy="12" r="9" />
                <path d="M12 8v5M12 16h.01" />
              </Icon>
              <span>{loiLichSu}</span>
            </div>
          )}

          <div className="nhap-so-lieu__table-wrapper">
            <table className="nhap-so-lieu__table">
              <thead>
                <tr>
                  <th style={{ width: '80px' }}>Mã #</th>
                  <th>Tên file</th>
                  <th>Thời gian nhập</th>
                  <th>Người thực hiện</th>
                  <th style={{ textAlign: 'right' }}>Tổng dòng</th>
                  <th style={{ textAlign: 'right' }}>Thành công</th>
                  <th style={{ textAlign: 'center', width: '130px' }}>Tỷ lệ</th>
                </tr>
              </thead>
              <tbody>
                {dangTaiLichSu ? (
                  <tr>
                    <td colSpan="7" style={{ textAlign: 'center', padding: '32px' }}>
                      Đang tải danh sách nhật ký...
                    </td>
                  </tr>
                ) : lichSu.length === 0 ? (
                  <tr>
                    <td colSpan="7" style={{ textAlign: 'center', padding: '32px', color: '#64748b' }}>
                      Chưa có nhật ký nhập file nào trong hệ thống.
                    </td>
                  </tr>
                ) : (
                  lichSu.map((item) => {
                    const tyLe = item.totalRecords > 0
                      ? Math.round((item.successRecords / item.totalRecords) * 100)
                      : 0
                    const badgeClass =
                      tyLe === 100
                        ? 'nhap-so-lieu__badge--100'
                        : tyLe > 0
                        ? 'nhap-so-lieu__badge--partial'
                        : 'nhap-so-lieu__badge--0'

                    return (
                      <tr key={item.id}>
                        <td><strong>#{item.id}</strong></td>
                        <td>{item.fileName}</td>
                        <td>{dinhDangNgayGio(item.importedAt)}</td>
                        <td>{item.importedByName || `User #${item.importedBy}`}</td>
                        <td style={{ textAlign: 'right' }}>{item.totalRecords.toLocaleString('vi-VN')}</td>
                        <td style={{ textAlign: 'right', fontWeight: 600, color: '#16a34a' }}>
                          {item.successRecords.toLocaleString('vi-VN')}
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          <span className={`nhap-so-lieu__badge ${badgeClass}`}>
                            {tyLe}%
                          </span>
                        </td>
                      </tr>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Phan trang */}
          {tongTrang > 1 && (
            <div className="nhap-so-lieu__phan-trang">
              <span>
                Trang {trang + 1} / {tongTrang}
              </span>
              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  type="button"
                  className="nhap-so-lieu__phan-trang-btn"
                  onClick={() => setTrang((prev) => Math.max(prev - 1, 0))}
                  disabled={trang === 0 || dangTaiLichSu}
                >
                  Trang trước
                </button>
                <button
                  type="button"
                  className="nhap-so-lieu__phan-trang-btn"
                  onClick={() => setTrang((prev) => Math.min(prev + 1, tongTrang - 1))}
                  disabled={trang >= tongTrang - 1 || dangTaiLichSu}
                >
                  Trang sau
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
