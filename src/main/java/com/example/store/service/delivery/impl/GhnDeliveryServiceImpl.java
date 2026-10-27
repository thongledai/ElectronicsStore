package com.example.store.service.delivery.impl;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.example.store.dto.ghn.GhnCreateOrderPreviewDTO;
import com.example.store.entity.GhnWebhookEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.store.dto.ghn.GhnAddressOptionDTO;
import com.example.store.dto.ghn.GhnCreateOrderResponse;
import com.example.store.dto.ghn.GhnCreateOrderRequest;
import com.example.store.dto.ghn.GhnShipmentCreateRequestDTO;
import com.example.store.dto.ghn.GhnShipmentResponseDTO;
import com.example.store.entity.Order;
import com.example.store.entity.OrderStatus;
import com.example.store.enums.OrderStatusName;
import com.example.store.enums.DeliveryMessage;
import com.example.store.exception.BusinessException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.GhnOrderMapper;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderStatusRepository;
import com.example.store.repository.GhnWebhookEventRepository;
import com.example.store.service.delivery.GhnApiClient;
import com.example.store.service.delivery.IGhnAddressService;
import com.example.store.service.delivery.IGhnDeliveryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GhnDeliveryServiceImpl implements IGhnDeliveryService {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final GhnWebhookEventRepository webhookEventRepository;
    private final GhnOrderMapper ghnOrderMapper;
    private final GhnApiClient apiClient;
    private final IGhnAddressService addressService;

    @Override
    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(DeliveryMessage.ORDER_NOT_FOUND.getMessage()));

        if (StringUtils.hasText(order.getGhnOrderCode()))
            throw new BusinessException(DeliveryMessage.ORDER_HAS_SHIPMENT_CONFIRM.getMessage());

        if (currentStatus(order) != OrderStatusName.NOT_PROCESSED)
            throw new BusinessException(DeliveryMessage.ORDER_CONFIRM_STATUS_INVALID.getMessage());

        ensurePaymentVerified(order);

        order.setStatus(resolveStatus(OrderStatusName.CONFIRMED));
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public GhnShipmentResponseDTO createShipment(UUID orderId, GhnShipmentCreateRequestDTO input) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(DeliveryMessage.ORDER_NOT_FOUND.getMessage()));

        if (StringUtils.hasText(order.getGhnOrderCode()))
            return response(order, true);

        ensureConfirmed(order);
        ensurePaymentVerified(order);
        GhnCreateOrderRequest request = ghnOrderMapper.toCreateOrderRequest(order);

        // Áp dụng thông số kiện hàng từ form người dùng nếu có
        if (input != null) {
            if (input.getWeight() != null && input.getWeight() > 0) request.setWeight(input.getWeight());
            if (input.getLength() != null && input.getLength() > 0) request.setLength(input.getLength());
            if (input.getWidth() != null && input.getWidth() > 0) request.setWidth(input.getWidth());
            if (input.getHeight() != null && input.getHeight() > 0) request.setHeight(input.getHeight());
        }

        GhnCreateOrderResponse result = apiClient.createOrder(request);

        order.setGhnOrderCode(result.getOrderCode());
        if (result.getExpectedDeliveryTime() != null) {
            order.setExpectedDeliveryDate(LocalDateTime.ofInstant(Instant.parse(result.getExpectedDeliveryTime()), VIETNAM));
        }

        // Đồng bộ dữ liệu chuẩn hóa vào entity
        if (!StringUtils.hasText(order.getFullAddress()) && StringUtils.hasText(request.getToAddress())) {
            order.setFullAddress(request.getToAddress());
        }
        if (!StringUtils.hasText(order.getWardName()) && StringUtils.hasText(request.getToWardName())) {
            order.setWardName(request.getToWardName());
        }
        if (!StringUtils.hasText(order.getProvinceName()) && StringUtils.hasText(request.getToProvinceName())) {
            order.setProvinceName(request.getToProvinceName());
        }
        if (!StringUtils.hasText(order.getToName()) && StringUtils.hasText(request.getToName())) {
            order.setToName(request.getToName());
        }

        orderRepository.save(order);
        return response(order, false);
    }

    @Override
    @Transactional(readOnly = true)
    public GhnShipmentResponseDTO getShipment(UUID orderId) {
        Order order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(DeliveryMessage.ORDER_NOT_FOUND.getMessage()));
        return response(order, StringUtils.hasText(order.getGhnOrderCode()));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9]+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private String composeAddress(String detail, String ward, String province) {
        return java.util.stream.Stream.of(detail, ward, province)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /** Đơn VietQR chỉ đi tiếp khi backend đã xác minh tiền về (PAID). Ẩn nút trên UI là chưa đủ. */
    private void ensurePaymentVerified(Order order) {
        if (order.awaitingPayment())
            throw new BusinessException(DeliveryMessage.ORDER_PAYMENT_NOT_CONFIRMED.getMessage());
    }

    private void ensureConfirmed(Order order) {
        if (currentStatus(order) != OrderStatusName.CONFIRMED)
            throw new BusinessException(DeliveryMessage.ORDER_MUST_BE_CONFIRMED.getMessage());
    }

    private OrderStatusName currentStatus(Order order) {
        return OrderStatusName.fromDbName(order.getStatus().getName()).orElse(null);
    }

    private OrderStatus resolveStatus(OrderStatusName target) {
        return orderStatusRepository.findAll().stream()
                .filter(status -> OrderStatusName.fromDbName(status.getName()).orElse(null) == target)
                .findFirst().orElseThrow(() -> new BusinessException(
                        String.format(DeliveryMessage.STATUS_ROW_MISSING.getMessage(), target)));
    }

    private GhnShipmentResponseDTO response(Order order, boolean alreadyCreated) {
        var lastUpdate = StringUtils.hasText(order.getGhnOrderCode())
                ? webhookEventRepository.findFirstByOrderCodeOrderByEventTimeDesc(order.getGhnOrderCode())
                        .map(GhnWebhookEvent::getEventTime).orElse(null)
                : null;

        return GhnShipmentResponseDTO.builder().orderId(order.getId())
                .orderStatus(order.getStatus().getName()).ghnOrderCode(order.getGhnOrderCode())
                .ghnStatus(order.getStatus().getName()).ghnShippingFee(order.getShippingFee())
                .expectedDeliveryTime(order.getExpectedDeliveryDate()).ghnStatusUpdatedAt(lastUpdate)
                .alreadyCreated(alreadyCreated).build();
    }
}
