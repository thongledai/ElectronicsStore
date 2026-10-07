package com.example.store.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.store.dto.style.StyleValueResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponseDTO {

    private UUID id;

    private UUID productId;

    private String productName;

    private String sku;

    private BigDecimal price;

    private BigDecimal promotionalPrice;

    private Integer quantity;

    private Integer sold;

    private Boolean isActive;

    private Boolean isSelling;

    private List<String> imageUrls;

    private List<StyleValueResponseDTO> styleValues;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}