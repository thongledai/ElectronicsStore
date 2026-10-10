package com.example.store.service.auth;

import com.example.store.entity.User;

public interface IJwtService {
    String generateToken(User user);
    String generateToken(User user, long expirationMillis);
    boolean validateToken(String token);
    String extractEmail(String token);
    String extractRole(String token);
    long getExpirationTime();
    String extractUserId(String token);
    String extractFullName(String token);
}
