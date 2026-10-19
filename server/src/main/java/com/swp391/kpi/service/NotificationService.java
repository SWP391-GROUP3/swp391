package com.swp391.kpi.service;

import com.swp391.kpi.entity.Notification;
import com.swp391.kpi.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository repository;
    private final UserReadService userReadService;

    @Transactional
    public void notifyHeads(Integer articleId, Integer sectionId, String message) {
        for (var user : userReadService.getUsers()) {
            if (!"ACTIVE".equals(user.status()) || !"HEAD_OF_DEPARTMENT".equals(user.role())
                    || !sectionId.equals(user.sectionId())) continue;
            var notification = new Notification();
            notification.setUserId(user.id());
            notification.setArticleId(articleId);
            notification.setMessage(message);
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            repository.save(notification);
        }
    }
}
