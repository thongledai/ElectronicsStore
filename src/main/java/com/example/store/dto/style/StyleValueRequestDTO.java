package com.example.store.dto.style;

import java.util.UUID;

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
public class StyleValueRequestDTO {

    @NotBlank(message = "STYLE_VALUE_NAME_REQUIRED")
    @Size(
            max = 100,
            message = "STYLE_VALUE_NAME_MAX_LENGTH"
    )
    private String name;

    @NotNull(message = "STYLE_VALUE_STYLE_REQUIRED")
    private UUID styleId;

    private Boolean isActive;
}