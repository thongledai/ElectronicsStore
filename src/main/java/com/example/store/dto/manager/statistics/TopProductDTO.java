package com.example.store.dto.manager.statistics;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDTO {
    private UUID productId;
    private String productName;
    private String thumbnailUrl;
    private Long totalSold;
    private BigDecimal totalRevenue;
}
