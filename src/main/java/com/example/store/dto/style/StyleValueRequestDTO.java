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

    @NotBlank(message = "Tên giá trị thuộc tính không được để trống")
    @Size(
            max = 100,
            message = "Tên giá trị thuộc tính không được vượt quá 100 ký tự"
    )
    private String name;

    @NotNull(message = "Style không được để trống")
    private UUID styleId;

    private Boolean isDeleted;
}