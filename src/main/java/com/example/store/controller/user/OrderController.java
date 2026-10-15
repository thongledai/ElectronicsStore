package com.example.store.controller.user;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.order.CheckoutPreviewDTO;
import com.example.store.dto.order.CheckoutPreviewRequest;
import com.example.store.dto.order.CheckoutRequest;
import com.example.store.dto.order.OrderDTO;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.order.IOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping("/checkout")
    public ApiResponse<CheckoutPreviewDTO> preview(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CheckoutPreviewRequest req) {
        return ApiResponse.success("OK", orderService.preview(user.getId(), req));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderDTO> place(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CheckoutRequest req) {
        return ApiResponse.success("Đặt hàng thành công", orderService.placeOrder(user.getId(), req));
    }
}
