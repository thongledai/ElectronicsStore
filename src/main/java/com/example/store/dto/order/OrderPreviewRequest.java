package com.example.store.dto.order;

import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record OrderPreviewRequest(
        @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm")
        List<UUID> cartItemIds,

        @NotBlank(message = "Vui lòng nhập tên người nhận")
        String toName,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^(0|\\+84)\\d{9}$", message = "Số điện thoại không hợp lệ")
        String phone,

        @NotNull(message = "Vui lòng chọn tỉnh/thành phố")
        Integer provinceId,

        @NotBlank(message = "Vui lòng chọn phường/xã")
        String wardCode,

        @NotBlank(message = "Vui lòng nhập tên tỉnh/thành phố") String provinceName,
        @NotBlank(message = "Vui lòng nhập tên phường/xã") String wardName,

        @NotBlank(message = "Vui lòng nhập địa chỉ cụ thể")
        String detailAddress,

        @NotNull(message = "Vui lòng chọn phương thức thanh toán")
        Integer paymentMethodId,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note,

        String voucherCode
) {}
