package com.example.store.controller.payment;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.payment.VietQrPaymentDTO;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.payment.IVietQrPaymentService;

import lombok.RequiredArgsConstructor;

/** Khách xem QR và trạng thái thanh toán của đơn của chính mình (/orders/** chỉ dành cho CUSTOMER). */
@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final IVietQrPaymentService paymentService;

    @GetMapping("/orders/{orderId}/payment")
    public ApiResponse<VietQrPaymentDTO> getPayment(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID orderId) {
        return ApiResponse.success("OK", paymentService.getPayment(user.getId(), orderId));
    }
}
