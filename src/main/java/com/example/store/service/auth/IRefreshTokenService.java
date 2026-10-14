package com.example.store.service.auth;

import java.util.UUID;
import com.example.store.entity.RefreshToken;
import com.example.store.entity.User;

public interface IRefreshTokenService {
    RefreshToken createRefreshToken(User user, boolean rememberMe);
    RefreshToken verifyExpiration(RefreshToken token);
    RefreshToken rotateRefreshToken(String tokenStr);
    void revokeAllByUserId(UUID userId);
    void revokeToken(String tokenStr);
    long getRefreshTokenDuration(boolean rememberMe);
}
