package com.example.store.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.store.entity.OtpToken;
import com.example.store.enums.OtpType;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {
    Optional<OtpToken> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(String email, OtpType type);
    List<OtpToken> findAllByEmailAndTypeAndUsedFalse(String email, OtpType type);
}
