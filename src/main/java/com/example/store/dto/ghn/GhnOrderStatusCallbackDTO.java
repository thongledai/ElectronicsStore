package com.example.store.dto.ghn;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GhnOrderStatusCallbackDTO {
    @JsonProperty("ShopID") private Integer shopId;
    @JsonProperty("Time") private Instant time;
    @JsonProperty("OrderCode") private String orderCode;
    @JsonProperty("ClientOrderCode") private String clientOrderCode;
    @JsonProperty("Type") private String type;
    @JsonProperty("Status") private String status;
    @JsonProperty("Description") private String description;
    @JsonProperty("Reason") private String reason;
    @JsonProperty("ReasonCode") private String reasonCode;
}
