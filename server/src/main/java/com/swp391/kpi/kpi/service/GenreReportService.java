package com.swp391.kpi.kpi.service;

import com.swp391.kpi.admin.entity.Genre;
import com.swp391.kpi.admin.repository.GenreRepository;
import com.swp391.kpi.appeal.service.AppealEffectiveScoreService;
import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.entity.ArticleStatus;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.kpi.dto.GenrePopularityItemResponse;
import com.swp391.kpi.kpi.dto.GenrePopularityReportResponse;
import com.swp391.kpi.kpi.entity.ReaderInteraction;
import com.swp391.kpi.kpi.repository.ReaderInteractionRepository;
import com.swp391.kpi.royalty.entity.RoyaltyGrading;
import com.swp391.kpi.royalty.repository.RoyaltyGradingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service xu ly bao cao muc do quan tam the loai & chi phi nhuan but (UC-64, BR-KPI-04).
 * Cung cap cac chi so: mean/median views, Bayesian rating, cost per 1,000 views, avg quality score.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   41  field                           repository / service duoc inject
 *   54  getGenrePopularityReport()      Lay bao cao phan tich the loai theo nam va thang (UC-64: View Genre Popularity Report)
 */
@Service
@RequiredArgsConstructor
public class GenreReportService {

    private final GenreRepository genreRepository;
    private final ArticleRepository articleRepository;
    private final ReaderInteractionRepository readerInteractionRepository;
    private final RoyaltyGradingRepository royaltyGradingRepository;
    private final AppealEffectiveScoreService appealEffectiveScoreService;  // BF-03: diem hieu luc cua bai

    private final GenreReportCalculator calculator = new GenreReportCalculator();

    /**
     * Lay bao cao phan tich the loai theo nam va thang (UC-64: View Genre Popularity Report).
     * Neu year va month duoc truyen vao, loc cac bai viet PUBLISHED trong thang do.
     */
    @Transactional(readOnly = true)
    public GenrePopularityReportResponse getGenrePopularityReport(Short year, Byte month) {
        // 1. Lay tat ca the loai dang su dung
        List<Genre> genres = genreRepository.findByIsActiveTrueOrderByNameAsc();

        // 2. Loc danh sach bai viet da xuat ban (PUBLISHED) theo thoi gian
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (year != null && month != null) {
            start = LocalDateTime.of(year, month, 1, 0, 0, 0);
            end = start.plusMonths(1).minusNanos(1);
        } else if (year != null) {
            start = LocalDateTime.of(year, 1, 1, 0, 0, 0);
            end = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        }

        List<Article> allArticles = articleRepository.findAll();
        List<Article> publishedArticles = new ArrayList<>();
        List<Integer> articleIds = new ArrayList<>();

        for (Article a : allArticles) {
            if (a.getStatus() == ArticleStatus.PUBLISHED && a.getPublishedAt() != null) {
                if (start != null && end != null) {
                    if (!a.getPublishedAt().isBefore(start) && !a.getPublishedAt().isAfter(end)) {
                        publishedArticles.add(a);
                        articleIds.add(a.getId());
                    }
                } else {
                    publishedArticles.add(a);
                    articleIds.add(a.getId());
                }
            }
        }

        // 3. Gom nhom tuong tac doc gia theo articleId
        Map<Integer, Long> articleViewsMap = new HashMap<>();
        Map<Integer, Long> articleRatingCountMap = new HashMap<>();
        Map<Integer, Long> articleRatingSumMap = new HashMap<>();

        if (!articleIds.isEmpty()) {
            List<ReaderInteraction> interactions = readerInteractionRepository.findByArticleIdIn(articleIds);
            for (ReaderInteraction ri : interactions) {
                int artId = ri.getArticleId();
                long views = (ri.getPageViews() != null) ? ri.getPageViews() : 0L;
                long rCount = (ri.getRatingCount() != null) ? ri.getRatingCount() : 0L;
                long rSum = (ri.getRatingSum() != null) ? ri.getRatingSum() : 0L;

                articleViewsMap.put(artId, articleViewsMap.getOrDefault(artId, 0L) + views);
                articleRatingCountMap.put(artId, articleRatingCountMap.getOrDefault(artId, 0L) + rCount);
                articleRatingSumMap.put(artId, articleRatingSumMap.getOrDefault(artId, 0L) + rSum);
            }
        }

        // 4. Lay phieu cham nhuan but de tinh tien va diem chat luong
        Map<Integer, RoyaltyGrading> articleGradingMap = new HashMap<>();
        for (Integer artId : articleIds) {
            Optional<RoyaltyGrading> gradingOpt = royaltyGradingRepository.findFirstByArticleIdOrderByVersionNoDesc(artId);
            gradingOpt.ifPresent(royaltyGrading -> articleGradingMap.put(artId, royaltyGrading));
        }

        // 5. Tinh diem danh gia toan he thong (global C cho cong thuc Bayesian)
        long systemTotalRatingCount = 0;
        long systemTotalRatingSum = 0;
        for (Map.Entry<Integer, Long> entry : articleRatingCountMap.entrySet()) {
            systemTotalRatingCount += entry.getValue();
        }
        for (Map.Entry<Integer, Long> entry : articleRatingSumMap.entrySet()) {
            systemTotalRatingSum += entry.getValue();
        }

        double globalC = GenreReportCalculator.DEFAULT_GLOBAL_RATING_C;
        if (systemTotalRatingCount > 0) {
            globalC = (double) systemTotalRatingSum / systemTotalRatingCount;
        }

        // 6. Gom nhom bai viet theo the loai va tinh chi so cho tung the loai
        Map<Integer, List<Article>> genreArticlesMap = new HashMap<>();
        for (Article a : publishedArticles) {
            Integer gid = a.getCategoryId();
            if (gid != null) {
                genreArticlesMap.computeIfAbsent(gid, k -> new ArrayList<>()).add(a);
            }
        }

        List<GenrePopularityItemResponse> itemResponses = new ArrayList<>();
        long totalSystemViews = 0;
        BigDecimal totalSystemRoyalty = BigDecimal.ZERO;
        int totalSystemArticles = publishedArticles.size();

        for (Genre g : genres) {
            List<Article> gArticles = genreArticlesMap.getOrDefault(g.getId(), Collections.emptyList());
            int count = gArticles.size();

            List<Long> viewsList = new ArrayList<>();
            List<BigDecimal> qualityScores = new ArrayList<>();
            long genreTotalViews = 0;
            long genreRatingCount = 0;
            long genreRatingSum = 0;
            BigDecimal genreTotalRoyalty = BigDecimal.ZERO;

            for (Article a : gArticles) {
                // Luot xem
                long views = articleViewsMap.getOrDefault(a.getId(), 0L);
                viewsList.add(views);
                genreTotalViews += views;

                // Danh gia
                genreRatingCount += articleRatingCountMap.getOrDefault(a.getId(), 0L);
                genreRatingSum += articleRatingSumMap.getOrDefault(a.getId(), 0L);

                // Nhuan but & diem chat luong
                RoyaltyGrading rg = articleGradingMap.get(a.getId());
                if (rg != null && rg.getAmount() != null) {
                    genreTotalRoyalty = genreTotalRoyalty.add(rg.getAmount());
                }
                // Diem chat luong = diem hieu luc cua bai (BR-QC-10), da tinh ca ket qua khieu nai
                BigDecimal effectiveScore = appealEffectiveScoreService.getEffectiveScore(a.getId());
                if (effectiveScore != null) {
                    qualityScores.add(effectiveScore);
                }
            }

            // Tinh cac chi so bang calculator
            BigDecimal meanViews = calculator.calculateMeanViews(viewsList);
            BigDecimal medianViews = calculator.calculateMedianViews(viewsList);
            BigDecimal bayesianRating = calculator.calculateBayesianRating(genreRatingCount, genreRatingSum, GenreReportCalculator.DEFAULT_BAYESIAN_M, globalC);
            BigDecimal avgQualityScore = calculator.calculateAverageQualityScore(qualityScores);
            BigDecimal costPer1000 = calculator.calculateCostPer1000Views(genreTotalRoyalty, genreTotalViews);
            boolean insufficient = calculator.isNeglectedOrInsufficient(count, GenreReportCalculator.DEFAULT_MIN_ARTICLES_K);

            GenrePopularityItemResponse item = new GenrePopularityItemResponse();
            item.setGenreId(g.getId());
            item.setGenreCode(g.getCode());
            item.setGenreName(g.getName());
            item.setNdGroup(g.getNdGroup());
            item.setBaseFactor(g.getBaseFactor());
            item.setArticleCount(count);
            item.setTotalViews(genreTotalViews);
            item.setMeanViews(meanViews);
            item.setMedianViews(medianViews);
            item.setRatingCount(genreRatingCount);
            item.setBayesianRating(bayesianRating);
            item.setAverageQualityScore(avgQualityScore);
            item.setTotalRoyaltyPaid(genreTotalRoyalty);
            item.setCostPer1000Views(costPer1000);
            item.setInsufficientData(insufficient);

            itemResponses.add(item);

            totalSystemViews += genreTotalViews;
            totalSystemRoyalty = totalSystemRoyalty.add(genreTotalRoyalty);
        }

        // 7. Tao response tong hop
        GenrePopularityReportResponse response = new GenrePopularityReportResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setTotalGenres(genres.size());
        response.setTotalArticles(totalSystemArticles);
        response.setTotalViews(totalSystemViews);
        response.setTotalRoyaltyPaid(totalSystemRoyalty);

        if (totalSystemArticles > 0) {
            BigDecimal globalMean = BigDecimal.valueOf(totalSystemViews)
                    .divide(BigDecimal.valueOf(totalSystemArticles), 2, RoundingMode.HALF_UP);
            response.setGlobalMeanViews(globalMean);
        } else {
            response.setGlobalMeanViews(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        response.setGlobalBayesianRating(BigDecimal.valueOf(globalC).setScale(2, RoundingMode.HALF_UP));
        response.setItems(itemResponses);

        return response;
    }
}
