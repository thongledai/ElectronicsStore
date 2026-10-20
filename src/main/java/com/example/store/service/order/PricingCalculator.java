package com.example.store.service.order;

import com.example.store.dto.order.PriceBreakdown;
import com.example.store.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PricingCalculator {

    public PriceBreakdown calculate(List<CartItem> items, BigDecimal shippingFee) {
        if (items == null || items.isEmpty()) {
            BigDecimal ship = shippingFee == null ? BigDecimal.ZERO : shippingFee;
            return new PriceBreakdown(BigDecimal.ZERO, ship, BigDecimal.ZERO, ship);
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

        // 2. Giảm giá khuyến mãi: discount = sum((price - promotionalPrice) * count)
        BigDecimal discount = items.stream()
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

        // 3. Phí vận chuyển
        BigDecimal ship = shippingFee == null ? BigDecimal.ZERO : shippingFee;

        // 4. Tổng thanh toán = Tổng tiền hàng + Phí vận chuyển - Giảm giá
        BigDecimal total = subtotal.add(ship).subtract(discount).max(BigDecimal.ZERO);

        return new PriceBreakdown(subtotal, ship, discount, total);
    }
}
