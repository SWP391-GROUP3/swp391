package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyMediaItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Doc/ghi bang royalty_media_items (tien tung anh/video, UC-48).
 *
 * MUC LUC (so dong)
 *   16  existsByAttachmentId()       - Anh nay DA TUNG duoc tra nhuan but chua (co dong trong royalty_media_it…
 */
public interface RoyaltyMediaItemRepository extends JpaRepository<RoyaltyMediaItem, Integer> {

    // Anh nay DA TUNG duoc tra nhuan but chua (co dong trong royalty_media_items).
    // Dung de nhan anh tai su dung: anh trung voi anh da tra tien thi khong tinh nua (BR-IN-05, BR-RY-04).
    boolean existsByAttachmentId(Integer attachmentId);
}
