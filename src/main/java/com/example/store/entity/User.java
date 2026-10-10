package com.example.store.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200, columnDefinition = "nvarchar(200)")
    private String fullName;

    @Column(unique = true)
    private String slug;

    @Column(unique = true, length = 20)
    private String idCard;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(unique = true, length = 15)
    private String phone;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isEmailActive = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPhoneActive = false;

    @Column(nullable = true)
    private String hashedPassword;

    // Cần load role ngay khi đăng nhập (Spring Security) nên dùng EAGER
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // Nhiều địa chỉ lưu trong 1 chuỗi (tối đa 6 địa chỉ, mỗi địa chỉ <= 200 ký tự), tách bằng ký tự ngăn cách
    @Column(length = 1200, columnDefinition = "nvarchar(1200)")
    private String addresses;

    @Column(length = 1000)
    private String avatar;

    @Builder.Default
    @Column(nullable = false)
    private Integer point = 0;

    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal eWallet = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
}