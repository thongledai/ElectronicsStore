package com.example.store.dto.manager.statistics;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsAnalyticsDTO {
    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long newUsers;
    private List<ChartDataPointDTO> chartData;
    private List<TopProductDTO> topProducts;
    private List<CategorySalesDTO> categoryDistribution;
}
