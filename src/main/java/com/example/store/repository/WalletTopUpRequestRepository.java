package com.example.store.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.entity.WalletTopUpRequest;

import jakarta.persistence.LockModeType;

public interface WalletTopUpRequestRepository extends JpaRepository<WalletTopUpRequest, UUID> {
    boolean existsByPaymentReference(String paymentReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from WalletTopUpRequest r where r.paymentReference = :reference")
    Optional<WalletTopUpRequest> findByPaymentReferenceForUpdate(@Param("reference") String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from WalletTopUpRequest r where r.id = :id")
    Optional<WalletTopUpRequest> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from WalletTopUpRequest r where r.id = :id and r.user.id = :userId")
    Optional<WalletTopUpRequest> findByIdAndUserIdForUpdate(
            @Param("id") UUID id, @Param("userId") UUID userId);
}
