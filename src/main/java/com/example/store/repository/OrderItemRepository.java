package com.example.store.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.store.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    // Top sản phẩm bán chạy có thể lọc linh hoạt theo trạng thái đơn hàng (mặc định DELIVERED)
    @Query("SELECT oi.variant.product.id, oi.productName, oi.imageUrl, SUM(oi.quantity), SUM(oi.subtotal) " +
           "FROM OrderItem oi " +
           "WHERE (:status IS NULL OR :status = '' OR :status = 'ALL' OR UPPER(oi.order.status.name) = UPPER(:status)) " +
           "AND (:startDate IS NULL OR oi.order.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR oi.order.createdAt <= :endDate) " +
           "GROUP BY oi.variant.product.id, oi.productName, oi.imageUrl " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProducts(@Param("status") String status,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          Pageable pageable);

    // Phân bổ doanh số theo danh mục có thể lọc linh hoạt theo trạng thái đơn hàng (mặc định DELIVERED)
    @Query("SELECT oi.variant.product.category.id, oi.variant.product.category.name, SUM(oi.subtotal), SUM(oi.quantity) " +
           "FROM OrderItem oi " +
           "WHERE (:status IS NULL OR :status = '' OR :status = 'ALL' OR UPPER(oi.order.status.name) = UPPER(:status)) " +
           "AND (:startDate IS NULL OR oi.order.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR oi.order.createdAt <= :endDate) " +
           "GROUP BY oi.variant.product.category.id, oi.variant.product.category.name " +
           "ORDER BY SUM(oi.subtotal) DESC")
    List<Object[]> findSalesByCategory(@Param("status") String status,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
}
