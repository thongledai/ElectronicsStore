package com.example.store.enums.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ShippingErrorCode implements ErrorCode {
    ADDRESS_INVALID(8002, "Địa chỉ giao hàng không hợp lệ"),
    GHN_SERVICE_ERROR(9006, "Không tính được phí vận chuyển, vui lòng thử lại sau"),
    SHIPMENT_FEE_CHANGED(9007, "Phí vận chuyển thay đổi"),
    ;

    private final int code;
    private final String message;
}
