package com.example.store.dto.order;

import java.math.BigDecimal;

public record PriceBreakdown(
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal discount,
        BigDecimal total,
        BigDecimal productDiscount,
        BigDecimal voucherDiscount
) {
    public PriceBreakdown(BigDecimal subtotal, BigDecimal shippingFee, BigDecimal discount, BigDecimal total) {
        this(subtotal, shippingFee, discount, total, discount, BigDecimal.ZERO);
    }
}
