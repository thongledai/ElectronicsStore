package com.example.store.service.order;

import com.example.store.dto.order.CheckoutPreviewDTO;
import com.example.store.dto.order.CheckoutPreviewRequest;
import com.example.store.dto.order.CheckoutRequest;
import com.example.store.dto.order.OrderDTO;

import java.util.UUID;

public interface IOrderService {
    CheckoutPreviewDTO preview(UUID userId, CheckoutPreviewRequest req);

    OrderDTO placeOrder(UUID userId, CheckoutRequest req);
}
