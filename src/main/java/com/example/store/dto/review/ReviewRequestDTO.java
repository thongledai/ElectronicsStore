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

    @NotNull(message = "ID sản phẩm không được để trống")
    private UUID productId;

    /**
     * Không bắt buộc (null nếu đánh giá trực tiếp từ trang chi tiết sản phẩm).
     * Nếu có, hệ thống sẽ xác minh để gắn nhãn "Đã mua tại TechNova".
     */
    private UUID orderId;

    @NotNull(message = "Số sao đánh giá không được để trống")
    @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
    private Integer stars;

    @NotBlank(message = "Nội dung nhận xét không được để trống")
    @Size(max = 1000, message = "Nội dung nhận xét tối đa 1000 ký tự")
    private String content;

    private List<String> imageUrls;
}
