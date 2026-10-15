package com.example.store.service.delivery.impl;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.example.store.dto.ghn.GhnCreateOrderPreviewDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.store.dto.ghn.GhnAddressOptionDTO;
import com.example.store.dto.ghn.GhnCreateOrderData;
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
    private final GhnOrderMapper orderMapper;
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
        order.setStatus(resolveStatus(OrderStatusName.CONFIRMED));
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void saveAddressSelection(UUID orderId, GhnShipmentCreateRequestDTO input) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(DeliveryMessage.ORDER_NOT_FOUND.getMessage()));
        ensureConfirmed(order);
        if (StringUtils.hasText(order.getGhnOrderCode()))
            throw new BusinessException(DeliveryMessage.ORDER_HAS_SHIPMENT_ADDRESS_CHANGE.getMessage());
        validateAndApplyAddress(order, input);
    }

    @Override
    @Transactional(readOnly = true)
    public GhnCreateOrderPreviewDTO previewShipment(
            UUID orderId,
            GhnShipmentCreateRequestDTO input) {

        Order order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(DeliveryMessage.ORDER_NOT_FOUND.getMessage()));

        ensureConfirmed(order);

        if (StringUtils.hasText(order.getGhnOrderCode())) {
            throw new BusinessException(
                    DeliveryMessage.ORDER_HAS_SHIPMENT_PREVIEW.getMessage());
        }

        if (!orderMapper.isNewAddressModel()) {
            throw new BusinessException(
                    DeliveryMessage.NEW_ADDRESS_MODEL_REQUIRED.getMessage());
        }

        if (input == null)
            throw new BusinessException(DeliveryMessage.ADDRESS_INPUT_REQUIRED.getMessage());

        String detail = input.getAddressDetail() == null
                ? ""
                : input.getAddressDetail().trim();

        if (!StringUtils.hasText(detail)) {
            throw new BusinessException(
                    DeliveryMessage.ADDRESS_DETAIL_REQUIRED.getMessage());
        }

        if (detail.length() > 500) {
            throw new BusinessException(
                    DeliveryMessage.ADDRESS_DETAIL_TOO_LONG.getMessage());
        }

        GhnAddressOptionDTO province = input.getProvinceId() == null ? null
                : addressService.getProvinces()
                        .stream()
                        .filter(item -> item.getId() != null
                                && item.getId().equals(input.getProvinceId()))
                        .findFirst()
                        .orElse(null);

        GhnAddressOptionDTO ward = province == null || input.getWardId() == null ? null
                : addressService.getWards(province.getId())
                        .stream()
                        .filter(item -> item.getId() != null
                                && item.getId().equals(input.getWardId()))
                        .findFirst()
                        .orElse(null);

        String toAddress = composeAddress(detail, ward == null ? null : ward.getName(),
                province == null ? null : province.getName());

        validatePreviewReady(order, toAddress, ward == null ? null : ward.getName(),
                province == null ? null : province.getName());

        GhnCreateOrderRequest base = orderMapper.toCreateOrderRequest(
                order,
                toAddress,
                ward == null ? null : ward.getName(),
                province == null ? null : province.getName());

        GhnCreateOrderRequest payload = applyOverrides(base, order, input);

        return GhnCreateOrderPreviewDTO.builder()
                .sourceAddress(order.getToDetailAddress())
                .provinceId(province == null ? null : province.getId())
                .wardId(ward == null ? null : ward.getId())
                .provinceName(province == null ? null : province.getName())
                .wardName(ward == null ? null : ward.getName())
                .addressSource(
                        "GHN staging v3 master-data API; freshly fetched for this request")
                .validated(true)
                .districtFieldOmitted(payload.getToDistrictName() == null)
                .payload(payload)
                .build();
    }

    @Override
    @Transactional
    public GhnShipmentResponseDTO createShipment(UUID orderId, GhnShipmentCreateRequestDTO input) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(DeliveryMessage.ORDER_NOT_FOUND.getMessage()));
        if (StringUtils.hasText(order.getGhnOrderCode()))
            return response(order, true);
        ensureConfirmed(order);
        validateAndApplyAddress(order, input);
        validateReady(order);

        GhnCreateOrderRequest request = applyOverrides(orderMapper.toCreateOrderRequest(order), order, input);
        GhnCreateOrderData result = apiClient.createOrder(request);
        order.setGhnOrderCode(result.getOrderCode());
        order.setGhnStatus("created");
        order.setGhnShippingFee(result.getTotalFee());
        if (result.getExpectedDeliveryTime() != null) {
            order.setExpectedDeliveryTime(LocalDateTime.ofInstant(result.getExpectedDeliveryTime(), VIETNAM));
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

    private void validateAndApplyAddress(Order order, GhnShipmentCreateRequestDTO input) {
        if (!orderMapper.isNewAddressModel())
            throw new BusinessException(DeliveryMessage.NEW_ADDRESS_MODEL_REQUIRED.getMessage());
        if (input == null)
            throw new BusinessException(DeliveryMessage.ADDRESS_INPUT_REQUIRED.getMessage());
        String detail = input.getAddressDetail() == null ? "" : input.getAddressDetail().trim();
        if (!StringUtils.hasText(detail))
            throw new BusinessException(DeliveryMessage.ADDRESS_DETAIL_REQUIRED.getMessage());
        if (detail.length() > 500)
            throw new BusinessException(DeliveryMessage.ADDRESS_DETAIL_TOO_LONG.getMessage());

        GhnAddressOptionDTO province = input.getProvinceId() == null ? null
                : addressService.getProvinces().stream()
                        .filter(item -> item.getId() != null && item.getId().equals(input.getProvinceId()))
                        .findFirst()
                        .orElse(null);
        GhnAddressOptionDTO ward = province == null || input.getWardId() == null ? null
                : addressService.getWards(province.getId()).stream()
                        .filter(item -> item.getId() != null && item.getId().equals(input.getWardId()))
                        .findFirst()
                        .orElse(null);

        order.setToProvinceName(province == null ? null : province.getName());
        order.setToWardName(ward == null ? null : ward.getName());
        order.setToDetailAddress(detail);
        orderRepository.save(order);
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^a-zA-Z0-9]+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private void validateReady(Order order) {
        List<String> invalid = new ArrayList<>();
        if (order.getUser() == null || !StringUtils.hasText(order.getUser().getFullName()))
            invalid.add(DeliveryMessage.INVALID_RECIPIENT_NAME.getMessage());
        if (GhnOrderMapper.normalizePhone(order.getPhone()) == null)
            invalid.add(DeliveryMessage.INVALID_RECIPIENT_PHONE.getMessage());
        if (!StringUtils.hasText(order.getToDetailAddress()))
            invalid.add(DeliveryMessage.INVALID_DETAIL_ADDRESS.getMessage());
        if (!StringUtils.hasText(order.getToWardName()))
            invalid.add(DeliveryMessage.INVALID_WARD.getMessage());
        if (!StringUtils.hasText(order.getToProvinceName()))
            invalid.add(DeliveryMessage.INVALID_PROVINCE.getMessage());
        if (order.getItems() == null || order.getItems().isEmpty())
            invalid.add(DeliveryMessage.INVALID_ORDER_ITEMS.getMessage());
        if (!invalid.isEmpty())
            throw new BusinessException(String.format(DeliveryMessage.SHIPMENT_DATA_MISSING_PREFIX.getMessage(),
                    String.join(", ", invalid)));
        if (orderMapper.calculateTotalWeightGram(order) <= 0
                || orderMapper.calculateTotalWeightGram(order) > GhnOrderMapper.MAX_WEIGHT_GRAM_SERVICE_2) {
            throw new BusinessException(DeliveryMessage.SHIPMENT_WEIGHT_INVALID.getMessage());
        }
        if (orderMapper.calculateCodAmount(order) > GhnOrderMapper.MAX_COD_AMOUNT)
            throw new BusinessException(DeliveryMessage.COD_LIMIT_EXCEEDED.getMessage());
    }

    private String composeAddress(String detail, String ward, String province) {
        return java.util.stream.Stream.of(detail, ward, province)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private GhnCreateOrderRequest applyOverrides(GhnCreateOrderRequest base, Order order,
            GhnShipmentCreateRequestDTO input) {
        int weight = input.getWeight() == null ? base.getWeight() : input.getWeight();
        int length = input.getLength() == null ? base.getLength() : input.getLength();
        int width = input.getWidth() == null ? base.getWidth() : input.getWidth();
        int height = input.getHeight() == null ? base.getHeight() : input.getHeight();
        int payment = input.getPaymentTypeId() == null ? base.getPaymentTypeId() : input.getPaymentTypeId();
        if (weight <= 0 || weight > 19_999 || length <= 0 || width <= 0 || height <= 0 || length > 200 || width > 200
                || height > 200) {
            throw new BusinessException(DeliveryMessage.SHIPMENT_DIMENSIONS_INVALID.getMessage());
        }
        if (payment != 1 && payment != 2)
            throw new BusinessException(DeliveryMessage.PAYMENT_TYPE_INVALID.getMessage());
        return GhnCreateOrderRequest.builder().paymentTypeId(payment).requiredNote("CHOXEMHANGKHONGTHU")
                .clientOrderCode(base.getClientOrderCode()).toName(base.getToName()).toPhone(base.getToPhone())
                .toAddress(base.getToAddress()).toWardName(base.getToWardName()).toDistrictName(null)
                .toProvinceName(base.getToProvinceName()).isNewToAddress(true).serviceTypeId(base.getServiceTypeId())
                .weight(weight).length(length).width(width).height(height).codAmount(base.getCodAmount())
                .orderValue(base.getOrderValue()).insuranceValue(base.getInsuranceValue()).items(base.getItems())
                .build();
    }

    private void validatePreviewReady(
            Order order,
            String toAddress,
            String wardName,
            String provinceName) {

        List<String> invalid = new ArrayList<>();

        if (order.getUser() == null
                || !StringUtils.hasText(order.getUser().getFullName())) {
            invalid.add(DeliveryMessage.INVALID_RECIPIENT_NAME.getMessage());
        }

        if (GhnOrderMapper.normalizePhone(order.getPhone()) == null) {
            invalid.add(DeliveryMessage.INVALID_RECIPIENT_PHONE.getMessage());
        }

        if (!StringUtils.hasText(toAddress)) {
            invalid.add(DeliveryMessage.INVALID_ADDRESS.getMessage());
        }

        if (!StringUtils.hasText(wardName)) {
            invalid.add(DeliveryMessage.INVALID_WARD.getMessage());
        }

        if (!StringUtils.hasText(provinceName)) {
            invalid.add(DeliveryMessage.INVALID_PROVINCE.getMessage());
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            invalid.add(DeliveryMessage.INVALID_ORDER_ITEMS.getMessage());
        }

        if (!invalid.isEmpty()) {
            throw new BusinessException(
                    String.format(DeliveryMessage.SHIPMENT_DATA_MISSING_PREFIX.getMessage(),
                            String.join(", ", invalid)));
        }

        int totalWeight = orderMapper.calculateTotalWeightGram(order);

        if (totalWeight <= 0
                || totalWeight > GhnOrderMapper.MAX_WEIGHT_GRAM_SERVICE_2) {
            throw new BusinessException(
                    DeliveryMessage.SHIPMENT_WEIGHT_INVALID.getMessage());
        }

        if (orderMapper.calculateCodAmount(order) > GhnOrderMapper.MAX_COD_AMOUNT) {
            throw new BusinessException(DeliveryMessage.COD_LIMIT_EXCEEDED.getMessage());
        }
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
                        .map(event -> event.getEventTime()).orElse(null)
                : null;
        return GhnShipmentResponseDTO.builder().orderId(order.getId())
                .orderStatus(order.getStatus().getName()).ghnOrderCode(order.getGhnOrderCode())
                .ghnStatus(order.getGhnStatus()).ghnShippingFee(order.getGhnShippingFee())
                .expectedDeliveryTime(order.getExpectedDeliveryTime()).ghnStatusUpdatedAt(lastUpdate)
                .alreadyCreated(alreadyCreated).build();
    }
}
