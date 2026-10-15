package com.example.store.service.order;

import com.example.store.dto.order.PriceBreakdown;
import com.example.store.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PricingCalculator {

        public PriceBreakdown calculate(
                        List<CartItem> items,
                        BigDecimal shippingFee) {

                BigDecimal subtotal = items.stream()
                                .map(item -> item.getVariant()
                                                .getPrice()
                                                .multiply(BigDecimal.valueOf(item.getCount())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (shippingFee == null || shippingFee.compareTo(BigDecimal.ZERO) < 0) {
                        shippingFee = BigDecimal.ZERO;
                }

                BigDecimal discount = BigDecimal.ZERO;

                BigDecimal total = subtotal
                                .add(shippingFee)
                                .subtract(discount)
                                .max(BigDecimal.ZERO);

                return new PriceBreakdown(
                                subtotal,
                                shippingFee,
                                discount,
                                total);
        }
}