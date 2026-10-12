package com.example.store.dto.request;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(1) int count
) {}
