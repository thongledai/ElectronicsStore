package com.example.store.service.order;

import com.example.store.dto.order.PriceBreakdown;
import com.example.store.entity.CartItem;
import com.example.store.entity.Delivery;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PricingCalculator {

    public PriceBreakdown calculate(List<CartItem> items, Delivery delivery) {
        BigDecimal subtotal = items.stream()
                .map(i -> i.getVariant().getPrice()
                        .multiply(BigDecimal.valueOf(i.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingFee = delivery.getPrice();
        BigDecimal discount = BigDecimal.ZERO;   // chưa có module khuyến mãi

        BigDecimal total = subtotal.add(shippingFee).subtract(discount)
                .max(BigDecimal.ZERO);           // không để âm

        return new PriceBreakdown(subtotal, shippingFee, discount, total);
    }
}
