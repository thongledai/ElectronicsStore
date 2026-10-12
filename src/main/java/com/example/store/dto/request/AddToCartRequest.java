package com.example.store.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.UUID;

public record AddToCartRequest(
    @NotNull UUID productVariantId,
    @Min(1) int count
) {}
