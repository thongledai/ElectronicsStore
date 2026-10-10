package com.example.store.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.example.store.enums.PromotionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Mã giảm giá khách nhập/chọn
    @Column(nullable = false, unique = true, length = 50, columnDefinition = "nvarchar(50)")
    private String code;

    @Column(length = 1000, columnDefinition = "nvarchar(1000)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionType type;

    // Phần trăm giảm (0 - 100)
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    // Mức giảm tối đa (null = không giới hạn)
    @Column(precision = 18, scale = 2)
    private BigDecimal maxDiscountAmount;

    // Giá trị đơn hàng tối thiểu để áp dụng
    @Builder.Default
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    // Số lượt sử dụng còn lại
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isEnabled = true;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}