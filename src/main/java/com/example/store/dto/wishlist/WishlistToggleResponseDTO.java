package com.example.store.dto.wishlist;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistToggleResponseDTO {
    private UUID productId;
    private boolean wishlisted;
    private long totalCount;
    private String message;
}
