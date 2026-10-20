package com.example.store.dto.ghn;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GhnShipmentCreateRequestDTO {
    @Min(1) @Max(19999) private Integer weight;
    @Min(1) @Max(200) private Integer length;
    @Min(1) @Max(200) private Integer width;
    @Min(1) @Max(200) private Integer height;
}
