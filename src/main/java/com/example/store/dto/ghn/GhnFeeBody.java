package com.example.store.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GhnFeeBody(
        @JsonProperty("service_type_id") Integer serviceTypeId,
        @JsonProperty("to_district_id") Integer toDistrictId,
        @JsonProperty("to_ward_code") String toWardCode,
        Integer weight
) {}
