package com.example.store.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.entity.ProductVariantImage;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantImageRepository
        extends JpaRepository<ProductVariantImage, UUID> {

    List<ProductVariantImage> findByProductVariantIdOrderByDisplayOrderAsc(
            UUID productVariantId
    );

    boolean existsByProductVariantId(UUID productVariantId);

    void deleteByProductVariantId(UUID productVariantId);
}