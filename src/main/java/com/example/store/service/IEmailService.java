package com.example.store.service;

public interface IEmailService {
    void sendOtpEmail(String toEmail, String otp, String subject, String actionDescription);
}
