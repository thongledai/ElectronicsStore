package com.example.store.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderPreviewDTO(
        UUID id,
        String status,
        LocalDateTime createdAt,

        String receiverName,
        String receiverPhone,
        String shippingAddress,

        List<OrderItemDTO> items,

        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discount,
        BigDecimal total,

        String paymentMethod,
        Boolean isPaid,
        String note,
        String expectedDeliveryTime,
        String voucherCode,
        BigDecimal voucherDiscount
) {
    public OrderPreviewDTO(
            UUID id, String status, LocalDateTime createdAt,
            String receiverName, String receiverPhone, String shippingAddress,
            List<OrderItemDTO> items,
            BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discount, BigDecimal total,
            String paymentMethod, Boolean isPaid, String note, String expectedDeliveryTime) {
        this(id, status, createdAt, receiverName, receiverPhone, shippingAddress, items,
                subtotal, shippingFee, discount, total, paymentMethod, isPaid, note, expectedDeliveryTime, null, BigDecimal.ZERO);
    }
}
