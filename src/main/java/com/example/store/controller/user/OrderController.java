package com.example.store.controller.user;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.order.CheckoutPreviewDTO;
import com.example.store.dto.order.CheckoutRequest;
import com.example.store.dto.order.OrderPreviewRequest;
import com.example.store.dto.order.OrderPreviewDTO;
import com.example.store.dto.payment_method.PaymentMethodDTO;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.delivery.IGhnDeliveryService;
import com.example.store.service.order.IOrderService;
import com.example.store.service.order.IShipmentService;
import com.example.store.service.payment_method.IPaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class OrderController {

    private final IOrderService orderService;
    private final IPaymentMethodService paymentMethodService;


    @PostMapping("/checkout")
    public ApiResponse<CheckoutPreviewDTO> checkout(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody CheckoutRequest req) {
        return ApiResponse.success("OK", orderService.checkout(user.getId(), req));
    }

    @PostMapping("/preview-order")
    public ApiResponse<OrderPreviewDTO> createOrderPreview(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody OrderPreviewRequest req) {
        return ApiResponse.success("OK", orderService.createOrderPreview(user.getId(), req));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderPreviewDTO> placeOrder(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody OrderPreviewRequest req) {
        return ApiResponse.success("Đặt hàng thành công", orderService.placeOrder(user.getId(), req));
    }

    @GetMapping("/payment-methods")
    public ApiResponse<List<PaymentMethodDTO>> getAll() {
        return ApiResponse.success("OK", paymentMethodService.getAll());
    }
}
