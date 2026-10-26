package com.example.store.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thông tin chuyển khoản của một đơn VietQR.
 * qrImageUrl chỉ có khi đơn còn chờ thanh toán (paymentStatus = UNPAID, chưa hết hạn).
 */
public record VietQrPaymentDTO(
        UUID orderId,
        String orderCode,
        String paymentStatus,
        BigDecimal amount,
        String transferContent,
        String bankName,
        String accountNumber,
        String accountName,
        String qrImageUrl,
        LocalDateTime expiresAt,
        long secondsRemaining,
        LocalDateTime paidAt
) {}
