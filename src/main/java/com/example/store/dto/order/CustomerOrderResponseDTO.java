package com.example.store.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CustomerOrderResponseDTO(
        UUID id,
        String orderCode,
        String status,
        String statusDisplayName,
        LocalDateTime createdAt,
        String receiverName,
        String receiverPhone,
        String shippingAddress,
        List<CustomerOrderItemDTO> items,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        Boolean isPaid,
        String note,
        String ghnOrderCode,
        LocalDateTime expectedDeliveryDate,
        boolean canCancel
) {}
