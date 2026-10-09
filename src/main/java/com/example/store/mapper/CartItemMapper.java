package com.example.store.mapper;

import com.example.store.dto.respone.CartItemDTO;
import com.example.store.entity.CartItem;
import com.example.store.entity.Product;
import com.example.store.entity.ProductVariant;
import com.example.store.entity.ProductVariantImage;
import com.example.store.entity.StyleValue;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public class CartItemMapper {
    public CartItemDTO toCartItemDTO(CartItem cartItem) {
        ProductVariant productVariant = cartItem.getVariant();
        Product product = productVariant.getProduct();
        String firstImageUrl = productVariant.getImages() == null
                ? null
                : productVariant.getImages().stream()
                        .map(ProductVariantImage::getImageUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .findFirst()
                        .orElse(null);

        BigDecimal subtotal = productVariant.getPrice().multiply(BigDecimal.valueOf(cartItem.getCount()));

        return new CartItemDTO(cartItem.getId(),
                product.getId(),
                productVariant.getId(),
                product.getName(),
                firstImageUrl,
                productVariant.getPrice(),
                cartItem.getCount(),
                subtotal,
                getStyleValues(cartItem),
                isAvailable(cartItem),
                productVariant.getQuantity()
                );
    }

    private String getStyleValues(CartItem cartItem) {
        ProductVariant productVariant = cartItem.getVariant();
        if (productVariant == null) {
            return "";
        }

        Set<StyleValue> styleValues = productVariant.getStyleValues();
        if (styleValues == null || styleValues.isEmpty()) {
            return "";
        }
        return styleValues.stream().map(StyleValue::getName)
                .collect(Collectors.joining(", "));
    }

    private boolean isAvailable(CartItem cartItem) {
        ProductVariant variant = cartItem.getVariant();
        return variant != null
                && variant.getProduct() != null
                && variant.getProduct().isActive()
                && variant.getProduct().isSelling();
    }
}
