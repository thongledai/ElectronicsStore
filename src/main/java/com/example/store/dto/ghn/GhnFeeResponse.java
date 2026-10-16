package com.example.store.dto.ghn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GhnFeeResponse(Integer code, String message, Data data) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(Integer total) {}
}
