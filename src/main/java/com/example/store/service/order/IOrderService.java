package com.example.store.service.order;

import com.example.store.dto.order.CancelOrderRequest;
import com.example.store.dto.order.CheckoutPreviewDTO;
import com.example.store.dto.order.CheckoutRequest;
import com.example.store.dto.order.CustomerOrderResponseDTO;
import com.example.store.dto.order.OrderPreviewDTO;
import com.example.store.dto.order.OrderPreviewRequest;

import java.util.List;
import java.util.UUID;

public interface IOrderService {
    CheckoutPreviewDTO checkout(UUID userId, CheckoutRequest req);
    OrderPreviewDTO createOrderPreview(UUID userId, OrderPreviewRequest req);
    OrderPreviewDTO placeOrder(UUID userId, OrderPreviewRequest req);
    List<CustomerOrderResponseDTO> getCustomerOrders(UUID userId, String status);
    CustomerOrderResponseDTO getCustomerOrderDetail(UUID userId, UUID orderId);
    void cancelCustomerOrder(UUID userId, UUID orderId, CancelOrderRequest req);
}
