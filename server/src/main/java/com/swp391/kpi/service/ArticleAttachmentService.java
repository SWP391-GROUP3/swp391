package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticleAttachmentFileResponse;
import com.swp391.kpi.dto.ArticleAttachmentResponse;
import com.swp391.kpi.dto.ArticleAttachmentUploadRequest;
import com.swp391.kpi.dto.MediaFactorResponse;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleAttachment;
import com.swp391.kpi.entity.ArticleAuthor;
import com.swp391.kpi.entity.ArticleHashResult;
import com.swp391.kpi.entity.ArticleMediaType;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ArticleAttachmentRepository;
import com.swp391.kpi.repository.ArticleAuthorRepository;
import com.swp391.kpi.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Anh/video cua bai BF-01: them, xoa, xem (UC-03, UC-04 buoc 4, Q7) va kiem trung anh/video luc nop (UC-07, BR-IN-05).
 * Quyet dinh 03/11 "chuyen dong cu": moi dong article_attachments cua bai la anh hien tai; moi lan nop / nop lai
 * doi version_id cua tat ca sang ban SUBMISSION moi (BF-04 doc anh theo ban SUBMISSION moi nhat).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   72  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   78  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   94  getAttachments()                Anh/video cua bai kem ket qua kiem trung; nguoi xem nhu chi tiet bai (B8)
 *  109  addAttachment()                 UC-03 / UC-04 buoc 4: tac gia chinh them 1 anh (file JPG/PNG) hoac 1 video (link)
 *  158  deleteAttachment()              Tac gia chinh xoa 1 anh/video khi bai con sua duoc
 *  180  getImageFile()                  Q7: noi dung file anh de trinh duyet hien (nguoi xem nhu B8)
 *  206  findAuthorError()               BR-IN-05 / B23: tac gia moi anh/video phai la 1 trong cac tac gia cua bai
 *  226  checkAndAttachToSubmission()    UC-07 luc nop / nop lai (UC-05 buoc 2, UC-10 buoc 3): kiem trung tung anh/video, roi gan tat ca anh/video cua…
 *  ---- Ham phu (private, chi dung trong file) ----
 *  237  checkDuplicate()                B21: so voi anh/video cung dinh dang cua cac bai KHAC da nop (khac DRAFT)
 *  292  saveImage()                     Kiem file anh (A1: JPG/PNG, toi da 10 MB), luu vao server/uploads/nam/thang/, tinh SHA-256 + dHash
 *  332  toResponse()                    Doi entity sang DTO kem ho ten tac gia, bai chua anh bi trung va ly do khong tinh tien (B22)
 *  350  checkCanEditMedia()             BR-IN-01: chi tac gia chinh them / xoa anh, khi bai Nhap hoac bi tra ve sua
 *  360  checkMediaKind()                Loai anh/video phai co trong bang media_factors (luong admin, doc qua MediaFactorService)
 *  370  authorIdsOf()                   Tac gia chinh + cac dong article_authors cua bai
 *  380  findArticle()                   Tim bai theo ma, khong co thi bao 404 (NotFoundException)
 *  386  findAttachment()                Tim anh/video thuoc dung bai, khong co thi bao 404
 *  395  filePathOf()                    url luu dang "/uploads/2026/11/abc.jpg" -> file uploads/2026/11/abc.jpg (tinh tu thu muc chay server)
 *  400  extensionOf()                   "Anh Bia.JPG" -> "jpg"; khong co dau cham -> chuoi rong
 *  408  blankToNull()                   Chuoi rong / toan khoang trang -> null (cot NULL trong DB)
 */
@Service
@RequiredArgsConstructor
public class ArticleAttachmentService {

    // A1 (nhom chot 01/11): file luu o server/uploads/ (server chay tu thu muc server/), toi da 10 MB, chi JPG/PNG
    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final List<String> IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png");
    private static final String CONTENT_TYPE_PNG = "image/png";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";

    private final ArticleAttachmentRepository articleAttachmentRepository;
    private final ArticleRepository articleRepository;
    private final ArticleAuthorRepository articleAuthorRepository;
    // Ai duoc xem anh = ai duoc xem bai (B8)
    private final ArticleViewService articleViewService;
    // Anh bi trung da tung duoc tra nhuan but chua (BF-04, B22)
    private final RoyaltyGradingService royaltyGradingService;
    // Loai anh/video (bang media_factors) va DHASH_MAX_BITS la du lieu luong admin
    private final MediaFactorService mediaFactorService;
    private final SystemParameterService systemParameterService;
    private final UserReadService userReadService;

    /**
     * Anh/video cua bai kem ket qua kiem trung; nguoi xem nhu chi tiet bai (B8).
     */
    @Transactional(readOnly = true)
    public List<ArticleAttachmentResponse> getAttachments(Integer articleId, Integer currentUserId) {
        articleViewService.checkCanView(articleId, currentUserId);
        Map<Integer, String> userNames = userReadService.getUserNames();
        List<ArticleAttachmentResponse> result = new ArrayList<>();
        for (ArticleAttachment attachment : articleAttachmentRepository.findByArticleIdOrderByIdAsc(articleId)) {
            result.add(toResponse(attachment, userNames));
        }
        return result;
    }

    /**
     * UC-03 / UC-04 buoc 4: tac gia chinh them 1 anh (file JPG/PNG) hoac 1 video (link).
     * Chua kiem trung o day: kiem luc nop (UC-05 buoc 2), nen hashResult = null.
     */
    @Transactional
    public ArticleAttachmentResponse addAttachment(Integer articleId, Integer currentUserId,
                                                   ArticleAttachmentUploadRequest request) {
        Article article = findArticle(articleId);
        checkCanEditMedia(article, currentUserId);
        checkMediaKind(request.getMediaKind());

        // BR-IN-05: tac gia anh/video phai la 1 trong cac tac gia cua bai
        if (!authorIdsOf(article).contains(request.getAuthorId())) {
            throw new BusinessException("Tác giả của ảnh/video phải là một trong các tác giả của bài.");
        }

        boolean hasFile = request.getFile() != null && !request.getFile().isEmpty();
        boolean hasVideo = request.getVideoUrl() != null && !request.getVideoUrl().isBlank();
        if (hasFile == hasVideo) {
            throw new BusinessException("Vui lòng chọn một file ảnh hoặc nhập một link video.");
        }

        ArticleAttachment attachment = new ArticleAttachment();
        attachment.setArticleId(article.getId());
        attachment.setVersionId(null);   // dang soan, gan vao ban nop luc nop
        attachment.setAuthorId(request.getAuthorId());
        attachment.setMediaKind(request.getMediaKind());
        attachment.setExternalSource(blankToNull(request.getExternalSource()));
        attachment.setCaption(blankToNull(request.getCaption()));
        attachment.setCreatedAt(LocalDateTime.now());

        if (hasFile) {
            saveImage(attachment, request.getFile());
        } else {
            // A3: video chi nhap link; file_hash = SHA-256 cua link da chuan hoa
            String link = request.getVideoUrl().trim();
            String lowerLink = link.toLowerCase();
            if (!lowerLink.startsWith("http://") && !lowerLink.startsWith("https://")) {
                throw new BusinessException("Link video phải bắt đầu bằng http:// hoặc https://.");
            }
            attachment.setMediaType(ArticleMediaType.VIDEO);
            attachment.setUrl(link);
            attachment.setFileHash(ArticleMediaHasher.videoLinkHash(link));
        }

        articleAttachmentRepository.save(attachment);
        return toResponse(attachment, userReadService.getUserNames());
    }

    /**
     * Tac gia chinh xoa 1 anh/video khi bai con sua duoc. Anh dang duoc anh bai khac tro toi
     * (matched_attachment_id) thi chan, vi xoa se lam mat ket qua kiem trung cua bai kia (quyet dinh 03/11).
     */
    @Transactional
    public void deleteAttachment(Integer articleId, Integer attachmentId, Integer currentUserId) {
        Article article = findArticle(articleId);
        checkCanEditMedia(article, currentUserId);
        ArticleAttachment attachment = findAttachment(articleId, attachmentId);
        if (articleAttachmentRepository.existsByMatchedAttachmentId(attachment.getId())) {
            throw new BusinessException("Ảnh/video này đang được dùng để đối chiếu trùng lặp với bài khác nên không xóa được.");
        }
        articleAttachmentRepository.delete(attachment);

        if (attachment.getMediaType() == ArticleMediaType.IMAGE) {
            try {
                Files.deleteIfExists(filePathOf(attachment));
            } catch (IOException e) {
                // Khong xoa duoc file thi chi con 1 file thua tren o dia, dong DB da xoa -> bo qua
            }
        }
    }

    /**
     * Q7: noi dung file anh de trinh duyet hien (nguoi xem nhu B8). Video khong co file, mo bang link.
     */
    @Transactional(readOnly = true)
    public ArticleAttachmentFileResponse getImageFile(Integer articleId, Integer attachmentId, Integer currentUserId) {
        articleViewService.checkCanView(articleId, currentUserId);
        ArticleAttachment attachment = findAttachment(articleId, attachmentId);
        if (attachment.getMediaType() != ArticleMediaType.IMAGE) {
            throw new BusinessException("Video không có file trên máy chủ, vui lòng mở bằng link.");
        }

        Path path = filePathOf(attachment);
        if (!Files.exists(path)) {
            throw new NotFoundException("Không tìm thấy file ảnh trên máy chủ.");
        }
        ArticleAttachmentFileResponse file = new ArticleAttachmentFileResponse();
        try {
            file.setContent(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được file ảnh, vui lòng thử lại.", e);
        }
        file.setContentType(attachment.getUrl().toLowerCase().endsWith(".png") ? CONTENT_TYPE_PNG : CONTENT_TYPE_JPEG);
        return file;
    }

    /**
     * BR-IN-05 / B23: tac gia moi anh/video phai la 1 trong cac tac gia cua bai.
     * Goi luc nop (ArticleEditorService): hop le -> null; sai -> cau loi de gop vao danh sach loi UC-05 E2.
     */
    @Transactional(readOnly = true)
    public String findAuthorError(Integer articleId, Integer mainAuthorId, List<ArticleAuthor> authorRows) {
        Set<Integer> authorIds = new HashSet<>();
        authorIds.add(mainAuthorId);
        for (ArticleAuthor row : authorRows) {
            authorIds.add(row.getUserId());
        }
        for (ArticleAttachment attachment : articleAttachmentRepository.findByArticleIdOrderByIdAsc(articleId)) {
            if (!authorIds.contains(attachment.getAuthorId())) {
                return "Có ảnh/video mà tác giả không còn trong danh sách tác giả của bài, vui lòng chọn lại tác giả cho ảnh/video đó.";
            }
        }
        return null;
    }

    /**
     * UC-07 luc nop / nop lai (UC-05 buoc 2, UC-10 buoc 3): kiem trung tung anh/video,
     * roi gan tat ca anh/video cua bai vao ban SUBMISSION moi (quyet dinh 03/11 "chuyen dong cu").
     * Trung khong chan nop (UC-05 A3): chi luu ket qua, chi tiet bai hien "0 d".
     */
    @Transactional
    public void checkAndAttachToSubmission(Integer articleId, Integer submissionVersionId) {
        int dhashMaxBits = systemParameterService.getIntakeParameters().getDhashMaxBits();
        for (ArticleAttachment attachment : articleAttachmentRepository.findByArticleIdOrderByIdAsc(articleId)) {
            checkDuplicate(attachment, dhashMaxBits);
            attachment.setVersionId(submissionVersionId);
            articleAttachmentRepository.save(attachment);
        }
    }

    // B21: so voi anh/video cung dinh dang cua cac bai KHAC da nop (khac DRAFT).
    // SHA-256 trung -> DUPLICATE; anh co dHash lech <= DHASH_MAX_BITS -> SIMILAR; khong trung -> NEW
    private void checkDuplicate(ArticleAttachment attachment, int dhashMaxBits) {
        ArticleAttachment bestMatch = null;
        ArticleHashResult bestResult = ArticleHashResult.NEW;
        int bestDistance = 0;
        boolean bestPaid = false;

        List<ArticleAttachment> candidates = articleAttachmentRepository.findCompareCandidates(
                attachment.getArticleId(), attachment.getMediaType(), ArticleStatus.DRAFT);
        for (ArticleAttachment other : candidates) {
            // Anh kia da bi danh dau la ban sao cua chinh anh nay -> anh nay la anh goc, khong tinh trung nguoc lai
            // (vd nop lai bai goc sau khi bai khac da chep anh cua no)
            if (attachment.getId().equals(other.getMatchedAttachmentId())) {
                continue;
            }
            ArticleHashResult result;
            int distance;
            if (other.getFileHash().equals(attachment.getFileHash())) {
                result = ArticleHashResult.DUPLICATE;
                distance = 0;
            } else if (attachment.getDhash() != null && other.getDhash() != null) {
                distance = ArticleMediaHasher.hammingDistance(attachment.getDhash(), other.getDhash());
                if (distance > dhashMaxBits) {
                    continue;
                }
                result = ArticleHashResult.SIMILAR;
            } else {
                continue;
            }

            // Trung nhieu anh: uu tien anh da tung duoc tra nhuan but (de BF-04 khong tra lan 2 - B22),
            // sau do den anh giong nhat (lech it bit nhat)
            boolean paid = royaltyGradingService.hasMediaBeenPaid(other.getId());
            boolean better = bestMatch == null
                    || (paid && !bestPaid)
                    || (paid == bestPaid && distance < bestDistance);
            if (better) {
                bestMatch = other;
                bestResult = result;
                bestDistance = distance;
                bestPaid = paid;
            }
        }

        attachment.setHashResult(bestResult);
        if (bestMatch == null) {
            attachment.setDistanceBits(null);
            attachment.setMatchedAttachmentId(null);
        } else {
            // Video chi so link nen khong co khoang cach bit
            attachment.setDistanceBits(attachment.getMediaType() == ArticleMediaType.IMAGE ? (byte) bestDistance : null);
            attachment.setMatchedAttachmentId(bestMatch.getId());
        }
    }

    // Kiem file anh (A1: JPG/PNG, toi da 10 MB), luu vao server/uploads/nam/thang/, tinh SHA-256 + dHash
    private void saveImage(ArticleAttachment attachment, MultipartFile file) {
        String extension = extensionOf(file.getOriginalFilename());
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Ảnh chỉ nhận file JPG hoặc PNG.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException("Ảnh tối đa 10 MB.");
        }

        byte[] bytes;
        BufferedImage image;
        try {
            bytes = file.getBytes();
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new BusinessException("Không đọc được file ảnh, vui lòng chọn file khác.");
        }
        if (image == null) {
            throw new BusinessException("File không phải ảnh JPG/PNG hợp lệ.");
        }

        // Ten file ngau nhien (UUID) de 2 file trung ten khong de len nhau
        LocalDate today = LocalDate.now();
        String folder = UPLOAD_DIR + "/" + today.getYear() + "/" + String.format("%02d", today.getMonthValue());
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Path dir = Path.of(folder);
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Không lưu được file ảnh, vui lòng thử lại.", e);
        }

        attachment.setMediaType(ArticleMediaType.IMAGE);
        attachment.setUrl("/" + folder + "/" + fileName);
        attachment.setFileHash(ArticleMediaHasher.sha256Hex(bytes));
        attachment.setDhash(ArticleMediaHasher.dHash(image));
    }

    // Doi entity sang DTO kem ho ten tac gia, bai chua anh bi trung va ly do khong tinh tien (B22)
    private ArticleAttachmentResponse toResponse(ArticleAttachment attachment, Map<Integer, String> userNames) {
        ArticleAttachmentResponse dto = ArticleAttachmentResponse.from(attachment, userNames.get(attachment.getAuthorId()));
        if (attachment.getMatchedAttachmentId() != null) {
            Optional<ArticleAttachment> matched = articleAttachmentRepository.findById(attachment.getMatchedAttachmentId());
            if (matched.isPresent()) {
                dto.setMatchedArticleId(matched.get().getArticleId());
            }
        }
        if (attachment.getExternalSource() != null) {
            dto.setNoRoyaltyReason(ArticleAttachmentResponse.NO_ROYALTY_EXTERNAL);
        } else if (attachment.getMatchedAttachmentId() != null
                && royaltyGradingService.hasMediaBeenPaid(attachment.getMatchedAttachmentId())) {
            dto.setNoRoyaltyReason(ArticleAttachmentResponse.NO_ROYALTY_REUSED);
        }
        return dto;
    }

    // BR-IN-01: chi tac gia chinh them / xoa anh, khi bai Nhap hoac bi tra ve sua
    private void checkCanEditMedia(Article article, Integer currentUserId) {
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new BusinessException("Chỉ tác giả chính mới được thêm hoặc xóa ảnh, video của bài.");
        }
        if (article.getStatus() != ArticleStatus.DRAFT && article.getStatus() != ArticleStatus.REVISION_REQUESTED) {
            throw new BusinessException("Chỉ thêm hoặc xóa ảnh, video khi bài đang Nháp hoặc bị trả về sửa.");
        }
    }

    // Loai anh/video phai co trong bang media_factors (luong admin, doc qua MediaFactorService)
    private void checkMediaKind(String mediaKind) {
        for (MediaFactorResponse factor : mediaFactorService.getAllMediaFactors()) {
            if (factor.getMediaKind().equals(mediaKind)) {
                return;
            }
        }
        throw new BusinessException("Loại ảnh/video không hợp lệ.");
    }

    // Tac gia chinh + cac dong article_authors cua bai
    private Set<Integer> authorIdsOf(Article article) {
        Set<Integer> ids = new HashSet<>();
        ids.add(article.getAuthorId());
        for (ArticleAuthor row : articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId())) {
            ids.add(row.getUserId());
        }
        return ids;
    }

    // Tim bai theo ma, khong co thi bao 404 (NotFoundException)
    private Article findArticle(Integer articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết có mã " + articleId + "."));
    }

    // Tim anh/video thuoc dung bai, khong co thi bao 404
    private ArticleAttachment findAttachment(Integer articleId, Integer attachmentId) {
        Optional<ArticleAttachment> attachment = articleAttachmentRepository.findById(attachmentId);
        if (attachment.isEmpty() || !attachment.get().getArticleId().equals(articleId)) {
            throw new NotFoundException("Không tìm thấy ảnh/video có mã " + attachmentId + " trong bài này.");
        }
        return attachment.get();
    }

    // url luu dang "/uploads/2026/11/abc.jpg" -> file uploads/2026/11/abc.jpg (tinh tu thu muc chay server)
    private Path filePathOf(ArticleAttachment attachment) {
        return Path.of(attachment.getUrl().substring(1));
    }

    // "Anh Bia.JPG" -> "jpg"; khong co dau cham -> chuoi rong
    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    // Chuoi rong / toan khoang trang -> null (cot NULL trong DB)
    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
