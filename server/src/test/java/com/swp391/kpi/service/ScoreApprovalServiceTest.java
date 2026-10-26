package com.swp391.kpi.service;

import com.swp391.kpi.dto.ScoreApprovalDecisionRequest;
import com.swp391.kpi.dto.ScoreApprovalItemResponse;
import com.swp391.kpi.dto.ScoreApprovalResponse;
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
@DisplayName("Unit Test cho ScoreApprovalService (Task 2.4 Part 2: Trưởng ban duyệt điểm & Trả về)")
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
    }

    @Test
    @DisplayName("Lấy danh sách chờ duyệt điểm thành công (UC-29)")
    void getPendingApprovals_Success() {
        when(articleRepository.findByStatusOrderByUpdatedAtDesc(ArticleStatus.PENDING_APPROVAL))
                .thenReturn(List.of(mockArticle));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10))
                .thenReturn(Optional.of(mockAttempt));
        when(userRepository.findById(3))
                .thenReturn(Optional.of(mockReviewer));

        List<ScoreApprovalItemResponse> result = scoreApprovalService.getPendingApprovals(null);

        assertEquals(1, result.size());
        assertEquals("Bài viết phân tích chuyển đổi số", result.get(0).getArticleTitle());
        assertEquals("Lê Hoàng Cường", result.get(0).getReviewerName());
        assertEquals(new BigDecimal("85.50"), result.get(0).getTotalScore());
    }

    @Test
    @DisplayName("Duyệt điểm thành công (UC-32, BR-SC-10)")
    void approveScore_Success() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(mockArticle));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(mockAttempt));
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
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(mockAttempt));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoreApprovalService.approveScore(10, 2)
        );

        assertTrue(ex.getMessage().contains("chưa hoàn tất nộp"));
    }

    @Test
    @DisplayName("Yêu cầu chấm lại thành công (UC-31, BR-SC-10)")
    void returnScore_Success() {
        when(articleRepository.findById(10)).thenReturn(Optional.of(mockArticle));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(10)).thenReturn(Optional.of(mockAttempt));
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
    @DisplayName("Yêu cầu chấm lại thất bại nếu không nhập lý do (BR-SC-10, UC-31 Exception E2)")
    void returnScore_EmptyReason_ThrowsException() {
        ScoreApprovalDecisionRequest emptyRequest = new ScoreApprovalDecisionRequest("   ");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                scoreApprovalService.returnScore(10, emptyRequest, 2)
        );

        assertTrue(ex.getMessage().contains("Vui lòng nhập lý do"));
        verify(scoreApprovalRepository, never()).save(any());
    }
}
