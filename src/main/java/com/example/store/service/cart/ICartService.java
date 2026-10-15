package com.example.store.service.cart;

import com.example.store.dto.cart.AddToCartRequest;
import com.example.store.dto.cart.UpdateCartItemRequest;
import com.example.store.dto.cart.CartItemDTO;
import com.example.store.entity.CartItem;

import java.util.List;
import java.util.UUID;

public interface ICartService {
    List<CartItemDTO> getCart(UUID userId);
    CartItemDTO addToCart(UUID userId, AddToCartRequest request);
    CartItemDTO updateQuantity(UUID userId, UUID itemId, UpdateCartItemRequest request);
    void removeItem(UUID userId, UUID itemId);
    void removeItems(UUID userId, List<UUID> itemIds);
    void clearCart(UUID userId);

    List<CartItem> getOwnedItems(UUID userId, List<UUID> itemIds);
    long countItems(UUID userId);
}
