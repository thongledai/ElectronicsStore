package com.example.store.mapper;

import com.example.store.dto.cart.CartItemDTO;
import com.example.store.entity.CartItem;
import com.example.store.entity.Product;
import com.example.store.entity.ProductVariant;
import com.example.store.entity.StyleValue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Component
public class CartItemMapper {
    public CartItemDTO toCartItemDTO(CartItem cartItem) {
        ProductVariant productVariant = cartItem.getVariant();
        Product product = productVariant.getProduct();
        String firstImageUrl = productVariant.getImages() == null
                ? null
                : productVariant.getImages().stream()
                        .filter(image -> image != null)
                        .map(image -> image.getImageUrl())
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
                getStyleValues(productVariant),
                productVariant.getQuantity()
                );
    }

    private String getStyleValues(ProductVariant variant) {
        return variant.getStyleValues().stream()
                .map(StyleValue::getName)
                .collect(Collectors.joining(", "));
    }

}
