package com.example.store.enums.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    DELIVERY_NOT_AVAILABLE(5008, "Phương thức giao hàng không khả dụng"),
    PAYMENT_METHOD_NOT_SUPPORTED(5009, "Phương thức thanh toán không khả dụng"),
    ;

    private final int code;
    private final String message;
}
