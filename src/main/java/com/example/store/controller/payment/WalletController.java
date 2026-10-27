package com.example.store.controller.payment;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.common.ApiResponse;
import com.example.store.dto.wallet.WalletSummaryDTO;
import com.example.store.dto.wallet.WalletTopUpDTO;
import com.example.store.security.CustomUserDetails;
import com.example.store.service.wallet.IWalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final IWalletService walletService;

    @GetMapping
    public ApiResponse<WalletSummaryDTO> getWallet(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success("OK", walletService.getSummary(user.getId()));
    }

    @PostMapping("/top-ups")
    public ApiResponse<WalletTopUpDTO> createTopUp(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success("Đã tạo yêu cầu nạp ví", walletService.createTopUp(user.getId()));
    }

    @GetMapping("/top-ups/{topUpId}")
    public ApiResponse<WalletTopUpDTO> getTopUp(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable UUID topUpId) {
        return ApiResponse.success("OK", walletService.getTopUp(user.getId(), topUpId));
    }
}
