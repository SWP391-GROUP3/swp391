import './ArticleDuplicationResult.css'

/**
 * Ket qua kiem trung noi dung (UC-06) + so doan trung (UC-08). Dung o 2 man:
 *  - ArticleEditor: ket qua tinh thu truoc khi xac nhan nop (check.saved = false).
 *  - ArticleDetail: ket qua da luu luc nop gan nhat, kem giai trinh.
 * B7: chi hien tieu de bai bi trung, % va cac doan trung (chu cua bai nay), khong hien bai cua nguoi khac.
 *
 * CODE LIEN QUAN (Ctrl+P trong VS Code, dan duong dan de mo file):
 *   server/src/main/java/com/swp391/kpi/article/service/DuplicationCheckService.java       tinh / luu ket qua (previewCheck, saveResult, getLatestCheck)
 *   server/src/main/java/com/swp391/kpi/article/service/DuplicationShingleCalculator.java  tach cum am tiet, tinh %, gom doan trung
 *   Khong goi API: nhan ket qua qua prop tu ArticleEditor.jsx / ArticleDetail.jsx
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  KET_QUA                         Chu + mau cua 3 ket qua (gia tri cot duplication_checks.result)
 *   26  ArticleDuplicationResult()      article duplication result
 */

// Chu + mau cua 3 ket qua (gia tri cot duplication_checks.result)
const KET_QUA = {
  NORMAL: { chu: 'Bình thường', mau: 'tot' },
  NEED_EXPLAIN: { chu: 'Cần giải trình', mau: 'canh-bao' },
  BLOCKED: { chu: 'Bị chặn – bài Sáng tác trùng quá ngưỡng', mau: 'loi' },
}

export default function ArticleDuplicationResult({ check }) {
  const ketQua = KET_QUA[check.result] || { chu: check.result, mau: 'canh-bao' }
  return (
    <div className="trung-noi-dung">
      <p className="trung-noi-dung__tong">
        Trùng cao nhất: <strong>{Number(check.maxOverlapPercent)}%</strong>
        <span className={`trung-noi-dung__badge trung-noi-dung__badge--${ketQua.mau}`}>{ketQua.chu}</span>
      </p>
      {check.result === 'BLOCKED' && (
        <p className="trung-noi-dung__muted">Vui lòng sửa nội dung hoặc đổi tính chất bài rồi kiểm tra lại.</p>
      )}

      {check.matches.length === 0 && <p className="trung-noi-dung__muted">Không trùng với bài nào đã nộp.</p>}
      <ul className="trung-noi-dung__list">
        {check.matches.map((match) => (
          <li key={match.matchedArticleId} className="trung-noi-dung__item">
            <div className="trung-noi-dung__bai">
              <span>Bài #{match.matchedArticleId}{match.matchedTitle ? ` – ${match.matchedTitle}` : ''}</span>
              <strong>{Number(match.overlapPercent)}%</strong>
            </div>
            {match.passages.map((passage, index) => (
              <blockquote key={index} className="trung-noi-dung__doan">{passage}</blockquote>
            ))}
          </li>
        ))}
      </ul>

      {check.explanation && (
        <p className="trung-noi-dung__giai-trinh"><strong>Giải trình:</strong> {check.explanation}</p>
      )}
    </div>
  )
}
