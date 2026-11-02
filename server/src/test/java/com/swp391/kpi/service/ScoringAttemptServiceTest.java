package com.swp391.kpi.service;

import com.swp391.kpi.dto.ScoringAttemptResponse;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test cho ScoringAttemptService (Task 2.1 - Mở phiếu chấm)")
class ScoringAttemptServiceTest {

    @Mock
    private ScoringAttemptRepository scoringAttemptRepository;

    @Mock
    private ScoringAttemptDetailRepository scoringAttemptDetailRepository;

    @Mock
    private CriteriaSetVersionRepository criteriaSetVersionRepository;

    @Mock
    private CriteriaRepository criteriaRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleVersionRepository articleVersionRepository;

    @Mock
    private CategoryChangeLogRepository categoryChangeLogRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private ArticleService articleService;

    @Mock
    private ScoreApprovalRepository scoreApprovalRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ScoringAttemptService scoringAttemptService;

    private Article article;
    private Assignment assignment;
    private CriteriaSetVersion activeCriteriaSet;
    private Criteria criterion1;

    @BeforeEach
    void setUp() {
        article = new Article();
        article.setId(10);
        article.setTitle("Bài báo mẫu");
        article.setContent("Nội dung bài báo mẫu");
        article.setAuthorId(1);
        article.setDeclaredCategoryId(2);
        article.setCategoryId(2);

        assignment = new Assignment();
        assignment.setId(100);
        assignment.setArticleId(10);
        assignment.setEditorId(3); // Editor ID = 3
        assignment.setAssignedBy(5);

        activeCriteriaSet = new CriteriaSetVersion();
        activeCriteriaSet.setId(1);
        activeCriteriaSet.setCategoryId(2);
        activeCriteriaSet.setStatus(CriteriaSetStatus.ACTIVE);

        criterion1 = new Criteria();
        criterion1.setId(101);
        criterion1.setCriteriaSetVersion(activeCriteriaSet);
        criterion1.setName("Tính thời sự");
        criterion1.setWeight(new BigDecimal("50.00"));
        criterion1.setSortOrder(1);
    }

    @Test
    @DisplayName("Mở phiếu chấm thành công lần đầu và snapshot tiêu chí")
    void testOpenScoringAttempt_SuccessInitial() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.empty());
        when(criteriaSetVersionRepository.findByCategoryIdAndStatus(2, CriteriaSetStatus.ACTIVE))
                .thenReturn(Optional.of(activeCriteriaSet));
        when(criteriaRepository.findByCriteriaSetVersionIdOrderBySortOrderAsc(1))
                .thenReturn(List.of(criterion1));

        ArticleVersion articleVersion = new ArticleVersion();
        articleVersion.setId(201);
        when(articleVersionRepository.findFirstByArticleIdAndKindOrderByVersionNoDesc(10, ArticleVersionKind.SUBMISSION))
                .thenReturn(Optional.of(articleVersion));

        when(scoringAttemptRepository.save(any(ScoringAttempt.class))).thenAnswer(invocation -> {
            ScoringAttempt a = invocation.getArgument(0);
            a.setId(501);
            return a;
        });

        when(scoringAttemptDetailRepository.save(any(ScoringAttemptDetail.class))).thenAnswer(invocation -> {
            ScoringAttemptDetail d = invocation.getArgument(0);
            d.setId(601);
            return d;
        });

        ScoringAttemptResponse res = scoringAttemptService.openScoringAttempt(10, 3);

        assertNotNull(res);
        assertEquals(501, res.getId());
        assertEquals(10, res.getArticleId());
        assertEquals(3, res.getReviewerId());
        assertEquals(1, res.getAttemptNo());
        assertEquals(AttemptType.INITIAL, res.getAttemptType());
        assertEquals(1, res.getDetails().size());
        assertEquals("Tính thời sự", res.getDetails().get(0).getCriterionName());
    }

    @Test
    @DisplayName("Mở lại phiếu chấm đang làm dở (chưa nộp) thì trả về attempt hiện tại không tạo mới")
    void testOpenScoringAttempt_ReturnExistingOpenAttempt() {
        ScoringAttempt existingAttempt = new ScoringAttempt();
        existingAttempt.setId(502);
        existingAttempt.setArticleId(10);
        existingAttempt.setReviewerId(3);
        existingAttempt.setAttemptNo(1);
        existingAttempt.setAttemptType(AttemptType.INITIAL);
        existingAttempt.setSubmittedAt(null); // Đang làm dở

        ScoringAttemptDetail existingDetail = new ScoringAttemptDetail();
        existingDetail.setId(602);
        existingDetail.setScoringAttempt(existingAttempt);
        existingDetail.setCriterion(criterion1);
        existingDetail.setScore(new BigDecimal("75.00"));

        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10))
                .thenReturn(Optional.of(existingAttempt));
        when(scoringAttemptDetailRepository.findByScoringAttemptId(502))
                .thenReturn(List.of(existingDetail));

        ScoringAttemptResponse res = scoringAttemptService.openScoringAttempt(10, 3);

        assertNotNull(res);
        assertEquals(502, res.getId());
        verify(scoringAttemptRepository, never()).save(any(ScoringAttempt.class));
    }

    @Test
    @DisplayName("Mở phiếu chấm của bài viết đã nộp đang chờ duyệt (PENDING_APPROVAL) trả về attempt đã nộp, không tạo attempt mới")
    void testOpenScoringAttempt_SubmittedArticlePendingApproval_ReturnsSubmittedAttemptWithoutCreatingNew() {
        article.setStatus(ArticleStatus.PENDING_APPROVAL);

        ScoringAttempt submittedAttempt = new ScoringAttempt();
        submittedAttempt.setId(503);
        submittedAttempt.setArticleId(10);
        submittedAttempt.setReviewerId(3);
        submittedAttempt.setAttemptNo(1);
        submittedAttempt.setAttemptType(AttemptType.INITIAL);
        submittedAttempt.setDecision(ScoringDecision.ACCEPT);
        submittedAttempt.setTotalScore(new BigDecimal("88.00"));
        submittedAttempt.setSubmittedAt(LocalDateTime.now().minusMinutes(5));

        ScoringAttemptDetail detail = new ScoringAttemptDetail();
        detail.setId(603);
        detail.setScoringAttempt(submittedAttempt);
        detail.setCriterion(criterion1);
        detail.setScore(new BigDecimal("88.00"));

        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10))
                .thenReturn(Optional.of(submittedAttempt));
        when(scoringAttemptDetailRepository.findByScoringAttemptId(503))
                .thenReturn(List.of(detail));

        ScoringAttemptResponse res = scoringAttemptService.openScoringAttempt(10, 3);

        assertNotNull(res);
        assertEquals(503, res.getId());
        assertEquals(new BigDecimal("88.00"), res.getTotalScore());
        assertEquals(ScoringDecision.ACCEPT, res.getDecision());
        assertNotNull(res.getSubmittedAt());
        verify(scoringAttemptRepository, never()).save(any(ScoringAttempt.class));
    }

    @Test
    @DisplayName("Ném BusinessException khi BTV khác cố mở bài không được phân công cho mình")
    void testOpenScoringAttempt_WrongReviewer_ThrowsException() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoringAttemptService.openScoringAttempt(10, 99) // Editor 99 khong phai 3
        );

        assertTrue(ex.getMessage().contains("Bạn không được phân công chấm bài viết này"));
    }

    @Test
    @DisplayName("Ném BusinessException khi thể loại chưa có bộ tiêu chí ACTIVE")
    void testOpenScoringAttempt_NoActiveCriteriaSet_ThrowsException() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.empty());
        when(criteriaSetVersionRepository.findByCategoryIdAndStatus(2, CriteriaSetStatus.ACTIVE))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoringAttemptService.openScoringAttempt(10, 3)
        );

        assertTrue(ex.getMessage().contains("chưa có bộ tiêu chí nào ở trạng thái ACTIVE"));
    }

    @Test
    @DisplayName("Đổi thể loại thành công: lưu log, cập nhật bài viết và snapshot bộ tiêu chí mới")
    void testChangeCategory_Success() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));
        when(genreRepository.findById(4)).thenReturn(Optional.of(new Genre()));

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setId(501);
        attempt.setArticleId(10);
        attempt.setReviewerId(3);
        attempt.setCategoryId(2);
        attempt.setAttemptType(AttemptType.INITIAL);
        attempt.setSubmittedAt(null);
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(attempt));

        CriteriaSetVersion newActiveCriteria = new CriteriaSetVersion();
        newActiveCriteria.setId(2);
        newActiveCriteria.setCategoryId(4);
        newActiveCriteria.setStatus(CriteriaSetStatus.ACTIVE);
        when(criteriaSetVersionRepository.findByCategoryIdAndStatus(4, CriteriaSetStatus.ACTIVE))
                .thenReturn(Optional.of(newActiveCriteria));

        Criteria newCriterion = new Criteria();
        newCriterion.setId(102);
        newCriterion.setName("Kỹ năng phỏng vấn");
        newCriterion.setWeight(new BigDecimal("100.00"));
        when(criteriaRepository.findByCriteriaSetVersionIdOrderBySortOrderAsc(2))
                .thenReturn(List.of(newCriterion));

        when(scoringAttemptDetailRepository.findByScoringAttemptId(501)).thenReturn(List.of());
        when(scoringAttemptDetailRepository.save(any(ScoringAttemptDetail.class))).thenAnswer(i -> {
            ScoringAttemptDetail d = i.getArgument(0);
            d.setId(701);
            return d;
        });

        ScoringAttemptResponse res = scoringAttemptService.changeCategory(10, 4, 3);

        assertNotNull(res);
        assertEquals(4, res.getCategoryId());
        assertEquals(1, res.getDetails().size());
        assertEquals("Kỹ năng phỏng vấn", res.getDetails().get(0).getCriterionName());
        verify(categoryChangeLogRepository, times(1)).save(any(CategoryChangeLog.class));
        verify(articleRepository, times(1)).save(article);
    }

    @Test
    @DisplayName("Ném BusinessException khi đổi sang thể loại trùng với thể loại hiện tại")
    void testChangeCategory_SameCategory_ThrowsException() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));
        when(genreRepository.findById(2)).thenReturn(Optional.of(new Genre()));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoringAttemptService.changeCategory(10, 2, 3) // Thể loại 2 trùng thể loại hiện tại 2
        );

        assertTrue(ex.getMessage().contains("phải khác với thể loại hiện tại"));
    }


    @Test
    @DisplayName("Ném BusinessException khi cố đổi thể loại cho phiếu chấm đã nộp")
    void testChangeCategory_AlreadySubmitted_ThrowsException() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));
        when(genreRepository.findById(4)).thenReturn(Optional.of(new Genre()));

        ScoringAttempt submittedAttempt = new ScoringAttempt();
        submittedAttempt.setId(503);
        submittedAttempt.setArticleId(10);
        submittedAttempt.setCategoryId(2);
        submittedAttempt.setAttemptType(AttemptType.INITIAL);
        submittedAttempt.setSubmittedAt(LocalDateTime.now()); // Đã nộp
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(submittedAttempt));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoringAttemptService.changeCategory(10, 4, 3)
        );

        assertTrue(ex.getMessage().contains("Không thể đổi thể loại cho phiếu chấm đã nộp"));
    }

    @Test
    @DisplayName("Lưu điểm tiêu chí thành công và tính tổng điểm có trọng số (UC-24, UC-25)")
    void testSaveDraftScores_Success() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setId(504);
        attempt.setArticleId(10);
        attempt.setReviewerId(3);
        attempt.setAttemptNo(1);
        attempt.setSubmittedAt(null);
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(attempt));

        Criteria c1 = new Criteria();
        c1.setId(1);
        c1.setName("Tiêu chí 1");
        c1.setWeight(new BigDecimal("60.00"));

        Criteria c2 = new Criteria();
        c2.setId(2);
        c2.setName("Tiêu chí 2");
        c2.setWeight(new BigDecimal("40.00"));

        ScoringAttemptDetail d1 = new ScoringAttemptDetail();
        d1.setId(11);
        d1.setScoringAttempt(attempt);
        d1.setCriterion(c1);
        d1.setScore(BigDecimal.ZERO);

        ScoringAttemptDetail d2 = new ScoringAttemptDetail();
        d2.setId(12);
        d2.setScoringAttempt(attempt);
        d2.setCriterion(c2);
        d2.setScore(BigDecimal.ZERO);

        when(scoringAttemptDetailRepository.findByScoringAttemptId(504)).thenReturn(List.of(d1, d2));

        com.swp391.kpi.dto.ScoringSaveRequest req = new com.swp391.kpi.dto.ScoringSaveRequest(List.of(
                new com.swp391.kpi.dto.CriteriaScoreItemRequest(1, new BigDecimal("80.00"), null),
                new com.swp391.kpi.dto.CriteriaScoreItemRequest(2, new BigDecimal("70.00"), null)
        ));

        ScoringAttemptResponse res = scoringAttemptService.saveDraftScores(10, req, 3);

        assertNotNull(res);
        // 80*60% + 70*40% = 48 + 28 = 76.00
        assertEquals(new BigDecimal("76.00"), res.getTotalScore());
        verify(scoringAttemptDetailRepository, times(2)).save(any(ScoringAttemptDetail.class));
        verify(scoringAttemptRepository, times(1)).save(attempt);
    }

    @Test
    @DisplayName("Ném BusinessException khi điểm dưới 50 mà không có nhận xét (BR-SC-06)")
    void testSaveDraftScores_Below50WithoutComment_ThrowsException() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setId(505);
        attempt.setSubmittedAt(null);
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(attempt));

        Criteria c1 = new Criteria();
        c1.setId(1);
        c1.setName("Tính thời sự");
        c1.setWeight(new BigDecimal("100.00"));

        ScoringAttemptDetail d1 = new ScoringAttemptDetail();
        d1.setId(11);
        d1.setCriterion(c1);
        d1.setScore(BigDecimal.ZERO);

        when(scoringAttemptDetailRepository.findByScoringAttemptId(505)).thenReturn(List.of(d1));

        com.swp391.kpi.dto.ScoringSaveRequest req = new com.swp391.kpi.dto.ScoringSaveRequest(List.of(
                new com.swp391.kpi.dto.CriteriaScoreItemRequest(1, new BigDecimal("45.00"), "   ") // Rỗng
        ));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoringAttemptService.saveDraftScores(10, req, 3)
        );

        assertTrue(ex.getMessage().contains("đạt dưới 50 điểm"));
        assertTrue(ex.getMessage().contains("bắt buộc phải nhập nhận xét lý do"));
    }

    @Test
    @DisplayName("Nộp phiếu chấm với quyết định ACCEPT thành công (UC-26)")
    void testSubmitDecision_Accept_Success() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setId(506);
        attempt.setArticleId(10);
        attempt.setReviewerId(3);
        attempt.setTotalScore(new BigDecimal("75.50"));
        attempt.setSubmittedAt(null);
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(attempt));
        when(scoringAttemptRepository.save(any(ScoringAttempt.class))).thenAnswer(i -> i.getArgument(0));

        com.swp391.kpi.dto.ScoringSubmitRequest req = new com.swp391.kpi.dto.ScoringSubmitRequest(
                ScoringDecision.ACCEPT, "Bài viết đạt yêu cầu chất lượng"
        );

        ScoringAttemptResponse res = scoringAttemptService.submitDecision(10, req, 3);

        assertNotNull(res);
        assertEquals(ScoringDecision.ACCEPT, res.getDecision());
        assertEquals("Bài viết đạt yêu cầu chất lượng", res.getDecisionNote());
        assertNotNull(res.getSubmittedAt());
        verify(articleService, times(1)).updateArticleStatus(10, ArticleStatus.PENDING_APPROVAL, 3, "SUBMIT_SCORE", "Bài viết đạt yêu cầu chất lượng");
    }

    @Test
    @DisplayName("Ném BusinessException khi chọn ACCEPT mà chưa có tổng điểm (UC-26)")
    void testSubmitDecision_Accept_WithoutTotalScore_ThrowsException() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setId(507);
        attempt.setTotalScore(null); // Chưa có điểm
        attempt.setSubmittedAt(null);
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(attempt));

        com.swp391.kpi.dto.ScoringSubmitRequest req = new com.swp391.kpi.dto.ScoringSubmitRequest(
                ScoringDecision.ACCEPT, null
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoringAttemptService.submitDecision(10, req, 3)
        );

        assertTrue(ex.getMessage().contains("Vui lòng nhập và lưu điểm"));
    }

    @Test
    @DisplayName("Nộp phiếu chấm với quyết định REVISION thành công (UC-27)")
    void testSubmitDecision_Revision_Success() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setId(508);
        attempt.setSubmittedAt(null);
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(attempt));
        when(scoringAttemptRepository.save(any(ScoringAttempt.class))).thenAnswer(i -> i.getArgument(0));

        com.swp391.kpi.dto.ScoringSubmitRequest req = new com.swp391.kpi.dto.ScoringSubmitRequest(
                ScoringDecision.REVISION, "Yêu cầu bổ sung thêm số liệu hiện trường"
        );

        ScoringAttemptResponse res = scoringAttemptService.submitDecision(10, req, 3);

        assertNotNull(res);
        assertEquals(ScoringDecision.REVISION, res.getDecision());
        verify(articleService, times(1)).updateArticleStatus(10, ArticleStatus.REVISION_REQUESTED, 3, "RETURN", "Yêu cầu bổ sung thêm số liệu hiện trường");
    }

    @Test
    @DisplayName("Ném BusinessException khi chọn REVISION mà không có ghi chú nhận xét (UC-27)")
    void testSubmitDecision_Revision_MissingNote_ThrowsException() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setId(509);
        attempt.setSubmittedAt(null);
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(attempt));

        com.swp391.kpi.dto.ScoringSubmitRequest req = new com.swp391.kpi.dto.ScoringSubmitRequest(
                ScoringDecision.REVISION, ""
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoringAttemptService.submitDecision(10, req, 3)
        );

        assertTrue(ex.getMessage().contains("Yêu cầu sửa đổi (REVISION) bắt buộc phải nhập nhận xét lý do"));
    }

    @Test
    @DisplayName("Ném BusinessException khi chọn REJECT mà không có lý do từ chối (UC-28)")
    void testSubmitDecision_Reject_MissingNote_ThrowsException() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(assignmentRepository.findByArticleId(10)).thenReturn(Optional.of(assignment));

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setId(510);
        attempt.setSubmittedAt(null);
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(attempt));

        com.swp391.kpi.dto.ScoringSubmitRequest req = new com.swp391.kpi.dto.ScoringSubmitRequest(
                ScoringDecision.REJECT, null
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoringAttemptService.submitDecision(10, req, 3)
        );

        assertTrue(ex.getMessage().contains("Từ chối bài viết (REJECT) bắt buộc phải nhập lý do"));
    }

    @Test
    @DisplayName("Lấy phiếu chấm gần nhất thành công kèm thông tin BTV và thời hạn khiếu nại 7 ngày (UC-34, BR-SC-10)")
    void testGetLatestAttemptByArticle_Success_WithAnnouncedScore() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));

        ScoringAttempt attempt = new ScoringAttempt();
        attempt.setId(510);
        attempt.setArticleId(10);
        attempt.setReviewerId(3);
        attempt.setTotalScore(new BigDecimal("88.00"));
        attempt.setDecision(ScoringDecision.ACCEPT);
        attempt.setSubmittedAt(LocalDateTime.now().minusDays(1));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(attempt));

        ScoringAttemptDetail detail = new ScoringAttemptDetail();
        detail.setId(1);
        detail.setScore(new BigDecimal("90.00"));
        detail.setComment("Bài viết sắc sảo");
        detail.setCriterion(criterion1);
        when(scoringAttemptDetailRepository.findByScoringAttemptId(510)).thenReturn(List.of(detail));

        User reviewer = new User();
        reviewer.setId(3);
        reviewer.setFullName("Lê Hoàng Cường");
        when(userRepository.findById(3)).thenReturn(Optional.of(reviewer));

        ScoreApproval approval = new ScoreApproval();
        approval.setAnnouncedAt(LocalDateTime.now().minusDays(2));
        when(scoreApprovalRepository.findByScoringAttemptId(510)).thenReturn(Optional.of(approval));

        com.swp391.kpi.dto.ScoringAttemptResponse res = scoringAttemptService.getLatestAttemptByArticle(10);

        assertNotNull(res);
        assertEquals(510, res.getId());
        assertEquals("Lê Hoàng Cường", res.getReviewerName());
        assertEquals(new BigDecimal("88.00"), res.getTotalScore());
        assertNotNull(res.getAnnouncedAt());
        assertTrue(res.getIsAppealWindowOpen());
        assertEquals(4, res.getDaysRemaining());
        assertEquals(1, res.getDetails().size());
        assertEquals("Tính thời sự", res.getDetails().get(0).getCriterionName());
    }

    @Test
    @DisplayName("Ném NotFoundException khi bài viết chưa có phiếu chấm nào (UC-34)")
    void testGetLatestAttemptByArticle_NotFound_ThrowsException() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(article));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                scoringAttemptService.getLatestAttemptByArticle(10)
        );
    }
}
