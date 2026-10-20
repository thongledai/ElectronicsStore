package com.example.store.dto.manager.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerOrderDetailDTO {
    private UUID id;
    private String shortCode;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String toName;
    private String phone;
    private String fullAddress;
    private String detailAddress;
    private String wardName;
    private String provinceName;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expectedDeliveryDate;
    private String paymentMethodName;
    private Boolean isPaidBefore;
    private String statusName;
    private String ghnOrderCode;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discount;
    private BigDecimal total;
    private List<ManagerOrderItemDTO> items;
}
