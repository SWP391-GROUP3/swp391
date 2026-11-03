import { useEffect, useState } from 'react'
import {
  addAttachment, deleteAttachment, getAttachmentFile, getAttachments, getMediaFactors,
} from '../../api/articleApi.js'
import './ArticleAttachments.css'

/**
 * Khoi "Ảnh / video" cua 1 bai (Iter 3): danh sach anh/video kem ket qua kiem trung (UC-07) va
 * nhuan but 0 d (B22). Dung o 2 man:
 *  - ArticleEditor (canEdit = true): them anh (file JPG/PNG) / video (link), chon tac gia anh (UC-04 buoc 4), xoa.
 *  - ArticleDetail (canEdit = false): chi xem.
 * Ket qua kiem trung chi co sau khi nop bai (server kiem luc nop - UC-05 buoc 2).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   34  TEN_LOAI                        Ten loai anh/video (ma lay tu bang media_factors); ma moi chua co ten thi hien ma
 *   36  ANH_TOI_DA_MB                   A1 (nhom chot 01/11): anh toi da 10 MB - kiem truoc o day de bao loi tieng Viet ngay
 *   37  FORM_TRONG                      hang so
 *   40  ketQuaKiemTrung()               Chu + mau cua ket qua kiem trung 1 anh/video (UC-07)
 *   54  KHONG_TINH_TIEN                 Ly do khong tinh nhuan but (B22, UC-05 A3)
 *   60  AnhXemTruoc()                   Anh nho: tai file kem token (goiApi) roi hien bang URL tam (Q7, A2)
 *   64    useEffect                     tai du lieu khi mo trang
 *   85  ArticleAttachments()            article attachments
 *   95    useEffect                     Tai danh sach anh/video (va loai anh neu dang sua)
 *  109    changeField()                 Go vao 1 o cua form them anh/video
 *  114    themAnhVideo()                Bam "Them": gui file anh hoac link video len server
 *  144    xoaAnhVideo()                 Bam "Xoa" o 1 dong: hoi lai roi xoa
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  190  [Xoa]                           xoaAnhVideo() Bam "Xoa" o 1 dong: hoi lai roi xoa -> hoi xac nhan; goi DELETE /api/articles/{articleId}/attachments/{attachmentId}
 *  238  [Dang them… / Them]             themAnhVideo() Bam "Them": gui file anh hoac link video len server -> goi POST /api/articles/{articleId}/attachments
 */

// Ten loai anh/video (ma lay tu bang media_factors); ma moi chua co ten thi hien ma
const TEN_LOAI = { PHOTO: 'Ảnh chụp', GRAPHIC: 'Đồ họa', VIDEO: 'Video' }
// A1 (nhom chot 01/11): anh toi da 10 MB - kiem truoc o day de bao loi tieng Viet ngay
const ANH_TOI_DA_MB = 10
const FORM_TRONG = { loai: 'IMAGE', videoUrl: '', mediaKind: '', authorId: '', externalSource: '', caption: '' }

/** Chu + mau cua ket qua kiem trung 1 anh/video (UC-07). */
function ketQuaKiemTrung(item) {
  if (!item.hashResult) {
    return { chu: 'Chưa kiểm – kiểm khi nộp bài', mau: 'xam' }
  }
  if (item.hashResult === 'NEW') {
    return { chu: 'Không trùng', mau: 'tot' }
  }
  if (item.hashResult === 'DUPLICATE') {
    return { chu: `Trùng y hệt với bài #${item.matchedArticleId}`, mau: 'loi' }
  }
  return { chu: `Gần giống ảnh của bài #${item.matchedArticleId} (lệch ${item.distanceBits} bit)`, mau: 'canh-bao' }
}

// Ly do khong tinh nhuan but (B22, UC-05 A3)
const KHONG_TINH_TIEN = {
  REUSED: 'Tái sử dụng – 0 đ',
  EXTERNAL: 'Nguồn ngoài – 0 đ',
}

/** Anh nho: tai file kem token (goiApi) roi hien bang URL tam (Q7, A2). */
function AnhXemTruoc({ articleId, attachmentId }) {
  const [url, setUrl] = useState('')
  const [loi, setLoi] = useState(false)

  useEffect(() => {
    let urlTam = ''
    let isMounted = true
    getAttachmentFile(articleId, attachmentId)
      .then((blob) => {
        urlTam = URL.createObjectURL(blob)
        if (isMounted) setUrl(urlTam)
      })
      .catch(() => { if (isMounted) setLoi(true) })
    // Roi man / doi anh thi tra lai bo nho cua URL tam
    return () => {
      isMounted = false
      if (urlTam) URL.revokeObjectURL(urlTam)
    }
  }, [articleId, attachmentId])

  if (loi) return <div className="anh-bai__thumb anh-bai__thumb--trong">Không tải được ảnh</div>
  if (!url) return <div className="anh-bai__thumb anh-bai__thumb--trong">Đang tải…</div>
  return <img className="anh-bai__thumb" src={url} alt="" />
}

export default function ArticleAttachments({ articleId, canEdit, authors = [] }) {
  const [items, setItems] = useState([])
  const [kinds, setKinds] = useState([])             // loai anh/video tu bang media_factors
  const [form, setForm] = useState(FORM_TRONG)
  const [file, setFile] = useState(null)
  const [fileKey, setFileKey] = useState(0)          // doi key de xoa o chon file sau khi them
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  // Tai danh sach anh/video (va loai anh neu dang sua)
  useEffect(() => {
    let isMounted = true
    const loadKinds = canEdit ? getMediaFactors() : Promise.resolve([])
    Promise.all([getAttachments(articleId), loadKinds])
      .then(([list, kindList]) => {
        if (!isMounted) return
        setItems(list)
        setKinds(kindList)
      })
      .catch((err) => { if (isMounted) setError(err.message) })
    return () => { isMounted = false }
  }, [articleId, canEdit])

  /** Go vao 1 o cua form them anh/video. */
  function changeField(field, value) {
    setForm({ ...form, [field]: value })
  }

  /** Bam "Thêm": gui file anh hoac link video len server. */
  function themAnhVideo() {
    setError('')
    if (form.loai === 'IMAGE' && file && file.size > ANH_TOI_DA_MB * 1024 * 1024) {
      setError(`Ảnh tối đa ${ANH_TOI_DA_MB} MB.`)
      return
    }
    const duLieu = new FormData()
    if (form.loai === 'IMAGE') {
      if (file) duLieu.append('file', file)
    } else {
      duLieu.append('videoUrl', form.videoUrl)
    }
    duLieu.append('mediaKind', form.mediaKind)
    duLieu.append('authorId', form.authorId)
    duLieu.append('externalSource', form.externalSource)
    duLieu.append('caption', form.caption)

    setBusy(true)
    addAttachment(articleId, duLieu)
      .then((item) => {
        setItems([...items, item])
        setForm({ ...FORM_TRONG, loai: form.loai, authorId: form.authorId })
        setFile(null)
        setFileKey(fileKey + 1)
      })
      .catch((err) => setError(err.message))
      .finally(() => setBusy(false))
  }

  /** Bam "Xóa" o 1 dong: hoi lai roi xoa. */
  function xoaAnhVideo(item) {
    if (!window.confirm('Xóa ảnh/video này khỏi bài?')) return
    setError('')
    deleteAttachment(articleId, item.id)
      .then(() => setItems(items.filter((x) => x.id !== item.id)))
      .catch((err) => setError(err.message))
  }

  // Video chi chon loai VIDEO, anh chon cac loai con lai
  const kindsTheoLoai = kinds.filter((k) => (form.loai === 'VIDEO') === (k.mediaKind === 'VIDEO'))

  return (
    <section className="anh-bai">
      <h2 className="anh-bai__title">Ảnh / video</h2>
      {error && <p className="thong-bao-loi" role="alert">{error}</p>}

      {items.length === 0 && <p className="anh-bai__muted">Bài chưa có ảnh/video.</p>}
      <ul className="anh-bai__list">
        {items.map((item) => {
          const ketQua = ketQuaKiemTrung(item)
          return (
            <li key={item.id} className="anh-bai__item">
              {item.mediaType === 'IMAGE'
                ? <AnhXemTruoc articleId={articleId} attachmentId={item.id} />
                : <div className="anh-bai__thumb anh-bai__thumb--trong">Video</div>}

              <div className="anh-bai__info">
                <div className="anh-bai__name">
                  {item.caption || (item.mediaType === 'IMAGE' ? 'Ảnh không chú thích' : 'Video')}
                </div>
                <div className="anh-bai__muted">
                  {TEN_LOAI[item.mediaKind] || item.mediaKind} · Tác giả: {item.authorName || `#${item.authorId}`}
                  {item.externalSource && ` · Nguồn: ${item.externalSource}`}
                </div>
                {item.videoUrl && (
                  <a className="anh-bai__link" href={item.videoUrl} target="_blank" rel="noreferrer">{item.videoUrl}</a>
                )}
                <div className="anh-bai__badges">
                  <span className={`anh-bai__badge anh-bai__badge--${ketQua.mau}`}>{ketQua.chu}</span>
                  {item.noRoyaltyReason && (
                    <span className="anh-bai__badge anh-bai__badge--loi">{KHONG_TINH_TIEN[item.noRoyaltyReason]}</span>
                  )}
                </div>
              </div>

              {canEdit && (
                <button type="button" className="nut-nhe" onClick={() => xoaAnhVideo(item)}>Xóa</button>
              )}
            </li>
          )
        })}
      </ul>

      {canEdit && (
        <div className="anh-bai__form">
          <div className="anh-bai__row">
            <label className="anh-bai__radio">
              <input type="radio" name="loai-anh-video" checked={form.loai === 'IMAGE'}
                onChange={() => setForm({ ...form, loai: 'IMAGE', mediaKind: '' })} /> Ảnh (JPG/PNG, tối đa {ANH_TOI_DA_MB} MB)
            </label>
            <label className="anh-bai__radio">
              <input type="radio" name="loai-anh-video" checked={form.loai === 'VIDEO'}
                onChange={() => setForm({ ...form, loai: 'VIDEO', mediaKind: '' })} /> Video (nhập link)
            </label>
          </div>

          {form.loai === 'IMAGE' ? (
            <input key={fileKey} type="file" accept=".jpg,.jpeg,.png" className="o-nhap" aria-label="Chọn file ảnh"
              onChange={(e) => setFile(e.target.files[0] || null)} />
          ) : (
            <input className="o-nhap" placeholder="https://..." aria-label="Link video" value={form.videoUrl}
              onChange={(e) => changeField('videoUrl', e.target.value)} />
          )}

          <div className="anh-bai__row">
            <select className="o-nhap" aria-label="Loại ảnh/video" value={form.mediaKind}
              onChange={(e) => changeField('mediaKind', e.target.value)}>
              <option value="">-- Loại --</option>
              {kindsTheoLoai.map((k) => <option key={k.id} value={k.mediaKind}>{TEN_LOAI[k.mediaKind] || k.mediaKind}</option>)}
            </select>
            {/* UC-04 buoc 4 (BR-IN-05): tac gia anh/video chon trong danh sach tac gia cua bai */}
            <select className="o-nhap" aria-label="Tác giả ảnh/video" value={form.authorId}
              onChange={(e) => changeField('authorId', e.target.value)}>
              <option value="">-- Tác giả --</option>
              {authors.map((a) => <option key={a.userId} value={a.userId}>{a.fullName}</option>)}
            </select>
          </div>

          <input className="o-nhap" placeholder="Nguồn ngoài (bỏ trống nếu tự làm) – có nguồn ngoài thì 0 đ" maxLength={200}
            aria-label="Nguồn ngoài" value={form.externalSource} onChange={(e) => changeField('externalSource', e.target.value)} />
          <input className="o-nhap" placeholder="Chú thích" maxLength={300} aria-label="Chú thích"
            value={form.caption} onChange={(e) => changeField('caption', e.target.value)} />

          <div>
            <button type="button" className="nut-nhe" disabled={busy} onClick={themAnhVideo}>
              {busy ? 'Đang thêm…' : 'Thêm'}
            </button>
          </div>
          <p className="anh-bai__muted">
            Tác giả ảnh/video phải có trong danh sách tác giả đã lưu. Kiểm trùng ảnh/video chạy khi nộp bài;
            trùng không chặn nộp nhưng ảnh tái sử dụng hoặc nguồn ngoài được 0 đ.
          </p>
        </div>
      )}
    </section>
  )
}
