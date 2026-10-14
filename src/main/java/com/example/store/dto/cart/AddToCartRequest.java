package com.example.store.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Builder
public record AddToCartRequest(
    @NotNull(message = "Vui lòng chọn phân loại hàng")
    UUID productVariantId,

    @Min(value = 1, message = "Số lượng phải từ 1 trở lên")
    int count
) {}
