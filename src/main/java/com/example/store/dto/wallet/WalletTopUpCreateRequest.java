package com.example.store.dto.wallet;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record WalletTopUpCreateRequest(
        @NotNull(message = "Vui lòng nhập số tiền cần nạp")
        @DecimalMin(value = "1.00", message = "Số tiền nạp phải lớn hơn 0")
        @Digits(integer = 16, fraction = 2, message = "Số tiền nạp không hợp lệ")
        BigDecimal amount
) {}
