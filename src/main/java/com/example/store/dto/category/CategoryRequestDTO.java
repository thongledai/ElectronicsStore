package com.example.store.dto.category;

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
public class CategoryRequestDTO {

    @NotBlank(message = "CATEGORY_NAME_REQUIRED")
    @Size(max = 100, message = "CATEGORY_NAME_MAX_LENGTH")
    private String name;

    private UUID parentId;

    @Size(max = 1000, message = "CATEGORY_IMAGE_URL_MAX_LENGTH")
    private String image;

    private Boolean isActive;
}