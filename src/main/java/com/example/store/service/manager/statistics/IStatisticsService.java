package com.example.store.service.manager.statistics;

import java.time.LocalDate;

import com.example.store.dto.manager.statistics.DashboardSummaryDTO;
import com.example.store.dto.manager.statistics.StatisticsAnalyticsDTO;

public interface IStatisticsService {
    DashboardSummaryDTO getDashboardSummary();
    StatisticsAnalyticsDTO getAnalytics(String period, String type, String status, LocalDate startDate, LocalDate endDate);
}
