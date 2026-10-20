package com.example.store.controller.view.employee;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.store.entity.Order;
import com.example.store.service.order.EmployeeOrderServiceImpl;
import com.example.store.service.order.IEmployeeOrderService;

import lombok.RequiredArgsConstructor;

@Slf4j
@Controller
@RequestMapping("/employee/orders")
@RequiredArgsConstructor
public class EmployeeOrderViewController {
    private final IEmployeeOrderService employeeOrderService;

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        log.info("EmployeeOrderViewController.list status={} keyword={}", status, keyword);
        Page<Order> orders = employeeOrderService.search(status, keyword, page);
        model.addAttribute("orders", orders);
        model.addAttribute("orderCodes", employeeOrderService.orderCodes(orders.getContent()));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("statusFilter", status == null ? "" : status);
        model.addAttribute("active", "orders");   // đồng nghiệp đã thêm, sidebar cần biến này
        return "employee/orders";
    }

    @GetMapping("/{orderId}")
    public String detail(@PathVariable UUID orderId, Model model) {
        log.info("EmployeeOrderViewController.detail orderId={}", orderId);
        Order order = employeeOrderService.getDetail(orderId);

        model.addAttribute("order", order);
        model.addAttribute("displayOrderCode", EmployeeOrderServiceImpl.orderCode(order.getId()));
        model.addAttribute("ghnEvents", employeeOrderService.getGhnEvents(order.getGhnOrderCode()));
        model.addAttribute("latestGhnStatusTime", employeeOrderService.getFormattedLatestGhnStatusTime(order.getGhnOrderCode()));
        model.addAttribute("defaultPaymentTypeId", 1);
        model.addAttribute("defaultWeight", 500);
        model.addAttribute("defaultLength", 30);
        model.addAttribute("defaultWidth", 20);
        model.addAttribute("defaultHeight", 10);
        model.addAttribute("active", "orders");
        return "employee/order-detail";
    }
}
