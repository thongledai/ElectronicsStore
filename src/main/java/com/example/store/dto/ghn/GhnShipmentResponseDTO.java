package com.example.store.dto.ghn;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value @Builder
public class GhnShipmentResponseDTO {
    UUID orderId;
    String orderStatus;
    String ghnOrderCode;
    String ghnStatus;
    BigDecimal ghnShippingFee;
    LocalDateTime expectedDeliveryTime;
    boolean alreadyCreated;
}
