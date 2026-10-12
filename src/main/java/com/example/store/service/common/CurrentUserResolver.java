package com.example.store.service.common;

import com.example.store.service.auth.IJwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final IJwtService jwtService;

    public UUID getUserId(String token) {
        if (token == null || !jwtService.validateToken(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        return UUID.fromString(jwtService.extractUserId(token));
    }
}
