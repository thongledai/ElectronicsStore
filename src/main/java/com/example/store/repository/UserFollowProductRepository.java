package com.example.store.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.store.entity.UserFollowProduct;

@Repository
public interface UserFollowProductRepository extends JpaRepository<UserFollowProduct, UUID> {

    @Query("""
            SELECT f FROM UserFollowProduct f
            JOIN FETCH f.product p
            LEFT JOIN FETCH p.category
            LEFT JOIN FETCH p.brand
            WHERE f.user.id = :userId
            ORDER BY f.createdAt DESC
            """)
    List<UserFollowProduct> findAllWithProductByUserId(@Param("userId") UUID userId);

    Optional<UserFollowProduct> findByUserIdAndProductId(UUID userId, UUID productId);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    @Query("SELECT f.product.id FROM UserFollowProduct f WHERE f.user.id = :userId")
    List<UUID> findProductIdsByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserFollowProduct f WHERE f.user.id = :userId AND f.product.id = :productId")
    void deleteByUserIdAndProductId(@Param("userId") UUID userId, @Param("productId") UUID productId);

    @Modifying
    @Query("DELETE FROM UserFollowProduct f WHERE f.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    long countByUserId(UUID userId);
}
