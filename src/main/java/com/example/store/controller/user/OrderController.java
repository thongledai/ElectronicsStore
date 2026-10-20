package com.example.store.controller.user;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.order.CancelOrderRequest;
import com.example.store.dto.order.CheckoutPreviewDTO;
import com.example.store.dto.order.CheckoutRequest;
import com.example.store.dto.order.CustomerOrderResponseDTO;
import com.example.store.dto.order.OrderPreviewRequest;
import com.example.store.dto.order.OrderPreviewDTO;
import com.example.store.dto.payment_method.PaymentMethodDTO;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.order.IOrderService;
import com.example.store.service.payment_method.IPaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/orders")
    public ApiResponse<List<CustomerOrderResponseDTO>> getCustomerOrders(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) String status) {
        return ApiResponse.success("OK", orderService.getCustomerOrders(user.getId(), status));
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<CustomerOrderResponseDTO> getCustomerOrderDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID orderId) {
        return ApiResponse.success("OK", orderService.getCustomerOrderDetail(user.getId(), orderId));
    }

    @PostMapping("/orders/{orderId}/cancel")
    public ApiResponse<Void> cancelCustomerOrder(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID orderId,
            @RequestBody(required = false) CancelOrderRequest req) {
        orderService.cancelCustomerOrder(user.getId(), orderId, req);
        return ApiResponse.success("Hủy đơn hàng thành công", null);
    }

    @GetMapping("/payment-methods")
    public ApiResponse<List<PaymentMethodDTO>> getAll() {
        return ApiResponse.success("OK", paymentMethodService.getAll());
    }
}
