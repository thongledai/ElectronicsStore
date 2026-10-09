package com.example.store.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(
        name = "product_variants",
        indexes = @Index(
                name = "idx_product_variants_product",
                columnList = "product_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100, columnDefinition = "nvarchar(100)")
    private String sku;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(precision = 18, scale = 2)
    private BigDecimal promotionalPrice;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer sold = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean isSelling = true;

        @Builder.Default
        @Column(nullable = false, columnDefinition = "boolean not null default true")
        private boolean isActive = true;

    @Builder.Default
    @OneToMany(
            mappedBy = "productVariant",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProductVariantImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_variant_style_values",
            joinColumns = @JoinColumn(name = "variant_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "style_value_id", nullable = false)
    )
    private Set<StyleValue> styleValues = new HashSet<>();
}