package com.example.store.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.store.config.PaymentProperties;
import com.example.store.dto.payment.SepayWebhookPayload;
import com.example.store.repository.BankTransferEventRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderStatusRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.repository.WalletTopUpRequestRepository;
import com.example.store.service.payment.IVietQrPaymentService.TransferResult;
import com.example.store.service.wallet.IWalletService;

@ExtendWith(MockitoExtension.class)
class VietQrPaymentServiceIdempotencyTest {
    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusRepository orderStatusRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private BankTransferEventRepository eventRepository;
    @Mock private WalletTopUpRequestRepository topUpRepository;
    @Mock private IWalletService walletService;

    private VietQrPaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        paymentService = new VietQrPaymentServiceImpl(
                properties,
                orderRepository,
                orderStatusRepository,
                variantRepository,
                eventRepository,
                topUpRepository,
                walletService,
                new VietQrUrlBuilder(properties));
    }

    @Test
    void repeatedSePayWebhookIsRecognizedBeforeAnyWalletOrOrderMutation() {
        SepayWebhookPayload payload = new SepayWebhookPayload();
        payload.setId(8800123L);
        when(eventRepository.existsByProviderAndProviderTransactionId("SEPAY", "8800123"))
                .thenReturn(true);

        assertEquals(TransferResult.DUPLICATE_EVENT, paymentService.handleSepayTransfer(payload));

        verify(walletService, never()).processSepayTopUp(any(), any());
        verify(eventRepository, never()).saveAndFlush(any());
    }
}
