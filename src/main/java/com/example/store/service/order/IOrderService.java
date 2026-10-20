package com.example.store.service.order;

import com.example.store.dto.order.CheckoutPreviewDTO;
import com.example.store.dto.order.CheckoutRequest;
import com.example.store.dto.order.OrderPreviewDTO;
import com.example.store.dto.order.OrderPreviewRequest;

import java.util.UUID;

public interface IOrderService {
    CheckoutPreviewDTO checkout(UUID userId, CheckoutRequest req);
    OrderPreviewDTO createOrderPreview(UUID userId, OrderPreviewRequest req);
    OrderPreviewDTO placeOrder(UUID userId, OrderPreviewRequest req);
}
