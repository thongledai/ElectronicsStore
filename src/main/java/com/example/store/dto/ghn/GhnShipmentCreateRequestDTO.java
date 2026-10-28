package com.example.store.dto.ghn;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GhnShipmentCreateRequestDTO {
    @Min(value = 1, message = "SHIPMENT_WEIGHT_MIN")
    @Max(value = 19999, message = "SHIPMENT_WEIGHT_MAX")
    private Integer weight;

    @Min(value = 1, message = "SHIPMENT_LENGTH_MIN")
    @Max(value = 200, message = "SHIPMENT_LENGTH_MAX")
    private Integer length;

    @Min(value = 1, message = "SHIPMENT_WIDTH_MIN")
    @Max(value = 200, message = "SHIPMENT_WIDTH_MAX")
    private Integer width;

    @Min(value = 1, message = "SHIPMENT_HEIGHT_MIN")
    @Max(value = 200, message = "SHIPMENT_HEIGHT_MAX")
    private Integer height;
}
