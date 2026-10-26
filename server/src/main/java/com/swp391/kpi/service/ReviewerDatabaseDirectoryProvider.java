package com.swp391.kpi.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * MUC LUC (so dong)
 *   13  field                        - repository / service duoc inject
 *   15  findEditor()
 */
@Service @RequiredArgsConstructor
public class ReviewerDatabaseDirectoryProvider implements ReviewerDirectoryProvider {
    private final UserReadService userReadService;
    @Override
    public Optional<ReviewerDirectoryEntry> findEditor(Integer editorId) {
        for (var user : userReadService.getUsers()) {
            if (editorId.equals(user.id()) && "EDITOR".equals(user.role())) {
                return Optional.of(new ReviewerDirectoryEntry(user.id(), user.fullName(),
                        user.sectionId(), user.sectionName()));
            }
        }
        return Optional.empty();
    }
}
