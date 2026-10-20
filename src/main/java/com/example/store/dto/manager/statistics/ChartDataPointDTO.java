package com.example.store.dto.manager.statistics;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataPointDTO {
    private String label;
    private BigDecimal value;
    private Long count;
}
