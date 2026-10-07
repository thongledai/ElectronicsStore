package com.example.store.dto.product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.store.dto.brand.BrandResponseDTO;
import com.example.store.dto.category.CategoryResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailResponseDTO {

    private UUID id;

    private String name;

    private String slug;

    private String description;

    private Boolean isActive;

    private Boolean isSelling;

    private Double rating;

    private CategoryResponseDTO category;

    private BrandResponseDTO brand;

    private List<ProductVariantResponseDTO> variants;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}