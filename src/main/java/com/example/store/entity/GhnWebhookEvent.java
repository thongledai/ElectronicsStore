package com.example.store.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ghn_webhook_events", uniqueConstraints = @UniqueConstraint(
        name = "uk_ghn_event_dedupe", columnNames = {"order_code", "event_type", "event_time"}))
@Getter
@Setter
@NoArgsConstructor
public class GhnWebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_code", nullable = false, length = 50)
    private String orderCode;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "status_value", length = 80)
    private String status;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();
}
