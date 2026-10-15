package com.example.store.service.delivery.impl;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.store.dto.ghn.GhnOrderStatusCallbackDTO;
import com.example.store.entity.GhnWebhookEvent;
import com.example.store.entity.Order;
import com.example.store.entity.OrderStatus;
import com.example.store.enums.OrderStatusName;
import com.example.store.enums.DeliveryMessage;
import com.example.store.exception.BusinessException;
import com.example.store.repository.GhnWebhookEventRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderStatusRepository;
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
    private final OrderStatusRepository orderStatusRepository;
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
                    DeliveryMessage.WEBHOOK_PAYLOAD_INVALID.getMessage());
        }

        if (callback.getShopId() == null
                || !String.valueOf(callback.getShopId()).equals(configuredShopId)) {
            throw new BusinessException(
                    DeliveryMessage.WEBHOOK_SHOP_MISMATCH.getMessage());
        }

        String type = callback.getType().trim().toLowerCase(Locale.ROOT);

        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(
                    String.format(DeliveryMessage.WEBHOOK_TYPE_UNSUPPORTED_PREFIX.getMessage(), type));
        }

        if (eventRepository.existsByOrderCodeAndEventTypeAndEventTime(
                callback.getOrderCode(), type, callback.getTime())) {
            return;
        }

        Order order = orderRepository.findByGhnOrderCodeForUpdate(callback.getOrderCode())
                .orElseGet(() -> StringUtils.hasText(callback.getClientOrderCode())
                        ? findByClientOrderCodeForUpdate(callback.getClientOrderCode())
                        : null);

        if (order == null) {
            throw new IllegalArgumentException(
                    DeliveryMessage.WEBHOOK_ORDER_NOT_FOUND.getMessage());
        }

        if (StringUtils.hasText(order.getGhnOrderCode())
                && !order.getGhnOrderCode().equals(callback.getOrderCode())) {
            throw new IllegalArgumentException(
                    DeliveryMessage.WEBHOOK_ORDER_CODE_CONFLICT.getMessage());
        }

        if (eventRepository.existsByOrderCodeAndEventTypeAndEventTime(
                callback.getOrderCode(), type, callback.getTime())) {
            return;
        }

        Instant latestEventTime = eventRepository
                .findFirstByOrderCodeOrderByEventTimeDesc(callback.getOrderCode())
                .map(GhnWebhookEvent::getEventTime)
                .orElse(null);

        GhnWebhookEvent event = new GhnWebhookEvent();
        event.setOrderCode(callback.getOrderCode());
        event.setEventType(type);
        event.setEventTime(callback.getTime());
        event.setStatus(callback.getStatus());

        eventRepository.saveAndFlush(event);

        // Keep an audit record for delayed callbacks, but never let one roll state back.
        boolean newest = latestEventTime == null || !callback.getTime().isBefore(latestEventTime);
        if (newest && StringUtils.hasText(callback.getStatus())) {
            order.setGhnOrderCode(callback.getOrderCode());
            order.setGhnStatus(callback.getStatus().trim().toLowerCase(Locale.ROOT));
            OrderStatusName mapped = mapOrderStatus(order.getGhnStatus());
            OrderStatusName current = OrderStatusName.fromDbName(order.getStatus().getName()).orElse(null);
            if ((current == OrderStatusName.DELIVERED
                    || current == OrderStatusName.CANCELLED
                    || current == OrderStatusName.RETURNED)
                    && mapped != current) {
                mapped = null;
            }
            if (current == OrderStatusName.SHIPPING && mapped == OrderStatusName.CONFIRMED) {
                mapped = null;
            }
            if (mapped != null) {
                OrderStatusName statusToApply = mapped;
                OrderStatus target = orderStatusRepository.findAll().stream()
                        .filter(status -> OrderStatusName.fromDbName(status.getName()).orElse(null) == statusToApply)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                                String.format(DeliveryMessage.STATUS_ROW_MISSING.getMessage(), statusToApply.getDbName())));
                order.setStatus(target);
            }
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

    private Order findByClientOrderCodeForUpdate(String clientOrderCode) {
        try {
            return orderRepository
                    .findByIdForUpdate(UUID.fromString(clientOrderCode))
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private OrderStatusName mapOrderStatus(String ghnStatus) {
        return switch (ghnStatus) {
            // Order is confirmed while GHN has accepted it and is arranging pickup.
            case "ready_to_pick", "picking", "money_collect_picking" -> OrderStatusName.CONFIRMED;
            // Intermediate delivery and return activity stays in the shipping lifecycle.
            case "picked", "storing", "sorting", "transporting", "delivering",
                    "money_collect_delivering", "delivery_fail", "waiting_to_return",
                    "return", "return_transporting", "return_sorting", "returning",
                    "return_fail" -> OrderStatusName.SHIPPING;
            case "delivered" -> OrderStatusName.DELIVERED;
            case "returned" -> OrderStatusName.RETURNED;
            case "cancel" -> OrderStatusName.CANCELLED;
            // exception/lost/damage/scrap have no matching business enum: preserve order status.
            default -> null;
        };
    }
}
