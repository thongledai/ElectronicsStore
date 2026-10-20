package com.example.store.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "detail_address", columnDefinition = "nvarchar(255)")
    private String detailAddress;

    @Column(name = "ward_name", columnDefinition = "nvarchar(100)")
    private String wardName;

    @Column(name = "province_name", columnDefinition = "nvarchar(100)")
    private String provinceName;

    @Column(name = "full_address", length = 500, columnDefinition = "nvarchar(500)")
    private String fullAddress;

    @Column(nullable = false, length = 11)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private OrderStatus status;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isPaidBefore = false;

    @Column(name = "to_name", length = 100)
    private String toName;

    @Column(precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 18, scale = 2)
    private BigDecimal shippingFee;

    @Column(precision = 18, scale = 2)
    private BigDecimal discount;

    @Column(precision = 18, scale = 2)
    private BigDecimal total;

    @Column(length = 500, columnDefinition = "nvarchar(500)")
    private String note;

    @Column(name = "ghn_order_code", length = 50, unique = true)
    private String ghnOrderCode;

    @Column(name = "expectedDeliveryDate")
    private LocalDateTime expectedDeliveryDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentMethod payment;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "order_promotions",
            joinColumns = @JoinColumn(name = "order_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "promotion_id", nullable = false))
    private Set<Promotion> promotions = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    @PrePersist
    @PreUpdate
    public void syncDatabaseColumns() {
        if (!hasText(this.fullAddress)) {
            List<String> parts = new ArrayList<>();
            if (hasText(detailAddress)) parts.add(detailAddress.trim());
            if (hasText(wardName)) parts.add(wardName.trim());
            if (hasText(provinceName)) parts.add(provinceName.trim());
            this.fullAddress = String.join(", ", parts);
        }

        if (this.subtotal == null) {
            this.subtotal = this.total != null ? this.total : BigDecimal.ZERO;
        }
        if (this.shippingFee == null) {
            this.shippingFee = BigDecimal.ZERO;
        }
        if (this.discount == null) {
            this.discount = BigDecimal.ZERO;
        }
    }

    public String getWardName() {
        if (hasText(wardName)) return wardName.trim();
        return extractWard(getFullAddress());
    }

    public String getProvinceName() {
        if (hasText(provinceName)) return provinceName.trim();
        return extractProvince(getFullAddress());
    }

    public String getFullAddress() {
        if (hasText(fullAddress)) return fullAddress.trim();
        List<String> parts = new ArrayList<>();
        if (hasText(detailAddress)) parts.add(detailAddress.trim());
        if (hasText(wardName)) parts.add(wardName.trim());
        if (hasText(provinceName)) parts.add(provinceName.trim());
        return String.join(", ", parts);
    }

    public String getDetailAddress() {
        if (hasText(detailAddress)) return detailAddress.trim();
        String full = getFullAddress();
        if (hasText(full)) {
            String[] parts = full.split(",");
            if (parts.length > 0) return parts[0].trim();
        }
        return full;
    }

    public String getToName() {
        if (hasText(toName)) return toName.trim();
        if (user != null && hasText(user.getFullName())) return user.getFullName().trim();
        return "Khách hàng";
    }

    public BigDecimal getTotal() {
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal getSubtotal() {
        if (subtotal != null) return subtotal;
        if (total != null) return total;
        return BigDecimal.ZERO;
    }

    public BigDecimal getShippingFee() {
        return shippingFee != null ? shippingFee : BigDecimal.ZERO;
    }

    public BigDecimal getDiscount() {
        return discount != null ? discount : BigDecimal.ZERO;
    }

    private static boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    private static String extractProvince(String addr) {
        if (!hasText(addr)) return null;
        String[] parts = addr.split(",");
        if (parts.length >= 2) {
            return parts[parts.length - 1].trim();
        }
        return null;
    }

    private static String extractWard(String addr) {
        if (!hasText(addr)) return null;
        String[] parts = addr.split(",");
        if (parts.length >= 3) {
            return parts[parts.length - 2].trim();
        } else if (parts.length == 2) {
            return parts[0].trim();
        }
        return null;
    }
}
