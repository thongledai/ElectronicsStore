package com.example.store.service.wallet;

import java.util.UUID;

import com.example.store.dto.payment.SepayWebhookPayload;
import com.example.store.dto.wallet.WalletSummaryDTO;
import com.example.store.dto.wallet.WalletTopUpCreateRequest;
import com.example.store.dto.wallet.WalletTopUpDTO;
import com.example.store.entity.Order;

public interface IWalletService {
    enum TopUpTransferResult {
        PAID,
        DUPLICATE_PAYMENT,
        AMOUNT_MISMATCH,
        LATE_PAYMENT,
        NOT_PAYABLE
    }

    WalletSummaryDTO getSummary(UUID userId);

    WalletTopUpDTO createTopUp(UUID userId, WalletTopUpCreateRequest request);

    WalletTopUpDTO getTopUp(UUID userId, UUID topUpId);

    /** Chỉ được gọi trong transaction đang xử lý webhook SePay. */
    TopUpTransferResult processSepayTopUp(UUID topUpId, SepayWebhookPayload payload);

    /** Trừ số dư và ghi ledger trong cùng transaction với việc tạo đơn hàng. */
    void debitForOrder(UUID userId, Order order);
}
