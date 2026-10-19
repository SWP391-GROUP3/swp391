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
}
