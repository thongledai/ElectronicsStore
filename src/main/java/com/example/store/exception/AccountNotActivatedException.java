package com.example.store.exception;

public class AccountNotActivatedException extends RuntimeException {
    private final String email;

    public AccountNotActivatedException(String email) {
        super("ACCOUNT_NOT_ACTIVATED");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
