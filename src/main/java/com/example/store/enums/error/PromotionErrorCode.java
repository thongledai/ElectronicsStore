package com.example.store.enums.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PromotionErrorCode implements ErrorCode {
    VOUCHER_NOT_FOUND(6001, "Mã giảm giá không tồn tại"),
    VOUCHER_DISABLED(6002, "Mã giảm giá hiện đang bị tạm khóa"),
    VOUCHER_NOT_STARTED(6003, "Mã giảm giá chưa đến thời gian áp dụng"),
    VOUCHER_EXPIRED(6004, "Mã giảm giá đã hết hạn sử dụng"),
    VOUCHER_OUT_OF_STOCK(6005, "Mã giảm giá đã hết lượt sử dụng"),
    VOUCHER_MIN_ORDER_NOT_MET(6006, "Đơn hàng chưa đạt giá trị tối thiểu để áp dụng mã giảm giá này");

    private final int code;
    private final String message;
}
