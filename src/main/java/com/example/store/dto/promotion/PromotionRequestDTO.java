package com.example.store.dto.promotion;

import com.example.store.enums.PromotionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequestDTO {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(min = 3, max = 50, message = "Mã voucher phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã voucher chỉ bao gồm chữ, số, gạch ngang và gạch dưới")
    private String code;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    @NotNull(message = "Loại voucher không được để trống")
    private PromotionType type;

    @NotNull(message = "Phần trăm giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Phần trăm giảm phải lớn hơn 0")
    @DecimalMax(value = "100.00", message = "Phần trăm giảm tối đa là 100%")
    private BigDecimal discountPercent;

    @DecimalMin(value = "0.00", message = "Mức giảm tối đa không được âm")
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0.00", message = "Đơn tối thiểu không được âm")
    private BigDecimal minOrderValue;

    @NotNull(message = "Số lượt sử dụng không được để trống")
    @Min(value = 1, message = "Số lượt sử dụng tối thiểu là 1")
    private Integer quantity;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    private LocalDateTime endDate;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isEnabled = true;
}
