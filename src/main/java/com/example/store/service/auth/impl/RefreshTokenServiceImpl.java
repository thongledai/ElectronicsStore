package com.example.store.service.auth.impl;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.entity.RefreshToken;
import com.example.store.entity.User;
import com.example.store.exception.BusinessException;
import com.example.store.repository.RefreshTokenRepository;
import com.example.store.repository.UserRepository;
import com.example.store.service.auth.IRefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements IRefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh.expiration:2700000}")
    private long refreshExpiration; // Mặc định 45 phút trượt

    @Value("${jwt.refresh.remember-me-expiration:2592000000}")
    private long rememberMeExpiration; // Mặc định 30 ngày

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, boolean rememberMe) {
        long duration = getRefreshTokenDuration(rememberMe);
        Instant expiryDate = Instant.now().plusMillis(duration);

        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiryDate(expiryDate)
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            throw new BusinessException("Refresh token đã bị vô hiệu hóa hoặc thu hồi. Vui lòng đăng nhập lại!");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new BusinessException("Phiên đăng nhập đã hết hạn (quá 45 phút không hoạt động). Vui lòng đăng nhập lại!");
        }

        return token;
    }

    @Override
    @Transactional
    public RefreshToken rotateRefreshToken(String tokenStr) {
        if (tokenStr == null || tokenStr.trim().isEmpty()) {
            throw new BusinessException("Refresh token không được để trống!");
        }

        RefreshToken oldToken = refreshTokenRepository.findByToken(tokenStr.trim())
                .orElseThrow(() -> new BusinessException("Refresh token không tồn tại hoặc đã bị xóa. Vui lòng đăng nhập lại!"));

        // Chống tấn công Replay Attack: Nếu token đã từng bị thu hồi/dùng rồi mà lại được gửi lại
        if (oldToken.isRevoked()) {
            log.warn("CẢNH BÁO BẢO MẬT: Phát hiện tái sử dụng Refresh Token đã bị thu hồi của user {}", oldToken.getUser().getEmail());
            refreshTokenRepository.revokeAllByUser(oldToken.getUser());
            throw new BusinessException("Phát hiện bất thường về phiên làm việc. Toàn bộ phiên đã bị thu hồi để đảm bảo an toàn, vui lòng đăng nhập lại!");
        }

        // Kiểm tra thời hạn
        verifyExpiration(oldToken);

        // Vô hiệu hóa token cũ
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Kiểm tra xem token cũ có phải loại Remember Me (thời hạn > 2 ngày) không
        boolean wasRememberMe = oldToken.getExpiryDate().isAfter(Instant.now().plusSeconds(2 * 24 * 3600));

        // Sinh token mới với hạn trượt mới
        return createRefreshToken(oldToken.getUser(), wasRememberMe);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        if (userId != null) {
            userRepository.findById(userId).ifPresent(refreshTokenRepository::revokeAllByUser);
        }
    }

    @Override
    @Transactional
    public void revokeToken(String tokenStr) {
        if (tokenStr != null && !tokenStr.isBlank()) {
            refreshTokenRepository.findByToken(tokenStr.trim()).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Override
    public long getRefreshTokenDuration(boolean rememberMe) {
        return rememberMe ? rememberMeExpiration : refreshExpiration;
    }
}
