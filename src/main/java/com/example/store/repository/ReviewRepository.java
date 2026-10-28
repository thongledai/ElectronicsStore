package com.example.store.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.store.entity.Review;
import com.example.store.enums.ReviewStatus;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByProductIdAndStatusAndStarsOrderByCreatedAtDesc(UUID productId, ReviewStatus status, Integer stars, Pageable pageable);

    Page<Review> findByProductIdAndStatusAndImagesIsNotEmptyOrderByCreatedAtDesc(UUID productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByStatusOrderByCreatedAtDesc(ReviewStatus status, Pageable pageable);

    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(ReviewStatus status);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    Optional<Review> findByUserIdAndProductId(UUID userId, UUID productId);

    boolean existsByUserIdAndProductIdAndOrderId(UUID userId, UUID productId, UUID orderId);

    Optional<Review> findByUserIdAndProductIdAndOrderId(UUID userId, UUID productId, UUID orderId);

    long countByProductIdAndStatus(UUID productId, ReviewStatus status);

    long countByProductIdAndStatusAndStars(UUID productId, ReviewStatus status, Integer stars);

    long countByProductIdAndStatusAndImagesIsNotEmpty(UUID productId, ReviewStatus status);

    @Query("SELECT AVG(CAST(r.stars AS double)) FROM Review r WHERE r.product.id = :productId AND r.status = :status")
    Double findAverageRatingByProductIdAndStatus(@Param("productId") UUID productId, @Param("status") ReviewStatus status);

    List<Review> findByOrderId(UUID orderId);
}
