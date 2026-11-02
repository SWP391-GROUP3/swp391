import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { guiKhieuNai, layDieuKienKhieuNai, layBaiCoTheKhieuNai } from '../../api/appealApi.js'
import { dinhDangThoiGian, thoiGianConLai } from './appealFormat.js'
import { formatNumber } from '../../utils/formatNumber.js'
import '../../styles/AppealPage.css'

/**
 * Man Phong vien tao khieu nai diem (UC-35): chon bai da cong bo diem con trong han, chon tieu chi bi
 * khieu nai va ghi ly do, roi gui.
 *
 * MO TRANG NAY TAI: /my-appeals/new
 *
 * MUC LUC (so dong - ten - tac dung)
 *   29  AppealCreatePage()              appeal create page
 *   42    useEffect                     tai du lieu khi mo trang
 *   50    useEffect                     tai du lieu khi mo trang
 *   71    doiLuaChon()                  doi lua chon
 *   83    doiLyDo()                     doi ly do
 *   87    guiForm()                     gui form
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  118  link [Khieu nai cua toi]        mo trang /my-appeals (MyAppealsPage)
 *  122  form, nut [Xem ket qua cham]    doi tham so tren URL -> tai lai danh sach
 *  143  form, nut [Dang gui… / Gui khieu nai]  guiForm() -> goi POST /api/appeals; chuyen sang /my-appeals (MyAppealsPage)
 *  189  link [Huy]                      mo trang /main (trang chu theo vai tro)
 */

export default function AppealCreatePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const articleId = Number(searchParams.get('articleId'))
  const [duLieu, setDuLieu] = useState(null)
  const [lyDoTheoTieuChi, setLyDoTheoTieuChi] = useState({})
  const [loi, setLoi] = useState('')
  const [dangGui, setDangGui] = useState(false)
  const [maBaiViet, setMaBaiViet] = useState('')
  const [baiCoTheKhieuNai, setBaiCoTheKhieuNai] = useState(null)
  const articleIdHopLe = Number.isInteger(articleId) && articleId > 0
  const loiHienThi = articleIdHopLe ? loi : 'Không xác định được bài viết cần khiếu nại.'

  useEffect(() => {
    let conHienThi = true
    layBaiCoTheKhieuNai()
      .then((data) => { if (conHienThi) setBaiCoTheKhieuNai(data) })
      .catch((error) => { if (conHienThi) setLoi(error.message) })
    return () => { conHienThi = false }
  }, [])

  useEffect(() => {
    if (!articleIdHopLe) return undefined

    let conHienThi = true
    layDieuKienKhieuNai(articleId)
      .then((data) => {
        if (conHienThi) {
          setDuLieu(data)
          setLyDoTheoTieuChi({})
          setLoi('')
        }
      })
      .catch((error) => {
        if (conHienThi) {
          setDuLieu(null)
          setLoi(error.message)
        }
      })
    return () => { conHienThi = false }
  }, [articleId, articleIdHopLe])

  function doiLuaChon(criterionId, dangChon) {
    setLyDoTheoTieuChi((hienTai) => {
      const moi = { ...hienTai }
      if (dangChon) {
        moi[criterionId] = ''
      } else {
        delete moi[criterionId]
      }
      return moi
    })
  }

  function doiLyDo(criterionId, reason) {
    setLyDoTheoTieuChi((hienTai) => ({ ...hienTai, [criterionId]: reason }))
  }

  async function guiForm(event) {
    event.preventDefault()
    setLoi('')
    const criteria = Object.entries(lyDoTheoTieuChi).map(([criterionId, reason]) => ({
      criterionId: Number(criterionId),
      reason: reason.trim(),
    }))

    if (criteria.length === 0 || criteria.some((item) => !item.reason)) {
      setLoi('Phải chọn ít nhất một tiêu chí và nhập lý do cho từng tiêu chí.')
      return
    }

    setDangGui(true)
    try {
      await guiKhieuNai(articleId, criteria) // [LUONG 1 - GUI KHIEU NAI] buoc 2/6 bam "Gui khieu nai" -> goi ham API
      navigate('/my-appeals', { state: { thongBao: 'Gửi khiếu nại thành công.' } })
    } catch (error) {
      setLoi(error.message)
    } finally {
      setDangGui(false)
    }
  }

  return (
    <div className="khieu-nai-pv">
      <div className="khieu-nai-pv__dau">
        <div>
          <h1 className="khieu-nai-pv__tieu-de">Gửi khiếu nại điểm</h1>
          <p className="khieu-nai-pv__phu-de">Chọn tiêu chí chưa đồng ý và giải thích cụ thể.</p>
        </div>
        <Link to="/my-appeals" className="nut-nhe">Khiếu nại của tôi</Link>
      </div>

      {!articleIdHopLe && (
        <form className="khieu-nai-pv__the" onSubmit={(event) => {
          event.preventDefault()
          setSearchParams({ articleId: maBaiViet })
        }}>
          <label htmlFor="ma-bai-khieu-nai">Bài viết còn trong thời hạn khiếu nại</label>
          <select id="ma-bai-khieu-nai" required value={maBaiViet}
            onChange={(event) => setMaBaiViet(event.target.value)} className="o-nhap">
            <option value="">Chọn bài viết</option>
            {baiCoTheKhieuNai?.map((bai) => (
              <option key={bai.articleId} value={bai.articleId}>{bai.articleTitle}</option>
            ))}
          </select>
          {baiCoTheKhieuNai?.length === 0 && <p>Hiện không có bài viết đủ điều kiện khiếu nại.</p>}
          {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
          <button className="nut-nhe" type="submit" disabled={!maBaiViet}>Xem kết quả chấm</button>
        </form>
      )}
      {articleIdHopLe && loiHienThi && <p className="thong-bao-loi" role="alert">{loiHienThi}</p>}
      {!duLieu && !loiHienThi && <p className="dang-tai">Đang tải kết quả chấm…</p>}

      {duLieu?.articleId === articleId && (
        <form onSubmit={guiForm}>
          <section className="khieu-nai-pv__the">
            <h2>{duLieu.articleTitle}</h2>
            <div className="khieu-nai-pv__tong-quan">
              <span>Điểm công bố: <strong>{formatNumber(duLieu.totalScore)}/100</strong></span>
              <span>Công bố: {dinhDangThoiGian(duLieu.announcedAt)}</span>
              <span>Hạn khiếu nại: {dinhDangThoiGian(duLieu.appealDeadline)}</span>
              <span>Còn lại: {thoiGianConLai(duLieu.appealDeadline)}</span>
            </div>
          </section>

          <section className="khieu-nai-pv__the">
            <h2>Tiêu chí khiếu nại</h2>
            <div className="khieu-nai-pv__danh-sach">
              {duLieu.criteria.map((criterion) => {
                const dangChon = Object.hasOwn(lyDoTheoTieuChi, criterion.criterionId)
                return (
                  <div key={criterion.criterionId} className="khieu-nai-pv__tieu-chi">
                    <label className="khieu-nai-pv__chon">
                      <input
                        type="checkbox"
                        checked={dangChon}
                        onChange={(event) => doiLuaChon(criterion.criterionId, event.target.checked)}
                      />
                      <span>{criterion.criterionName}</span>
                      <strong>{formatNumber(criterion.score)}/100</strong>
                    </label>
                    {criterion.comment && <p className="khieu-nai-pv__nhan-xet">Nhận xét: {criterion.comment}</p>}
                    {dangChon && (
                      <textarea
                        className="khieu-nai-pv__ly-do"
                        rows="3"
                        maxLength="1000"
                        placeholder="Nhập lý do chi tiết cho tiêu chí này"
                        value={lyDoTheoTieuChi[criterion.criterionId]}
                        onChange={(event) => doiLyDo(criterion.criterionId, event.target.value)}
                        required
                      />
                    )}
                  </div>
                )
              })}
            </div>
          </section>

          <div className="khieu-nai-pv__hanh-dong">
            <Link to="/main" className="nut-nhe">Hủy</Link>
            <button type="submit" className="nut-chinh khieu-nai-pv__nut-gui" disabled={dangGui}>
              {dangGui ? 'Đang gửi…' : 'Gửi khiếu nại'}
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
