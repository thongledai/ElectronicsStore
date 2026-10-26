package com.example.store.dto.payment;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Payload webhook SePay: https://developer.sepay.vn/vi/sepay-webhooks/tich-hop-webhook */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepayWebhookPayload {
    private Long id;
    private String gateway;
    private String transactionDate;
    private String accountNumber;
    private String code;
    private String content;
    /** "in" = tiền vào, "out" = tiền ra. */
    private String transferType;
    private BigDecimal transferAmount;
    private String referenceCode;
    private String description;
}
