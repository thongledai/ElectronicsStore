package com.example.store.service;

import com.example.store.entity.User;

public interface IJwtService {
    String generateToken(User user);
    String generateToken(User user, long expirationMillis);
    boolean validateToken(String token);
    String extractUsername(String token);
    String extractRole(String token);
    long getExpirationTime();
}
