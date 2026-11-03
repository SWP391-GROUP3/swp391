package com.swp391.kpi.service;

import com.swp391.kpi.dto.ScoringAttemptDetailResponse;
import com.swp391.kpi.dto.ScoringAttemptResponse;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.swp391.kpi.dto.*;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Service xu ly lan cham diem cua BTV (UC-22, BR-SC-03, BR-SC-06)
/**
 * Nghiep vu lan cham cua BTV (UC-22...28, BR-SC-03, BR-SC-06): mo phieu va chup bo tieu chi ACTIVE,
 * doi the loai truoc khi cham, luu diem nhap, nop quyet dinh.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   47  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   63  openScoringAttempt()            Mo phieu cham cho bai viet va snapshot bo tieu chi ACTIVE
 *  151  getMyScorings()                 M-A6, M-A7: "Lich su cham" cua BTV dang dang nhap - moi bai duoc giao cho BTV (assignments.editor_id) kem lan…
 *  195  getPublishedAttempt()           M-D2 (cho BF-03, BF-04, BF-05): lan cham da duoc Truong ban duyet va cong bo diem moi nhat cua bai (diem S0)
 *  211  getAttemptCriteria()            M-D2: diem + nhan xet tung tieu chi cua 1 lan cham, theo thu tu tieu chi
 *  224  getLatestAttemptByArticle()     Lay lan cham gan nhat cua mot bai viet (UC-30, UC-34)
 *  267  getLatestAttemptForUser()       Lay lan cham gan nhat cua mot bai viet kem kiem tra quyen nguoi dung (UC-30, UC-34)
 *  310  changeCategory()                BTV doi the loai bai viet truoc khi cham (UC-23, BR-SC-05)
 *  385  saveDraftScores()               BTV luu diem tung tieu chi va tinh tong diem theo trong so (UC-24, UC-25, BR-SC-06, BR-SC-07)
 *  464  submitDecision()                BTV nop phieu cham voi 1 trong 3 quyet dinh: ACCEPT, REVISION, REJECT (UC-26, UC-27, UC-28)
 *  ---- Ham phu (private, chi dung trong file) ----
 *  541  findLatestAttempt()             M-A2: lan cham moi nhat cua BF-02 (INITIAL / RESCORE), bo qua lan cham APPEAL do BF-03 ghi
 */
@Service
@RequiredArgsConstructor
public class ScoringAttemptService {

    private final ScoringAttemptRepository scoringAttemptRepository;
    private final ScoringAttemptDetailRepository scoringAttemptDetailRepository;
    private final CriteriaSetVersionRepository criteriaSetVersionRepository;
    private final CriteriaRepository criteriaRepository;
    private final AssignmentRepository assignmentRepository;
    private final ArticleRepository articleRepository;
    private final ArticleVersionRepository articleVersionRepository;
    private final CategoryChangeLogRepository categoryChangeLogRepository;
    private final GenreRepository genreRepository;
    private final ArticleService articleService;
    private final ScoreApprovalRepository scoreApprovalRepository;
    private final UserRepository userRepository;
    private final AppealRuleConfigService appealRuleConfigService;   // M-A5: so ngay khieu nai lay tu cau hinh BF-03

    // Mo phieu cham cho bai viet va snapshot bo tieu chi ACTIVE
    @Transactional
    public synchronized ScoringAttemptResponse openScoringAttempt(Integer articleId, Integer reviewerId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        Assignment assignment = assignmentRepository.findByArticleId(articleId)
                .orElseThrow(() -> new BusinessException("Bài viết chưa được phân công cho Biên tập viên"));

        if (!assignment.getEditorId().equals(reviewerId)) {
            throw new BusinessException("Bạn không được phân công chấm bài viết này!");
        }

        // M-A2: bo qua lan cham APPEAL (BF-03) -> BTV goc khong thay / khong tiep tuc ban nhap cham khieu nai
        Optional<ScoringAttempt> openAttemptOpt = findLatestAttempt(articleId);

        // Neu da co attempt:
        // 1. Chua nop (submitted_at == null): tra ve de tiep tuc cham.
        // 2. Hoac bai viet khong con o trang thai ASSIGNED (vd dang PENDING_APPROVAL, SCORE_PUBLISHED, PUBLISHED...):
        //    tra ve phieu cham da nop de xem lai (read-only), tuyet doi khong tao attempt moi lam ghi de ket qua.
        if (openAttemptOpt.isPresent()) {
            ScoringAttempt latestAttempt = openAttemptOpt.get();
            if (latestAttempt.getSubmittedAt() == null || (article.getStatus() != null && article.getStatus() != ArticleStatus.ASSIGNED)) {
                List<ScoringAttemptDetail> details = scoringAttemptDetailRepository.findByScoringAttemptId(latestAttempt.getId());
                List<ScoringAttemptDetailResponse> detailResponses = details.stream()
                        .map(ScoringAttemptDetailResponse::from)
                        .toList();
                return ScoringAttemptResponse.from(latestAttempt, detailResponses, article.getTitle(), article.getContent());
            }
        }

        // Lay the loai de tim bo tieu chi ACTIVE
        Integer categoryId = article.getCategoryId() != null ? article.getCategoryId() : article.getDeclaredCategoryId();
        CriteriaSetVersion activeCriteriaSet = criteriaSetVersionRepository
                .findByCategoryIdAndStatus(categoryId, CriteriaSetStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("Thể loại của bài viết chưa có bộ tiêu chí nào ở trạng thái ACTIVE"));

        List<Criteria> criteriaList = criteriaRepository
                .findByCriteriaSetVersionIdOrderBySortOrderAsc(activeCriteriaSet.getId());
        if (criteriaList.isEmpty()) {
            throw new BusinessException("Bộ tiêu chí ACTIVE chưa có tiêu chí thành phần nào");
        }

        // Lay ban nop moi nhat cua bai viet
        ArticleVersion articleVersion = articleVersionRepository
                .findFirstByArticleIdAndKindOrderByVersionNoDesc(articleId, ArticleVersionKind.SUBMISSION)
                .orElseThrow(() -> new BusinessException("Bài viết chưa có bản nộp nào"));

        // So attempt_no tiep theo tinh tren moi lan cham, ke ca APPEAL (UNIQUE article_id, attempt_no)
        int nextAttemptNo = scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(articleId)
                .map(a -> a.getAttemptNo() + 1).orElse(1);
        AttemptType attemptType = openAttemptOpt.isEmpty() ? AttemptType.INITIAL : AttemptType.RESCORE;

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setArticleId(articleId);
        attempt.setVersionId(articleVersion.getId());
        attempt.setAssignment(assignment);
        attempt.setReviewerId(reviewerId);
        attempt.setAttemptNo(nextAttemptNo);
        attempt.setAttemptType(attemptType);
        attempt.setCriteriaSetVersion(activeCriteriaSet);
        attempt.setCategoryId(categoryId);
        attempt.setOpenedAt(LocalDateTime.now());
        attempt.setIsCrossDepartment(false);
        if (openAttemptOpt.isPresent()) {
            attempt.setReplacesAttempt(openAttemptOpt.get());
        }

        ScoringAttempt savedAttempt = scoringAttemptRepository.save(attempt);

        // Snapshot danh sach tieu chi vao scoring_attempt_details
        List<ScoringAttemptDetailResponse> detailResponses = new ArrayList<>();
        for (Criteria c : criteriaList) {
            ScoringAttemptDetail detail = new ScoringAttemptDetail();
            detail.setScoringAttempt(savedAttempt);
            detail.setCriterion(c);
            detail.setScore(BigDecimal.ZERO);
            detail.setComment(null);
            ScoringAttemptDetail savedDetail = scoringAttemptDetailRepository.save(detail);
            detailResponses.add(ScoringAttemptDetailResponse.from(savedDetail));
        }

        return ScoringAttemptResponse.from(savedAttempt, detailResponses, article.getTitle(), article.getContent());
    }

    /**
     * M-A6, M-A7: "Lich su cham" cua BTV dang dang nhap - moi bai duoc giao cho BTV (assignments.editor_id)
     * kem lan cham BF-02 moi nhat. Bai bi Truong ban tra cham lai thi kem ly do (UC-32).
     */
    @Transactional(readOnly = true)
    public List<ScoringAttemptMyItemResponse> getMyScorings(Integer editorId) {
        List<ScoringAttemptMyItemResponse> result = new ArrayList<>();
        for (Assignment assignment : assignmentRepository.findByEditorIdOrderByAssignedAtDesc(editorId)) {
            Article article = articleRepository.findById(assignment.getArticleId()).orElse(null);
            if (article == null) {
                continue;
            }
            ScoringAttemptMyItemResponse item = new ScoringAttemptMyItemResponse();
            item.setArticleId(article.getId());
            item.setTitle(article.getTitle());
            item.setArticleStatus(article.getStatus());
            Integer categoryId = article.getCategoryId() != null ? article.getCategoryId() : article.getDeclaredCategoryId();
            item.setCategoryName(genreRepository.findById(categoryId).map(Genre::getName).orElse("Thể loại #" + categoryId));
            item.setAssignedAt(assignment.getAssignedAt());

            Optional<ScoringAttempt> latest = findLatestAttempt(article.getId());
            if (latest.isPresent()) {
                ScoringAttempt attempt = latest.get();
                item.setAttemptId(attempt.getId());
                item.setAttemptNo(attempt.getAttemptNo());
                item.setAttemptType(attempt.getAttemptType());
                item.setTotalScore(attempt.getTotalScore());
                item.setDecision(attempt.getDecision());
                item.setSubmittedAt(attempt.getSubmittedAt());
                // Lan cham lai (RESCORE) thay cho lan bi Truong ban tra ve -> lay ly do tra
                if (attempt.getReplacesAttempt() != null) {
                    scoreApprovalRepository.findByScoringAttemptId(attempt.getReplacesAttempt().getId())
                            .filter(approval -> approval.getDecision() == ApprovalDecision.RETURNED)
                            .ifPresent(approval -> {
                                item.setReturnReason(approval.getReason());
                                item.setReturnedAt(approval.getDecidedAt());
                            });
                }
            }
            result.add(item);
        }
        return result;
    }

    /**
     * M-D2 (cho BF-03, BF-04, BF-05): lan cham da duoc Truong ban duyet va cong bo diem moi nhat cua bai (diem S0).
     * Lan cham APPEAL khong co dong duyet nen khong bao gio duoc chon. Chua cong bo diem -> null.
     */
    @Transactional(readOnly = true)
    public ScoringReadResponse getPublishedAttempt(Integer articleId) {
        List<ScoreApproval> approvals = scoreApprovalRepository
                .findByScoringAttemptArticleIdAndDecisionAndAnnouncedAtIsNotNullOrderByAnnouncedAtDesc(
                        articleId, ApprovalDecision.APPROVED);
        if (approvals.isEmpty()) {
            return null;
        }
        ScoreApproval approval = approvals.get(0);
        ScoringAttempt attempt = approval.getScoringAttempt();
        return new ScoringReadResponse(attempt.getId(), attempt.getArticleId(), attempt.getReviewerId(),
                attempt.getAttemptType().name(), attempt.getTotalScore(), attempt.getSubmittedAt(),
                approval.getAnnouncedAt(), attempt.getAppealId());
    }

    // M-D2: diem + nhan xet tung tieu chi cua 1 lan cham, theo thu tu tieu chi
    @Transactional(readOnly = true)
    public List<ScoringCriterionResponse> getAttemptCriteria(Integer attemptId) {
        List<ScoringCriterionResponse> result = new ArrayList<>();
        for (ScoringAttemptDetail detail : scoringAttemptDetailRepository.findByScoringAttemptId(attemptId)) {
            Criteria criterion = detail.getCriterion();
            result.add(new ScoringCriterionResponse(criterion.getId(), criterion.getName(),
                    detail.getScore(), detail.getComment()));
        }
        result.sort(java.util.Comparator.comparing(ScoringCriterionResponse::criterionId));
        return result;
    }

    // Lay lan cham gan nhat cua mot bai viet (UC-30, UC-34)
    @Transactional(readOnly = true)
    public ScoringAttemptResponse getLatestAttemptByArticle(Integer articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        ScoringAttempt attempt = findLatestAttempt(articleId)
                .orElseThrow(() -> new NotFoundException("Bài viết này chưa có lần chấm điểm nào"));

        List<ScoringAttemptDetail> details = scoringAttemptDetailRepository.findByScoringAttemptId(attempt.getId());
        List<ScoringAttemptDetailResponse> detailResponses = details.stream()
                .map(ScoringAttemptDetailResponse::from)
                .toList();

        ScoringAttemptResponse res = ScoringAttemptResponse.from(attempt, detailResponses, article.getTitle(), article.getContent());

        // Bo sung thong tin BTV cham
        if (attempt.getReviewerId() != null) {
            userRepository.findById(attempt.getReviewerId())
                    .ifPresent(u -> res.setReviewerName(u.getFullName()));
        }

        // Bo sung thong tin cong bo diem va thoi han khieu nai (BR-SC-10, UC-34)
        scoreApprovalRepository.findByScoringAttemptId(attempt.getId()).ifPresent(approval -> {
            if (approval.getAnnouncedAt() != null) {
                LocalDateTime announcedAt = approval.getAnnouncedAt();
                // M-A5: han = luc cong bo + appeal_window_days cua cau hinh dang bat (BF-03, Q-D2)
                LocalDateTime deadline = appealRuleConfigService.getAppealDeadline(announcedAt);
                boolean open = LocalDateTime.now().isBefore(deadline);
                long days = open ? java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), deadline) : 0;

                res.setAnnouncedAt(announcedAt);
                res.setAppealDeadline(deadline);
                res.setIsAppealWindowOpen(open);
                res.setDaysRemaining((int) Math.max(0, days));
            }
        });

        return res;
    }

    /**
     * Lay lan cham gan nhat cua mot bai viet kem kiem tra quyen nguoi dung (UC-30, UC-34).
     */
    @Transactional(readOnly = true)
    public ScoringAttemptResponse getLatestAttemptForUser(Integer articleId, Integer currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));
        RoleCode roleCode = user.getRole() != null ? user.getRole().getCode() : null;

        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        // ADMIN va Truong ban luon co quyen xem
        if (RoleCode.ADMIN.equals(roleCode) || RoleCode.HEAD_OF_DEPARTMENT.equals(roleCode)) {
            return getLatestAttemptByArticle(articleId);
        }

        // BTV phai la nguoi duoc phan cong bai viet hoac la nguoi cham
        if (RoleCode.EDITOR.equals(roleCode)) {
            Assignment assignment = assignmentRepository.findByArticleId(articleId).orElse(null);
            boolean isAssignedEditor = assignment != null && assignment.getEditorId().equals(currentUserId);

            ScoringAttempt attempt = findLatestAttempt(articleId).orElse(null);
            boolean isReviewer = attempt != null && currentUserId.equals(attempt.getReviewerId());

            if (!isAssignedEditor && !isReviewer) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không được phân công biên tập bài viết này");
            }
            return getLatestAttemptByArticle(articleId);
        }

        // Tac gia chinh hoac dong tac gia cua bai viet (UC-34: "main author or a co-author")
        if (articleService.isAuthorOrCoAuthor(articleId, currentUserId)) {
            // Chi duoc xem khi diem da cong bo hoac bai bi tra ve / tu choi
            ArticleStatus status = article.getStatus();
            if (status == ArticleStatus.SCORE_PUBLISHED || status == ArticleStatus.PUBLISHED
                    || status == ArticleStatus.REVISION_REQUESTED || status == ArticleStatus.REJECTED) {
                return getLatestAttemptByArticle(articleId);
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Điểm đánh giá của bài viết đang chờ phê duyệt, chưa được công bố");
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem thông tin phiếu chấm của bài viết này");
    }

    // BTV doi the loai bai viet truoc khi cham (UC-23, BR-SC-05)
    @Transactional
    public ScoringAttemptResponse changeCategory(Integer articleId, Integer newCategoryId, Integer editorId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        Assignment assignment = assignmentRepository.findByArticleId(articleId)
                .orElseThrow(() -> new BusinessException("Bài viết chưa được phân công"));

        if (!assignment.getEditorId().equals(editorId)) {
            throw new BusinessException("Bạn không được phân công chấm bài viết này!");
        }

        genreRepository.findById(newCategoryId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại mới với ID: " + newCategoryId));

        Integer currentCategoryId = article.getCategoryId() != null ? article.getCategoryId() : article.getDeclaredCategoryId();
        if (currentCategoryId.equals(newCategoryId)) {
            throw new BusinessException("Thể loại mới phải khác với thể loại hiện tại của bài viết!");
        }

        ScoringAttempt attempt = findLatestAttempt(articleId)
                .orElseGet(() -> {
                    openScoringAttempt(articleId, editorId);
                    return findLatestAttempt(articleId).orElseThrow();
                });

        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException("Không thể đổi thể loại cho phiếu chấm đã nộp!");
        }

        CriteriaSetVersion newActiveCriteriaSet = criteriaSetVersionRepository
                .findByCategoryIdAndStatus(newCategoryId, CriteriaSetStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("Thể loại mới chưa có bộ tiêu chí nào ở trạng thái ACTIVE"));

        List<Criteria> newCriteriaList = criteriaRepository
                .findByCriteriaSetVersionIdOrderBySortOrderAsc(newActiveCriteriaSet.getId());
        if (newCriteriaList.isEmpty()) {
            throw new BusinessException("Bộ tiêu chí ACTIVE của thể loại mới chưa có tiêu chí thành phần nào");
        }

        // Luu log vao category_change_logs
        CategoryChangeLog log = new CategoryChangeLog();
        log.setAssignment(assignment);
        log.setFromCategoryId(currentCategoryId);
        log.setToCategoryId(newCategoryId);
        log.setChangedBy(editorId);
        log.setChangedAt(LocalDateTime.now());
        categoryChangeLogRepository.save(log);

        // M-A4: doi the loai bai qua ArticleService (AGENTS.md muc 2: khong ghi thang bang cua BF-01)
        articleService.changeCategory(articleId, newCategoryId);

        // Cap nhat the loai va snapshot bo tieu chi moi cho attempt
        attempt.setCategoryId(newCategoryId);
        attempt.setCriteriaSetVersion(newActiveCriteriaSet);
        scoringAttemptRepository.save(attempt);

        List<ScoringAttemptDetail> oldDetails = scoringAttemptDetailRepository.findByScoringAttemptId(attempt.getId());
        scoringAttemptDetailRepository.deleteAll(oldDetails);

        List<ScoringAttemptDetailResponse> detailResponses = new ArrayList<>();
        for (Criteria c : newCriteriaList) {
            ScoringAttemptDetail detail = new ScoringAttemptDetail();
            detail.setScoringAttempt(attempt);
            detail.setCriterion(c);
            detail.setScore(BigDecimal.ZERO);
            detail.setComment(null);
            ScoringAttemptDetail savedDetail = scoringAttemptDetailRepository.save(detail);
            detailResponses.add(ScoringAttemptDetailResponse.from(savedDetail));
        }

        return ScoringAttemptResponse.from(attempt, detailResponses, article.getTitle(), article.getContent());
    }

    // BTV luu diem tung tieu chi va tinh tong diem theo trong so (UC-24, UC-25, BR-SC-06, BR-SC-07)
    @Transactional
    public ScoringAttemptResponse saveDraftScores(Integer articleId, ScoringSaveRequest request, Integer editorId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        Assignment assignment = assignmentRepository.findByArticleId(articleId)
                .orElseThrow(() -> new BusinessException("Bài viết chưa được phân công"));

        if (!assignment.getEditorId().equals(editorId)) {
            throw new BusinessException("Bạn không được phân công chấm bài viết này!");
        }

        ScoringAttempt attempt = findLatestAttempt(articleId)
                .orElseThrow(() -> new NotFoundException("Bài viết này chưa mở phiếu chấm điểm"));

        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException("Không thể chỉnh sửa điểm cho phiếu chấm đã nộp!");
        }

        List<ScoringAttemptDetail> details = scoringAttemptDetailRepository.findByScoringAttemptId(attempt.getId());
        if (details.isEmpty()) {
            throw new BusinessException("Phiếu chấm chưa có danh sách tiêu chí nào!");
        }

        // Kiem tra moi criterionId trong request phai thuoc phieu cham nay
        for (CriteriaScoreItemRequest item : request.getScores()) {
            boolean exists = details.stream().anyMatch(d -> d.getCriterion().getId().equals(item.getCriterionId()));
            if (!exists) {
                throw new BusinessException("Tiêu chí ID " + item.getCriterionId() + " không thuộc phiếu chấm của bài viết này!");
            }
        }

        // Cap nhat diem va nhan xet
        List<CriteriaScoreInput> calcInputs = new ArrayList<>();
        List<ScoringAttemptDetailResponse> detailResponses = new ArrayList<>();

        for (ScoringAttemptDetail detail : details) {
            Integer cId = detail.getCriterion().getId();
            CriteriaScoreItemRequest inputItem = request.getScores().stream()
                    .filter(s -> s.getCriterionId().equals(cId))
                    .findFirst()
                    .orElse(null);

            if (inputItem != null) {
                BigDecimal score = inputItem.getScore().setScale(2, RoundingMode.HALF_UP);
                String comment = inputItem.getComment() != null ? inputItem.getComment().trim() : null;

                // BR-SC-06: neu diem < 50 bat buoc phai co nhan xet
                if (score.compareTo(BigDecimal.valueOf(50)) < 0) {
                    if (comment == null || comment.isEmpty()) {
                        throw new BusinessException("Tiêu chí '" + detail.getCriterion().getName() + "' đạt dưới 50 điểm (" + score + ") bắt buộc phải nhập nhận xét lý do!");
                    }
                }

                detail.setScore(score);
                detail.setComment(comment);
                scoringAttemptDetailRepository.save(detail);
            }

            calcInputs.add(new CriteriaScoreInput(
                    detail.getCriterion().getId(),
                    detail.getCriterion().getName(),
                    detail.getCriterion().getWeight(),
                    detail.getScore(),
                    detail.getComment()
            ));
            detailResponses.add(ScoringAttemptDetailResponse.from(detail));
        }

        // Tinh tong diem theo cong thuc BR-SC-07
        ScoreCalculator calculator = new ScoreCalculator();
        BigDecimal totalScore = calculator.calculateTotalScore(calcInputs);
        attempt.setTotalScore(totalScore);
        scoringAttemptRepository.save(attempt);

        return ScoringAttemptResponse.from(attempt, detailResponses, article.getTitle(), article.getContent());
    }

    // BTV nop phieu cham voi 1 trong 3 quyet dinh: ACCEPT, REVISION, REJECT (UC-26, UC-27, UC-28)
    @Transactional
    public ScoringAttemptResponse submitDecision(Integer articleId, com.swp391.kpi.dto.ScoringSubmitRequest request, Integer editorId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết với ID: " + articleId));

        Assignment assignment = assignmentRepository.findByArticleId(articleId)
                .orElseThrow(() -> new BusinessException("Bài viết chưa được phân công"));

        if (!assignment.getEditorId().equals(editorId)) {
            throw new BusinessException("Bạn không được phân công chấm bài viết này!");
        }

        ScoringAttempt attempt = findLatestAttempt(articleId)
                .orElseThrow(() -> new NotFoundException("Bài viết này chưa mở phiếu chấm điểm"));

        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException("Phiếu chấm điểm này đã được nộp trước đó!");
        }

        ScoringDecision decision = request.getDecision();
        String decisionNote = request.getDecisionNote() != null ? request.getDecisionNote().trim() : null;

        if (decision == ScoringDecision.ACCEPT) {
            // UC-26: Phai co tong diem hop le
            if (attempt.getTotalScore() == null) {
                throw new BusinessException("Vui lòng nhập và lưu điểm các tiêu chí trước khi chọn Đồng ý (ACCEPT)!");
            }
            // Chuyen trang thai bai viet sang PENDING_APPROVAL
            articleService.updateArticleStatus(articleId, ArticleStatus.PENDING_APPROVAL, editorId, "SUBMIT_SCORE", decisionNote);
        } else if (decision == ScoringDecision.REVISION) {
            // UC-27: Bat buoc phai co nhan xet sua doi
            if (decisionNote == null || decisionNote.isEmpty()) {
                throw new BusinessException("Yêu cầu sửa đổi (REVISION) bắt buộc phải nhập nhận xét lý do!");
            }
            // Chuyen trang thai bai viet sang REVISION_REQUESTED
            articleService.updateArticleStatus(articleId, ArticleStatus.REVISION_REQUESTED, editorId, "RETURN", decisionNote);
        } else if (decision == ScoringDecision.REJECT) {
            // UC-28: Bat buoc phai co ly do tu choi
            if (decisionNote == null || decisionNote.isEmpty()) {
                throw new BusinessException("Từ chối bài viết (REJECT) bắt buộc phải nhập lý do từ chối!");
            }
            // Chuyen trang thai bai viet sang REJECTED
            articleService.updateArticleStatus(articleId, ArticleStatus.REJECTED, editorId, "REJECT", decisionNote);
        } else {
            throw new BusinessException("Quyết định không hợp lệ: " + decision);
        }

        // Tinh tong diem neu chua co de thoa man check constraint ck_scoring_attempts_submitted
        if (attempt.getTotalScore() == null) {
            List<ScoringAttemptDetail> currentDetails = scoringAttemptDetailRepository.findByScoringAttemptId(attempt.getId());
            BigDecimal total = BigDecimal.ZERO;
            if (currentDetails != null && !currentDetails.isEmpty()) {
                for (ScoringAttemptDetail d : currentDetails) {
                    BigDecimal score = d.getScore() != null ? d.getScore() : BigDecimal.ZERO;
                    BigDecimal weight = (d.getCriterion() != null && d.getCriterion().getWeight() != null)
                            ? d.getCriterion().getWeight()
                            : BigDecimal.ZERO;
                    BigDecimal contribution = score.multiply(weight).divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP);
                    total = total.add(contribution);
                }
            }
            attempt.setTotalScore(total.setScale(2, RoundingMode.HALF_UP));
        }

        attempt.setDecision(decision);
        attempt.setDecisionNote(decisionNote);
        attempt.setSubmittedAt(LocalDateTime.now());
        ScoringAttempt savedAttempt = scoringAttemptRepository.save(attempt);

        List<ScoringAttemptDetail> details = scoringAttemptDetailRepository.findByScoringAttemptId(savedAttempt.getId());
        List<ScoringAttemptDetailResponse> detailResponses = details.stream()
                .map(ScoringAttemptDetailResponse::from)
                .toList();

        return ScoringAttemptResponse.from(savedAttempt, detailResponses, article.getTitle(), article.getContent());
    }

    // M-A2: lan cham moi nhat cua BF-02 (INITIAL / RESCORE), bo qua lan cham APPEAL do BF-03 ghi
    private Optional<ScoringAttempt> findLatestAttempt(Integer articleId) {
        return scoringAttemptRepository.findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc(articleId, AttemptType.APPEAL);
    }
}
