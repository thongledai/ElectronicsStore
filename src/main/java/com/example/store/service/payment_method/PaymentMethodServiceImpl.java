package com.example.store.service.payment_method;

import com.example.store.dto.payment_method.PaymentMethodDTO;
import com.example.store.entity.PaymentMethod;
import com.example.store.repository.PaymentMethodRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AllArgsConstructor
@Service
public class PaymentMethodServiceImpl implements IPaymentMethodService {
    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentMethodDTO> getAll() {
        return paymentMethodRepository.findAll().stream()
                .map(p -> new PaymentMethodDTO(p.getId(), p.getName()))
                .toList();
    }
}
