package com.example.store.service.order;

import com.example.store.dto.order.PriceBreakdown;
import com.example.store.entity.CartItem;
import com.example.store.entity.Promotion;
import com.example.store.enums.PromotionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PricingCalculator {

    public PriceBreakdown calculate(List<CartItem> items, BigDecimal shippingFee) {
        return calculate(items, shippingFee, null);
    }

    public PriceBreakdown calculate(List<CartItem> items, BigDecimal shippingFee, Promotion promotion) {
        BigDecimal ship = shippingFee == null ? BigDecimal.ZERO : shippingFee;

        if (items == null || items.isEmpty()) {
            return new PriceBreakdown(BigDecimal.ZERO, ship, BigDecimal.ZERO, ship, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // 1. Tổng tiền hàng (giá gốc): subtotal = sum(price * count)
        BigDecimal subtotal = items.stream()
                .map(item -> {
                    BigDecimal price = (item.getVariant() != null && item.getVariant().getPrice() != null)
                            ? item.getVariant().getPrice()
                            : BigDecimal.ZERO;
                    int count = item.getCount() != null ? item.getCount() : 1;
                    return price.multiply(BigDecimal.valueOf(count));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Giảm giá sản phẩm (khuyến mãi phân loại): productDiscount = sum((price - promotionalPrice) * count)
        BigDecimal productDiscount = items.stream()
                .map(item -> {
                    if (item.getVariant() == null) return BigDecimal.ZERO;
                    BigDecimal price = item.getVariant().getPrice() != null
                            ? item.getVariant().getPrice()
                            : BigDecimal.ZERO;
                    BigDecimal promoPrice = item.getVariant().getPromotionalPrice();
                    int count = item.getCount() != null ? item.getCount() : 1;

                    if (promoPrice != null && promoPrice.compareTo(BigDecimal.ZERO) > 0 && promoPrice.compareTo(price) < 0) {
                        return price.subtract(promoPrice).multiply(BigDecimal.valueOf(count));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Giảm giá từ Voucher (nếu có)
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        if (promotion != null && promotion.getDiscountPercent() != null) {
            BigDecimal minOrder = promotion.getMinOrderValue() != null ? promotion.getMinOrderValue() : BigDecimal.ZERO;

            if (subtotal.compareTo(minOrder) >= 0) {
                BigDecimal percent = promotion.getDiscountPercent().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

                if (promotion.getType() == PromotionType.PRODUCT_PERCENT) {
                    // Giảm % theo tổng tiền hàng
                    voucherDiscount = subtotal.multiply(percent).setScale(0, RoundingMode.HALF_UP);
                } else if (promotion.getType() == PromotionType.SHIPPING_PERCENT) {
                    // Giảm % theo phí vận chuyển
                    voucherDiscount = ship.multiply(percent).setScale(0, RoundingMode.HALF_UP);
                }

                // Áp dụng mức giảm tối đa nếu có cấu hình
                if (promotion.getMaxDiscountAmount() != null && promotion.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                    voucherDiscount = voucherDiscount.min(promotion.getMaxDiscountAmount());
                }
            }
        }

        // 4. Tổng số tiền được giảm
        BigDecimal totalDiscount = productDiscount.add(voucherDiscount);

        // 5. Tổng thanh toán = Tổng tiền hàng + Phí vận chuyển - Tổng giảm giá
        BigDecimal total = subtotal.add(ship).subtract(totalDiscount).max(BigDecimal.ZERO);

        return new PriceBreakdown(subtotal, ship, totalDiscount, total, productDiscount, voucherDiscount);
    }
}
