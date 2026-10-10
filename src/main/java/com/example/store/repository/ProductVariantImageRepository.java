package com.example.store.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.store.entity.ProductVariantImage;

@Repository
public interface ProductVariantImageRepository extends JpaRepository<ProductVariantImage, UUID> {

    List<ProductVariantImage> findByProductVariantIdOrderByImageUrlAsc(UUID productVariantId);

    List<ProductVariantImage> findByProductVariantId(UUID productVariantId);

    Optional<ProductVariantImage> findByProductVariantIdAndImageUrl(UUID productVariantId, String imageUrl);

    boolean existsByProductVariantId(UUID productVariantId);

    boolean existsByProductVariantIdAndImageUrl(UUID productVariantId, String imageUrl);

    long countByProductVariantId(UUID productVariantId);

    void deleteByProductVariantIdAndImageUrl(UUID productVariantId, String imageUrl);

    void deleteByProductVariantId(UUID productVariantId);
}