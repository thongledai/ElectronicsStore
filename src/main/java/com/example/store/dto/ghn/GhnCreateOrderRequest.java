package com.example.store.dto.ghn;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GhnCreateOrderRequest {
    @JsonProperty("payment_type_id") private Integer paymentTypeId;
    @JsonProperty("required_note") private String requiredNote;
    @JsonProperty("client_order_code") private String clientOrderCode;
    @JsonProperty("to_name") private String toName;
    @JsonProperty("to_phone") private String toPhone;
    @JsonProperty("to_address") private String toAddress;
    @JsonProperty("to_ward_name") private String toWardName;
    @JsonProperty("to_province_name") private String toProvinceName;
    @JsonProperty("is_new_to_address") private Boolean isNewToAddress;
    @JsonProperty("service_type_id") private Integer serviceTypeId;
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    @JsonProperty("content") private String content;
    @JsonProperty("cod_amount") private Integer codAmount;
    @JsonProperty("order_value") private Integer orderValue;
    private List<Item> items;

    @Getter @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        private String name;
        private String code;
        private Integer quantity;
        private Integer weight;
        private Integer length;
        private Integer width;
        private Integer height;
    }
}
