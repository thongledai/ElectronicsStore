package com.example.store.dto.auth;

import com.example.store.dto.common.UserResponseDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private long expiresIn;
    private String redirectUrl;
    private UserResponseDTO user;
}
