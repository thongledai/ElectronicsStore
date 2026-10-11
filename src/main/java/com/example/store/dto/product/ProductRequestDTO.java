package com.example.store.dto.product;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDTO {

    @NotBlank(message = "PRODUCT_NAME_REQUIRED")
    @Size(max = 100, message = "PRODUCT_NAME_MAX_LENGTH")
    private String name;

    @NotBlank(message = "PRODUCT_DESCRIPTION_REQUIRED")
    @Size(max = 1000, message = "PRODUCT_DESCRIPTION_MAX_LENGTH")
    private String description;

    @NotNull(message = "PRODUCT_CATEGORY_REQUIRED")
    private UUID categoryId;

    @NotNull(message = "PRODUCT_BRAND_REQUIRED")
    private Long brandId;

    private Boolean isSelling;

    private Boolean isActive;
}
