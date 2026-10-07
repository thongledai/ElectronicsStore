package com.example.store.dto.product;

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
public class ProductResponseDTO {

    private UUID id;

    private String name;

    private String slug;

    private String description;

    private Boolean isActive;

    private Boolean isSelling;

    private Double rating;

    private UUID categoryId;

    private String categoryName;

    private String categorySlug;

    private Long brandId;

    private String brandName;

    private String brandSlug;

    private Integer variantCount;

    private BigDecimal minPrice;

    private BigDecimal maxPrice;

    private String thumbnailUrl;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}