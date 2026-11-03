package com.swp391.kpi.service;

import com.swp391.kpi.dto.AssignmentCreateRequest;
import com.swp391.kpi.dto.AssignmentResponse;
import com.swp391.kpi.dto.EditorWorkloadDTO;
import com.swp391.kpi.dto.PendingAssignmentResponse;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.Assignment;
import com.swp391.kpi.entity.Genre;
import com.swp391.kpi.entity.ScoringAttempt;
import com.swp391.kpi.entity.User;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.ArticleRepository;
import com.swp391.kpi.repository.AssignmentRepository;
import com.swp391.kpi.repository.GenreRepository;
import com.swp391.kpi.repository.ScoringAttemptRepository;
import com.swp391.kpi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Nghiep vu phan cong bai cho BTV (UC-18...20): giao bai, tra phan cong theo bai / theo BTV, tinh khoi
 * luong viec de goi y BTV.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   33  field                           repository / service duoc inject
 *   41  assignArticle()                 Truong ban phan cong mot bai viet cho mot BTV cham chat luong (UC-18, UC-20, BR-QA-04)
 *   70  getAssignmentByArticleId()      Lay thong tin phan cong hien tai cua bai viet
 *   79  getAssignmentsByEditorId()      Lay danh sach cac bai viet duoc phan cong cho 1 BTV (Hang viec cua BTV - UC-21)
 *   90  getEditorWorkload()             Thong ke khoi luong cong viec hien tai cua mot BTV de phuc vu goi y phan cong (UC-19)
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final ScoringAttemptRepository scoringAttemptRepository;

    /**
     * Lấy danh sách bài viết SUBMITTED cho Trưởng ban phân công (UC-18).
     */
    @Transactional(readOnly = true)
    public List<PendingAssignmentResponse> getPendingAssignments(Integer sectionId) {
        List<Article> articles = (sectionId != null)
                ? articleRepository.findByStatusAndSectionIdOrderByUpdatedAtDesc(ArticleStatus.SUBMITTED, sectionId)
                : articleRepository.findByStatusOrderByUpdatedAtDesc(ArticleStatus.SUBMITTED);

        List<PendingAssignmentResponse> result = new ArrayList<>();
        for (Article art : articles) {
            String authorName = (art.getAuthorId() != null)
                    ? userRepository.findById(art.getAuthorId()).map(User::getFullName).orElse("Tác giả #" + art.getAuthorId())
                    : "Chưa rõ";

            Integer catId = art.getCategoryId() != null ? art.getCategoryId() : art.getDeclaredCategoryId();
            String catName = (catId != null)
                    ? genreRepository.findById(catId).map(Genre::getName).orElse("Thể loại #" + catId)
                    : "Chưa phân loại";

            result.add(new PendingAssignmentResponse(
                    art.getId(),
                    art.getTitle(),
                    authorName,
                    catName,
                    catId,
                    art.getSubmittedAt(),
                    art.getSectionId()
            ));
        }
        return result;
    }

    /**
     * Trưởng ban phân công một bài viết cho một BTV chấm chất lượng (UC-18, UC-20, BR-QA-04).
     * Ràng buộc: Mỗi bài viết tại 1 thời điểm chỉ giao cho 1 BTV chính.
     * Nếu bài đã được phân công trước đó, cập nhật lại phân công (Reassign).
     */
    @Transactional
    public AssignmentResponse assignArticle(AssignmentCreateRequest request) {
        if (request.getArticleId() == null) {
            throw new BusinessException("Mã bài viết không được để trống");
        }
        if (request.getEditorId() == null) {
            throw new BusinessException("Vui lòng chọn Biên tập viên để giao bài");
        }
        if (request.getAssignedBy() == null) {
            throw new BusinessException("Mã người giao bài không được để trống");
        }

        // Cập nhật trạng thái bài viết sang ASSIGNED và ghi log lịch sử (UC-20)
        articleService.updateArticleStatus(
                request.getArticleId(),
                ArticleStatus.ASSIGNED,
                request.getAssignedBy(),
                "ASSIGN",
                "Phân công cho Biên tập viên #" + request.getEditorId()
        );

        // Kiểm tra xem bài đã có phân công chưa (BR-QA-04)
        Assignment assignment = assignmentRepository.findByArticleId(request.getArticleId())
                .orElse(new Assignment());

        assignment.setArticleId(request.getArticleId());
        assignment.setEditorId(request.getEditorId());
        assignment.setAssignedBy(request.getAssignedBy());
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setWasSuggested(Boolean.TRUE.equals(request.getWasSuggested()));

        Assignment saved = assignmentRepository.save(assignment);
        return AssignmentResponse.from(saved);
    }

    /**
     * Lấy thông tin phân công hiện tại của bài viết.
     */
    @Transactional(readOnly = true)
    public Optional<AssignmentResponse> getAssignmentByArticleId(Integer articleId) {
        return assignmentRepository.findByArticleId(articleId)
                .map(AssignmentResponse::from);
    }

    /**
     * Lấy danh sách các bài viết được phân công cho 1 BTV (Hàng việc của BTV - UC-21).
     * Bổ sung thông tin tiêu đề, thể loại và trạng thái đã chấm / chưa chấm để BTV xem trước.
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByEditorId(Integer editorId) {
        List<Assignment> assignments = assignmentRepository.findByEditorIdOrderByAssignedAtDesc(editorId);
        List<AssignmentResponse> result = new ArrayList<>();

        for (Assignment a : assignments) {
            AssignmentResponse dto = AssignmentResponse.from(a);

            // Lay thong tin bai viet
            if (a.getArticleId() != null) {
                articleRepository.findById(a.getArticleId()).ifPresent(art -> {
                    dto.setArticleTitle(art.getTitle());
                    if (art.getStatus() != null) {
                        dto.setArticleStatus(art.getStatus().name());
                    }
                    Integer catId = art.getCategoryId() != null ? art.getCategoryId() : art.getDeclaredCategoryId();
                    if (catId != null) {
                        genreRepository.findById(catId).ifPresent(g -> dto.setCategoryName(g.getName()));
                    }
                });

                // Lay thong tin lan cham gan nhat de xac dinh da cham / chua cham
                Optional<ScoringAttempt> latestAttempt = scoringAttemptRepository
                        .findTopByArticleIdOrderByAttemptNoDesc(a.getArticleId());

                if (latestAttempt.isPresent()) {
                    ScoringAttempt sa = latestAttempt.get();
                    if (sa.getSubmittedAt() != null) {
                        dto.setScoringStatus("DA_CHAM");
                        dto.setTotalScore(sa.getTotalScore());
                        if (sa.getDecision() != null) {
                            dto.setDecision(sa.getDecision().name());
                        }
                    } else {
                        dto.setScoringStatus("DANG_CHAM");
                        dto.setTotalScore(sa.getTotalScore());
                    }
                } else {
                    dto.setScoringStatus("CHUA_CHAM");
                }
            } else {
                dto.setScoringStatus("CHUA_CHAM");
            }

            result.add(dto);
        }

        return result;
    }

    /**
     * Thống kê khối lượng công việc hiện tại của một BTV để phục vụ gợi ý phân công (UC-19).
     */
    @Transactional(readOnly = true)
    public EditorWorkloadDTO getEditorWorkload(Integer editorId) {
        long count = assignmentRepository.countAssignedArticlesByEditorId(editorId);
        return new EditorWorkloadDTO(editorId, count);
    }
}
