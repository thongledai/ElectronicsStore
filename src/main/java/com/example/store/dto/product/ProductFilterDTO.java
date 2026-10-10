package com.example.store.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterDTO {

    private String q;

    private String search;

    // Hỗ trợ truyền 1 hoặc nhiều category theo slug hoặc UUID
    private List<String> category;

    // Hỗ trợ truyền 1 hoặc nhiều brand theo slug hoặc id
    private List<String> brand;

    private UUID categoryId;

        private Long brandId;

    @DecimalMin(
            value = "0.0",
            message = "MIN_PRICE_NON_NEGATIVE"
    )
    private BigDecimal minPrice;

    @DecimalMin(
            value = "0.0",
            message = "MAX_PRICE_NON_NEGATIVE"
    )
    private BigDecimal maxPrice;

    @DecimalMin(
            value = "0.0",
            message = "MIN_RATING_NON_NEGATIVE"
    )
    @DecimalMax(
            value = "5.0",
            message = "MIN_RATING_MAX_FIVE"
    )
    private Double minRating;

    private Boolean inStock;

    private Boolean onSale;

    private String sort;

    @Min(
            value = 0,
            message = "PAGE_NON_NEGATIVE"
    )
    @Builder.Default
    private Integer page = 0;

    @Min(
            value = 1,
            message = "PAGE_SIZE_POSITIVE"
    )
    @Max(
            value = 50,
            message = "PAGE_SIZE_MAX_FIFTY"
    )
    @Builder.Default
    private Integer size = 12;

    public String getKeyword() {
        if (q != null && !q.trim().isEmpty()) {
            return q.trim();
        }
        if (search != null && !search.trim().isEmpty()) {
            return search.trim();
        }
        return null;
    }
}