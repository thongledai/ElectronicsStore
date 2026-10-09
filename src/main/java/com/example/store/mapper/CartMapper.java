package com.example.store.mapper;

import com.example.store.dto.respone.CartDTO;
import com.example.store.dto.respone.CartItemDTO;
import com.example.store.entity.Cart;
import java.time.LocalDate;

import java.util.List;

public class CartMapper {
    private final CartItemMapper cartItemMapper = new CartItemMapper();

    public CartDTO toDTO(Cart cart) {
        List<CartItemDTO> items = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream()
                        .map(cartItemMapper::toCartItemDTO)
                        .toList();
        LocalDate updatedAt = cart.getUpdatedAt() == null
                ? null
                : cart.getUpdatedAt().toLocalDate();
        return CartDTO.of(cart.getId(), items, updatedAt);
    }
}
