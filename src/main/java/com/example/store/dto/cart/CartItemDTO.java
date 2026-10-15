package com.example.store.dto.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemDTO(
        UUID id,                        // id của CartItem, dùng để cập nhật/xóa dòng
        UUID productId,
        UUID productVariantId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,           // giá hiện tại của sản phẩm
        int quantity,
        BigDecimal subtotal,           // unitPrice × count
        String styleValues,
        Integer availableStock
) {}
