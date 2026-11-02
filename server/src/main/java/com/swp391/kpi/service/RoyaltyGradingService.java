package com.swp391.kpi.service;

import com.swp391.kpi.dto.AppealEffectiveAttemptResponse;
import com.swp391.kpi.dto.ArticlePublishedResponse;
import com.swp391.kpi.dto.ArticleRoyaltyAttachmentResponse;
import com.swp391.kpi.dto.ArticleRoyaltyAuthorResponse;
import com.swp391.kpi.dto.ArticleRoyaltyInfoResponse;
import com.swp391.kpi.dto.RoyaltyEstimateItemResponse;
import com.swp391.kpi.dto.RoyaltyEstimateResponse;
import com.swp391.kpi.dto.RoyaltyGradingAuthorResponse;
import com.swp391.kpi.dto.RoyaltyGradingCreateRequest;
import com.swp391.kpi.dto.RoyaltyGradingMediaResponse;
import com.swp391.kpi.dto.RoyaltyGradingResponse;
import com.swp391.kpi.dto.RoyaltyPendingArticleResponse;
import com.swp391.kpi.entity.Event;
import com.swp391.kpi.entity.MediaFactor;
import com.swp391.kpi.entity.RoyaltyGrading;
import com.swp391.kpi.entity.RoyaltyLine;
import com.swp391.kpi.entity.RoyaltyLineType;
import com.swp391.kpi.entity.RoyaltyMediaItem;
import com.swp391.kpi.entity.RoyaltyNature;
import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.entity.RoyaltyPeriodStatus;
import com.swp391.kpi.entity.RoyaltyRate;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.EventRepository;
import com.swp391.kpi.repository.MediaFactorRepository;
import com.swp391.kpi.repository.RoyaltyGradingRepository;
import com.swp391.kpi.repository.RoyaltyLineRepository;
import com.swp391.kpi.repository.RoyaltyMediaItemRepository;
import com.swp391.kpi.repository.RoyaltyPeriodRepository;
import com.swp391.kpi.repository.RoyaltyRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cham nhuan but (Thu ky toa soan):
 *   UC-46 danh sach bai cho cham, UC-47 cham phan bai viet, UC-48 cham anh/video.
 * Cong thuc nam trong RoyaltyCalculator (Iter 1); class nay lay du lieu,
 * goi may tinh va luu ket qua.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   90  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   99  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *  117  getPendingArticles()            UC-46: bai da dang ma chua co phieu cham, kem han chi va canh bao sap toi han
 *  143  previewGrading()                UC-47 buoc 1-3: tinh truoc he so va tien de Thu ky xem, CHUA luu gi
 *  166  completeGrading()               UC-47 buoc 4 + UC-48: Thu ky bam Hoan tat -> luu phieu, anh/video va cac dong tien cua tung nguoi vao ky dang…
 *  206  hasMediaBeenPaid()              BF-01 goi (BR-IN-05): anh da tung duoc tra nhuan but chua (co dong royalty_media_items)
 *  215  estimateRoyalty()               BF-01 goi (UC-09): uoc tinh tien phan bai viet truoc khi nop o 3 muc diem 100 / 80 / 50, khong luu gi
 *  ---- Ham phu (private, chi dung trong file) ----
 *  245  checkCanRegrade()               Nhom chot 27/10/2026: chi cham lai khi ky cua phien ban cu con OPEN (vd Ke toan vua tra ky, UC-52); phien ban…
 *  260  calculate()                     Tinh du cac buoc cong thuc cho 1 bai (dung chung cho xem truoc va hoan tat)
 *  302  findSaturationRate()            Ty le bao hoa dung de tinh tien (GBR-09): bai khong thuoc su kien -> 100; co su kien thi RoyaltyCalculator xe…
 *  314  splitArticleAmount()            Chia tien bai cho cac tac gia (GBR-10): dong tac gia lam tron xuong 1.000d, tac gia chinh nhan phan con lai
 *  333  gradeMedia()                    UC-48: cham tung anh/video
 *  353  findNoRoyaltyReason()           Ly do anh/video KHONG duoc tinh tien; null = duoc tinh (BR-RY-04, BR-IN-05): - lay tu nguon ngoai - trung (DU…
 *  366  findMediaFactor()               He so co dinh cua loai anh/video (bang media_factors, UC-56)
 *  372  sumMediaAmount()                sum media amount
 *  381  findRateOnPublishDate()         UC-47 E1: don gia hieu luc tai ngay dang bai (GBR-08); khong co thi khong cham duoc
 *  389  findRateOnDate()                UC-09: don gia hieu luc tai 1 ngay (uoc tinh dung hom nay)
 *  396  findExemptScore()               UC-09: muc diem duoc mien giam bao hoa cua su kien; bai khong thuoc su kien -> null
 *  409  parseNature()                   UC-09: doi tinh chat bai (chuoi BF-01 truyen vao) sang enum, sai thi bao loi
 *  420  findOpenPeriod()                tim open period
 *  424  fillPeriod()                    fill period
 *  434  saveGrading()                   Phieu cham 1 phien ban (khong sua phien ban cu)
 *  461  saveArticleLines()              Moi tac gia 1 dong ARTICLE; tong cac dong = dung tien phan bai viet (GBR-10)
 *  472  saveMediaItemsAndLines()        Moi anh/video duoc tinh tien: 1 dong royalty_media_items + 1 dong MEDIA cho tac gia anh
 *  492  newLine()                       new line
 *  508  readInt()                       Lay 1 tham so so nguyen
 */
@Service
@RequiredArgsConstructor
public class RoyaltyGradingService {

    private static final String ROYALTY_FLOW = "BF-04";  // gia tri cot system_parameters.flow cua luong nhuan but
    private static final int FIRST_VERSION = 1;          // lan cham dau; cham lai thi tang dan
    private static final String HASH_DUPLICATE = "DUPLICATE";  // trung y het file anh da co
    private static final String HASH_SIMILAR = "SIMILAR";      // dhash gan giong anh da co
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // UC-09: bai chua co diem nen uoc tinh o 3 muc diem gia dinh (nhom chot 01/11/2026), diem cao len dau
    private static final List<BigDecimal> ESTIMATE_SCORES =
            List.of(new BigDecimal("100"), new BigDecimal("80"), new BigDecimal("50"));

    private final RoyaltyGradingRepository royaltyGradingRepository;
    private final RoyaltyMediaItemRepository royaltyMediaItemRepository;
    private final RoyaltyLineRepository royaltyLineRepository;
    private final RoyaltyPeriodRepository royaltyPeriodRepository;
    private final RoyaltyRateRepository royaltyRateRepository;
    private final MediaFactorRepository mediaFactorRepository;
    private final EventRepository eventRepository;
    private final ArticlePublishedService articlePublishedService;  // service cua BF-01, khong query bang articles
    private final AppealEffectiveScoreService appealEffectiveScoreService;  // BF-03: diem hieu luc (S0 hoac sau khieu nai)
    private final GenreService genreService;                        // luong admin: he so chuan the loai
    private final SystemParameterService systemParameterService;
    private final AuditService auditService;

    // ------------------------------------------------------------------ UC-46

    // UC-46: bai da dang ma chua co phieu cham, kem han chi va canh bao sap toi han.
    // Thu tu giu nguyen nhu BF-01 tra ve: bai dang som nhat (gan han nhat) len dau.
    @Transactional(readOnly = true)
    public List<RoyaltyPendingArticleResponse> getPendingArticles() {
        Map<String, String> params = systemParameterService.getValuesByFlow(ROYALTY_FLOW);
        int deadlineDays = readInt(params, "PAYMENT_DEADLINE_DAYS");  // han chi tinh tu ngay dang
        int warnDays = readInt(params, "PAYMENT_WARN_DAYS");          // con <= so ngay nay thi canh bao

        // Doi sang Set de kiem "bai da cham chua" nhanh, khong phai duyet ca danh sach moi lan
        Set<Integer> gradedArticleIds = new HashSet<>(royaltyGradingRepository.findGradedArticleIds());
        LocalDate today = LocalDate.now();

        List<RoyaltyPendingArticleResponse> result = new ArrayList<>();
        for (ArticlePublishedResponse article : articlePublishedService.getPublishedArticles()) {
            if (gradedArticleIds.contains(article.getId())) {
                continue;  // da co phieu cham -> khong con cho cham
            }
            LocalDate deadline = article.getPublishedAt().toLocalDate().plusDays(deadlineDays);
            long daysLeft = ChronoUnit.DAYS.between(today, deadline);  // am = da qua han
            boolean nearDeadline = daysLeft <= warnDays;
            result.add(RoyaltyPendingArticleResponse.from(article, deadline, daysLeft, nearDeadline));
        }
        return result;
    }

    // ------------------------------------------------------------- UC-47, 48

    // UC-47 buoc 1-3: tinh truoc he so va tien de Thu ky xem, CHUA luu gi.
    @Transactional(readOnly = true)
    public RoyaltyGradingResponse previewGrading(Integer articleId) {
        ArticleRoyaltyInfoResponse article = articlePublishedService.getArticleForRoyalty(articleId);
        RoyaltyRate rate = findRateOnPublishDate(article);
        RoyaltyGradingResponse preview = calculate(article, rate);

        // Cho Thu ky biet bai se vao ky nao: cham lai thi vao ky cua phien ban cu (checkCanRegrade).
        Optional<RoyaltyGrading> previous = royaltyGradingRepository.findFirstByArticleIdOrderByVersionNoDesc(articleId);
        if (previous.isPresent()) {
            fillPeriod(preview, previous.get().getPeriod());
            return preview;
        }
        // Lan dau: ky OPEN. Chua co ky OPEN thi de trong, luc Hoan tat moi bao loi.
        Optional<RoyaltyPeriod> openPeriod = findOpenPeriod();
        if (openPeriod.isPresent()) {
            fillPeriod(preview, openPeriod.get());
        }
        return preview;
    }

    // UC-47 buoc 4 + UC-48: Thu ky bam Hoan tat -> luu phieu, anh/video va cac dong tien
    // cua tung nguoi vao ky dang OPEN (BR-RY-05). Tat ca trong 1 transaction: loi la khong luu gi.
    // Bai da cham thi day la cham lai: phien ban moi, bat buoc ly do (xem checkCanRegrade).
    @Transactional
    public RoyaltyGradingResponse completeGrading(RoyaltyGradingCreateRequest request, Integer currentUserId) {
        ArticleRoyaltyInfoResponse article = articlePublishedService.getArticleForRoyalty(request.getArticleId());
        Optional<RoyaltyGrading> previous = royaltyGradingRepository.findFirstByArticleIdOrderByVersionNoDesc(article.getId());

        RoyaltyPeriod period;
        int versionNo;
        String recalcReason = null;
        if (previous.isPresent()) {
            period = checkCanRegrade(previous.get(), request.getRecalcReason());
            versionNo = previous.get().getVersionNo() + 1;
            recalcReason = request.getRecalcReason().trim();
            royaltyLineRepository.deleteByGradingId(previous.get().getId());   // ky con OPEN nen xoa duoc
        } else {
            // UC-47 E1: khong co ky OPEN thi khong hoan tat duoc
            period = findOpenPeriod().orElseThrow(() -> new BusinessException(
                    "Chưa có kỳ nhuận bút đang mở, chưa thể hoàn tất chấm nhuận bút."));
            versionNo = FIRST_VERSION;
        }
        RoyaltyRate rate = findRateOnPublishDate(article);
        RoyaltyGradingResponse result = calculate(article, rate);

        RoyaltyGrading grading = saveGrading(result, period, rate, versionNo, recalcReason, currentUserId);
        saveArticleLines(grading, period, result.getAuthors());
        saveMediaItemsAndLines(grading, period, result.getMediaItems());
        String action = previous.isPresent() ? "RECALCULATE" : "CREATE";
        auditService.log(currentUserId, action, "royalty_gradings", grading.getId(), null,
                "{\"version_no\":" + versionNo + ",\"final_factor\":" + grading.getFinalFactor()
                        + ",\"amount\":" + grading.getAmount() + "}");

        result.setGradingId(grading.getId());
        fillPeriod(result, period);
        return result;
    }

    // ------------------------------------------------------------ BF-01 goi

    // BR-IN-05, UC-05 A3: anh nay DA TUNG duoc tra nhuan but chua (co dong trong royalty_media_items).
    // BF-01 goi khi kiem trung anh: anh moi trung voi anh da tra tien = tai su dung, hien 0 d.
    // Trung voi anh chua tung tra tien thi van duoc tinh (comment bang article_attachments).
    @Transactional(readOnly = true)
    public boolean hasMediaBeenPaid(Integer attachmentId) {
        return royaltyMediaItemRepository.existsByAttachmentId(attachmentId);
    }

    // UC-09: uoc tinh nhuan but phan bai viet TRUOC khi nop, khong luu gi. BF-01 truyen gia tri
    // cua ban nhap vao (khong doc bang articles). Cung cong thuc luc cham that (BR-RY-01..03),
    // tinh o tung muc diem ESTIMATE_SCORES; don gia lay theo hom nay vi bai chua dang.
    // saturationRate = ty le BF-01 se khoa luc nop (null = khong thuoc su kien). Chua tinh anh/video.
    @Transactional(readOnly = true)
    public RoyaltyEstimateResponse estimateRoyalty(Integer categoryId, String nature, Integer eventId,
                                                   BigDecimal saturationRate) {
        RoyaltyNature royaltyNature = parseNature(nature);
        BigDecimal baseFactor = genreService.getBaseFactor(categoryId);
        BigDecimal unitPrice = findRateOnDate(LocalDate.now()).getAmount();
        BigDecimal exemptScore = findExemptScore(eventId, saturationRate);
        RoyaltyCalculator calculator = new RoyaltyCalculator(systemParameterService.getValuesByFlow(ROYALTY_FLOW));

        List<RoyaltyEstimateItemResponse> items = new ArrayList<>();
        for (BigDecimal score : ESTIMATE_SCORES) {
            BigDecimal suggestedFactor = calculator.suggestedFactor(baseFactor, score);                    // BR-RY-01
            BigDecimal rate = calculator.effectiveSaturationRate(score, saturationRate, exemptScore);      // GBR-09
            BigDecimal finalFactor = calculator.finalFactor(suggestedFactor, rate, royaltyNature);         // BR-RY-02
            BigDecimal amount = calculator.calculateAmount(finalFactor, unitPrice);                        // BR-RY-03
            items.add(RoyaltyEstimateItemResponse.of(score, suggestedFactor, rate, finalFactor, amount));
        }

        RoyaltyEstimateResponse response = new RoyaltyEstimateResponse();
        response.setCategoryId(categoryId);
        response.setBaseFactor(baseFactor);
        response.setNature(royaltyNature);
        response.setNatureRate(calculator.natureRate(royaltyNature));
        response.setUnitPrice(unitPrice);
        response.setItems(items);
        return response;
    }

    // Nhom chot 27/10/2026: chi cham lai khi ky cua phien ban cu con OPEN (vd Ke toan vua tra ky,
    // UC-52); phien ban moi vao CUNG ky do. Ky da chot/khoa thi khong cham lai duoc (BR-RY-06),
    // chenh lech sau khoa se thanh dong dieu chinh khi lam BF-03 (UC-47 A1).
    private RoyaltyPeriod checkCanRegrade(RoyaltyGrading previous, String recalcReason) {
        RoyaltyPeriod period = previous.getPeriod();
        if (period.getStatus() != RoyaltyPeriodStatus.OPEN) {
            throw new BusinessException("Bài này đã được chấm nhuận bút trong kỳ " + period.getPeriodMonth() + "/"
                    + period.getPeriodYear() + " (đang " + period.getStatus() + "), không chấm lại được.");
        }
        if (recalcReason == null || recalcReason.isBlank()) {
            throw new BusinessException("Bài này đã được chấm nhuận bút. Muốn chấm lại phải nhập lý do.");
        }
        return period;
    }

    // ------------------------------------------------------------ tinh toan

    // Tinh du cac buoc cong thuc cho 1 bai (dung chung cho xem truoc va hoan tat)
    private RoyaltyGradingResponse calculate(ArticleRoyaltyInfoResponse article, RoyaltyRate rate) {
        // Diem hieu luc (BR-QC-10): diem S0 da duyet, hoac diem Truong ban chot sau khieu nai (BF-03)
        AppealEffectiveAttemptResponse effective = appealEffectiveScoreService.getEffectiveAttempt(article.getId());
        if (effective == null) {
            throw new BusinessException("Bài chưa có điểm chất lượng được công bố, chưa chấm nhuận bút được.");
        }
        BigDecimal score = effective.score();
        BigDecimal baseFactor = genreService.getBaseFactor(article.getCategoryId());
        RoyaltyCalculator calculator = new RoyaltyCalculator(systemParameterService.getValuesByFlow(ROYALTY_FLOW));
        BigDecimal unitPrice = rate.getAmount();
        // Tinh chat bai = tinh chat PV khai, da khoa luc nop bai (BR-IN-03). Thu ky khong doi duoc.
        RoyaltyNature nature = RoyaltyNature.valueOf(article.getDeclaredNature());

        BigDecimal suggestedFactor = calculator.suggestedFactor(baseFactor, score);                // BR-RY-01
        BigDecimal saturationRate = findSaturationRate(article, score, calculator);               // GBR-09
        BigDecimal finalFactor = calculator.finalFactor(suggestedFactor, saturationRate, nature); // BR-RY-02
        BigDecimal articleAmount = calculator.calculateAmount(finalFactor, unitPrice);            // BR-RY-03

        RoyaltyGradingResponse response = new RoyaltyGradingResponse();
        response.setArticleId(article.getId());
        response.setTitle(article.getTitle());
        response.setPublishedAt(article.getPublishedAt());
        response.setScoringAttemptId(effective.attemptId());
        response.setScoreUsed(score);
        response.setCategoryId(article.getCategoryId());
        response.setBaseFactor(baseFactor);
        response.setSuggestedFactor(suggestedFactor);
        response.setSaturationRate(saturationRate);
        response.setNature(nature);
        response.setNatureRate(calculator.natureRate(nature));
        response.setFinalFactor(finalFactor);
        response.setUnitPrice(unitPrice);
        response.setArticleAmount(articleAmount);
        response.setAuthors(splitArticleAmount(article.getAuthors(), articleAmount, calculator));
        response.setMediaItems(gradeMedia(article.getAttachments(), unitPrice, calculator));
        response.setMediaAmount(sumMediaAmount(response.getMediaItems()));
        response.setTotalAmount(articleAmount.add(response.getMediaAmount()));
        return response;
    }

    // Ty le bao hoa dung de tinh tien (GBR-09): bai khong thuoc su kien -> 100;
    // co su kien thi RoyaltyCalculator xet mien giam theo exempt_score cua su kien.
    private BigDecimal findSaturationRate(ArticleRoyaltyInfoResponse article, BigDecimal score,
                                          RoyaltyCalculator calculator) {
        if (article.getEventId() == null) {
            return calculator.effectiveSaturationRate(score, null, null);
        }
        Event event = eventRepository.findById(article.getEventId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện có ID: " + article.getEventId()));
        return calculator.effectiveSaturationRate(score, article.getSaturationRate(), event.getExemptScore());
    }

    // Chia tien bai cho cac tac gia (GBR-10): dong tac gia lam tron xuong 1.000d,
    // tac gia chinh nhan phan con lai. RoyaltyCalculator.splitAmount can tac gia chinh o vi tri 0.
    private List<RoyaltyGradingAuthorResponse> splitArticleAmount(List<ArticleRoyaltyAuthorResponse> authors,
                                                                  BigDecimal amount, RoyaltyCalculator calculator) {
        if (authors.isEmpty() || !authors.get(0).isMainAuthor()) {
            throw new BusinessException("Dữ liệu tác giả của bài không hợp lệ, chưa thể chia nhuận bút.");
        }
        List<BigDecimal> shares = new ArrayList<>();
        for (ArticleRoyaltyAuthorResponse author : authors) {
            shares.add(author.getSharePercent());
        }
        List<BigDecimal> parts = calculator.splitAmount(amount, shares);  // cung thu tu voi authors

        List<RoyaltyGradingAuthorResponse> result = new ArrayList<>();
        for (int i = 0; i < authors.size(); i++) {
            result.add(RoyaltyGradingAuthorResponse.from(authors.get(i), parts.get(i)));
        }
        return result;
    }

    // UC-48: cham tung anh/video. Anh duoc tinh tien = he so co dinh cua loai x don gia (BR-RY-04).
    private List<RoyaltyGradingMediaResponse> gradeMedia(List<ArticleRoyaltyAttachmentResponse> attachments,
                                                         BigDecimal unitPrice, RoyaltyCalculator calculator) {
        List<RoyaltyGradingMediaResponse> result = new ArrayList<>();
        for (ArticleRoyaltyAttachmentResponse attachment : attachments) {
            String noRoyaltyReason = findNoRoyaltyReason(attachment);
            if (noRoyaltyReason != null) {
                result.add(RoyaltyGradingMediaResponse.from(attachment, null, BigDecimal.ZERO, noRoyaltyReason));
                continue;
            }
            BigDecimal factor = findMediaFactor(attachment.getMediaKind());
            BigDecimal amount = calculator.calculateAmount(factor, unitPrice);
            result.add(RoyaltyGradingMediaResponse.from(attachment, factor, amount, null));
        }
        return result;
    }

    // Ly do anh/video KHONG duoc tinh tien; null = duoc tinh (BR-RY-04, BR-IN-05):
    //  - lay tu nguon ngoai
    //  - trung (DUPLICATE / SIMILAR) voi anh DA TUNG duoc tra nhuan but = tai su dung.
    //    Trung voi anh chua tung tra tien thi van tinh (comment bang article_attachments).
    private String findNoRoyaltyReason(ArticleRoyaltyAttachmentResponse attachment) {
        if (attachment.getExternalSource() != null && !attachment.getExternalSource().isBlank()) {
            return "Lấy từ nguồn ngoài: " + attachment.getExternalSource();
        }
        String hashResult = attachment.getHashResult();
        boolean duplicated = HASH_DUPLICATE.equals(hashResult) || HASH_SIMILAR.equals(hashResult);
        if (duplicated && royaltyMediaItemRepository.existsByAttachmentId(attachment.getMatchedAttachmentId())) {
            return "Trùng ảnh/video đã được trả nhuận bút (tái sử dụng)";
        }
        return null;
    }

    // He so co dinh cua loai anh/video (bang media_factors, UC-56)
    private BigDecimal findMediaFactor(String mediaKind) {
        MediaFactor mediaFactor = mediaFactorRepository.findByMediaKind(mediaKind)
                .orElseThrow(() -> new BusinessException("Chưa có hệ số cho loại ảnh/video " + mediaKind + "."));
        return mediaFactor.getFactor();
    }

    private static BigDecimal sumMediaAmount(List<RoyaltyGradingMediaResponse> mediaItems) {
        BigDecimal total = BigDecimal.ZERO;
        for (RoyaltyGradingMediaResponse media : mediaItems) {
            total = total.add(media.getAmount());
        }
        return total;
    }

    // UC-47 E1: don gia hieu luc tai ngay dang bai (GBR-08); khong co thi khong cham duoc
    private RoyaltyRate findRateOnPublishDate(ArticleRoyaltyInfoResponse article) {
        LocalDate publishDate = article.getPublishedAt().toLocalDate();
        return royaltyRateRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(publishDate)
                .orElseThrow(() -> new BusinessException("Chưa có đơn giá nhuận bút hiệu lực tại ngày đăng "
                        + publishDate.format(DATE_FORMAT) + "."));
    }

    // UC-09: don gia hieu luc tai 1 ngay (uoc tinh dung hom nay); chua co thi khong uoc tinh duoc
    private RoyaltyRate findRateOnDate(LocalDate date) {
        return royaltyRateRepository.findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(date)
                .orElseThrow(() -> new BusinessException("Chưa có đơn giá nhuận bút hiệu lực tại ngày "
                        + date.format(DATE_FORMAT) + ", chưa ước tính được."));
    }

    // UC-09: muc diem duoc mien giam bao hoa cua su kien (GBR-09); bai khong thuoc su kien -> null
    private BigDecimal findExemptScore(Integer eventId, BigDecimal saturationRate) {
        if (saturationRate == null) {
            return null;
        }
        if (eventId == null) {
            throw new BusinessException("Bài có tỷ lệ bão hòa thì phải chọn sự kiện.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sự kiện có ID: " + eventId));
        return event.getExemptScore();
    }

    // UC-09: doi tinh chat bai (chuoi BF-01 truyen vao) sang enum; sai thi bao loi tieng Viet
    private static RoyaltyNature parseNature(String nature) {
        if (nature == null) {
            throw new BusinessException("Chưa chọn tính chất bài.");
        }
        try {
            return RoyaltyNature.valueOf(nature);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Tính chất bài không hợp lệ: " + nature);
        }
    }

    private Optional<RoyaltyPeriod> findOpenPeriod() {
        return royaltyPeriodRepository.findFirstByStatusOrderByPeriodYearAscPeriodMonthAsc(RoyaltyPeriodStatus.OPEN);
    }

    private static void fillPeriod(RoyaltyGradingResponse response, RoyaltyPeriod period) {
        response.setPeriodId(period.getId());
        response.setPeriodYear(period.getPeriodYear());
        response.setPeriodMonth(period.getPeriodMonth());
    }

    // ------------------------------------------------------------------ luu

    // Phieu cham 1 phien ban (khong sua phien ban cu). tier_rate luu bang diem, applied = suggested
    // (BR-RY-01 moi). recalcReason NULL voi phien ban 1, bat buoc tu phien ban 2 (ck_royalty_gradings_recalc).
    private RoyaltyGrading saveGrading(RoyaltyGradingResponse result, RoyaltyPeriod period, RoyaltyRate rate,
                                       int versionNo, String recalcReason, Integer currentUserId) {
        RoyaltyGrading grading = new RoyaltyGrading();
        grading.setScoringAttemptId(result.getScoringAttemptId());
        grading.setArticleId(result.getArticleId());
        grading.setCategoryId(result.getCategoryId());
        grading.setGradedBy(currentUserId);
        grading.setVersionNo(versionNo);
        grading.setRecalcReason(recalcReason);
        grading.setPeriod(period);
        grading.setRate(rate);
        grading.setScoreUsed(result.getScoreUsed());
        grading.setTierRate(result.getScoreUsed());
        grading.setBaseFactor(result.getBaseFactor());
        grading.setSuggestedFactor(result.getSuggestedFactor());
        grading.setAppliedFactor(result.getSuggestedFactor());
        grading.setSaturationRate(result.getSaturationRate());
        grading.setNature(result.getNature());
        grading.setNatureRate(result.getNatureRate());
        grading.setFinalFactor(result.getFinalFactor());
        grading.setUnitPrice(result.getUnitPrice());
        grading.setAmount(result.getArticleAmount());
        grading.setGradedAt(LocalDateTime.now());  // cot co DEFAULT nhung Hibernate van ghi NULL neu de trong
        return royaltyGradingRepository.save(grading);
    }

    // Moi tac gia 1 dong ARTICLE; tong cac dong = dung tien phan bai viet (GBR-10)
    private void saveArticleLines(RoyaltyGrading grading, RoyaltyPeriod period,
                                  List<RoyaltyGradingAuthorResponse> authors) {
        for (RoyaltyGradingAuthorResponse author : authors) {
            RoyaltyLine line = newLine(grading, period, author.getUserId(), RoyaltyLineType.ARTICLE, author.getAmount());
            line.setSharePercent(author.getSharePercent());
            royaltyLineRepository.save(line);
        }
    }

    // Moi anh/video duoc tinh tien: 1 dong royalty_media_items + 1 dong MEDIA cho tac gia anh.
    // Anh khong tinh tien thi khong tao dong nao (BR-RY-04).
    private void saveMediaItemsAndLines(RoyaltyGrading grading, RoyaltyPeriod period,
                                        List<RoyaltyGradingMediaResponse> mediaItems) {
        for (RoyaltyGradingMediaResponse media : mediaItems) {
            if (!media.getPayable()) {
                continue;
            }
            RoyaltyMediaItem item = new RoyaltyMediaItem();
            item.setGrading(grading);
            item.setAttachmentId(media.getAttachmentId());
            item.setFactor(media.getFactor());
            item.setUnitPrice(grading.getUnitPrice());
            item.setAmount(media.getAmount());
            RoyaltyMediaItem savedItem = royaltyMediaItemRepository.save(item);

            RoyaltyLine line = newLine(grading, period, media.getAuthorId(), RoyaltyLineType.MEDIA, media.getAmount());
            line.setMediaItem(savedItem);
            royaltyLineRepository.save(line);
        }
    }

    private static RoyaltyLine newLine(RoyaltyGrading grading, RoyaltyPeriod period, Integer userId,
                                       RoyaltyLineType lineType, BigDecimal amount) {
        RoyaltyLine line = new RoyaltyLine();
        line.setPeriod(period);
        line.setGrading(grading);
        line.setUserId(userId);
        line.setLineType(lineType);
        line.setAmount(amount);
        line.setCreatedAt(LocalDateTime.now());
        return line;
    }

    // ---------------------------------------------------------------- tien ich

    // Lay 1 tham so so nguyen. Thieu hoac sai dinh dang thi bao loi ro rang,
    // khong tu dung so mac dinh (AGENTS.md muc 5: khong viet cung so trong code).
    private static int readInt(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null) {
            throw new BusinessException("Thiếu tham số hệ thống " + key + ", vui lòng báo Quản trị viên bổ sung.");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Tham số hệ thống " + key + " không phải là số nguyên: " + value);
        }
    }
}
