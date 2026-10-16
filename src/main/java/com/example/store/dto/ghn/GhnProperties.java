package com.example.store.dto.ghn;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ghn")
public record GhnProperties(
        String baseUrl,
        String token,
        Integer shopId,
        Integer fromDistrictId,
        String fromWardCode
) {}