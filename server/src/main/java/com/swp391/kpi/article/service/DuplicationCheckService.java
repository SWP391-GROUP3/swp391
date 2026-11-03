package com.swp391.kpi.article.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.kpi.admin.dto.SystemParameterIntakeResponse;
import com.swp391.kpi.admin.service.SystemParameterService;
import com.swp391.kpi.article.dto.DuplicationCheckResponse;
import com.swp391.kpi.article.dto.DuplicationMatchResponse;
import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.entity.ArticleNature;
import com.swp391.kpi.article.entity.ArticleStatus;
import com.swp391.kpi.article.entity.ArticleVersion;
import com.swp391.kpi.article.entity.ArticleVersionKind;
import com.swp391.kpi.article.entity.DuplicationCheck;
import com.swp391.kpi.article.entity.DuplicationMatch;
import com.swp391.kpi.article.entity.DuplicationResult;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.article.repository.ArticleVersionRepository;
import com.swp391.kpi.article.repository.DuplicationCheckRepository;
import com.swp391.kpi.article.repository.DuplicationMatchRepository;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Kiem trung noi dung (UC-06) va giai trinh (UC-08), BR-IN-04, cach hieu B1-B8 (nhom truong OK 01/11).
 * Thu tu khi nop: checkBeforeSubmit() (tinh + chan) -> ArticleEditorService chup ban SUBMISSION
 * -> saveResult() (luu ket qua gan voi ban do).
 *
 * CODE LIEN QUAN (duong dan tinh tu server/src/main/java/com/swp391/kpi/):
 *   Duoc goi tu: article/controller/ArticleController.java (previewDuplicationCheck, getDuplicationCheck);
 *     article/service/ArticleEditorService.java (checkBeforeSubmit, saveResult luc nop)
 *   Goi sang: article/service/DuplicationShingleCalculator.java (tinh %), article/service/ArticleViewService.java (checkCanView),
 *     admin/service/SystemParameterService.java (nguong 20 / 50, so am tiet moi cum)
 *
 * MUC LUC (so dong - ten - tac dung)
 *   63  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   77  previewCheck()                  UC-05 buoc 2 / B5: "Kiem tra" truoc khi bam xac nhan nop
 *   93  checkBeforeSubmit()             Luc nop / nop lai: tinh lai roi chan neu can (B6)
 *  110  saveResult()                    Luu ket qua kiem trung gan voi ban SUBMISSION vua tao
 *  139  getLatestCheck()                Ket qua kiem trung luc nop gan nhat de hien o chi tiet bai
 *  ---- Ham phu (private, chi dung trong file) ----
 *  174  calculate()                     Tinh trung noi dung cua bai voi ban nop moi nhat cua moi bai khac da nop (B1-B3)
 *  214  toJson()                        B4: danh sach doan trung -> chuoi JSON de luu cot passages
 *  224  fromJson()                      Cot passages (JSON mang chuoi) -> danh sach doan trung
 */
@Service
@RequiredArgsConstructor
public class DuplicationCheckService {

    private final ArticleRepository articleRepository;
    private final ArticleVersionRepository articleVersionRepository;
    private final DuplicationCheckRepository duplicationCheckRepository;
    private final DuplicationMatchRepository duplicationMatchRepository;
    private final ArticleViewService articleViewService;
    // Du lieu luong khac -> goi service cua ho (AGENTS.md muc 2)
    private final SystemParameterService systemParameterService;
    private final ObjectMapper objectMapper;   // B4: passages luu dang JSON mang chuoi

    /**
     * UC-05 buoc 2 / B5: "Kiem tra" truoc khi bam xac nhan nop. Chi tinh va tra ket qua, khong luu
     * (duplication_checks.version_id NOT NULL, ban SUBMISSION chi tao khi nop that).
     */
    @Transactional(readOnly = true)
    public DuplicationCheckResponse previewCheck(Integer articleId, Integer currentUserId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết."));
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new BusinessException("Chỉ tác giả chính mới được kiểm tra trùng trước khi nộp.");
        }
        if (article.getStatus() != ArticleStatus.DRAFT && article.getStatus() != ArticleStatus.REVISION_REQUESTED) {
            throw new BusinessException("Chỉ kiểm tra trùng được bài ở trạng thái Nháp hoặc Trả về – cần sửa.");
        }
        return calculate(article);
    }

    /**
     * Luc nop / nop lai: tinh lai roi chan neu can (B6). BLOCKED -> 409 (UC-05 E1);
     * NEED_EXPLAIN ma chua giai trinh -> 409 (UC-05 A1). Tra ket qua de luu sau khi tao ban nop.
     */
    public DuplicationCheckResponse checkBeforeSubmit(Article article, String explanation) {
        DuplicationCheckResponse check = calculate(article);
        String percent = check.getMaxOverlapPercent().stripTrailingZeros().toPlainString();
        if (check.getResult() == DuplicationResult.BLOCKED) {
            throw new BusinessException("Nội dung trùng " + percent + "% với bài khác và tính chất là Sáng tác nên "
                    + "không nộp được. Vui lòng sửa nội dung hoặc đổi tính chất.");
        }
        if (check.getResult() == DuplicationResult.NEED_EXPLAIN && (explanation == null || explanation.isBlank())) {
            throw new BusinessException("Nội dung trùng " + percent + "% với bài khác, vui lòng nhập giải trình trước khi nộp.");
        }
        return check;
    }

    /**
     * Luu ket qua kiem trung gan voi ban SUBMISSION vua tao. Giai trinh chi luu khi NEED_EXPLAIN (B6).
     */
    @Transactional
    public void saveResult(Integer articleId, Integer versionId, DuplicationCheckResponse result,
                           String explanation, LocalDateTime checkedAt) {
        DuplicationCheck check = new DuplicationCheck();
        check.setArticleId(articleId);
        check.setVersionId(versionId);
        check.setMaxOverlapPercent(result.getMaxOverlapPercent());
        check.setResult(result.getResult());
        if (result.getResult() == DuplicationResult.NEED_EXPLAIN) {
            check.setExplanation(explanation.trim());
        }
        check.setCheckedAt(checkedAt);
        duplicationCheckRepository.save(check);

        // B4: luu moi bai co trung > 0%
        for (DuplicationMatchResponse item : result.getMatches()) {
            DuplicationMatch match = new DuplicationMatch();
            match.setCheckId(check.getId());
            match.setMatchedArticleId(item.getMatchedArticleId());
            match.setOverlapPercent(item.getOverlapPercent());
            match.setPassages(toJson(item.getPassages()));
            duplicationMatchRepository.save(match);
        }
    }

    /**
     * Ket qua kiem trung luc nop gan nhat de hien o chi tiet bai. Nguoi xem nhu chi tiet bai (B8).
     * Bai chua nop lan nao -> null.
     */
    @Transactional(readOnly = true)
    public DuplicationCheckResponse getLatestCheck(Integer articleId, Integer currentUserId) {
        articleViewService.checkCanView(articleId, currentUserId);
        Optional<DuplicationCheck> latest = duplicationCheckRepository.findFirstByArticleIdOrderByIdDesc(articleId);
        if (latest.isEmpty()) {
            return null;
        }
        DuplicationCheck check = latest.get();
        List<DuplicationMatchResponse> matches = new ArrayList<>();
        for (DuplicationMatch match : duplicationMatchRepository.findByCheckIdOrderByOverlapPercentDesc(check.getId())) {
            // B7: chi lay tieu de bai bi trung, khong tra noi dung bai
            String title = null;
            Optional<Article> matched = articleRepository.findById(match.getMatchedArticleId());
            if (matched.isPresent()) {
                title = matched.get().getTitle();
            }
            matches.add(DuplicationMatchResponse.of(match.getMatchedArticleId(), title,
                    match.getOverlapPercent(), fromJson(match.getPassages())));
        }

        DuplicationCheckResponse response = new DuplicationCheckResponse();
        response.setSaved(true);
        response.setMaxOverlapPercent(check.getMaxOverlapPercent());
        response.setResult(check.getResult());
        response.setExplanation(check.getExplanation());
        response.setCheckedAt(check.getCheckedAt());
        response.setMatches(matches);
        return response;
    }

    // ---------------------------------------------------------------- ham phu

    /**
     * Tinh trung noi dung cua bai voi ban nop moi nhat cua moi bai khac da nop (B1-B3).
     * Nguong va so am tiet 1 cum doc tu system_parameters (Q2: khong viet cung).
     */
    private DuplicationCheckResponse calculate(Article article) {
        SystemParameterIntakeResponse params = systemParameterService.getIntakeParameters();
        int size = params.getShingleSize();
        List<String> words = DuplicationShingleCalculator.splitWords(article.getContent());
        List<String> myShingles = DuplicationShingleCalculator.shingles(words, size);

        List<DuplicationMatchResponse> matches = new ArrayList<>();
        BigDecimal maxPercent = BigDecimal.ZERO.setScale(2);
        if (!myShingles.isEmpty()) {
            List<ArticleVersion> others = articleVersionRepository.findLatestOfOtherArticles(
                    article.getId(), ArticleVersionKind.SUBMISSION, ArticleStatus.DRAFT);
            for (ArticleVersion other : others) {
                Set<String> otherShingles = new HashSet<>(DuplicationShingleCalculator.shingles(
                        DuplicationShingleCalculator.splitWords(other.getContent()), size));
                BigDecimal percent = DuplicationShingleCalculator.overlapPercent(myShingles, otherShingles);
                if (percent.signum() == 0) {
                    continue;
                }
                List<String> passages = DuplicationShingleCalculator.passages(words, myShingles, otherShingles, size);
                matches.add(DuplicationMatchResponse.of(other.getArticleId(), other.getTitle(), percent, passages));
                // B2: lay muc cao nhat, khong cong gop nhieu bai
                if (percent.compareTo(maxPercent) > 0) {
                    maxPercent = percent;
                }
            }
        }
        matches.sort(Comparator.comparing(DuplicationMatchResponse::getOverlapPercent).reversed());

        boolean original = article.getDeclaredNature() == ArticleNature.ORIGINAL;
        DuplicationCheckResponse response = new DuplicationCheckResponse();
        response.setSaved(false);
        response.setMaxOverlapPercent(maxPercent);
        response.setResult(DuplicationShingleCalculator.classify(maxPercent,
                params.getDupWarnPercent(), params.getDupBlockPercent(), original));
        response.setCheckedAt(LocalDateTime.now());
        response.setMatches(matches);
        return response;
    }

    // B4: danh sach doan trung -> chuoi JSON de luu cot passages
    private String toJson(List<String> passages) {
        try {
            return objectMapper.writeValueAsString(passages);
        } catch (JsonProcessingException e) {
            // Danh sach chuoi luon ghi duoc ra JSON, khong xay ra
            throw new IllegalStateException("Không lưu được các đoạn trùng.", e);
        }
    }

    // Cot passages (JSON mang chuoi) -> danh sach doan trung
    private List<String> fromJson(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return List.of(objectMapper.readValue(json, String[].class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được các đoạn trùng đã lưu.", e);
        }
    }
}
