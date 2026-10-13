package com.example.store.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.entity.GhnWebhookEvent;

public interface GhnWebhookEventRepository extends JpaRepository<GhnWebhookEvent, UUID> {
    boolean existsByOrderCodeAndEventTypeAndEventTime(String orderCode, String eventType, Instant eventTime);
}
