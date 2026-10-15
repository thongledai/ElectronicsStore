package com.example.store.mapper;

import com.example.store.dto.order.OrderDTO;
import com.example.store.dto.order.OrderItemDTO;
import com.example.store.entity.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {
    public OrderDTO toOrderDTO(Order o) {
        List<OrderItemDTO> items = o.getItems().stream().map(i -> new OrderItemDTO(
                i.getId(), i.getProductName(), i.getVariantDescription(),
                i.getImageUrl(), i.getUnitPrice(), i.getQuantity(), i.getSubtotal())).toList();

        return new OrderDTO(
                o.getId(),
                o.getStatus().getName(),
                o.getCreatedAt(),
                o.getUser().getFullName(),
                o.getPhone(),
                o.getAddress(),
                items,
                o.getSubtotal(),
                o.getShippingFee(),
                o.getDiscount(),
                o.getTotal(),
                o.getPayment().getName(),
                o.getIsPaidBefore(),
                o.getNote());
    }
}
