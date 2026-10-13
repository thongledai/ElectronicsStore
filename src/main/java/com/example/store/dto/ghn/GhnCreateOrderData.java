package com.example.store.dto.ghn;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GhnCreateOrderData {
    @JsonProperty("order_code") private String orderCode;
    @JsonProperty("total_fee") private BigDecimal totalFee;
    @JsonProperty("expected_delivery_time") private Instant expectedDeliveryTime;
}
