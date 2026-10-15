package com.example.store.controller.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.ghn.GhnOrderStatusCallbackDTO;
import com.example.store.service.delivery.IGhnWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/webhooks/ghn")
@RequiredArgsConstructor
@Slf4j
public class GhnWebhookController {
    private final IGhnWebhookService webhookService;

    @Value("${ghn.webhook-secret:}")
    private String webhookSecret;

    @PostMapping("/order-status")
    public ResponseEntity<Void> orderStatus(
            @RequestHeader(name = "X-GHN-Webhook-Secret", required = false) String suppliedSecret,
            @RequestBody GhnOrderStatusCallbackDTO callback) {
        if (!StringUtils.hasText(webhookSecret) || !constantTimeEquals(webhookSecret, suppliedSecret)) {
            log.warn("Rejected GHN callback due to missing/invalid configured custom header.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            webhookService.handle(callback);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException ex) {
            // 5xx để GHN retry; callback chưa liên kết được không được ACK thành công.
            log.warn("GHN callback could not be linked to an internal order: orderCode={}",
                    callback == null ? null : callback.getOrderCode());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (RuntimeException ex) {
            log.error("GHN callback processing failed: orderCode={}",
                    callback == null ? null : callback.getOrderCode(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null)
            return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
