package com.example.store.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

import com.example.store.entity.Order;
import com.example.store.enums.PaymentStatus;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    @EntityGraph(attributePaths = {"user", "status", "payment"})
    @Query("select o from Order o where (:status is null or upper(o.status.name) = upper(:status)) "
            + "and (:keyword is null or :keyword = '' or lower(o.user.fullName) like lower(concat('%', :keyword, '%')) "
            + "or o.phone like concat('%', :keyword, '%') or lower(o.ghnOrderCode) like lower(concat('%', :keyword, '%'))) ")
    Page<Order> search(@Param("status") String status, @Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "status", "payment", "items"})
    @Query("select o from Order o where "
            + "(:status is null or :status = '' or upper(o.status.name) = upper(:status)) "
            + "and (:paymentMethodId is null or o.payment.id = :paymentMethodId) "
            + "and (:startDate is null or o.createdAt >= :startDate) "
            + "and (:endDate is null or o.createdAt <= :endDate) "
            + "and (:keyword is null or :keyword = '' "
            + "     or lower(o.user.fullName) like lower(concat('%', :keyword, '%')) "
            + "     or lower(o.toName) like lower(concat('%', :keyword, '%')) "
            + "     or o.phone like concat('%', :keyword, '%') "
            + "     or lower(o.ghnOrderCode) like lower(concat('%', :keyword, '%'))) ")
    Page<Order> searchOrdersForManager(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("paymentMethodId") Integer paymentMethodId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @EntityGraph(attributePaths = {"user", "status", "payment", "items", "items.variant", "items.variant.product"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "status", "payment"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    Optional<Order> findByGhnOrderCode(String ghnOrderCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.ghnOrderCode = :ghnOrderCode")
    Optional<Order> findByGhnOrderCodeForUpdate(@Param("ghnOrderCode") String ghnOrderCode);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"user", "status", "payment", "items", "items.variant", "items.variant.product"})
    @Query("select o from Order o where o.id = :id and o.user.id = :userId")
    Optional<Order> findDetailedByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);

    @EntityGraph(attributePaths = {"status", "payment", "items", "items.variant", "items.variant.product"})
    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT COUNT(o) FROM Order o WHERE UPPER(o.status.name) IN ('NOT_PROCESSED', 'CONFIRMED')")
    long countPendingOrders();

    @Query("SELECT o.status.name, COUNT(o) FROM Order o GROUP BY o.status.name")
    List<Object[]> countOrdersByStatusGroup();

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    List<Order> findOrdersBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Tính doanh thu: Chuẩn phương án 2 (Chỉ tính đơn giao thành công DELIVERED)
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE UPPER(o.status.name) = 'DELIVERED'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE UPPER(o.status.name) = 'DELIVERED' AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    BigDecimal calculateRevenueBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @EntityGraph(attributePaths = {"status", "payment", "items", "items.variant", "items.variant.product"})
    List<Order> findByUserIdAndStatus_NameIgnoreCaseOrderByCreatedAtDesc(UUID userId, String statusName);

    @EntityGraph(attributePaths = {"status", "payment", "items", "items.variant", "items.variant.product"})
    List<Order> findByUserIdAndStatus_NameInIgnoreCaseOrderByCreatedAtDesc(UUID userId, Collection<String> statusNames);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "status", "payment"})
    @Query("select o from Order o where o.paymentReference = :reference")
    Optional<Order> findByPaymentReferenceForUpdate(@Param("reference") String reference);

    boolean existsByPaymentReference(String paymentReference);

    @Query("select o.id from Order o where o.paymentStatus = :paymentStatus "
            + "and o.paymentExpiresAt < :now and upper(o.status.name) = 'NOT_PROCESSED'")
    List<UUID> findExpiredPaymentOrderIds(@Param("paymentStatus") PaymentStatus paymentStatus,
                                          @Param("now") LocalDateTime now);
}
