package com.example.store.service.cart;

import com.example.store.dto.request.AddToCartRequest;
import com.example.store.dto.request.UpdateCartItemRequest;
import com.example.store.dto.respone.CartItemDTO;

import java.util.List;
import java.util.UUID;

public interface ICartService {
    List<CartItemDTO> getCart(UUID userId);
    CartItemDTO addToCart(UUID userId, AddToCartRequest request);
    CartItemDTO updateQuantity(UUID userId, UUID itemId, UpdateCartItemRequest request);
    void removeItem(UUID userId, UUID itemId);
    void removeItems(UUID userId, List<UUID> itemIds);
    void clearCart(UUID userId);
}
