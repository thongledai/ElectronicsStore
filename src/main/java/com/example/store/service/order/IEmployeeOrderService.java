package com.example.store.service.order;

import com.example.store.entity.Order;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface IEmployeeOrderService {
    Page<Order> search(String status, String keyword, int page);
    Order getDetail(UUID orderId);
    Map<UUID, String> orderCodes(Collection<Order> orders);
}
