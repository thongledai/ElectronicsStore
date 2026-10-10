package com.example.store.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandRequestDTO {

    @NotBlank(message = "BRAND_NAME_REQUIRED")
    @Size(max = 100, message = "BRAND_NAME_MAX_LENGTH")
    private String name;

    @Size(max = 1000, message = "BRAND_LOGO_URL_MAX_LENGTH")
    private String logoUrl;

    @Size(max = 1000, message = "DESCRIPTION_MAX_LENGTH")
    private String description;

    private Boolean isActive;
}