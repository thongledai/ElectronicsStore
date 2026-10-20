package com.example.store.enums.error;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    DELIVERY_NOT_AVAILABLE(5008, "Phương thức giao hàng không khả dụng"),
    PAYMENT_METHOD_NOT_SUPPORTED(5009, "Phương thức thanh toán không khả dụng"),
    ORDER_STATUS_NOT_CONFIGURED(5010, "Hệ thống chưa cấu hình trạng thái đơn hàng")
    ;

    private final int code;
    private final String message;
}
