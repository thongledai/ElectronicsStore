package com.example.store.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "product_name", columnDefinition = "nvarchar(255)")
    private String productName;

    @Column(name = "variant_description", columnDefinition = "nvarchar(255)")
    private String variantDescription;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "subtotal", precision = 15, scale = 2)
    private BigDecimal subtotal;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void initDefaults() {
        if (this.quantity == null || this.quantity <= 0) {
            this.quantity = 1;
        }
        if (this.productName == null && this.variant != null && this.variant.getProduct() != null) {
            this.productName = this.variant.getProduct().getName();
        }
        if (this.unitPrice == null && this.variant != null && this.variant.getPrice() != null) {
            this.unitPrice = this.variant.getPrice();
        }
        if (this.subtotal == null && this.unitPrice != null && this.quantity != null) {
            this.subtotal = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
        }
    }

    public Integer getQuantity() {
        return (quantity != null && quantity > 0) ? quantity : 1;
    }

    public String getProductName() {
        if (productName != null && !productName.trim().isEmpty()) {
            return productName.trim();
        }
        if (variant != null && variant.getProduct() != null && variant.getProduct().getName() != null) {
            return variant.getProduct().getName().trim();
        }
        return "Sản phẩm";
    }

    public BigDecimal getUnitPrice() {
        if (unitPrice != null) return unitPrice;
        if (variant != null && variant.getPrice() != null) return variant.getPrice();
        return BigDecimal.ZERO;
    }

    public BigDecimal getSubtotal() {
        if (subtotal != null) return subtotal;
        return getUnitPrice().multiply(BigDecimal.valueOf(getQuantity()));
    }
}
