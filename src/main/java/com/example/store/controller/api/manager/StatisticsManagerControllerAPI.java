package com.example.store.controller.api.manager;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.manager.statistics.DashboardSummaryDTO;
import com.example.store.dto.manager.statistics.StatisticsAnalyticsDTO;
import com.example.store.service.manager.statistics.IStatisticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/manager/statistics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('MANAGER','ROLE_MANAGER')")
public class StatisticsManagerControllerAPI {

    private final IStatisticsService statisticsService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getDashboardSummary() {
        DashboardSummaryDTO data = statisticsService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tổng quan thành công", data));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<StatisticsAnalyticsDTO>> getAnalytics(
            @RequestParam(defaultValue = "30days") String period,
            @RequestParam(defaultValue = "revenue") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        StatisticsAnalyticsDTO data = statisticsService.getAnalytics(period, type, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu thống kê thành công", data));
    }
}
