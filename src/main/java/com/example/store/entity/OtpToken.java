package com.example.store.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.store.enums.OtpType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "otp_tokens", indexes = @Index(name = "idx_otp_email_type", columnList = "email,type"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(nullable = false, length = 100)
    private String otpHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpType type;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;
    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}