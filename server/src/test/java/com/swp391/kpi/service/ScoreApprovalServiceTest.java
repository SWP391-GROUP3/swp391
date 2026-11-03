package com.swp391.kpi.service;

import com.swp391.kpi.dto.GenreResponse;
import com.swp391.kpi.dto.PublishingItemResponse;
import com.swp391.kpi.dto.PublishingResponse;
import com.swp391.kpi.dto.ScoreApprovalDecisionRequest;
import com.swp391.kpi.dto.ScoreApprovalItemResponse;
import com.swp391.kpi.dto.ScoreApprovalResponse;
import com.swp391.kpi.dto.UserAccessResponse;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ArticleRepository;
import com.swp391.kpi.repository.ScoreApprovalRepository;
import com.swp391.kpi.repository.ScoringAttemptRepository;
import com.swp391.kpi.repository.UserRepository;
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
@DisplayName("Unit Test cho ScoreApprovalService (Trưởng ban duyệt điểm & Duyệt xuất bản UC-29..33)")
class ScoreApprovalServiceTest {

    @Mock
    private ScoreApprovalRepository scoreApprovalRepository;

    @Mock
    private ScoringAttemptRepository scoringAttemptRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ArticleService articleService;

    @Mock
    private ScoringAttemptService scoringAttemptService;

    @Mock
    private GenreService genreService;

    @Mock
    private AppealRuleConfigService appealRuleConfigService;

    @Mock
    private AppealEffectiveScoreService appealEffectiveScoreService;

    @Mock
    private UserReadService userReadService;

    @InjectMocks
    private ScoreApprovalService scoreApprovalService;

    private Article mockArticle;
    private ScoringAttempt mockAttempt;
    private User mockReviewer;

    @BeforeEach
    void setUp() {
        mockArticle = new Article();
        mockArticle.setId(10);
        mockArticle.setTitle("Bài viết phân tích chuyển đổi số");
        mockArticle.setStatus(ArticleStatus.PENDING_APPROVAL);
        mockArticle.setSectionId(1);

        mockAttempt = new ScoringAttempt();
        mockAttempt.setId(100);
        mockAttempt.setArticleId(10);
        mockAttempt.setAttemptNo(1);
        mockAttempt.setAttemptType(AttemptType.INITIAL);
        mockAttempt.setReviewerId(3);
        mockAttempt.setCategoryId(1);
        mockAttempt.setTotalScore(new BigDecimal("85.50"));
        mockAttempt.setDecision(ScoringDecision.ACCEPT);
        mockAttempt.setSubmittedAt(LocalDateTime.now().minusHours(1));

        mockReviewer = new User();
        mockReviewer.setId(3);
        mockReviewer.setFullName("Lê Hoàng Cường");

        // Truong ban (id 2 trong test) thuoc ban 1
        lenient().when(userReadService.requireActive(2, "HEAD_OF_DEPARTMENT"))
                .thenReturn(new UserAccessResponse(2, "Trưởng ban", "HEAD_OF_DEPARTMENT", 1, "Ban 1", "ACTIVE"));
    }

    @Test
    @DisplayName("Lấy danh sách chờ duyệt điểm thành công (UC-29)")
    void getPendingApprovals_Success() {
        when(articleRepository.findByStatusAndSectionIdOrderByUpdatedAtDesc(ArticleStatus.PENDING_APPROVAL, 1))
                .thenReturn(List.of(mockArticle));
        when(scoringAttemptRepository.findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc(10, AttemptType.APPEAL))
                .thenReturn(Optional.of(mockAttempt));
        when(userRepository.findById(3))
                .thenReturn(Optional.of(mockReviewer));

        List<ScoreApprovalItemResponse> result = scoreApprovalService.getPendingApprovals(2);

        assertEquals(1, result.size());
        assertEquals("Bài viết phân tích chuyển đổi số", result.get(0).getArticleTitle());
        assertEquals("Lê Hoàng Cường", result.get(0).getReviewerName());
        assertEquals(new BigDecimal("85.50"), result.get(0).getTotalScore());
    }

    @Test
    @DisplayName("Duyệt điểm thành công (UC-31, BR-SC-10)")
    void approveScore_Success() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(mockArticle));
        when(scoringAttemptRepository.findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc(10, AttemptType.APPEAL)).thenReturn(Optional.of(mockAttempt));
        when(scoreApprovalRepository.findByScoringAttemptId(100)).thenReturn(Optional.empty());

        ScoreApproval savedApproval = new ScoreApproval();
        savedApproval.setId(1);
        savedApproval.setScoringAttempt(mockAttempt);
        savedApproval.setDecision(ApprovalDecision.APPROVED);
        savedApproval.setDecidedBy(2);
        savedApproval.setDecidedAt(LocalDateTime.now());
        savedApproval.setAnnouncedAt(LocalDateTime.now());

        when(scoreApprovalRepository.save(any(ScoreApproval.class))).thenReturn(savedApproval);

        ScoreApprovalResponse response = scoreApprovalService.approveScore(10, 2);

        assertNotNull(response);
        assertEquals(ApprovalDecision.APPROVED, response.getDecision());
        assertNotNull(response.getAnnouncedAt());

        verify(articleService).updateArticleStatus(
                eq(10),
                eq(ArticleStatus.SCORE_PUBLISHED),
                eq(2),
                eq("APPROVE_SCORE"),
                anyString()
        );
    }

    @Test
    @DisplayName("Duyệt điểm thất bại nếu bài viết không ở trạng thái PENDING_APPROVAL")
    void approveScore_WrongStatus_ThrowsException() {
        mockArticle.setStatus(ArticleStatus.ASSIGNED);
        when(articleRepository.findById(10)).thenReturn(Optional.of(mockArticle));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoreApprovalService.approveScore(10, 2)
        );

        assertTrue(ex.getMessage().contains("Chờ duyệt điểm"));
        verify(scoreApprovalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duyệt điểm thất bại nếu phiếu chấm chưa nộp (submittedAt == null)")
    void approveScore_NotSubmitted_ThrowsException() {
        mockAttempt.setSubmittedAt(null);
        when(articleRepository.findById(10)).thenReturn(Optional.of(mockArticle));
        when(scoringAttemptRepository.findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc(10, AttemptType.APPEAL)).thenReturn(Optional.of(mockAttempt));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoreApprovalService.approveScore(10, 2)
        );

        assertTrue(ex.getMessage().contains("chưa hoàn tất nộp"));
    }

    @Test
    @DisplayName("Yêu cầu chấm lại thành công (UC-32, BR-SC-10)")
    void returnScore_Success() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(mockArticle));
        when(scoringAttemptRepository.findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc(10, AttemptType.APPEAL)).thenReturn(Optional.of(mockAttempt));
        when(scoreApprovalRepository.findByScoringAttemptId(100)).thenReturn(Optional.empty());

        ScoreApproval savedApproval = new ScoreApproval();
        savedApproval.setId(2);
        savedApproval.setScoringAttempt(mockAttempt);
        savedApproval.setDecision(ApprovalDecision.RETURNED);
        savedApproval.setReason("Tiêu chí hình ảnh cho điểm quá cao so với chất lượng thực tế");
        savedApproval.setDecidedBy(2);
        savedApproval.setDecidedAt(LocalDateTime.now());
        savedApproval.setAnnouncedAt(null);

        when(scoreApprovalRepository.save(any(ScoreApproval.class))).thenReturn(savedApproval);

        ScoreApprovalDecisionRequest request = new ScoreApprovalDecisionRequest("  Tiêu chí hình ảnh cho điểm quá cao so với chất lượng thực tế  ");
        ScoreApprovalResponse response = scoreApprovalService.returnScore(10, request, 2);

        assertNotNull(response);
        assertEquals(ApprovalDecision.RETURNED, response.getDecision());
        assertEquals("Tiêu chí hình ảnh cho điểm quá cao so với chất lượng thực tế", response.getReason());
        assertNull(response.getAnnouncedAt());

        // Kiểm tra chuyển trạng thái bài về ASSIGNED
        verify(articleService).updateArticleStatus(
                eq(10),
                eq(ArticleStatus.ASSIGNED),
                eq(2),
                eq("RETURN_SCORE"),
                eq("Tiêu chí hình ảnh cho điểm quá cao so với chất lượng thực tế")
        );

        // Kiểm tra tự động mở lần chấm tiếp theo cho BTV đó
        verify(scoringAttemptService).openScoringAttempt(10, 3);
    }

    @Test
    @DisplayName("Yêu cầu chấm lại thất bại nếu không nhập lý do (BR-SC-10, UC-32 Exception E2)")
    void returnScore_EmptyReason_ThrowsException() {
        ScoreApprovalDecisionRequest emptyRequest = new ScoreApprovalDecisionRequest("   ");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoreApprovalService.returnScore(10, emptyRequest, 2)
        );

        assertTrue(ex.getMessage().contains("Vui lòng nhập lý do"));
        verify(scoreApprovalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy danh sách chờ duyệt xuất bản bài viết thành công (UC-33)")
    void getPendingPublishing_Success() {
        Article publishedScoreArticle = new Article();
        publishedScoreArticle.setId(20);
        publishedScoreArticle.setTitle("Bài viết điều tra tiêu điểm");
        publishedScoreArticle.setStatus(ArticleStatus.SCORE_PUBLISHED);
        publishedScoreArticle.setAuthorId(5);
        publishedScoreArticle.setCategoryId(1);
        publishedScoreArticle.setSectionId(1);
        publishedScoreArticle.setUpdatedAt(LocalDateTime.now());

        User mockAuthor = new User();
        mockAuthor.setId(5);
        mockAuthor.setFullName("Nguyễn Văn Tác Giả");

        ScoreApproval mockApproval = new ScoreApproval();
        mockApproval.setAnnouncedAt(LocalDateTime.now().minusDays(2));

        GenreResponse mockGenre = new GenreResponse();
        mockGenre.setId(1);
        mockGenre.setCode("PHONG_SU");
        mockGenre.setName("Phóng sự");

        when(articleRepository.findByStatusAndSectionIdOrderByUpdatedAtDesc(ArticleStatus.SCORE_PUBLISHED, 1))
                .thenReturn(List.of(publishedScoreArticle));
        when(genreService.getActiveGenres())
                .thenReturn(List.of(mockGenre));
        when(scoringAttemptRepository.findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc(20, AttemptType.APPEAL))
                .thenReturn(Optional.of(mockAttempt));
        when(userRepository.findById(3))
                .thenReturn(Optional.of(mockReviewer));
        when(userRepository.findById(5))
                .thenReturn(Optional.of(mockAuthor));
        when(scoreApprovalRepository.findByScoringAttemptId(mockAttempt.getId()))
                .thenReturn(Optional.of(mockApproval));

        when(appealRuleConfigService.getAppealDeadline(any())).thenAnswer(inv -> ((LocalDateTime) inv.getArgument(0)).plusDays(7));
        when(appealEffectiveScoreService.getEffectiveScore(20, mockAttempt.getTotalScore())).thenReturn(mockAttempt.getTotalScore());
        List<PublishingItemResponse> result = scoreApprovalService.getPendingPublishing(2);

        assertEquals(1, result.size());
        PublishingItemResponse item = result.get(0);
        assertEquals(20, item.getArticleId());
        assertEquals("Bài viết điều tra tiêu điểm", item.getTitle());
        assertEquals("Nguyễn Văn Tác Giả", item.getAuthorName());
        assertEquals("Phóng sự", item.getCategoryName());
        assertEquals("Lê Hoàng Cường", item.getReviewerName());
        assertTrue(item.getIsAppealWindowOpen());
        assertTrue(item.getDaysRemaining() == 4 || item.getDaysRemaining() == 5);
    }

    @Test
    @DisplayName("Trưởng ban duyệt xuất bản bài viết thành công (UC-33, BR-SC-11)")
    void approvePublishing_Success() {
        Article article = new Article();
        article.setId(20);
        article.setSectionId(1);
        article.setStatus(ArticleStatus.SCORE_PUBLISHED);

        when(articleRepository.findById(20)).thenReturn(Optional.of(article));

        PublishingResponse response = scoreApprovalService.approvePublishing(20, 2);

        assertNotNull(response);
        assertEquals(20, response.getArticleId());
        assertEquals("PUBLISHED", response.getStatus());
        assertEquals(2, response.getApprovedBy());
        assertNotNull(response.getPublishedAt());

        // Đảm bảo gọi hàm publishArticle của ArticleService
        verify(articleService).publishArticle(20, 2);
    }

    @Test
    @DisplayName("Duyệt xuất bản thất bại nếu bài viết chưa ở trạng thái SCORE_PUBLISHED (UC-33 E1)")
    void approvePublishing_WrongStatus_ThrowsException() {
        Article article = new Article();
        article.setId(20);
        article.setSectionId(1);
        article.setStatus(ArticleStatus.PENDING_APPROVAL);

        when(articleRepository.findById(20)).thenReturn(Optional.of(article));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoreApprovalService.approvePublishing(20, 2)
        );

        assertTrue(ex.getMessage().contains("SCORE_PUBLISHED"));
        verify(articleService, never()).publishArticle(any(), any());
    }

    @Test
    @DisplayName("Duyệt xuất bản thất bại nếu không tìm thấy bài viết (NotFoundException)")
    void approvePublishing_NotFound_ThrowsException() {
        when(articleRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                scoreApprovalService.approvePublishing(999, 2)
        );
        verify(articleService, never()).publishArticle(any(), any());
    }
}
