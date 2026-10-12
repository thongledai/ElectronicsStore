package com.example.store.mapper;

import com.example.store.dto.respone.CartItemDTO;
import com.example.store.entity.CartItem;
import com.example.store.entity.Product;
import com.example.store.entity.ProductVariant;
import com.example.store.entity.StyleValue;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CartItemMapper {
    public CartItemDTO toCartItemDTO(CartItem cartItem) {
        ProductVariant productVariant = cartItem.getVariant();
        Product product = productVariant.getProduct();
        String firstImageUrl = productVariant.getImages().getFirst().getImageUrl();
        BigDecimal subtotal = productVariant.getPrice().multiply(BigDecimal.valueOf(cartItem.getCount()));

        return new CartItemDTO(cartItem.getId(),
                product.getId(),
                productVariant.getId(),
                product.getName(),
                firstImageUrl,
                productVariant.getPrice(),
                cartItem.getCount(),
                subtotal,
                getStyleValues(productVariant)
                );
    }

    private String getStyleValues(ProductVariant variant) {
        return variant.getStyleValues().stream()
                .map(StyleValue::getName)
                .collect(Collectors.joining(", "));
    }

}
