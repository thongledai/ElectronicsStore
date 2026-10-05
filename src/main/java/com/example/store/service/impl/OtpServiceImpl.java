package com.example.store.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.store.entity.OtpToken;
import com.example.store.enums.OtpType;
import com.example.store.repository.OtpTokenRepository;
import com.example.store.service.IOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements IOtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public String generateAndSaveOtp(String email, OtpType type) {
        // Invalidate older unused tokens for this email and type
        List<OtpToken> oldTokens = otpTokenRepository.findAllByEmailAndTypeAndUsedFalse(email, type);
        for (OtpToken old : oldTokens) {
            old.setUsed(true);
        }
        otpTokenRepository.saveAll(oldTokens);

        // Generate 6 digit numeric OTP
        int number = secureRandom.nextInt(1_000_000);
        String rawOtp = String.format("%06d", number);

        OtpToken otpToken = OtpToken.builder()
                .email(email.toLowerCase().trim())
                .otpHash(passwordEncoder.encode(rawOtp))
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attempts(0)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        otpTokenRepository.save(otpToken);
        log.info("Generated OTP for email: {} with type: {}", email, type);
        return rawOtp;
    }

    @Override
    @Transactional
    public boolean verifyOtp(String email, String rawOtp, OtpType type) {
        if (email == null || rawOtp == null) {
            return false;
        }

        Optional<OtpToken> otpOpt = otpTokenRepository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(
                email.toLowerCase().trim(), type);

        if (otpOpt.isEmpty()) {
            log.warn("No active OTP found for email: {} and type: {}", email, type);
            return false;
        }

        OtpToken token = otpOpt.get();

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for email: {}", email);
            token.setUsed(true);
            otpTokenRepository.save(token);
            return false;
        }

        if (token.getAttempts() >= 5) {
            log.warn("OTP exceeded max attempts for email: {}", email);
            token.setUsed(true);
            otpTokenRepository.save(token);
            return false;
        }

        boolean matches = passwordEncoder.matches(rawOtp, token.getOtpHash());
        if (matches) {
            token.setUsed(true);
            otpTokenRepository.save(token);
            log.info("OTP successfully verified for email: {} and type: {}", email, type);
            return true;
        } else {
            token.setAttempts(token.getAttempts() + 1);
            otpTokenRepository.save(token);
            log.warn("Invalid OTP entered for email: {}, attempts: {}", email, token.getAttempts());
            return false;
        }
    }
}
