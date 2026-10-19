package com.example.store.dto.order;

import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderPreviewDTO(
        UUID id,
        String status,               // PENDING, CONFIRMED, ...
        LocalDateTime createdAt,     // ngày đặt

        String receiverName,         // người nhận
        String receiverPhone,
        String shippingAddress,

        List<OrderItemDTO> items,    // danh sách sản phẩm trong đơn

        BigDecimal subtotal,         // tạm tính
        BigDecimal shippingFee,      // phí vận chuyển
        BigDecimal discount,         // giảm giá
        BigDecimal total,            // tổng cộng


        String paymentMethod,        // COD
        Boolean isPaid,        // UNPAID, PAID
        String note,
        String expectedDeliveryTime
) {}
