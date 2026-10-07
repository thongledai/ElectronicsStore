package com.example.store.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.store.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    @Query("""
        SELECT p FROM Product p
        JOIN FETCH p.category
        JOIN FETCH p.brand
        WHERE p.slug = :slug
    """)
    Optional<Product> findBySlug(@Param("slug") String slug);

    @Query("""
        SELECT p FROM Product p
        JOIN FETCH p.category
        JOIN FETCH p.brand
        WHERE p.slug = :slug
            AND p.isActive = true
            AND p.isSelling = true
            AND COALESCE(p.category.isDeleted, false) = false
            AND p.brand.isActive = true
    """)
    Optional<Product> findPublicBySlug(@Param("slug") String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByCategoryId(UUID categoryId);

    boolean existsByBrandId(Long brandId);

    boolean existsByCategoryIdAndIsActiveTrue(UUID categoryId);

    boolean existsByBrandIdAndIsActiveTrue(Long brandId);

    @Query("""
        SELECT p
        FROM Product p
        JOIN FETCH p.category
        JOIN FETCH p.brand
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
        JOIN FETCH p.category c
        JOIN FETCH p.brand b
        WHERE
            p.isActive = true
            AND p.isSelling = true
            AND COALESCE(c.isDeleted, false) = false
            AND b.isActive = true
            AND EXISTS (
                SELECT 1
                FROM ProductVariant v
                WHERE v.product = p
                    AND v.isActive = true
                    AND v.isSelling = true
            )
            AND (:q IS NULL OR :q = '' OR
                LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.slug) LIKE LOWER(CONCAT('%', :q, '%')) OR
                LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            AND (:categoryIds IS NULL OR c.id IN :categoryIds)
            AND (:brandIds IS NULL OR b.id IN :brandIds)
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
                                WHEN v.promotionalPrice IS NOT NULL AND v.promotionalPrice > 0
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
                                WHEN v.promotionalPrice IS NOT NULL AND v.promotionalPrice > 0
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
            @Param("categoryIds") Collection<UUID> categoryIds,
            @Param("brandIds") Collection<Long> brandIds,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minRating") Double minRating,
            @Param("inStock") Boolean inStock,
            @Param("onSale") Boolean onSale,
            Pageable pageable
    );

    @Query("""
        SELECT MAX(
            CASE 
                WHEN v.promotionalPrice IS NOT NULL AND v.promotionalPrice > 0 
                THEN v.promotionalPrice 
                ELSE v.price 
            END
        )
        FROM ProductVariant v
        WHERE v.isActive = true
            AND v.isSelling = true
            AND v.product.isActive = true
            AND v.product.isSelling = true
    """)
    BigDecimal findMaxEffectivePrice();

    @Query("""
        SELECT DISTINCT p
        FROM Product p
        JOIN FETCH p.category c
        JOIN FETCH p.brand b
        WHERE p.isActive = true
            AND p.isSelling = true
            AND COALESCE(c.isDeleted, false) = false
            AND b.isActive = true
            AND p.id <> :excludeId
            AND (c.id = :categoryId OR b.id = :brandId)
            AND EXISTS (
                SELECT 1
                FROM ProductVariant v
                WHERE v.product = p
                    AND v.isActive = true
                    AND v.isSelling = true
            )
        ORDER BY CASE WHEN c.id = :categoryId THEN 0 ELSE 1 END, p.createdAt DESC
    """)
    List<Product> findRelatedProducts(
            @Param("excludeId") UUID excludeId,
            @Param("categoryId") UUID categoryId,
            @Param("brandId") Long brandId,
            Pageable pageable
    );
}