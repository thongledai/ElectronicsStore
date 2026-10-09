package com.example.store.dto.respone;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CartItemDTO(
        UUID id,                        // id của CartItem, dùng để cập nhật/xóa dòng
        UUID productId,
        UUID productVariantId,
        String productName,
        String productImageUrl,
        BigDecimal unitPrice,           // giá hiện tại của sản phẩm
        int count,
        BigDecimal subtotal,           // unitPrice × count
        String styleValues,
        boolean available,              // còn bán và đủ tồn kho
        Integer availableStock        // tồn kho tối đa, dùng giới hạn ô nhập số lượng
) {}
