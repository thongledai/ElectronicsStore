package com.example.store.dto.promotion;

import com.example.store.enums.PromotionType;

import java.math.BigDecimal;

public record VoucherCheckResponse(
        boolean valid,
        String message,
        String code,
        String description,
        PromotionType type,
        BigDecimal discountPercent,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderValue,
        BigDecimal discountAmount,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal total
) {}
