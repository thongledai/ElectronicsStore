package com.example.store.service.wishlist;

import java.util.List;
import java.util.UUID;

import com.example.store.dto.wishlist.WishlistItemDTO;
import com.example.store.dto.wishlist.WishlistToggleResponseDTO;

public interface IWishlistService {

    List<WishlistItemDTO> getWishlist(UUID userId);

    List<UUID> getWishlistProductIds(UUID userId);

    WishlistToggleResponseDTO toggleWishlist(UUID userId, UUID productId);

    void removeFromWishlist(UUID userId, UUID productId);

    void clearWishlist(UUID userId);

    long getWishlistCount(UUID userId);
}
