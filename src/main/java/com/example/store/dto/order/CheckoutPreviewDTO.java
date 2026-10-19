package com.example.store.dto.order;

import com.example.store.dto.cart.CartItemDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CheckoutPreviewDTO(
        List<CartItemDTO> items,     // các món sẽ mua (tái dùng CartItemDTO)
        BigDecimal subtotal,         // tạm tính
        BigDecimal discount,         // giảm giá
        BigDecimal total             // tổng thanh toán
) {}

