package com.example.store.service;

import com.example.store.enums.OtpType;

public interface IOtpService {
    String generateAndSaveOtp(String email, OtpType type);
    boolean verifyOtp(String email, String rawOtp, OtpType type);
}
