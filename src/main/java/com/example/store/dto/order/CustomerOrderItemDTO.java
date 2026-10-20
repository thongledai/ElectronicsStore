package com.example.store.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerOrderItemDTO(
        UUID id,
        UUID variantId,
        UUID productId,
        String productName,
        String variantDescription,
        String imageUrl,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {}
