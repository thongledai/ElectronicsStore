package com.example.store.service.manager.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.manager.order.ManagerOrderDetailDTO;
import com.example.store.dto.manager.order.ManagerOrderItemDTO;
import com.example.store.dto.manager.order.ManagerOrderSummaryDTO;
import com.example.store.dto.manager.order.OrderStatusUpdateDTO;
import com.example.store.entity.Order;
import com.example.store.entity.OrderItem;
import com.example.store.entity.OrderStatus;
import com.example.store.entity.ProductVariant;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.OrderStatusRepository;
import com.example.store.repository.ProductVariantRepository;
import com.example.store.service.manager.IManagerOrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerOrderServiceImpl implements IManagerOrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final ProductVariantRepository productVariantRepository;

    private String generateShortCode(UUID id) {
        if (id == null) return "ORD-0000";
        return "ORD-" + String.format("%04d", Math.abs(id.hashCode() % 10000));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ManagerOrderSummaryDTO> getOrders(
            String status,
            String keyword,
            Integer paymentMethodId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {

        String normalizedStatus = (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim()))
                ? status.trim().toUpperCase()
                : null;

        String normalizedKeyword = (keyword != null && !keyword.trim().isEmpty())
                ? keyword.trim()
                : null;

        LocalDateTime startDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime endDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

        Page<Order> page = orderRepository.searchOrdersForManager(
                normalizedStatus,
                normalizedKeyword,
                paymentMethodId,
                startDateTime,
                endDateTime,
                pageable);

        return page.map(this::toSummaryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getOrderCountsByStatus() {
        Map<String, Long> countMap = new HashMap<>();
        countMap.put("ALL", 0L);
        countMap.put("NOT_PROCESSED", 0L);
        countMap.put("CONFIRMED", 0L);
        countMap.put("SHIPPING", 0L);
        countMap.put("DELIVERED", 0L);
        countMap.put("CANCELLED", 0L);
        countMap.put("RETURNED", 0L);

        List<Object[]> groups = orderRepository.countOrdersByStatusGroup();
        long total = 0;
        if (groups != null) {
            for (Object[] row : groups) {
                if (row[0] != null && row[1] != null) {
                    String statusName = row[0].toString().trim().toUpperCase();
                    long cnt = ((Number) row[1]).longValue();
                    countMap.put(statusName, cnt);
                    total += cnt;
                }
            }
        }
        countMap.put("ALL", total);
        return countMap;
    }

    @Override
    @Transactional(readOnly = true)
    public ManagerOrderDetailDTO getOrderDetail(UUID orderId) {
        Order order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderId));
        return toDetailDTO(order);
    }

    @Override
    @Transactional
    public ManagerOrderDetailDTO updateOrderStatus(UUID orderId, OrderStatusUpdateDTO dto) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng với mã: " + orderId));

        String oldStatusName = order.getStatus() != null ? order.getStatus().getName().toUpperCase() : "";
        String newStatusName = dto.getNewStatus().trim().toUpperCase();

        OrderStatus targetStatus = orderStatusRepository.findByNameIgnoreCase(newStatusName)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trạng thái đơn hàng: " + dto.getNewStatus()));

        // Nghiệp vụ hoàn trả tồn kho nếu hủy đơn
        if ("CANCELLED".equals(newStatusName) && !"CANCELLED".equals(oldStatusName)) {
            restoreStockForOrder(order);
        }

        order.setStatus(targetStatus);

        if (dto.getNote() != null && !dto.getNote().trim().isEmpty()) {
            String noteEntry = "[Quản lý: " + dto.getNote().trim() + "]";
            if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
                order.setNote(order.getNote() + " | " + noteEntry);
            } else {
                order.setNote(noteEntry);
            }
        }

        orderRepository.save(order);
        log.info("Manager updated order {} status from {} to {}", orderId, oldStatusName, newStatusName);

        Order updated = orderRepository.findDetailedById(orderId).orElse(order);
        return toDetailDTO(updated);
    }

    private void restoreStockForOrder(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        Set<UUID> variantIds = order.getItems().stream()
                .filter(item -> item.getVariant() != null)
                .map(item -> item.getVariant().getId())
                .collect(Collectors.toSet());

        if (variantIds.isEmpty()) {
            return;
        }

        List<ProductVariant> variants = productVariantRepository.findAllByIdForUpdate(variantIds);
        Map<UUID, ProductVariant> variantMap = variants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        for (OrderItem item : order.getItems()) {
            if (item.getVariant() != null) {
                ProductVariant v = variantMap.get(item.getVariant().getId());
                if (v != null && item.getQuantity() != null && item.getQuantity() > 0) {
                    int currentQty = v.getQuantity() != null ? v.getQuantity() : 0;
                    v.setQuantity(currentQty + item.getQuantity());
                    log.info("Restored stock for variant {}: {} -> {}", v.getId(), currentQty, v.getQuantity());
                }
            }
        }
        productVariantRepository.saveAll(variants);
    }

    private ManagerOrderSummaryDTO toSummaryDTO(Order order) {
        String customerName = order.getUser() != null ? order.getUser().getFullName() : (order.getToName() != null ? order.getToName() : "Khách hàng");
        String customerPhone = order.getPhone() != null ? order.getPhone() : (order.getUser() != null ? order.getUser().getPhone() : "");
        String customerEmail = order.getUser() != null ? order.getUser().getEmail() : "";
        String statusName = order.getStatus() != null ? order.getStatus().getName() : "NOT_PROCESSED";
        String paymentName = order.getPayment() != null ? order.getPayment().getName() : "COD";
        int itemsCount = order.getItems() != null ? order.getItems().size() : 0;

        return ManagerOrderSummaryDTO.builder()
                .id(order.getId())
                .shortCode(generateShortCode(order.getId()))
                .customerName(customerName)
                .customerPhone(customerPhone)
                .customerEmail(customerEmail)
                .createdAt(order.getCreatedAt())
                .total(order.getTotal())
                .paymentMethodName(paymentName)
                .isPaidBefore(order.getIsPaidBefore() != null ? order.getIsPaidBefore() : false)
                .statusName(statusName)
                .ghnOrderCode(order.getGhnOrderCode())
                .itemCount(itemsCount)
                .build();
    }

    private ManagerOrderDetailDTO toDetailDTO(Order order) {
        String customerName = order.getUser() != null ? order.getUser().getFullName() : (order.getToName() != null ? order.getToName() : "Khách hàng");
        String customerPhone = order.getPhone() != null ? order.getPhone() : (order.getUser() != null ? order.getUser().getPhone() : "");
        String customerEmail = order.getUser() != null ? order.getUser().getEmail() : "";
        String statusName = order.getStatus() != null ? order.getStatus().getName() : "NOT_PROCESSED";
        String paymentName = order.getPayment() != null ? order.getPayment().getName() : "COD";

        List<ManagerOrderItemDTO> itemDTOs = order.getItems() != null
                ? order.getItems().stream().map(item -> ManagerOrderItemDTO.builder()
                        .id(item.getId())
                        .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                        .productName(item.getProductName())
                        .variantDescription(item.getVariantDescription())
                        .imageUrl(item.getImageUrl())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build()).collect(Collectors.toList())
                : new ArrayList<>();

        return ManagerOrderDetailDTO.builder()
                .id(order.getId())
                .shortCode(generateShortCode(order.getId()))
                .customerName(customerName)
                .customerPhone(customerPhone)
                .customerEmail(customerEmail)
                .toName(order.getToName())
                .phone(order.getPhone())
                .fullAddress(order.getFullAddress())
                .detailAddress(order.getDetailAddress())
                .wardName(order.getWardName())
                .provinceName(order.getProvinceName())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .paymentMethodName(paymentName)
                .isPaidBefore(order.getIsPaidBefore() != null ? order.getIsPaidBefore() : false)
                .statusName(statusName)
                .ghnOrderCode(order.getGhnOrderCode())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discount(order.getDiscount())
                .total(order.getTotal())
                .items(itemDTOs)
                .build();
    }
}
