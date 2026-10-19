package com.example.store.service.payment_method;

import com.example.store.dto.payment_method.PaymentMethodDTO;

import java.util.List;

public interface IPaymentMethodService {
    List<PaymentMethodDTO> getAll();
}
