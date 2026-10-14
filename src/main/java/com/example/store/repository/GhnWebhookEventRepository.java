package com.example.store.repository;

import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.entity.GhnWebhookEvent;

public interface GhnWebhookEventRepository extends JpaRepository<GhnWebhookEvent, UUID> {
    boolean existsByOrderCodeAndEventTypeAndEventTime(String orderCode, String eventType, Instant eventTime);

    Optional<GhnWebhookEvent> findFirstByOrderCodeOrderByEventTimeDesc(String orderCode);

    List<GhnWebhookEvent> findTop20ByOrderCodeOrderByEventTimeDesc(String orderCode);
}
