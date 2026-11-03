package com.swp391.kpi.appeal.service;

import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.appeal.dto.ReviewerDeviationResponse;
import com.swp391.kpi.appeal.entity.ReviewerDeviationIndex;
import com.swp391.kpi.appeal.repository.ReviewerDeviationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Doc bao cao do lech nguoi cham (UC-41) cho man Truong ban.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   29  field                           repository / service duoc inject
 *   36  getReport()                     Chi doc bang chi so da luu: khong tinh lai, khong khoa he thong
 *   84  recalculate()                   Tinh lai chi so 1 ky (hoac moi ky neu period null) roi luu; chi Admin / Truong ban
 *      recalculate()                   tinh lai va luu chi so
 */
@Service
@RequiredArgsConstructor
public class ReviewerDeviationService {

    private final ReviewerDeviationRepository reviewerDeviationRepository;
    private final ReviewerDirectoryProvider reviewerDirectoryProvider;
    private final UserReadService userReadService;
    private final ReviewerDeviationRefreshService refreshService;

    // Chi doc bang chi so da luu: khong tinh lai, khong khoa he thong. Muon tinh lai thi goi recalculate().
    @Transactional(readOnly = true)
    public List<ReviewerDeviationResponse> getReport(
            LocalDate period,
            Integer editorId,
            Integer sectionId,
            Integer currentUserId
    ) {
        var user = userReadService.requireActive(currentUserId, "ADMIN", "HEAD_OF_DEPARTMENT");
        if (period != null && period.getDayOfMonth() != 1)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Kỳ báo cáo phải là ngày đầu tháng.");
        if ("HEAD_OF_DEPARTMENT".equals(user.role())) {
            if (user.sectionId() == null || (sectionId != null && !sectionId.equals(user.sectionId())))
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Bạn không có quyền xem báo cáo của ban này.");
            sectionId = user.sectionId();
        }
        List<ReviewerDeviationIndex> indices =
                reviewerDeviationRepository.findAllByOrderByPeriodDescEditorIdAsc();
        List<ReviewerDeviationResponse> result = new ArrayList<>();

        for (ReviewerDeviationIndex index : indices) {
            if (period != null && !period.equals(index.getPeriod())) {
                continue;
            }
            if (editorId != null && !editorId.equals(index.getEditorId())) {
                continue;
            }

            Optional<ReviewerDirectoryEntry> editor = reviewerDirectoryProvider.findEditor(index.getEditorId());
            if (editor.isEmpty()) {
                continue;
            }
            if (sectionId != null && !sectionId.equals(editor.get().getSectionId())) {
                continue;
            }

            result.add(ReviewerDeviationResponse.from(
                    index,
                    editor.get().getEditorName(),
                    editor.get().getSectionId(),
                    editor.get().getSectionName()
            ));
        }
        return result;
    }

    // Tinh lai chi so 1 ky (hoac moi ky neu period null) roi luu; chi Admin / Truong ban.
    @Transactional
    public void recalculate(LocalDate period, Integer currentUserId) {
        java.util.Set<LocalDate> periods = period == null ? refreshService.getPeriods() : java.util.Set.of(period);
        for (LocalDate month : periods) refreshService.refresh(month, currentUserId);
    }
}
