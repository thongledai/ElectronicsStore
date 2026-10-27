package com.example.store.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.entity.BankTransferEvent;

public interface BankTransferEventRepository extends JpaRepository<BankTransferEvent, UUID> {
    boolean existsByProviderAndProviderTransactionId(String provider, String providerTransactionId);
}
