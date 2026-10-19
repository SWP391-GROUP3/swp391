package com.swp391.kpi.service;

import com.swp391.kpi.dto.ReviewerDeviationResponse;
import com.swp391.kpi.entity.ReviewerDeviationIndex;
import com.swp391.kpi.repository.ReviewerDeviationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MUC LUC (so dong)
 *   24  field                        - repository / service duoc inject
 *   30  getReport()
 */
@Service
@RequiredArgsConstructor
public class ReviewerDeviationService {

    private final ReviewerDeviationRepository reviewerDeviationRepository;
    private final ReviewerDirectoryProvider reviewerDirectoryProvider;
    private final UserReadService userReadService;
    private final ReviewerDeviationRefreshService refreshService;

    @Transactional
    public List<ReviewerDeviationResponse> getReport(
            LocalDate period,
            Integer editorId,
            Integer sectionId,
            Integer currentUserId
    ) {
        var user = userReadService.requireActive(currentUserId, "ADMIN", "HEAD_OF_DEPARTMENT");
        if ("HEAD_OF_DEPARTMENT".equals(user.role())) {
            if (user.sectionId() == null || (sectionId != null && !sectionId.equals(user.sectionId())))
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Bạn không có quyền xem báo cáo của ban này.");
            sectionId = user.sectionId();
        }
        java.util.Map<LocalDate, java.util.Set<Integer>> validComparisons = new java.util.HashMap<>();
        java.util.Set<LocalDate> periods = period == null ? refreshService.getPeriods() : java.util.Set.of(period);
        for (LocalDate month : periods) validComparisons.put(month, refreshService.refresh(month, currentUserId));
        List<ReviewerDeviationIndex> indices =
                reviewerDeviationRepository.findAllByOrderByPeriodDescEditorIdAsc();
        List<ReviewerDeviationResponse> result = new ArrayList<>();

        for (ReviewerDeviationIndex index : indices) {
            if (!validComparisons.getOrDefault(index.getPeriod(), java.util.Set.of()).contains(index.getEditorId()))
                continue;
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
}
