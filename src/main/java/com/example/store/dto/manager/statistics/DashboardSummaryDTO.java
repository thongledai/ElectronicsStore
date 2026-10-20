package com.example.store.dto.manager.statistics;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private BigDecimal totalRevenue;
    private BigDecimal previousMonthRevenue;
    private Double revenueGrowthRate;

    private Long totalOrders;
    private Long pendingOrders;

    private Long totalUsers;
    private Long newUsersThisMonth;

    private Long totalProducts;
    private Long lowStockProducts;

    private Map<String, Long> orderStatusCounts;
    private List<ChartDataPointDTO> last7DaysRevenue;
}
