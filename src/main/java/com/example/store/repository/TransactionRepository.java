package com.example.store.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByOrderIdAndTypeAndIsUpFalseAndAmount(UUID orderId, String type, java.math.BigDecimal amount);
}
