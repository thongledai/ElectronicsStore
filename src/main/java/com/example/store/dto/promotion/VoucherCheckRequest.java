package com.example.store.dto.promotion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record VoucherCheckRequest(
        @NotBlank(message = "Vui lòng nhập mã giảm giá")
        String code,

        @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm")
        List<UUID> cartItemIds,

        BigDecimal shippingFee,
        Integer provinceId,
        String wardCode
) {
    public VoucherCheckRequest(String code, List<UUID> cartItemIds) {
        this(code, cartItemIds, BigDecimal.ZERO, null, null);
    }

    public VoucherCheckRequest(String code, List<UUID> cartItemIds, Integer provinceId, String wardCode) {
        this(code, cartItemIds, BigDecimal.ZERO, provinceId, wardCode);
    }
}
