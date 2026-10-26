package com.example.store.dto.wallet;

import java.math.BigDecimal;
import java.util.List;

public record WalletSummaryDTO(
        BigDecimal balance,
        List<WalletTransactionDTO> transactions
) {}
