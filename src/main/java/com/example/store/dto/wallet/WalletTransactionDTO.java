package com.example.store.dto.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTransactionDTO(
        UUID id,
        String type,
        boolean moneyIn,
        BigDecimal amount,
        String reference,
        UUID orderId,
        LocalDateTime createdAt
) {}
