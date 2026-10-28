package com.example.store.dto.review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.store.enums.ReviewStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDTO {

    private UUID id;
    private UUID productId;
    private String productName;
    private UUID orderId;
    private UUID userId;
    private String userName;
    private String userAvatar;

    /**
     * Huy hiệu kiểu Thế Giới Di Động: "✓ Đã mua tại TechNova"
     */
    private boolean isPurchased;

    private Integer stars;
    private String content;
    private ReviewStatus status;
    private String statusDescription;
    private List<String> imageUrls;
    private String adminReply;
    private String repliedByName;
    private LocalDateTime repliedAt;
    private LocalDateTime createdAt;
}
