package com.example.store.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class GhnDeliveryConfig {
    @Bean("ghnRestClient")
    RestClient ghnRestClient(
            @Value("${ghn.base-url:https://dev-online-gateway.ghn.vn}") String baseUrl,
            @Value("${ghn.token:}") String token,
            @Value("${ghn.shop-id:}") String shopId) {
        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader("Token", token)
                .defaultHeader("ShopId", shopId)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
