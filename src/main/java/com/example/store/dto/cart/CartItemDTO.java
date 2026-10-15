package com.example.store.dto.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDTO(
        UUID id,
        UUID productId,
        UUID productVariantId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        String styleValues,
        boolean isAvailable,        // còn bán
        Integer availableStock
) {}
