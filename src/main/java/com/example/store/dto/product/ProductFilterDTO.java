package com.example.store.dto.product;

import java.math.BigDecimal;
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

    private UUID category;

    private Long brand;

    @DecimalMin(
            value = "0.0",
            message = "Giá thấp nhất không được nhỏ hơn 0"
    )
    private BigDecimal minPrice;

    @DecimalMin(
            value = "0.0",
            message = "Giá cao nhất không được nhỏ hơn 0"
    )
    private BigDecimal maxPrice;

    @DecimalMin(
            value = "0.0",
            message = "Đánh giá thấp nhất không được nhỏ hơn 0"
    )
    @DecimalMax(
            value = "5.0",
            message = "Đánh giá thấp nhất không được lớn hơn 5"
    )
    private Double minRating;

    private Boolean inStock;

    private Boolean onSale;

    private String sort;

    @Min(
            value = 0,
            message = "Số trang không được nhỏ hơn 0"
    )
    @Builder.Default
    private Integer page = 0;

    @Min(
            value = 1,
            message = "Kích thước trang phải lớn hơn 0"
    )
    @Max(
            value = 100,
            message = "Kích thước trang không được lớn hơn 100"
    )
    @Builder.Default
    private Integer size = 10;
}