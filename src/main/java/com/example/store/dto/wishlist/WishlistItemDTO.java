package com.example.store.dto.wishlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemDTO {
    private UUID productId;
    private UUID defaultVariantId;
    private String name;
    private String slug;
    private String description;
    private String thumbnailUrl;
    private String brandName;
    private String categoryName;
    private Double rating;
    private BigDecimal originalPrice;
    private BigDecimal promotionalPrice;
    private BigDecimal price;
    private Integer stock;
    private Boolean isSelling;
    private Boolean isActive;
    private LocalDateTime addedAt;
}
