package com.example.store.service.promotion;

import com.example.store.dto.promotion.VoucherCheckRequest;
import com.example.store.dto.promotion.VoucherCheckResponse;
import com.example.store.entity.Promotion;

import java.math.BigDecimal;
import java.util.UUID;

public interface IPromotionService {
    VoucherCheckResponse checkVoucher(UUID userId, VoucherCheckRequest req);
    Promotion validatePromotion(String code, BigDecimal subtotal);
}
