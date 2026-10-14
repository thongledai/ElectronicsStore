package com.example.store.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CheckoutRequest(
        @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm")
        List<UUID> cartItemIds,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        String phone,

        @NotBlank(message = "Vui lòng nhập địa chỉ giao hàng")
        String address,

        @NotNull(message = "Vui lòng chọn phương thức giao hàng")
        UUID deliveryId,

        @NotNull(message = "Vui lòng chọn phương thức thanh toán")
        Integer paymentMethodId,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {}
