package com.swp391.kpi.service;

import com.swp391.kpi.dto.AssignmentCreateRequest;
import com.swp391.kpi.dto.AssignmentResponse;
import com.swp391.kpi.dto.EditorWorkloadDTO;
import com.swp391.kpi.dto.PendingAssignmentResponse;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.Assignment;
import com.swp391.kpi.entity.Genre;
import com.swp391.kpi.entity.User;
import com.swp391.kpi.repository.ArticleRepository;
import com.swp391.kpi.repository.AssignmentRepository;
import com.swp391.kpi.repository.GenreRepository;
import com.swp391.kpi.repository.ScoringAttemptRepository;
import com.swp391.kpi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test cho AssignmentService (Phân công bài viết UC-18..20)")
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ArticleService articleService;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private ScoringAttemptRepository scoringAttemptRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    @DisplayName("getPendingAssignments: Lấy danh sách bài chờ phân công theo ban")
    void testGetPendingAssignments_Success() {
        Article article = new Article();
        article.setId(101);
        article.setTitle("Bài viết kiểm thử");
        article.setAuthorId(1);
        article.setCategoryId(2);
        article.setSectionId(1);
        article.setStatus(ArticleStatus.SUBMITTED);
        article.setSubmittedAt(LocalDateTime.now());

        User author = new User();
        author.setId(1);
        author.setFullName("Nguyễn Văn An");

        Genre genre = new Genre();
        genre.setId(2);
        genre.setName("Tin sâu");

        when(articleRepository.findByStatusAndSectionIdOrderByUpdatedAtDesc(ArticleStatus.SUBMITTED, 1))
                .thenReturn(List.of(article));
        when(userRepository.findById(1)).thenReturn(Optional.of(author));
        when(genreRepository.findById(2)).thenReturn(Optional.of(genre));

        List<PendingAssignmentResponse> result = assignmentService.getPendingAssignments(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(101, result.get(0).getId());
        assertEquals("Bài viết kiểm thử", result.get(0).getTitle());
        assertEquals("Nguyễn Văn An", result.get(0).getAuthor());
        assertEquals("Tin sâu", result.get(0).getCategory());
    }

    @Test
    @DisplayName("assignArticle: Phân công bài viết thành công")
    void testAssignArticle_Success() {
        AssignmentCreateRequest req = new AssignmentCreateRequest();
        req.setArticleId(101);
        req.setEditorId(3);
        req.setAssignedBy(5);
        req.setWasSuggested(false);

        Assignment assignment = new Assignment();
        assignment.setId(1);
        assignment.setArticleId(101);
        assignment.setEditorId(3);
        assignment.setAssignedBy(5);
        assignment.setAssignedAt(LocalDateTime.now());

        when(assignmentRepository.findByArticleId(101)).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(assignment);

        AssignmentResponse res = assignmentService.assignArticle(req);

        assertNotNull(res);
        assertEquals(101, res.getArticleId());
        assertEquals(3, res.getEditorId());
        verify(articleService).updateArticleStatus(eq(101), eq(ArticleStatus.ASSIGNED), eq(5), eq("ASSIGN"), anyString());
    }

    @Test
    @DisplayName("getEditorWorkload: Lấy số lượng bài đang phụ trách của BTV")
    void testGetEditorWorkload_Success() {
        when(assignmentRepository.countAssignedArticlesByEditorId(3)).thenReturn(5L);

        EditorWorkloadDTO workload = assignmentService.getEditorWorkload(3);

        assertNotNull(workload);
        assertEquals(3, workload.getEditorId());
        assertEquals(5L, workload.getActiveArticleCount());
    }

    @Test
    @DisplayName("getAssignmentsByEditorId: Lấy danh sách kèm trạng thái đã chấm/chưa chấm")
    void testGetAssignmentsByEditorId_Success() {
        Assignment assignment = new Assignment();
        assignment.setId(1);
        assignment.setArticleId(101);
        assignment.setEditorId(3);
        assignment.setAssignedAt(LocalDateTime.now());

        Article article = new Article();
        article.setId(101);
        article.setTitle("Bài viết số 101");
        article.setStatus(ArticleStatus.ASSIGNED);
        article.setCategoryId(2);

        Genre genre = new Genre();
        genre.setId(2);
        genre.setName("Tin sâu");

        when(assignmentRepository.findByEditorIdOrderByAssignedAtDesc(3)).thenReturn(List.of(assignment));
        when(articleRepository.findById(101)).thenReturn(Optional.of(article));
        when(genreRepository.findById(2)).thenReturn(Optional.of(genre));
        when(scoringAttemptRepository.findTopByArticleIdOrderByAttemptNoDesc(101)).thenReturn(Optional.empty());

        List<AssignmentResponse> results = assignmentService.getAssignmentsByEditorId(3);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Bài viết số 101", results.get(0).getArticleTitle());
        assertEquals("Tin sâu", results.get(0).getCategoryName());
        assertEquals("CHUA_CHAM", results.get(0).getScoringStatus());
    }
}

