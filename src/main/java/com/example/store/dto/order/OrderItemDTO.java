package com.example.store.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDTO(
        UUID id,
        String productName,          // tên lúc mua
        String variantDescription,   // "Đỏ, Size M"
        String imageUrl,
        BigDecimal unitPrice,        // giá lúc mua
        int quantity,
        BigDecimal subtotal          // unitPrice x quantity
) {}
