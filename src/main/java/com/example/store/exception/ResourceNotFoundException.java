package com.example.store.exception;

import com.example.store.enums.BusinessMessage;
import com.example.store.enums.CloudinaryMessage;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(BusinessMessage message, Object... args) {
        super(message != null ? message.format(args) : null);
    }

    public ResourceNotFoundException(CloudinaryMessage message, Object... args) {
        super(message != null ? message.format(args) : null);
    }
}
