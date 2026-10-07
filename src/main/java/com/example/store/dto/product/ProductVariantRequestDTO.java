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

    @NotBlank(message = "SKU không được để trống")
    @Size(max = 100, message = "SKU không được vượt quá 100 ký tự")
    private String sku;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(
            value = "0.0",
            message = "Giá sản phẩm không được nhỏ hơn 0"
    )
    private BigDecimal price;

    @DecimalMin(
            value = "0.0",
            message = "Giá khuyến mãi không được nhỏ hơn 0"
    )
    private BigDecimal promotionalPrice;

    @NotNull(message = "Số lượng sản phẩm không được để trống")
    @Min(
            value = 0,
            message = "Số lượng sản phẩm không được nhỏ hơn 0"
    )
    private Integer quantity;

    private Boolean isActive;

    private Boolean isSelling;

    private Set<UUID> styleValueIds;

    @AssertTrue(message = "Giá khuyến mãi phải nhỏ hơn giá sản phẩm")
    @JsonIgnore
    public boolean isPromotionalPriceValid() {
        return promotionalPrice == null
                || price == null
                || promotionalPrice.compareTo(price) < 0;
    }
}