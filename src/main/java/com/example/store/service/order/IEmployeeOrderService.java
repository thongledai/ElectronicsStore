package com.example.store.service.order;

import com.example.store.entity.GhnWebhookEvent;
import com.example.store.entity.Order;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IEmployeeOrderService {
    Page<Order> search(String status, String keyword, int page);
    Order getDetail(UUID orderId);
    Map<UUID, String> orderCodes(Collection<Order> orders);
    List<GhnWebhookEvent> getGhnEvents(String ghnOrderCode);
    String getFormattedLatestGhnStatusTime(String ghnOrderCode);
}
