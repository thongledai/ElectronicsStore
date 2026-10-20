package com.example.store.mapper;

import com.example.store.dto.order.CustomerOrderItemDTO;
import com.example.store.dto.order.CustomerOrderResponseDTO;
import com.example.store.dto.order.OrderPreviewDTO;
import com.example.store.dto.order.OrderItemDTO;
import com.example.store.entity.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {
    public OrderPreviewDTO toOrderReviewDTO(Order o) {
        List<OrderItemDTO> items = o.getItems().stream().map(i -> new OrderItemDTO(
                i.getId(), i.getProductName(), i.getVariantDescription(),
                i.getImageUrl(), i.getUnitPrice(), i.getQuantity(), i.getSubtotal())).toList();

        return new OrderPreviewDTO(
                o.getId(),
                o.getStatus().getName(),
                o.getCreatedAt(),
                o.getUser().getFullName(),
                o.getPhone(),
                o.getFullAddress(),
                items,
                o.getSubtotal(),
                o.getShippingFee(),
                o.getDiscount(),
                o.getTotal(),
                o.getPayment().getName(),
                o.getIsPaidBefore(),
                o.getNote(),
                o.getGhnOrderCode());
    }

    public CustomerOrderResponseDTO toCustomerOrderResponseDTO(Order o) {
        List<CustomerOrderItemDTO> items = o.getItems().stream().map(i -> new CustomerOrderItemDTO(
                i.getId(),
                i.getVariant() != null ? i.getVariant().getId() : null,
                i.getVariant() != null && i.getVariant().getProduct() != null ? i.getVariant().getProduct().getId() : null,
                i.getProductName(),
                i.getVariantDescription(),
                i.getImageUrl(),
                i.getUnitPrice(),
                i.getQuantity() != null ? i.getQuantity() : 0,
                i.getSubtotal())).toList();

        String rawStatus = o.getStatus() != null ? o.getStatus().getName() : "";
        String statusDisplayName = getStatusDisplayName(rawStatus);
        boolean canCancel = "NOT_PROCESSED".equalsIgnoreCase(rawStatus);

        String orderCode = "#ORD-" + o.getId().toString().substring(0, 8).toUpperCase();
        String receiverName = o.getToName() != null ? o.getToName() : (o.getUser() != null ? o.getUser().getFullName() : "");
        String paymentMethodName = o.getPayment() != null ? o.getPayment().getName() : "COD";

        return new CustomerOrderResponseDTO(
                o.getId(),
                orderCode,
                rawStatus,
                statusDisplayName,
                o.getCreatedAt(),
                receiverName,
                o.getPhone(),
                o.getFullAddress(),
                items,
                o.getSubtotal(),
                o.getShippingFee(),
                o.getDiscount(),
                o.getTotal(),
                paymentMethodName,
                o.getIsPaidBefore(),
                o.getNote(),
                o.getGhnOrderCode(),
                o.getExpectedDeliveryDate(),
                canCancel
        );
    }

    public static String getStatusDisplayName(String status) {
        if (status == null) return "Không xác định";
        return switch (status.toUpperCase()) {
            case "NOT_PROCESSED" -> "Chờ xử lý";
            case "CONFIRMED", "SHIPPING" -> "Đang giao";
            case "DELIVERED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            case "RETURNED" -> "Trả hàng";
            default -> status;
        };
    }
}
