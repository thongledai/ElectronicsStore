package com.example.store.dto.respone;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CartDTO(
    UUID id,
    List<CartItemDTO> items,
    int totalItems,
    int totalQuantity,
    BigDecimal totalPrice,
    boolean hasUnavailableItems,
    LocalDate updatedAt
) {
    // Compact constructor: đảm bảo không null và bất biến
    public CartDTO {
        items = (items == null) ? List.of() : List.copyOf(items);
        totalPrice = (totalPrice == null) ? BigDecimal.ZERO : totalPrice;
    }

    // Factory method: tính các trường tổng hợp từ danh sách item
    public static CartDTO of(UUID id, List<CartItemDTO> items, LocalDate updatedAt) {
        int totalQuantity = items.stream().mapToInt(CartItemDTO::count).sum();

        BigDecimal totalPrice = items.stream()
                .filter(CartItemDTO::available)
                .map(CartItemDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean hasUnavailable = items.stream().anyMatch(i -> !i.available());

        return new CartDTO(id, items, items.size(), totalQuantity,
                totalPrice, hasUnavailable, updatedAt);
    }

    // Giỏ rỗng "ảo" khi user chưa có giỏ (không lưu DB)
    public static CartDTO empty() {
        return new CartDTO(null, List.of(), 0, 0, BigDecimal.ZERO, false, null);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
