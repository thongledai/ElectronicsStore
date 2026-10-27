package com.example.store.service.payment;

import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Định kỳ hủy các đơn VietQR quá hạn chưa thanh toán và hoàn tồn kho. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentExpiryScheduler {
    private final IVietQrPaymentService paymentService;

    @Scheduled(fixedDelayString = "${payment.expire-scan-ms:60000}", initialDelayString = "${payment.expire-scan-ms:60000}")
    public void expireOverdueOrders() {
        for (UUID id : paymentService.findExpiredOrderIds()) {
            try {
                paymentService.expireOrder(id); // mỗi đơn một transaction riêng
            } catch (RuntimeException ex) {
                log.error("Không thể hết hạn đơn {}", id, ex);
            }
        }
    }
}
