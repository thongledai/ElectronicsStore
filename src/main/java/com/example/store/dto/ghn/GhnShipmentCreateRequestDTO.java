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
    @Min(1) @Max(2) private Integer paymentTypeId;
    private String requiredNote;
    @Min(1) private Integer provinceId;
    @Min(1) private Integer wardId;
    @Size(max = 500) private String addressDetail;
}
