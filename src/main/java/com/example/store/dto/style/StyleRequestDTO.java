package com.example.store.dto.style;

import java.util.Set;
import java.util.UUID;

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
public class StyleRequestDTO {

    @NotBlank(message = "STYLE_NAME_REQUIRED")
    @Size(
            max = 100,
            message = "STYLE_NAME_MAX_LENGTH"
    )
    private String name;

    private Set<UUID> categoryIds;

    private Boolean isActive;
}