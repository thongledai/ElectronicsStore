package com.example.store.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductVariantImageId implements Serializable {

    @Column(name = "product_variant_id", nullable = false)
    private UUID productVariantId;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;
}