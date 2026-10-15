package com.example.store.dto.order;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CheckoutPreviewRequest(
        @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm") List<UUID> cartItemIds,

        String voucherCode // không bắt buộc, chưa dùng cho đến khi có module khuyến mãi
) {
}
