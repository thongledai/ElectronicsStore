package com.example.store.service.promotion;

import com.example.store.dto.order.PriceBreakdown;
import com.example.store.dto.promotion.VoucherCheckRequest;
import com.example.store.dto.promotion.VoucherCheckResponse;
import com.example.store.entity.CartItem;
import com.example.store.entity.Promotion;
import com.example.store.enums.error.PromotionErrorCode;
import com.example.store.exception.AppException;
import com.example.store.repository.PromotionRepository;
import com.example.store.service.cart.ICartService;
import com.example.store.service.order.PricingCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements IPromotionService {

    private final PromotionRepository promotionRepository;
    private final ICartService cartService;
    private final PricingCalculator pricingCalculator;

    @Override
    @Transactional(readOnly = true)
    public VoucherCheckResponse checkVoucher(UUID userId, VoucherCheckRequest req) {
        if (!StringUtils.hasText(req.code())) {
            return new VoucherCheckResponse(
                    false, "Vui lòng nhập mã giảm giá", null, null, null,
                    null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<CartItem> items = cartService.getOwnedItems(userId, req.cartItemIds());
        if (items.isEmpty()) {
            return new VoucherCheckResponse(
                    false, "Giỏ hàng không có sản phẩm hợp lệ", req.code(), null, null,
                    null, null, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Tạm tính giá trị tiền hàng (hoàn toàn riêng biệt với phí vận chuyển)
        PriceBreakdown basePrice = pricingCalculator.calculate(items, BigDecimal.ZERO, null);
        BigDecimal subtotal = basePrice.subtotal();

        // Kiểm tra mã voucher trong database
        Promotion promotion = promotionRepository.findByCodeIgnoreCase(req.code().trim()).orElse(null);
        if (promotion == null) {
            return new VoucherCheckResponse(
                    false, PromotionErrorCode.VOUCHER_NOT_FOUND.getMessage(), req.code(), null, null,
                    null, null, null, BigDecimal.ZERO, subtotal, BigDecimal.ZERO, basePrice.total());
        }

        if (!Boolean.TRUE.equals(promotion.getIsActive()) || !Boolean.TRUE.equals(promotion.getIsEnabled())) {
            return new VoucherCheckResponse(
                    false, PromotionErrorCode.VOUCHER_DISABLED.getMessage(), promotion.getCode(), promotion.getDescription(), promotion.getType(),
                    promotion.getDiscountPercent(), promotion.getMaxDiscountAmount(), promotion.getMinOrderValue(),
                    BigDecimal.ZERO, subtotal, BigDecimal.ZERO, basePrice.total());
        }

        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            return new VoucherCheckResponse(
                    false, PromotionErrorCode.VOUCHER_NOT_STARTED.getMessage(), promotion.getCode(), promotion.getDescription(), promotion.getType(),
                    promotion.getDiscountPercent(), promotion.getMaxDiscountAmount(), promotion.getMinOrderValue(),
                    BigDecimal.ZERO, subtotal, BigDecimal.ZERO, basePrice.total());
        }

        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            return new VoucherCheckResponse(
                    false, PromotionErrorCode.VOUCHER_EXPIRED.getMessage(), promotion.getCode(), promotion.getDescription(), promotion.getType(),
                    promotion.getDiscountPercent(), promotion.getMaxDiscountAmount(), promotion.getMinOrderValue(),
                    BigDecimal.ZERO, subtotal, BigDecimal.ZERO, basePrice.total());
        }

        if (promotion.getQuantity() != null && promotion.getQuantity() <= 0) {
            return new VoucherCheckResponse(
                    false, PromotionErrorCode.VOUCHER_OUT_OF_STOCK.getMessage(), promotion.getCode(), promotion.getDescription(), promotion.getType(),
                    promotion.getDiscountPercent(), promotion.getMaxDiscountAmount(), promotion.getMinOrderValue(),
                    BigDecimal.ZERO, subtotal, BigDecimal.ZERO, basePrice.total());
        }

        BigDecimal minOrder = promotion.getMinOrderValue() != null ? promotion.getMinOrderValue() : BigDecimal.ZERO;
        if (subtotal.compareTo(minOrder) < 0) {
            return new VoucherCheckResponse(
                    false, "Đơn hàng tối thiểu " + formatMoney(minOrder) + " để áp dụng mã này",
                    promotion.getCode(), promotion.getDescription(), promotion.getType(),
                    promotion.getDiscountPercent(), promotion.getMaxDiscountAmount(), promotion.getMinOrderValue(),
                    BigDecimal.ZERO, subtotal, BigDecimal.ZERO, basePrice.total());
        }

        // Tính toán số tiền voucher giảm giá (hoàn toàn riêng biệt, không tự tính toán phí vận chuyển)
        BigDecimal currentShipping = req.shippingFee() != null ? req.shippingFee() : BigDecimal.ZERO;
        PriceBreakdown finalPrice = pricingCalculator.calculate(items, currentShipping, promotion);
        BigDecimal discountAmount = finalPrice.voucherDiscount();
        BigDecimal totalAfterVoucher = basePrice.total().subtract(discountAmount).max(BigDecimal.ZERO);

        return new VoucherCheckResponse(
                true,
                "Áp dụng mã giảm giá thành công",
                promotion.getCode(),
                promotion.getDescription(),
                promotion.getType(),
                promotion.getDiscountPercent(),
                promotion.getMaxDiscountAmount(),
                promotion.getMinOrderValue(),
                discountAmount,
                subtotal,
                BigDecimal.ZERO,
                totalAfterVoucher
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Promotion validatePromotion(String code, BigDecimal subtotal) {
        if (!StringUtils.hasText(code)) {
            return null;
        }

        Promotion promotion = promotionRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new AppException(PromotionErrorCode.VOUCHER_NOT_FOUND));

        if (!Boolean.TRUE.equals(promotion.getIsActive()) || !Boolean.TRUE.equals(promotion.getIsEnabled())) {
            throw new AppException(PromotionErrorCode.VOUCHER_DISABLED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            throw new AppException(PromotionErrorCode.VOUCHER_NOT_STARTED);
        }

        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            throw new AppException(PromotionErrorCode.VOUCHER_EXPIRED);
        }

        if (promotion.getQuantity() != null && promotion.getQuantity() <= 0) {
            throw new AppException(PromotionErrorCode.VOUCHER_OUT_OF_STOCK);
        }

        BigDecimal minOrder = promotion.getMinOrderValue() != null ? promotion.getMinOrderValue() : BigDecimal.ZERO;
        if (subtotal != null && subtotal.compareTo(minOrder) < 0) {
            throw new AppException(PromotionErrorCode.VOUCHER_MIN_ORDER_NOT_MET);
        }

        return promotion;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 đ";
        return String.format("%,d đ", amount.longValue());
    }
}
