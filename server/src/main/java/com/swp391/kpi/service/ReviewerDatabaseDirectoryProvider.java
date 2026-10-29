package com.swp391.kpi.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Ban lay du lieu tu DB cho ReviewerDirectoryProvider: tim thong tin 1 BTV.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   15  field                           repository / service duoc inject
 *   17  findEditor()                    tim editor
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
