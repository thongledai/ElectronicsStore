package com.example.store.entity;

import jakarta.persistence.*;

import lombok.*;

@Entity
@Table(name = "product_variant_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariantImage {

    @EmbeddedId
    private ProductVariantImageId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("productVariantId")
    @JoinColumn(
            name = "product_variant_id",
            nullable = false
    )
    private ProductVariant productVariant;
}