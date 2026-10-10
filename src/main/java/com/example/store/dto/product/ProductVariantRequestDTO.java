package com.example.store.dto.product;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class ProductVariantRequestDTO {

        @NotNull(message = "PRODUCT_PRICE_REQUIRED")
        @DecimalMin(value = "0.0", message = "PRODUCT_PRICE_NON_NEGATIVE")
        private BigDecimal price;

        @DecimalMin(value = "0.0", message = "PRODUCT_PROMOTIONAL_PRICE_NON_NEGATIVE")
        private BigDecimal promotionalPrice;

        @NotNull(message = "PRODUCT_QUANTITY_REQUIRED")
        @Min(value = 0, message = "PRODUCT_QUANTITY_NON_NEGATIVE")
        private Integer quantity;

        private Boolean isSelling;

        private Boolean isActive;

        private Set<UUID> styleValueIds;

        @AssertTrue(message = "PRODUCT_PROMOTIONAL_PRICE_BELOW_PRICE")
        @JsonIgnore
        public boolean isPromotionalPriceValid() {
                return promotionalPrice == null
                                || price == null
                                || promotionalPrice.compareTo(price) < 0;
        }
}
