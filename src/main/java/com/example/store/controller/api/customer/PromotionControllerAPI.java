package com.example.store.controller.api.customer;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.promotion.VoucherCheckRequest;
import com.example.store.dto.promotion.VoucherCheckResponse;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.promotion.IPromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/promotions")
public class PromotionControllerAPI {

    private final IPromotionService promotionService;

    @PostMapping("/check")
    public ApiResponse<VoucherCheckResponse> checkVoucher(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody VoucherCheckRequest req) {
        return ApiResponse.success("OK", promotionService.checkVoucher(user.getId(), req));
    }
}
