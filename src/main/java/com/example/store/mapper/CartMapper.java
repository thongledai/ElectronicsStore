package com.example.store.mapper;

import com.example.store.dto.respone.CartDTO;
import com.example.store.dto.respone.CartItemDTO;
import com.example.store.entity.Cart;

import java.util.List;

public class CartMapper {
    public CartDTO toDTO(Cart cart) {
        List<CartItemDTO> items = cart.getItems().stream().map(this::toItemDTO).toList();
    }
}
