package com.example.store.service.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.store.config.PaymentProperties;
import com.example.store.dto.payment.SepayWebhookPayload;
import com.example.store.entity.BankTransferEvent;
import com.example.store.entity.Order;
import com.example.store.entity.OrderItem;
import com.example.store.entity.OrderStatus;
import com.example.store.entity.PaymentMethod;
import com.example.store.entity.ProductVariant;
import com.example.store.enums.PaymentStatus;
import com.example.store.repository.BankTransferEventRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderStatusRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.service.payment.IVietQrPaymentService.TransferResult;

class VietQrPaymentServiceImplTest {
    private static final String ACCOUNT = "0905928100";

    private OrderRepository orderRepository;
    private OrderStatusRepository orderStatusRepository;
    private ProductVariantRepository variantRepository;
    private BankTransferEventRepository eventRepository;
    private PaymentProperties props;
    private VietQrPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderStatusRepository = mock(OrderStatusRepository.class);
        variantRepository = mock(ProductVariantRepository.class);
        eventRepository = mock(BankTransferEventRepository.class);
        props = new PaymentProperties();
        props.setAccountNumber(ACCOUNT);
        props.setReferencePrefix("ES");
        service = new VietQrPaymentServiceImpl(props, orderRepository, orderStatusRepository, variantRepository, eventRepository);
    }

    // ---------- helpers ----------
    private Order bankOrder(PaymentStatus ps, String ref, long total) {
        return Order.builder()
                .id(UUID.randomUUID())
                .payment(PaymentMethod.builder().name("BANK_QR").build())
                .status(OrderStatus.builder().name("NOT_PROCESSED").build())
                .paymentStatus(ps)
                .paymentReference(ref)
                .paymentExpiresAt(LocalDateTime.now().plusMinutes(10))
                .total(BigDecimal.valueOf(total))
                .build();
    }

    private SepayWebhookPayload payload(long id, String account, String type, long amount, String content) {
        SepayWebhookPayload p = new SepayWebhookPayload();
        p.setId(id);
        p.setAccountNumber(account);
        p.setTransferType(type);
        p.setTransferAmount(BigDecimal.valueOf(amount));
        p.setContent(content);
        p.setReferenceCode("FT123");
        return p;
    }

    private String savedResult() {
        ArgumentCaptor<BankTransferEvent> c = ArgumentCaptor.forClass(BankTransferEvent.class);
        verify(eventRepository).saveAndFlush(c.capture());
        return c.getValue().getResult();
    }

    // ---------- tạo thông tin thanh toán / QR ----------
    @Test
    void initPayment_setsUnpaidReferenceAndFutureExpiry() {
        Order order = bankOrder(null, null, 1000);
        service.initPayment(order);

        assertEquals(PaymentStatus.UNPAID, order.getPaymentStatus());
        assertTrue(order.getPaymentReference().matches("ES[A-HJ-NP-Z2-9]{8}"), order.getPaymentReference());
        assertTrue(order.getPaymentExpiresAt().isAfter(LocalDateTime.now()));
        assertFalse(order.getIsPaidBefore());
    }

    @Test
    void qrUrl_usesConfiguredBankAccountBackendAmountAndReference() {
        String url = service.buildQrImageUrl(new BigDecimal("1500000.00"), "ESABCD2345");
        assertTrue(url.startsWith("https://img.vietqr.io/image/970422-0905928100-compact2.png?"), url);
        assertTrue(url.contains("amount=1500000"), url);
        assertTrue(url.contains("addInfo=ESABCD2345"), url);
    }

    // ---------- webhook ----------
    @Test
    void validTransfer_marksPaidOnce_andSetsPaidBefore() {
        Order order = bankOrder(PaymentStatus.UNPAID, "ESABCD2345", 1500000);
        when(orderRepository.findByPaymentReferenceForUpdate("ESABCD2345")).thenReturn(Optional.of(order));

        TransferResult r = service.handleSepayTransfer(payload(1, ACCOUNT, "in", 1500000, "KHACH CK ESABCD2345"));

        assertEquals(TransferResult.PAID, r);
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertTrue(order.getIsPaidBefore());
        assertNotNull(order.getPaidAt());
        assertEquals("PAID", savedResult());
    }

    @Test
    void referenceGluedInsideBankText_isStillFound() {
        Order order = bankOrder(PaymentStatus.UNPAID, "ESABCD2345", 1000);
        when(orderRepository.findByPaymentReferenceForUpdate(anyString())).thenReturn(Optional.empty());
        when(orderRepository.findByPaymentReferenceForUpdate("ESABCD2345")).thenReturn(Optional.of(order));

        TransferResult r = service.handleSepayTransfer(payload(2, ACCOUNT, "in", 1000, "MBVCBCESESABCD2345CTTU NGUYEN VAN A"));

        assertEquals(TransferResult.PAID, r);
    }

    @Test
    void wrongAmount_isNeverMarkedPaid() {
        Order order = bankOrder(PaymentStatus.UNPAID, "ESABCD2345", 1500000);
        when(orderRepository.findByPaymentReferenceForUpdate("ESABCD2345")).thenReturn(Optional.of(order));

        TransferResult r = service.handleSepayTransfer(payload(3, ACCOUNT, "in", 1000, "ESABCD2345"));

        assertEquals(TransferResult.AMOUNT_MISMATCH, r);
        assertEquals(PaymentStatus.NEEDS_RECONCILIATION, order.getPaymentStatus());
        assertFalse(Boolean.TRUE.equals(order.getIsPaidBefore()));
    }

    @Test
    void overpayment_isNotAutoAccepted() {
        Order order = bankOrder(PaymentStatus.UNPAID, "ESABCD2345", 1000);
        when(orderRepository.findByPaymentReferenceForUpdate("ESABCD2345")).thenReturn(Optional.of(order));

        assertEquals(TransferResult.AMOUNT_MISMATCH,
                service.handleSepayTransfer(payload(4, ACCOUNT, "in", 5000, "ESABCD2345")));
        assertEquals(PaymentStatus.NEEDS_RECONCILIATION, order.getPaymentStatus());
    }

    @Test
    void wrongReceivingAccount_isRejected_withoutLookingUpOrders() {
        TransferResult r = service.handleSepayTransfer(payload(5, "9999999999", "in", 1000, "ESABCD2345"));

        assertEquals(TransferResult.ACCOUNT_MISMATCH, r);
        verify(orderRepository, never()).findByPaymentReferenceForUpdate(anyString());
    }

    @Test
    void outgoingTransfer_isIgnored() {
        TransferResult r = service.handleSepayTransfer(payload(6, ACCOUNT, "out", 1000, "ESABCD2345"));

        assertEquals(TransferResult.NOT_INCOMING, r);
        verify(orderRepository, never()).findByPaymentReferenceForUpdate(anyString());
    }

    @Test
    void unknownReference_isLoggedForReconciliation() {
        when(orderRepository.findByPaymentReferenceForUpdate(anyString())).thenReturn(Optional.empty());

        assertEquals(TransferResult.ORDER_NOT_FOUND,
                service.handleSepayTransfer(payload(7, ACCOUNT, "in", 1000, "ESZZZZ2222")));
        assertEquals("ORDER_NOT_FOUND", savedResult());
    }

    @Test
    void replayedTransactionId_isIgnored_andDoesNotTouchOrder() {
        when(eventRepository.existsByProviderAndProviderTransactionId("SEPAY", "8")).thenReturn(true);

        assertEquals(TransferResult.DUPLICATE_EVENT,
                service.handleSepayTransfer(payload(8, ACCOUNT, "in", 1000, "ESABCD2345")));
        verify(orderRepository, never()).findByPaymentReferenceForUpdate(anyString());
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void secondTransferForPaidOrder_doesNotChangeStateButIsRecorded() {
        Order order = bankOrder(PaymentStatus.PAID, "ESABCD2345", 1000);
        when(orderRepository.findByPaymentReferenceForUpdate("ESABCD2345")).thenReturn(Optional.of(order));

        assertEquals(TransferResult.DUPLICATE_PAYMENT,
                service.handleSepayTransfer(payload(9, ACCOUNT, "in", 1000, "ESABCD2345")));
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
    }

    @Test
    void lateTransferForExpiredOrder_isFlaggedNotRevived() {
        Order order = bankOrder(PaymentStatus.EXPIRED, "ESABCD2345", 1000);
        order.setStatus(OrderStatus.builder().name("CANCELLED").build());
        when(orderRepository.findByPaymentReferenceForUpdate("ESABCD2345")).thenReturn(Optional.of(order));

        assertEquals(TransferResult.LATE_PAYMENT,
                service.handleSepayTransfer(payload(10, ACCOUNT, "in", 1000, "ESABCD2345")));
        assertEquals(PaymentStatus.NEEDS_RECONCILIATION, order.getPaymentStatus());
        assertEquals("CANCELLED", order.getStatus().getName());
        assertFalse(Boolean.TRUE.equals(order.getIsPaidBefore()));
    }

    // ---------- hết hạn ----------
    @Test
    void expireOrder_cancelsAndRestoresStock() {
        ProductVariant variant = ProductVariant.builder().id(UUID.randomUUID()).quantity(3).build();
        Order order = bankOrder(PaymentStatus.UNPAID, "ESABCD2345", 1000);
        order.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));
        order.addItem(OrderItem.builder().variant(variant).quantity(2).build());
        OrderStatus cancelled = OrderStatus.builder().name("CANCELLED").build();

        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orderStatusRepository.findByNameIgnoreCase("CANCELLED")).thenReturn(Optional.of(cancelled));
        when(variantRepository.findAllByIdForUpdate(any())).thenReturn(List.of(variant));

        service.expireOrder(order.getId());

        assertEquals(PaymentStatus.EXPIRED, order.getPaymentStatus());
        assertEquals("CANCELLED", order.getStatus().getName());
        assertEquals(5, variant.getQuantity());
    }

    @Test
    void expireOrder_leavesPaidOrderUntouched() {
        Order order = bankOrder(PaymentStatus.PAID, "ESABCD2345", 1000);
        order.setPaymentExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        service.expireOrder(order.getId());

        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals("NOT_PROCESSED", order.getStatus().getName());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void awaitingPayment_blocksUnpaidBankOrders_butNotCodOrPaid() {
        assertTrue(bankOrder(PaymentStatus.UNPAID, "X", 1).awaitingPayment());
        assertTrue(bankOrder(PaymentStatus.NEEDS_RECONCILIATION, "X", 1).awaitingPayment());
        assertFalse(bankOrder(PaymentStatus.PAID, "X", 1).awaitingPayment());

        Order cod = bankOrder(null, null, 1);
        cod.setPayment(PaymentMethod.builder().name("COD").build());
        assertFalse(cod.awaitingPayment());
        assertNull(cod.getPaymentStatus());
    }
}
