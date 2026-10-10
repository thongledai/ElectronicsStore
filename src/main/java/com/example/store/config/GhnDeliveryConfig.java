package com.example.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GhnDeliveryConfig {
    @Value("${ghn.base-url}")
    private String baseUrl;

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private String shopId;

    @Bean
    public RestClient ghnRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Token", token)
                .defaultHeader("ShopId", shopId)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
