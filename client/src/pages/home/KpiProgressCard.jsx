import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { layKpiThangNay } from '../../api/reporterHomeApi.js'
import { formatNumber } from '../../utils/formatNumber.js'
import '../../styles/KpiProgressCard.css'

/** Phan tram do dai thanh tien do, toi da 100 (vuot chi tieu van chi day thanh). */
function tinhPhanTram(thucTe, chiTieu) {
  if (chiTieu <= 0) {
    return 100
  }
  return Math.min(100, (thucTe / chiTieu) * 100)
}

/**
 * 1 dong tien do: nhan, "4 / 6 bai", thanh ngang, dong goi y con thieu bao nhieu.
 * thucTe = null nghia la chua co so lieu (vd chua co bai nao duoc cham diem).
 */
function DongTienDo({ nhan, thucTe, chiTieu, donVi }) {
  const coSoLieu = thucTe !== null && thucTe !== undefined
  const daDat = coSoLieu && thucTe >= chiTieu
  const phanTram = coSoLieu ? tinhPhanTram(thucTe, chiTieu) : 0

  let goiY = 'Chưa có số liệu'
  if (daDat) {
    goiY = 'Đã đạt'
  } else if (coSoLieu) {
    goiY = `Còn thiếu ${formatNumber(chiTieu - thucTe)} ${donVi}`
  }

  return (
    <div className="kpi-pv__dong">
      <div className="kpi-pv__dong-dau">
        <span>{nhan}</span>
        <span className="kpi-pv__dong-so">
          {formatNumber(thucTe)} / {formatNumber(chiTieu)} {donVi}
        </span>
      </div>
      {/* role="progressbar" + aria-*: trinh doc man hinh doc duoc phan tram */}
      <div
        className="kpi-pv__thanh"
        role="progressbar"
        aria-label={nhan}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={Math.round(phanTram)}
        aria-valuetext={`${formatNumber(thucTe)} trên ${formatNumber(chiTieu)} ${donVi}`}
      >
        {/* Keo gian bang transform (scaleX) thay vi doi width: trinh duyet
            khong phai tinh lai bo cuc trang moi khung hinh -> muot hon */}
        <div
          className={daDat ? 'kpi-pv__thanh-day kpi-pv__thanh-day--dat' : 'kpi-pv__thanh-day'}
          style={{ transform: `scaleX(${phanTram / 100})` }}
        />
      </div>
      <p className={daDat ? 'kpi-pv__goi-y kpi-pv__goi-y--dat' : 'kpi-pv__goi-y'}>{goiY}</p>
    </div>
  )
}

/**
 * Khoi "KPI thang nay" (UC-60): so bai da dang va diem TB so voi chi tieu
 * Truong ban giao. So bai cong theo ty le dong tac gia nen co the le (BR-KPI-02).
 * Dat khi dat CA HAI chi tieu (BR-KPI-05). Trong thang chi la ket qua DU KIEN,
 * ket qua chinh thuc chi co khi Truong ban chot ky (UC-66, UC-67).
 *
 * MUC LUC (so dong - ten - tac dung)
 *    8  tinhPhanTram()                  Phan tram do dai thanh tien do, toi da 100 (vuot chi tieu van chi day thanh)
 *   19  DongTienDo()                    1 dong tien do: nhan, "4 / 6 bai", thanh ngang, dong goi y con thieu bao nhieu
 *   76  KpiProgressCard()               xem tom tat dau file
 *   80    useEffect                     tai du lieu khi mo trang
 *
 * NUT / LINK TREN MAN HINH (so dong - bam vao - ket qua)
 *  119  link [Xem chi tiet KPI →]       mo trang /my-kpi (PersonalKpiProgress)
 */
export default function KpiProgressCard() {
  const [kpi, setKpi] = useState(undefined)   // undefined = dang tai, null = chua giao chi tieu
  const [loi, setLoi] = useState('')

  useEffect(() => {
    let conHienThi = true

    layKpiThangNay()
      .then((duLieu) => { if (conHienThi) setKpi(duLieu) })
      .catch(() => { if (conHienThi) setLoi('Không tải được KPI tháng này.') })

    return () => { conHienThi = false }
  }, [])

  const dangTai = kpi === undefined && !loi
  const duKienDat = kpi
    && kpi.actualPublished >= kpi.minPublished
    && kpi.actualAvgScore !== null
    && kpi.actualAvgScore >= kpi.minAvgScore

  return (
    <section className="trang-chu__khoi" aria-labelledby="tieu-de-kpi">
      <div className="trang-chu__khoi-dau">
        <h2 id="tieu-de-kpi" className="trang-chu__khoi-tieu-de">
          KPI tháng {kpi ? `${kpi.month}/${kpi.year}` : 'này'}
        </h2>
        {kpi && (
          <span className={duKienDat ? 'kpi-pv__nhan kpi-pv__nhan--dat' : 'kpi-pv__nhan'}>
            {duKienDat ? 'Dự kiến đạt' : 'Dự kiến chưa đạt'}
          </span>
        )}
      </div>

      {loi && <p className="thong-bao-loi" role="alert">{loi}</p>}
      {dangTai && <p className="dang-tai">Đang tải…</p>}
      {kpi === null && <p className="trong-rong">Trưởng ban chưa giao chỉ tiêu tháng này.</p>}

      {kpi && (
        <>
          <DongTienDo nhan="Bài đã đăng" thucTe={kpi.actualPublished}
            chiTieu={kpi.minPublished} donVi="bài" />
          <DongTienDo nhan="Điểm chất lượng trung bình" thucTe={kpi.actualAvgScore}
            chiTieu={kpi.minAvgScore} donVi="điểm" />
          <Link to="/my-kpi" className="kpi-pv__xem">Xem chi tiết KPI →</Link>
        </>
      )}
    </section>
  )
}
