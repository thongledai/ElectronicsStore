package com.example.store.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.entity.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @EntityGraph(attributePaths = {"user", "status", "delivery", "payment"})
    @Query("select o from Order o where (:status is null or upper(o.status.name) = upper(:status)) "
            + "and (:keyword is null or :keyword = '' or lower(o.user.fullName) like lower(concat('%', :keyword, '%')) "
            + "or o.phone like concat('%', :keyword, '%') or lower(o.ghnOrderCode) like lower(concat('%', :keyword, '%'))) ")
    Page<Order> search(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "status", "delivery", "payment", "items", "items.variant", "items.variant.product"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "status", "delivery", "payment"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    Optional<Order> findByGhnOrderCode(String ghnOrderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.ghnOrderCode = :ghnOrderCode")
    Optional<Order> findByGhnOrderCodeForUpdate(@Param("ghnOrderCode") String ghnOrderCode);
}
