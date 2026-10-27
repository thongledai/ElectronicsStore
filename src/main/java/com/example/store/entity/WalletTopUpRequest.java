package com.example.store.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu nạp ví riêng biệt với đơn hàng. Một yêu cầu chỉ được ghi có một lần.
 */
@Entity
@Table(name = "wallet_top_up_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wallet_top_up_reference", columnNames = "payment_reference"),
        indexes = @Index(name = "idx_wallet_top_up_user_status", columnList = "user_id,status"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTopUpRequest {
    public enum Status {
        PENDING,
        AMOUNT_MISMATCH,
        PAID,
        EXPIRED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "payment_reference", nullable = false, length = 30)
    private String paymentReference;

    @Column(name = "requested_amount", precision = 18, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "received_amount", precision = 18, scale = 2)
    private BigDecimal receivedAmount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.PENDING;

    @Column(name = "provider_transaction_id", length = 100, unique = true)
    private String providerTransactionId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
