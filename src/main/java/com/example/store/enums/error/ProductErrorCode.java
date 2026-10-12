package com.example.store.enums.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    VARIANT_NOT_FOUND(3002, "Không tìm thấy sản phẩm"),
    PRODUCT_NOT_AVAILABLE(3003, "Sản phẩm hiện không được bán"),
    INSUFFICIENT_STOCK(3102, "Số lượng vượt quá tồn kho"),
    ;

    private final int code;
    private final String message;
}
