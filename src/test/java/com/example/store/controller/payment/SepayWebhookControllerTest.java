package com.example.store.controller.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import com.example.store.config.PaymentProperties;
import com.example.store.dto.payment.SepayWebhookPayload;
import com.example.store.service.payment.IVietQrPaymentService;
import com.example.store.service.payment.IVietQrPaymentService.TransferResult;

class SepayWebhookControllerTest {
    private IVietQrPaymentService service;
    private PaymentProperties props;
    private SepayWebhookController controller;

    @BeforeEach
    void setUp() {
        service = mock(IVietQrPaymentService.class);
        props = new PaymentProperties();
        props.setSepayApiKey("test-key-not-real");
        controller = new SepayWebhookController(props, service);
    }

    private SepayWebhookPayload body() {
        SepayWebhookPayload p = new SepayWebhookPayload();
        p.setId(1L);
        return p;
    }

    @Test
    void missingOrWrongKey_isRejected_andNothingProcessed() {
        assertEquals(HttpStatus.UNAUTHORIZED, controller.receive(null, body()).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.receive("Apikey wrong", body()).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, controller.receive("Bearer test-key-not-real", body()).getStatusCode());
        verify(service, never()).handleSepayTransfer(any());
    }

    @Test
    void unconfiguredKey_failsClosed() {
        props.setSepayApiKey("");
        assertEquals(HttpStatus.UNAUTHORIZED, controller.receive("Apikey ", body()).getStatusCode());
        verify(service, never()).handleSepayTransfer(any());
    }

    @Test
    void correctKey_processesAndAcks() {
        when(service.handleSepayTransfer(any())).thenReturn(TransferResult.PAID);
        var res = controller.receive("Apikey test-key-not-real", body());
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(true, res.getBody().get("success"));
    }

    @Test
    void raceOnDuplicateInsert_isAckedWithoutRetry() {
        when(service.handleSepayTransfer(any())).thenThrow(new DataIntegrityViolationException("dup"));
        assertEquals(HttpStatus.OK, controller.receive("Apikey test-key-not-real", body()).getStatusCode());
    }

    @Test
    void unexpectedFailure_returns5xxSoProviderRetries() {
        when(service.handleSepayTransfer(any())).thenThrow(new IllegalStateException("db down"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                controller.receive("Apikey test-key-not-real", body()).getStatusCode());
    }
}
