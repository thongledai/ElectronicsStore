package com.example.store.controller;

import java.security.Principal;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.store.service.auth.IAuthenticationService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final IAuthenticationService authenticationService;

    // Trang chủ: chuyển hướng về /customer/index
    @GetMapping("/")
    public String home() {
        return "redirect:/customer/index";
    }

    // Trang đổi mật khẩu Customer (dành cho user đã có mật khẩu)
    @GetMapping("/customer/reset-password")
    public String customerResetPassword(Principal principal) {
        if (principal != null && !authenticationService.hasPassword(principal.getName())) {
            // Google user chưa có mật khẩu -> chuyển hướng sang set-password
            return "redirect:/customer/set-password";
        }
        return "customer/reset-password";
    }

    // Trang tạo mật khẩu mới Customer (dành cho Google user chưa có mật khẩu)
    @GetMapping("/customer/set-password")
    public String customerSetPassword(Principal principal) {
        if (principal != null && authenticationService.hasPassword(principal.getName())) {
            // Đã có mật khẩu rồi -> chuyển hướng sang reset-password
            return "redirect:/customer/reset-password";
        }
        return "customer/set-password";
    }

    // Các trang của Customer: /customer/{page}
    @GetMapping("/customer/{page}")
    public String customerPages(@PathVariable String page) {
        return "customer/" + page;
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
