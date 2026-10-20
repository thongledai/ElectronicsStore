package com.example.store.service.order;

import com.example.store.dto.ghn.GhnCreateOrderResponse;
import com.example.store.dto.order.OrderPreviewRequest;
import com.example.store.entity.Order;

import java.math.BigDecimal;

public interface IShipmentService {
    GhnCreateOrderResponse createOrderPreview(OrderPreviewRequest orderPreviewRequest);
    GhnCreateOrderResponse createOrder(OrderPreviewRequest orderPreviewRequest, BigDecimal codAmount, BigDecimal expectedDeliveryCost, Order order);
}