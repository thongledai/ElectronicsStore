package com.example.store.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.store.entity.PaymentMethod;
import com.example.store.repository.PaymentMethodRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Đảm bảo có phương thức BANK_QR (và COD) trong bảng payment_methods; không ghi đè dữ liệu có sẵn. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodInitializer implements ApplicationRunner {
    private final PaymentMethodRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        ensure(PaymentMethod.COD);
        ensure(PaymentMethod.BANK_QR);
    }

    private void ensure(String name) {
        boolean exists = repository.findAll().stream().anyMatch(p -> name.equalsIgnoreCase(p.getName()));
        if (!exists) {
            repository.save(PaymentMethod.builder().name(name).build());
            log.info("Đã thêm phương thức thanh toán {}", name);
        }
    }
}
