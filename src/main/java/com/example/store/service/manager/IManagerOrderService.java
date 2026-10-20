package com.example.store.service.manager;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.store.dto.manager.order.ManagerOrderDetailDTO;
import com.example.store.dto.manager.order.ManagerOrderSummaryDTO;
import com.example.store.dto.manager.order.OrderStatusUpdateDTO;

public interface IManagerOrderService {
    Page<ManagerOrderSummaryDTO> getOrders(
            String status,
            String keyword,
            Integer paymentMethodId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable);

    Map<String, Long> getOrderCountsByStatus();

    ManagerOrderDetailDTO getOrderDetail(UUID orderId);

    ManagerOrderDetailDTO updateOrderStatus(UUID orderId, OrderStatusUpdateDTO dto);
}
