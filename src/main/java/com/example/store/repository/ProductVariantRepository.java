package com.example.store.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.entity.ProductVariant;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    boolean existsByProductId(UUID productId);

    boolean existsByProductIdAndIsActiveTrue(UUID productId);

    boolean existsByProductIdAndIsSellingTrue(UUID productId);

    boolean existsByStyleValuesId(UUID styleValueId);

    Page<ProductVariant> findByProductId(UUID productId, Pageable pageable);

    Page<ProductVariant> findByProductIdAndIsActiveTrue(
            UUID productId,
            Pageable pageable
    );

    Page<ProductVariant> findByProductIdAndIsActiveTrueAndIsSellingTrue(
            UUID productId,
            Pageable pageable
    );

    @Query("""
        SELECT v
        FROM ProductVariant v
        WHERE
            (:productId IS NULL OR v.product.id = :productId)
            AND (:q IS NULL OR :q = '' OR
                LOWER(v.sku) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:isActive IS NULL OR v.isActive = :isActive)
            AND (:isSelling IS NULL OR v.isSelling = :isSelling)
            AND (:inStock IS NULL OR
                :inStock = false OR v.quantity > 0
            )
            AND (:onSale IS NULL OR
                :onSale = false OR
                (
                    v.promotionalPrice IS NOT NULL
                    AND v.promotionalPrice < v.price
                )
            )
        """)
    Page<ProductVariant> search(
            @Param("productId") UUID productId,
            @Param("q") String q,
            @Param("isActive") Boolean isActive,
            @Param("isSelling") Boolean isSelling,
            @Param("inStock") Boolean inStock,
            @Param("onSale") Boolean onSale,
            Pageable pageable
    );

    @Query("""
        SELECT v
        FROM ProductVariant v
        WHERE v.product.id = :productId
            AND v.isActive = true
            AND v.isSelling = true
        """)
    Page<ProductVariant> findPublicByProductId(
            @Param("productId") UUID productId,
            Pageable pageable
    );
}