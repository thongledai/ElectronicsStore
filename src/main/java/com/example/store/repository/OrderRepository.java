package com.example.store.repository;

import com.example.store.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository <Order, UUID>{
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
