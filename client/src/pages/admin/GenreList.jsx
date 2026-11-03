import { useEffect, useState } from 'react'
import { getGenres } from '../../api/adminApi.js'
import { formatNumber } from '../../utils/formatNumber.js'
import './AdminPages.css'

/**
 * Danh sach the loai dang dung (Admin, M-A7): ma, ten, he so chuan (BR-RY-01).
 * Chi xem; them / sua / tat the loai lam sau.
 *
 * MO TRANG NAY TAI: /genres (Admin)
 *
 * MO TRANG NAY TAI: /genres
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  GenreList()                     Danh sach the loai dang dung (Admin, M-A7): ma, ten, he so chuan (BR-RY-01)
 *   22    useEffect                     tai du lieu khi mo trang
 */
export default function GenreList() {
  const [genres, setGenres] = useState(null)   // null = dang tai
  const [error, setError] = useState('')

  useEffect(() => {
    let isMounted = true
    getGenres()
      .then((data) => { if (isMounted) setGenres(data) })
      .catch((err) => { if (isMounted) setError(err.message) })
    return () => { isMounted = false }
  }, [])

  return (
    <div>
      <div className="trang-qt__header">
        <h1 className="trang-qt__title">Thể loại</h1>
        <p className="trang-qt__subtitle">Thể loại bài đang dùng. Hệ số chuẩn dùng khi tính nhuận bút (hệ số × điểm / 100).</p>
      </div>

      <div className="trang-qt__card">
        {error && <p className="thong-bao-loi" role="alert">{error}</p>}
        {!genres && !error && <p className="trang-qt__muted">Đang tải…</p>}
        {genres && genres.length === 0 && <p className="trang-qt__muted">Chưa có thể loại nào.</p>}
        {genres && genres.length > 0 && (
          <div className="trang-qt__scroll">
            <table className="bang-fm">
              <thead>
                <tr><th>#</th><th>Mã</th><th>Tên</th><th className="trang-qt__number">Hệ số chuẩn</th></tr>
              </thead>
              <tbody>
                {genres.map((genre) => (
                  <tr key={genre.id}>
                    <td>{genre.id}</td>
                    <td>{genre.code}</td>
                    <td>{genre.name}</td>
                    <td className="trang-qt__number">{formatNumber(genre.baseFactor)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
