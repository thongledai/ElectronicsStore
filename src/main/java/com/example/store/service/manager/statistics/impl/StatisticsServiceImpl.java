package com.example.store.service.manager.statistics.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.manager.statistics.CategorySalesDTO;
import com.example.store.dto.manager.statistics.ChartDataPointDTO;
import com.example.store.dto.manager.statistics.DashboardSummaryDTO;
import com.example.store.dto.manager.statistics.StatisticsAnalyticsDTO;
import com.example.store.dto.manager.statistics.TopProductDTO;
import com.example.store.entity.Order;
import com.example.store.repository.OrderItemRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.repository.UserRepository;
import com.example.store.service.manager.statistics.IStatisticsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements IStatisticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).with(LocalTime.MIN);
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);
        LocalDateTime endOfLastMonth = startOfThisMonth.minusNanos(1);

        // 1. Doanh thu
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        BigDecimal thisMonthRevenue = orderRepository.calculateRevenueBetween(startOfThisMonth, now);
        BigDecimal lastMonthRevenue = orderRepository.calculateRevenueBetween(startOfLastMonth, endOfLastMonth);

        Double growthRate = 0.0;
        if (lastMonthRevenue != null && lastMonthRevenue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = thisMonthRevenue.subtract(lastMonthRevenue);
            growthRate = diff.multiply(BigDecimal.valueOf(100))
                    .divide(lastMonthRevenue, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // 2. Đơn hàng
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countPendingOrders();

        // 3. Người dùng
        long totalUsers = userRepository.count();
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(startOfThisMonth);

        // 4. Sản phẩm
        long totalProducts = productRepository.countByIsActiveTrue();
        long lowStockProducts = productVariantRepository.countByQuantityLessThanEqualAndIsActiveTrue(5);

        // 5. Thống kê theo trạng thái đơn
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        statusCounts.put("NOT_PROCESSED", 0L);
        statusCounts.put("CONFIRMED", 0L);
        statusCounts.put("SHIPPING", 0L);
        statusCounts.put("DELIVERED", 0L);
        statusCounts.put("CANCELLED", 0L);
        statusCounts.put("RETURNED", 0L);

        List<Object[]> rawStatus = orderRepository.countOrdersByStatusGroup();
        if (rawStatus != null) {
            for (Object[] row : rawStatus) {
                if (row != null && row.length >= 2 && row[0] != null) {
                    String statusName = row[0].toString().toUpperCase();
                    Long count = ((Number) row[1]).longValue();
                    statusCounts.put(statusName, count);
                }
            }
        }

        // 6. Doanh thu 7 ngày gần nhất
        List<ChartDataPointDTO> last7Days = new ArrayList<>();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 6; i >= 0; i--) {
            LocalDate targetDate = now.toLocalDate().minusDays(i);
            LocalDateTime dayStart = targetDate.atStartOfDay();
            LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

            BigDecimal dayRevenue = orderRepository.calculateRevenueBetween(dayStart, dayEnd);
            last7Days.add(ChartDataPointDTO.builder()
                    .label(targetDate.format(dayFormatter))
                    .value(dayRevenue != null ? dayRevenue : BigDecimal.ZERO)
                    .build());
        }

        return DashboardSummaryDTO.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .previousMonthRevenue(lastMonthRevenue != null ? lastMonthRevenue : BigDecimal.ZERO)
                .revenueGrowthRate(growthRate)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .totalUsers(totalUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .totalProducts(totalProducts)
                .lowStockProducts(lowStockProducts)
                .orderStatusCounts(statusCounts)
                .last7DaysRevenue(last7Days)
                .build();
    }

    @Override
    public StatisticsAnalyticsDTO getAnalytics(String period, String type, LocalDate startDate, LocalDate endDate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime rangeStart;
        LocalDateTime rangeEnd = now;
        boolean isYearly = false;

        String safePeriod = (period != null) ? period.toLowerCase() : "30days";
        switch (safePeriod) {
            case "7days" -> rangeStart = now.minusDays(6).with(LocalTime.MIN);
            case "this_month" -> rangeStart = now.withDayOfMonth(1).with(LocalTime.MIN);
            case "this_year" -> {
                rangeStart = now.withDayOfYear(1).with(LocalTime.MIN);
                isYearly = true;
            }
            case "custom" -> {
                rangeStart = (startDate != null) ? startDate.atStartOfDay() : now.minusDays(29).with(LocalTime.MIN);
                if (endDate != null) rangeEnd = endDate.atTime(LocalTime.MAX);
            }
            default -> rangeStart = now.minusDays(29).with(LocalTime.MIN);
        }

        // 1. Tổng hợp chuỗi dữ liệu biểu đồ
        List<ChartDataPointDTO> chartPoints = new ArrayList<>();
        List<Order> ordersInRange = orderRepository.findOrdersBetween(rangeStart, rangeEnd);

        String safeType = (type != null) ? type.toLowerCase() : "revenue";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

        if (isYearly) {
            // Nhóm theo 12 tháng
            for (int month = 1; month <= 12; month++) {
                final int m = month;
                BigDecimal monthRev = BigDecimal.ZERO;
                long monthOrders = 0;

                for (Order o : ordersInRange) {
                    if (o.getCreatedAt() != null && o.getCreatedAt().getMonthValue() == m) {
                        String st = o.getStatus() != null && o.getStatus().getName() != null ? o.getStatus().getName().toUpperCase() : "";
                        if (!"CANCELLED".equals(st) && !"RETURNED".equals(st)) {
                            if (o.getTotal() != null) monthRev = monthRev.add(o.getTotal());
                            monthOrders++;
                        }
                    }
                }

                BigDecimal val = "orders".equals(safeType) ? BigDecimal.valueOf(monthOrders) : monthRev;
                chartPoints.add(ChartDataPointDTO.builder()
                        .label("Tháng " + (m < 10 ? "0" + m : m))
                        .value(val)
                        .count(monthOrders)
                        .build());
            }
        } else {
            // Nhóm theo từng ngày trong khoảng
            LocalDate cur = rangeStart.toLocalDate();
            LocalDate last = rangeEnd.toLocalDate();

            while (!cur.isAfter(last)) {
                final LocalDate d = cur;
                BigDecimal dayRev = BigDecimal.ZERO;
                long dayOrders = 0;

                for (Order o : ordersInRange) {
                    if (o.getCreatedAt() != null && o.getCreatedAt().toLocalDate().isEqual(d)) {
                        String st = o.getStatus() != null && o.getStatus().getName() != null ? o.getStatus().getName().toUpperCase() : "";
                        if (!"CANCELLED".equals(st) && !"RETURNED".equals(st)) {
                            if (o.getTotal() != null) dayRev = dayRev.add(o.getTotal());
                            dayOrders++;
                        }
                    }
                }

                BigDecimal val = "orders".equals(safeType) ? BigDecimal.valueOf(dayOrders) : dayRev;
                chartPoints.add(ChartDataPointDTO.builder()
                        .label(d.format(formatter))
                        .value(val)
                        .count(dayOrders)
                        .build());

                cur = cur.plusDays(1);
            }
        }

        // 2. Tổng doanh thu & đơn hàng trong kỳ
        BigDecimal totalPeriodRevenue = BigDecimal.ZERO;
        long totalPeriodOrders = 0;
        for (Order o : ordersInRange) {
            String st = o.getStatus() != null && o.getStatus().getName() != null ? o.getStatus().getName().toUpperCase() : "";
            if (!"CANCELLED".equals(st) && !"RETURNED".equals(st)) {
                if (o.getTotal() != null) totalPeriodRevenue = totalPeriodRevenue.add(o.getTotal());
                totalPeriodOrders++;
            }
        }

        long newUsers = userRepository.countByCreatedAtBetween(rangeStart, rangeEnd);

        // 3. Top 5 sản phẩm bán chạy nhất
        List<Object[]> rawTop = orderItemRepository.findTopSellingProducts(rangeStart, rangeEnd, PageRequest.of(0, 5));
        List<TopProductDTO> topProducts = new ArrayList<>();
        if (rawTop != null) {
            for (Object[] r : rawTop) {
                if (r != null && r.length >= 5) {
                    topProducts.add(TopProductDTO.builder()
                            .productId(r[0] != null ? (UUID) r[0] : null)
                            .productName(r[1] != null ? r[1].toString() : "Sản phẩm")
                            .thumbnailUrl(r[2] != null ? r[2].toString() : "/images/placeholder.webp")
                            .totalSold(r[3] != null ? ((Number) r[3]).longValue() : 0L)
                            .totalRevenue(r[4] != null ? (BigDecimal) r[4] : BigDecimal.ZERO)
                            .build());
                }
            }
        }

        // 4. Cơ cấu doanh số theo danh mục
        List<Object[]> rawCat = orderItemRepository.findSalesByCategory(rangeStart, rangeEnd);
        List<CategorySalesDTO> categories = new ArrayList<>();
        BigDecimal totalCatSales = BigDecimal.ZERO;

        if (rawCat != null) {
            for (Object[] r : rawCat) {
                if (r != null && r.length >= 4 && r[2] != null) {
                    totalCatSales = totalCatSales.add((BigDecimal) r[2]);
                }
            }
            for (Object[] r : rawCat) {
                if (r != null && r.length >= 4) {
                    BigDecimal catRev = r[2] != null ? (BigDecimal) r[2] : BigDecimal.ZERO;
                    Double pct = 0.0;
                    if (totalCatSales.compareTo(BigDecimal.ZERO) > 0) {
                        pct = catRev.multiply(BigDecimal.valueOf(100))
                                .divide(totalCatSales, 1, RoundingMode.HALF_UP)
                                .doubleValue();
                    }
                    categories.add(CategorySalesDTO.builder()
                            .categoryId(r[0] != null ? (UUID) r[0] : null)
                            .categoryName(r[1] != null ? r[1].toString() : "Khác")
                            .totalRevenue(catRev)
                            .totalSold(r[3] != null ? ((Number) r[3]).longValue() : 0L)
                            .percentage(pct)
                            .build());
                }
            }
        }

        return StatisticsAnalyticsDTO.builder()
                .totalRevenue(totalPeriodRevenue)
                .totalOrders(totalPeriodOrders)
                .newUsers(newUsers)
                .chartData(chartPoints)
                .topProducts(topProducts)
                .categoryDistribution(categories)
                .build();
    }
}
