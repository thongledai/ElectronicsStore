package com.example.store.controller.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.config.PaymentProperties;
import com.example.store.dto.payment.SepayWebhookPayload;
import com.example.store.service.payment.IVietQrPaymentService;
import com.example.store.service.payment.IVietQrPaymentService.TransferResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Nhận webhook "tiền vào" từ SePay. Đây là NGUỒN DUY NHẤT được phép đánh dấu đơn đã thanh toán.
 * Không dùng session/JWT của người dùng; xác thực bằng header "Authorization: Apikey <SEPAY_API_KEY>".
 */
@RestController
@RequestMapping("/api/webhooks/sepay")
@RequiredArgsConstructor
@Slf4j
public class SepayWebhookController {
    private static final Map<String, Object> ACK = Map.of("success", true);

    private final PaymentProperties props;
    private final IVietQrPaymentService paymentService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> receive(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody SepayWebhookPayload payload) {

        if (!isAuthorized(authorization)) {
            log.warn("Từ chối webhook SePay: thiếu hoặc sai API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        try {
            TransferResult result = paymentService.handleSepayTransfer(payload);
            log.info("Webhook SePay id={} -> {}", payload.getId(), result);
            if (result == TransferResult.PAID || result == TransferResult.DUPLICATE_EVENT) {
                return ResponseEntity.ok(ACK);
            }
            // SePay chỉ coi webhook thành công khi nhận 2xx cùng success=true; trả lỗi để retry/hiển thị Failed.
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("success", false, "result", result.name()));
        } catch (IllegalArgumentException bad) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        } catch (RuntimeException ex) {
            // 5xx để SePay gọi lại; không ACK khi chưa xử lý được
            log.error("Xử lý webhook SePay id={} thất bại", payload.getId(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false));
        }
    }

    private boolean isAuthorized(String authorization) {
        String key = props.getSepayApiKey();
        if (!StringUtils.hasText(key)) {
            return false; // chưa cấu hình khóa => đóng hoàn toàn (fail closed)
        }
        if (authorization == null) {
            return false;
        }
        byte[] expected = ("Apikey " + key).getBytes(StandardCharsets.UTF_8);
        byte[] actual = authorization.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
}
