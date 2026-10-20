package com.example.store.dto.manager.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerOrderSummaryDTO {
    private UUID id;
    private String shortCode;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private LocalDateTime createdAt;
    private BigDecimal total;
    private String paymentMethodName;
    private Boolean isPaidBefore;
    private String statusName;
    private String ghnOrderCode;
    private int itemCount;
}
