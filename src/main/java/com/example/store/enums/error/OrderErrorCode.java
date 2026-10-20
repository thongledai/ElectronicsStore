package com.example.store.enums.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    DELIVERY_NOT_AVAILABLE(5008, "Phương thức giao hàng không khả dụng"),
    PAYMENT_METHOD_NOT_SUPPORTED(5009, "Phương thức thanh toán không khả dụng"),
    ORDER_STATUS_NOT_CONFIGURED(5010, "Hệ thống chưa cấu hình trạng thái đơn hàng"),
    ORDER_NOT_FOUND(5011, "Không tìm thấy đơn hàng"),
    ORDER_CANNOT_BE_CANCELLED(5012, "Đơn hàng không thể hủy ở trạng thái hiện tại");

    private final int code;
    private final String message;
}
