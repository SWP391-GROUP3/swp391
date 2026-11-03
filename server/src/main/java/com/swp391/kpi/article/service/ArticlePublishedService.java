package com.swp391.kpi.article.service;

import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.article.dto.ArticlePublishedResponse;
import com.swp391.kpi.article.dto.ArticleRoyaltyAttachmentResponse;
import com.swp391.kpi.article.dto.ArticleRoyaltyAuthorResponse;
import com.swp391.kpi.article.dto.ArticleRoyaltyInfoResponse;
import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.entity.ArticleAttachment;
import com.swp391.kpi.article.entity.ArticleAuthor;
import com.swp391.kpi.article.entity.ArticleAuthorConfirmStatus;
import com.swp391.kpi.article.entity.ArticleStatus;
import com.swp391.kpi.article.entity.ArticleVersion;
import com.swp391.kpi.article.entity.ArticleVersionKind;
import com.swp391.kpi.article.repository.ArticleAttachmentRepository;
import com.swp391.kpi.article.repository.ArticleAuthorRepository;
import com.swp391.kpi.article.repository.ArticlePublishedRow;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.article.repository.ArticleVersionRepository;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Doc bai DA DANG cho luong khac (hien la BF-04 nhuan but). Chi doc, khong ghi DB.
 * Tach rieng khoi ArticleService (nghiep vu cua Phong vien) cho de tim va de doc.
 * Luong khac goi cac ham public o day, KHONG goi repository cua BF-01 (AGENTS.md muc 2).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   52  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   63  getPublishedArticles()          UC-46 (BF-04): moi bai PUBLISHED, bai dang som nhat len dau (gan han chi nhat)
 *   78  getArticleForRoyalty()          UC-47, UC-48 (BF-04): thong tin 1 bai de cham nhuan but
 *  ---- Ham phu (private, chi dung trong file) ----
 *   95  loadUserNames()                 Ho ten moi nguoi dung, lay qua service cua luong admin (khong tu query bang users)
 *  106  buildAuthorList()               Tac gia da dong y (ACCEPTED)
 *  129  buildAttachmentList()           Anh/video cua ban nop (SUBMISSION) moi nhat - Minh chot 17/10
 */
@Service
@RequiredArgsConstructor
public class ArticlePublishedService {

    private final ArticleRepository articleRepository;
    private final ArticleAuthorRepository articleAuthorRepository;
    private final ArticleVersionRepository articleVersionRepository;
    private final ArticleAttachmentRepository articleAttachmentRepository;
    private final UserReadService userReadService;   // service cua luong admin, lay ho ten

    /**
     * UC-46 (BF-04): moi bai PUBLISHED, bai dang som nhat len dau (gan han chi nhat).
     * BF-04 tu bo bai da co phieu cham va tu loc theo ban/the loai.
     */
    @Transactional(readOnly = true)
    public List<ArticlePublishedResponse> getPublishedArticles() {
        Map<Integer, String> userNames = loadUserNames();

        List<ArticlePublishedResponse> result = new ArrayList<>();
        for (ArticlePublishedRow row : articleRepository.findPublishedRows()) {
            result.add(ArticlePublishedResponse.from(row, userNames.get(row.getAuthorId())));
        }
        return result;
    }

    /**
     * UC-47, UC-48 (BF-04): thong tin 1 bai de cham nhuan but.
     * Khong co bai -> 404; bai chua PUBLISHED -> 409.
     */
    @Transactional(readOnly = true)
    public ArticleRoyaltyInfoResponse getArticleForRoyalty(Integer articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết có mã " + articleId + "."));

        // Chi bai da dang moi duoc cham nhuan but (BF-04 bat dau sau khi bai PUBLISHED)
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new BusinessException("Bài chưa được đăng, chưa thể chấm nhuận bút.");
        }

        List<ArticleRoyaltyAuthorResponse> authors = buildAuthorList(article, loadUserNames());
        List<ArticleRoyaltyAttachmentResponse> attachments = buildAttachmentList(article.getId());
        return ArticleRoyaltyInfoResponse.from(article, authors, attachments);
    }

    // ---------------------------------------------------------------- ham phu

    // Ho ten moi nguoi dung, lay qua service cua luong admin (khong tu query bang users)
    private Map<Integer, String> loadUserNames() {
        Map<Integer, String> names = new HashMap<>();
        for (UserAccessResponse user : userReadService.getUsers()) {
            names.put(user.id(), user.fullName());
        }
        return names;
    }

    // Tac gia da dong y (ACCEPTED). Tac gia chinh DUNG DAU vi RoyaltyCalculator cua BF-04
    // coi phan tu dau la tac gia chinh de don phan du lam tron (GBR-10).
    // Cac dong tac gia con lai giu thu tu ty le giam dan (repository da sap xep).
    private List<ArticleRoyaltyAuthorResponse> buildAuthorList(Article article, Map<Integer, String> userNames) {
        Integer mainAuthorId = article.getAuthorId();
        List<ArticleAuthor> rows = articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId());

        List<ArticleRoyaltyAuthorResponse> result = new ArrayList<>();
        // Vong 1: tac gia chinh
        for (ArticleAuthor row : rows) {
            if (row.getUserId().equals(mainAuthorId) && row.getConfirmStatus() == ArticleAuthorConfirmStatus.ACCEPTED) {
                result.add(ArticleRoyaltyAuthorResponse.from(row, userNames.get(row.getUserId()), mainAuthorId));
            }
        }
        // Vong 2: dong tac gia
        for (ArticleAuthor row : rows) {
            if (!row.getUserId().equals(mainAuthorId) && row.getConfirmStatus() == ArticleAuthorConfirmStatus.ACCEPTED) {
                result.add(ArticleRoyaltyAuthorResponse.from(row, userNames.get(row.getUserId()), mainAuthorId));
            }
        }
        return result;
    }

    // Anh/video cua ban nop (SUBMISSION) moi nhat - Minh chot 17/10.
    // Khong lay theo version_no lon nhat vi ban cuoi thuong la ban EDITED cua BTV, ban do
    // co the khong gan anh. Bo anh version_id NULL (dang soan, chua nop).
    private List<ArticleRoyaltyAttachmentResponse> buildAttachmentList(Integer articleId) {
        List<ArticleRoyaltyAttachmentResponse> result = new ArrayList<>();

        Optional<ArticleVersion> latestSubmission = articleVersionRepository
                .findFirstByArticleIdAndKindOrderByVersionNoDesc(articleId, ArticleVersionKind.SUBMISSION);
        if (latestSubmission.isEmpty()) {
            return result;   // bai chua co ban nop nao -> khong co anh
        }

        for (ArticleAttachment attachment : articleAttachmentRepository.findByVersionIdOrderByIdAsc(latestSubmission.get().getId())) {
            result.add(ArticleRoyaltyAttachmentResponse.from(attachment));
        }
        return result;
    }
}
