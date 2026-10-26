package com.example.store.dto.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTopUpDTO(
        UUID id,
        BigDecimal requestedAmount,
        BigDecimal receivedAmount,
        String status,
        String transferContent,
        String bankName,
        String accountNumber,
        String accountName,
        String qrImageUrl,
        LocalDateTime expiresAt,
        long secondsRemaining,
        LocalDateTime paidAt
) {}
