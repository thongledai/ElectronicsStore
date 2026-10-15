package com.example.store.controller.view.employee;

import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.store.enums.OrderStatusName;
import com.example.store.enums.EmployeeMessage;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.GhnOrderMapper;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.GhnWebhookEventRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/employee/orders")
@RequiredArgsConstructor
public class EmployeeOrderViewController {
    private static final DateTimeFormatter GHN_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private final OrderRepository orderRepository;
    private final GhnOrderMapper orderMapper;
    private final GhnWebhookEventRepository webhookEventRepository;

    private String generateOrderCode(UUID orderId) {
        if (orderId == null) {
            return "ORD001";
        }
        return "ORD" + String.format("%03d", Math.abs(orderId.hashCode() % 1000));
    }

    @GetMapping
    public String orders(@RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, Model model) {
        String normalizedStatus = OrderStatusName.fromDbName(status).map(OrderStatusName::getDbName).orElse(null);
        var orders = orderRepository.search(normalizedStatus, keyword,
                PageRequest.of(Math.max(0, page), 25, Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("orders", orders);
        Map<UUID, String> orderCodes = orders.getContent().stream()
                .collect(Collectors.toMap(order -> order.getId(), order -> generateOrderCode(order.getId())));
        model.addAttribute("orderCodes", orderCodes);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("statusFilter", normalizedStatus == null ? "" : normalizedStatus);
        model.addAttribute("active", "orders");
        return "employee/orders";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable UUID orderId, Model model) {
        var order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(EmployeeMessage.ORDER_NOT_FOUND.getMessage()));
        model.addAttribute("order", order);
        model.addAttribute("displayOrderCode", generateOrderCode(order.getId()));
        model.addAttribute("ghnEvents", order.getGhnOrderCode() == null ? java.util.List.of()
                : webhookEventRepository.findTop20ByOrderCodeOrderByEventTimeDesc(order.getGhnOrderCode()));
        model.addAttribute("latestGhnStatusTime", order.getGhnOrderCode() == null ? null
                : webhookEventRepository.findFirstByOrderCodeOrderByEventTimeDesc(order.getGhnOrderCode())
                        .map(event -> GHN_TIME_FORMAT.format(event.getEventTime())).orElse(null));
        model.addAttribute("latestGhnStatusEventTime", order.getGhnOrderCode() == null ? null
                : webhookEventRepository.findFirstByOrderCodeOrderByEventTimeDesc(order.getGhnOrderCode())
                        .map(event -> event.getEventTime()).orElse(null));
        model.addAttribute("defaultPaymentTypeId", orderMapper.getPaymentTypeId());
        model.addAttribute("defaultWeight", Math.max(1, orderMapper.calculateTotalWeightGram(order)));
        model.addAttribute("defaultLength", orderMapper.getDefaultLengthCm());
        model.addAttribute("defaultWidth", orderMapper.getDefaultWidthCm());
        model.addAttribute("defaultHeight", orderMapper.getDefaultHeightCm());
        model.addAttribute("active", "orders");
        return "employee/order-detail";
    }
}
