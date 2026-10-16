package com.example.store.dto.ghn;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CalculateShipmentFeeRequest(
        @NotNull @Min(1) Integer weight,        // gram
        @NotNull Integer districtId,
        @NotBlank String wardCode,
        @NotNull Integer serviceTypeId
) {}
