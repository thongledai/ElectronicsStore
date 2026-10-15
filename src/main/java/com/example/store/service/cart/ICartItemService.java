package com.example.store.service.cart;

import com.example.store.dto.cart.AddToCartRequest;
import com.example.store.dto.cart.CartItemDTO;

import java.util.List;
import java.util.UUID;

public interface ICartItemService {
    CartItemDTO addItemToCart(UUID userId, AddToCartRequest request);
    CartItemDTO updateQuantity(UUID userId, CartItemDTO cartItem, int quantity);
    void removeItemFromCart(UUID userId, CartItemDTO cartItem);
    List<CartItemDTO> getItemsByUserId(UUID userId);
}
