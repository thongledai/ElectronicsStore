package com.example.store.controller.view.employee;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.store.enums.OrderStatusName;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.mapper.GhnOrderMapper;
import com.example.store.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/employee/orders")
@RequiredArgsConstructor
public class EmployeeOrderViewController {
    private final OrderRepository orderRepository;
    private final GhnOrderMapper orderMapper;

    @GetMapping
    public String orders(@RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page, Model model) {
        String normalizedStatus = OrderStatusName.fromDbName(status).map(OrderStatusName::getDbName).orElse(null);
        model.addAttribute("orders", orderRepository.search(normalizedStatus, keyword,
                PageRequest.of(Math.max(0, page), 25, Sort.by(Sort.Direction.DESC, "createdAt"))));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("statusFilter", normalizedStatus == null ? "" : normalizedStatus);
        model.addAttribute("active", "orders");
        return "employee/orders";
    }

    @GetMapping("/{orderId}")
    public String orderDetail(@PathVariable UUID orderId, Model model) {
        var order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng."));
        model.addAttribute("order", order);
        model.addAttribute("newAddressModel", orderMapper.isNewAddressModel());
        model.addAttribute("defaultPaymentTypeId", orderMapper.getPaymentTypeId());
        model.addAttribute("defaultWeight", Math.max(1, orderMapper.calculateTotalWeightGram(order)));
        model.addAttribute("defaultLength", orderMapper.getDefaultLengthCm());
        model.addAttribute("defaultWidth", orderMapper.getDefaultWidthCm());
        model.addAttribute("defaultHeight", orderMapper.getDefaultHeightCm());
        model.addAttribute("active", "orders");
        return "employee/order-detail";
    }
}
