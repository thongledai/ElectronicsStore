package com.example.store.dto.review;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDTO {

    @NotNull(message = "REVIEW_PRODUCT_ID_REQUIRED")
    private UUID productId;

    /**
     * Không bắt buộc (null nếu đánh giá trực tiếp từ trang chi tiết sản phẩm).
     * Nếu có, hệ thống sẽ xác minh để gắn nhãn "Đã mua tại TechNova".
     */
    private UUID orderId;

    @NotNull(message = "REVIEW_RATING_REQUIRED")
    @Min(value = 1, message = "REVIEW_RATING_MIN")
    @Max(value = 5, message = "REVIEW_RATING_MAX")
    private Integer stars;

    @NotBlank(message = "REVIEW_CONTENT_REQUIRED")
    @Size(max = 1000, message = "REVIEW_CONTENT_MAX_LENGTH")
    private String content;

    private List<String> imageUrls;
}
