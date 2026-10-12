package com.example.store.exception;

import com.example.store.enums.BusinessMessage;
import com.example.store.enums.CloudinaryMessage;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(BusinessMessage message, Object... args) {
        super(message != null ? message.format(args) : null);
    }

    public BusinessException(CloudinaryMessage message, Object... args) {
        super(message != null ? message.format(args) : null);
    }
}
