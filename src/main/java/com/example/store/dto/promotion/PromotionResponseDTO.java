package com.example.store.dto.promotion;

import com.example.store.enums.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponseDTO {

    private UUID id;
    private String code;
    private String description;
    private PromotionType type;
    private String typeText;
    private BigDecimal discountPercent;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderValue;
    private Integer quantity;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private Boolean isEnabled;
    private String status;        // ACTIVE, UPCOMING, EXPIRED, OUT_OF_STOCK, INACTIVE
    private String statusText;    // "Đang diễn ra", "Sắp diễn ra", "Đã kết thúc", "Hết lượt dùng", "Tạm ngưng"
    private Long usedCount;       // Số đơn hàng đã dùng voucher này
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
