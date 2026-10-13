package com.example.store.enums.error;

import lombok.*;

@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    AUTH_LOGIN_REQUIRED(1001, "Yêu cầu đăng nhập"),
    ;

    private final int code;
    private final String message;
}
