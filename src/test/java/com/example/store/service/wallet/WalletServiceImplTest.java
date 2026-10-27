package com.example.store.service.wallet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.store.config.PaymentProperties;
import com.example.store.dto.payment.SepayWebhookPayload;
import com.example.store.entity.Order;
import com.example.store.entity.Role;
import com.example.store.entity.Transaction;
import com.example.store.entity.User;
import com.example.store.entity.WalletTopUpRequest;
import com.example.store.exception.BusinessException;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.TransactionRepository;
import com.example.store.repository.UserRepository;
import com.example.store.repository.WalletTopUpRequestRepository;
import com.example.store.service.payment.VietQrUrlBuilder;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private WalletTopUpRequestRepository topUpRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private TransactionRepository transactionRepository;

    private WalletServiceImpl walletService;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        walletService = new WalletServiceImpl(
                userRepository,
                topUpRepository,
                orderRepository,
                transactionRepository,
                properties,
                new VietQrUrlBuilder(properties));
    }

    @Test
    void validTopUpCreditsWebhookAmountAndDoesNotCreditSameRequestTwice() {
        User user = customerWithBalance("5000.00");
        UUID topUpId = UUID.randomUUID();
        WalletTopUpRequest request = pendingTopUp(topUpId, user, "20000.00");
        SepayWebhookPayload payload = transfer(77001L, "20000");

        when(topUpRepository.findByIdForUpdate(topUpId)).thenReturn(Optional.of(request));
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));

        assertEquals(IWalletService.TopUpTransferResult.PAID,
                walletService.processSepayTopUp(topUpId, payload));
        assertEquals(0, new BigDecimal("25000.00").compareTo(user.getEWallet()));
        assertEquals(WalletTopUpRequest.Status.PAID, request.getStatus());
        verify(transactionRepository).save(any(Transaction.class));

        assertEquals(IWalletService.TopUpTransferResult.DUPLICATE_PAYMENT,
                walletService.processSepayTopUp(topUpId, transfer(77002L, "20000")));
        assertEquals(0, new BigDecimal("25000.00").compareTo(user.getEWallet()));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void topUpCreditsReceivedAmountEvenWhenItDiffersFromFormerRequestedAmount() {
        User user = customerWithBalance("0.00");
        UUID topUpId = UUID.randomUUID();
        WalletTopUpRequest request = pendingTopUp(topUpId, user, "20000.00");
        when(topUpRepository.findByIdForUpdate(topUpId)).thenReturn(Optional.of(request));

        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));

        assertEquals(IWalletService.TopUpTransferResult.PAID,
                walletService.processSepayTopUp(topUpId, transfer(77003L, "15000")));
        assertEquals(WalletTopUpRequest.Status.PAID, request.getStatus());
        assertEquals(0, new BigDecimal("15000.00").compareTo(user.getEWallet()));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void walletPaymentDebitsBalanceAndWritesOrderLedgerEntry() {
        User user = customerWithBalance("50000.00");
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .total(new BigDecimal("25000.00"))
                .build();
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));

        walletService.debitForOrder(user.getId(), order);

        assertEquals(0, new BigDecimal("25000.00").compareTo(user.getEWallet()));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void insufficientBalanceThrowsWithoutChangingWalletOrWritingLedger() {
        User user = customerWithBalance("5000.00");
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .total(new BigDecimal("9000.00"))
                .build();
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));

        assertThrows(BusinessException.class, () -> walletService.debitForOrder(user.getId(), order));

        assertEquals(0, new BigDecimal("5000.00").compareTo(user.getEWallet()));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    private User customerWithBalance(String balance) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("customer@example.test")
                .fullName("Test Customer")
                .role(Role.builder().id(1).name("CUSTOMER").build())
                .eWallet(new BigDecimal(balance))
                .build();
    }

    private WalletTopUpRequest pendingTopUp(UUID id, User user, String requestedAmount) {
        return WalletTopUpRequest.builder()
                .id(id)
                .user(user)
                .paymentReference("ESABCDEFGH")
                .requestedAmount(new BigDecimal(requestedAmount))
                .status(WalletTopUpRequest.Status.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    private SepayWebhookPayload transfer(long id, String amount) {
        SepayWebhookPayload payload = new SepayWebhookPayload();
        payload.setId(id);
        payload.setTransferAmount(new BigDecimal(amount));
        return payload;
    }
}
