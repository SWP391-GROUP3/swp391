package com.swp391.kpi.service;

import com.swp391.kpi.dto.AssignmentCreateRequest;
import com.swp391.kpi.dto.AssignmentResponse;
import com.swp391.kpi.dto.EditorWorkloadDTO;
import com.swp391.kpi.dto.PendingAssignmentResponse;
import com.swp391.kpi.dto.UserAccessResponse;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.Assignment;
import com.swp391.kpi.entity.Genre;
import com.swp391.kpi.entity.User;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ArticleRepository;
import com.swp391.kpi.repository.AssignmentRepository;
import com.swp391.kpi.repository.GenreRepository;
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
 *   48  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   60  getPendingAssignments()         Lay danh sach bai viet SUBMITTED cho Truong ban phan cong (UC-18)
 *   95  assignArticle()                 Truong ban phan cong mot bai viet cho mot BTV cham chat luong (UC-18, UC-20, BR-SC-04)
 *  143  getAssignmentByArticleId()      Lay thong tin phan cong hien tai cua bai viet
 *  152  getAssignmentsByEditorId()      Lay danh sach cac bai viet duoc phan cong cho 1 BTV (Hang viec cua BTV - UC-21)
 *  163  getEditorWorkload()             Thong ke khoi luong cong viec hien tai cua mot BTV de phuc vu goi y phan cong (UC-19)
 *  ---- Ham phu (private, chi dung trong file) ----
 *  169  checkEditorOfSection()          UC-20 E2: nguoi duoc giao phai la BTV dang ACTIVE cung ban voi Truong ban
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final UserReadService userReadService;   // service luong admin: kiem tra vai tro, lay ban cua Truong ban

    /**
     * Lấy danh sách bài viết SUBMITTED cho Trưởng ban phân công (UC-18).
     * M-A3: ban lay tu tai khoan Truong ban dang dang nhap (users.section_id), khong lay tu query string.
     */
    @Transactional(readOnly = true)
    public List<PendingAssignmentResponse> getPendingAssignments(Integer currentUserId) {
        UserAccessResponse head = userReadService.requireActive(currentUserId, "HEAD_OF_DEPARTMENT");
        List<Article> articles = articleRepository.findByStatusAndSectionIdOrderByUpdatedAtDesc(
                ArticleStatus.SUBMITTED, head.sectionId());

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
     * Trưởng ban phân công một bài viết cho một BTV chấm chất lượng (UC-18, UC-20, BR-SC-04).
     * Ràng buộc: Mỗi bài viết tại 1 thời điểm chỉ giao cho 1 BTV chính.
     * Nếu bài đã được phân công trước đó, cập nhật lại phân công (Reassign).
     */
    @Transactional
    public AssignmentResponse assignArticle(AssignmentCreateRequest request, Integer currentUserId) {
        // M-A3: nguoi giao bai la Truong ban dang dang nhap (lay tu token)
        UserAccessResponse head = userReadService.requireActive(currentUserId, "HEAD_OF_DEPARTMENT");
        if (request.getArticleId() == null) {
            throw new BusinessException("Mã bài viết không được để trống");
        }
        if (request.getEditorId() == null) {
            throw new BusinessException("Vui lòng chọn Biên tập viên để giao bài");
        }
        // M-A4: UC-20 chi nhan bai dang SUBMITTED cua ban minh (E1: bai da rut hoac da phan cong)
        Article article = articleRepository.findById(request.getArticleId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết."));
        if (article.getStatus() != ArticleStatus.SUBMITTED) {
            throw new BusinessException("Bài viết không còn chờ phân công.");
        }
        if (!article.getSectionId().equals(head.sectionId())) {
            throw new BusinessException("Bài viết không thuộc ban của bạn.");
        }
        // UC-20 E2: chi giao cho BTV dang hoat dong cua ban
        checkEditorOfSection(request.getEditorId(), head.sectionId());

        // Cập nhật trạng thái bài viết sang ASSIGNED và ghi log lịch sử (UC-20)
        articleService.updateArticleStatus(
                request.getArticleId(),
                ArticleStatus.ASSIGNED,
                currentUserId,
                "ASSIGN",
                "Phân công cho Biên tập viên #" + request.getEditorId()
        );

        // Kiểm tra xem bài đã có phân công chưa (UC-20)
        Assignment assignment = assignmentRepository.findByArticleId(request.getArticleId())
                .orElse(new Assignment());

        assignment.setArticleId(request.getArticleId());
        assignment.setEditorId(request.getEditorId());
        assignment.setAssignedBy(currentUserId);
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
     */
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByEditorId(Integer editorId) {
        return assignmentRepository.findByEditorIdOrderByAssignedAtDesc(editorId)
                .stream()
                .map(AssignmentResponse::from)
                .toList();
    }

    /**
     * Thống kê khối lượng công việc hiện tại của một BTV để phục vụ gợi ý phân công (UC-19).
     */
    @Transactional(readOnly = true)
    public EditorWorkloadDTO getEditorWorkload(Integer editorId) {
        long count = assignmentRepository.countAssignedArticlesByEditorId(editorId);
        return new EditorWorkloadDTO(editorId, count);
    }

    // UC-20 E2: nguoi duoc giao phai la BTV dang ACTIVE cung ban voi Truong ban
    private void checkEditorOfSection(Integer editorId, Integer sectionId) {
        for (UserAccessResponse user : userReadService.getUsers()) {
            if (user.id().equals(editorId)) {
                if (!"EDITOR".equals(user.role()) || !sectionId.equals(user.sectionId())) {
                    throw new BusinessException("Người được chọn không phải Biên tập viên của ban.");
                }
                if (!"ACTIVE".equals(user.status())) {
                    throw new BusinessException("Tài khoản Biên tập viên đang bị khóa, không giao bài được.");
                }
                return;
            }
        }
        throw new BusinessException("Không tìm thấy Biên tập viên đã chọn.");
    }
}
