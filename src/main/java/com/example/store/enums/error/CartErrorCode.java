package com.example.store.enums.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CartErrorCode implements ErrorCode {
    CART_EMPTY(4003, "Giỏ hàng đang trống"),               // dùng khi checkout
    CART_ITEM_NOT_FOUND(4002, "Không tìm thấy sản phẩm trong giỏ"),// dùng khi sửa/xóa
    CART_ITEM_LIMIT_EXCEEDED(4006, "Số lượng tối đa")
    ;

    private final int code;
    private final String message;
}
