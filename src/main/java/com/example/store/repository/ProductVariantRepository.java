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

import com.example.store.entity.ProductVariant;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, UUID id);

    boolean existsByProductId(UUID productId);

    boolean existsByProductIdAndIsActiveTrue(UUID productId);

    boolean existsByProductIdAndIsSellingTrue(UUID productId);

    boolean existsByStyleValuesId(UUID styleValueId);

    @Query("""
        SELECT COUNT(v) > 0
        FROM ProductVariant v
        JOIN v.styleValues sv
        WHERE sv.id = :styleValueId
            AND v.isActive = true
    """)
    boolean existsActiveVariantByStyleValueId(@Param("styleValueId") UUID styleValueId);

    List<ProductVariant> findByProductId(UUID productId);

    List<ProductVariant> findByProductIdAndIsActiveTrue(UUID productId);

    List<ProductVariant> findByProductIdAndIsActiveTrueAndIsSellingTrue(UUID productId);

    Page<ProductVariant> findByProductId(UUID productId, Pageable pageable);

    @Query("""
        SELECT DISTINCT v
        FROM ProductVariant v
        LEFT JOIN FETCH v.images
        WHERE v.product.id = :productId
    """)
    List<ProductVariant> findAllWithImagesByProductId(@Param("productId") UUID productId);

    @Query("""
        SELECT DISTINCT v
        FROM ProductVariant v
        LEFT JOIN FETCH v.styleValues sv
        LEFT JOIN FETCH sv.style
        WHERE v.product.id = :productId
    """)
    List<ProductVariant> findAllWithStyleValuesByProductId(@Param("productId") UUID productId);

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
}