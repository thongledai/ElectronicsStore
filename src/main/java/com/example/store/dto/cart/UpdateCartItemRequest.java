package com.example.store.dto.cart;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(value = 1, message = "Số lượng phải từ 1 trở lên")
        int count
) {}
