package com.example.store.exception;

import com.example.store.enums.BusinessMessage;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(BusinessMessage message, Object... args) {
        super(message != null ? message.format(args) : null);
    }
}
