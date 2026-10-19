package com.example.store.service.order;

import com.example.store.dto.order.PriceBreakdown;
import com.example.store.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Component
public class PricingCalculator {

    public PriceBreakdown calculate(List<CartItem> items, BigDecimal shippingFee) {

            BigDecimal subtotal = items.stream()
                            .map(item -> item.getVariant()
                                            .getPrice()
                                            .multiply(BigDecimal.valueOf(item.getCount())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal ship = shippingFee == null ? BigDecimal.ZERO : shippingFee;
            BigDecimal total = subtotal.add(ship).subtract(discount).max(BigDecimal.ZERO);

            return new PriceBreakdown(subtotal, ship, discount, total);
    }
}