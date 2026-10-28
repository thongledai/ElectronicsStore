package com.example.store.repository;

import com.example.store.entity.Promotion;
import com.example.store.enums.PromotionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByCodeIgnoreCase(String code);

    Optional<Promotion> findByCodeIgnoreCaseAndIsActiveTrueAndIsEnabledTrue(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where p.id = :id")
    Optional<Promotion> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Promotion p where lower(p.code) = lower(:code)")
    Optional<Promotion> findByCodeForUpdate(@Param("code") String code);

    @Query("SELECT COUNT(o) FROM Order o JOIN o.promotions p WHERE p.id = :promotionId")
    long countOrdersUsingPromotion(@Param("promotionId") UUID promotionId);

    @Query("SELECT p FROM Promotion p WHERE " +
           "(:search IS NULL OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:type IS NULL OR p.type = :type) AND " +
           "(:isActive IS NULL OR p.isActive = :isActive)")
    Page<Promotion> findWithFilters(@Param("search") String search,
                                    @Param("type") PromotionType type,
                                    @Param("isActive") Boolean isActive,
                                    Pageable pageable);
}
