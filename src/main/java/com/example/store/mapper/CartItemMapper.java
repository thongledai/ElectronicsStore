package com.example.store.mapper;

import com.example.store.dto.cart.CartItemDTO;
import com.example.store.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class CartItemMapper {
    public CartItemDTO toCartItemDTO(CartItem cartItem) {
        ProductVariant productVariant = cartItem.getVariant();
        Product product = productVariant.getProduct();
        String firstImageUrl = productVariant.getImages() == null
                ? null
                : productVariant.getImages().stream()
                        .filter(Objects::nonNull)
                        .map(ProductVariantImage::getImageUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .findFirst()
                        .orElse(null);

        BigDecimal originalPrice = productVariant.getPrice() != null ? productVariant.getPrice() : BigDecimal.ZERO;
        BigDecimal promoPrice = productVariant.getPromotionalPrice();
        BigDecimal effectivePrice = (promoPrice != null && promoPrice.compareTo(BigDecimal.ZERO) > 0 && promoPrice.compareTo(originalPrice) < 0)
                ? promoPrice
                : originalPrice;

        BigDecimal subtotal = effectivePrice.multiply(BigDecimal.valueOf(cartItem.getCount()));

        return new CartItemDTO(
                cartItem.getId(),
                product.getId(),
                productVariant.getId(),
                product.getName(),
                firstImageUrl,
                effectivePrice, // unitPrice là giá đã được giảm
                cartItem.getCount(),
                subtotal,
                getStyleValues(productVariant),
                this.isAvailable(productVariant),
                productVariant.getQuantity(),
                product.getSlug(),
                originalPrice,
                promoPrice
        );
    }

    private boolean isAvailable(ProductVariant productVariant) {
        Product product = productVariant.getProduct();
        return product.isActive() && product.isSelling()
                && productVariant.isActive() && productVariant.isSelling();
    }

    private String getStyleValues(ProductVariant variant) {
        return variant.getStyleValues().stream()
                .map(StyleValue::getName)
                .collect(Collectors.joining(", "));
    }
}
