package com.example.store.service.cart;

import com.example.store.dto.respone.CartItemDTO;

import java.util.List;
import java.util.UUID;

public interface ICartService {
    List<CartItemDTO> getCart(UUID userId);
    void clearCart(UUID userId);

}
