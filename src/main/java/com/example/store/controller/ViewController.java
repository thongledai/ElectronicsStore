package com.example.store.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ViewController {

    // Trang chủ: chuyển hướng về /customer/index
    @GetMapping("/")
    public String home() {
        return "redirect:/customer/index";
    }

    @GetMapping("/customer/index")
    public String customerIndex() {
        return "customer/index";
    }

    @GetMapping("/customer/cart")
    public String customerCart() {
        return "customer/cart";
    }

    @GetMapping("/customer/wishlist")
    public String customerWishlist() {
        return "customer/wishlist";
    }

    @GetMapping("/customer/reset-password")
    public String customerResetPassword() {
        return "customer/reset-password";
    }

    // Các trang thông tin: /pages/{page}
    @GetMapping("/pages/{page}")
    public String staticPages(@PathVariable String page) {
        return "pages/" + page;
    }

    // Dashboard Quản lý: các trang tĩnh khác
    @GetMapping("/manager/index")
    public String managerIndex() {
        return "manager/index";
    }

    @GetMapping("/manager/users")
    public String managerUsers() {
        return "manager/users";
    }

    @GetMapping("/manager/promotions")
    public String managerPromotions() {
        return "manager/promotions";
    }

    @GetMapping("/manager/carriers")
    public String managerCarriers() {
        return "manager/carriers";
    }

    @GetMapping("/manager/orders")
    public String managerOrders() {
        return "manager/orders";
    }

    @GetMapping("/manager/statistics")
    public String managerStatistics() {
        return "manager/statistics";
    }

    @GetMapping("/manager/reset-password")
    public String managerResetPassword() {
        return "manager/reset-password";
    }

    // Dashboard Nhân viên: /employee/{page}
    @GetMapping("/employee/{page}")
    public String employeePages(@PathVariable String page) {
        return "employee/" + page;
    }

    // Dashboard Shipper: /shipper/{page}
    @GetMapping("/shipper/{page}")
    public String shipperPages(@PathVariable String page) {
        return "shipper/" + page;
    }
}
