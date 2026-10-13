package com.example.store.service.delivery.impl;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.store.dto.ghn.GhnOrderStatusCallbackDTO;
import com.example.store.entity.GhnWebhookEvent;
import com.example.store.entity.Order;
import com.example.store.exception.BusinessException;
import com.example.store.repository.GhnWebhookEventRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.service.delivery.IGhnWebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GhnWebhookServiceImpl implements IGhnWebhookService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "create",
            "switch_status",
            "update_weight",
            "update_cod",
            "update_fee",
            "update_payment_type",
            "cod",
            "update_partial_return"
    );

    private final OrderRepository orderRepository;
    private final GhnWebhookEventRepository eventRepository;

    @Value("${ghn.shop-id:}")
    private String configuredShopId;

    @Override
    @Transactional
    public void handle(GhnOrderStatusCallbackDTO callback) {
        if (callback == null
                || !StringUtils.hasText(callback.getOrderCode())
                || !StringUtils.hasText(callback.getType())
                || callback.getTime() == null) {
            throw new BusinessException(
                    "Payload callback GHN thiếu OrderCode, Type hoặc Time.");
        }

        if (callback.getShopId() == null
                || !String.valueOf(callback.getShopId()).equals(configuredShopId)) {
            throw new BusinessException(
                    "ShopID callback không khớp cấu hình GHN.");
        }

        String type = callback.getType().trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(
                    "Loại callback GHN chưa được hỗ trợ: " + type);
        }

        if (eventRepository.existsByOrderCodeAndEventTypeAndEventTime(
                callback.getOrderCode(), type, callback.getTime())) {
            return;
        }

        Order order = orderRepository
                .findByGhnOrderCode(callback.getOrderCode())
                .orElseGet(() -> StringUtils.hasText(callback.getClientOrderCode())
                        ? findByClientOrderCode(callback.getClientOrderCode())
                        : null);

        if (order == null) {
            throw new IllegalArgumentException(
                    "Không tìm thấy đơn nội bộ tương ứng callback GHN.");
        }

        GhnWebhookEvent event = new GhnWebhookEvent();
        event.setOrderCode(callback.getOrderCode());
        event.setEventType(type);
        event.setEventTime(callback.getTime());
        event.setStatus(callback.getStatus());

        try {
            eventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException duplicate) {
            return;
        }

        if (StringUtils.hasText(callback.getStatus())) {
            order.setGhnStatus(callback.getStatus().trim());
            orderRepository.save(order);
        }

        log.info(
                "Processed GHN callback: orderCode={}, type={}, status={}, time={}",
                callback.getOrderCode(),
                type,
                callback.getStatus(),
                callback.getTime()
        );
    }

    private Order findByClientOrderCode(String clientOrderCode) {
        try {
            return orderRepository
                    .findById(UUID.fromString(clientOrderCode))
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
