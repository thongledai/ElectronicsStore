package com.example.store.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.entity.Product;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByBrandId(Long brandId);

    boolean existsByCategoryIdAndIsActiveTrue(UUID categoryId);

    boolean existsByBrandIdAndIsActiveTrue(Long brandId);

    @Query("""
        SELECT p
        FROM Product p
        WHERE
            (:q IS NULL OR :q = '' OR
                LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.slug) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:brandId IS NULL OR p.brand.id = :brandId)
            AND (:isActive IS NULL OR p.isActive = :isActive)
            AND (:isSelling IS NULL OR p.isSelling = :isSelling)
        """)
    Page<Product> search(
            @Param("q") String q,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") Long brandId,
            @Param("isActive") Boolean isActive,
            @Param("isSelling") Boolean isSelling,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p
        FROM Product p
        WHERE
            p.isActive = true
            AND p.isSelling = true
            AND COALESCE(p.category.isDeleted, false) = false
            AND p.brand.isActive = true
            AND (:q IS NULL OR :q = '' OR
                LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.slug) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:categoryId IS NULL OR p.category.id = :categoryId)
            AND (:brandId IS NULL OR p.brand.id = :brandId)
            AND (:minRating IS NULL OR p.rating >= :minRating)
            AND (
                :minPrice IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM ProductVariant v
                    WHERE v.product = p
                        AND v.isActive = true
                        AND v.isSelling = true
                        AND (
                            CASE
                                WHEN v.promotionalPrice IS NOT NULL
                                    THEN v.promotionalPrice
                                ELSE v.price
                            END
                        ) >= :minPrice
                )
            )
            AND (
                :maxPrice IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM ProductVariant v
                    WHERE v.product = p
                        AND v.isActive = true
                        AND v.isSelling = true
                        AND (
                            CASE
                                WHEN v.promotionalPrice IS NOT NULL
                                    THEN v.promotionalPrice
                                ELSE v.price
                            END
                        ) <= :maxPrice
                )
            )
            AND (
                :inStock IS NULL
                OR :inStock = false
                OR EXISTS (
                    SELECT 1
                    FROM ProductVariant v
                    WHERE v.product = p
                        AND v.isActive = true
                        AND v.isSelling = true
                        AND v.quantity > 0
                )
            )
            AND (
                :onSale IS NULL
                OR :onSale = false
                OR EXISTS (
                    SELECT 1
                    FROM ProductVariant v
                    WHERE v.product = p
                        AND v.isActive = true
                        AND v.isSelling = true
                        AND v.promotionalPrice IS NOT NULL
                        AND v.promotionalPrice < v.price
                )
            )
        """)
    Page<Product> searchPublicProducts(
            @Param("q") String q,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") Long brandId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("inStock") Boolean inStock,
            @Param("onSale") Boolean onSale,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM Product p
        WHERE p.isActive = true
            AND p.isSelling = true
            AND COALESCE(p.category.isDeleted, false) = false
            AND p.brand.isActive = true
            AND EXISTS (
                SELECT 1
                FROM ProductVariant v
                WHERE v.product = p
                    AND v.isActive = true
                    AND v.isSelling = true
            )
        """)
    Page<Product> findAllPublic(Pageable pageable);
}