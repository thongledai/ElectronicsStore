package com.example.store.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nhật ký mỗi giao dịch tiền vào mà nhà cung cấp (SePay) báo về.
 * Dùng để: chống xử lý trùng (unique provider + provider_transaction_id) và đối soát.
 * Không lưu payload thô hay thông tin tài khoản người chuyển.
 */
@Entity
@Table(name = "bank_transfer_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_bank_event_provider_tx",
                columnNames = {"provider", "provider_transaction_id"}),
        indexes = {
                @Index(name = "idx_bank_event_order", columnList = "order_id"),
                @Index(name = "idx_bank_event_result", columnList = "result")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransferEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_transaction_id", nullable = false, length = 100)
    private String providerTransactionId;

    /** Mã tham chiếu bank (referenceCode của SePay), chỉ để tra cứu. */
    @Column(name = "bank_reference_code", length = 100)
    private String bankReferenceCode;

    @Column(name = "event_type", length = 30)
    private String eventType;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "wallet_top_up_request_id")
    private UUID walletTopUpRequestId;

    @Column(name = "payment_reference", length = 30)
    private String paymentReference;

    @Column(name = "account_number", length = 30)
    private String accountNumber;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "transfer_type", length = 10)
    private String transferType;

    @Column(length = 500, columnDefinition = "nvarchar(500)")
    private String content;

    /** PAID, DUPLICATE_PAYMENT, AMOUNT_MISMATCH, LATE_PAYMENT, ORDER_NOT_FOUND, ACCOUNT_MISMATCH, NOT_INCOMING, NOT_PAYABLE. */
    @Column(nullable = false, length = 30)
    private String result;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
