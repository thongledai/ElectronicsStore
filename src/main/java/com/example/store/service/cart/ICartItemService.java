package com.example.store.service.cart;

import com.example.store.dto.request.AddToCartRequest;
import com.example.store.dto.respone.CartItemDTO;
import com.example.store.entity.ProductVariant;

import java.util.List;
import java.util.UUID;

public interface ICartItemService {
    CartItemDTO addItemToCart(UUID userId, AddToCartRequest request);
    CartItemDTO updateQuantity(UUID userId, CartItemDTO cartItem, int quantity);
    void removeItemFromCart(UUID userId, CartItemDTO cartItem);
    List<CartItemDTO> getItemsByUserId(UUID userId);
}
