package com.swp391.kpi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swp391.kpi.dto.*;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.*;
import com.swp391.kpi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Nghiep vu Truong ban quan ly khieu nai (UC-36, UC-43): hang cho, chi tiet, BTV hop le (khong phai
 * nguoi cham goc), phan cong, giao lai, giao cham lan 2.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   47  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   48  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   61  getQueue()                      lay queue
 *   91  getDetail()                     lay detail
 *   96  getCandidates()                 lay candidates
 *  102  assign()                        phan cong
 *  118  assignSecond()                  phan cong second
 *  143  reassign()                      phan cong lai
 *  ---- Ham phu (private, chi dung trong file) ----
 *  129  requiresSecond()                requires second
 *  164  scopedAppeal()                  scoped appeal
 *  179  currentAttempt()                current attempt
 *  187  candidates()                    candidates
 *  214  requireCandidate()              bat buoc candidate
 *  224  addHistoricalReviewer()         them historical reviewer
 *  234  assignmentJson()                assignment json
 *  243  response()                      response
 *  267  badRequest()                    bad request
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppealManagementService {
    private static final int MAX_PAGE_SIZE = 100;
    private final AppealRepository appeals;
    private final AppealCriterionRepository criteria;
    private final AppealRuleConfigRepository rules;
    private final UserReadService users;
    private final ArticleReadService articles;
    private final ScoringReadService scores;
    private final ScoringAppealService scoring;
    private final AuditService audit;
    private final NotificationService notifications;
    private final ObjectMapper json;
    private final jakarta.persistence.EntityManager em;
    private final ReviewerDeviationRefreshService deviations;

    public Page<AppealManagementResponse> getQueue(Integer userId, String status, Integer articleId,
            Integer reviewerId, LocalDate fromDate, LocalDate toDate, int page, int size) {
        var head = users.requireActive(userId, "HEAD_OF_DEPARTMENT");
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE
                || (fromDate != null && toDate != null && fromDate.isAfter(toDate)))
            throw badRequest("Trang, kích thước hoặc khoảng ngày không hợp lệ.");
        AppealStatus filter = null;
        if (status != null && !status.isBlank()) {
            try { filter = AppealStatus.valueOf(status); }
            catch (IllegalArgumentException ex) { throw badRequest("Trạng thái khiếu nại không hợp lệ."); }
        }
        List<AppealManagementResponse> rows = new ArrayList<>();
        Map<Integer, String> names = userNames();
        // shortcut: findAll() roi loc/phan trang bang Java, doi sang query co Pageable khi so khieu nai len hang tram.
        for (var appeal : appeals.findAll()) {
            var article = articles.getArticle(appeal.getArticleId());
            if (head.sectionId() == null || !head.sectionId().equals(article.sectionId())) continue;
            if (filter != null && filter != appeal.getStatus()) continue;
            if (articleId != null && !articleId.equals(article.id())) continue;
            LocalDate day = appeal.getSubmittedAt().toLocalDate();
            if ((fromDate != null && day.isBefore(fromDate)) || (toDate != null && day.isAfter(toDate))) continue;
            var current = currentAttempt(appeal);
            if (reviewerId != null && (current == null || !reviewerId.equals(current.reviewerId()))) continue;
            rows.add(response(appeal, article, current, false, names));
        }
        rows.sort(Comparator.comparing(AppealManagementResponse::submittedAt).reversed()
                .thenComparing(AppealManagementResponse::id, Comparator.reverseOrder()));
        var pageable = PageRequest.of(page, size);
        int start = (int) Math.min(pageable.getOffset(), rows.size());
        return new PageImpl<>(rows.subList(start, Math.min(start + size, rows.size())), pageable, rows.size());
    }

    public AppealManagementResponse getDetail(Integer id, Integer userId) {
        var appeal = scopedAppeal(id, userId, false);
        return response(appeal, articles.getArticle(appeal.getArticleId()), currentAttempt(appeal), true, userNames());
    }

    public List<AppealReviewerCandidateResponse> getCandidates(Integer id, Integer userId) {
        var appeal = scopedAppeal(id, userId, false);
        return candidates(appeal);
    }

    @Transactional
    public AppealManagementResponse assign(Integer id, AppealAssignRequest request, Integer userId) { // [LUONG 2 - PHAN CONG] buoc 5/6 service: dung ban? dang cho phan cong? nguoi chon co hop le? -> tao phieu -> IN_REVIEW
        var appeal = scopedAppeal(id, userId, true);
        if (appeal.getStatus() != AppealStatus.SUBMITTED)
            throw new BusinessException("Khiếu nại không còn chờ phân công.");
        var candidate = requireCandidate(appeal, request.getReviewerId(), request.getReason(), false);
        var attempt = scoring.createDraft(id, appeal.getArticleId(), appeal.getScoringAttemptId(),
                candidate.reviewerId(), candidate.crossDepartment());
        appeal.setStatus(AppealStatus.IN_REVIEW);
        audit.log(userId, "ASSIGN", "appeals", id, "{\"status\":\"SUBMITTED\"}",
                assignmentJson(attempt, request.getReason()));
        notifications.notifyUser(candidate.reviewerId(), appeal.getArticleId(),
                "Bạn được giao phiếu chấm mù #" + attempt.id() + ".");
        return response(appeal, articles.getArticle(appeal.getArticleId()), attempt, true, userNames());
    }

    @Transactional
    public AppealManagementResponse assignSecond(Integer id, AppealAssignRequest request, Integer userId) { // [LUONG 4 - SO SANH & CHOT] nhanh S2: chi giao khi S1 da nop va lech > T (requiresSecond)
        var appeal = scopedAppeal(id, userId, true);
        if (!requiresSecond(appeal)) throw new BusinessException("Khiếu nại chưa cần S2 hoặc đã được giao S2.");
        var candidate = requireCandidate(appeal, request.getReviewerId(), request.getReason(), false);
        var attempt = scoring.createSecondDraft(id, appeal.getArticleId(), appeal.getScoringAttemptId(),
                candidate.reviewerId(), candidate.crossDepartment());
        audit.log(userId, "ASSIGN", "appeals", id, null, assignmentJson(attempt, request.getReason()));
        notifications.notifyUser(candidate.reviewerId(), appeal.getArticleId(), "Bạn được giao phiếu chấm mù #" + attempt.id() + ".");
        return response(appeal, articles.getArticle(appeal.getArticleId()), attempt, true, userNames());
    }

    private boolean requiresSecond(Appeal appeal) {
        if (appeal.getStatus() != AppealStatus.IN_REVIEW) return false;
        List<ScoringAppealAttemptResponse> reviews = new ArrayList<>();
        for (var attempt : scoring.getArticleAttempts(appeal.getArticleId()))
            if (appeal.getId().equals(attempt.appealId())) reviews.add(attempt);
        if (reviews.size() != 1 || reviews.get(0).submittedAt() == null) return false;
        var original = scoring.getComparisonCard(appeal.getScoringAttemptId());
        var first = scoring.getComparisonCard(reviews.get(0).id());
        var rule = rules.findById(appeal.getRuleConfigId()).orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình khiếu nại."));
        return new AppealScoreCalculator().compare(original.totalScore(), first.totalScore(), null,
                rule.getVarianceThreshold()).requiresSecondReview();
    }

    @Transactional
    public AppealManagementResponse reassign(Integer id, AppealReassignRequest request, Integer userId) { // [LUONG 2 - PHAN CONG] nhanh phu "Giao lai" (UC-43): chi giao lai phieu CHUA nop, co kiem tra expectedReviewerId
        var appeal = scopedAppeal(id, userId, true);
        if (appeal.getStatus() != AppealStatus.IN_REVIEW)
            throw new BusinessException("Chỉ giao lại khiếu nại đang chấm.");
        var old = currentAttempt(appeal);
        if (old == null || !old.id().equals(request.getAttemptId())
                || !old.reviewerId().equals(request.getExpectedReviewerId()))
            throw new BusinessException("Phân công đã thay đổi. Vui lòng tải lại khiếu nại.");
        if (old.submittedAt() != null)
            throw new BusinessException("Không thể đổi người chấm cho phiếu đã nộp.");
        var candidate = requireCandidate(appeal, request.getReviewerId(), request.getReason(), true);
        var updated = scoring.reassignDraft(id, appeal.getArticleId(), old.id(), old.reviewerId(),
                candidate.reviewerId(), candidate.crossDepartment(), request.getReason(), userId);
        notifications.notifyUser(old.reviewerId(), appeal.getArticleId(),
                "Phiếu chấm mù #" + old.id() + " đã được giao cho biên tập viên khác.");
        notifications.notifyUser(updated.reviewerId(), appeal.getArticleId(),
                "Bạn được giao phiếu chấm mù #" + updated.id() + ".");
        appeals.flush();
        return response(appeal, articles.getArticle(appeal.getArticleId()), updated, true, userNames());
    }

    private Appeal scopedAppeal(Integer id, Integer userId, boolean lock) { // [LUONG 2 - PHAN CONG] 5a: chi Truong ban DUNG BAN cua bai moi duoc thao tac (+ khoa dong khi lock=true)
        var head = users.requireActive(userId, "HEAD_OF_DEPARTMENT");
        if (lock) deviations.lockWorkflow();
        var appeal = (lock ? appeals.findLockedById(id) : appeals.findById(id))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy khiếu nại."));
        if (lock) {
            em.refresh(appeal, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            scoring.lockReviewSnapshots(id, appeal.getArticleId());
        }
        var article = articles.getArticle(appeal.getArticleId());
        if (head.sectionId() == null || !head.sectionId().equals(article.sectionId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền quản lý khiếu nại này.");
        return appeal;
    }

    private ScoringAppealAttemptResponse currentAttempt(Appeal appeal) {
        ScoringAppealAttemptResponse current = null;
        for (var attempt : scoring.getArticleAttempts(appeal.getArticleId())) {
            if (appeal.getId().equals(attempt.appealId())) current = attempt;
        }
        return current;
    }

    private List<AppealReviewerCandidateResponse> candidates(Appeal appeal) { // [LUONG 2 - PHAN CONG] 5b: danh sach BTV hop le: loai nguoi da cham bai nay, uu tien cung ban
        var current = currentAttempt(appeal);
        if (appeal.getStatus() == AppealStatus.RESOLVED
                || (current != null && current.submittedAt() != null && !requiresSecond(appeal)))
            return List.of();
        Set<Integer> excluded = new HashSet<>();
        for (var attempt : scoring.getArticleAttempts(appeal.getArticleId())) excluded.add(attempt.reviewerId());
        for (var log : audit.getLogs(null, null, "appeals", appeal.getId(), null, null)) {
            if (!"ASSIGN".equals(log.getActionType()) && !"REASSIGN".equals(log.getActionType())) continue;
            addHistoricalReviewer(excluded, log.getOldValue()); addHistoricalReviewer(excluded, log.getNewValue());
        }
        Integer section = articles.getArticle(appeal.getArticleId()).sectionId();
        List<AppealReviewerCandidateResponse> same = new ArrayList<>();
        List<AppealReviewerCandidateResponse> other = new ArrayList<>();
        for (var editor : users.getUsers()) {
            if (!"ACTIVE".equals(editor.status()) || !"EDITOR".equals(editor.role())
                    || editor.sectionId() == null || excluded.contains(editor.id())) continue;
            boolean cross = !section.equals(editor.sectionId());
            var candidate = new AppealReviewerCandidateResponse(editor.id(), editor.fullName(),
                    editor.sectionId(), editor.sectionName(), cross);
            (cross ? other : same).add(candidate);
        }
        var result = same.isEmpty() ? other : same;
        result.sort(Comparator.comparing(AppealReviewerCandidateResponse::reviewerId));
        return result;
    }

    private AppealReviewerCandidateResponse requireCandidate(Appeal appeal, Integer id, String reason, boolean reassign) { // [LUONG 2 - PHAN CONG] 5c: chon nguoi ngoai danh sach 5b thi bi tu choi; khac ban/giao lai bat buoc co ly do
        for (var candidate : candidates(appeal)) {
            if (!candidate.reviewerId().equals(id)) continue;
            if ((candidate.crossDepartment() || reassign) && (reason == null || reason.isBlank()))
                throw new BusinessException("Phải nhập lý do giao lại hoặc phân công khác ban.");
            return candidate;
        }
        throw new BusinessException("Biên tập viên không đủ điều kiện hoặc đã tham gia chấm bài này.");
    }

    private void addHistoricalReviewer(Set<Integer> ids, String value) {
        if (value == null) return;
        try {
            var node = json.readTree(value).get("reviewerId");
            if (node != null && node.isIntegralNumber()) ids.add(node.intValue());
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new BusinessException("Nhật ký phân công không hợp lệ, cần kiểm tra trước khi giao tiếp.");
        }
    }

    private String assignmentJson(ScoringAppealAttemptResponse attempt, String reason) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("status", "IN_REVIEW"); value.put("attemptId", attempt.id());
        value.put("reviewerId", attempt.reviewerId()); value.put("crossDepartment", attempt.crossDepartment());
        value.put("reason", reason == null ? null : reason.trim());
        try { return json.writeValueAsString(value); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Không thể ghi nhật ký phân công khiếu nại.", ex); }
    }

    private AppealManagementResponse response(Appeal appeal, ArticleReadResponse article,
            ScoringAppealAttemptResponse attempt, boolean withCriteria, Map<Integer, String> names) {
        String appellant = names.getOrDefault(appeal.getAppellantId(), "Người dùng #" + appeal.getAppellantId());
        List<AppealCriterionResponse> items = new ArrayList<>();
        if (withCriteria) {
            var originalCriteria = scores.getCriteria(appeal.getScoringAttemptId());
            for (var criterion : criteria.findAllByAppealIdOrderById(appeal.getId())) {
                var item = new AppealCriterionResponse();
                item.setCriterionId(criterion.getCriterionId()); item.setReason(criterion.getReason());
                for (var original : originalCriteria) if (original.criterionId().equals(criterion.getCriterionId()))
                    item.setCriterionName(original.name());
                items.add(item);
            }
        }
        var rule = rules.findById(appeal.getRuleConfigId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cấu hình đã gắn với khiếu nại."));
        var announced = scores.getAttempt(appeal.getScoringAttemptId()).announcedAt();
        LocalDateTime deadline = announced == null ? null : announced.plusDays(rule.getAppealWindowDays());
        return new AppealManagementResponse(appeal.getId(), article.id(), article.title(), article.sectionId(),
                appeal.getAppellantId(), appellant, appeal.getStatus().name(), appeal.getSubmittedAt(), deadline,
                deadline != null && LocalDateTime.now().isAfter(deadline), attempt, items);
    }

    // Doc bang users 1 lan cho ca danh sach thay vi 1 lan moi dong.
    private Map<Integer, String> userNames() {
        Map<Integer, String> names = new HashMap<>();
        for (var user : users.getUsers()) names.put(user.id(), user.fullName());
        return names;
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
