package com.example.store.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartDTO(
        List<CartItemDTO> items,
        int totalLines, // số dòng trong giỏ
        int totalQuantity, // tổng số lượng của mọi dòng
        int selectedLines, // số dòng đã chọn (và còn bán)
        int selectedQuantity,
        BigDecimal totalAmount, // chỉ tính dòng đã chọn và còn bán
        boolean allSelected // mọi dòng còn bán đều đã chọn
) {
    public static CartDTO from(List<CartItemDTO> items) {
        List<CartItemDTO> payable = items.stream().filter(CartItemDTO::available).toList();
        List<CartItemDTO> chosen = payable.stream().filter(CartItemDTO::selected).toList();

        return new CartDTO(
                items,
                items.size(),
                items.stream().mapToInt(CartItemDTO::quantity).sum(),
                chosen.size(),
                chosen.stream().mapToInt(CartItemDTO::quantity).sum(),
                chosen.stream().map(CartItemDTO::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                !payable.isEmpty() && payable.stream().allMatch(CartItemDTO::selected));
    }
}
