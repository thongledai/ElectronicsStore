package com.example.store.enums.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ShippingErrorCode implements ErrorCode {
    ADDRESS_INVALID(8002, "Địa chỉ giao hàng không hợp lệ"),
    GHN_SERVICE_ERROR(9006, "Không tính được phí vận chuyển, vui lòng thử lại sau")
    ;

    private final int code;
    private final String message;
}
