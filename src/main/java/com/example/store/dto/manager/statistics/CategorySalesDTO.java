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
public class CategorySalesDTO {
    private UUID categoryId;
    private String categoryName;
    private BigDecimal totalRevenue;
    private Long totalSold;
    private Double percentage;
}
